package com.xemay.repository;

import com.xemay.entity.HangXe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository hãng xe - phần của thành viên phụ trách xe máy, hãng xe (chuyển package khi tích hợp). */
@Repository
public interface HangXeRepository extends JpaRepository<HangXe, Integer> {

    // 1. Tìm kiếm hãng xe theo tên gần đúng
    List<HangXe> findByTenHangContainingIgnoreCase(String tenHang);

    // 2. Lấy danh sách hãng xe đang hoạt động
    List<HangXe> findByTrangThaiTrue();

    // 3. Kiểm tra tên hãng xe đã tồn tại hay chưa
    boolean existsByTenHangIgnoreCase(String tenHang);

    // 4. Kiểm tra tên hãng xe đã tồn tại, ngoại trừ một mã hãng
    // Dùng khi cập nhật hãng xe để tránh trùng tên
    boolean existsByTenHangIgnoreCaseAndMaHangNot(
            String tenHang,
            Integer maHang
    );

    // 5. Kiểm tra hãng xe có xe máy tham chiếu hay không
    // Dùng để ngăn xóa hãng xe đang được sử dụng
    @Query("""
        SELECT CASE WHEN COUNT(x) > 0 THEN true ELSE false END
        FROM XeMay x
        WHERE x.hangXe.maHang = :maHang
    """)
    boolean existsXeByMaHang(@Param("maHang") Integer maHang);

    // 6. Tìm hãng xe theo tên chính xác
    boolean existsByTenHang(String tenHang);

    /** Tích hợp: số mẫu xe của từng hãng, mỗi phần tử là {mã hãng, số xe} - hiển thị trên trang danh sách hãng. */
    @Query("SELECT x.hangXe.maHang, COUNT(x) FROM XeMay x GROUP BY x.hangXe.maHang")
    List<Object[]> demXeTheoHang();
}
