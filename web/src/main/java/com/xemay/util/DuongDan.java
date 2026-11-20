package com.xemay.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tạo đường dẫn "trang hiện tại nhưng đổi một vài tham số" - dùng trong template cho phân trang và sắp xếp,
 * để khi bấm sang trang 2 thì từ khóa tìm kiếm, bộ lọc... vẫn được giữ nguyên.
 *
 * <p>Gọi trong Thymeleaf qua tên bean {@code duongDan}:
 * <pre>
 * th:href="${@duongDan.thay('page', 2)}"                       &lt;!-- /quan-tri/tai-khoan?tuKhoa=an&amp;page=2 --&gt;
 * th:href="${@duongDan.thay('sort', 'hoTen,asc', 'page', 1)}"
 * </pre>
 */
@Component("duongDan")
public class DuongDan {

    /** Spring tiêm một "proxy": mỗi lần gọi sẽ trỏ tới đúng request của luồng đang xử lý. */
    private final HttpServletRequest request;

    public DuongDan(HttpServletRequest request) {
        this.request = request;
    }

    public String thay(String ten, Object giaTri) {
        Map<String, Object> thayThe = new LinkedHashMap<>();
        thayThe.put(ten, giaTri);
        return xayDung(thayThe);
    }

    public String thay(String ten1, Object giaTri1, String ten2, Object giaTri2) {
        Map<String, Object> thayThe = new LinkedHashMap<>();
        thayThe.put(ten1, giaTri1);
        thayThe.put(ten2, giaTri2);
        return xayDung(thayThe);
    }

    /**
     * Trang hiện tại có nằm dưới đường dẫn {@code tienTo} không - dùng để tô sáng mục menu đang mở:
     * {@code th:classappend="${@duongDan.dangO('/quan-tri/tai-khoan')} ? 'active'"}
     */
    public boolean dangO(String tienTo) {
        String duongDan = duongDanHienTai();
        return duongDan.equals(tienTo) || duongDan.startsWith(tienTo.endsWith("/") ? tienTo : tienTo + "/");
    }

    /** Trang hiện tại có đúng là {@code duongDan} không (so khớp chính xác, dùng cho mục "Trang chủ"). */
    public boolean laTrang(String duongDan) {
        return duongDanHienTai().equals(duongDan);
    }

    private String duongDanHienTai() {
        String duongDan = request.getRequestURI().substring(request.getContextPath().length());
        return duongDan.isEmpty() ? "/" : duongDan;
    }

    private String xayDung(Map<String, Object> thayThe) {
        StringBuilder url = new StringBuilder(request.getRequestURI());
        char dauNoi = '?';
        // Giữ lại các tham số đang có, trừ những tham số sắp bị thay và token CSRF
        for (Map.Entry<String, String[]> thamSo : request.getParameterMap().entrySet()) {
            if (thayThe.containsKey(thamSo.getKey()) || "_csrf".equals(thamSo.getKey())) {
                continue;
            }
            for (String giaTri : thamSo.getValue()) {
                noi(url, dauNoi, thamSo.getKey(), giaTri);
                dauNoi = '&';
            }
        }
        for (Map.Entry<String, Object> thamSo : thayThe.entrySet()) {
            if (thamSo.getValue() != null) {
                noi(url, dauNoi, thamSo.getKey(), String.valueOf(thamSo.getValue()));
                dauNoi = '&';
            }
        }
        return url.toString();
    }

    private static void noi(StringBuilder url, char dauNoi, String ten, String giaTri) {
        url.append(dauNoi)
                .append(URLEncoder.encode(ten, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(giaTri, StandardCharsets.UTF_8));
    }
}
