package com.xemay.controller;

import com.xemay.dto.DangKyForm;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.security.DangNhapThatBaiHandler;
import com.xemay.security.LyDoThatBai;
import com.xemay.security.TheoDoiDangNhap;
import com.xemay.service.TaiKhoanService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Trang đăng nhập và đăng ký.
 *
 * <p>Lưu ý: KHÔNG có phương thức xử lý POST /dang-nhap ở đây. Việc kiểm tra tên đăng nhập và mật khẩu do
 * bộ lọc của Spring Security đảm nhận (cấu hình trong SecurityConfig); Controller chỉ hiển thị form
 * và thông báo kết quả.
 */
@Controller
public class XacThucController {

    private final TaiKhoanService taiKhoanService;
    private final TheoDoiDangNhap theoDoiDangNhap;

    public XacThucController(TaiKhoanService taiKhoanService, TheoDoiDangNhap theoDoiDangNhap) {
        this.taiKhoanService = taiKhoanService;
        this.theoDoiDangNhap = theoDoiDangNhap;
    }

    /**
     * Trang đăng nhập, đăng ký tự hiển thị thông báo ngay trên form (gần chỗ người dùng đang nhìn),
     * nên báo cho bố cục chung không hiển thị thêm một lần ở đầu trang.
     */
    @ModelAttribute("anThongBaoChung")
    public boolean anThongBaoChung() {
        return true;
    }

    @GetMapping("/dang-nhap")
    public String trangDangNhap(@RequestParam(name = "loi", required = false) String loi,
                                @RequestParam(name = "dangXuat", required = false) String dangXuat,
                                @RequestParam(name = "hetPhien", required = false) String hetPhien,
                                Authentication nguoiDung, HttpSession session, Model model) {
        if (nguoiDung != null && nguoiDung.isAuthenticated()) {
            return "redirect:/";   // đã đăng nhập rồi thì không cần xem lại form
        }
        if (loi != null) {
            model.addAttribute("thongBaoLoi", theoDoiDangNhap.thongBao(LyDoThatBai.tuMa(loi)));
        } else if (hetPhien != null) {
            model.addAttribute("thongBaoLoi", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
        } else if (dangXuat != null) {
            model.addAttribute("thongBaoThanhCong", "Bạn đã đăng xuất.");
        }
        // Điền sẵn tên đăng nhập vừa gõ sai (handler đăng nhập thất bại đã cất vào session)
        model.addAttribute("tenVuaNhap", session.getAttribute(DangNhapThatBaiHandler.TEN_VUA_NHAP));
        session.removeAttribute(DangNhapThatBaiHandler.TEN_VUA_NHAP);
        return "xac-thuc/dang-nhap";
    }

    @GetMapping("/dang-ky")
    public String trangDangKy(Authentication nguoiDung, Model model) {
        if (nguoiDung != null && nguoiDung.isAuthenticated()) {
            return "redirect:/";
        }
        model.addAttribute("form", new DangKyForm());
        return "xac-thuc/dang-ky";
    }

    @PostMapping("/dang-ky")
    public String dangKy(@Valid @ModelAttribute("form") DangKyForm form, BindingResult ketQua,
                         RedirectAttributes chuyenHuong) {
        if (ketQua.hasErrors()) {
            return "xac-thuc/dang-ky";          // hiển thị lại form kèm lỗi cạnh từng ô
        }
        try {
            taiKhoanService.dangKy(form);
        } catch (DuLieuKhongHopLeException e) { // trùng tên đăng nhập hoặc email
            ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            return "xac-thuc/dang-ky";
        }
        chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đăng ký thành công! Mời bạn đăng nhập.");
        return "redirect:/dang-nhap";
    }
}
