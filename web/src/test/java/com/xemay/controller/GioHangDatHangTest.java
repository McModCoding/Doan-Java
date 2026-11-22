package com.xemay.controller;

import com.xemay.KiemThuTichHop;
import com.xemay.entity.ChiTietGioHang;
import com.xemay.entity.DonHang;
import com.xemay.entity.KhachHang;
import com.xemay.entity.TaiKhoan;
import com.xemay.entity.XeMay;
import com.xemay.exception.HetHangException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.DonHangRepository;
import com.xemay.repository.GioHangRepository;
import com.xemay.repository.KhachHangRepository;
import com.xemay.security.TaiKhoanDetailsService;
import com.xemay.service.DatHangService;
import com.xemay.service.GioHangService;
import com.xemay.service.TonKhoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Trang cho khách (trang chủ, danh sách, chi tiết xe), giỏ hàng lưu trong CSDL và đặt hàng. */
class GioHangDatHangTest extends KiemThuTichHop {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanDetailsService taiKhoanDetailsService;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private DatHangService datHangService;

    @Autowired
    private TonKhoService tonKhoService;

    @Autowired
    private TransactionTemplate giaoDich;

    private UserDetails dangNhap(TaiKhoan tk) {
        return taiKhoanDetailsService.loadUserByUsername(tk.getTenDangNhap());
    }

    private KhachHang hoSo(TaiKhoan tk) {
        return khachHangRepository.findByTaiKhoan_Id(tk.getId()).orElseThrow();
    }

    /** Số lượng từng xe trong giỏ của khách (mã xe -> số lượng), đọc trong giao dịch. */
    private List<int[]> gioCua(TaiKhoan tk) {
        return giaoDich.execute(s -> gioHangRepository.findByKhachHang_MaKhachHang(hoSo(tk).getMaKhachHang())
                .map(g -> gioHangRepository.dongTrongGio(g.getMaGioHang()).stream()
                        .map((ChiTietGioHang d) -> new int[]{d.getXeMay().getMaXe(), d.getSoLuong()}).toList())
                .orElse(List.of()));
    }

    private List<DonHang> donCua(TaiKhoan tk) {
        return giaoDich.execute(s -> {
            List<DonHang> ds = donHangRepository.findByKhachHang_MaKhachHangOrderByMaDonHangDesc(hoSo(tk).getMaKhachHang());
            ds.forEach(d -> d.getDsChiTiet().size());   // đọc luôn các dòng của đơn
            return ds;
        });
    }

    @Test
    @DisplayName("Trang công khai: trang chủ, danh sách (tìm cả theo 'hãng + tên'), chi tiết; xe ngừng bán hoặc không có -> 404")
    void trangCongKhai() throws Exception {
        XeMay xe = taoXe(3);
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString(xe.getTenXe())));
        mockMvc.perform(get("/xe-may").param("q", xe.getTenXe()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(xe.getTenXe())));
        mockMvc.perform(get("/xe-may").param("q", "hãng kiểm thử " + xe.getTenXe().toLowerCase()))
                .andExpect(content().string(containsString(xe.getTenXe())));
        mockMvc.perform(get("/xe-may/{id}", xe.getMaXe()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Hãng kiểm thử")));
        mockMvc.perform(get("/xe-may").param("giaTu", "50").param("giaDen", "10"))
                .andExpect(content().string(containsString("Khoảng giá chưa hợp lệ")));

        xe.setTrangThai(false);
        xeMayRepository.save(xe);
        mockMvc.perform(get("/xe-may/{id}", xe.getMaXe())).andExpect(status().isNotFound());
        mockMvc.perform(get("/xe-may/{id}", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Giỏ hàng chỉ dành cho khách hàng: chưa đăng nhập -> trang đăng nhập; nhân viên -> 403")
    void gioHangChiDanhChoKhachHang() throws Exception {
        mockMvc.perform(get("/gio-hang")).andExpect(redirectedUrlPattern("**/dang-nhap"));
        mockMvc.perform(get("/gio-hang").with(user(taiKhoanDetailsService.loadUserByUsername("nhanvien"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Thêm, sửa, xóa xe trong giỏ: lưu vào GIO_HANG/CHI_TIET_GIO_HANG; không cho vượt tồn kho")
    void gioHangLuuTrongCsdl() throws Exception {
        TaiKhoan tk = taoKhachHangMoi();
        UserDetails khach = dangNhap(tk);
        XeMay xe = taoXe(3);

        mockMvc.perform(post("/gio-hang/them").param("xeId", xe.getMaXe().toString()).param("soLuong", "2")
                        .with(user(khach)).with(csrf()))
                .andExpect(redirectedUrl("/gio-hang")).andExpect(flash().attributeExists("thongBaoThanhCong"));
        assertThat(gioCua(tk)).singleElement().satisfies(d -> assertThat(d).containsExactly(xe.getMaXe(), 2));

        mockMvc.perform(post("/gio-hang/them").param("xeId", xe.getMaXe().toString()).param("soLuong", "2")
                        .with(user(khach)).with(csrf()))
                .andExpect(flash().attribute("thongBaoLoi", "Số lượng yêu cầu vượt số xe đang có trong kho"));
        mockMvc.perform(get("/gio-hang").with(user(khach)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(xe.getTenXe())))
                .andExpect(content().string(containsString("60.000.000 đ")));

        mockMvc.perform(post("/gio-hang/cap-nhat").param("xeId", xe.getMaXe().toString()).param("soLuong", "1")
                .with(user(khach)).with(csrf()));
        assertThat(gioCua(tk)).singleElement().satisfies(d -> assertThat(d[1]).isEqualTo(1));
        mockMvc.perform(post("/gio-hang/xoa").param("xeId", xe.getMaXe().toString()).with(user(khach)).with(csrf()));
        assertThat(gioCua(tk)).isEmpty();
        assertThat(tonKho(xe.getMaXe())).isEqualTo(3);   // thêm vào giỏ không trừ kho
    }

    @Test
    @DisplayName("Đặt hàng: tạo DON_HANG và CHI_TIET_DON_HANG, tổng tiền đúng, trừ kho, giỏ trống")
    void datHangTaoDonVaTruKho() throws Exception {
        TaiKhoan tk = taoKhachHangMoi();
        UserDetails khach = dangNhap(tk);
        XeMay xeA = taoXe(5);
        XeMay xeB = taoXe(2);
        gioHangService.them(tk.getId(), xeA.getMaXe(), 2);
        gioHangService.them(tk.getId(), xeB.getMaXe(), 1);

        mockMvc.perform(post("/gio-hang/dat-hang").param("diaChiGiaoHang", "12 Lê Lợi, Quận 1").param("ghiChu", "Gọi trước 30 phút")
                        .with(user(khach)).with(csrf()))
                .andExpect(redirectedUrl("/gio-hang"))
                .andExpect(flash().attribute("thongBaoThanhCong", containsString("90.000.000 đ")));

        List<DonHang> don = donCua(tk);
        assertThat(don).hasSize(1);
        assertThat(don.get(0).getTongTien()).isEqualByComparingTo("90000000");
        assertThat(don.get(0).getTrangThai()).isEqualTo(DonHang.CHO_XAC_NHAN);
        assertThat(don.get(0).getDiaChiGiaoHang()).isEqualTo("12 Lê Lợi, Quận 1");
        assertThat(don.get(0).getDsChiTiet()).hasSize(2);
        assertThat(tonKho(xeA.getMaXe())).isEqualTo(3);
        assertThat(tonKho(xeB.getMaXe())).isEqualTo(1);
        assertThat(gioCua(tk)).isEmpty();
    }

    @Test
    @DisplayName("Đặt hàng khi kho đã giảm (người khác vừa mua) -> báo hết hàng, không tạo đơn, giỏ và kho giữ nguyên")
    void datHangThieuHang() throws Exception {
        TaiKhoan tk = taoKhachHangMoi();
        XeMay xe = taoXe(2);
        gioHangService.them(tk.getId(), xe.getMaXe(), 2);
        tonKhoService.xuatKho(xe.getMaXe(), 1);   // kho còn 1

        mockMvc.perform(post("/gio-hang/dat-hang").param("diaChiGiaoHang", "Thủ Đức").with(user(dangNhap(tk))).with(csrf()))
                .andExpect(flash().attribute("thongBaoLoi", containsString("chỉ còn 1 chiếc")));
        assertThat(donCua(tk)).isEmpty();
        assertThat(gioCua(tk)).singleElement().satisfies(d -> assertThat(d[1]).isEqualTo(2));
        assertThat(tonKho(xe.getMaXe())).isEqualTo(1);

        mockMvc.perform(post("/gio-hang/dat-hang").param("diaChiGiaoHang", " ").with(user(dangNhap(tk))).with(csrf()))
                .andExpect(flash().attribute("thongBaoLoi", "Vui lòng nhập địa chỉ giao xe."));
    }

    @Test
    @DisplayName("Xe ngừng bán: không thêm được vào giỏ; xe trong giỏ bị ngừng bán trước khi đặt -> không tạo đơn, không trừ kho")
    void xeNgungBan() throws Exception {
        TaiKhoan tk = taoKhachHangMoi();
        XeMay ngungBan = taoXe(5);
        ngungBan.setTrangThai(false);
        xeMayRepository.save(ngungBan);
        mockMvc.perform(post("/gio-hang/them").param("xeId", ngungBan.getMaXe().toString()).param("soLuong", "1")
                        .with(user(dangNhap(tk))).with(csrf()))
                .andExpect(flash().attribute("thongBaoLoi", "Không tìm thấy mẫu xe"));

        XeMay xe = taoXe(5);
        gioHangService.them(tk.getId(), xe.getMaXe(), 2);
        XeMay docLai = xeMayRepository.findById(xe.getMaXe()).orElseThrow();
        docLai.setTrangThai(false);   // quản trị viên ngừng bán sau khi khách đã cho xe vào giỏ
        xeMayRepository.save(docLai);

        mockMvc.perform(post("/gio-hang/dat-hang").param("diaChiGiaoHang", "Quận 5").with(user(dangNhap(tk))).with(csrf()))
                .andExpect(flash().attribute("thongBaoLoi", containsString("đã ngừng kinh doanh")));
        assertThat(donCua(tk)).isEmpty();
        assertThat(tonKho(xe.getMaXe())).isEqualTo(5);
        assertThat(gioCua(tk)).hasSize(1);
    }

    @Test
    @DisplayName("ĐỒNG THỜI: hai khách cùng đặt chiếc xe cuối cùng -> đúng một đơn, kho về 0, khách kia báo hết hàng")
    void haiKhachCungDatChiecXeCuoi() throws Exception {
        XeMay xe = taoXe(1);
        TaiKhoan an = taoKhachHangMoi();
        TaiKhoan binh = taoKhachHangMoi();
        gioHangService.them(an.getId(), xe.getMaXe(), 1);
        gioHangService.them(binh.getId(), xe.getMaXe(), 1);

        List<Object> ketQua = chayCungLuc(
                () -> datHangService.datHang(an.getId(), "Quận 1", null),
                () -> datHangService.datHang(binh.getId(), "Quận 3", null));

        assertThat(ketQua).filteredOn(DonHang.class::isInstance).hasSize(1);
        assertThat(ketQua).filteredOn(HetHangException.class::isInstance).hasSize(1);
        assertThat(tonKho(xe.getMaXe())).isZero();
        assertThat(donCua(an).size() + donCua(binh).size()).isEqualTo(1);
        // Người không mua được vẫn còn xe trong giỏ (giao dịch của họ bị hủy toàn bộ)
        assertThat(gioCua(an).size() + gioCua(binh).size()).isEqualTo(1);
    }

    @Test
    @DisplayName("ĐỒNG THỜI: một khách bấm Đặt hàng hai lần cùng lúc -> chỉ một đơn, kho chỉ trừ một lần")
    void motKhachDatHaiLanCungLuc() throws Exception {
        XeMay xe = taoXe(10);
        TaiKhoan tk = taoKhachHangMoi();
        gioHangService.them(tk.getId(), xe.getMaXe(), 3);

        List<Object> ketQua = chayCungLuc(
                () -> datHangService.datHang(tk.getId(), "Quận 7", null),
                () -> datHangService.datHang(tk.getId(), "Quận 7", null));

        assertThat(ketQua).filteredOn(DonHang.class::isInstance).hasSize(1);
        assertThat(ketQua).filteredOn(NghiepVuException.class::isInstance)
                .singleElement().satisfies(e -> assertThat(((Exception) e).getMessage()).isEqualTo("Giỏ hàng đang trống."));
        assertThat(donCua(tk)).hasSize(1);
        assertThat(tonKho(xe.getMaXe())).isEqualTo(7);
    }

    /** Chạy các công việc trên các luồng riêng, xuất phát cùng lúc; trả về kết quả hoặc ngoại lệ của từng việc. */
    private static List<Object> chayCungLuc(Callable<?>... congViec) throws Exception {
        ExecutorService nhomLuong = Executors.newFixedThreadPool(congViec.length);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        List<Future<Object>> tuongLai = new ArrayList<>();
        for (Callable<?> viec : congViec) {
            tuongLai.add(nhomLuong.submit(() -> {
                xuatPhat.await();
                try {
                    return viec.call();
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        xuatPhat.countDown();
        List<Object> ketQua = new ArrayList<>();
        for (Future<Object> f : tuongLai) {
            ketQua.add(f.get(60, TimeUnit.SECONDS));
        }
        nhomLuong.shutdown();
        return ketQua;
    }
}
