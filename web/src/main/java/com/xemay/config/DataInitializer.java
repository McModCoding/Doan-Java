package com.xemay.config;

import com.xemay.entity.HangXe;
import com.xemay.entity.KhachHang;
import com.xemay.entity.TaiKhoan;
import com.xemay.entity.VaiTro;
import com.xemay.entity.XeMay;
import com.xemay.repository.HangXeRepository;
import com.xemay.repository.KhachHangRepository;
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
 *   <li>3 tài khoản mặc định (admin, nhanvien, khachhang - mật khẩu 123456) - CHỈ thêm khi bảng TAI_KHOAN trống.</li>
 *   <li>Khi bật {@code xemay.du-lieu-mau=true} (profile h2): vài hãng xe, xe máy, khách hàng mẫu nếu bảng còn trống.</li>
 * </ul>
 * Nếu đã chạy script {@code database/QL_CUAHANG_XEMAY.sql} (có sẵn dữ liệu mẫu) thì lớp này không thêm gì cả.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Mật khẩu của các tài khoản mặc định và tài khoản mẫu trong script SQL. */
    public static final String MAT_KHAU_MAC_DINH = "123456";

    private final VaiTroRepository vaiTroRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final HangXeRepository hangXeRepository;
    private final XeMayRepository xeMayRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean taoDuLieuMau;

    public DataInitializer(VaiTroRepository vaiTroRepository,
                           TaiKhoanRepository taiKhoanRepository,
                           KhachHangRepository khachHangRepository,
                           HangXeRepository hangXeRepository,
                           XeMayRepository xeMayRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${xemay.du-lieu-mau:false}") boolean taoDuLieuMau) {
        this.vaiTroRepository = vaiTroRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
        this.hangXeRepository = hangXeRepository;
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
            TaiKhoan khach = taoTaiKhoan("khachhang", "Khách hàng mẫu", "khachhang@xemay.vn", "0901000003", khachHang);
            khachHangRepository.save(hoSo(khach, khach.getHoTen(), khach.getSoDienThoai(), khach.getEmail(), null));
            log.info("Đã tạo 3 tài khoản mặc định: admin, nhanvien, khachhang (mật khẩu: {})", MAT_KHAU_MAC_DINH);
        }

        if (taoDuLieuMau) {
            taoXeMayMau();
            taoKhachMuaTaiCuaHang();
        }
    }

    private VaiTro taoVaiTroNeuThieu(String ma, String ten) {
        return vaiTroRepository.findByMa(ma).orElseGet(() -> vaiTroRepository.save(new VaiTro(ma, ten)));
    }

    private TaiKhoan taoTaiKhoan(String tenDangNhap, String hoTen, String email, String soDienThoai, VaiTro vaiTro) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(tenDangNhap);
        taiKhoan.setMatKhau(passwordEncoder.encode(MAT_KHAU_MAC_DINH));
        taiKhoan.setHoTen(hoTen);
        taiKhoan.setEmail(email);
        taiKhoan.setSoDienThoai(soDienThoai);
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setKichHoat(true);
        return taiKhoanRepository.save(taiKhoan);
    }

    private static KhachHang hoSo(TaiKhoan taiKhoan, String hoTen, String soDienThoai, String email, String diaChi) {
        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(taiKhoan);
        khachHang.setHoTen(hoTen);
        khachHang.setSoDienThoai(soDienThoai);
        khachHang.setEmail(email);
        khachHang.setDiaChi(diaChi);
        return khachHang;
    }

    /** Vài hãng và mẫu xe để chạy thử khi không dùng SQL Server (lấy từ dữ liệu mẫu của script nhóm). */
    private void taoXeMayMau() {
        if (xeMayRepository.count() > 0) {
            return;
        }
        HangXe honda = hangXeRepository.save(new HangXe("Honda", "Nhật Bản"));
        HangXe yamaha = hangXeRepository.save(new HangXe("Yamaha", "Nhật Bản"));
        HangXe vespa = hangXeRepository.save(new HangXe("Vespa", "Ý"));
        HangXe vinfast = hangXeRepository.save(new HangXe("VinFast", "Việt Nam"));
        xeMayRepository.saveAll(List.of(
                xe(honda, "Vision", "Tay ga", 110, "Trắng", "32000000", 12),
                xe(honda, "Air Blade 160", "Tay ga", 160, "Đen", "56000000", 8),
                xe(honda, "Wave Alpha", "Xe số", 110, "Đỏ", "18000000", 20),
                xe(honda, "Winner X", "Côn tay", 150, "Đỏ đen", "47000000", 3),
                xe(yamaha, "Exciter 155", "Côn tay", 155, "Xanh", "52000000", 5),
                xe(yamaha, "Sirius", "Xe số", 115, "Đen", "22000000", 15),
                xe(yamaha, "Grande", "Tay ga", 125, "Trắng", "46000000", 2),
                xe(vespa, "Vespa Sprint", "Tay ga", 150, "Xanh", "80000000", 1),
                xe(vinfast, "Vento S", "Xe điện", null, "Đen", "18000000", 10)));
        log.info("Đã thêm 4 hãng xe và 9 xe máy mẫu.");
    }

    private void taoKhachMuaTaiCuaHang() {
        if (khachHangRepository.existsBySoDienThoai("0913000001")) {
            return;
        }
        khachHangRepository.saveAll(List.of(
                hoSo(null, "Phan Văn Lộc", "0913000001", null, "45 Lê Văn Việt, TP. Hồ Chí Minh"),
                hoSo(null, "Huỳnh Thị Mỹ", "0913000002", "my.huynh@gmail.com", "12 Đỗ Xuân Hợp, TP. Hồ Chí Minh")));
    }

    private static XeMay xe(HangXe hang, String ten, String loai, Integer phanKhoi, String mau, String gia, int soLuong) {
        XeMay xe = new XeMay(hang, ten, loai, new BigDecimal(gia), soLuong);
        xe.setPhanKhoi(phanKhoi);
        xe.setMauSac(mau);
        return xe;
    }
}
