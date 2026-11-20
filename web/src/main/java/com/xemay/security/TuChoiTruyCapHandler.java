package com.xemay.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Chạy khi một request bị TỪ CHỐI:
 * <ul>
 *   <li>Đã đăng nhập nhưng không đủ quyền -> trang lỗi 403.</li>
 *   <li>Form POST thiếu hoặc sai CSRF token: nếu chưa đăng nhập (thường do phiên đã hết hạn) thì đưa về
 *       trang đăng nhập; nếu đang đăng nhập thì báo 403 và ghi log nhắc lập trình viên dùng th:action.</li>
 * </ul>
 */
@Component
public class TuChoiTruyCapHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(TuChoiTruyCapHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException loi) throws IOException {
        Authentication nguoiDung = SecurityContextHolder.getContext().getAuthentication();
        boolean chuaDangNhap = nguoiDung == null || nguoiDung instanceof AnonymousAuthenticationToken;

        if (loi instanceof CsrfException) {
            if (chuaDangNhap) {
                response.sendRedirect(request.getContextPath() + "/dang-nhap?hetPhien");
                return;
            }
            log.warn("Từ chối {} {} vì thiếu hoặc sai CSRF token. Mọi form POST phải viết th:action=\"@{...}\" "
                    + "để Thymeleaf tự chèn token.", request.getMethod(), request.getRequestURI());
        } else {
            log.warn("Người dùng '{}' không đủ quyền truy cập {} {}",
                    chuaDangNhap ? "(chưa đăng nhập)" : nguoiDung.getName(), request.getMethod(), request.getRequestURI());
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
