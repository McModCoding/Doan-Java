package com.xemay.service;

import com.xemay.dto.MatHangGio;
import com.xemay.dto.ThongTinGio;
import com.xemay.entity.ChiTietGioHang;
import com.xemay.entity.GioHang;
import com.xemay.entity.KhachHang;
import com.xemay.entity.XeMay;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.GioHangRepository;
import com.xemay.repository.KhachHangRepository;
import com.xemay.repository.XeMayRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Giỏ hàng của khách đang đăng nhập, lưu trong bảng GIO_HANG / CHI_TIET_GIO_HANG (mỗi khách một giỏ).
 * Giao diện gọi các hàm thêm, cập nhật, xóa, hiển thị; giới hạn mỗi mẫu tối đa 20 chiếc và không vượt
 * số xe đang có. Thêm vào giỏ KHÔNG trừ kho: kho chỉ bị trừ khi đặt hàng ({@link DatHangService}).
 */
@Service
public class GioHangService {
    public static final int TOI_DA_MOI_MAU = 20;

    private final GioHangRepository gioHangRepository;
    private final KhachHangRepository khachHangRepository;
    private final XeMayRepository xeMayRepository;
    private final GiaoDienXeService xeService;
    /** Giao dịch riêng (REQUIRES_NEW) chỉ để tạo giỏ lần đầu cho khách. */
    private final TransactionTemplate giaoDichTaoGio;

    public GioHangService(GioHangRepository gioHangRepository, KhachHangRepository khachHangRepository,
                          XeMayRepository xeMayRepository, GiaoDienXeService xeService,
                          PlatformTransactionManager quanLyGiaoDich) {
        this.gioHangRepository = gioHangRepository;
        this.khachHangRepository = khachHangRepository;
        this.xeMayRepository = xeMayRepository;
        this.xeService = xeService;
        this.giaoDichTaoGio = new TransactionTemplate(quanLyGiaoDich);
        this.giaoDichTaoGio.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void them(Integer maTaiKhoan, Integer xeId, int soLuong) {
        if (soLuong < 1 || soLuong > TOI_DA_MOI_MAU) throw new NghiepVuException("Chọn từ 1 đến 20 xe");
        GioHang gio = khoaGio(maTaiKhoan);
        XeMay xe = xeMayRepository.findById(xeId == null ? 0 : xeId)
                .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy mẫu xe"));
        ChiTietGioHang dong = timDong(gio, xeId);
        int moi = (dong == null ? 0 : dong.getSoLuong()) + soLuong;
        if (moi > TOI_DA_MOI_MAU || moi > xe.getSoLuong())
            throw new NghiepVuException("Số lượng yêu cầu vượt số xe đang có trong kho");
        if (dong == null) {
            gio.getDsChiTiet().add(new ChiTietGioHang(gio, xe, moi, xe.getGiaBan()));
        } else {
            dong.setSoLuong(moi);
            dong.setDonGia(xe.getGiaBan());
        }
    }

    @Transactional
    public void capNhat(Integer maTaiKhoan, Integer xeId, int soLuong) {
        if (soLuong <= 0) { xoa(maTaiKhoan, xeId); return; }
        if (soLuong > TOI_DA_MOI_MAU) throw new NghiepVuException("Mỗi mẫu xe được chọn tối đa 20 chiếc");
        GioHang gio = khoaGio(maTaiKhoan);
        ChiTietGioHang dong = timDong(gio, xeId);
        if (dong == null) throw new NghiepVuException("Xe này không có trong giỏ hàng");
        if (soLuong > dong.getXeMay().getSoLuong()) throw new NghiepVuException("Không đủ số lượng tồn kho");
        dong.setSoLuong(soLuong);
        dong.setDonGia(dong.getXeMay().getGiaBan());
    }

    @Transactional
    public void xoa(Integer maTaiKhoan, Integer xeId) {
        GioHang gio = khoaGio(maTaiKhoan);
        gio.getDsChiTiet().removeIf(d -> d.getXeMay().getMaXe().equals(xeId));
    }

    /** Giỏ để hiển thị, giá theo giá bán hiện tại của từng xe. */
    @Transactional(readOnly = true)
    public ThongTinGio hienThi(Integer maTaiKhoan) {
        Optional<GioHang> gio = khachHangCua(maTaiKhoan)
                .flatMap(kh -> gioHangRepository.findByKhachHang_MaKhachHang(kh.getMaKhachHang()));
        if (gio.isEmpty()) return new ThongTinGio(List.of(), 0, BigDecimal.ZERO);
        List<MatHangGio> items = new ArrayList<>();
        int tongSoLuong = 0;
        BigDecimal tongTien = BigDecimal.ZERO;
        for (ChiTietGioHang d : gioHangRepository.dongTrongGio(gio.get().getMaGioHang())) {
            int sl = d.getSoLuong();
            BigDecimal tien = d.getXeMay().getGiaBan().multiply(BigDecimal.valueOf(sl));
            items.add(new MatHangGio(xeService.map(d.getXeMay()), sl, tien));
            tongSoLuong += sl;
            tongTien = tongTien.add(tien);
        }
        return new ThongTinGio(List.copyOf(items), tongSoLuong, tongTien);
    }

    /** Địa chỉ trong hồ sơ khách hàng, điền sẵn vào ô địa chỉ giao xe. */
    @Transactional(readOnly = true)
    public String diaChiMacDinh(Integer maTaiKhoan) {
        return khachHangCua(maTaiKhoan).map(KhachHang::getDiaChi).orElse("");
    }

    /**
     * Giỏ của khách, đã KHÓA dòng giỏ đến hết giao dịch (các thao tác trên cùng một giỏ chạy lần lượt).
     * Khách chưa có giỏ thì tạo trong một giao dịch riêng; hai yêu cầu cùng tạo thì chỉ một bản ghi được lưu
     * (cột MaKhachHang là UNIQUE), yêu cầu còn lại đọc lại giỏ vừa tạo.
     */
    GioHang khoaGio(Integer maTaiKhoan) {
        KhachHang kh = khachHangCua(maTaiKhoan)
                .orElseThrow(() -> new NghiepVuException("Tài khoản chưa có hồ sơ khách hàng."));
        Optional<GioHang> gio = gioHangRepository.khoaTheoKhachHang(kh.getMaKhachHang());
        if (gio.isPresent()) return gio.get();
        try {
            giaoDichTaoGio.executeWithoutResult(trangThai -> gioHangRepository.saveAndFlush(
                    new GioHang(khachHangRepository.getReferenceById(kh.getMaKhachHang()))));
        } catch (DataIntegrityViolationException e) {
            // Yêu cầu khác vừa tạo giỏ cho cùng khách: dùng giỏ đó
        }
        return gioHangRepository.khoaTheoKhachHang(kh.getMaKhachHang())
                .orElseThrow(() -> new NghiepVuException("Không tạo được giỏ hàng, vui lòng thử lại."));
    }

    private Optional<KhachHang> khachHangCua(Integer maTaiKhoan) {
        return maTaiKhoan == null ? Optional.empty() : khachHangRepository.findByTaiKhoan_Id(maTaiKhoan);
    }

    private static ChiTietGioHang timDong(GioHang gio, Integer xeId) {
        for (ChiTietGioHang d : gio.getDsChiTiet()) {
            if (d.getXeMay().getMaXe().equals(xeId)) return d;
        }
        return null;
    }
}
