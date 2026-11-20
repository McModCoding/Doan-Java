package com.xemay.security;

import com.xemay.config.SecurityConfig;
import com.xemay.entity.LichSuDangNhap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Chạy khi đăng nhập trên web THẤT BẠI: đếm lần sai, ghi nhật ký rồi quay lại trang đăng nhập
 * kèm mã lỗi trên URL (/dang-nhap?loi=sai | tam-khoa | bi-khoa | he-thong).
 */
@Component
public class DangNhapThatBaiHandler implements AuthenticationFailureHandler {

    /** Tên thuộc tính trong session giữ tên đăng nhập vừa gõ, để điền sẵn lại vào form. */
    public static final String TEN_VUA_NHAP = "TEN_DANG_NHAP_VUA_NHAP";

    private final TheoDoiDangNhap theoDoiDangNhap;

    public DangNhapThatBaiHandler(TheoDoiDangNhap theoDoiDangNhap) {
        this.theoDoiDangNhap = theoDoiDangNhap;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String tenDangNhap = request.getParameter(SecurityConfig.THAM_SO_TEN_DANG_NHAP);
        LyDoThatBai lyDo = theoDoiDangNhap.thatBai(tenDangNhap, request.getRemoteAddr(),
                LichSuDangNhap.KENH_WEB, exception);

        if (tenDangNhap != null && tenDangNhap.length() <= 30) {
            request.getSession().setAttribute(TEN_VUA_NHAP, tenDangNhap.trim());
        }
        response.sendRedirect(request.getContextPath() + "/dang-nhap?loi=" + lyDo.getMa());
    }
}
