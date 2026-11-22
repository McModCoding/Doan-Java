package com.xemay.repository;

import com.xemay.entity.ChiTietGioHang;
import com.xemay.entity.GioHang;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    Optional<GioHang> findByKhachHang_MaKhachHang(Integer maKhachHang);

    /** Kiểm tra khách đã có giỏ chưa, không khóa gì (đọc xong là nhả). */
    boolean existsByKhachHang_MaKhachHang(Integer maKhachHang);

    /**
     * Đọc giỏ của khách và KHÓA dòng giỏ đến hết giao dịch. Mọi thao tác thay đổi giỏ (thêm, sửa, xóa, đặt hàng)
     * của cùng một khách vì thế chạy lần lượt: bấm "Đặt hàng" hai lần liên tiếp không tạo ra hai đơn.
     * Trên SQL Server khóa này là {@code WITH (UPDLOCK, HOLDLOCK, ROWLOCK)}: nếu khách CHƯA có giỏ, SQL Server khóa
     * cả khoảng chỉ mục nơi dòng đó sẽ nằm, nên chỉ gọi sau khi chắc chắn giỏ đã tồn tại (xem GioHangService.khoaGio).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GioHang g WHERE g.khachHang.maKhachHang = :maKhachHang")
    Optional<GioHang> khoaTheoKhachHang(@Param("maKhachHang") Integer maKhachHang);

    /** Các dòng trong giỏ, đọc kèm xe và hãng xe để hiển thị. */
    @Query("""
        SELECT c FROM ChiTietGioHang c
        JOIN FETCH c.xeMay x JOIN FETCH x.hangXe
        WHERE c.gioHang.maGioHang = :maGioHang
        ORDER BY c.maChiTietGioHang
    """)
    List<ChiTietGioHang> dongTrongGio(@Param("maGioHang") Integer maGioHang);
}
