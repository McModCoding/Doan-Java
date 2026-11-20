package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Tài khoản đăng nhập của mọi người dùng (quản trị viên, nhân viên, khách hàng).
 *
 * <p>Bảng này chỉ giữ thông tin phục vụ ĐĂNG NHẬP và PHÂN QUYỀN. Thông tin riêng của khách hàng
 * (địa chỉ giao hàng, ngày sinh...) nằm ở bảng khách hàng và tham chiếu về đây qua {@code tai_khoan_id}.
 */
@Entity
// Tên ràng buộc đặt giống hệt trong script database/01_tai_khoan_phan_quyen.sql
@Table(name = "tai_khoan", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_tai_khoan_ten_dang_nhap", columnNames = "ten_dang_nhap"),
        @UniqueConstraint(name = "UQ_tai_khoan_email", columnNames = "email")
})
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Luôn lưu ở dạng chữ thường để việc so sánh giống nhau trên mọi hệ quản trị CSDL. */
    @Column(name = "ten_dang_nhap", nullable = false, length = 30)
    private String tenDangNhap;

    /** Mật khẩu đã băm bằng BCrypt (60 ký tự). KHÔNG bao giờ lưu mật khẩu gốc. */
    @Column(name = "mat_khau", nullable = false, length = 100)
    private String matKhau;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "so_dien_thoai", length = 15)
    private String soDienThoai;

    /** false = tài khoản bị quản trị viên khóa, không đăng nhập được. */
    @Column(name = "kich_hoat", nullable = false)
    private boolean kichHoat = true;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    /** Quan hệ nhiều - một: mỗi tài khoản có đúng một vai trò. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "vai_tro_id", nullable = false, foreignKey = @ForeignKey(name = "FK_tai_khoan_vai_tro"))
    private VaiTro vaiTro;

    @PrePersist
    void ganNgayTao() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }

    /**
     * Chuẩn hóa tên đăng nhập trước khi lưu hoặc tra cứu: bỏ khoảng trắng hai đầu và đổi thành chữ thường.
     * Nhờ đó "Admin", " admin " và "admin" đều là cùng một tài khoản.
     */
    public static String chuanHoaTenDangNhap(String tenDangNhap) {
        return tenDangNhap == null ? "" : tenDangNhap.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public boolean isKichHoat() {
        return kichHoat;
    }

    public void setKichHoat(boolean kichHoat) {
        this.kichHoat = kichHoat;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }
}
