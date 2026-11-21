package com.xemay.network;

import com.xemay.KiemThuTichHop;
import com.xemay.entity.XeMay;
import com.xemay.security.BoDemDangNhapSai;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KIỂM THỬ PHẦN MẠNG: kết nối tới Socket server thật (chạy ở cổng ngẫu nhiên trong lúc kiểm thử)
 * và trao đổi đúng theo giao thức như chương trình client.
 */
class KhoSocketServerTest extends KiemThuTichHop {

    @Autowired
    private KhoSocketServer server;

    @Autowired
    private BoDemDangNhapSai boDemDangNhapSai;

    @BeforeEach
    void choServerRanh() throws InterruptedException {
        boDemDangNhapSai.xoa("nhanvien");
        boDemDangNhapSai.xoa("khachhang");
        // Các kết nối của bài kiểm thử trước được server giải phóng bất đồng bộ - chờ server rảnh hẳn
        long hanChot = System.currentTimeMillis() + 5_000;
        while (server.getSoClientDangPhucVu() > 0 && System.currentTimeMillis() < hanChot) {
            Thread.sleep(20);
        }
        Thread.sleep(50);
    }

    @Test
    @DisplayName("Kết nối xong nhận được lời chào; chưa đăng nhập thì không dùng được lệnh nào")
    void chaoVaYeuCauDangNhap() throws IOException {
        try (KetNoiThu kn = new KetNoiThu(server.getCong())) {
            assertThat(kn.loiChao).containsExactly("OK 1", GiaoThuc.LOI_CHAO);
            assertThat(kn.gui("DSXE")).first().asString().startsWith("ERR 401");
            assertThat(kn.gui("XUATKHO 1 1")).first().asString().startsWith("ERR 401");
        }
    }

    @Test
    @DisplayName("Khách hàng đúng mật khẩu vẫn không được dùng client kho (403)")
    void khachHangKhongCoQuyen() throws IOException {
        try (KetNoiThu kn = new KetNoiThu(server.getCong())) {
            assertThat(kn.gui("DANGNHAP khachhang 123456")).first().asString().startsWith("ERR 403");
            assertThat(kn.gui("DSXE")).first().asString().startsWith("ERR 401");
        }
    }

    @Test
    @DisplayName("Đăng nhập sai 3 lần trong một kết nối -> server ngắt kết nối")
    void saiBaLanBiNgat() throws IOException {
        try (KetNoiThu kn = new KetNoiThu(server.getCong())) {
            assertThat(kn.gui("DANGNHAP khongtontai sai1")).first().asString().startsWith("ERR 401");
            assertThat(kn.gui("DANGNHAP khongtontai sai2")).first().asString().startsWith("ERR 401");
            assertThat(kn.gui("DANGNHAP khongtontai sai3")).first().asString()
                    .startsWith("ERR 401").contains("ngắt kết nối");
            assertThat(kn.docDong()).as("server đã đóng kết nối").isNull();
        }
    }

    @Test
    @DisplayName("Nhân viên: xem, nhập, xuất kho; xử lý lỗi cú pháp, không tìm thấy, không đủ hàng")
    void nhanVienQuanLyKho() throws IOException {
        XeMay xe = taoXe(5);
        int id = xe.getMaXe();
        try (KetNoiThu kn = new KetNoiThu(server.getCong())) {
            assertThat(kn.gui("DANGNHAP nhanvien 123456")).containsExactly("OK 1", "Nhân viên bán hàng|NHANVIEN");

            assertThat(kn.gui("TONKHO " + id)).containsExactly("OK 1", id + "|" + xe.getTenDayDu() + "|30000000|5");
            assertThat(kn.gui("xuatkho " + id + " 2")).containsExactly("OK 1", id + "|" + xe.getTenDayDu() + "|30000000|3");
            assertThat(kn.gui("NHAPKHO " + id + " 10")).containsExactly("OK 1", id + "|" + xe.getTenDayDu() + "|30000000|13");

            assertThat(kn.gui("XUATKHO " + id + " 100")).first().asString().startsWith("ERR 409");
            assertThat(kn.gui("TONKHO 999999")).first().asString().startsWith("ERR 404");
            assertThat(kn.gui("XUATKHO abc 1")).first().asString().startsWith("ERR 400");
            assertThat(kn.gui("XUATKHO " + id + " 0")).first().asString().startsWith("ERR 400");
            assertThat(kn.gui("LENHLA")).first().asString().startsWith("ERR 400");
            assertThat(kn.gui("DSXE " + xe.getTenXe())).hasSize(2).first().isEqualTo("OK 1");

            assertThat(kn.gui("THOAT")).containsExactly("OK 0");
            assertThat(kn.docDong()).as("server đóng kết nối sau THOAT").isNull();
        }
        assertThat(tonKho(id)).isEqualTo(13);
    }

    @Test
    @DisplayName("ĐA LUỒNG QUA MẠNG: 3 client cùng xuất kho 1 xe (15 yêu cầu, kho có 12) -> 12 OK, 3 hết hàng, tồn 0")
    void baClientCungLuc() throws Exception {
        XeMay xe = taoXe(12);
        int soClient = 3;
        int soLanMoiClient = 5;
        AtomicInteger thanhCong = new AtomicInteger();
        AtomicInteger hetHang = new AtomicInteger();
        CountDownLatch xuatPhat = new CountDownLatch(1);

        ExecutorService nhomLuong = Executors.newFixedThreadPool(soClient);
        List<Future<?>> ketQua = new ArrayList<>();
        for (int i = 0; i < soClient; i++) {
            ketQua.add(nhomLuong.submit(() -> {
                try (KetNoiThu kn = new KetNoiThu(server.getCong())) {
                    assertThat(kn.gui("DANGNHAP nhanvien 123456")).first().isEqualTo("OK 1");
                    xuatPhat.await();
                    for (int lan = 0; lan < soLanMoiClient; lan++) {
                        String dau = kn.gui("XUATKHO " + xe.getMaXe() + " 1").get(0);
                        if (dau.startsWith("OK")) {
                            thanhCong.incrementAndGet();
                        } else if (dau.startsWith("ERR 409")) {
                            hetHang.incrementAndGet();
                        }
                    }
                }
                return null;
            }));
        }
        xuatPhat.countDown();
        for (Future<?> f : ketQua) {
            f.get(60, TimeUnit.SECONDS);
        }
        nhomLuong.shutdown();

        assertThat(thanhCong.get()).isEqualTo(12);
        assertThat(hetHang.get()).isEqualTo(3);
        assertThat(tonKho(xe.getMaXe())).isZero();
    }

    @Test
    @DisplayName("Đủ số client tối đa (5) -> client thứ 6 nhận ERR 503; một client thoát thì client mới vào được")
    void vuotSoClientToiDa() throws Exception {
        List<KetNoiThu> dangMo = new ArrayList<>();
        try {
            for (int i = 0; i < server.getSoClientToiDa(); i++) {
                dangMo.add(new KetNoiThu(server.getCong()));   // đọc được lời chào = đã có luồng phục vụ
            }
            try (KetNoiThu thuSau = new KetNoiThu(server.getCong())) {
                assertThat(thuSau.loiChao).first().asString().startsWith("ERR 503");
                assertThat(thuSau.docDong()).isNull();
            }

            dangMo.remove(0).close();
            List<String> loiChao = List.of();
            for (int lan = 0; lan < 50 && !loiChao.contains(GiaoThuc.LOI_CHAO); lan++) {
                Thread.sleep(50);   // chờ server giải phóng luồng của client vừa thoát
                try (KetNoiThu moi = new KetNoiThu(server.getCong())) {
                    loiChao = moi.loiChao;
                }
            }
            assertThat(loiChao).contains(GiaoThuc.LOI_CHAO);
        } finally {
            for (KetNoiThu kn : dangMo) {
                kn.close();
            }
        }
    }

    @Test
    @DisplayName("Dòng lệnh quá dài -> báo lỗi và ngắt kết nối (chống làm cạn bộ nhớ server)")
    void dongLenhQuaDai() throws IOException {
        try (KetNoiThu kn = new KetNoiThu(server.getCong())) {
            assertThat(kn.gui("DSXE " + "x".repeat(GiaoThuc.DO_DAI_DONG_TOI_DA + 10)))
                    .first().asString().startsWith("ERR 400");
            assertThat(kn.docDong()).isNull();
        }
    }

    /** Client tối giản dùng trong kiểm thử: gửi một dòng, đọc câu trả lời "OK n" + n dòng hoặc "ERR ...". */
    private static final class KetNoiThu implements Closeable {
        private final Socket socket;
        private final BufferedReader doc;
        private final BufferedWriter ghi;
        private final List<String> loiChao;

        KetNoiThu(int cong) throws IOException {
            socket = new Socket("localhost", cong);
            socket.setSoTimeout(10_000);
            doc = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            ghi = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            loiChao = docPhanHoi();
        }

        List<String> gui(String lenh) throws IOException {
            ghi.write(lenh);
            ghi.write('\n');
            ghi.flush();
            return docPhanHoi();
        }

        String docDong() throws IOException {
            try {
                return doc.readLine();
            } catch (java.net.SocketException e) {
                return null;   // server đóng kết nối kiểu "reset" cũng tính là đã đóng
            }
        }

        private List<String> docPhanHoi() throws IOException {
            List<String> cacDong = new ArrayList<>();
            String dau = doc.readLine();
            cacDong.add(dau);
            if (dau != null && dau.startsWith("OK ")) {
                int n = Integer.parseInt(dau.substring(3).trim());
                for (int i = 0; i < n; i++) {
                    cacDong.add(doc.readLine());
                }
            }
            return cacDong;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
