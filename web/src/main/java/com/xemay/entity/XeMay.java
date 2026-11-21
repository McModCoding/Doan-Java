package com.xemay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Xe máy. Phần của thành viên phụ trách quản lý xe máy, hãng xe.
 *
 * <p>Tích hợp: chuyển từ package {@code com.example.quanlyxemay.entity}, bỏ {@code schema = "dbo"},
 * thêm {@link #ganNgayTao()} và hàm {@link #getTenDayDu()}. Entity tạm của phần tồn kho trước đây
 * (bảng {@code xe_may} chỉ có 4 cột) được thay hẳn bằng lớp này.
 *
 * <p><b>Quy ước quan trọng:</b> cột {@code SoLuong} là tài nguyên dùng chung của phần xử lý đồng thời.
 * Chỉ thay đổi nó qua {@code TonKhoService} ({@code xuatKho}/{@code nhapKho}, có khóa dòng). Form sửa xe
 * KHÔNG ghi đè số lượng, nếu không sẽ làm mất các lần trừ kho diễn ra cùng lúc.
 *
 * <p>{@code @DynamicUpdate}: câu UPDATE chỉ chứa các cột THỰC SỰ thay đổi. Mặc định Hibernate ghi lại MỌI cột,
 * kể cả {@code SoLuong} đọc được từ lúc mở form sửa; nếu trong lúc đó có khách mua xe thì con số cũ này
 * ghi đè lên số tồn mới (mất cập nhật) dù Service không hề gọi {@code setSoLuong}. Có kiểm thử chứng minh
 * trong {@code QuanLyXeMayTest}.
 */
@Entity
@Table(name = "XE_MAY")
@DynamicUpdate
public class XeMay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaXe")
    private Integer maXe;

    // Nhiều xe máy thuộc một hãng xe
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaHang", nullable = false)
    private HangXe hangXe;

    @Nationalized
    @Column(name = "TenXe", nullable = false, length = 150)
    private String tenXe;

    @Nationalized
    @Column(name = "LoaiXe", nullable = false, length = 50)
    private String loaiXe;

    @Column(name = "PhanKhoi")
    private Integer phanKhoi;

    @Nationalized
    @Column(name = "MauSac", length = 50)
    private String mauSac;

    @Column(name = "GiaBan", nullable = false, precision = 18, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong = 0;

    @Nationalized
    @Column(name = "HinhAnh", length = 500)
    private String hinhAnh;

    @Nationalized
    @Column(name = "MoTa", length = 1000)
    private String moTa;

    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    // Constructor rỗng
    public XeMay() {
    }

    public XeMay(HangXe hangXe, String tenXe, String loaiXe, BigDecimal giaBan, int soLuong) {
        this.hangXe = hangXe;
        this.tenXe = tenXe;
        this.loaiXe = loaiXe;
        this.giaBan = giaBan;
        this.soLuong = soLuong;
    }

    /** Bảo đảm cột NgayTao (NOT NULL) luôn có giá trị, kể cả khi xe được tạo ngoài XeMayService. */
    @PrePersist
    void ganNgayTao() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }

    /** Tên kèm hãng, ví dụ "Honda Vision" - dùng ở nơi không có cột hãng riêng (client kho, cảnh báo tồn kho). */
    public String getTenDayDu() {
        return hangXe == null ? tenXe : hangXe.getTenHang() + " " + tenXe;
    }

    // Getter và Setter
    public Integer getMaXe() {
        return maXe;
    }

    public void setMaXe(Integer maXe) {
        this.maXe = maXe;
    }

    public HangXe getHangXe() {
        return hangXe;
    }

    public void setHangXe(HangXe hangXe) {
        this.hangXe = hangXe;
    }

    public String getTenXe() {
        return tenXe;
    }

    public void setTenXe(String tenXe) {
        this.tenXe = tenXe;
    }

    public String getLoaiXe() {
        return loaiXe;
    }

    public void setLoaiXe(String loaiXe) {
        this.loaiXe = loaiXe;
    }

    public Integer getPhanKhoi() {
        return phanKhoi;
    }

    public void setPhanKhoi(Integer phanKhoi) {
        this.phanKhoi = phanKhoi;
    }

    public String getMauSac() {
        return mauSac;
    }

    public void setMauSac(String mauSac) {
        this.mauSac = mauSac;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
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

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
