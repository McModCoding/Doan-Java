package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Nhật ký mỗi lần đăng nhập (thành công lẫn thất bại), từ trang web hoặc từ client Socket.
 *
 * <p>Bản ghi được lưu bằng một tác vụ nền ({@code @Async}) nên không làm chậm việc đăng nhập.
 */
@Entity
@Table(name = "LICH_SU_DANG_NHAP", indexes = {
        @Index(name = "IX_LICHSU_THOIGIAN", columnList = "ThoiGian"),
        @Index(name = "IX_LICHSU_TENDANGNHAP", columnList = "TenDangNhap")
})
public class LichSuDangNhap {

    public static final String KENH_WEB = "WEB";
    public static final String KENH_SOCKET = "SOCKET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLichSu")
    private Long id;

    /** Tên đăng nhập người dùng đã gõ - có thể là tên không tồn tại trong hệ thống. */
    @Nationalized
    @Column(name = "TenDangNhap", nullable = false, length = 100)
    private String tenDangNhap;

    /** Tài khoản tương ứng; null nếu tên đăng nhập không tồn tại hoặc tài khoản đã bị xóa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", foreignKey = @ForeignKey(name = "FK_LICHSU_TAIKHOAN"))
    @OnDelete(action = OnDeleteAction.SET_NULL)   // xóa tài khoản thì giữ nhật ký, chỉ bỏ liên kết
    private TaiKhoan taiKhoan;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime thoiGian;

    @Column(name = "DiaChiIp", length = 45)
    private String diaChiIp;

    /** Nơi đăng nhập: {@link #KENH_WEB} hoặc {@link #KENH_SOCKET}. */
    @Column(name = "Kenh", nullable = false, length = 10)
    private String kenh;

    @Column(name = "ThanhCong", nullable = false)
    private boolean thanhCong;

    /** Lý do thất bại (sai mật khẩu, tài khoản bị khóa...). */
    @Nationalized
    @Column(name = "GhiChu", length = 255)
    private String ghiChu;

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

    public TaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public void setTaiKhoan(TaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
    }

    public LocalDateTime getThoiGian() {
        return thoiGian;
    }

    public void setThoiGian(LocalDateTime thoiGian) {
        this.thoiGian = thoiGian;
    }

    public String getDiaChiIp() {
        return diaChiIp;
    }

    public void setDiaChiIp(String diaChiIp) {
        this.diaChiIp = diaChiIp;
    }

    public String getKenh() {
        return kenh;
    }

    public void setKenh(String kenh) {
        this.kenh = kenh;
    }

    public boolean isThanhCong() {
        return thanhCong;
    }

    public void setThanhCong(boolean thanhCong) {
        this.thanhCong = thanhCong;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}
