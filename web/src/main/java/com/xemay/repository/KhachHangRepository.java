package com.xemay.repository;

import com.xemay.entity.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository khách hàng - dựa trên {@code CustomerRepository} của thành viên phụ trách khách hàng
 * (câu tìm theo họ tên hoặc số điện thoại), viết lại cho bảng {@code KHACH_HANG}.
 */
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    /**
     * Tìm khách hàng theo họ tên hoặc số điện thoại (không phân biệt hoa thường), có phân trang.
     *
     * @param mau mẫu LIKE chữ thường, ví dụ {@code %nguyen%}; {@code %} để lấy tất cả
     */
    @EntityGraph(attributePaths = "taiKhoan")
    @Query("""
        SELECT k FROM KhachHang k
        WHERE LOWER(k.hoTen) LIKE :mau OR k.soDienThoai LIKE :mau
    """)
    Page<KhachHang> timKiem(@Param("mau") String mau, Pageable pageable);

    boolean existsBySoDienThoai(String soDienThoai);

    /** Số điện thoại đã thuộc về một khách hàng KHÁC chưa (dùng khi sửa). */
    boolean existsBySoDienThoaiAndMaKhachHangNot(String soDienThoai, Integer maKhachHang);

    Optional<KhachHang> findByTaiKhoan_Id(Integer maTaiKhoan);

    /** Gỡ liên kết hồ sơ với tài khoản sắp bị xóa: hồ sơ (và đơn hàng của khách) vẫn được giữ lại. */
    @Modifying
    @Query("UPDATE KhachHang k SET k.taiKhoan = null WHERE k.taiKhoan.id = :maTaiKhoan")
    int goLienKetTaiKhoan(@Param("maTaiKhoan") Integer maTaiKhoan);
}
