package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Một dòng trong giỏ hàng (bảng {@code CHI_TIET_GIO_HANG}): một mẫu xe và số lượng khách muốn mua.
 * {@code DonGia} là giá bán lúc thêm vào giỏ; khi đặt hàng, giá được lấy lại từ bảng {@code XE_MAY}.
 */
@Entity
@Table(name = "CHI_TIET_GIO_HANG",
        uniqueConstraints = @UniqueConstraint(name = "UQ_CTG_GIO_XE", columnNames = {"MaGioHang", "MaXe"}))
public class ChiTietGioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietGioHang")
    private Integer maChiTietGioHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaGioHang", nullable = false)
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaXe", nullable = false)
    private XeMay xeMay;

    @Column(name = "SoLuong", nullable = false)
    private int soLuong;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 2)
    private BigDecimal donGia;

    public ChiTietGioHang() {
    }

    public ChiTietGioHang(GioHang gioHang, XeMay xeMay, int soLuong, BigDecimal donGia) {
        this.gioHang = gioHang;
        this.xeMay = xeMay;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public Integer getMaChiTietGioHang() {
        return maChiTietGioHang;
    }

    public GioHang getGioHang() {
        return gioHang;
    }

    public XeMay getXeMay() {
        return xeMay;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }
}
