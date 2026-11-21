package com.xemay.service;

import com.xemay.entity.HangXe;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.HangXeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ hãng xe - phần của thành viên phụ trách xe máy, hãng xe.
 *
 * <p>Tích hợp: chuyển package; lỗi nghiệp vụ dùng các ngoại lệ chung của dự án thay cho
 * {@code IllegalArgumentException} (không tìm thấy -> trang 404, lỗi dữ liệu -> hiện ngay dưới ô nhập);
 * thao tác ngừng hoạt động hãng xe chỉ dành cho quản trị viên; thêm hàm đếm số xe của từng hãng.
 */
@Service
@Transactional
public class HangXeService {

    private final HangXeRepository hangXeRepository;

    // Constructor Injection
    public HangXeService(HangXeRepository hangXeRepository) {
        this.hangXeRepository = hangXeRepository;
    }

    // 1. Lấy tất cả hãng xe
    @Transactional(readOnly = true)
    public List<HangXe> getAllHangXe() {
        return hangXeRepository.findAll(Sort.by("tenHang"));
    }

    // 2. Lấy danh sách hãng xe đang hoạt động
    @Transactional(readOnly = true)
    public List<HangXe> getHangXeDangHoatDong() {
        return hangXeRepository.findByTrangThaiTrue().stream()
                .sorted((a, b) -> a.getTenHang().compareToIgnoreCase(b.getTenHang()))
                .toList();
    }

    // 3. Tìm hãng xe theo mã
    @Transactional(readOnly = true)
    public HangXe getHangXeById(Integer maHang) {
        return hangXeRepository.findById(maHang)
                .orElseThrow(() -> new KhongTimThayException("Không tìm thấy hãng xe có mã: " + maHang));
    }

    // 4. Tìm kiếm hãng xe theo tên gần đúng
    @Transactional(readOnly = true)
    public List<HangXe> searchHangXe(String tenHang) {
        if (tenHang == null || tenHang.trim().isEmpty()) {
            return getAllHangXe();
        }

        return hangXeRepository
                .findByTenHangContainingIgnoreCase(tenHang.trim());
    }

    /** Tích hợp: số mẫu xe của từng hãng (mã hãng -> số xe), hãng chưa có xe thì không có trong Map. */
    @Transactional(readOnly = true)
    public Map<Integer, Long> demXeTheoHang() {
        Map<Integer, Long> ketQua = new HashMap<>();
        for (Object[] dong : hangXeRepository.demXeTheoHang()) {
            ketQua.put((Integer) dong[0], (Long) dong[1]);
        }
        return ketQua;
    }

    // 5. Thêm hãng xe
    public HangXe addHangXe(HangXe hangXe) {

        validateHangXe(hangXe);

        // Kiểm tra tên hãng đã tồn tại
        if (hangXeRepository.existsByTenHangIgnoreCase(
                hangXe.getTenHang().trim())) {
            throw new DuLieuKhongHopLeException("tenHang", "Tên hãng xe đã tồn tại!");
        }

        hangXe.setMaHang(null);   // tích hợp: luôn tạo mới, không cho form ghi đè hãng đang có
        hangXe.setTenHang(hangXe.getTenHang().trim());

        if (hangXe.getTrangThai() == null) {
            hangXe.setTrangThai(true);
        }

        return hangXeRepository.save(hangXe);
    }

    // 6. Cập nhật hãng xe
    public HangXe updateHangXe(Integer maHang, HangXe hangXeMoi) {

        validateHangXe(hangXeMoi);

        HangXe hangXeCu = getHangXeById(maHang);

        String tenHangMoi = hangXeMoi.getTenHang().trim();

        // Kiểm tra trùng tên với hãng khác
        if (hangXeRepository
                .existsByTenHangIgnoreCaseAndMaHangNot(
                        tenHangMoi, maHang)) {
            throw new DuLieuKhongHopLeException("tenHang", "Tên hãng xe đã được sử dụng!");
        }

        hangXeCu.setTenHang(tenHangMoi);
        hangXeCu.setQuocGia(hangXeMoi.getQuocGia());
        hangXeCu.setMoTa(hangXeMoi.getMoTa());
        // Tích hợp: không gửi trạng thái (nhân viên không có ô này trên form) thì giữ nguyên, thay vì tự bật lại
        if (hangXeMoi.getTrangThai() != null) {
            hangXeCu.setTrangThai(hangXeMoi.getTrangThai());
        }

        return hangXeRepository.save(hangXeCu);
    }

    // 7. Ngừng hoạt động hãng xe
    // Không xóa vật lý vì có thể đang được xe máy tham chiếu
    @PreAuthorize("hasRole('ADMIN')")
    public HangXe deleteHangXe(Integer maHang) {

        HangXe hangXe = getHangXeById(maHang);

        if (hangXeRepository.existsXeByMaHang(maHang)) {
            throw new NghiepVuException(
                    "Không thể ngừng hoạt động hãng \"" + hangXe.getTenHang() + "\" vì vẫn còn xe máy thuộc hãng này!"
            );
        }

        hangXe.setTrangThai(false);
        return hangXeRepository.save(hangXe);
    }

    // 8. Kiểm tra dữ liệu hãng xe
    private void validateHangXe(HangXe hangXe) {

        if (hangXe == null) {
            throw new NghiepVuException("Thông tin hãng xe không được để trống!");
        }

        if (hangXe.getTenHang() == null
                || hangXe.getTenHang().trim().isEmpty()) {
            throw new DuLieuKhongHopLeException("tenHang", "Tên hãng xe không được để trống!");
        }

        if (hangXe.getTenHang().trim().length() > 100) {
            throw new DuLieuKhongHopLeException("tenHang", "Tên hãng xe không được vượt quá 100 ký tự!");
        }

        // Tích hợp: kiểm tra thêm độ dài hai cột còn lại để không bị lỗi khi lưu xuống CSDL
        if (hangXe.getQuocGia() != null && hangXe.getQuocGia().trim().length() > 100) {
            throw new DuLieuKhongHopLeException("quocGia", "Quốc gia không được vượt quá 100 ký tự!");
        }

        if (hangXe.getMoTa() != null && hangXe.getMoTa().trim().length() > 500) {
            throw new DuLieuKhongHopLeException("moTa", "Mô tả không được vượt quá 500 ký tự!");
        }
    }
}
