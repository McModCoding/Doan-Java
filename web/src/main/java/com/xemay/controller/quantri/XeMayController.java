package com.xemay.controller.quantri;

import com.xemay.dto.BoLocXeMay;
import com.xemay.dto.XeMayDTO;
import com.xemay.entity.HangXe;
import com.xemay.entity.XeMay;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.NghiepVuException;
import com.xemay.security.TaiKhoanDetails;
import com.xemay.service.HangXeService;
import com.xemay.service.TonKhoService;
import com.xemay.service.XeMayService;
import com.xemay.util.PhanTrangUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.math.RoundingMode;
import java.util.Set;

/**
 * Quản lý xe máy trong khu vực quản trị - phần của thành viên phụ trách xe máy, hãng xe.
 *
 * <p>Tích hợp: đổi đường dẫn {@code /xemay} thành {@code /quan-tri/xe-may} để SecurityConfig chặn người
 * không phải nhân sự; đường dẫn từng thao tác theo cùng kiểu với trang quản lý tài khoản
 * ({@code /them}, {@code /{id}/sua}...); thêm lọc theo trạng thái, sắp xếp, nhập kho; ngừng kinh doanh chỉ
 * dành cho quản trị viên. Logic chính (chuyển DTO, gọi Service, báo lỗi) giữ như bản gốc.
 */
@Controller
@RequestMapping("/quan-tri/xe-may")
public class XeMayController {

    private static final String DANH_SACH = "redirect:/quan-tri/xe-may";
    private static final String VIEW_FORM = "quan-tri/xe-may/form";

    /** Các cột được phép sắp xếp trên trang danh sách. */
    private static final Set<String> COT_SAP_XEP =
            Set.of("maXe", "tenXe", "hangXe.tenHang", "loaiXe", "giaBan", "soLuong", "trangThai");

    private final XeMayService xeMayService;
    private final HangXeService hangXeService;
    private final TonKhoService tonKhoService;

    public XeMayController(
            XeMayService xeMayService,
            HangXeService hangXeService,
            TonKhoService tonKhoService) {

        this.xeMayService = xeMayService;
        this.hangXeService = hangXeService;
        this.tonKhoService = tonKhoService;
    }

    // 1. Danh sách, tìm kiếm, lọc và phân trang
    @GetMapping
    public String danhSach(@ModelAttribute("boLoc") BoLocXeMay boLoc, BindingResult loiBoLoc,
                           @PageableDefault(size = 10) Pageable pageable, Model model) {

        Pageable phanTrang = PhanTrangUtils.chiSapXepTheo(
                pageable, Sort.by("maXe").descending(), COT_SAP_XEP, "maXe");

        if (loiBoLoc.hasErrors()) {
            // Ví dụ ?giaMin=abc: bỏ điều kiện sai, vẫn hiện danh sách
            loiBoLoc.getFieldErrors().forEach(loi -> {
                switch (loi.getField()) {
                    case "giaMin" -> boLoc.setGiaMin(null);
                    case "giaMax" -> boLoc.setGiaMax(null);
                    case "maHang" -> boLoc.setMaHang(null);
                    case "trangThai" -> boLoc.setTrangThai(null);
                    default -> { }
                }
            });
            model.addAttribute("thongBaoLoi", "Một số điều kiện lọc không hợp lệ đã được bỏ qua.");
        }

        Page<XeMay> trangXe;
        try {
            trangXe = xeMayService.timKiemVaLoc(
                    boLoc.getTenXe(),
                    boLoc.getMaHang(),
                    boLoc.getLoaiXe(),
                    boLoc.getGiaMin(),
                    boLoc.getGiaMax(),
                    boLoc.getTrangThai(),
                    phanTrang
            );
        } catch (NghiepVuException e) {
            model.addAttribute("thongBaoLoi", e.getMessage());
            trangXe = Page.empty(phanTrang);
        }

        model.addAttribute("trang", trangXe);
        model.addAttribute("nguongCanhBao", tonKhoService.getCanhBaoGanNhat().nguong());
        model.addAttribute("dsHangXe", hangXeService.getAllHangXe());
        model.addAttribute("dsLoaiXe", xeMayService.getDanhSachLoaiXe());
        return "quan-tri/xe-may/danh-sach";
    }

    // 2. Hiển thị form thêm xe
    @GetMapping("/them")
    public String hienThiFormThem(Model model) {

        XeMayDTO dto = new XeMayDTO();
        dto.setSoLuong(0);
        model.addAttribute("xeMayDTO", dto);
        return hienForm(model);
    }

    // 3. Lưu xe mới
    @PostMapping("/them")
    public String themXeMay(
            @Valid @ModelAttribute("xeMayDTO") XeMayDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        dto.setMaXe(null);
        if (bindingResult.hasErrors()) {
            return hienForm(model);
        }

        try {
            XeMay xeMay = xeMayService.addXeMay(chuyenDTOThanhEntity(dto));

            redirectAttributes.addFlashAttribute(
                    "thongBaoThanhCong",
                    "Đã thêm xe \"" + xeMay.getTenXe() + "\"."
            );

            return DANH_SACH;

        } catch (DuLieuKhongHopLeException e) {
            bindingResult.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
        } catch (NghiepVuException e) {
            model.addAttribute("thongBaoLoi", e.getMessage());
        }
        return hienForm(model);
    }

    // 4. Hiển thị form sửa xe
    @GetMapping("/{id}/sua")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model) {

        XeMay xeMay = xeMayService.getXeMayById(id);   // không có xe -> trang 404
        model.addAttribute("xeMayDTO", chuyenEntityThanhDTO(xeMay));
        model.addAttribute("xeMay", xeMay);
        return hienForm(model);
    }

    // 5. Cập nhật xe (số lượng tồn KHÔNG đổi ở đây)
    @PostMapping("/{id}/sua")
    public String capNhatXeMay(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("xeMayDTO") XeMayDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal TaiKhoanDetails nguoiDung,
            Model model,
            RedirectAttributes redirectAttributes) {

        XeMay hienTai = xeMayService.getXeMayById(id);
        dto.setMaXe(id);
        if (!nguoiDung.laQuanTriVien()) {
            dto.setTrangThai(null);   // chỉ quản trị viên được ngừng / mở bán lại xe
        }
        if (!bindingResult.hasErrors()) {
            try {
                xeMayService.updateXeMay(id, chuyenDTOThanhEntity(dto));

                redirectAttributes.addFlashAttribute(
                        "thongBaoThanhCong",
                        "Đã cập nhật xe \"" + dto.getTenXe().trim() + "\"."
                );

                return DANH_SACH;

            } catch (DuLieuKhongHopLeException e) {
                bindingResult.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
            } catch (NghiepVuException e) {
                model.addAttribute("thongBaoLoi", e.getMessage());
            }
        }
        model.addAttribute("xeMay", hienTai);
        return hienForm(model);
    }

    // 6. Ngừng kinh doanh xe (chỉ quản trị viên - kiểm tra bằng @PreAuthorize trong Service)
    @PostMapping("/{id}/ngung-ban")
    public String ngungKinhDoanh(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {

        XeMay xeMay = xeMayService.deleteXeMay(id);
        redirectAttributes.addFlashAttribute(
                "thongBaoThanhCong",
                "Đã ngừng kinh doanh xe \"" + xeMay.getTenXe() + "\". Mở bán lại trong trang sửa xe."
        );
        return DANH_SACH;
    }

    // 7. Tích hợp: nhập thêm xe vào kho - đi qua TonKhoService (khóa dòng), như client quản lý kho
    @PostMapping("/{id}/nhap-kho")
    public String nhapKho(@PathVariable("id") Integer id,
                          @RequestParam(name = "soLuong", defaultValue = "0") String soLuong,
                          RedirectAttributes redirectAttributes) {
        try {
            int soLuongNhap;
            try {
                soLuongNhap = Integer.parseInt(soLuong.trim());
            } catch (NumberFormatException e) {
                throw new NghiepVuException("Số lượng nhập kho phải là số nguyên.");
            }
            int tonMoi = tonKhoService.nhapKho(id, soLuongNhap);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                    "Đã nhập thêm " + soLuongNhap + " xe. Tồn kho hiện tại: " + tonMoi + ".");
        } catch (NghiepVuException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", e.getMessage());
        }
        return "redirect:/quan-tri/xe-may/" + id + "/sua";
    }

    private String hienForm(Model model) {
        model.addAttribute("dsHangXe", hangXeService.getHangXeDangHoatDong());
        model.addAttribute("dsLoaiXe", xeMayService.getDanhSachLoaiXe());
        return VIEW_FORM;
    }

    // 8. Chuyển DTO sang Entity
    private XeMay chuyenDTOThanhEntity(XeMayDTO dto) {

        XeMay xeMay = new XeMay();

        HangXe hangXe = new HangXe();
        hangXe.setMaHang(dto.getMaHang());

        xeMay.setHangXe(hangXe);
        xeMay.setTenXe(dto.getTenXe());
        xeMay.setLoaiXe(dto.getLoaiXe() == null ? null : dto.getLoaiXe().trim());
        xeMay.setPhanKhoi(dto.getPhanKhoi());
        xeMay.setMauSac(rongThanhNull(dto.getMauSac()));
        xeMay.setGiaBan(dto.getGiaBan());
        xeMay.setSoLuong(dto.getSoLuong());
        xeMay.setHinhAnh(rongThanhNull(dto.getHinhAnh()));
        xeMay.setMoTa(rongThanhNull(dto.getMoTa()));
        xeMay.setTrangThai(dto.getTrangThai());

        return xeMay;
    }

    // 9. Chuyển Entity sang DTO
    private XeMayDTO chuyenEntityThanhDTO(XeMay xeMay) {

        XeMayDTO dto = new XeMayDTO();

        dto.setMaXe(xeMay.getMaXe());
        dto.setMaHang(xeMay.getHangXe().getMaHang());
        dto.setTenXe(xeMay.getTenXe());
        dto.setLoaiXe(xeMay.getLoaiXe());
        dto.setPhanKhoi(xeMay.getPhanKhoi());
        dto.setMauSac(xeMay.getMauSac());
        // Cột GiaBan là DECIMAL(18,2): bỏ phần ".00" để ô nhập hiện 32000000 thay vì 32000000.00
        dto.setGiaBan(xeMay.getGiaBan() == null ? null : xeMay.getGiaBan().setScale(0, RoundingMode.HALF_UP));
        dto.setSoLuong(xeMay.getSoLuong());
        dto.setHinhAnh(xeMay.getHinhAnh());
        dto.setMoTa(xeMay.getMoTa());
        dto.setTrangThai(xeMay.getTrangThai());

        return dto;
    }

    private static String rongThanhNull(String chuoi) {
        return (chuoi == null || chuoi.isBlank()) ? null : chuoi.trim();
    }
}
