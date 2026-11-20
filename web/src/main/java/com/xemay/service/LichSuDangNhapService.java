package com.xemay.service;

import com.xemay.entity.LichSuDangNhap;
import com.xemay.entity.TaiKhoan;
import com.xemay.repository.LichSuDangNhapRepository;
import com.xemay.repository.TaiKhoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Nhật ký đăng nhập: ghi bằng tác vụ nền, tra cứu cho quản trị viên, dọn dẹp định kỳ. */
@Service
public class LichSuDangNhapService {

    private static final Logger log = LoggerFactory.getLogger(LichSuDangNhapService.class);
    private static final int DO_DAI_TEN_TOI_DA = 100;

    private final LichSuDangNhapRepository lichSuDangNhapRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public LichSuDangNhapService(LichSuDangNhapRepository lichSuDangNhapRepository,
                                 TaiKhoanRepository taiKhoanRepository) {
        this.lichSuDangNhapRepository = lichSuDangNhapRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    /**
     * Ghi một dòng nhật ký đăng nhập - CHẠY TRÊN LUỒNG NỀN.
     *
     * <p>Nhờ {@code @Async}, nơi gọi (luồng đang xử lý request đăng nhập) không phải chờ câu lệnh INSERT:
     * Spring đưa công việc này vào hàng đợi của thread pool {@code xemay-nen-*} rồi trả về ngay,
     * nên người dùng được chuyển trang lập tức. Nếu việc ghi nhật ký bị lỗi thì cũng không làm hỏng
     * việc đăng nhập.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void ghiNhan(String tenDangNhap, String diaChiIp, String kenh, boolean thanhCong, String ghiChu) {
        String ten = TaiKhoan.chuanHoaTenDangNhap(tenDangNhap);
        if (ten.isEmpty()) {
            ten = "(để trống)";
        } else if (ten.length() > DO_DAI_TEN_TOI_DA) {
            ten = ten.substring(0, DO_DAI_TEN_TOI_DA);
        }

        LichSuDangNhap lichSu = new LichSuDangNhap();
        lichSu.setTenDangNhap(ten);
        lichSu.setTaiKhoan(taiKhoanRepository.findByTenDangNhap(ten).orElse(null));
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setDiaChiIp(diaChiIp);
        lichSu.setKenh(kenh);
        lichSu.setThanhCong(thanhCong);
        lichSu.setGhiChu(ghiChu);
        lichSuDangNhapRepository.save(lichSu);

        log.info("Đã ghi lịch sử đăng nhập ({}) qua {} [luồng nền: {}]",
                thanhCong ? "thành công" : "thất bại", kenh, Thread.currentThread().getName());
    }

    @Transactional(readOnly = true)
    public Page<LichSuDangNhap> timKiem(String tuKhoa, Pageable pageable) {
        String tk = tuKhoa == null ? "" : tuKhoa.trim();
        return lichSuDangNhapRepository.findByTenDangNhapContainingIgnoreCase(tk, pageable);
    }

    /**
     * Xóa nhật ký cũ hơn số ngày cho trước.
     *
     * @return số bản ghi đã xóa
     */
    @Transactional
    public int xoaCuHon(int soNgay) {
        return lichSuDangNhapRepository.xoaTruocThoiDiem(LocalDateTime.now().minusDays(soNgay));
    }
}
