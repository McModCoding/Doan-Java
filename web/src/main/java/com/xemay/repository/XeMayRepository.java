package com.xemay.repository;

import com.xemay.entity.XeMay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository xe máy: gộp các truy vấn của thành viên phụ trách xe máy (tìm kiếm, lọc) với các truy vấn
 * phần tồn kho cần (cảnh báo sắp hết hàng, tra cứu cho client Socket).
 *
 * <p>{@code @EntityGraph(attributePaths = "hangXe")}: đọc luôn hãng xe trong cùng câu SELECT (JOIN), tránh
 * mỗi dòng xe lại chạy thêm một truy vấn lấy hãng, và tránh lỗi LazyInitialization ở luồng Socket
 * (luồng này không có phiên Hibernate mở sẵn như request web).
 */
public interface XeMayRepository extends JpaRepository<XeMay, Integer> {

    // ------------------------------------------------------------------ Quản lý xe máy (thành viên 4)

    // Tìm xe theo tên gần đúng
    List<XeMay> findByTenXeContainingIgnoreCase(String tenXe);

    // Lấy xe theo hãng xe
    List<XeMay> findByHangXe_MaHang(Integer maHang);

    // Lấy xe đang kinh doanh
    List<XeMay> findByTrangThaiTrue();

    long countByTrangThaiTrue();

    /**
     * Tìm kiếm và lọc xe theo nhiều điều kiện, có phân trang. Tham số nào {@code null} thì bỏ qua điều kiện đó.
     * Tích hợp: thêm điều kiện {@code trangThai} (trước đây luôn lọc xe đang bán) để trang quản trị
     * xem được cả xe đã ngừng kinh doanh; trang công khai truyền {@code true}.
     */
    @EntityGraph(attributePaths = "hangXe")
    @Query("""
        SELECT x FROM XeMay x
        WHERE (:tenXe IS NULL OR
               LOWER(x.tenXe) LIKE LOWER(CONCAT('%', :tenXe, '%')))
        AND (:maHang IS NULL OR x.hangXe.maHang = :maHang)
        AND (:loaiXe IS NULL OR x.loaiXe = :loaiXe)
        AND (:giaMin IS NULL OR x.giaBan >= :giaMin)
        AND (:giaMax IS NULL OR x.giaBan <= :giaMax)
        AND (:trangThai IS NULL OR x.trangThai = :trangThai)
    """)
    Page<XeMay> timKiemVaLoc(
            @Param("tenXe") String tenXe,
            @Param("maHang") Integer maHang,
            @Param("loaiXe") String loaiXe,
            @Param("giaMin") BigDecimal giaMin,
            @Param("giaMax") BigDecimal giaMax,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );

    // Lấy danh sách xe thuộc một hãng
    Page<XeMay> findByHangXe_MaHangAndTrangThaiTrue(
            Integer maHang,
            Pageable pageable
    );

    // Kiểm tra xe có tồn tại hay không
    boolean existsByTenXeIgnoreCaseAndHangXe_MaHang(
            String tenXe,
            Integer maHang
    );

    /** Các loại xe đang có (Xe số, Tay ga...) cho ô lọc. */
    @Query("SELECT DISTINCT x.loaiXe FROM XeMay x ORDER BY x.loaiXe")
    List<String> danhSachLoaiXe();

    // ------------------------------------------------------------------ Trang cho khách (thành viên 2)

    /**
     * Tìm xe đang bán cho trang danh sách của khách: theo tên xe hoặc tên hãng ("Honda Vision" cũng tìm được),
     * khoảng giá, chỉ xe còn hàng. {@code q} đã đổi sang chữ thường; chuỗi rỗng là không lọc theo tên.
     */
    @EntityGraph(attributePaths = "hangXe")
    @Query("""
        SELECT x FROM XeMay x JOIN x.hangXe h
        WHERE x.trangThai = true
        AND (:q = '' OR LOWER(x.tenXe) LIKE CONCAT('%', :q, '%')
             OR LOWER(h.tenHang) LIKE CONCAT('%', :q, '%')
             OR LOWER(CONCAT(h.tenHang, ' ', x.tenXe)) LIKE CONCAT('%', :q, '%'))
        AND (:giaTu IS NULL OR x.giaBan >= :giaTu)
        AND (:giaDen IS NULL OR x.giaBan <= :giaDen)
        AND (:conHang = false OR x.soLuong > 0)
    """)
    Page<XeMay> timXeChoKhach(@Param("q") String q,
                              @Param("giaTu") BigDecimal giaTu,
                              @Param("giaDen") BigDecimal giaDen,
                              @Param("conHang") boolean conHang,
                              Pageable pageable);

    /** Xe đang bán, có phân trang (trang chủ lấy các xe mới thêm). */
    @EntityGraph(attributePaths = "hangXe")
    Page<XeMay> findByTrangThaiTrue(Pageable pageable);

    // ------------------------------------------------------------------ Tồn kho, client Socket

    @EntityGraph(attributePaths = "hangXe")
    Optional<XeMay> findWithHangXeByMaXe(Integer maXe);

    /** Xe ĐANG BÁN có số lượng tồn nhỏ hơn hoặc bằng ngưỡng cảnh báo, xe ít hàng nhất đứng trước. */
    @EntityGraph(attributePaths = "hangXe")
    List<XeMay> findByTrangThaiTrueAndSoLuongLessThanEqualOrderBySoLuongAscTenXeAsc(int nguong);

    /**
     * Tìm xe theo tên xe hoặc tên hãng (dùng cho lệnh DSXE của client kho).
     *
     * @param mau mẫu LIKE chữ thường, ví dụ {@code %honda%}; {@code %} để lấy tất cả
     */
    @EntityGraph(attributePaths = "hangXe")
    @Query("""
        SELECT x FROM XeMay x
        WHERE LOWER(x.tenXe) LIKE :mau OR LOWER(x.hangXe.tenHang) LIKE :mau
        ORDER BY x.hangXe.tenHang, x.tenXe
    """)
    List<XeMay> timTheoTenHoacHang(@Param("mau") String mau, Pageable gioiHan);
}
