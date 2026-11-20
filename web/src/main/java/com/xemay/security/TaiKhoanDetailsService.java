package com.xemay.security;

import com.xemay.entity.TaiKhoan;
import com.xemay.repository.TaiKhoanRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cầu nối giữa Spring Security và bảng {@code tai_khoan}.
 *
 * <p>Khi người dùng gửi form đăng nhập, Spring Security gọi {@link #loadUserByUsername} để đọc tài khoản
 * từ cơ sở dữ liệu, sau đó tự so sánh mật khẩu người dùng nhập với mật khẩu đã băm BCrypt.
 */
@Service
public class TaiKhoanDetailsService implements UserDetailsService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final BoDemDangNhapSai boDemDangNhapSai;

    public TaiKhoanDetailsService(TaiKhoanRepository taiKhoanRepository, BoDemDangNhapSai boDemDangNhapSai) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.boDemDangNhapSai = boDemDangNhapSai;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        String ten = TaiKhoan.chuanHoaTenDangNhap(tenDangNhap);
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(ten)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + ten));
        return new TaiKhoanDetails(taiKhoan, boDemDangNhapSai.dangBiKhoa(ten));
    }
}
