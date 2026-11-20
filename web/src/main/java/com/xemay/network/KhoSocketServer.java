package com.xemay.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SOCKET SERVER cho chương trình client quản lý kho (lập trình mạng với {@link ServerSocket}).
 *
 * <p>Chạy bên trong ứng dụng web nên dùng chung cơ sở dữ liệu và các Service. Mô hình đa luồng:
 * <ul>
 *   <li>1 luồng {@code kho-socket-chap-nhan}: chỉ làm một việc là chờ {@code accept()} kết nối mới.</li>
 *   <li>Thread pool {@code kho-client-N}: mỗi client được một luồng phục vụ riêng ({@link XuLyClient}),
 *       nên client này đang chờ không làm client khác bị chậm.</li>
 *   <li>Pool có giới hạn ({@code xemay.socket.so-client-toi-da}): khi đã đủ số client, kết nối mới nhận
 *       ngay {@code ERR 503} rồi bị đóng, thay vì tạo luồng không giới hạn làm sập server.</li>
 * </ul>
 *
 * <p>Là một {@link SmartLifecycle}: Spring gọi {@link #start()} khi ứng dụng khởi động xong và {@link #stop()}
 * khi ứng dụng tắt, để đóng cổng và các kết nối đúng cách. Tắt hẳn bằng {@code xemay.socket.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "xemay.socket.enabled", havingValue = "true", matchIfMissing = true)
public class KhoSocketServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(KhoSocketServer.class);

    private final BoXuLyLenh boXuLyLenh;
    private final int congCauHinh;
    private final int soClientToiDa;
    private final int thoiGianChoMs;

    // ---- Số liệu thống kê: được nhiều luồng cập nhật cùng lúc nên dùng biến Atomic ----
    private final AtomicInteger soClientDangPhucVu = new AtomicInteger();
    private final AtomicLong tongSoKetNoi = new AtomicLong();
    private final AtomicLong soKetNoiBiTuChoi = new AtomicLong();
    /** Các kết nối đang mở, để đóng hết khi server dừng. Tập hợp an toàn khi nhiều luồng cùng thêm/xóa. */
    private final Set<Socket> cacKetNoi = ConcurrentHashMap.newKeySet();

    private volatile boolean dangChay;
    private volatile ServerSocket serverSocket;
    private volatile ThreadPoolExecutor nhomLuong;

    public KhoSocketServer(BoXuLyLenh boXuLyLenh,
                           @Value("${xemay.socket.port:9090}") int cong,
                           @Value("${xemay.socket.so-client-toi-da:10}") int soClientToiDa,
                           @Value("${xemay.socket.thoi-gian-cho-giay:300}") int thoiGianChoGiay) {
        this.boXuLyLenh = boXuLyLenh;
        this.congCauHinh = cong;
        this.soClientToiDa = soClientToiDa;
        this.thoiGianChoMs = thoiGianChoGiay * 1000;
    }

    @Override
    public synchronized void start() {
        if (dangChay) {
            return;
        }
        try {
            serverSocket = new ServerSocket(congCauHinh);
        } catch (IOException e) {
            // Không làm hỏng cả ứng dụng web chỉ vì cổng Socket đang bị chương trình khác chiếm
            log.error("KHÔNG mở được Socket server ở cổng {}: {}. Ứng dụng web vẫn chạy nhưng client quản lý kho "
                    + "sẽ không kết nối được. Hãy tắt chương trình đang dùng cổng này hoặc đổi xemay.socket.port.",
                    congCauHinh, e.getMessage());
            return;
        }

        AtomicInteger soThuTu = new AtomicInteger();
        // core = max = số client tối đa, hàng đợi SynchronousQueue (không chứa phần tử nào):
        // còn luồng rảnh thì giao việc ngay, hết luồng thì execute() ném RejectedExecutionException.
        nhomLuong = new ThreadPoolExecutor(soClientToiDa, soClientToiDa, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                congViec -> {
                    Thread luong = new Thread(congViec, "kho-client-" + soThuTu.incrementAndGet());
                    luong.setDaemon(true);
                    return luong;
                });
        nhomLuong.allowCoreThreadTimeOut(true);   // luồng rảnh quá 60 giây thì được giải phóng

        dangChay = true;
        Thread luongChapNhan = new Thread(this::vongLapChapNhan, "kho-socket-chap-nhan");
        luongChapNhan.setDaemon(true);
        luongChapNhan.start();
        log.info("Socket server quản lý kho đang lắng nghe ở cổng {} (phục vụ tối đa {} client cùng lúc)",
                getCong(), soClientToiDa);
    }

    /** Chạy trên luồng kho-socket-chap-nhan: nhận kết nối mới rồi giao cho thread pool. */
    private void vongLapChapNhan() {
        while (dangChay) {
            Socket socket;
            try {
                socket = serverSocket.accept();   // chặn (block) cho đến khi có client kết nối
            } catch (IOException e) {
                if (dangChay && !serverSocket.isClosed()) {
                    log.warn("Lỗi khi chấp nhận kết nối: {}", e.getMessage());
                    continue;
                }
                break;   // stop() đã đóng serverSocket
            }
            tongSoKetNoi.incrementAndGet();
            try {
                nhomLuong.execute(new XuLyClient(socket, this, boXuLyLenh, thoiGianChoMs));
            } catch (RejectedExecutionException e) {
                tuChoi(socket);
            }
        }
    }

    /** Đã đủ số client tối đa: báo bận rồi đóng kết nối ngay. */
    private void tuChoi(Socket socket) {
        soKetNoiBiTuChoi.incrementAndGet();
        log.warn("Từ chối client {}: đã phục vụ đủ {} client", socket.getRemoteSocketAddress(), soClientToiDa);
        try (socket;
             BufferedWriter ghi = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            ghi.write(PhanHoi.loi(GiaoThuc.LOI_MAY_CHU_BAN,
                    "Máy chủ đang phục vụ tối đa " + soClientToiDa + " client, vui lòng thử lại sau.").thanhCacDong().get(0));
            ghi.write('\n');
            ghi.flush();
        } catch (IOException e) {
            log.debug("Không gửi được thông báo từ chối: {}", e.getMessage());
        }
    }

    /** Gọi bởi {@link XuLyClient} khi bắt đầu phục vụ. @return số client đang được phục vụ */
    int batDauPhucVu(Socket socket) {
        cacKetNoi.add(socket);
        return soClientDangPhucVu.incrementAndGet();
    }

    /** Gọi bởi {@link XuLyClient} khi kết thúc (trong finally). @return số client còn lại */
    int ketThucPhucVu(Socket socket) {
        cacKetNoi.remove(socket);
        return soClientDangPhucVu.decrementAndGet();
    }

    @Override
    public synchronized void stop() {
        if (!dangChay) {
            return;
        }
        dangChay = false;
        dong(serverSocket);                       // accept() đang chặn sẽ ném lỗi và vòng lặp kết thúc
        cacKetNoi.forEach(KhoSocketServer::dong); // read() đang chặn trong các luồng phục vụ cũng kết thúc
        nhomLuong.shutdown();
        try {
            if (!nhomLuong.awaitTermination(5, TimeUnit.SECONDS)) {
                nhomLuong.shutdownNow();
            }
        } catch (InterruptedException e) {
            nhomLuong.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Đã dừng Socket server quản lý kho");
    }

    @Override
    public boolean isRunning() {
        return dangChay;
    }

    private static void dong(Closeable taiNguyen) {
        try {
            if (taiNguyen != null) {
                taiNguyen.close();
            }
        } catch (IOException e) {
            log.debug("Lỗi khi đóng tài nguyên: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ Thông tin hiển thị trên trang quản trị

    /** Cổng đang lắng nghe (cấu hình cổng 0 thì hệ điều hành tự chọn một cổng trống - dùng khi kiểm thử). */
    public int getCong() {
        ServerSocket ss = serverSocket;
        return ss != null && ss.isBound() ? ss.getLocalPort() : congCauHinh;
    }

    public boolean isDangChay() {
        return dangChay;
    }

    public int getSoClientDangPhucVu() {
        return soClientDangPhucVu.get();
    }

    public long getTongSoKetNoi() {
        return tongSoKetNoi.get();
    }

    public long getSoKetNoiBiTuChoi() {
        return soKetNoiBiTuChoi.get();
    }

    public int getSoClientToiDa() {
        return soClientToiDa;
    }
}
