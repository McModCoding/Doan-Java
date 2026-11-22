package com.xemay.config;

import com.xemay.entity.VaiTro;
import com.xemay.security.DangNhapThanhCongHandler;
import com.xemay.security.DangNhapThatBaiHandler;
import com.xemay.security.TuChoiTruyCapHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

/**
 * CẤU HÌNH ĐĂNG NHẬP VÀ PHÂN QUYỀN cho toàn bộ ứng dụng.
 *
 * <p>Hệ thống có 3 vai trò: ADMIN (quản trị viên), NHANVIEN (nhân viên), KHACHHANG (khách hàng).
 * Phân quyền được kiểm tra ở 3 lớp:
 * <ol>
 *   <li><b>Theo URL</b> - trong {@link #securityFilterChain} bên dưới.</li>
 *   <li><b>Theo phương thức</b> - {@code @PreAuthorize} trên Service (bật bằng {@code @EnableMethodSecurity}).</li>
 *   <li><b>Theo giao diện</b> - thuộc tính {@code sec:authorize} trong Thymeleaf để ẩn/hiện menu, nút bấm.</li>
 * </ol>
 *
 * <p><b>Lưu ý cho cả nhóm:</b> URL nào KHÔNG được liệt kê ở đây thì mặc định phải đăng nhập mới vào được.
 * Muốn một trang mới được xem công khai, hãy thêm đường dẫn của nó vào nhóm (1).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Tên hai ô nhập trong form đăng nhập. */
    public static final String THAM_SO_TEN_DANG_NHAP = "tenDangNhap";
    public static final String THAM_SO_MAT_KHAU = "matKhau";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DangNhapThanhCongHandler dangNhapThanhCong,
                                                   DangNhapThatBaiHandler dangNhapThatBai,
                                                   TuChoiTruyCapHandler tuChoiTruyCap) throws Exception {
        http
                .authorizeHttpRequests(quyen -> quyen
                        // (1) Ai cũng xem được, không cần đăng nhập
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/dang-nhap", "/dang-ky", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/xe-may", "/xe-may/**", "/hang-xe/**").permitAll()

                        // Giỏ hàng chỉ dành cho KHACHHANG, mọi thay đổi dùng POST + CSRF
                        .requestMatchers("/gio-hang", "/gio-hang/**").hasRole(VaiTro.KHACHHANG)

                        // (2) Khu vực quản trị: quy tắc cụ thể hơn phải đặt TRƯỚC quy tắc chung
                        .requestMatchers("/quan-tri/tai-khoan/**", "/quan-tri/lich-su-dang-nhap/**").hasRole(VaiTro.ADMIN)
                        .requestMatchers("/quan-tri/**").hasAnyRole(VaiTro.ADMIN, VaiTro.NHANVIEN)

                        // (3) Mọi URL còn lại (giỏ hàng, đặt hàng, hóa đơn, tài khoản cá nhân...): phải đăng nhập
                        .anyRequest().authenticated())

                .formLogin(dangNhap -> dangNhap
                        .loginPage("/dang-nhap")              // trang đăng nhập do nhóm tự thiết kế
                        .loginProcessingUrl("/dang-nhap")     // form gửi POST về đây, Spring Security tự xử lý
                        .usernameParameter(THAM_SO_TEN_DANG_NHAP)
                        .passwordParameter(THAM_SO_MAT_KHAU)
                        .successHandler(dangNhapThanhCong)
                        .failureHandler(dangNhapThatBai))

                .logout(dangXuat -> dangXuat
                        .logoutUrl("/dang-xuat")              // phải gửi bằng POST (có CSRF token)
                        .logoutSuccessUrl("/dang-nhap?dangXuat")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))

                .exceptionHandling(loi -> loi.accessDeniedHandler(tuChoiTruyCap));

        return http.build();
    }

    /** Mật khẩu được băm bằng BCrypt (có "muối" ngẫu nhiên) trước khi lưu, không thể dịch ngược. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bộ xác thực dùng chung. Trang web dùng nó qua form đăng nhập; Socket server gọi trực tiếp
     * để kiểm tra tài khoản của nhân viên kho - cả hai dùng chung một cách kiểm tra mật khẩu.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cauHinh) throws Exception {
        return cauHinh.getAuthenticationManager();
    }

    /**
     * Ghi nhớ trang mà người dùng định vào trước khi bị yêu cầu đăng nhập, để đăng nhập xong quay lại đúng trang đó.
     * Chỉ ghi nhớ khi trình duyệt đang mở một TRANG (chấp nhận text/html); bỏ qua các request ngầm
     * của trình duyệt (icon, file cấu hình...) để không bị chuyển tới một địa chỉ lạ sau khi đăng nhập.
     */
    @Bean
    public RequestCache requestCache() {
        HttpSessionRequestCache boNho = new HttpSessionRequestCache();
        boNho.setRequestMatcher(request -> {
            String chapNhan = request.getHeader(HttpHeaders.ACCEPT);
            return HttpMethod.GET.matches(request.getMethod()) && chapNhan != null && chapNhan.contains("text/html");
        });
        return boNho;
    }
}
