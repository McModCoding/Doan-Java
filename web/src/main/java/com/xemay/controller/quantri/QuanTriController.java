package com.xemay.controller.quantri;

import com.xemay.entity.VaiTro;
import com.xemay.network.KhoSocketServer;
import com.xemay.service.TaiKhoanService;
import com.xemay.service.TonKhoService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang tổng quan của khu vực quản trị (quản trị viên và nhân viên). */
@Controller
public class QuanTriController {

    private final TaiKhoanService taiKhoanService;
    private final TonKhoService tonKhoService;
    /** Có thể không tồn tại nếu tắt Socket server bằng xemay.socket.enabled=false. */
    private final ObjectProvider<KhoSocketServer> khoSocketServer;

    public QuanTriController(TaiKhoanService taiKhoanService, TonKhoService tonKhoService,
                             ObjectProvider<KhoSocketServer> khoSocketServer) {
        this.taiKhoanService = taiKhoanService;
        this.tonKhoService = tonKhoService;
        this.khoSocketServer = khoSocketServer;
    }

    @GetMapping("/quan-tri")
    public String tongQuan(Model model) {
        model.addAttribute("soTaiKhoan", taiKhoanService.demTatCa());
        model.addAttribute("soKhachHang", taiKhoanService.demTheoVaiTro(VaiTro.KHACHHANG));
        // Kết quả do tác vụ định kỳ quét sẵn - trang này chỉ đọc, không truy vấn lại bảng xe_may
        model.addAttribute("canhBaoTonKho", tonKhoService.getCanhBaoGanNhat());
        model.addAttribute("socket", khoSocketServer.getIfAvailable());
        return "quan-tri/tong-quan";
    }
}
