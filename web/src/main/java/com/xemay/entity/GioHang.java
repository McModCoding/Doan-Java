package com.xemay.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Giỏ hàng (bảng {@code GIO_HANG}) - phần của thành viên phụ trách giỏ hàng, đặt hàng.
 *
 * <p>Mỗi khách hàng có đúng một giỏ (cột {@code MaKhachHang} là UNIQUE). Đặt hàng xong thì các dòng trong giỏ
 * được xóa, bản thân giỏ giữ lại để dùng tiếp.
 */
@Entity
@Table(name = "GIO_HANG")
public class GioHang {

    public static final String DANG_MUA = "DANG_MUA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGioHang")
    private Integer maGioHang;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKhachHang", nullable = false, unique = true)
    private KhachHang khachHang;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = DANG_MUA;

    /** Quan hệ một - nhiều: một giỏ có nhiều dòng xe. */
    @OneToMany(mappedBy = "gioHang", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maChiTietGioHang")
    private List<ChiTietGioHang> dsChiTiet = new ArrayList<>();

    public GioHang() {
    }

    public GioHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    @PrePersist
    void ganNgayTao() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }

    public Integer getMaGioHang() {
        return maGioHang;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public List<ChiTietGioHang> getDsChiTiet() {
        return dsChiTiet;
    }
}
