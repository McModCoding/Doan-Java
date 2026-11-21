package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hồ sơ khách hàng (bảng {@code KHACH_HANG}). Phần của thành viên phụ trách quản lý khách hàng.
 *
 * <p>Tích hợp: lớp {@code Customer} gốc ánh xạ tới bảng {@code Customers} (tên tiếng Anh, khác script chung),
 * nên được viết lại theo bảng {@code KHACH_HANG} của script {@code QL_CUAHANG_XEMAY.sql}:
 * fullName -> hoTen, phone -> soDienThoai, address -> diaChi; thêm ngaySinh, ngayTao và liên kết tài khoản.
 *
 * <p>Hai loại khách hàng:
 * <ul>
 *   <li>Khách tự đăng ký trên web: {@link #taiKhoan} trỏ tới tài khoản đăng nhập (mỗi tài khoản tối đa 1 hồ sơ).</li>
 *   <li>Khách mua tại cửa hàng, do nhân viên nhập: {@link #taiKhoan} là {@code null}.</li>
 * </ul>
 */
@Entity
@Table(name = "KHACH_HANG")
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKhachHang")
    private Integer maKhachHang;

    /** Tài khoản web của khách; null nếu là khách mua tại cửa hàng. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", unique = true)
    private TaiKhoan taiKhoan;

    @Nationalized
    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "SoDienThoai", nullable = false, length = 15, unique = true)
    private String soDienThoai;

    @Column(name = "Email", length = 100)
    private String email;

    @Nationalized
    @Column(name = "DiaChi", length = 255)
    private String diaChi;

    @Column(name = "NgaySinh")
    private LocalDate ngaySinh;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    public KhachHang() {
    }

    @PrePersist
    void ganNgayTao() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }

    /** Khách đã có tài khoản đăng nhập web chưa. */
    public boolean coTaiKhoan() {
        return taiKhoan != null;
    }

    public Integer getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(Integer maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public TaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public void setTaiKhoan(TaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
