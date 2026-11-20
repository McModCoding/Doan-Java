package com.xemay.security;

import com.xemay.entity.LichSuDangNhap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Chạy khi đăng nhập trên web THÀNH CÔNG: ghi nhật ký rồi chuyển hướng.
 *
 * <ul>
 *   <li>Nếu trước đó người dùng bị chặn ở một trang cần đăng nhập (ví dụ /gio-hang) thì quay lại đúng trang đó.</li>
 *   <li>Nếu không: quản trị viên và nhân viên vào /quan-tri, khách hàng về trang chủ.</li>
 * </ul>
 */
@Component
public class DangNhapThanhCongHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final TheoDoiDangNhap theoDoiDangNhap;

    public DangNhapThanhCongHandler(TheoDoiDangNhap theoDoiDangNhap, RequestCache requestCache) {
        this.theoDoiDangNhap = theoDoiDangNhap;
        setRequestCache(requestCache);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        theoDoiDangNhap.thanhCong(authentication.getName(), request.getRemoteAddr(), LichSuDangNhap.KENH_WEB);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        boolean laNhanSu = authentication.getPrincipal() instanceof TaiKhoanDetails nguoiDung && nguoiDung.laNhanSu();
        return laNhanSu ? "/quan-tri" : "/";
    }
}
