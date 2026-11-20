package com.xemay.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * XỬ LÝ NGOẠI LỆ CHUNG cho mọi Controller: biến ngoại lệ thành trang báo lỗi thân thiện (templates/error.html)
 * thay vì để người dùng nhìn thấy trang lỗi mặc định hoặc stack trace.
 *
 * <p>Các lỗi khác không liệt kê ở đây (404 do gõ sai địa chỉ, 403 do không đủ quyền, 500 do lỗi lập trình)
 * được Spring Boot tự chuyển tới cùng trang error.html. Cố ý KHÔNG bắt {@code Exception} chung chung,
 * vì làm vậy sẽ nuốt mất ngoại lệ phân quyền của Spring Security và biến lỗi 403/404 thành lỗi 500.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(KhongTimThayException.class)
    public ModelAndView khongTimThay(KhongTimThayException e) {
        return trangLoi(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Vi phạm quy tắc nghiệp vụ mà Controller chưa tự xử lý (ví dụ hết hàng khi đặt). */
    @ExceptionHandler(NghiepVuException.class)
    public ModelAndView nghiepVu(NghiepVuException e) {
        return trangLoi(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Vi phạm ràng buộc của cơ sở dữ liệu: xóa bản ghi đang được tham chiếu, trùng mã... */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView viPhamRangBuoc(DataIntegrityViolationException e) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", e.getMostSpecificCause().getMessage());
        return trangLoi(HttpStatus.CONFLICT,
                "Không thể thực hiện vì dữ liệu đang được sử dụng ở nơi khác hoặc bị trùng với dữ liệu đã có.");
    }

    /** Xung đột khi nhiều giao dịch cùng sửa một dữ liệu (chờ khóa quá lâu, deadlock...). */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ModelAndView xungDotDongThoi(ConcurrencyFailureException e) {
        log.warn("Xung đột truy cập đồng thời: {}", e.getMessage());
        return trangLoi(HttpStatus.CONFLICT,
                "Dữ liệu vừa được người khác thay đổi hoặc hệ thống đang bận. Vui lòng thử lại.");
    }

    private static ModelAndView trangLoi(HttpStatus trangThai, String thongDiep) {
        ModelAndView trang = new ModelAndView("error");
        trang.setStatus(trangThai);
        trang.addObject("status", trangThai.value());
        trang.addObject("thongDiep", thongDiep);
        return trang;
    }
}
