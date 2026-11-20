package com.xemay.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Một kết nối TCP tới Socket server quản lý kho, đóng gói phần đọc/ghi theo giao thức:
 * <pre>
 * Gửi:  1 dòng lệnh, ví dụ "XUATKHO 3 1"
 * Nhận: "OK n" rồi n dòng dữ liệu, hoặc "ERR mã thông-báo"
 * </pre>
 * Dùng với try-with-resources để socket luôn được đóng:
 * <pre>
 * try (KetNoiKho ketNoi = new KetNoiKho("localhost", 9090)) {
 *     ketNoi.docLoiChao();
 *     PhanHoiServer kq = ketNoi.gui("DANGNHAP nhanvien 123456");
 * }
 * </pre>
 */
public class KetNoiKho implements Closeable {

    /** Chờ kết nối tối đa 5 giây, chờ câu trả lời tối đa 30 giây. */
    public static final int THOI_GIAN_CHO_KET_NOI_MS = 5_000;
    public static final int THOI_GIAN_CHO_TRA_LOI_MS = 30_000;

    private final Socket socket;
    private final BufferedReader doc;
    private final BufferedWriter ghi;

    public KetNoiKho(String mayChu, int cong) throws IOException {
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(mayChu, cong), THOI_GIAN_CHO_KET_NOI_MS);
            socket.setSoTimeout(THOI_GIAN_CHO_TRA_LOI_MS);
            doc = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            ghi = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            socket.close();   // không để rò rỉ socket khi kết nối thất bại giữa chừng
            throw e;
        }
    }

    /** Đọc lời chào server gửi ngay sau khi kết nối (hoặc ERR 503 nếu server đang quá tải). */
    public PhanHoiServer docLoiChao() throws IOException {
        return docPhanHoi();
    }

    /** Gửi một dòng lệnh và chờ câu trả lời. */
    public PhanHoiServer gui(String lenh) throws IOException {
        ghi.write(lenh.replace('\n', ' ').replace('\r', ' '));
        ghi.write('\n');
        ghi.flush();
        return docPhanHoi();
    }

    private PhanHoiServer docPhanHoi() throws IOException {
        String dauTien = docDong();
        if (dauTien.startsWith("OK ")) {
            int soDong;
            try {
                soDong = Integer.parseInt(dauTien.substring(3).trim());
            } catch (NumberFormatException e) {
                throw new IOException("Phản hồi không đúng giao thức: " + dauTien);
            }
            List<String> duLieu = new ArrayList<>(soDong);
            for (int i = 0; i < soDong; i++) {
                duLieu.add(docDong());
            }
            return new PhanHoiServer(true, 0, null, duLieu);
        }
        if (dauTien.startsWith("ERR ")) {
            String[] phan = dauTien.split(" ", 3);
            int maLoi;
            try {
                maLoi = Integer.parseInt(phan[1]);
            } catch (NumberFormatException e) {
                maLoi = 0;
            }
            return new PhanHoiServer(false, maLoi, phan.length > 2 ? phan[2] : "", List.of());
        }
        throw new IOException("Phản hồi không đúng giao thức: " + dauTien);
    }

    private String docDong() throws IOException {
        String dong = doc.readLine();
        if (dong == null) {
            throw new EOFException("Máy chủ đã đóng kết nối");
        }
        return dong;
    }

    @Override
    public void close() throws IOException {
        socket.close();   // đóng socket cũng đóng luôn luồng đọc và luồng ghi
    }
}
