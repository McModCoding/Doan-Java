package com.xemay.controller;

import com.xemay.KiemThuTichHop;
import com.xemay.entity.XeMay;
import com.xemay.security.TaiKhoanDetailsService;
import com.xemay.service.TonKhoService;
import com.xemay.service.XeMayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KIỂM THỬ TÍCH HỢP phần quản lý xe máy (thành viên phụ trách xe máy) với phân quyền và tồn kho.
 */
class QuanLyXeMayTest extends KiemThuTichHop {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanDetailsService taiKhoanDetailsService;

    @Autowired
    private TonKhoService tonKhoService;

    @Autowired
    private XeMayService xeMayService;

    @Autowired
    private TransactionTemplate giaoDich;

    private UserDetails admin;
    private UserDetails nhanVien;
    private UserDetails khachHang;

    @BeforeEach
    void chuanBi() {
        admin = taiKhoanDetailsService.loadUserByUsername("admin");
        nhanVien = taiKhoanDetailsService.loadUserByUsername("nhanvien");
        khachHang = taiKhoanDetailsService.loadUserByUsername("khachhang");
    }

    @Test
    @DisplayName("Phân quyền: chưa đăng nhập -> về trang đăng nhập; khách hàng -> 403; nhân viên xem được danh sách")
    void phanQuyenTrangXeMay() throws Exception {
        XeMay xe = taoXe(4);
        mockMvc.perform(get("/quan-tri/xe-may")).andExpect(redirectedUrlPattern("**/dang-nhap"));
        mockMvc.perform(get("/quan-tri/xe-may").with(user(khachHang))).andExpect(status().isForbidden());
        mockMvc.perform(get("/quan-tri/xe-may").param("tenXe", xe.getTenXe()).with(user(nhanVien)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(xe.getTenXe())))
                .andExpect(content().string(containsString("30.000.000 đ")));
    }

    @Test
    @DisplayName("Lọc sai kiểu dữ liệu trên URL (giaMin=abc) -> bỏ điều kiện đó, không văng lỗi 400")
    void locSaiKieuKhongLoi() throws Exception {
        mockMvc.perform(get("/quan-tri/xe-may").param("giaMin", "abc").with(user(nhanVien)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("thongBaoLoi"));
        mockMvc.perform(get("/quan-tri/xe-may").param("giaMin", "50").param("giaMax", "10").with(user(nhanVien)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Giá tối thiểu không được lớn hơn giá tối đa")));
    }

    @Test
    @DisplayName("Nhân viên thêm xe hợp lệ; thêm trùng tên trong cùng hãng -> lỗi ngay dưới ô tên xe")
    void themXe() throws Exception {
        Integer maHang = hangKiemThu().getMaHang();
        String ten = "SH Mode " + soThuTu();
        mockMvc.perform(post("/quan-tri/xe-may/them").with(user(nhanVien)).with(csrf())
                        .param("maHang", maHang.toString()).param("tenXe", ten).param("loaiXe", "Tay ga")
                        .param("phanKhoi", "125").param("giaBan", "57000000").param("soLuong", "6"))
                .andExpect(redirectedUrl("/quan-tri/xe-may"));
        XeMay daLuu = xeMayRepository.findByTenXeContainingIgnoreCase(ten).get(0);
        assertThat(daLuu.getSoLuong()).isEqualTo(6);
        assertThat(daLuu.getTrangThai()).isTrue();
        assertThat(daLuu.getNgayTao()).isNotNull();

        mockMvc.perform(post("/quan-tri/xe-may/them").with(user(nhanVien)).with(csrf())
                        .param("maHang", maHang.toString()).param("tenXe", ten.toUpperCase()).param("loaiXe", "Tay ga")
                        .param("giaBan", "57000000").param("soLuong", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("xeMayDTO", "tenXe", "nghiepVu"));
    }

    @Test
    @DisplayName("Sửa xe KHÔNG ghi đè tồn kho: form gửi số lượng cũ (10) sau khi đã bán 3 -> tồn kho vẫn 7")
    void suaXeKhongGhiDeTonKho() throws Exception {
        XeMay xe = taoXe(10);
        tonKhoService.xuatKho(xe.getMaXe(), 3);

        mockMvc.perform(post("/quan-tri/xe-may/{id}/sua", xe.getMaXe()).with(user(admin)).with(csrf())
                        .param("maHang", xe.getHangXe().getMaHang().toString()).param("tenXe", xe.getTenXe() + " 2025")
                        .param("loaiXe", "Tay ga").param("giaBan", "31000000").param("soLuong", "10")
                        .param("trangThai", "true"))
                .andExpect(redirectedUrl("/quan-tri/xe-may"));

        XeMay sauKhiSua = xeMayRepository.findById(xe.getMaXe()).orElseThrow();
        assertThat(sauKhiSua.getTenXe()).endsWith(" 2025");
        assertThat(sauKhiSua.getGiaBan()).isEqualByComparingTo("31000000");
        assertThat(sauKhiSua.getSoLuong()).isEqualTo(7);
    }

    @Test
    @DisplayName("ĐỒNG THỜI: mở form sửa (đọc tồn kho 50), khách mua 1 xe, rồi lưu form -> tồn kho 49, không bị ghi đè")
    void suaXeTrongLucCoNguoiMua() {
        XeMay xe = taoXe(50);
        Integer ma = xe.getMaXe();

        giaoDich.executeWithoutResult(trangThai -> {
            // (1) Giao dịch sửa xe đọc dòng xe: SoLuong = 50 nằm trong bộ nhớ của Hibernate
            XeMay banDangSua = xeMayRepository.findById(ma).orElseThrow();
            assertThat(banDangSua.getSoLuong()).isEqualTo(50);

            // (2) Đúng lúc đó, một khách mua 1 xe trên luồng khác (giao dịch riêng, đã commit): CSDL còn 49
            CompletableFuture.runAsync(() -> tonKhoService.xuatKho(ma, 1)).join();

            // (3) Lưu form sửa qua Service thật; Service lấy lại đúng đối tượng (cũ) ở bước (1)
            XeMay duLieuForm = new XeMay(xe.getHangXe(), xe.getTenXe(), "Tay ga", new BigDecimal("33000000"), 50);
            duLieuForm.setMoTa("Bản cập nhật giá");
            xeMayService.updateXeMay(ma, duLieuForm);
        });

        // Không có @DynamicUpdate, câu UPDATE ghi lại cả SoLuong = 50 (số đọc ở bước 1) -> mất lần bán ở bước 2
        assertThat(tonKho(ma)).isEqualTo(49);
        assertThat(xeMayRepository.findById(ma).orElseThrow().getGiaBan()).isEqualByComparingTo("33000000");
    }

    @Test
    @DisplayName("ĐỒNG THỜI: 20 luồng mua và 10 luồng sửa thông tin cùng một xe -> tồn kho cuối đúng bằng 50 - 20")
    void banVaSuaCungLuc() throws Exception {
        XeMay xe = taoXe(50);
        Integer ma = xe.getMaXe();
        ExecutorService nhomLuong = Executors.newFixedThreadPool(30);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        List<Future<?>> ketQua = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            boolean laNguoiMua = i < 20;
            int lan = i;
            ketQua.add(nhomLuong.submit(() -> {
                xuatPhat.await();
                if (laNguoiMua) {
                    tonKhoService.xuatKho(ma, 1);
                } else {
                    XeMay duLieuForm = new XeMay(xe.getHangXe(), xe.getTenXe(), "Tay ga",
                            new BigDecimal("30000000").add(BigDecimal.valueOf(lan)), 50);
                    xeMayService.updateXeMay(ma, duLieuForm);
                }
                return null;
            }));
        }
        xuatPhat.countDown();
        for (Future<?> f : ketQua) {
            f.get(60, TimeUnit.SECONDS);
        }
        nhomLuong.shutdown();
        assertThat(tonKho(ma)).isEqualTo(30);
    }

    @Test
    @DisplayName("Ngừng kinh doanh: nhân viên bị từ chối (403), quản trị viên làm được; xe ngừng bán thì không xuất kho được")
    void ngungKinhDoanhChiQuanTriVien() throws Exception {
        XeMay xe = taoXe(5);
        mockMvc.perform(post("/quan-tri/xe-may/{id}/ngung-ban", xe.getMaXe()).with(user(nhanVien)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(xeMayRepository.findById(xe.getMaXe()).orElseThrow().getTrangThai()).isTrue();

        mockMvc.perform(post("/quan-tri/xe-may/{id}/ngung-ban", xe.getMaXe()).with(user(admin)).with(csrf()))
                .andExpect(redirectedUrl("/quan-tri/xe-may"));
        assertThat(xeMayRepository.findById(xe.getMaXe()).orElseThrow().getTrangThai()).isFalse();

        assertThatThrownBy(() -> tonKhoService.xuatKho(xe.getMaXe(), 1))
                .hasMessageContaining("ngừng kinh doanh");
    }

    @Test
    @DisplayName("Nhân viên sửa xe và gửi kèm trangThai=false -> trạng thái giữ nguyên (chỉ quản trị viên được đổi)")
    void nhanVienKhongDoiDuocTrangThai() throws Exception {
        XeMay xe = taoXe(5);
        mockMvc.perform(post("/quan-tri/xe-may/{id}/sua", xe.getMaXe()).with(user(nhanVien)).with(csrf())
                        .param("maHang", xe.getHangXe().getMaHang().toString()).param("tenXe", xe.getTenXe())
                        .param("loaiXe", "Tay ga").param("giaBan", "30000000").param("soLuong", "5")
                        .param("trangThai", "false"))
                .andExpect(redirectedUrl("/quan-tri/xe-may"));
        assertThat(xeMayRepository.findById(xe.getMaXe()).orElseThrow().getTrangThai()).isTrue();
    }

    @Test
    @DisplayName("Nhập kho trên trang sửa xe: cộng đúng qua TonKhoService; nhập chữ hoặc số âm -> báo lỗi")
    void nhapKhoTrenTrang() throws Exception {
        XeMay xe = taoXe(2);
        mockMvc.perform(post("/quan-tri/xe-may/{id}/nhap-kho", xe.getMaXe()).param("soLuong", "5")
                        .with(user(nhanVien)).with(csrf()))
                .andExpect(redirectedUrl("/quan-tri/xe-may/" + xe.getMaXe() + "/sua"))
                .andExpect(flash().attribute("thongBaoThanhCong", containsString("Tồn kho hiện tại: 7")));
        assertThat(tonKho(xe.getMaXe())).isEqualTo(7);

        mockMvc.perform(post("/quan-tri/xe-may/{id}/nhap-kho", xe.getMaXe()).param("soLuong", "abc")
                        .with(user(nhanVien)).with(csrf()))
                .andExpect(flash().attributeExists("thongBaoLoi"));
        mockMvc.perform(post("/quan-tri/xe-may/{id}/nhap-kho", xe.getMaXe()).param("soLuong", "-3")
                        .with(user(nhanVien)).with(csrf()))
                .andExpect(flash().attributeExists("thongBaoLoi"));
        assertThat(tonKho(xe.getMaXe())).isEqualTo(7);
    }

    @Test
    @DisplayName("Các trang thêm, sửa hiển thị được; nút ngừng kinh doanh chỉ hiện với quản trị viên; mã không tồn tại -> 404")
    void hienThiTrang() throws Exception {
        XeMay xe = taoXe(3);
        mockMvc.perform(get("/quan-tri/xe-may/them").with(user(nhanVien))).andExpect(status().isOk());
        mockMvc.perform(get("/quan-tri/xe-may/{id}/sua", xe.getMaXe()).with(user(nhanVien)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nhập kho")));
        mockMvc.perform(get("/quan-tri/xe-may").param("tenXe", xe.getTenXe()).with(user(nhanVien)))
                .andExpect(content().string(not(containsString("/ngung-ban"))));
        mockMvc.perform(get("/quan-tri/xe-may").param("tenXe", xe.getTenXe()).with(user(admin)))
                .andExpect(content().string(containsString("/ngung-ban")));
        mockMvc.perform(get("/quan-tri/xe-may/{id}/sua", 999_999).with(user(admin))).andExpect(status().isNotFound());
    }
}
