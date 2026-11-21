package com.xemay.controller.quantri;

import com.xemay.entity.HangXe;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.NghiepVuException;
import com.xemay.security.TaiKhoanDetails;
import com.xemay.service.HangXeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quản lý hãng xe trong khu vực quản trị - phần của thành viên phụ trách xe máy, hãng xe.
 *
 * <p>Tích hợp: đổi {@code /hangxe} thành {@code /quan-tri/hang-xe}; khi nhập sai thì hiện lại form kèm dữ liệu
 * vừa gõ (bản gốc chuyển hướng về form trống); chỉ nhận các ô của form ({@link #chiNhanOCuaForm}).
 */
@Controller
@RequestMapping("/quan-tri/hang-xe")
public class HangXeController {

    private static final String DANH_SACH = "redirect:/quan-tri/hang-xe";
    private static final String VIEW_FORM = "quan-tri/hang-xe/form";

    private final HangXeService hangXeService;

    public HangXeController(HangXeService hangXeService) {
        this.hangXeService = hangXeService;
    }

    /**
     * Form gắn thẳng vào Entity HangXe, nên chỉ cho phép đúng các ô có trên form. Nếu không, người dùng có thể
     * tự thêm tham số {@code maHang=1} để ghi đè hãng khác, hoặc {@code trangThai} khi không có quyền.
     */
    @InitBinder("hangXe")
    void chiNhanOCuaForm(WebDataBinder binder) {
        binder.setAllowedFields("tenHang", "quocGia", "moTa", "trangThai");
    }

    // 1. Danh sách hãng xe
    @GetMapping
    public String danhSach(
            @RequestParam(name = "tenHang", required = false) String tenHang,
            Model model) {

        model.addAttribute("dsHangXe", hangXeService.searchHangXe(tenHang));
        model.addAttribute("soXeTheoHang", hangXeService.demXeTheoHang());
        model.addAttribute("tenHang", tenHang == null ? "" : tenHang.trim());

        return "quan-tri/hang-xe/danh-sach";
    }

    // 2. Hiển thị form thêm hãng xe
    @GetMapping("/them")
    public String hienThiFormThem(Model model) {

        model.addAttribute("hangXe", new HangXe());

        return VIEW_FORM;
    }

    // 3. Lưu hãng xe mới
    @PostMapping("/them")
    public String themHangXe(
            @ModelAttribute("hangXe") HangXe hangXe,
            BindingResult ketQua,
            @AuthenticationPrincipal TaiKhoanDetails nguoiDung,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!nguoiDung.laQuanTriVien()) {
            hangXe.setTrangThai(true);
        }
        try {
            hangXeService.addHangXe(hangXe);

            redirectAttributes.addFlashAttribute(
                    "thongBaoThanhCong",
                    "Đã thêm hãng xe \"" + hangXe.getTenHang() + "\"."
            );

            return DANH_SACH;

        } catch (DuLieuKhongHopLeException e) {
            ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
        } catch (NghiepVuException e) {
            model.addAttribute("thongBaoLoi", e.getMessage());
        }
        return VIEW_FORM;
    }

    // 4. Hiển thị form sửa hãng xe
    @GetMapping("/{id}/sua")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model) {

        model.addAttribute("hangXe", hangXeService.getHangXeById(id));   // không có -> trang 404

        return VIEW_FORM;
    }

    // 5. Cập nhật hãng xe
    @PostMapping("/{id}/sua")
    public String capNhatHangXe(
            @PathVariable("id") Integer id,
            @ModelAttribute("hangXe") HangXe hangXe,
            BindingResult ketQua,
            @AuthenticationPrincipal TaiKhoanDetails nguoiDung,
            Model model,
            RedirectAttributes redirectAttributes) {

        hangXe.setMaHang(id);
        if (!nguoiDung.laQuanTriVien()) {
            hangXe.setTrangThai(null);   // giữ nguyên trạng thái: chỉ quản trị viên được ngừng / mở lại hãng
        }
        try {
            hangXeService.updateHangXe(id, hangXe);

            redirectAttributes.addFlashAttribute(
                    "thongBaoThanhCong",
                    "Đã cập nhật hãng xe \"" + hangXe.getTenHang().trim() + "\"."
            );

            return DANH_SACH;

        } catch (DuLieuKhongHopLeException e) {
            ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
        } catch (NghiepVuException e) {
            model.addAttribute("thongBaoLoi", e.getMessage());
        }
        return VIEW_FORM;
    }

    // 6. Ngừng hoạt động hãng xe (chỉ quản trị viên - @PreAuthorize trong Service)
    @PostMapping("/{id}/ngung")
    public String xoaHangXe(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            HangXe hangXe = hangXeService.deleteHangXe(id);

            redirectAttributes.addFlashAttribute(
                    "thongBaoThanhCong",
                    "Đã ngừng hoạt động hãng xe \"" + hangXe.getTenHang() + "\"."
            );

        } catch (NghiepVuException e) {

            redirectAttributes.addFlashAttribute(
                    "thongBaoLoi",
                    e.getMessage()
            );
        }

        return DANH_SACH;
    }
}
