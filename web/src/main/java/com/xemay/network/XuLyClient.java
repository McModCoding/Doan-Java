package com.xemay.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Phục vụ MỘT client trong suốt thời gian kết nối. Mỗi đối tượng chạy trên một luồng riêng của
 * thread pool trong {@link KhoSocketServer}, nên nhiều client được phục vụ song song.
 *
 * <p>Vòng đời: gửi lời chào -> lặp (đọc 1 dòng lệnh, xử lý, trả lời) -> đóng kết nối khi client gửi THOAT,
 * client tự ngắt, client im lặng quá lâu, hoặc server dừng. Socket và luồng vào/ra luôn được đóng trong
 * try-with-resources, kể cả khi có lỗi.
 */
class XuLyClient implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(XuLyClient.class);

    private final Socket socket;
    private final KhoSocketServer server;
    private final BoXuLyLenh boXuLyLenh;
    private final int thoiGianChoMs;

    XuLyClient(Socket socket, KhoSocketServer server, BoXuLyLenh boXuLyLenh, int thoiGianChoMs) {
        this.socket = socket;
        this.server = server;
        this.boXuLyLenh = boXuLyLenh;
        this.thoiGianChoMs = thoiGianChoMs;
    }

    @Override
    public void run() {
        String diaChi = socket.getRemoteSocketAddress().toString();
        int soClient = server.batDauPhucVu(socket);
        log.info("Client {} đã kết nối [luồng phục vụ: {}] - đang phục vụ {} client",
                diaChi, Thread.currentThread().getName(), soClient);

        PhienKetNoi phien = new PhienKetNoi(socket.getInetAddress().getHostAddress());
        Socket ketNoi = socket;
        try (ketNoi;
             BufferedReader doc = new BufferedReader(
                     new InputStreamReader(ketNoi.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter ghi = new BufferedWriter(
                     new OutputStreamWriter(ketNoi.getOutputStream(), StandardCharsets.UTF_8))) {

            // Client không gửi gì trong khoảng thời gian này thì read() ném SocketTimeoutException
            ketNoi.setSoTimeout(thoiGianChoMs);
            gui(ghi, PhanHoi.ok(GiaoThuc.LOI_CHAO));

            while (true) {
                String dong;
                try {
                    dong = docDong(doc, GiaoThuc.DO_DAI_DONG_TOI_DA);
                } catch (SocketTimeoutException e) {
                    gui(ghi, PhanHoi.loi(GiaoThuc.LOI_HET_THOI_GIAN,
                            "Quá " + thoiGianChoMs / 1000 + " giây không nhận được lệnh, máy chủ ngắt kết nối."));
                    log.info("Client {} im lặng quá lâu, ngắt kết nối", diaChi);
                    break;
                } catch (DongQuaDaiException e) {
                    gui(ghi, PhanHoi.loi(GiaoThuc.LOI_CU_PHAP, "Dòng lệnh dài quá "
                            + GiaoThuc.DO_DAI_DONG_TOI_DA + " ký tự, máy chủ ngắt kết nối."));
                    break;
                }
                if (dong == null) {
                    break;   // client đã đóng kết nối
                }
                PhanHoi phanHoi = boXuLyLenh.xuLy(dong, phien);
                gui(ghi, phanHoi);
                if (phanHoi.dongKetNoi()) {
                    break;
                }
            }
        } catch (IOException e) {
            // Client tắt ngang, mất mạng, hoặc server đang dừng và đã đóng socket
            log.info("Kết nối với client {} bị gián đoạn: {}", diaChi, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Lỗi không mong muốn khi phục vụ client {}", diaChi, e);
        } finally {
            int conLai = server.ketThucPhucVu(socket);
            log.info("Client {} đã ngắt kết nối [luồng: {}] - còn {} client",
                    diaChi, Thread.currentThread().getName(), conLai);
        }
    }

    private static void gui(Writer ghi, PhanHoi phanHoi) throws IOException {
        for (String dong : phanHoi.thanhCacDong()) {
            ghi.write(dong);
            ghi.write('\n');
        }
        ghi.flush();
    }

    /**
     * Đọc một dòng (kết thúc bằng \n, bỏ \r) nhưng không quá {@code toiDa} ký tự.
     * Không dùng {@code readLine()} vì nó chấp nhận dòng dài vô hạn: client xấu có thể gửi một dòng
     * hàng trăm MB không có dấu xuống dòng để làm cạn bộ nhớ của server.
     *
     * @return nội dung dòng, hoặc {@code null} nếu client đã đóng kết nối
     */
    static String docDong(Reader doc, int toiDa) throws IOException {
        StringBuilder dong = new StringBuilder();
        int kyTu;
        while ((kyTu = doc.read()) != -1) {
            if (kyTu == '\n') {
                return dong.toString();
            }
            if (kyTu != '\r') {
                if (dong.length() >= toiDa) {
                    throw new DongQuaDaiException();
                }
                dong.append((char) kyTu);
            }
        }
        return dong.isEmpty() ? null : dong.toString();
    }

    /** Client gửi một dòng dài quá giới hạn của giao thức. */
    static class DongQuaDaiException extends IOException {
        DongQuaDaiException() {
            super("Dòng lệnh quá dài");
        }
    }
}
