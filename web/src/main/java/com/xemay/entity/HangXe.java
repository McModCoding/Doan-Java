package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

import java.util.ArrayList;
import java.util.List;

/**
 * Hãng xe (Honda, Yamaha...). Phần của thành viên phụ trách quản lý xe máy, hãng xe.
 *
 * <p>Tích hợp: chuyển từ package {@code com.example.quanlyxemay.entity}, bỏ {@code schema = "dbo"}
 * (để chạy được cả trên H2 khi kiểm thử).
 */
@Entity
@Table(name = "HANG_XE")
public class HangXe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaHang")
    private Integer maHang;

    @Nationalized
    @Column(name = "TenHang", nullable = false, length = 100, unique = true)
    private String tenHang;

    @Nationalized
    @Column(name = "QuocGia", length = 100)
    private String quocGia;

    @Nationalized
    @Column(name = "MoTa", length = 500)
    private String moTa;

    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai = true;

    // Một hãng xe có nhiều xe máy
    @OneToMany(mappedBy = "hangXe", fetch = FetchType.LAZY)
    private List<XeMay> danhSachXe = new ArrayList<>();

    // Constructor rỗng
    public HangXe() {
    }

    public HangXe(String tenHang, String quocGia) {
        this.tenHang = tenHang;
        this.quocGia = quocGia;
    }

    // Getter và Setter
    public Integer getMaHang() {
        return maHang;
    }

    public void setMaHang(Integer maHang) {
        this.maHang = maHang;
    }

    public String getTenHang() {
        return tenHang;
    }

    public void setTenHang(String tenHang) {
        this.tenHang = tenHang;
    }

    public String getQuocGia() {
        return quocGia;
    }

    public void setQuocGia(String quocGia) {
        this.quocGia = quocGia;
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

    public List<XeMay> getDanhSachXe() {
        return danhSachXe;
    }

    public void setDanhSachXe(List<XeMay> danhSachXe) {
        this.danhSachXe = danhSachXe;
    }
}
