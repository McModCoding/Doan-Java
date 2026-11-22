package com.xemay.service;

import com.xemay.entity.ChiTietGioHang;
import com.xemay.entity.DonHang;
import com.xemay.entity.GioHang;
import com.xemay.entity.KhachHang;
import com.xemay.entity.XeMay;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.DonHangRepository;
import com.xemay.repository.GioHangRepository;
import com.xemay.repository.KhachHangRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

/**
 * ĐẶT HÀNG từ giỏ: tạo đơn (DON_HANG, CHI_TIET_DON_HANG) và trừ tồn kho trong CÙNG một giao dịch.
 *
 * <p>Thứ tự khóa: dòng giỏ của khách trước, các dòng xe sau (theo mã xe tăng dần, trong {@link TonKhoService}).
 * Mọi giao dịch đều khóa theo cùng thứ tự đó nên không thể chờ vòng tròn (deadlock).
 * <ul>
 *   <li>Hai khách cùng đặt chiếc xe cuối cùng: giao dịch sau phải chờ khóa dòng xe, đọc lại thấy hết hàng,
 *       nhận {@code HetHangException} và bị hủy toàn bộ - không tạo đơn, giỏ của khách đó còn nguyên.</li>
 *   <li>Một khách bấm "Đặt hàng" hai lần liên tiếp: lần sau chờ khóa giỏ, đến lượt thì giỏ đã trống.</li>
 * </ul>
 * Đơn mới ở trạng thái chờ xác nhận và đã trừ kho; hủy đơn thì trả hàng bằng {@code TonKhoService.nhapKho}.
 */
@Service
public class DatHangService {

    private static final Logger log = LoggerFactory.getLogger(DatHangService.class);

    private final GioHangRepository gioHangRepository;
    private final KhachHangRepository khachHangRepository;
    private final DonHangRepository donHangRepository;
    private final TonKhoService tonKhoService;

    public DatHangService(GioHangRepository gioHangRepository, KhachHangRepository khachHangRepository,
                          DonHangRepository donHangRepository, TonKhoService tonKhoService) {
        this.gioHangRepository = gioHangRepository;
        this.khachHangRepository = khachHangRepository;
        this.donHangRepository = donHangRepository;
        this.tonKhoService = tonKhoService;
    }

    @Transactional
    public DonHang datHang(Integer maTaiKhoan, String diaChiGiaoHang, String ghiChu) {
        String diaChi = diaChiGiaoHang == null ? "" : diaChiGiaoHang.trim();
        if (diaChi.isEmpty()) throw new DuLieuKhongHopLeException("diaChiGiaoHang", "Vui lòng nhập địa chỉ giao xe.");
        if (diaChi.length() > 255) throw new DuLieuKhongHopLeException("diaChiGiaoHang", "Địa chỉ tối đa 255 ký tự.");
        String loiNhan = ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
        if (loiNhan != null && loiNhan.length() > 500) throw new DuLieuKhongHopLeException("ghiChu", "Ghi chú tối đa 500 ký tự.");

        KhachHang khach = khachHangRepository.findByTaiKhoan_Id(maTaiKhoan)
                .orElseThrow(() -> new NghiepVuException("Tài khoản chưa có hồ sơ khách hàng."));
        // (1) Khóa giỏ của khách
        GioHang gio = gioHangRepository.khoaTheoKhachHang(khach.getMaKhachHang()).orElse(null);
        if (gio == null || gio.getDsChiTiet().isEmpty()) throw new NghiepVuException("Giỏ hàng đang trống.");

        // (2) Khóa từng xe theo mã tăng dần và trừ kho; thiếu hàng hoặc xe ngừng bán thì cả giao dịch bị hủy
        Map<Integer, Integer> soLuongTheoXe = new TreeMap<>();
        for (ChiTietGioHang dong : gio.getDsChiTiet()) {
            soLuongTheoXe.merge(dong.getXeMay().getMaXe(), dong.getSoLuong(), Integer::sum);
        }
        tonKhoService.xuatKhoNhieuXe(soLuongTheoXe);

        // (3) Tạo đơn với giá bán mới nhất (dòng xe vừa được đọc lại khi khóa)
        DonHang don = new DonHang();
        don.setKhachHang(khach);
        don.setDiaChiGiaoHang(diaChi);
        don.setGhiChu(loiNhan);
        for (ChiTietGioHang dong : gio.getDsChiTiet()) {
            XeMay xe = dong.getXeMay();
            don.themDong(xe, dong.getSoLuong(), xe.getGiaBan());
        }
        DonHang daLuu = donHangRepository.save(don);

        // (4) Đặt xong thì làm trống giỏ
        gio.getDsChiTiet().clear();
        log.info("Khách #{} đặt đơn #{}: {} mẫu xe, tổng {} [luồng: {}]", khach.getMaKhachHang(), daLuu.getMaDonHang(),
                soLuongTheoXe.size(), daLuu.getTongTien().toPlainString(), Thread.currentThread().getName());
        return daLuu;
    }
}
