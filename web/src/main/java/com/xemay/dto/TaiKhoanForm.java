package com.xemay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu của form quản trị viên THÊM hoặc SỬA tài khoản.
 *
 * <p>Tên đăng nhập và mật khẩu chỉ được nhập khi thêm mới, nên hai ràng buộc đó thuộc nhóm {@link KhiThem}.
 * Controller dùng {@code @Validated({Default.class, KhiThem.class})} khi thêm và {@code @Valid} khi sửa.
 */
public class TaiKhoanForm {

    /** Nhóm kiểm tra chỉ áp dụng khi thêm tài khoản mới. */
    public interface KhiThem {
    }

    private Long id;

    @NotNull(message = QuyTacNhap.TEN_DANG_NHAP_SAI, groups = KhiThem.class)
    @Pattern(regexp = QuyTacNhap.TEN_DANG_NHAP, message = QuyTacNhap.TEN_DANG_NHAP_SAI, groups = KhiThem.class)
    private String tenDangNhap;

    @NotNull(message = QuyTacNhap.MAT_KHAU_SAI, groups = KhiThem.class)
    @Pattern(regexp = QuyTacNhap.MAT_KHAU, message = QuyTacNhap.MAT_KHAU_SAI, groups = KhiThem.class)
    private String matKhau;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String hoTen;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Pattern(regexp = QuyTacNhap.SO_DIEN_THOAI, message = QuyTacNhap.SO_DIEN_THOAI_SAI)
    private String soDienThoai;

    @NotNull(message = "Vui lòng chọn vai trò")
    private Integer vaiTroId;

    private boolean kichHoat = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public Integer getVaiTroId() {
        return vaiTroId;
    }

    public void setVaiTroId(Integer vaiTroId) {
        this.vaiTroId = vaiTroId;
    }

    public boolean isKichHoat() {
        return kichHoat;
    }

    public void setKichHoat(boolean kichHoat) {
        this.kichHoat = kichHoat;
    }
}
