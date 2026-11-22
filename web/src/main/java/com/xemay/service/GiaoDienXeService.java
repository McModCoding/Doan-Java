package com.xemay.service;

import com.xemay.dto.SanPhamHienThi;
import com.xemay.entity.XeMay;
import com.xemay.repository.XeMayRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lớp chuyển đổi giữa dữ liệu xe máy của thành viên quản lý xe và giao diện.
 * Chỉ đưa ra trang của khách các xe đang kinh doanh (TrangThai = 1).
 */
@Service
public class GiaoDienXeService {
    private final XeMayRepository repo;
    public GiaoDienXeService(XeMayRepository repo) {this.repo=repo;}

    @Transactional(readOnly=true)
    public Page<SanPhamHienThi> danhSach(String q, BigDecimal giaTu, BigDecimal giaDen,
                                       boolean conHang, int page, String sapXep) {
        String tuKhoa = q == null ? "" : q.trim();
        if (tuKhoa.length() > 100) tuKhoa=tuKhoa.substring(0,100);
        Sort sort = switch (sapXep) {
            case "gia-tang" -> Sort.by("giaBan").ascending().and(Sort.by("maXe").ascending());
            case "gia-giam" -> Sort.by("giaBan").descending().and(Sort.by("maXe").descending());
            case "ten" -> Sort.by("tenXe").ascending().and(Sort.by("maXe").ascending());
            default -> Sort.by("maXe").descending();
        };
        return repo.timXeChoKhach(tuKhoa.toLowerCase(Locale.ROOT), giaTu, giaDen, conHang,
                PageRequest.of(Math.max(0,Math.min(page,999)), 9, sort)).map(this::map);
    }

    @Transactional(readOnly=true)
    public List<SanPhamHienThi> noiBat() {
        return repo.findByTrangThaiTrue(PageRequest.of(0,6,Sort.by("maXe").descending()))
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly=true)
    public Optional<SanPhamHienThi> chiTiet(Integer id) {
        if (id == null) return Optional.empty();
        return repo.findWithHangXeByMaXe(id).filter(xe -> Boolean.TRUE.equals(xe.getTrangThai())).map(this::map);
    }

    /** Dùng chung cho giỏ hàng: chuyển một xe (đã đọc kèm hãng) sang dữ liệu hiển thị. */
    public SanPhamHienThi map(XeMay xe) {
        String moTa = (xe.getMoTa() == null || xe.getMoTa().isBlank())
                ? "Thông tin chi tiết của mẫu xe đang được cập nhật." : xe.getMoTa();
        // Ảnh lấy từ cột HinhAnh; xe chưa có ảnh thì thử ảnh đặt theo mã xe, không có nữa thì giao diện báo chưa có ảnh
        String anh = (xe.getHinhAnh() == null || xe.getHinhAnh().isBlank())
                ? "/images/xe/xe-"+xe.getMaXe()+".jpg" : xe.getHinhAnh().trim();
        return new SanPhamHienThi(xe.getMaXe(), xe.getTenXe(), xe.getHangXe().getTenHang(), xe.getLoaiXe(),
                xe.getGiaBan(), xe.getSoLuong(), moTa, anh);
    }
}
