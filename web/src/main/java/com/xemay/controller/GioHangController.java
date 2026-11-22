package com.xemay.controller;

import com.xemay.entity.DonHang;
import com.xemay.exception.NghiepVuException;
import com.xemay.security.TaiKhoanDetails;
import com.xemay.service.DatHangService;
import com.xemay.service.GioHangService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/** Giỏ hàng của khách (lưu trong CSDL) và đặt hàng. Chỉ vai trò KHACHHANG vào được (SecurityConfig). */
@Controller
@RequestMapping("/gio-hang")
public class GioHangController {
    private final GioHangService gio;
    private final DatHangService datHang;
    public GioHangController(GioHangService gio, DatHangService datHang){this.gio=gio;this.datHang=datHang;}

    @GetMapping
    public String xem(@AuthenticationPrincipal TaiKhoanDetails nguoiDung, Model model){
        model.addAttribute("gio",gio.hienThi(nguoiDung.getId()));
        model.addAttribute("diaChiMacDinh",gio.diaChiMacDinh(nguoiDung.getId()));
        return "gio-hang/xem";
    }

    @PostMapping("/them")
    public String them(@AuthenticationPrincipal TaiKhoanDetails nguoiDung,
                @RequestParam Integer xeId,
                @RequestParam(defaultValue="1") int soLuong,
                RedirectAttributes a){
        try {gio.them(nguoiDung.getId(),xeId,soLuong);a.addFlashAttribute("thongBaoThanhCong","Đã thêm xe vào giỏ hàng");}
        catch(NghiepVuException e){a.addFlashAttribute("thongBaoLoi",e.getMessage());}
        return "redirect:/gio-hang";
    }
    @PostMapping("/cap-nhat")
    public String capNhat(@AuthenticationPrincipal TaiKhoanDetails nguoiDung,
                @RequestParam Integer xeId,@RequestParam int soLuong,RedirectAttributes a){
        try{gio.capNhat(nguoiDung.getId(),xeId,soLuong);a.addFlashAttribute("thongBaoThanhCong","Đã cập nhật giỏ hàng");}
        catch(NghiepVuException e){a.addFlashAttribute("thongBaoLoi",e.getMessage());}
        return "redirect:/gio-hang";
    }
    @PostMapping("/xoa")
    public String xoa(@AuthenticationPrincipal TaiKhoanDetails nguoiDung,@RequestParam Integer xeId,RedirectAttributes a){
        gio.xoa(nguoiDung.getId(),xeId);a.addFlashAttribute("thongBaoThanhCong","Đã xóa xe khỏi giỏ hàng");
        return "redirect:/gio-hang";
    }

    /** Đặt mua các xe trong giỏ: tạo đơn hàng và trừ kho trong cùng một giao dịch (DatHangService). */
    @PostMapping("/dat-hang")
    public String datHang(@AuthenticationPrincipal TaiKhoanDetails nguoiDung,
                @RequestParam(defaultValue="") String diaChiGiaoHang,
                @RequestParam(required=false) String ghiChu,
                RedirectAttributes a){
        try{
            DonHang don=datHang.datHang(nguoiDung.getId(),diaChiGiaoHang,ghiChu);
            a.addFlashAttribute("thongBaoThanhCong",String.format(Locale.forLanguageTag("vi-VN"),
                    "Đặt hàng thành công. Mã đơn hàng: %d, tổng tiền %,d đ. Cửa hàng sẽ gọi điện xác nhận và hẹn ngày giao xe.",
                    don.getMaDonHang(),don.getTongTien().longValue()));
        }catch(NghiepVuException e){a.addFlashAttribute("thongBaoLoi",e.getMessage());}
        return "redirect:/gio-hang";
    }
}
