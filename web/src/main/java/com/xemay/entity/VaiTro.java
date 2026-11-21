package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Nationalized;

import java.util.ArrayList;
import java.util.List;

/**
 * Vai trò của người dùng. Hệ thống có 3 vai trò cố định: quản trị viên, nhân viên, khách hàng.
 *
 * <p>Cột {@code TenVaiTro} (thuộc tính {@code ma}) được dùng làm tên quyền trong Spring Security
 * (thêm tiền tố {@code ROLE_}), ví dụ {@code ADMIN} tương ứng với {@code hasRole('ADMIN')}.
 * Cột {@code MoTa} (thuộc tính {@code ten}) là tên vai trò hiển thị trên giao diện.
 */
@Entity
@Table(name = "VAI_TRO",
        uniqueConstraints = @UniqueConstraint(name = "UQ_VAITRO_TENVAITRO", columnNames = "TenVaiTro"))
public class VaiTro {

    public static final String ADMIN = "ADMIN";
    public static final String NHANVIEN = "NHANVIEN";
    public static final String KHACHHANG = "KHACHHANG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaVaiTro")
    private Integer id;

    @Column(name = "TenVaiTro", nullable = false, length = 30)
    private String ma;

    @Nationalized
    @Column(name = "MoTa", length = 255)
    private String ten;

    /** Quan hệ một - nhiều: một vai trò có nhiều tài khoản. */
    @OneToMany(mappedBy = "vaiTro")
    private List<TaiKhoan> danhSachTaiKhoan = new ArrayList<>();

    public VaiTro() {
    }

    public VaiTro(String ma, String ten) {
        this.ma = ma;
        this.ten = ten;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public List<TaiKhoan> getDanhSachTaiKhoan() {
        return danhSachTaiKhoan;
    }

    public void setDanhSachTaiKhoan(List<TaiKhoan> danhSachTaiKhoan) {
        this.danhSachTaiKhoan = danhSachTaiKhoan;
    }
}
