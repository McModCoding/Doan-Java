package com.xemay.dto;

import java.math.BigDecimal;

/** DTO chỉ dùng hiển thị, không trực tiếp lưu dữ liệu vào database. */
public record SanPhamHienThi(
        Integer id,
        String tenXe,
        String hangXe,
        String loaiXe,
        BigDecimal giaBan,
        int soLuongTon,
        String moTa,
        String anh
) {
    public boolean conHang() { return soLuongTon > 0; }
}
