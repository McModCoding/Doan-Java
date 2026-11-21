package com.xemay.service;

import com.xemay.KiemThuTichHop;
import com.xemay.entity.XeMay;
import com.xemay.exception.HetHangException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KIỂM THỬ ĐỒNG THỜI cho tồn kho: nhiều luồng cùng nhập/xuất kho một xe trong cùng một lúc.
 * Kết quả các bài này dùng cho Chương 4 của báo cáo (kiểm thử đồng thời).
 */
class TonKhoDongThoiTest extends KiemThuTichHop {

    private static final Logger log = LoggerFactory.getLogger(TonKhoDongThoiTest.class);

    @Autowired
    private TonKhoService tonKhoService;

    @Test
    @DisplayName("20 luồng cùng mua 1 xe khi kho còn 10 -> đúng 10 thành công, 10 hết hàng, tồn kho 0")
    void xuatKhoDongThoiKhongBanVuot() throws Exception {
        XeMay xe = taoXe(10);
        int soLuong = 20;
        AtomicInteger thanhCong = new AtomicInteger();
        AtomicInteger hetHang = new AtomicInteger();
        Set<String> tenCacLuong = ConcurrentHashMap.newKeySet();

        chayDongThoi(soLuong, () -> {
            tenCacLuong.add(Thread.currentThread().getName());
            try {
                tonKhoService.xuatKho(xe.getMaXe(), 1);
                thanhCong.incrementAndGet();
            } catch (HetHangException e) {
                hetHang.incrementAndGet();
            }
        });

        log.info("CÓ KHÓA   : {} luồng mua, thành công {}, hết hàng {}, tồn kho cuối {} (đúng phải là 0)",
                soLuong, thanhCong.get(), hetHang.get(), tonKho(xe.getMaXe()));
        assertThat(tenCacLuong).hasSize(soLuong);   // thật sự chạy trên 20 luồng khác nhau
        assertThat(thanhCong.get()).isEqualTo(10);
        assertThat(hetHang.get()).isEqualTo(10);
        assertThat(tonKho(xe.getMaXe())).isZero();
    }

    @Test
    @DisplayName("SO SÁNH - KHÔNG đồng bộ: 20 luồng cùng đọc 10 rồi cùng ghi 9 -> bán 20 xe nhưng kho chỉ giảm 1")
    void khongDongBoThiBanVuotVaMatCapNhat() throws Exception {
        XeMay xe = taoXe(10);
        int soLuong = 20;
        AtomicInteger thanhCong = new AtomicInteger();
        // Hàng rào: buộc mọi luồng đọc xong rồi mới ghi - tình huống xấu nhất có thể xảy ra khi không khóa
        CyclicBarrier docXongHet = new CyclicBarrier(soLuong);

        chayDongThoi(soLuong, () -> {
            XeMay banDoc = xeMayRepository.findById(xe.getMaXe()).orElseThrow();   // (1) đọc, KHÔNG khóa
            docXongHet.await(10, TimeUnit.SECONDS);
            if (banDoc.getSoLuong() >= 1) {                                        // (2) kiểm tra trên dữ liệu cũ
                banDoc.setSoLuong(banDoc.getSoLuong() - 1);
                xeMayRepository.save(banDoc);                                      // (3) ghi đè lên nhau
                thanhCong.incrementAndGet();
            }
        });

        int tonCuoi = tonKho(xe.getMaXe());
        log.info("KHÔNG KHÓA: {} luồng mua, thành công {}, tồn kho cuối {} -> bán vượt {} xe, mất {} lần cập nhật",
                soLuong, thanhCong.get(), tonCuoi, thanhCong.get() - 10, thanhCong.get() - (10 - tonCuoi));
        // Đây là LỖI mà TonKhoService ngăn chặn: 20 khách đều mua được trong khi kho chỉ có 10 xe
        assertThat(thanhCong.get()).isEqualTo(20);
        assertThat(tonCuoi).isEqualTo(9);
    }

    @Test
    @DisplayName("25 luồng nhập kho và 25 luồng xuất kho cùng lúc -> tồn kho cuối bằng tồn kho đầu")
    void nhapVaXuatDongThoi() throws Exception {
        XeMay xe = taoXe(50);
        AtomicInteger soLuot = new AtomicInteger();

        chayDongThoi(50, () -> {
            if (soLuot.incrementAndGet() % 2 == 0) {
                tonKhoService.nhapKho(xe.getMaXe(), 1);
            } else {
                tonKhoService.xuatKho(xe.getMaXe(), 1);
            }
        });

        assertThat(tonKho(xe.getMaXe())).isEqualTo(50);
    }

    @Test
    @DisplayName("Đơn nhiều xe khóa theo thứ tự ngược nhau -> không deadlock, tồn kho đúng")
    void xuatNhieuXeKhongDeadlock() throws Exception {
        XeMay xeA = taoXe(100);
        XeMay xeB = taoXe(100);
        AtomicInteger soLuot = new AtomicInteger();

        // 20 luồng x 5 đơn; nửa số đơn ghi xe A trước, nửa còn lại ghi xe B trước
        chayDongThoi(20, () -> {
            boolean aTruoc = soLuot.incrementAndGet() % 2 == 0;
            for (int lan = 0; lan < 5; lan++) {
                Map<Integer, Integer> donHang = new LinkedHashMap<>();
                if (aTruoc) {
                    donHang.put(xeA.getMaXe(), 1);
                    donHang.put(xeB.getMaXe(), 1);
                } else {
                    donHang.put(xeB.getMaXe(), 1);
                    donHang.put(xeA.getMaXe(), 1);
                }
                tonKhoService.xuatKhoNhieuXe(donHang);
            }
        });

        assertThat(tonKho(xeA.getMaXe())).isZero();
        assertThat(tonKho(xeB.getMaXe())).isZero();
    }

    @Test
    @DisplayName("Đơn nhiều xe thiếu hàng ở 1 xe -> không trừ xe nào (giao dịch bị hủy toàn bộ)")
    void xuatNhieuXeThieuHangThiHuyCaDon() {
        XeMay xeDu = taoXe(5);
        XeMay xeThieu = taoXe(1);
        Map<Integer, Integer> donHang = new LinkedHashMap<>();
        donHang.put(xeDu.getMaXe(), 2);
        donHang.put(xeThieu.getMaXe(), 3);

        assertThatThrownBy(() -> tonKhoService.xuatKhoNhieuXe(donHang)).isInstanceOf(HetHangException.class);
        assertThat(tonKho(xeDu.getMaXe())).isEqualTo(5);
        assertThat(tonKho(xeThieu.getMaXe())).isEqualTo(1);
    }

    @Test
    @DisplayName("Kiểm tra dữ liệu: số lượng không hợp lệ, xe không tồn tại, không đủ hàng")
    void kiemTraDuLieu() {
        XeMay xe = taoXe(3);
        assertThatThrownBy(() -> tonKhoService.xuatKho(xe.getMaXe(), 0)).isInstanceOf(NghiepVuException.class);
        assertThatThrownBy(() -> tonKhoService.nhapKho(xe.getMaXe(), -5)).isInstanceOf(NghiepVuException.class);
        assertThatThrownBy(() -> tonKhoService.xuatKho(999_999, 1)).isInstanceOf(KhongTimThayException.class);
        assertThatThrownBy(() -> tonKhoService.xuatKho(xe.getMaXe(), 4))
                .isInstanceOf(HetHangException.class)
                .hasMessageContaining("chỉ còn 3 chiếc");
        assertThat(tonKho(xe.getMaXe())).isEqualTo(3);
    }

    @Test
    @DisplayName("Tác vụ định kỳ quét được xe sắp hết hàng")
    void quetXeSapHetHang() {
        XeMay xe = taoXe(0);
        assertThat(tonKhoService.quetXeSapHetHang().danhSach())
                .anySatisfy(muc -> assertThat(muc.id()).isEqualTo(xe.getMaXe()));
        assertThat(tonKhoService.getCanhBaoGanNhat().thoiDiemQuet()).isNotNull();
    }

    // ------------------------------------------------------------------ Hàm phụ

    @FunctionalInterface
    private interface CongViec {
        void chay() throws Exception;
    }

    /** Chạy cùng một công việc trên {@code soLuong} luồng, cho tất cả XUẤT PHÁT CÙNG LÚC, rồi chờ xong hết. */
    private static void chayDongThoi(int soLuong, CongViec congViec) throws Exception {
        ExecutorService nhomLuong = Executors.newFixedThreadPool(soLuong);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        var ketQua = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < soLuong; i++) {
            ketQua.add(nhomLuong.submit(() -> {
                xuatPhat.await();
                congViec.chay();
                return null;
            }));
        }
        xuatPhat.countDown();
        for (var f : ketQua) {
            f.get(60, TimeUnit.SECONDS);   // ném lỗi nếu một luồng gặp ngoại lệ hoặc bị treo (deadlock)
        }
        nhomLuong.shutdown();
    }
}
