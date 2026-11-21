package com.xemay.dto;

import java.math.BigDecimal;

/**
 * Điều kiện tìm kiếm, lọc trên trang danh sách xe máy (tham số trên URL, ví dụ {@code ?tenXe=vision&maHang=1}).
 *
 * <p>Gom vào một đối tượng để Spring gắn dữ liệu kèm {@code BindingResult}: người dùng sửa URL thành
 * {@code giaMin=abc} thì chỉ bỏ qua điều kiện đó và báo lỗi nhẹ, không văng trang lỗi 400.
 */
public class BoLocXeMay {

    private String tenXe;
    private Integer maHang;
    private String loaiXe;
    private BigDecimal giaMin;
    private BigDecimal giaMax;
    /** null: tất cả, true: đang bán, false: ngừng kinh doanh. */
    private Boolean trangThai;

    public boolean dangLoc() {
        return (tenXe != null && !tenXe.isBlank()) || maHang != null || (loaiXe != null && !loaiXe.isBlank())
                || giaMin != null || giaMax != null || trangThai != null;
    }

    public String getTenXe() {
        return tenXe;
    }

    public void setTenXe(String tenXe) {
        this.tenXe = tenXe;
    }

    public Integer getMaHang() {
        return maHang;
    }

    public void setMaHang(Integer maHang) {
        this.maHang = maHang;
    }

    public String getLoaiXe() {
        return loaiXe;
    }

    public void setLoaiXe(String loaiXe) {
        this.loaiXe = loaiXe;
    }

    public BigDecimal getGiaMin() {
        return giaMin;
    }

    public void setGiaMin(BigDecimal giaMin) {
        this.giaMin = giaMin;
    }

    public BigDecimal getGiaMax() {
        return giaMax;
    }

    public void setGiaMax(BigDecimal giaMax) {
        this.giaMax = giaMax;
    }

    public Boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Boolean trangThai) {
        this.trangThai = trangThai;
    }
}
