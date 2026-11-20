package com.xemay.repository;

import com.xemay.entity.XeMay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository tối thiểu cho phần tồn kho. Thành viên phụ trách quản lý xe máy bổ sung
 * thêm các truy vấn tìm kiếm, lọc theo hãng... vào đây.
 */
public interface XeMayRepository extends JpaRepository<XeMay, Long> {

    /** Các xe có số lượng tồn nhỏ hơn hoặc bằng ngưỡng cảnh báo, xe ít hàng nhất đứng trước. */
    List<XeMay> findBySoLuongTonLessThanEqualOrderBySoLuongTonAscTenXeAsc(int nguong);

    /** Tìm xe theo tên (không phân biệt hoa thường), tối đa 50 kết quả. */
    List<XeMay> findTop50ByTenXeContainingIgnoreCaseOrderByTenXeAsc(String tuKhoa);
}
