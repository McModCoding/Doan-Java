package com.xemay.service;

import com.xemay.dto.KhachHangForm;
import com.xemay.entity.KhachHang;
import com.xemay.entity.TaiKhoan;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.KhachHangRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Nghiệp vụ khách hàng - dựa trên {@code CustomerService} của thành viên phụ trách khách hàng
 * (lấy danh sách, tìm theo họ tên hoặc số điện thoại, thêm, sửa, xóa, gửi email chào mừng bất đồng bộ).
 *
 * <p>Tích hợp với phần tài khoản:
 * <ul>
 *   <li>Khách tự đăng ký (hoặc quản trị viên tạo tài khoản khách hàng) thì có ngay hồ sơ {@code KHACH_HANG}
 *       gắn với tài khoản ({@link #taoHoSoChoTaiKhoan}) - phần giỏ hàng, đặt hàng dùng hồ sơ này.</li>
 *   <li>Họ tên, số điện thoại được đồng bộ hai chiều giữa tài khoản và hồ sơ; email của khách có tài khoản
 *       chỉ đổi ở phía tài khoản (đó là email đăng nhập).</li>
 *   <li>Xóa tài khoản thì hồ sơ được giữ lại (thành khách không có tài khoản) để không mất lịch sử mua hàng.</li>
 * </ul>
 */
@Service
public class KhachHangService {

    private final KhachHangRepository khachHangRepository;
    private final ApplicationEventPublisher suKien;

    public KhachHangService(KhachHangRepository khachHangRepository, ApplicationEventPublisher suKien) {
        this.khachHangRepository = khachHangRepository;
        this.suKien = suKien;
    }

    // ------------------------------------------------------------------ Tra cứu

    /** Tìm theo họ tên hoặc số điện thoại, có phân trang. Từ khóa rỗng thì lấy tất cả. */
    @Transactional(readOnly = true)
    public Page<KhachHang> timKiem(String tuKhoa, Pageable pageable) {
        String mau = "%" + (tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT)) + "%";
        return khachHangRepository.timKiem(mau, pageable);
    }

    @Transactional(readOnly = true)
    public KhachHang timTheoMa(Integer maKhachHang) {
        return khachHangRepository.findById(maKhachHang)
                .orElseThrow(() -> new KhongTimThayException("Không tìm thấy khách hàng có mã " + maKhachHang + "."));
    }

    /** Hồ sơ khách hàng của một tài khoản (phần giỏ hàng, đặt hàng dùng hàm này). */
    @Transactional(readOnly = true)
    public Optional<KhachHang> timTheoTaiKhoan(Integer maTaiKhoan) {
        return khachHangRepository.findByTaiKhoan_Id(maTaiKhoan);
    }

    @Transactional(readOnly = true)
    public long demTatCa() {
        return khachHangRepository.count();
    }

    // ------------------------------------------------------------------ Trang quản trị khách hàng

    /** Nhân viên nhập khách mua tại cửa hàng (chưa có tài khoản web). */
    @Transactional
    public KhachHang them(KhachHangForm form) {
        String soDienThoai = form.getSoDienThoai().trim();
        kiemTraSoDienThoaiChuaDung(soDienThoai, null);

        KhachHang khachHang = new KhachHang();
        khachHang.setHoTen(form.getHoTen().trim());
        khachHang.setSoDienThoai(soDienThoai);
        khachHang.setEmail(chuanHoaEmail(form.getEmail()));
        khachHang.setDiaChi(rongThanhNull(form.getDiaChi()));
        khachHang.setNgaySinh(form.getNgaySinh());
        KhachHang daLuu = luu(khachHang);
        suKien.publishEvent(new SuKienKhachHangMoi(daLuu.getMaKhachHang(), daLuu.getHoTen(), daLuu.getEmail()));
        return daLuu;
    }

    /**
     * Sửa hồ sơ. Nếu hồ sơ gắn với tài khoản thì họ tên, số điện thoại được ghi sang cả tài khoản;
     * email giữ theo tài khoản (email đăng nhập chỉ đổi ở trang tài khoản).
     */
    @Transactional
    public KhachHang capNhat(Integer maKhachHang, KhachHangForm form) {
        KhachHang khachHang = timTheoMa(maKhachHang);
        String soDienThoai = form.getSoDienThoai().trim();
        kiemTraSoDienThoaiChuaDung(soDienThoai, maKhachHang);

        khachHang.setHoTen(form.getHoTen().trim());
        khachHang.setSoDienThoai(soDienThoai);
        khachHang.setDiaChi(rongThanhNull(form.getDiaChi()));
        khachHang.setNgaySinh(form.getNgaySinh());
        TaiKhoan taiKhoan = khachHang.getTaiKhoan();
        if (taiKhoan == null) {
            khachHang.setEmail(chuanHoaEmail(form.getEmail()));
        } else {
            taiKhoan.setHoTen(khachHang.getHoTen());
            taiKhoan.setSoDienThoai(soDienThoai);
            khachHang.setEmail(taiKhoan.getEmail());
        }
        return luu(khachHang);
    }

    /**
     * Xóa hồ sơ khách hàng. Không xóa được nếu hồ sơ đang gắn với tài khoản web, hoặc khách đã có
     * giỏ hàng / đơn hàng (ràng buộc khóa ngoại của các bảng GIO_HANG, DON_HANG).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void xoa(Integer maKhachHang) {
        KhachHang khachHang = timTheoMa(maKhachHang);
        if (khachHang.coTaiKhoan()) {
            throw new NghiepVuException("Khách hàng \"" + khachHang.getHoTen() + "\" đang có tài khoản \""
                    + khachHang.getTaiKhoan().getTenDangNhap() + "\". Hãy khóa hoặc xóa tài khoản trước.");
        }
        try {
            khachHangRepository.delete(khachHang);
            khachHangRepository.flush();   // chạy DELETE ngay để bắt lỗi khóa ngoại tại đây
        } catch (DataIntegrityViolationException e) {
            throw new NghiepVuException("Không thể xóa khách hàng \"" + khachHang.getHoTen()
                    + "\" vì đã có giỏ hàng hoặc đơn hàng.");
        }
    }

    // ------------------------------------------------------------------ Gắn với tài khoản (TaiKhoanService gọi)

    /**
     * Tạo hồ sơ khách hàng cho tài khoản vừa tạo (cùng họ tên, số điện thoại, email).
     * Phải được gọi BÊN TRONG giao dịch tạo tài khoản: lỗi ở đây làm hủy luôn việc tạo tài khoản.
     */
    @Transactional
    public KhachHang taoHoSoChoTaiKhoan(TaiKhoan taiKhoan) {
        String soDienThoai = batBuocSoDienThoai(taiKhoan.getSoDienThoai());
        kiemTraSoDienThoaiChuaDung(soDienThoai, null);

        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(taiKhoan);
        khachHang.setHoTen(taiKhoan.getHoTen());
        khachHang.setSoDienThoai(soDienThoai);
        khachHang.setEmail(taiKhoan.getEmail());
        KhachHang daLuu = luu(khachHang);
        suKien.publishEvent(new SuKienKhachHangMoi(daLuu.getMaKhachHang(), daLuu.getHoTen(), daLuu.getEmail()));
        return daLuu;
    }

    /** Tài khoản vừa đổi họ tên, số điện thoại, email: ghi sang hồ sơ khách hàng (nếu có, nếu chưa thì tạo). */
    @Transactional
    public void dongBoTuTaiKhoan(TaiKhoan taiKhoan) {
        Optional<KhachHang> hoSo = khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId());
        if (hoSo.isEmpty()) {
            taoHoSoChoTaiKhoan(taiKhoan);
            return;
        }
        KhachHang khachHang = hoSo.get();
        String soDienThoai = batBuocSoDienThoai(taiKhoan.getSoDienThoai());
        kiemTraSoDienThoaiChuaDung(soDienThoai, khachHang.getMaKhachHang());
        khachHang.setHoTen(taiKhoan.getHoTen());
        khachHang.setSoDienThoai(soDienThoai);
        khachHang.setEmail(taiKhoan.getEmail());
        luu(khachHang);
    }

    /** Tài khoản sắp bị xóa: giữ hồ sơ (và lịch sử mua hàng), chỉ bỏ liên kết. */
    @Transactional
    public void goLienKetTaiKhoan(Integer maTaiKhoan) {
        khachHangRepository.goLienKetTaiKhoan(maTaiKhoan);
    }

    // ------------------------------------------------------------------ Hàm dùng chung

    private void kiemTraSoDienThoaiChuaDung(String soDienThoai, Integer maKhachHangBoQua) {
        boolean daDung = maKhachHangBoQua == null
                ? khachHangRepository.existsBySoDienThoai(soDienThoai)
                : khachHangRepository.existsBySoDienThoaiAndMaKhachHangNot(soDienThoai, maKhachHangBoQua);
        if (daDung) {
            throw new DuLieuKhongHopLeException("soDienThoai", "Số điện thoại này đã thuộc về một khách hàng khác");
        }
    }

    /**
     * Lưu và đẩy ngay xuống CSDL. Hai người cùng lúc dùng một số điện thoại thì cả hai đều qua được bước kiểm tra
     * ở trên; chỉ mục UNIQUE của cột SoDienThoai là chốt chặn cuối cùng (giống cách xử lý tên đăng nhập).
     */
    private KhachHang luu(KhachHang khachHang) {
        try {
            return khachHangRepository.saveAndFlush(khachHang);
        } catch (DataIntegrityViolationException e) {
            throw new DuLieuKhongHopLeException("soDienThoai",
                    "Số điện thoại vừa được dùng cho khách hàng khác. Vui lòng kiểm tra lại.");
        }
    }

    private static String batBuocSoDienThoai(String soDienThoai) {
        if (soDienThoai == null || soDienThoai.isBlank()) {
            throw new DuLieuKhongHopLeException("soDienThoai",
                    "Vui lòng nhập số điện thoại (dùng cho hồ sơ khách hàng khi đặt mua xe)");
        }
        return soDienThoai.trim();
    }

    private static String chuanHoaEmail(String email) {
        return (email == null || email.isBlank()) ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String rongThanhNull(String chuoi) {
        return (chuoi == null || chuoi.isBlank()) ? null : chuoi.trim();
    }
}
