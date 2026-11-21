package com.xemay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dữ liệu form thêm / sửa xe máy - phần của thành viên phụ trách xe máy (chuyển package khi tích hợp).
 *
 * <p>{@code soLuong} chỉ có tác dụng khi THÊM xe (số xe có sẵn lúc nhập mẫu xe mới). Khi sửa, số lượng
 * không lấy từ form: muốn tăng tồn kho thì dùng chức năng Nhập kho (qua {@code TonKhoService}).
 */
public class XeMayDTO {

    private Integer maXe;

    @NotNull(message = "Vui lòng chọn hãng xe")
    private Integer maHang;

    @NotBlank(message = "Tên xe không được để trống")
    @Size(max = 150, message = "Tên xe không được vượt quá 150 ký tự")
    private String tenXe;

    @NotBlank(message = "Vui lòng chọn loại xe")
    @Size(max = 50, message = "Loại xe không được vượt quá 50 ký tự")
    private String loaiXe;

    @Min(value = 1, message = "Phân khối phải lớn hơn 0")
    private Integer phanKhoi;

    @Size(max = 50, message = "Màu sắc không được vượt quá 50 ký tự")
    private String mauSac;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
    private BigDecimal giaBan;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer soLuong;

    @Size(max = 500, message = "Đường dẫn hình ảnh không được vượt quá 500 ký tự")
    private String hinhAnh;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String moTa;

    private Boolean trangThai = true;

    public XeMayDTO() {
    }

    public Integer getMaXe() {
        return maXe;
    }

    public void setMaXe(Integer maXe) {
        this.maXe = maXe;
    }

    public Integer getMaHang() {
        return maHang;
    }

    public void setMaHang(Integer maHang) {
        this.maHang = maHang;
    }

    public String getTenXe() {
        return tenXe;
    }

    public void setTenXe(String tenXe) {
        this.tenXe = tenXe;
    }

    public String getLoaiXe() {
        return loaiXe;
    }

    public void setLoaiXe(String loaiXe) {
        this.loaiXe = loaiXe;
    }

    public Integer getPhanKhoi() {
        return phanKhoi;
    }

    public void setPhanKhoi(Integer phanKhoi) {
        this.phanKhoi = phanKhoi;
    }

    public String getMauSac() {
        return mauSac;
    }

    public void setMauSac(String mauSac) {
        this.mauSac = mauSac;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Boolean trangThai) {
        this.trangThai = trangThai;
    }
}
