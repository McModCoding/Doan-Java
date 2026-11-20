package com.xemay.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Dữ liệu của form người dùng tự đổi mật khẩu. */
public class DoiMatKhauForm {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String matKhauHienTai;

    @NotNull(message = QuyTacNhap.MAT_KHAU_SAI)
    @Pattern(regexp = QuyTacNhap.MAT_KHAU, message = QuyTacNhap.MAT_KHAU_SAI)
    private String matKhauMoi;

    private String nhapLaiMatKhauMoi;

    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isMatKhauKhop() {
        return matKhauMoi == null || matKhauMoi.equals(nhapLaiMatKhauMoi);
    }

    public String getMatKhauHienTai() {
        return matKhauHienTai;
    }

    public void setMatKhauHienTai(String matKhauHienTai) {
        this.matKhauHienTai = matKhauHienTai;
    }

    public String getMatKhauMoi() {
        return matKhauMoi;
    }

    public void setMatKhauMoi(String matKhauMoi) {
        this.matKhauMoi = matKhauMoi;
    }

    public String getNhapLaiMatKhauMoi() {
        return nhapLaiMatKhauMoi;
    }

    public void setNhapLaiMatKhauMoi(String nhapLaiMatKhauMoi) {
        this.nhapLaiMatKhauMoi = nhapLaiMatKhauMoi;
    }
}
