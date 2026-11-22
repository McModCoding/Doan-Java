package com.xemay.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Đơn hàng (bảng {@code DON_HANG}) tạo ra khi khách đặt mua các xe trong giỏ. */
@Entity
@Table(name = "DON_HANG")
public class DonHang {

    public static final String CHO_XAC_NHAN = "CHO_XAC_NHAN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDonHang")
    private Integer maDonHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKhachHang", nullable = false)
    private KhachHang khachHang;

    @Column(name = "NgayDat", nullable = false, updatable = false)
    private LocalDateTime ngayDat;

    @Column(name = "TongTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal tongTien = BigDecimal.ZERO;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = CHO_XAC_NHAN;

    @Nationalized
    @Column(name = "DiaChiGiaoHang", nullable = false, length = 255)
    private String diaChiGiaoHang;

    @Nationalized
    @Column(name = "GhiChu", length = 500)
    private String ghiChu;

    /** Quan hệ một - nhiều: một đơn có nhiều dòng xe. */
    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietDonHang> dsChiTiet = new ArrayList<>();

    @PrePersist
    void ganNgayDat() {
        if (ngayDat == null) {
            ngayDat = LocalDateTime.now();
        }
    }

    public void themDong(XeMay xeMay, int soLuong, BigDecimal donGia) {
        dsChiTiet.add(new ChiTietDonHang(this, xeMay, soLuong, donGia));
        tongTien = tongTien.add(donGia.multiply(BigDecimal.valueOf(soLuong)));
    }

    public Integer getMaDonHang() {
        return maDonHang;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public LocalDateTime getNgayDat() {
        return ngayDat;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getDiaChiGiaoHang() {
        return diaChiGiaoHang;
    }

    public void setDiaChiGiaoHang(String diaChiGiaoHang) {
        this.diaChiGiaoHang = diaChiGiaoHang;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public List<ChiTietDonHang> getDsChiTiet() {
        return dsChiTiet;
    }
}
