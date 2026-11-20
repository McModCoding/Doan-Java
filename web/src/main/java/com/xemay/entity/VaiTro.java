package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Vai trò của người dùng. Hệ thống có 3 vai trò cố định: quản trị viên, nhân viên, khách hàng.
 *
 * <p>Cột {@code ma} được dùng làm tên quyền trong Spring Security (thêm tiền tố {@code ROLE_}),
 * ví dụ mã {@code ADMIN} tương ứng với {@code hasRole('ADMIN')}.
 */
@Entity
@Table(name = "vai_tro",
        uniqueConstraints = @UniqueConstraint(name = "UQ_vai_tro_ma", columnNames = "ma"))
public class VaiTro {

    public static final String ADMIN = "ADMIN";
    public static final String NHANVIEN = "NHANVIEN";
    public static final String KHACHHANG = "KHACHHANG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", nullable = false, length = 20)
    private String ma;

    @Column(name = "ten", nullable = false, length = 50)
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
