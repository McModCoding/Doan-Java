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
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Tài khoản đăng nhập của mọi người dùng (quản trị viên, nhân viên, khách hàng).
 *
 * <p>Bảng {@code TAI_KHOAN} chỉ giữ thông tin phục vụ ĐĂNG NHẬP và PHÂN QUYỀN. Thông tin riêng của khách hàng
 * (địa chỉ giao hàng, ngày sinh...) nằm ở bảng {@code KHACH_HANG} và tham chiếu về đây qua cột {@code MaTaiKhoan}.
 *
 * <p>Tên bảng, tên cột theo script chung {@code database/QL_CUAHANG_XEMAY.sql}. Ứng dụng dùng chiến lược đặt tên
 * "giữ nguyên" nên MỖI thuộc tính phải ghi rõ {@code @Column(name = ...)}.
 */
@Entity
@Table(name = "TAI_KHOAN", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_TAIKHOAN_TENDANGNHAP", columnNames = "TenDangNhap"),
        @UniqueConstraint(name = "UQ_TAIKHOAN_EMAIL", columnNames = "Email")
})
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTaiKhoan")
    private Integer id;

    /** Luôn lưu ở dạng chữ thường để việc so sánh giống nhau trên mọi hệ quản trị CSDL. */
    @Column(name = "TenDangNhap", nullable = false, length = 50)
    private String tenDangNhap;

    /** Mật khẩu đã băm bằng BCrypt (60 ký tự). KHÔNG bao giờ lưu mật khẩu gốc. */
    @Column(name = "MatKhau", nullable = false, length = 255)
    private String matKhau;

    @Nationalized
    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "Email", nullable = false, length = 100)
    private String email;

    @Column(name = "SoDienThoai", length = 15)
    private String soDienThoai;

    /** Cột TrangThai: false = tài khoản bị quản trị viên khóa, không đăng nhập được. */
    @Column(name = "TrangThai", nullable = false)
    private boolean kichHoat = true;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    /** Quan hệ nhiều - một: mỗi tài khoản có đúng một vai trò. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MaVaiTro", nullable = false, foreignKey = @ForeignKey(name = "FK_TAIKHOAN_VAITRO"))
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
