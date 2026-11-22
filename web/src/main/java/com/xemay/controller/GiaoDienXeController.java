package com.xemay.controller;

import com.xemay.service.GiaoDienXeService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

/** Trang khách hàng: danh sách xe, lọc, phân trang và xem chi tiết. */
@Controller
@RequestMapping("/xe-may")
public class GiaoDienXeController {
    private final GiaoDienXeService xeService;
    public GiaoDienXeController(GiaoDienXeService xeService){this.xeService=xeService;}

    @GetMapping
    public String danhSach(@RequestParam(defaultValue="") String q,
             @RequestParam(required=false) BigDecimal giaTu,
             @RequestParam(required=false) BigDecimal giaDen,
             @RequestParam(defaultValue="false") boolean conHang,
             @RequestParam(defaultValue="0") int page,
             @RequestParam(defaultValue="moi") String sapXep,Model model){
        if((giaTu!=null && giaTu.signum()<0) || (giaDen!=null && giaDen.signum()<0)
            || (giaTu!=null && giaDen!=null && giaTu.compareTo(giaDen)>0)){
            model.addAttribute("loiBoLoc","Khoảng giá chưa hợp lệ. Vui lòng kiểm tra lại.");
            giaTu=null;giaDen=null;
        }
        Page<?> ds=xeService.danhSach(q,giaTu,giaDen,conHang,page,sapXep);
        model.addAttribute("ds",ds);
        model.addAttribute("q",q);
        model.addAttribute("giaTu",giaTu);
        model.addAttribute("giaDen",giaDen);
        model.addAttribute("conHang",conHang);
        model.addAttribute("sapXep",sapXep);
        return "xe-may/danh-sach";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Integer id,Model model){
        model.addAttribute("xe",xeService.chiTiet(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy xe")));
        return "xe-may/chi-tiet";
    }
}
