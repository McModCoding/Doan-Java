package com.xemay.controller;

import com.xemay.KiemThuTichHop;
import com.xemay.entity.KhachHang;
import com.xemay.entity.TaiKhoan;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.KhachHangRepository;
import com.xemay.repository.LichSuDangNhapRepository;
import com.xemay.repository.TaiKhoanRepository;
import com.xemay.security.BoDemDangNhapSai;
import com.xemay.security.TaiKhoanDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Kiểm thử chức năng ĐĂNG NHẬP, ĐĂNG KÝ và PHÂN QUYỀN qua HTTP (MockMvc giả lập trình duyệt). */
@ExtendWith(OutputCaptureExtension.class)
class DangNhapPhanQuyenTest extends KiemThuTichHop {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoDemDangNhapSai boDemDangNhapSai;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private LichSuDangNhapRepository lichSuDangNhapRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanDetailsService taiKhoanDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void xoaBoDemCuaTaiKhoanMacDinh() {
        boDemDangNhapSai.xoa("admin");
        boDemDangNhapSai.xoa("nhanvien");
        boDemDangNhapSai.xoa("khachhang");
    }

    /** Chờ (tối đa 5 giây) đến khi tên đăng nhập có đủ số dòng lịch sử mong đợi - vì việc ghi chạy trên luồng nền. */
    private long choLichSu(String tenDangNhap, int soDongMongDoi) throws InterruptedException {
        long hanChot = System.currentTimeMillis() + 5_000;
        long soDong;
        do {
            soDong = lichSuDangNhapRepository.findByTenDangNhapContainingIgnoreCase(tenDangNhap, Pageable.unpaged())
                    .getTotalElements();
            if (soDong >= soDongMongDoi) {
                break;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() < hanChot);
        return soDong;
    }

    @Nested
    @DisplayName("Đăng nhập")
    class DangNhap {

        @Test
        @DisplayName("Quản trị viên đăng nhập đúng -> vào trang quản trị, có quyền ROLE_ADMIN")
        void adminDangNhapDung() throws Exception {
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", "admin").password("matKhau", "123456"))
                    .andExpect(redirectedUrl("/quan-tri"))
                    .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));
        }

        @Test
        @DisplayName("Khách hàng đăng nhập đúng -> về trang chủ")
        void khachHangDangNhapDung() throws Exception {
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", "khachhang").password("matKhau", "123456"))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(authenticated().withRoles("KHACHHANG"));
        }

        @Test
        @DisplayName("Tên đăng nhập không phân biệt chữ hoa, chữ thường")
        void khongPhanBietHoaThuong() throws Exception {
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", "  AdMin ").password("matKhau", "123456"))
                    .andExpect(authenticated().withUsername("admin"));
        }

        @Test
        @DisplayName("Sai mật khẩu -> quay lại trang đăng nhập kèm thông báo lỗi")
        void saiMatKhau() throws Exception {
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", "admin").password("matKhau", "sai-mat-khau"))
                    .andExpect(redirectedUrl("/dang-nhap?loi=sai"))
                    .andExpect(unauthenticated());

            mockMvc.perform(get("/dang-nhap").param("loi", "sai"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Tên đăng nhập hoặc mật khẩu không đúng.")));
        }

        @Test
        @DisplayName("Sai 5 lần -> tạm khóa (kể cả nhập đúng); quản trị viên mở khóa -> đăng nhập lại được")
        void saiNamLanBiTamKhoa() throws Exception {
            TaiKhoan tk = taoKhachHangMoi();
            for (int i = 1; i <= 4; i++) {
                mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "sai" + i))
                        .andExpect(redirectedUrl("/dang-nhap?loi=sai"));
            }
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "sai5"))
                    .andExpect(redirectedUrl("/dang-nhap?loi=tam-khoa"));
            // Nhập đúng mật khẩu vẫn bị từ chối trong thời gian tạm khóa
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "matkhau1"))
                    .andExpect(redirectedUrl("/dang-nhap?loi=tam-khoa"))
                    .andExpect(unauthenticated());

            UserDetails admin = taiKhoanDetailsService.loadUserByUsername("admin");
            mockMvc.perform(post("/quan-tri/tai-khoan/{id}/mo-khoa", tk.getId()).with(user(admin)).with(csrf()))
                    .andExpect(redirectedUrl("/quan-tri/tai-khoan"));

            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "matkhau1"))
                    .andExpect(authenticated());
        }

        @Test
        @DisplayName("Tài khoản bị quản trị viên khóa -> không đăng nhập được")
        void taiKhoanBiKhoa() throws Exception {
            TaiKhoan tk = taoKhachHangMoi();
            tk.setKichHoat(false);
            taiKhoanRepository.save(tk);

            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "matkhau1"))
                    .andExpect(redirectedUrl("/dang-nhap?loi=bi-khoa"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("ĐA LUỒNG: lịch sử đăng nhập được ghi bởi luồng nền xemay-nen-*, không phải luồng xử lý request")
        void lichSuGhiBangLuongNen(CapturedOutput log) throws Exception {
            TaiKhoan tk = taoKhachHangMoi();

            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "matkhau1"))
                    .andExpect(authenticated());

            // Việc ghi chạy bất đồng bộ nên phải chờ một chút
            assertThat(choLichSu(tk.getTenDangNhap(), 1)).isEqualTo(1);
            assertThat(log.getOut())
                    .contains("Đăng nhập thành công: '" + tk.getTenDangNhap() + "' qua WEB [luồng xử lý: ")
                    .contains("Đã ghi lịch sử đăng nhập (thành công) qua WEB [luồng nền: xemay-nen-");
        }
    }

    @Nested
    @DisplayName("Đăng ký")
    class DangKy {

        @Test
        @DisplayName("Dữ liệu sai -> hiển thị lại form, lỗi gắn với từng ô nhập")
        void duLieuSai() throws Exception {
            mockMvc.perform(post("/dang-ky").with(csrf())
                            .param("tenDangNhap", "ab")
                            .param("matKhau", "123")
                            .param("nhapLaiMatKhau", "456")
                            .param("hoTen", "")
                            .param("email", "khong-phai-email")
                            .param("soDienThoai", "12345"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("form",
                            "tenDangNhap", "matKhau", "matKhauKhop", "hoTen", "email", "soDienThoai"))
                    .andExpect(content().string(containsString("Mật khẩu nhập lại không khớp")));
        }

        @Test
        @DisplayName("Đăng ký hợp lệ -> lưu tài khoản KHACHHANG, mật khẩu được băm BCrypt")
        void hopLe() throws Exception {
            mockMvc.perform(post("/dang-ky").with(csrf())
                            .param("tenDangNhap", "Tran.Binh")
                            .param("matKhau", "matkhau1")
                            .param("nhapLaiMatKhau", "matkhau1")
                            .param("hoTen", "Trần Văn Bình")
                            .param("email", "Binh.Tran@Gmail.com")
                            .param("soDienThoai", "0988000001"))
                    .andExpect(redirectedUrl("/dang-nhap"));

            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap("tran.binh").orElseThrow();
            assertThat(tk.getVaiTro().getMa()).isEqualTo("KHACHHANG");
            assertThat(tk.getEmail()).isEqualTo("binh.tran@gmail.com");
            assertThat(tk.getMatKhau()).startsWith("$2").isNotEqualTo("matkhau1");
            assertThat(passwordEncoder.matches("matkhau1", tk.getMatKhau())).isTrue();
        }

        @Test
        @DisplayName("Trùng tên đăng nhập -> báo lỗi ngay cạnh ô tên đăng nhập")
        void trungTenDangNhap() throws Exception {
            mockMvc.perform(post("/dang-ky").with(csrf())
                            .param("tenDangNhap", "ADMIN")
                            .param("matKhau", "matkhau1")
                            .param("nhapLaiMatKhau", "matkhau1")
                            .param("hoTen", "Người trùng tên")
                            .param("email", "trungten@kiemthu.vn")
                            .param("soDienThoai", "0988000002"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("form", "tenDangNhap", "nghiepVu"));
        }
    }

    @Nested
    @DisplayName("Phân quyền theo URL")
    class PhanQuyenUrl {

        @Test
        @DisplayName("Chưa đăng nhập: trang công khai xem được, trang cần đăng nhập bị chuyển về /dang-nhap")
        void chuaDangNhap() throws Exception {
            mockMvc.perform(get("/")).andExpect(status().isOk());
            mockMvc.perform(get("/dang-nhap")).andExpect(status().isOk());
            mockMvc.perform(get("/dang-ky")).andExpect(status().isOk());
            mockMvc.perform(get("/quan-tri")).andExpect(redirectedUrlPattern("**/dang-nhap"));
            mockMvc.perform(get("/tai-khoan")).andExpect(redirectedUrlPattern("**/dang-nhap"));
        }

        @Test
        @DisplayName("Chưa đăng nhập vẫn tải được CSS, JS, font, ảnh và Bootstrap (giao diện không bị vỡ)")
        void taiNguyenTinhCongKhai() throws Exception {
            for (String duongDan : new String[]{"/css/theme.css", "/js/app.js", "/fonts/be-vietnam-pro/be-vietnam-pro.css",
                    "/fonts/be-vietnam-pro/files/be-vietnam-pro-vietnamese-400-normal.woff2", "/images/bia-dang-nhap.jpg",
                    "/webjars/bootstrap/css/bootstrap.min.css"}) {
                mockMvc.perform(get(duongDan)).andExpect(status().isOk());
            }
        }

        @Test
        @WithUserDetails("khachhang")
        @DisplayName("Khách hàng: vào được trang cá nhân, KHÔNG vào được khu vực quản trị (403)")
        void khachHang() throws Exception {
            mockMvc.perform(get("/tai-khoan")).andExpect(status().isOk());
            mockMvc.perform(get("/quan-tri")).andExpect(status().isForbidden());
            mockMvc.perform(get("/quan-tri/tai-khoan")).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("nhanvien")
        @DisplayName("Nhân viên: vào được /quan-tri, KHÔNG vào được quản lý tài khoản và lịch sử đăng nhập")
        void nhanVien() throws Exception {
            mockMvc.perform(get("/quan-tri")).andExpect(status().isOk());
            mockMvc.perform(get("/quan-tri/tai-khoan")).andExpect(status().isForbidden());
            mockMvc.perform(get("/quan-tri/lich-su-dang-nhap")).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Quản trị viên: vào được mọi trang quản trị; tìm kiếm, phân trang, sắp xếp hoạt động")
        void quanTriVien() throws Exception {
            mockMvc.perform(get("/quan-tri")).andExpect(status().isOk());
            mockMvc.perform(get("/quan-tri/tai-khoan").param("tuKhoa", "admin").param("sort", "hoTen,desc").param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("admin@xemay.vn")));
            mockMvc.perform(get("/quan-tri/tai-khoan").param("sort", "cotKhongTonTai,asc"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/quan-tri/lich-su-dang-nhap")).andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Form POST thiếu CSRF token bị từ chối (403)")
        void thieuCsrf() throws Exception {
            mockMvc.perform(post("/quan-tri/tai-khoan/{id}/khoa", 2)).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Menu hiển thị theo vai trò")
    class MenuTheoVaiTro {

        @Test
        @WithUserDetails("admin")
        @DisplayName("Quản trị viên thấy menu Quản trị và Quản lý tài khoản")
        void admin() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(content().string(containsString("href=\"/quan-tri\"")))
                    .andExpect(content().string(containsString("href=\"/quan-tri/tai-khoan\"")));
        }

        @Test
        @WithUserDetails("nhanvien")
        @DisplayName("Nhân viên thấy menu Quản trị nhưng không thấy Quản lý tài khoản")
        void nhanVien() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(content().string(containsString("href=\"/quan-tri\"")))
                    .andExpect(content().string(not(containsString("href=\"/quan-tri/tai-khoan\""))));
        }

        @Test
        @WithUserDetails("khachhang")
        @DisplayName("Khách hàng không thấy menu Quản trị")
        void khachHang() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(content().string(containsString("Khách hàng mẫu")))
                    .andExpect(content().string(not(containsString("href=\"/quan-tri\""))));
        }
    }

    @Nested
    @DisplayName("Phân quyền theo phương thức (@PreAuthorize) và quy tắc nghiệp vụ")
    class PhanQuyenPhuongThuc {

        @Test
        @WithUserDetails("nhanvien")
        @DisplayName("Nhân viên gọi thẳng TaiKhoanService.khoa() vẫn bị từ chối")
        void nhanVienKhongDuocKhoaTaiKhoan() {
            TaiKhoan tk = taoKhachHangMoi();
            assertThatThrownBy(() -> taiKhoanService.khoa(tk.getId(), "nhanvien"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Quản trị viên không thể tự khóa hoặc tự xóa chính mình")
        void khongTuKhoaChinhMinh() {
            Integer adminId = taiKhoanRepository.findByTenDangNhap("admin").orElseThrow().getId();
            assertThatThrownBy(() -> taiKhoanService.khoa(adminId, "admin")).isInstanceOf(NghiepVuException.class);
            assertThatThrownBy(() -> taiKhoanService.xoa(adminId, "admin")).isInstanceOf(NghiepVuException.class);
        }

        @Test
        @DisplayName("Quản trị viên xóa tài khoản: tài khoản mất, nhật ký đăng nhập và hồ sơ khách hàng vẫn còn")
        void xoaTaiKhoanGiuLichSu() throws Exception {
            TaiKhoan tk = taoKhachHangMoi();
            mockMvc.perform(formLogin("/dang-nhap").user("tenDangNhap", tk.getTenDangNhap()).password("matKhau", "sai"));
            assertThat(choLichSu(tk.getTenDangNhap(), 1)).isEqualTo(1);

            UserDetails admin = taiKhoanDetailsService.loadUserByUsername("admin");
            mockMvc.perform(post("/quan-tri/tai-khoan/{id}/xoa", tk.getId()).with(user(admin)).with(csrf()))
                    .andExpect(redirectedUrl("/quan-tri/tai-khoan"))
                    .andExpect(flash().attribute("thongBaoThanhCong", "Đã xóa tài khoản."));

            assertThat(taiKhoanRepository.findById(tk.getId())).isEmpty();
            assertThat(choLichSu(tk.getTenDangNhap(), 1)).isEqualTo(1);
            // Hồ sơ khách hàng (và đơn hàng nếu có) được giữ lại, chỉ không còn gắn với tài khoản
            KhachHang hoSo = khachHangRepository.findAll().stream()
                    .filter(kh -> kh.getSoDienThoai().equals(tk.getSoDienThoai()))
                    .findFirst().orElseThrow();
            assertThat(hoSo.getTaiKhoan()).isNull();
        }
    }
}
