package com.xemay.service;

import com.xemay.entity.HangXe;
import com.xemay.entity.XeMay;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.HangXeRepository;
import com.xemay.repository.XeMayRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ xe máy - phần của thành viên phụ trách xe máy, hãng xe (tìm kiếm, lọc, thêm, sửa, ngừng kinh doanh).
 *
 * <p>Tích hợp:
 * <ul>
 *   <li><b>Sửa xe không ghi đè số lượng tồn.</b> Bản gốc gán {@code SoLuong} từ form khi sửa: nếu đúng lúc đó
 *       có khách đặt mua (trừ kho), con số trên form đã CŨ và ghi đè làm mất lần trừ kho (lost update).
 *       Số lượng chỉ đổi qua {@link TonKhoService} - xem nút "Nhập kho" trên trang sửa xe.</li>
 *   <li>Lỗi nghiệp vụ dùng ngoại lệ chung của dự án thay cho {@code IllegalArgumentException}.</li>
 *   <li>Lọc thêm theo trạng thái (trang quản trị xem được cả xe ngừng kinh doanh).</li>
 *   <li>Ngừng kinh doanh chỉ dành cho quản trị viên ({@code @PreAuthorize}).</li>
 * </ul>
 */
@Service
@Transactional
public class XeMayService {

    private final XeMayRepository xeMayRepository;
    private final HangXeRepository hangXeRepository;

    // Constructor Injection
    public XeMayService(
            XeMayRepository xeMayRepository,
            HangXeRepository hangXeRepository) {

        this.xeMayRepository = xeMayRepository;
        this.hangXeRepository = hangXeRepository;
    }

    // 1. Lấy tất cả xe máy
    @Transactional(readOnly = true)
    public List<XeMay> getAllXeMay() {
        return xeMayRepository.findAll();
    }

    // 2. Lấy danh sách xe đang kinh doanh
    @Transactional(readOnly = true)
    public List<XeMay> getXeMayDangKinhDoanh() {
        return xeMayRepository.findByTrangThaiTrue();
    }

    @Transactional(readOnly = true)
    public long demXeDangKinhDoanh() {
        return xeMayRepository.countByTrangThaiTrue();
    }

    // 3. Tìm xe theo mã
    @Transactional(readOnly = true)
    public XeMay getXeMayById(Integer maXe) {
        return xeMayRepository.findWithHangXeByMaXe(maXe)
                .orElseThrow(() -> new KhongTimThayException("Không tìm thấy xe máy có mã: " + maXe));
    }

    // 4. Tìm kiếm xe theo tên gần đúng
    @Transactional(readOnly = true)
    public List<XeMay> searchXeMay(String tenXe) {

        if (tenXe == null || tenXe.trim().isEmpty()) {
            return getXeMayDangKinhDoanh();
        }

        return xeMayRepository
                .findByTenXeContainingIgnoreCase(tenXe.trim());
    }

    // 5. Lọc xe theo hãng
    @Transactional(readOnly = true)
    public List<XeMay> getXeMayByHang(Integer maHang) {

        if (!hangXeRepository.existsById(maHang)) {
            throw new KhongTimThayException("Không tìm thấy hãng xe có mã: " + maHang);
        }

        return xeMayRepository.findByHangXe_MaHang(maHang);
    }

    /** Các loại xe đang có trong CSDL (Xe số, Tay ga, Côn tay...) cho ô lọc. */
    @Transactional(readOnly = true)
    public List<String> getDanhSachLoaiXe() {
        return xeMayRepository.danhSachLoaiXe();
    }

    /**
     * 6. Tìm kiếm, lọc xe và phân trang.
     *
     * @param trangThai {@code true}: chỉ xe đang bán (trang công khai), {@code false}: chỉ xe ngừng kinh doanh,
     *                  {@code null}: tất cả (trang quản trị)
     */
    @Transactional(readOnly = true)
    public Page<XeMay> timKiemVaLoc(
            String tenXe,
            Integer maHang,
            String loaiXe,
            BigDecimal giaMin,
            BigDecimal giaMax,
            Boolean trangThai,
            Pageable pageable) {

        if (giaMin != null && giaMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new DuLieuKhongHopLeException("giaMin", "Giá tối thiểu không được âm!");
        }

        if (giaMax != null && giaMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new DuLieuKhongHopLeException("giaMax", "Giá tối đa không được âm!");
        }

        if (giaMin != null && giaMax != null
                && giaMin.compareTo(giaMax) > 0) {
            throw new DuLieuKhongHopLeException("giaMin", "Giá tối thiểu không được lớn hơn giá tối đa!");
        }

        if (maHang != null && !hangXeRepository.existsById(maHang)) {
            throw new DuLieuKhongHopLeException("maHang", "Hãng xe không tồn tại!");
        }

        return xeMayRepository.timKiemVaLoc(
                lamSachChuoi(tenXe),
                maHang,
                lamSachChuoi(loaiXe),
                giaMin,
                giaMax,
                trangThai,
                pageable
        );
    }

    // 7. Thêm xe máy (số lượng ban đầu lấy từ form - xe mới chưa ai đặt được)
    public XeMay addXeMay(XeMay xeMay) {

        validateXeMay(xeMay);

        // Kiểm tra hãng xe
        HangXe hangXe = layHangXeHopLe(
                xeMay.getHangXe() == null ? null : xeMay.getHangXe().getMaHang()
        );

        // Kiểm tra trùng tên xe trong cùng hãng
        if (xeMayRepository
                .existsByTenXeIgnoreCaseAndHangXe_MaHang(
                        xeMay.getTenXe().trim(),
                        hangXe.getMaHang())) {

            throw new DuLieuKhongHopLeException("tenXe", "Tên xe đã tồn tại trong hãng này!");
        }

        xeMay.setMaXe(null);
        xeMay.setTenXe(xeMay.getTenXe().trim());
        xeMay.setHangXe(hangXe);
        xeMay.setNgayTao(LocalDateTime.now());

        if (xeMay.getTrangThai() == null) {
            xeMay.setTrangThai(true);
        }

        return xeMayRepository.save(xeMay);
    }

    // 8. Cập nhật xe máy - KHÔNG đổi số lượng tồn (xem chú thích đầu lớp)
    public XeMay updateXeMay(Integer maXe, XeMay xeMayMoi) {

        xeMayMoi.setSoLuong(0);   // số lượng trên form sửa không được dùng; gán 0 chỉ để qua bước kiểm tra
        validateXeMay(xeMayMoi);

        XeMay xeMayCu = getXeMayById(maXe);

        HangXe hangXe = layHangXeHopLe(
                xeMayMoi.getHangXe() == null ? null : xeMayMoi.getHangXe().getMaHang()
        );

        // Kiểm tra trùng tên xe trong cùng hãng
        if (xeMayRepository
                .existsByTenXeIgnoreCaseAndHangXe_MaHang(
                        xeMayMoi.getTenXe().trim(),
                        hangXe.getMaHang())) {

            // Cho phép giữ nguyên tên của chính xe đang sửa
            boolean trungChinhNo =
                    xeMayCu.getTenXe().equalsIgnoreCase(
                            xeMayMoi.getTenXe().trim())
                            && xeMayCu.getHangXe().getMaHang()
                            .equals(hangXe.getMaHang());

            if (!trungChinhNo) {
                throw new DuLieuKhongHopLeException("tenXe", "Tên xe đã tồn tại trong hãng này!");
            }
        }

        xeMayCu.setHangXe(hangXe);
        xeMayCu.setTenXe(xeMayMoi.getTenXe().trim());
        xeMayCu.setLoaiXe(xeMayMoi.getLoaiXe());
        xeMayCu.setPhanKhoi(xeMayMoi.getPhanKhoi());
        xeMayCu.setMauSac(xeMayMoi.getMauSac());
        xeMayCu.setGiaBan(xeMayMoi.getGiaBan());
        // Tích hợp: bỏ dòng xeMayCu.setSoLuong(xeMayMoi.getSoLuong()) - số lượng chỉ đổi qua TonKhoService
        xeMayCu.setHinhAnh(xeMayMoi.getHinhAnh());
        xeMayCu.setMoTa(xeMayMoi.getMoTa());

        if (xeMayMoi.getTrangThai() != null) {
            xeMayCu.setTrangThai(xeMayMoi.getTrangThai());
        }

        // Không thay đổi NgayTao khi cập nhật
        return xeMayRepository.save(xeMayCu);
    }

    // 9. Ngừng kinh doanh xe máy
    // Không xóa vật lý để giữ lịch sử đơn hàng và hóa đơn
    @PreAuthorize("hasRole('ADMIN')")
    public XeMay deleteXeMay(Integer maXe) {

        XeMay xeMay = getXeMayById(maXe);

        xeMay.setTrangThai(false);

        return xeMayRepository.save(xeMay);
    }

    // 10. Lấy hãng xe hợp lệ
    private HangXe layHangXeHopLe(Integer maHang) {

        if (maHang == null) {
            throw new DuLieuKhongHopLeException("maHang", "Vui lòng chọn hãng xe!");
        }

        HangXe hangXe = hangXeRepository.findById(maHang)
                .orElseThrow(() -> new DuLieuKhongHopLeException("maHang", "Hãng xe không tồn tại!"));

        if (!Boolean.TRUE.equals(hangXe.getTrangThai())) {
            throw new DuLieuKhongHopLeException("maHang", "Hãng xe đã ngừng hoạt động!");
        }

        return hangXe;
    }

    // 11. Kiểm tra dữ liệu xe máy
    private void validateXeMay(XeMay xeMay) {

        if (xeMay == null) {
            throw new NghiepVuException("Thông tin xe máy không được để trống!");
        }

        if (xeMay.getTenXe() == null
                || xeMay.getTenXe().trim().isEmpty()) {
            throw new DuLieuKhongHopLeException("tenXe", "Tên xe không được để trống!");
        }

        if (xeMay.getTenXe().trim().length() > 150) {
            throw new DuLieuKhongHopLeException("tenXe", "Tên xe không được vượt quá 150 ký tự!");
        }

        if (xeMay.getLoaiXe() == null
                || xeMay.getLoaiXe().trim().isEmpty()) {
            throw new DuLieuKhongHopLeException("loaiXe", "Loại xe không được để trống!");
        }

        if (xeMay.getGiaBan() == null
                || xeMay.getGiaBan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DuLieuKhongHopLeException("giaBan", "Giá bán phải lớn hơn 0!");
        }

        if (xeMay.getSoLuong() == null || xeMay.getSoLuong() < 0) {
            throw new DuLieuKhongHopLeException("soLuong", "Số lượng xe không được âm!");
        }

        if (xeMay.getPhanKhoi() != null
                && xeMay.getPhanKhoi() <= 0) {
            throw new DuLieuKhongHopLeException("phanKhoi", "Phân khối phải lớn hơn 0!");
        }
    }

    // 12. Làm sạch chuỗi tìm kiếm
    private String lamSachChuoi(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
