package com.xemay.scheduler;

import com.xemay.dto.CanhBaoTonKho;
import com.xemay.security.BoDemDangNhapSai;
import com.xemay.service.LichSuDangNhapService;
import com.xemay.service.TonKhoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CÁC TÁC VỤ ĐỊNH KỲ. Spring chạy chúng trên thread pool riêng (tên luồng {@code xemay-dinh-ky-N}),
 * song song với các luồng đang phục vụ người dùng.
 */
@Component
public class TacVuDinhKy {

    private static final Logger log = LoggerFactory.getLogger(TacVuDinhKy.class);

    private final TonKhoService tonKhoService;
    private final BoDemDangNhapSai boDemDangNhapSai;
    private final LichSuDangNhapService lichSuDangNhapService;
    private final int soNgayLuuLichSu;

    /** Kết quả lần quét trước, để chỉ ghi log khi danh sách cảnh báo thay đổi. */
    private volatile List<CanhBaoTonKho.Muc> lanQuetTruoc;

    public TacVuDinhKy(TonKhoService tonKhoService,
                       BoDemDangNhapSai boDemDangNhapSai,
                       LichSuDangNhapService lichSuDangNhapService,
                       @Value("${xemay.lich-su-dang-nhap.so-ngay-luu:90}") int soNgayLuuLichSu) {
        this.tonKhoService = tonKhoService;
        this.boDemDangNhapSai = boDemDangNhapSai;
        this.lichSuDangNhapService = lichSuDangNhapService;
        this.soNgayLuuLichSu = soNgayLuuLichSu;
    }

    /**
     * Quét tồn kho theo chu kỳ (mặc định mỗi phút) để tìm xe sắp hết hàng.
     * Kết quả được công bố cho trang Quản trị > Tổng quan; nhân viên không phải tự tra từng xe.
     * {@code fixedDelay}: tính từ lúc lần chạy trước KẾT THÚC, nên hai lần quét không bao giờ chồng lên nhau.
     */
    @Scheduled(initialDelayString = "${xemay.ton-kho.tre-lan-dau-ms:5000}",
            fixedDelayString = "${xemay.ton-kho.chu-ky-quet-ms:60000}")
    public void quetTonKho() {
        try {
            CanhBaoTonKho ketQua = tonKhoService.quetXeSapHetHang();
            List<CanhBaoTonKho.Muc> danhSach = ketQua.danhSach();
            if (danhSach.equals(lanQuetTruoc)) {
                log.debug("Quét tồn kho: không thay đổi [luồng: {}]", Thread.currentThread().getName());
            } else if (danhSach.isEmpty()) {
                log.info("Quét tồn kho: không có xe nào sắp hết hàng [luồng: {}]", Thread.currentThread().getName());
            } else {
                String moTa = danhSach.stream()
                        .map(muc -> muc.tenXe() + " (còn " + muc.soLuongTon() + ")")
                        .collect(Collectors.joining(", "));
                log.warn("CẢNH BÁO TỒN KHO: {} xe còn từ {} chiếc trở xuống: {} [luồng: {}]",
                        danhSach.size(), ketQua.nguong(), moTa, Thread.currentThread().getName());
            }
            lanQuetTruoc = danhSach;
        } catch (RuntimeException e) {
            log.warn("Không quét được tồn kho: {}", e.getMessage());
        }
    }

    /** Mỗi 5 phút: xóa các bộ đếm đăng nhập sai đã hết hạn để không chiếm bộ nhớ. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void donBoDemDangNhapSai() {
        int soMucDaXoa = boDemDangNhapSai.donDep();
        if (soMucDaXoa > 0) {
            log.info("Đã dọn {} bộ đếm đăng nhập sai hết hạn [luồng: {}]", soMucDaXoa, Thread.currentThread().getName());
        }
    }

    /** 02:00 mỗi ngày: xóa nhật ký đăng nhập quá cũ. Biểu thức cron: giây phút giờ ngày tháng thứ. */
    @Scheduled(cron = "${xemay.lich-su-dang-nhap.lich-don:0 0 2 * * *}")
    public void donLichSuDangNhapCu() {
        try {
            int soBanGhi = lichSuDangNhapService.xoaCuHon(soNgayLuuLichSu);
            log.info("Đã xóa {} dòng lịch sử đăng nhập cũ hơn {} ngày [luồng: {}]",
                    soBanGhi, soNgayLuuLichSu, Thread.currentThread().getName());
        } catch (RuntimeException e) {
            log.warn("Không dọn được lịch sử đăng nhập: {}", e.getMessage());
        }
    }
}
