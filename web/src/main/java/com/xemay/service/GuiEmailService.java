package com.xemay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * GỬI EMAIL CHÀO MỪNG KHÁCH HÀNG MỚI trên luồng nền - ý tưởng xử lý bất đồng bộ của thành viên phụ trách
 * khách hàng ({@code CustomerService.sendWelcomeEmailAsync}), sửa lại khi tích hợp:
 *
 * <ol>
 *   <li><b>Tách sang lớp riêng.</b> Bản gốc gọi phương thức {@code @Async} từ chính lớp chứa nó
 *       ({@code this.sendWelcomeEmailAsync(...)}). Lời gọi nội bộ như vậy đi thẳng vào đối tượng, không qua
 *       proxy của Spring, nên {@code @Async} bị bỏ qua: request phải đứng chờ 2 giây. Gọi sang một bean khác
 *       (ở đây: qua sự kiện) thì mới thật sự chạy trên luồng {@code xemay-nen-N}.</li>
 *   <li><b>Chỉ gửi SAU KHI giao dịch đã commit</b> ({@code @TransactionalEventListener(AFTER_COMMIT)}). Nếu gửi
 *       ngay lúc lưu, giao dịch có thể bị hủy sau đó (ví dụ trùng số điện thoại) mà khách vẫn nhận được thư.</li>
 * </ol>
 *
 * <p>Đồ án không gửi thư thật: luồng nền chờ một khoảng thời gian (giả lập kết nối máy chủ thư) rồi ghi log.
 */
@Service
public class GuiEmailService {

    private static final Logger log = LoggerFactory.getLogger(GuiEmailService.class);

    private final long thoiGianGiaLapMs;

    /** Số thư đã "gửi" - nhiều luồng nền cùng cộng nên dùng biến nguyên tử. */
    private final AtomicInteger soThuDaGui = new AtomicInteger();

    /** Tên luồng đã gửi thư gần nhất (để kiểm thử thấy thư được gửi trên luồng nền). */
    private volatile String luongGuiGanNhat;

    public GuiEmailService(@Value("${xemay.email.thoi-gian-gia-lap-ms:1500}") long thoiGianGiaLapMs) {
        this.thoiGianGiaLapMs = thoiGianGiaLapMs;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void guiEmailChaoMung(SuKienKhachHangMoi suKien) {
        if (suKien.email() == null || suKien.email().isBlank()) {
            return;
        }
        try {
            Thread.sleep(thoiGianGiaLapMs);   // giả lập thời gian kết nối máy chủ thư
            soThuDaGui.incrementAndGet();
            luongGuiGanNhat = Thread.currentThread().getName();
            log.info("Đã gửi email chào mừng tới {} <{}> [luồng: {}]",
                    suKien.hoTen(), suKien.email(), Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Gửi email cho {} bị ngắt: {}", suKien.email(), e.getMessage());
        }
    }

    public int getSoThuDaGui() {
        return soThuDaGui.get();
    }

    public String getLuongGuiGanNhat() {
        return luongGuiGanNhat;
    }
}
