package com.xemay.controller.quantri;

import com.xemay.service.LichSuDangNhapService;
import com.xemay.util.PhanTrangUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/** Quản trị viên xem nhật ký đăng nhập (dữ liệu do tác vụ nền @Async ghi lại). */
@Controller
public class LichSuDangNhapController {

    private static final Set<String> COT_SAP_XEP = Set.of("thoiGian", "tenDangNhap", "kenh", "thanhCong", "diaChiIp");

    private final LichSuDangNhapService lichSuDangNhapService;

    public LichSuDangNhapController(LichSuDangNhapService lichSuDangNhapService) {
        this.lichSuDangNhapService = lichSuDangNhapService;
    }

    @GetMapping("/quan-tri/lich-su-dang-nhap")
    public String danhSach(@RequestParam(name = "tuKhoa", defaultValue = "") String tuKhoa,
                           @PageableDefault(size = 15) Pageable pageable, Model model) {
        Pageable phanTrang = PhanTrangUtils.chiSapXepTheo(
                pageable, Sort.by(Sort.Direction.DESC, "thoiGian"), COT_SAP_XEP);
        model.addAttribute("trang", lichSuDangNhapService.timKiem(tuKhoa, phanTrang));
        model.addAttribute("tuKhoa", tuKhoa.trim());
        return "quan-tri/lich-su-dang-nhap";
    }
}
