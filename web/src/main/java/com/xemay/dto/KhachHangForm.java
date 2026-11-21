package com.xemay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Dữ liệu form thêm / sửa khách hàng trong trang quản trị.
 *
 * <p>Tích hợp: bản gốc ({@code CustomerController.saveCustomer}) gắn thẳng Entity vào form và tự kiểm tra
 * "họ tên, số điện thoại không được trống" bằng if. Ở đây dùng form riêng với {@code @Valid} như các form khác
 * của dự án, và kiểm tra thêm định dạng số điện thoại, email, ngày sinh.
 */
public class KhachHangForm {

    /** null khi thêm mới. */
    private Integer maKhachHang;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String hoTen;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = QuyTacNhap.SO_DIEN_THOAI, message = QuyTacNhap.SO_DIEN_THOAI_SAI)
    private String soDienThoai;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String diaChi;

    @Past(message = "Ngày sinh phải trước ngày hôm nay")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngaySinh;

    /** Chỉ để hiển thị: tên đăng nhập của tài khoản web gắn với hồ sơ (null nếu là khách mua tại cửa hàng). */
    private String tenDangNhap;

    public boolean laThem() {
        return maKhachHang == null;
    }

    public boolean coTaiKhoan() {
        return tenDangNhap != null;
    }

    public Integer getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(Integer maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }
}
