package com.xemay.repository;

import com.xemay.entity.TaiKhoan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    boolean existsByEmail(String email);

    /** Email đã được một tài khoản KHÁC dùng chưa (dùng khi sửa thông tin). */
    boolean existsByEmailAndIdNot(String email, Integer id);

    long countByVaiTro_Ma(String maVaiTro);

    /**
     * Tìm tài khoản theo từ khóa (tên đăng nhập, họ tên hoặc email) và lọc theo vai trò.
     *
     * @param mau      mẫu LIKE ở dạng chữ thường, ví dụ {@code %nguyen%}; truyền {@code %} để lấy tất cả
     * @param vaiTroId mã vai trò cần lọc, {@code null} nếu không lọc
     */
    @Query("""
            select t from TaiKhoan t
            where (lower(t.tenDangNhap) like :mau or lower(t.hoTen) like :mau or lower(t.email) like :mau)
              and (:vaiTroId is null or t.vaiTro.id = :vaiTroId)
            """)
    Page<TaiKhoan> timKiem(@Param("mau") String mau, @Param("vaiTroId") Integer vaiTroId, Pageable pageable);
}
