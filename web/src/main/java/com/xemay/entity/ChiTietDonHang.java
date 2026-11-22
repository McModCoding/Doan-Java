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
 * Một dòng của đơn hàng (bảng {@code CHI_TIET_DON_HANG}). Cột {@code ThanhTien} do SQL Server tự tính
 * ({@code SoLuong * DonGia}, cột PERSISTED) nên ứng dụng chỉ đọc, không ghi.
 */
@Entity
@Table(name = "CHI_TIET_DON_HANG",
        uniqueConstraints = @UniqueConstraint(name = "UQ_CTDH_DON_XE", columnNames = {"MaDonHang", "MaXe"}))
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietDonHang")
    private Integer maChiTietDonHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaXe", nullable = false)
    private XeMay xeMay;

    @Column(name = "SoLuong", nullable = false)
    private int soLuong;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 2)
    private BigDecimal donGia;

    @Column(name = "ThanhTien", precision = 18, scale = 2, insertable = false, updatable = false)
    private BigDecimal thanhTien;

    public ChiTietDonHang() {
    }

    public ChiTietDonHang(DonHang donHang, XeMay xeMay, int soLuong, BigDecimal donGia) {
        this.donHang = donHang;
        this.xeMay = xeMay;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public Integer getMaChiTietDonHang() {
        return maChiTietDonHang;
    }

    public DonHang getDonHang() {
        return donHang;
    }

    public XeMay getXeMay() {
        return xeMay;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    /** Thành tiền của dòng; tính lại từ số lượng và đơn giá nên dùng được cả trước khi lưu. */
    public BigDecimal getThanhTien() {
        return thanhTien != null ? thanhTien : donGia.multiply(BigDecimal.valueOf(soLuong));
    }
}
