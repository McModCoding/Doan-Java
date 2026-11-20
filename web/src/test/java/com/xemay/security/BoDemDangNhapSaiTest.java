package com.xemay.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm thử đơn vị cho bộ đếm đăng nhập sai (không cần khởi động Spring). */
class BoDemDangNhapSaiTest {

    @Test
    @DisplayName("Sai 4 lần chưa bị khóa, sai lần thứ 5 thì bị tạm khóa")
    void saiDuSoLanThiBiKhoa() {
        BoDemDangNhapSai boDem = new BoDemDangNhapSai(5, Duration.ofMinutes(15));
        for (int i = 0; i < 4; i++) {
            boDem.ghiNhanSai("an");
        }
        assertThat(boDem.dangBiKhoa("an")).isFalse();
        assertThat(boDem.soLanSai("an")).isEqualTo(4);

        boDem.ghiNhanSai("an");
        assertThat(boDem.dangBiKhoa("an")).isTrue();
    }

    @Test
    @DisplayName("Tên đăng nhập không phân biệt hoa thường và khoảng trắng hai đầu")
    void khongPhanBietHoaThuong() {
        BoDemDangNhapSai boDem = new BoDemDangNhapSai(2, Duration.ofMinutes(15));
        boDem.ghiNhanSai("Admin");
        boDem.ghiNhanSai(" admin ");
        assertThat(boDem.dangBiKhoa("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("Xóa bộ đếm (đăng nhập đúng / quản trị viên mở khóa) thì hết bị khóa")
    void xoaThiMoKhoa() {
        BoDemDangNhapSai boDem = new BoDemDangNhapSai(1, Duration.ofMinutes(15));
        boDem.ghiNhanSai("binh");
        assertThat(boDem.dangBiKhoa("binh")).isTrue();
        boDem.xoa("binh");
        assertThat(boDem.dangBiKhoa("binh")).isFalse();
        assertThat(boDem.soLanSai("binh")).isZero();
    }

    @Test
    @DisplayName("Hết thời gian khóa thì tự mở; tác vụ dọn dẹp xóa các mục đã hết hạn")
    void hetHanThiTuMoVaDuocDonDep() throws InterruptedException {
        BoDemDangNhapSai boDem = new BoDemDangNhapSai(1, Duration.ofMillis(100));
        boDem.ghiNhanSai("chi");
        assertThat(boDem.dangBiKhoa("chi")).isTrue();

        Thread.sleep(150);
        assertThat(boDem.dangBiKhoa("chi")).isFalse();
        assertThat(boDem.donDep()).isEqualTo(1);
    }

    @Test
    @DisplayName("ĐA LUỒNG: 50 luồng x 200 lần ghi nhận sai cùng một tên -> đếm đủ 10.000 lần, không mất lần nào")
    void nhieuLuongCungGhiNhanKhongMatLanDem() throws InterruptedException {
        int soLuong = 50;
        int soLanMoiLuong = 200;
        // Ngưỡng rất lớn để chỉ kiểm tra việc đếm
        BoDemDangNhapSai boDem = new BoDemDangNhapSai(Integer.MAX_VALUE, Duration.ofMinutes(15));

        ExecutorService nhomLuong = Executors.newFixedThreadPool(soLuong);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        for (int i = 0; i < soLuong; i++) {
            nhomLuong.submit(() -> {
                xuatPhat.await();
                for (int lan = 0; lan < soLanMoiLuong; lan++) {
                    boDem.ghiNhanSai("dung");
                }
                return null;
            });
        }
        xuatPhat.countDown();
        nhomLuong.shutdown();
        assertThat(nhomLuong.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Với HashMap thường + "đọc, cộng 1, ghi lại", kết quả thường nhỏ hơn 10.000 do các luồng ghi đè lên nhau
        assertThat(boDem.soLanSai("dung")).isEqualTo(soLuong * soLanMoiLuong);
    }
}
