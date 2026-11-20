package com.xemay.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang chủ TẠM THỜI để có nơi đứng sau khi đăng nhập.
 * Thành viên phụ trách giao diện thay nội dung templates/index.html (và lớp này nếu cần) bằng trang chủ thật.
 */
@Controller
public class TrangChuController {

    @GetMapping("/")
    public String trangChu() {
        return "index";
    }
}
