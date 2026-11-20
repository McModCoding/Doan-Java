package com.xemay.controller;

import com.xemay.dto.DoiMatKhauForm;
import com.xemay.dto.ThongTinCaNhanForm;
import com.xemay.entity.TaiKhoan;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.service.TaiKhoanService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Người dùng đã đăng nhập (mọi vai trò) tự xem, sửa thông tin và đổi mật khẩu của mình. */
@Controller
@RequestMapping("/tai-khoan")
public class TaiKhoanCaNhanController {

    private final TaiKhoanService taiKhoanService;

    public TaiKhoanCaNhanController(TaiKhoanService taiKhoanService) {
        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping
    public String xemThongTin(Principal nguoiDung, Model model) {
        TaiKhoan taiKhoan = taiKhoanService.timTheoTenDangNhap(nguoiDung.getName());
        ThongTinCaNhanForm form = new ThongTinCaNhanForm();
        form.setHoTen(taiKhoan.getHoTen());
        form.setEmail(taiKhoan.getEmail());
        form.setSoDienThoai(taiKhoan.getSoDienThoai());
        model.addAttribute("form", form);
        model.addAttribute("taiKhoan", taiKhoan);
        return "tai-khoan/thong-tin";
    }

    @PostMapping
    public String luuThongTin(@Valid @ModelAttribute("form") ThongTinCaNhanForm form, BindingResult ketQua,
                              Principal nguoiDung, Model model, RedirectAttributes chuyenHuong) {
        if (!ketQua.hasErrors()) {
            try {
                taiKhoanService.capNhatThongTinCaNhan(nguoiDung.getName(), form);
                chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã lưu thông tin cá nhân.");
                return "redirect:/tai-khoan";
            } catch (DuLieuKhongHopLeException e) {
                ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            }
        }
        model.addAttribute("taiKhoan", taiKhoanService.timTheoTenDangNhap(nguoiDung.getName()));
        return "tai-khoan/thong-tin";
    }

    @GetMapping("/doi-mat-khau")
    public String trangDoiMatKhau(Model model) {
        model.addAttribute("form", new DoiMatKhauForm());
        return "tai-khoan/doi-mat-khau";
    }

    @PostMapping("/doi-mat-khau")
    public String doiMatKhau(@Valid @ModelAttribute("form") DoiMatKhauForm form, BindingResult ketQua,
                             Principal nguoiDung, RedirectAttributes chuyenHuong) {
        if (ketQua.hasErrors()) {
            return "tai-khoan/doi-mat-khau";
        }
        try {
            taiKhoanService.doiMatKhau(nguoiDung.getName(), form.getMatKhauHienTai(), form.getMatKhauMoi());
        } catch (DuLieuKhongHopLeException e) {   // mật khẩu hiện tại không đúng, mật khẩu mới trùng mật khẩu cũ
            ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            return "tai-khoan/doi-mat-khau";
        }
        chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đổi mật khẩu thành công.");
        return "redirect:/tai-khoan";
    }
}
