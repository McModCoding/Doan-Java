package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * Xe máy - KHUNG TỐI THIỂU để phần tồn kho (xử lý đồng thời) và Socket server chạy được.
 *
 * <p><b>Thành viên phụ trách quản lý xe máy</b> bổ sung các cột còn lại (hãng xe, mô tả, hình ảnh,
 * màu sắc, dung tích...). Khi bổ sung xin GIỮ NGUYÊN 4 thuộc tính bên dưới vì
 * {@code TonKhoService} và Socket server đang dùng chúng.
 *
 * <p><b>Quy ước quan trọng:</b> chỉ thay đổi {@code soLuongTon} thông qua {@code TonKhoService}
 * ({@code xuatKho}/{@code nhapKho}). Form "sửa xe" KHÔNG được ghi đè số tồn kho, nếu không sẽ làm
 * mất các lần trừ kho diễn ra cùng lúc.
 */
@Entity
@Table(name = "xe_may")
public class XeMay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_xe", nullable = false, length = 150)
    private String tenXe;

    @Column(name = "gia_ban", nullable = false, precision = 18, scale = 0)
    private BigDecimal giaBan;

    /** Tài nguyên dùng chung của phần xử lý đồng thời: nhiều giao dịch có thể cùng trừ/cộng số này. */
    @Min(value = 0, message = "Số lượng tồn không được âm")
    @Column(name = "so_luong_ton", nullable = false)
    private int soLuongTon;

    public XeMay() {
    }

    public XeMay(String tenXe, BigDecimal giaBan, int soLuongTon) {
        this.tenXe = tenXe;
        this.giaBan = giaBan;
        this.soLuongTon = soLuongTon;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenXe() {
        return tenXe;
    }

    public void setTenXe(String tenXe) {
        this.tenXe = tenXe;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public int getSoLuongTon() {
        return soLuongTon;
    }

    public void setSoLuongTon(int soLuongTon) {
        this.soLuongTon = soLuongTon;
    }
}
