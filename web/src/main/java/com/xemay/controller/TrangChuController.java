package com.xemay.controller;

import com.xemay.service.GiaoDienXeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TrangChuController {
    private final GiaoDienXeService xeService;
    public TrangChuController(GiaoDienXeService xeService){this.xeService=xeService;}
    @GetMapping("/")
    public String trangChu(Model model) {
        model.addAttribute("noiBat",xeService.noiBat());
        return "index";
    }
}
