package com.xemay.controller.quantri;

import com.xemay.dto.KhachHangForm;
import com.xemay.entity.KhachHang;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.NghiepVuException;
import com.xemay.service.KhachHangService;
import com.xemay.util.PhanTrangUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * Quản lý khách hàng trong khu vực quản trị - dựa trên {@code CustomerController} của thành viên phụ trách
 * khách hàng (danh sách + tìm theo tên hoặc số điện thoại, thêm, sửa, xóa).
 *
 * <p>Tích hợp: đổi {@code /customers} thành {@code /quan-tri/khach-hang}; trang danh sách đọc dữ liệu thật
 * từ CSDL (bản gốc là trang tĩnh với dữ liệu viết sẵn trong JavaScript); xóa bằng POST kèm xác nhận
 * (bản gốc dùng liên kết GET, bị lỗi CSRF và dễ xóa nhầm); chỉ quản trị viên được xóa.
 */
@Controller
@RequestMapping("/quan-tri/khach-hang")
public class KhachHangController {

    private static final String DANH_SACH = "redirect:/quan-tri/khach-hang";
    private static final String VIEW_FORM = "quan-tri/khach-hang/form";

    private static final Set<String> COT_SAP_XEP =
            Set.of("maKhachHang", "hoTen", "soDienThoai", "email", "ngayTao");

    private final KhachHangService khachHangService;

    public KhachHangController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }

    @GetMapping
    public String danhSach(@RequestParam(name = "tuKhoa", defaultValue = "") String tuKhoa,
                           @PageableDefault(size = 10) Pageable pageable, Model model) {
        Pageable phanTrang = PhanTrangUtils.chiSapXepTheo(
                pageable, Sort.by("maKhachHang").descending(), COT_SAP_XEP, "maKhachHang");
        model.addAttribute("trang", khachHangService.timKiem(tuKhoa, phanTrang));
        model.addAttribute("tuKhoa", tuKhoa.trim());
        return "quan-tri/khach-hang/danh-sach";
    }

    // ------------------------------------------------------------------ Thêm (khách mua tại cửa hàng)

    @GetMapping("/them")
    public String formThem(Model model) {
        model.addAttribute("form", new KhachHangForm());
        return VIEW_FORM;
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("form") KhachHangForm form, BindingResult ketQua,
                       Model model, RedirectAttributes chuyenHuong) {
        form.setMaKhachHang(null);
        form.setTenDangNhap(null);
        if (!ketQua.hasErrors()) {
            try {
                KhachHang khachHang = khachHangService.them(form);
                chuyenHuong.addFlashAttribute("thongBaoThanhCong",
                        "Đã thêm khách hàng \"" + khachHang.getHoTen() + "\".");
                return DANH_SACH;
            } catch (DuLieuKhongHopLeException e) {
                ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            } catch (NghiepVuException e) {
                model.addAttribute("thongBaoLoi", e.getMessage());
            }
        }
        return VIEW_FORM;
    }

    // ------------------------------------------------------------------ Sửa

    @GetMapping("/{id}/sua")
    public String formSua(@PathVariable("id") Integer id, Model model) {
        KhachHang khachHang = khachHangService.timTheoMa(id);
        KhachHangForm form = new KhachHangForm();
        form.setMaKhachHang(khachHang.getMaKhachHang());
        form.setHoTen(khachHang.getHoTen());
        form.setSoDienThoai(khachHang.getSoDienThoai());
        form.setEmail(khachHang.getEmail());
        form.setDiaChi(khachHang.getDiaChi());
        form.setNgaySinh(khachHang.getNgaySinh());
        form.setTenDangNhap(khachHang.coTaiKhoan() ? khachHang.getTaiKhoan().getTenDangNhap() : null);
        model.addAttribute("form", form);
        return VIEW_FORM;
    }

    @PostMapping("/{id}/sua")
    public String sua(@PathVariable("id") Integer id,
                      @Valid @ModelAttribute("form") KhachHangForm form, BindingResult ketQua,
                      Model model, RedirectAttributes chuyenHuong) {
        // Mã và tài khoản liên kết lấy từ CSDL theo {id} trên URL, không tin giá trị gửi lên từ form
        KhachHang hienTai = khachHangService.timTheoMa(id);
        form.setMaKhachHang(hienTai.getMaKhachHang());
        form.setTenDangNhap(hienTai.coTaiKhoan() ? hienTai.getTaiKhoan().getTenDangNhap() : null);
        if (hienTai.coTaiKhoan()) {
            form.setEmail(hienTai.getTaiKhoan().getEmail());   // email đăng nhập chỉ đổi ở trang tài khoản
        }
        if (!ketQua.hasErrors()) {
            try {
                khachHangService.capNhat(id, form);
                chuyenHuong.addFlashAttribute("thongBaoThanhCong",
                        "Đã cập nhật khách hàng \"" + form.getHoTen().trim() + "\".");
                return DANH_SACH;
            } catch (DuLieuKhongHopLeException e) {
                ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            } catch (NghiepVuException e) {
                model.addAttribute("thongBaoLoi", e.getMessage());
            }
        }
        return VIEW_FORM;
    }

    // ------------------------------------------------------------------ Xóa (chỉ quản trị viên)

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable("id") Integer id, RedirectAttributes chuyenHuong) {
        try {
            khachHangService.xoa(id);
            chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã xóa khách hàng.");
        } catch (NghiepVuException e) {   // đang có tài khoản, hoặc đã có giỏ hàng / đơn hàng
            chuyenHuong.addFlashAttribute("thongBaoLoi", e.getMessage());
        }
        return DANH_SACH;
    }
}
