package com.xemay.repository;

import com.xemay.entity.LichSuDangNhap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LichSuDangNhapRepository extends JpaRepository<LichSuDangNhap, Long> {

    Page<LichSuDangNhap> findByTenDangNhapContainingIgnoreCase(String tuKhoa, Pageable pageable);

    /** Gỡ liên kết tới tài khoản sắp bị xóa nhưng vẫn giữ lại nhật ký. */
    @Modifying
    @Query("update LichSuDangNhap l set l.taiKhoan = null where l.taiKhoan.id = :taiKhoanId")
    int boLienKetTaiKhoan(@Param("taiKhoanId") Long taiKhoanId);

    /** Xóa nhật ký cũ hơn thời điểm cho trước (dùng cho tác vụ dọn dẹp định kỳ). */
    @Modifying
    @Query("delete from LichSuDangNhap l where l.thoiGian < :moc")
    int xoaTruocThoiDiem(@Param("moc") LocalDateTime moc);
}
