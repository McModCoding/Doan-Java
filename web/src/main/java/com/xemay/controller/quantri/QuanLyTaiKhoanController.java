package com.xemay.controller.quantri;

import com.xemay.dto.TaiKhoanForm;
import com.xemay.entity.TaiKhoan;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.NghiepVuException;
import com.xemay.service.TaiKhoanService;
import com.xemay.util.PhanTrangUtils;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Set;

/**
 * Quản trị viên quản lý tài khoản: danh sách (tìm kiếm, lọc, phân trang, sắp xếp), thêm, sửa,
 * khóa / mở khóa, đặt lại mật khẩu, xóa. Chỉ vai trò ADMIN vào được (xem SecurityConfig).
 */
@Controller
@RequestMapping("/quan-tri/tai-khoan")
public class QuanLyTaiKhoanController {

    private static final String DANH_SACH = "redirect:/quan-tri/tai-khoan";
    private static final String VIEW_FORM = "quan-tri/tai-khoan/form";

    /** Các cột được phép sắp xếp trên trang danh sách. */
    private static final Set<String> COT_SAP_XEP =
            Set.of("id", "tenDangNhap", "hoTen", "email", "vaiTro.ten", "kichHoat", "ngayTao");

    private final TaiKhoanService taiKhoanService;

    public QuanLyTaiKhoanController(TaiKhoanService taiKhoanService) {
        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping
    public String danhSach(@RequestParam(name = "tuKhoa", defaultValue = "") String tuKhoa,
                           @RequestParam(name = "vaiTroId", required = false) Integer vaiTroId,
                           @PageableDefault(size = 10) Pageable pageable, Model model) {
        Pageable phanTrang = PhanTrangUtils.chiSapXepTheo(pageable, Sort.by("id"), COT_SAP_XEP);
        model.addAttribute("trang", taiKhoanService.timKiem(tuKhoa, vaiTroId, phanTrang));
        model.addAttribute("tuKhoa", tuKhoa.trim());
        model.addAttribute("vaiTroId", vaiTroId);
        model.addAttribute("danhSachVaiTro", taiKhoanService.danhSachVaiTro());
        return "quan-tri/tai-khoan/danh-sach";
    }

    // ------------------------------------------------------------------ Thêm

    @GetMapping("/them")
    public String formThem(Model model) {
        model.addAttribute("form", new TaiKhoanForm());
        return hienForm(model);
    }

    @PostMapping("/them")
    public String them(@Validated({Default.class, TaiKhoanForm.KhiThem.class}) @ModelAttribute("form") TaiKhoanForm form,
                       BindingResult ketQua, Model model, RedirectAttributes chuyenHuong) {
        form.setId(null);
        if (!ketQua.hasErrors()) {
            try {
                TaiKhoan taiKhoan = taiKhoanService.them(form);
                chuyenHuong.addFlashAttribute("thongBaoThanhCong",
                        "Đã thêm tài khoản \"" + taiKhoan.getTenDangNhap() + "\".");
                return DANH_SACH;
            } catch (DuLieuKhongHopLeException e) {
                ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            }
        }
        return hienForm(model);
    }

    // ------------------------------------------------------------------ Sửa

    @GetMapping("/{id}/sua")
    public String formSua(@PathVariable("id") Long id, Model model) {
        TaiKhoan taiKhoan = taiKhoanService.timTheoId(id);
        TaiKhoanForm form = new TaiKhoanForm();
        form.setId(taiKhoan.getId());
        form.setTenDangNhap(taiKhoan.getTenDangNhap());
        form.setHoTen(taiKhoan.getHoTen());
        form.setEmail(taiKhoan.getEmail());
        form.setSoDienThoai(taiKhoan.getSoDienThoai());
        form.setVaiTroId(taiKhoan.getVaiTro().getId());
        form.setKichHoat(taiKhoan.isKichHoat());
        model.addAttribute("form", form);
        return hienForm(model);
    }

    @PostMapping("/{id}/sua")
    public String sua(@PathVariable("id") Long id,
                      @Valid @ModelAttribute("form") TaiKhoanForm form, BindingResult ketQua,
                      Principal nguoiDung, Model model, RedirectAttributes chuyenHuong) {
        // Mã và tên đăng nhập lấy từ CSDL theo {id} trên URL, không tin giá trị gửi lên từ form
        TaiKhoan hienTai = taiKhoanService.timTheoId(id);
        form.setId(hienTai.getId());
        form.setTenDangNhap(hienTai.getTenDangNhap());
        if (!ketQua.hasErrors()) {
            try {
                taiKhoanService.capNhat(id, form, nguoiDung.getName());
                chuyenHuong.addFlashAttribute("thongBaoThanhCong",
                        "Đã cập nhật tài khoản \"" + hienTai.getTenDangNhap() + "\".");
                return DANH_SACH;
            } catch (DuLieuKhongHopLeException e) {
                ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            } catch (NghiepVuException e) {
                model.addAttribute("thongBaoLoi", e.getMessage());
            }
        }
        return hienForm(model);
    }

    // ------------------------------------------------------------------ Các thao tác nhanh

    @PostMapping("/{id}/khoa")
    public String khoa(@PathVariable("id") Long id, Principal nguoiDung, RedirectAttributes chuyenHuong) {
        try {
            TaiKhoan taiKhoan = taiKhoanService.khoa(id, nguoiDung.getName());
            chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã khóa tài khoản \"" + taiKhoan.getTenDangNhap() + "\".");
        } catch (NghiepVuException e) {
            chuyenHuong.addFlashAttribute("thongBaoLoi", e.getMessage());
        }
        return DANH_SACH;
    }

    @PostMapping("/{id}/mo-khoa")
    public String moKhoa(@PathVariable("id") Long id, RedirectAttributes chuyenHuong) {
        TaiKhoan taiKhoan = taiKhoanService.moKhoa(id);
        chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã mở khóa tài khoản \"" + taiKhoan.getTenDangNhap() + "\".");
        return DANH_SACH;
    }

    @PostMapping("/{id}/dat-lai-mat-khau")
    public String datLaiMatKhau(@PathVariable("id") Long id,
                                @RequestParam(name = "matKhauMoi", defaultValue = "") String matKhauMoi,
                                RedirectAttributes chuyenHuong) {
        try {
            TaiKhoan taiKhoan = taiKhoanService.datLaiMatKhau(id, matKhauMoi);
            chuyenHuong.addFlashAttribute("thongBaoThanhCong",
                    "Đã đặt lại mật khẩu cho tài khoản \"" + taiKhoan.getTenDangNhap() + "\".");
            return DANH_SACH;
        } catch (NghiepVuException e) {
            chuyenHuong.addFlashAttribute("thongBaoLoi", e.getMessage());
            return "redirect:/quan-tri/tai-khoan/" + id + "/sua";
        }
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable("id") Long id, Principal nguoiDung, RedirectAttributes chuyenHuong) {
        try {
            taiKhoanService.xoa(id, nguoiDung.getName());
            chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã xóa tài khoản.");
        } catch (NghiepVuException e) {   // tự xóa mình, hoặc tài khoản đang được bảng khác tham chiếu
            chuyenHuong.addFlashAttribute("thongBaoLoi", e.getMessage());
        }
        return DANH_SACH;
    }

    private String hienForm(Model model) {
        model.addAttribute("danhSachVaiTro", taiKhoanService.danhSachVaiTro());
        return VIEW_FORM;
    }
}
