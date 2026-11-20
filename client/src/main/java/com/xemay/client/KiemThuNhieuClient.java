package com.xemay.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CHƯƠNG TRÌNH KIỂM THỬ ĐỒNG THỜI QUA MẠNG: nhiều client cùng lúc xuất kho CÙNG MỘT xe.
 *
 * <p>Mục đích: chứng minh (1) server phục vụ được nhiều client song song và (2) tồn kho luôn đúng dù nhiều
 * giao dịch trừ kho chạy cùng lúc. Kết quả đúng khi:
 * <ul>
 *   <li>Số lần xuất kho thành công = tồn kho trước - tồn kho sau (không mất lần trừ nào).</li>
 *   <li>Tồn kho sau không âm (không bán vượt số xe đang có).</li>
 * </ul>
 * Cuối bài kiểm thử, chương trình nhập lại đúng số xe đã xuất để có thể chạy lại nhiều lần.
 *
 * <pre>
 * Chạy:  java -cp xemay-client.jar com.xemay.client.KiemThuNhieuClient
 *            [máy chủ] [cổng] [tên đăng nhập] [mật khẩu] [mã xe] [số client] [số lần xuất mỗi client]
 * Mặc định: localhost 9090 nhanvien 123456 1 5 4
 * </pre>
 */
public class KiemThuNhieuClient {

    public static void main(String[] args) throws Exception {
        String mayChu = thamSo(args, 0, "localhost");
        int cong = Integer.parseInt(thamSo(args, 1, "9090"));
        String ten = thamSo(args, 2, "nhanvien");
        String matKhau = thamSo(args, 3, "123456");
        long maXe = Long.parseLong(thamSo(args, 4, "1"));
        int soClient = Integer.parseInt(thamSo(args, 5, "5"));
        int soLanMoiClient = Integer.parseInt(thamSo(args, 6, "4"));

        System.out.println("=== KIỂM THỬ " + soClient + " CLIENT CÙNG LÚC XUẤT KHO XE #" + maXe + " ===");

        // Kết nối giám sát: đọc tồn kho trước và sau khi kiểm thử
        try (KetNoiKho giamSat = moKetNoi(mayChu, cong, ten, matKhau)) {
            int tonTruoc = docTonKho(giamSat, maXe);
            System.out.println("Tồn kho trước kiểm thử : " + tonTruoc);
            System.out.println("Tổng số yêu cầu xuất kho: " + soClient + " client x " + soLanMoiClient
                    + " lần x 1 xe = " + (soClient * soLanMoiClient));

            AtomicInteger thanhCong = new AtomicInteger();
            AtomicInteger khongDuHang = new AtomicInteger();
            AtomicInteger loiKhac = new AtomicInteger();
            CountDownLatch sanSang = new CountDownLatch(soClient);
            CountDownLatch xuatPhat = new CountDownLatch(1);

            ExecutorService nhomLuong = Executors.newFixedThreadPool(soClient);
            List<Future<?>> ketQua = new ArrayList<>();
            for (int i = 1; i <= soClient; i++) {
                int soThuTu = i;
                ketQua.add(nhomLuong.submit(() -> {
                    try (KetNoiKho ketNoi = moKetNoi(mayChu, cong, ten, matKhau)) {
                        sanSang.countDown();
                        xuatPhat.await();   // mọi client cùng bắt đầu một lúc để tăng khả năng tranh chấp
                        for (int lan = 0; lan < soLanMoiClient; lan++) {
                            PhanHoiServer kq = ketNoi.gui("XUATKHO " + maXe + " 1");
                            if (kq.thanhCong()) {
                                thanhCong.incrementAndGet();
                            } else if (kq.maLoi() == 409) {
                                khongDuHang.incrementAndGet();
                            } else {
                                loiKhac.incrementAndGet();
                                System.out.println("  Client " + soThuTu + " nhận lỗi: " + kq.maLoi() + " " + kq.thongBao());
                            }
                        }
                        ketNoi.gui("THOAT");
                    }
                    return null;
                }));
            }

            if (!sanSang.await(30, TimeUnit.SECONDS)) {
                System.out.println("Không mở đủ " + soClient + " kết nối (server giới hạn số client?).");
            }
            long batDau = System.nanoTime();
            xuatPhat.countDown();
            for (Future<?> f : ketQua) {
                try {
                    f.get(2, TimeUnit.MINUTES);
                } catch (Exception e) {
                    loiKhac.incrementAndGet();
                    System.out.println("  Một client gặp lỗi: " + e.getCause());
                }
            }
            nhomLuong.shutdown();
            long thoiGianMs = (System.nanoTime() - batDau) / 1_000_000;

            int tonSau = docTonKho(giamSat, maXe);
            int kyVong = Math.min(tonTruoc, soClient * soLanMoiClient);
            // (1) Không mất lần trừ nào: số xe giảm đúng bằng số lần xuất thành công
            boolean khongMatCapNhat = tonTruoc - tonSau == thanhCong.get();
            // (2) Không bán vượt: tồn kho không bao giờ âm
            boolean khongBanVuot = tonSau >= 0;
            // (3) Không từ chối oan: còn hàng thì phải xuất được, nên số lần thành công = min(tồn trước, số yêu cầu)
            boolean khongTuChoiOan = thanhCong.get() == kyVong;
            boolean dung = khongMatCapNhat && khongBanVuot && khongTuChoiOan && loiKhac.get() == 0;

            System.out.println();
            System.out.println("Thời gian chạy          : " + thoiGianMs + " ms");
            System.out.println("Xuất kho thành công     : " + thanhCong.get() + " (kỳ vọng " + kyVong + ")");
            System.out.println("Bị từ chối do hết hàng  : " + khongDuHang.get());
            System.out.println("Lỗi khác                : " + loiKhac.get());
            System.out.println("Tồn kho sau kiểm thử    : " + tonSau);
            System.out.println("[" + (khongMatCapNhat ? "ĐẠT" : "SAI") + "] Tồn trước - tồn sau (" + (tonTruoc - tonSau)
                    + ") = số lần thành công (" + thanhCong.get() + ")");
            System.out.println("[" + (khongBanVuot ? "ĐẠT" : "SAI") + "] Tồn kho sau không âm");
            System.out.println("[" + (khongTuChoiOan ? "ĐẠT" : "SAI") + "] Còn hàng thì xuất được (không từ chối oan)");
            System.out.println(dung ? "==> KẾT QUẢ ĐÚNG: không mất lần trừ kho nào, không bán vượt tồn kho."
                                    : "==> KẾT QUẢ CHƯA ĐẠT: xem các dòng [SAI] hoặc lỗi khác ở trên.");

            if (thanhCong.get() > 0) {
                PhanHoiServer hoan = giamSat.gui("NHAPKHO " + maXe + " " + thanhCong.get());
                System.out.println(hoan.thanhCong()
                        ? "Đã nhập lại " + thanhCong.get() + " xe để khôi phục tồn kho ban đầu (" + tonTruoc + ")."
                        : "Không khôi phục được tồn kho: " + hoan.thongBao());
            }
            giamSat.gui("THOAT");
            if (!dung) {
                System.exit(1);
            }
        }
    }

    private static KetNoiKho moKetNoi(String mayChu, int cong, String ten, String matKhau) throws IOException {
        KetNoiKho ketNoi = new KetNoiKho(mayChu, cong);
        try {
            PhanHoiServer chao = ketNoi.docLoiChao();
            if (!chao.thanhCong()) {
                throw new IOException("Server từ chối: " + chao.thongBao());
            }
            PhanHoiServer dangNhap = ketNoi.gui("DANGNHAP " + ten + " " + matKhau);
            if (!dangNhap.thanhCong()) {
                throw new IOException("Đăng nhập thất bại: " + dangNhap.thongBao());
            }
            return ketNoi;
        } catch (IOException e) {
            ketNoi.close();
            throw e;
        }
    }

    private static int docTonKho(KetNoiKho ketNoi, long maXe) throws IOException {
        PhanHoiServer kq = ketNoi.gui("TONKHO " + maXe);
        if (!kq.thanhCong()) {
            throw new IOException("Không đọc được tồn kho: " + kq.thongBao());
        }
        return Integer.parseInt(PhanHoiServer.tachCot(kq.dongDau())[3]);
    }

    private static String thamSo(String[] args, int viTri, String macDinh) {
        return args.length > viTri ? args[viTri] : macDinh;
    }
}
