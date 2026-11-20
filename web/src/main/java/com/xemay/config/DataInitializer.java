package com.xemay.config;

import com.xemay.entity.TaiKhoan;
import com.xemay.entity.VaiTro;
import com.xemay.entity.XeMay;
import com.xemay.repository.TaiKhoanRepository;
import com.xemay.repository.VaiTroRepository;
import com.xemay.repository.XeMayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chạy một lần khi ứng dụng khởi động: bảo đảm cơ sở dữ liệu có đủ dữ liệu TỐI THIỂU để đăng nhập được.
 *
 * <ul>
 *   <li>3 vai trò ADMIN, NHANVIEN, KHACHHANG - thêm nếu còn thiếu.</li>
 *   <li>3 tài khoản mặc định (admin, nhanvien, khachhang - mật khẩu 123456) - CHỈ thêm khi bảng tai_khoan trống.</li>
 * </ul>
 * Nếu đã chạy script SQL của nhóm (có sẵn dữ liệu mẫu) thì lớp này không thêm gì cả.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Mật khẩu của các tài khoản mặc định và tài khoản mẫu trong script SQL. */
    public static final String MAT_KHAU_MAC_DINH = "123456";

    private final VaiTroRepository vaiTroRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final XeMayRepository xeMayRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean taoDuLieuMau;

    public DataInitializer(VaiTroRepository vaiTroRepository,
                           TaiKhoanRepository taiKhoanRepository,
                           XeMayRepository xeMayRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${xemay.du-lieu-mau:false}") boolean taoDuLieuMau) {
        this.vaiTroRepository = vaiTroRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.xeMayRepository = xeMayRepository;
        this.passwordEncoder = passwordEncoder;
        this.taoDuLieuMau = taoDuLieuMau;
    }

    @Override
    public void run(ApplicationArguments args) {
        VaiTro admin = taoVaiTroNeuThieu(VaiTro.ADMIN, "Quản trị viên");
        VaiTro nhanVien = taoVaiTroNeuThieu(VaiTro.NHANVIEN, "Nhân viên");
        VaiTro khachHang = taoVaiTroNeuThieu(VaiTro.KHACHHANG, "Khách hàng");

        if (taiKhoanRepository.count() == 0) {
            taoTaiKhoan("admin", "Nguyễn Minh Quân", "admin@xemay.vn", "0901000001", admin);
            taoTaiKhoan("nhanvien", "Nhân viên bán hàng", "nhanvien@xemay.vn", "0901000002", nhanVien);
            taoTaiKhoan("khachhang", "Khách hàng mẫu", "khachhang@xemay.vn", "0901000003", khachHang);
            log.info("Đã tạo 3 tài khoản mặc định: admin, nhanvien, khachhang (mật khẩu: {})", MAT_KHAU_MAC_DINH);
        }

        if (taoDuLieuMau) {
            taoXeMayMau();
        }
    }

    private VaiTro taoVaiTroNeuThieu(String ma, String ten) {
        return vaiTroRepository.findByMa(ma).orElseGet(() -> vaiTroRepository.save(new VaiTro(ma, ten)));
    }

    private void taoTaiKhoan(String tenDangNhap, String hoTen, String email, String soDienThoai, VaiTro vaiTro) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(tenDangNhap);
        taiKhoan.setMatKhau(passwordEncoder.encode(MAT_KHAU_MAC_DINH));
        taiKhoan.setHoTen(hoTen);
        taiKhoan.setEmail(email);
        taiKhoan.setSoDienThoai(soDienThoai);
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setKichHoat(true);
        taiKhoanRepository.save(taiKhoan);
    }

    /**
     * Thêm vài xe mẫu để chạy thử phần tồn kho khi bảng xe_may còn trống (bật bằng xemay.du-lieu-mau=true).
     * Khi Entity XeMay được bổ sung các cột bắt buộc khác, đoạn này có thể không còn chạy được -
     * lỗi chỉ được ghi log, không làm ứng dụng ngừng khởi động.
     */
    private void taoXeMayMau() {
        try {
            if (xeMayRepository.count() > 0) {
                return;
            }
            xeMayRepository.saveAll(List.of(
                    new XeMay("Honda Vision 2024", new BigDecimal("31900000"), 12),
                    new XeMay("Honda Air Blade 125", new BigDecimal("42790000"), 8),
                    new XeMay("Honda Wave Alpha 110", new BigDecimal("18900000"), 20),
                    new XeMay("Honda Winner X", new BigDecimal("46160000"), 3),
                    new XeMay("Yamaha Exciter 155 VVA", new BigDecimal("48000000"), 5),
                    new XeMay("Yamaha Sirius FI", new BigDecimal("21500000"), 15),
                    new XeMay("Yamaha Grande Hybrid", new BigDecimal("46000000"), 2),
                    new XeMay("Piaggio Vespa Sprint 125", new BigDecimal("79800000"), 1)));
            log.info("Đã thêm 8 xe máy mẫu để chạy thử phần tồn kho.");
        } catch (RuntimeException e) {
            log.warn("Bỏ qua việc thêm xe máy mẫu: {}", e.getMessage());
        }
    }
}
