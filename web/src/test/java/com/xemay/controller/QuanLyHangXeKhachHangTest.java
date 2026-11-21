package com.xemay.controller;

import com.xemay.KiemThuTichHop;
import com.xemay.entity.HangXe;
import com.xemay.entity.KhachHang;
import com.xemay.entity.TaiKhoan;
import com.xemay.repository.KhachHangRepository;
import com.xemay.repository.TaiKhoanRepository;
import com.xemay.security.TaiKhoanDetailsService;
import com.xemay.service.GuiEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KIỂM THỬ TÍCH HỢP phần quản lý hãng xe (thành viên phụ trách xe máy) và quản lý khách hàng
 * (thành viên phụ trách khách hàng) với phần tài khoản, phân quyền.
 */
class QuanLyHangXeKhachHangTest extends KiemThuTichHop {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanDetailsService taiKhoanDetailsService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private GuiEmailService guiEmailService;

    private UserDetails admin;
    private UserDetails nhanVien;
    private UserDetails khachHang;

    @BeforeEach
    void chuanBi() {
        admin = taiKhoanDetailsService.loadUserByUsername("admin");
        nhanVien = taiKhoanDetailsService.loadUserByUsername("nhanvien");
        khachHang = taiKhoanDetailsService.loadUserByUsername("khachhang");
    }

    private KhachHang hoSoCua(TaiKhoan taiKhoan) {
        return khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId()).orElseThrow();
    }

    @Nested
    @DisplayName("Hãng xe")
    class HangXeTest {

        @Test
        @DisplayName("Thêm hãng trùng tên (khác hoa thường) -> lỗi dưới ô tên hãng")
        void trungTenHang() throws Exception {
            String ten = "Hãng mới " + soThuTu();
            mockMvc.perform(post("/quan-tri/hang-xe/them").with(user(nhanVien)).with(csrf())
                            .param("tenHang", ten).param("quocGia", "Việt Nam"))
                    .andExpect(redirectedUrl("/quan-tri/hang-xe"));
            mockMvc.perform(post("/quan-tri/hang-xe/them").with(user(nhanVien)).with(csrf())
                            .param("tenHang", ten.toUpperCase()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("hangXe", "tenHang", "nghiepVu"));
        }

        @Test
        @DisplayName("Form thêm hãng gửi kèm maHang của hãng khác -> vẫn tạo hãng MỚI, không ghi đè hãng cũ")
        void khongGhiDeHangKhac() throws Exception {
            HangXe hangCu = hangKiemThu();
            String tenMoi = "Hãng chen ngang " + soThuTu();
            mockMvc.perform(post("/quan-tri/hang-xe/them").with(user(admin)).with(csrf())
                            .param("maHang", hangCu.getMaHang().toString()).param("tenHang", tenMoi))
                    .andExpect(redirectedUrl("/quan-tri/hang-xe"));
            assertThat(hangXeRepository.findById(hangCu.getMaHang()).orElseThrow().getTenHang()).isEqualTo("Hãng kiểm thử");
            assertThat(hangXeRepository.findByTenHangContainingIgnoreCase(tenMoi)).hasSize(1);
        }

        @Test
        @DisplayName("Ngừng hoạt động: nhân viên -> 403; hãng còn xe -> báo lỗi; hãng chưa có xe -> quản trị viên làm được")
        void ngungHoatDong() throws Exception {
            HangXe hangCoXe = hangKiemThu();
            taoXe(1);
            HangXe hangTrong = hangXeRepository.save(new HangXe("Hãng trống " + soThuTu(), "Ý"));

            mockMvc.perform(post("/quan-tri/hang-xe/{id}/ngung", hangTrong.getMaHang()).with(user(nhanVien)).with(csrf()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/quan-tri/hang-xe/{id}/ngung", hangCoXe.getMaHang()).with(user(admin)).with(csrf()))
                    .andExpect(flash().attribute("thongBaoLoi", containsString("vẫn còn xe máy")));
            mockMvc.perform(post("/quan-tri/hang-xe/{id}/ngung", hangTrong.getMaHang()).with(user(admin)).with(csrf()))
                    .andExpect(flash().attributeExists("thongBaoThanhCong"));

            assertThat(hangXeRepository.findById(hangCoXe.getMaHang()).orElseThrow().getTrangThai()).isTrue();
            assertThat(hangXeRepository.findById(hangTrong.getMaHang()).orElseThrow().getTrangThai()).isFalse();
        }

        @Test
        @DisplayName("Nhân viên sửa hãng, gửi kèm trangThai=false -> trạng thái giữ nguyên")
        void nhanVienKhongDoiTrangThaiHang() throws Exception {
            HangXe hang = hangXeRepository.save(new HangXe("Hãng sửa " + soThuTu(), "Nhật Bản"));
            mockMvc.perform(post("/quan-tri/hang-xe/{id}/sua", hang.getMaHang()).with(user(nhanVien)).with(csrf())
                            .param("tenHang", hang.getTenHang()).param("quocGia", "Thái Lan").param("trangThai", "false"))
                    .andExpect(redirectedUrl("/quan-tri/hang-xe"));
            HangXe sauKhiSua = hangXeRepository.findById(hang.getMaHang()).orElseThrow();
            assertThat(sauKhiSua.getQuocGia()).isEqualTo("Thái Lan");
            assertThat(sauKhiSua.getTrangThai()).isTrue();
        }

        @Test
        @DisplayName("Trang danh sách hãng hiển thị số mẫu xe của từng hãng")
        void danhSachHang() throws Exception {
            taoXe(1);
            mockMvc.perform(get("/quan-tri/hang-xe").param("tenHang", "kiểm thử").with(user(nhanVien)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Hãng kiểm thử")))
                    .andExpect(content().string(containsString("/quan-tri/xe-may?maHang=")));
            mockMvc.perform(get("/quan-tri/hang-xe/them").with(user(nhanVien))).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Khách hàng")
    class KhachHangTest {

        @Test
        @DisplayName("Đăng ký -> có ngay hồ sơ KHACH_HANG gắn với tài khoản, cùng họ tên, số điện thoại, email")
        void dangKyTaoHoSo() {
            TaiKhoan tk = taoKhachHangMoi();
            KhachHang hoSo = hoSoCua(tk);
            assertThat(hoSo.getHoTen()).isEqualTo(tk.getHoTen());
            assertThat(hoSo.getSoDienThoai()).isEqualTo(tk.getSoDienThoai());
            assertThat(hoSo.getEmail()).isEqualTo(tk.getEmail());
            assertThat(hoSo.getNgayTao()).isNotNull();
        }

        @Test
        @DisplayName("Đăng ký trùng số điện thoại của khách đã có -> lỗi ô số điện thoại, KHÔNG tạo tài khoản")
        void dangKyTrungSoDienThoai() throws Exception {
            TaiKhoan daCo = taoKhachHangMoi();
            String ten = "trungsdt" + soThuTu();
            mockMvc.perform(post("/dang-ky").with(csrf())
                            .param("tenDangNhap", ten).param("matKhau", "matkhau1").param("nhapLaiMatKhau", "matkhau1")
                            .param("hoTen", "Người Trùng Số").param("email", ten + "@kiemthu.vn")
                            .param("soDienThoai", daCo.getSoDienThoai()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("form", "soDienThoai", "nghiepVu"));
            // Tài khoản đã được INSERT trước khi tạo hồ sơ, nhưng cả giao dịch bị hủy nên không còn
            assertThat(taiKhoanRepository.findByTenDangNhap(ten)).isEmpty();

            mockMvc.perform(post("/dang-ky").with(csrf())
                            .param("tenDangNhap", ten).param("matKhau", "matkhau1").param("nhapLaiMatKhau", "matkhau1")
                            .param("hoTen", "Người Thiếu Số").param("email", ten + "@kiemthu.vn"))
                    .andExpect(model().attributeHasFieldErrors("form", "soDienThoai"));
        }

        @Test
        @DisplayName("Email chào mừng gửi trên LUỒNG NỀN xemay-nen-N sau khi đăng ký (request không phải chờ)")
        void emailChaoMungChayNen() throws Exception {
            int truoc = guiEmailService.getSoThuDaGui();
            taoKhachHangMoi();
            long hetHan = System.currentTimeMillis() + 5_000;
            while (guiEmailService.getSoThuDaGui() == truoc && System.currentTimeMillis() < hetHan) {
                Thread.sleep(20);
            }
            assertThat(guiEmailService.getSoThuDaGui()).isGreaterThan(truoc);
            assertThat(guiEmailService.getLuongGuiGanNhat()).startsWith("xemay-nen-");
        }

        @Test
        @DisplayName("Nhân viên thêm khách mua tại cửa hàng, tìm được theo số điện thoại và theo tên không phân biệt hoa thường")
        void themVaTimKhachMuaTaiCuaHang() throws Exception {
            String sdt = soDienThoaiMoi();
            mockMvc.perform(post("/quan-tri/khach-hang/them").with(user(nhanVien)).with(csrf())
                            .param("hoTen", "Ông Khách Vãng Lai").param("soDienThoai", sdt)
                            .param("diaChi", "Quận 9").param("ngaySinh", "1990-05-20"))
                    .andExpect(redirectedUrl("/quan-tri/khach-hang"));
            KhachHang daLuu = khachHangRepository.findAll().stream()
                    .filter(kh -> sdt.equals(kh.getSoDienThoai())).findFirst().orElseThrow();
            assertThat(daLuu.getTaiKhoan()).isNull();
            assertThat(daLuu.getNgaySinh()).hasYear(1990);

            mockMvc.perform(get("/quan-tri/khach-hang").param("tuKhoa", sdt).with(user(nhanVien)))
                    .andExpect(content().string(containsString("Ông Khách Vãng Lai")))
                    .andExpect(content().string(containsString("Mua tại cửa hàng")));
            mockMvc.perform(get("/quan-tri/khach-hang").param("tuKhoa", "KHÁCH VÃNG").with(user(nhanVien)))
                    .andExpect(content().string(containsString("Ông Khách Vãng Lai")));
        }

        @Test
        @DisplayName("Thêm khách: thiếu tên, số điện thoại sai, ngày sinh tương lai, số trùng -> lỗi từng ô")
        void themKhachDuLieuSai() throws Exception {
            mockMvc.perform(post("/quan-tri/khach-hang/them").with(user(nhanVien)).with(csrf())
                            .param("hoTen", "").param("soDienThoai", "123").param("ngaySinh", "2999-01-01"))
                    .andExpect(model().attributeHasFieldErrors("form", "hoTen", "soDienThoai", "ngaySinh"));
            mockMvc.perform(post("/quan-tri/khach-hang/them").with(user(nhanVien)).with(csrf())
                            .param("hoTen", "Khách Trùng Số").param("soDienThoai", "0901000003"))
                    .andExpect(model().attributeHasFieldErrorCode("form", "soDienThoai", "nghiepVu"));
        }

        @Test
        @DisplayName("Sửa hồ sơ khách có tài khoản -> họ tên, số điện thoại cập nhật sang tài khoản; email giữ theo tài khoản")
        void suaHoSoDongBoTaiKhoan() throws Exception {
            TaiKhoan tk = taoKhachHangMoi();
            KhachHang hoSo = hoSoCua(tk);
            String sdtMoi = soDienThoaiMoi();
            mockMvc.perform(post("/quan-tri/khach-hang/{id}/sua", hoSo.getMaKhachHang()).with(user(nhanVien)).with(csrf())
                            .param("hoTen", "Tên Đã Đổi").param("soDienThoai", sdtMoi)
                            .param("email", "doi-email@kiemthu.vn").param("diaChi", "Thủ Đức"))
                    .andExpect(redirectedUrl("/quan-tri/khach-hang"));

            TaiKhoan taiKhoanSau = taiKhoanRepository.findById(tk.getId()).orElseThrow();
            assertThat(taiKhoanSau.getHoTen()).isEqualTo("Tên Đã Đổi");
            assertThat(taiKhoanSau.getSoDienThoai()).isEqualTo(sdtMoi);
            assertThat(taiKhoanSau.getEmail()).isEqualTo(tk.getEmail());
            assertThat(hoSoCua(tk).getEmail()).isEqualTo(tk.getEmail());
            assertThat(hoSoCua(tk).getDiaChi()).isEqualTo("Thủ Đức");
        }

        @Test
        @DisplayName("Khách tự sửa thông tin cá nhân -> hồ sơ khách hàng cập nhật theo; xóa trắng số điện thoại -> báo lỗi")
        void khachTuSuaThongTin() throws Exception {
            TaiKhoan tk = taoKhachHangMoi();
            UserDetails khach = taiKhoanDetailsService.loadUserByUsername(tk.getTenDangNhap());
            String sdtMoi = soDienThoaiMoi();
            mockMvc.perform(post("/tai-khoan").with(user(khach)).with(csrf())
                            .param("hoTen", "Khách Tự Đổi Tên").param("email", tk.getEmail()).param("soDienThoai", sdtMoi))
                    .andExpect(redirectedUrl("/tai-khoan"));
            assertThat(hoSoCua(tk).getHoTen()).isEqualTo("Khách Tự Đổi Tên");
            assertThat(hoSoCua(tk).getSoDienThoai()).isEqualTo(sdtMoi);

            mockMvc.perform(post("/tai-khoan").with(user(khach)).with(csrf())
                            .param("hoTen", "Khách Tự Đổi Tên").param("email", tk.getEmail()).param("soDienThoai", ""))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("form", "soDienThoai", "nghiepVu"));
            assertThat(hoSoCua(tk).getSoDienThoai()).isEqualTo(sdtMoi);
        }

        @Test
        @DisplayName("Xóa khách: nhân viên -> 403; hồ sơ có tài khoản -> báo lỗi; khách mua tại cửa hàng -> xóa được")
        void xoaKhachHang() throws Exception {
            KhachHang vangLai = new KhachHang();
            vangLai.setHoTen("Khách Sẽ Bị Xóa");
            vangLai.setSoDienThoai(soDienThoaiMoi());
            vangLai = khachHangRepository.save(vangLai);
            KhachHang coTaiKhoan = hoSoCua(taoKhachHangMoi());

            mockMvc.perform(post("/quan-tri/khach-hang/{id}/xoa", vangLai.getMaKhachHang()).with(user(nhanVien)).with(csrf()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/quan-tri/khach-hang/{id}/xoa", coTaiKhoan.getMaKhachHang()).with(user(admin)).with(csrf()))
                    .andExpect(flash().attribute("thongBaoLoi", containsString("đang có tài khoản")));
            mockMvc.perform(post("/quan-tri/khach-hang/{id}/xoa", vangLai.getMaKhachHang()).with(user(admin)).with(csrf()))
                    .andExpect(flash().attribute("thongBaoThanhCong", "Đã xóa khách hàng."));

            assertThat(khachHangRepository.findById(vangLai.getMaKhachHang())).isEmpty();
            assertThat(khachHangRepository.findById(coTaiKhoan.getMaKhachHang())).isPresent();
        }

        @Test
        @DisplayName("Khách hàng không vào được trang quản lý khách hàng; nhân viên không thấy nút xóa")
        void phanQuyenTrangKhachHang() throws Exception {
            mockMvc.perform(get("/quan-tri/khach-hang").with(user(khachHang))).andExpect(status().isForbidden());
            mockMvc.perform(get("/quan-tri/khach-hang").with(user(nhanVien)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("/xoa"))));
            Optional<KhachHang> mau = khachHangRepository.findByTaiKhoan_Id(
                    taiKhoanRepository.findByTenDangNhap("khachhang").orElseThrow().getId());
            assertThat(mau).isPresent();
            mockMvc.perform(get("/quan-tri/khach-hang/{id}/sua", mau.get().getMaKhachHang()).with(user(nhanVien)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Email đăng nhập, đổi ở trang tài khoản")));
        }
    }
}
