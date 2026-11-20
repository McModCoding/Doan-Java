package com.xemay.security;

import com.xemay.entity.TaiKhoan;
import com.xemay.entity.VaiTro;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Thông tin của người dùng ĐANG ĐĂNG NHẬP, được Spring Security giữ trong phiên (session).
 *
 * <p>Cách lấy trong Controller của bất kỳ thành viên nào:
 * <pre>
 * &#64;GetMapping("/gio-hang")
 * public String gioHang(&#64;AuthenticationPrincipal TaiKhoanDetails nguoiDung, Model model) {
 *     Long taiKhoanId = nguoiDung.getId();   // mã tài khoản của người đang đăng nhập
 *     ...
 * }
 * </pre>
 */
public class TaiKhoanDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String tenDangNhap;
    private String matKhau;
    private final String hoTen;
    private final String maVaiTro;
    private final String tenVaiTro;
    private final boolean kichHoat;
    private final boolean biTamKhoa;

    /**
     * @param taiKhoan  tài khoản đọc từ cơ sở dữ liệu
     * @param biTamKhoa true nếu tài khoản đang bị tạm khóa do nhập sai mật khẩu quá nhiều lần
     */
    public TaiKhoanDetails(TaiKhoan taiKhoan, boolean biTamKhoa) {
        this.id = taiKhoan.getId();
        this.tenDangNhap = taiKhoan.getTenDangNhap();
        this.matKhau = taiKhoan.getMatKhau();
        this.hoTen = taiKhoan.getHoTen();
        this.maVaiTro = taiKhoan.getVaiTro().getMa();
        this.tenVaiTro = taiKhoan.getVaiTro().getTen();
        this.kichHoat = taiKhoan.isKichHoat();
        this.biTamKhoa = biTamKhoa;
    }

    /** Quyền của người dùng: mã vai trò thêm tiền tố ROLE_, ví dụ ROLE_ADMIN. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + maVaiTro));
    }

    @Override
    public String getPassword() {
        return matKhau;
    }

    @Override
    public String getUsername() {
        return tenDangNhap;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** false -> Spring Security từ chối đăng nhập với LockedException (kể cả khi nhập đúng mật khẩu). */
    @Override
    public boolean isAccountNonLocked() {
        return !biTamKhoa;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** false -> Spring Security từ chối đăng nhập với DisabledException (tài khoản bị quản trị viên khóa). */
    @Override
    public boolean isEnabled() {
        return kichHoat;
    }

    /** Spring Security gọi sau khi xác thực xong: xóa mật khẩu đã băm khỏi bộ nhớ phiên. */
    @Override
    public void eraseCredentials() {
        this.matKhau = null;
    }

    public Long getId() {
        return id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public String getMaVaiTro() {
        return maVaiTro;
    }

    /** Tên vai trò để hiển thị, ví dụ "Quản trị viên". */
    public String getTenVaiTro() {
        return tenVaiTro;
    }

    /** Quản trị viên hoặc nhân viên - những người được vào khu vực quản trị và dùng client quản lý kho. */
    public boolean laNhanSu() {
        return VaiTro.ADMIN.equals(maVaiTro) || VaiTro.NHANVIEN.equals(maVaiTro);
    }

    @Override
    public boolean equals(Object khac) {
        return khac instanceof TaiKhoanDetails that && tenDangNhap.equals(that.tenDangNhap);
    }

    @Override
    public int hashCode() {
        return tenDangNhap.hashCode();
    }

    @Override
    public String toString() {
        return "TaiKhoanDetails[" + tenDangNhap + ", " + maVaiTro + "]";
    }
}
