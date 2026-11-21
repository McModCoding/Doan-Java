package com.xemay.controller.quantri;

import com.xemay.network.KhoSocketServer;
import com.xemay.service.KhachHangService;
import com.xemay.service.TonKhoService;
import com.xemay.service.XeMayService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang tổng quan của khu vực quản trị (quản trị viên và nhân viên). */
@Controller
public class QuanTriController {

    private final XeMayService xeMayService;
    private final KhachHangService khachHangService;
    private final TonKhoService tonKhoService;
    /** Có thể không tồn tại nếu tắt Socket server bằng xemay.socket.enabled=false. */
    private final ObjectProvider<KhoSocketServer> khoSocketServer;

    public QuanTriController(XeMayService xeMayService, KhachHangService khachHangService,
                             TonKhoService tonKhoService, ObjectProvider<KhoSocketServer> khoSocketServer) {
        this.xeMayService = xeMayService;
        this.khachHangService = khachHangService;
        this.tonKhoService = tonKhoService;
        this.khoSocketServer = khoSocketServer;
    }

    @GetMapping("/quan-tri")
    public String tongQuan(Model model) {
        model.addAttribute("soXeDangBan", xeMayService.demXeDangKinhDoanh());
        model.addAttribute("soKhachHang", khachHangService.demTatCa());
        // Kết quả do tác vụ định kỳ quét sẵn - trang này chỉ đọc, không truy vấn lại bảng XE_MAY
        model.addAttribute("canhBaoTonKho", tonKhoService.getCanhBaoGanNhat());
        model.addAttribute("socket", khoSocketServer.getIfAvailable());
        return "quan-tri/tong-quan";
    }
}
