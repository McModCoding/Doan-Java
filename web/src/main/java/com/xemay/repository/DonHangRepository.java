package com.xemay.repository;

import com.xemay.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonHangRepository extends JpaRepository<DonHang, Integer> {

    List<DonHang> findByKhachHang_MaKhachHangOrderByMaDonHangDesc(Integer maKhachHang);

    long countByTrangThai(String trangThai);
}
