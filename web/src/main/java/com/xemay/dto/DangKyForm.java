package com.xemay.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dữ liệu của form khách hàng tự đăng ký tài khoản. */
public class DangKyForm {

    @NotNull(message = QuyTacNhap.TEN_DANG_NHAP_SAI)
    @Pattern(regexp = QuyTacNhap.TEN_DANG_NHAP, message = QuyTacNhap.TEN_DANG_NHAP_SAI)
    private String tenDangNhap;

    @NotNull(message = QuyTacNhap.MAT_KHAU_SAI)
    @Pattern(regexp = QuyTacNhap.MAT_KHAU, message = QuyTacNhap.MAT_KHAU_SAI)
    private String matKhau;

    private String nhapLaiMatKhau;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String hoTen;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    /** Bắt buộc khi đăng ký: hồ sơ khách hàng (bảng KHACH_HANG) cần số điện thoại để liên hệ giao xe. */
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = QuyTacNhap.SO_DIEN_THOAI, message = QuyTacNhap.SO_DIEN_THOAI_SAI)
    private String soDienThoai;

    /** Kiểm tra chéo hai ô mật khẩu; lỗi được gắn vào "thuộc tính" matKhauKhop để hiển thị dưới ô nhập lại. */
    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isMatKhauKhop() {
        return matKhau == null || matKhau.equals(nhapLaiMatKhau);
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

    public String getNhapLaiMatKhau() {
        return nhapLaiMatKhau;
    }

    public void setNhapLaiMatKhau(String nhapLaiMatKhau) {
        this.nhapLaiMatKhau = nhapLaiMatKhau;
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
}
