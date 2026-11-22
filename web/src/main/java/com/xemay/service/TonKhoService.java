package com.xemay.service;

import com.xemay.dto.CanhBaoTonKho;
import com.xemay.entity.XeMay;
import com.xemay.exception.HetHangException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.XeMayRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * QUẢN LÝ TỒN KHO - nơi xử lý truy cập đồng thời vào dữ liệu dùng chung.
 *
 * <p><b>Tài nguyên dùng chung:</b> cột {@code SoLuong} của mỗi dòng trong bảng {@code XE_MAY}.
 * <br><b>Ai cùng truy cập:</b> nhiều khách đặt cùng một mẫu xe (mỗi request là một luồng của Tomcat),
 * nhân viên nhập/xuất kho từ chương trình client (mỗi client là một luồng của Socket server).
 *
 * <p><b>Vấn đề nếu không đồng bộ:</b> thao tác trừ kho gồm 3 bước "đọc số tồn - kiểm tra đủ hàng - ghi số mới".
 * Xe chỉ còn 1 chiếc, hai giao dịch A và B cùng đọc được 1, cùng thấy đủ hàng, cùng ghi 0:
 * cửa hàng bán 2 chiếc trong khi chỉ có 1 (bán vượt tồn kho).
 *
 * <p><b>Cách đồng bộ:</b> khóa bi quan của JPA ({@link LockModeType#PESSIMISTIC_WRITE}). Trước khi đọc số tồn,
 * giao dịch khóa dòng của xe đó; giao dịch khác muốn khóa cùng dòng phải CHỜ đến khi giao dịch trước
 * commit hoặc rollback, rồi mới đọc được số tồn MỚI. Ba bước trên vì thế không bao giờ chạy xen kẽ nhau.
 *
 * <p><b>Cách dùng cho chức năng đặt hàng:</b> {@code DatHangService} gọi {@link #xuatKhoNhieuXe} BÊN TRONG
 * giao dịch tạo đơn hàng. Nếu hết hàng, {@link HetHangException} làm cả giao dịch bị hủy, không có đơn hàng
 * dở dang. Kho bị trừ ngay lúc đặt, nên procedure {@code sp_XacNhanDonHang} trong script SQL không trừ kho nữa;
 * hủy đơn thì trả hàng bằng {@link #nhapKho}.
 */
@Service
public class TonKhoService {

    private static final Logger log = LoggerFactory.getLogger(TonKhoService.class);

    /** Giới hạn cho một lần nhập/xuất, tránh gõ nhầm số quá lớn. */
    public static final int SO_LUONG_TOI_DA_MOT_LAN = 100_000;

    private final XeMayRepository xeMayRepository;
    private final EntityManager entityManager;
    private final int nguongCanhBao;

    /**
     * Kết quả lần quét tồn kho gần nhất. Luồng định kỳ GHI, các luồng xử lý request ĐỌC.
     * {@code volatile} bảo đảm luồng đọc luôn thấy đối tượng mới nhất; bản thân đối tượng là bất biến.
     */
    private volatile CanhBaoTonKho canhBaoGanNhat;

    public TonKhoService(XeMayRepository xeMayRepository,
                         EntityManager entityManager,
                         @Value("${xemay.ton-kho.nguong-canh-bao:3}") int nguongCanhBao) {
        this.xeMayRepository = xeMayRepository;
        this.entityManager = entityManager;
        this.nguongCanhBao = nguongCanhBao;
        this.canhBaoGanNhat = CanhBaoTonKho.chuaQuet(nguongCanhBao);
    }

    /**
     * Trừ tồn kho của một xe (bán hàng, đặt hàng, xuất kho).
     *
     * @return số lượng tồn còn lại sau khi trừ
     * @throws HetHangException      nếu không đủ hàng - số tồn KHÔNG bị thay đổi
     * @throws KhongTimThayException nếu không có xe mang mã này
     * @throws NghiepVuException     nếu xe đã ngừng kinh doanh
     */
    @Transactional
    public int xuatKho(Integer maXe, int soLuong) {
        kiemTraSoLuong(soLuong);
        XeMay xe = khoaDongXe(maXe);                          // (1) khóa dòng và đọc số tồn mới nhất
        if (!Boolean.TRUE.equals(xe.getTrangThai())) {
            throw new NghiepVuException("Xe \"" + xe.getTenDayDu() + "\" đã ngừng kinh doanh.");
        }
        if (xe.getSoLuong() < soLuong) {                      // (2) kiểm tra
            throw new HetHangException(xe.getTenDayDu(), xe.getSoLuong(), soLuong);
        }
        xe.setSoLuong(xe.getSoLuong() - soLuong);             // (3) ghi - Hibernate tự UPDATE khi commit
        log.info("Xuất kho xe #{} '{}': -{} -> còn {} [luồng: {}]",
                xe.getMaXe(), xe.getTenDayDu(), soLuong, xe.getSoLuong(), Thread.currentThread().getName());
        return xe.getSoLuong();
    }

    /**
     * Trừ tồn kho cho NHIỀU xe trong cùng một giao dịch (một đơn hàng có nhiều dòng).
     * Hoặc trừ được tất cả, hoặc không trừ xe nào.
     *
     * <p><b>Tránh deadlock:</b> luôn khóa các dòng theo thứ tự mã xe TĂNG DẦN. Nếu đơn A khóa xe 1 rồi chờ xe 2,
     * trong khi đơn B khóa xe 2 rồi chờ xe 1 thì hai giao dịch chờ nhau mãi mãi. Khi mọi giao dịch đều khóa
     * theo cùng một thứ tự thì tình huống đó không thể xảy ra.
     *
     * @param soLuongTheoXe mã xe -> số lượng cần trừ
     */
    @Transactional
    public void xuatKhoNhieuXe(Map<Integer, Integer> soLuongTheoXe) {
        for (Map.Entry<Integer, Integer> dong : new TreeMap<>(soLuongTheoXe).entrySet()) {
            int soLuong = dong.getValue() == null ? 0 : dong.getValue();
            xuatKho(dong.getKey(), soLuong);
        }
    }

    /**
     * Cộng tồn kho của một xe (nhập hàng, hoàn hàng khi hủy đơn).
     *
     * @return số lượng tồn sau khi cộng
     */
    @Transactional
    public int nhapKho(Integer maXe, int soLuong) {
        kiemTraSoLuong(soLuong);
        XeMay xe = khoaDongXe(maXe);
        long soMoi = (long) xe.getSoLuong() + soLuong;
        if (soMoi > Integer.MAX_VALUE) {
            throw new NghiepVuException("Số lượng tồn vượt quá giới hạn cho phép.");
        }
        xe.setSoLuong((int) soMoi);
        log.info("Nhập kho xe #{} '{}': +{} -> còn {} [luồng: {}]",
                xe.getMaXe(), xe.getTenDayDu(), soLuong, xe.getSoLuong(), Thread.currentThread().getName());
        return xe.getSoLuong();
    }

    /** Tìm xe theo mã, đọc kèm hãng xe (dùng được cả ngoài giao dịch, ví dụ ở luồng Socket). */
    @Transactional(readOnly = true)
    public XeMay timXe(Integer maXe) {
        if (maXe == null) {
            throw new KhongTimThayException("Chưa chọn xe máy.");
        }
        return xeMayRepository.findWithHangXeByMaXe(maXe)
                .orElseThrow(() -> new KhongTimThayException("Không tìm thấy xe máy có mã " + maXe + "."));
    }

    /** Danh sách xe kèm số tồn, lọc theo tên xe hoặc tên hãng (tối đa 50 xe). Từ khóa rỗng thì lấy tất cả. */
    @Transactional(readOnly = true)
    public List<XeMay> timXeTheoTen(String tuKhoa) {
        String mau = "%" + (tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT)) + "%";
        return xeMayRepository.timTheoTenHoacHang(mau, PageRequest.of(0, 50));
    }

    /**
     * Quét các xe sắp hết hàng và công bố kết quả cho trang quản trị. Được tác vụ định kỳ gọi.
     *
     * @return kết quả vừa quét
     */
    @Transactional(readOnly = true)
    public CanhBaoTonKho quetXeSapHetHang() {
        List<CanhBaoTonKho.Muc> danhSach = xeMayRepository
                .findByTrangThaiTrueAndSoLuongLessThanEqualOrderBySoLuongAscTenXeAsc(nguongCanhBao).stream()
                .map(xe -> new CanhBaoTonKho.Muc(xe.getMaXe(), xe.getTenDayDu(), xe.getSoLuong()))
                .toList();
        CanhBaoTonKho ketQua = new CanhBaoTonKho(LocalDateTime.now(), nguongCanhBao, danhSach);
        this.canhBaoGanNhat = ketQua;
        return ketQua;
    }

    /** Kết quả lần quét gần nhất (không truy vấn CSDL). */
    public CanhBaoTonKho getCanhBaoGanNhat() {
        return canhBaoGanNhat;
    }

    /**
     * Khóa dòng của một xe cho đến hết giao dịch hiện tại và trả về dữ liệu MỚI NHẤT của dòng đó.
     *
     * <p>{@code refresh(..., PESSIMISTIC_WRITE)} sinh ra câu lệnh
     * {@code SELECT ... FROM XE_MAY WITH (UPDLOCK, HOLDLOCK, ROWLOCK) WHERE MaXe = ?} trên SQL Server
     * (trên CSDL khác là {@code SELECT ... FOR UPDATE}).
     *
     * <p>Dùng {@code refresh} chứ không chỉ {@code find}: nơi gọi có thể đã đọc xe này từ trước trong cùng
     * giao dịch (ví dụ để lấy giá bán), khi đó Hibernate đang giữ một bản CŨ trong bộ nhớ. {@code refresh}
     * buộc đọc lại từ CSDL sau khi đã giữ khóa, nên số tồn dùng để kiểm tra luôn là số mới nhất.
     */
    private XeMay khoaDongXe(Integer maXe) {
        XeMay xe = timXe(maXe);
        try {
            entityManager.refresh(xe, LockModeType.PESSIMISTIC_WRITE);
        } catch (EntityNotFoundException e) {
            // Xe vừa bị xóa bởi giao dịch khác trong lúc chờ khóa
            throw new KhongTimThayException("Không tìm thấy xe máy có mã " + maXe + ".");
        }
        return xe;
    }

    private static void kiemTraSoLuong(int soLuong) {
        if (soLuong < 1 || soLuong > SO_LUONG_TOI_DA_MOT_LAN) {
            throw new NghiepVuException("Số lượng phải từ 1 đến " + SO_LUONG_TOI_DA_MOT_LAN + ".");
        }
    }
}
