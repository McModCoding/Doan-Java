package com.xemay.security;

import com.xemay.service.LichSuDangNhapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Xử lý chung SAU mỗi lần đăng nhập, dùng cho cả hai kênh: trang web và client Socket.
 * Nhờ đó quy tắc "sai 5 lần thì tạm khóa" và nhật ký đăng nhập áp dụng giống nhau ở mọi nơi.
 */
@Component
public class TheoDoiDangNhap {

    private static final Logger log = LoggerFactory.getLogger(TheoDoiDangNhap.class);

    private final BoDemDangNhapSai boDemDangNhapSai;
    private final LichSuDangNhapService lichSuDangNhapService;

    public TheoDoiDangNhap(BoDemDangNhapSai boDemDangNhapSai, LichSuDangNhapService lichSuDangNhapService) {
        this.boDemDangNhapSai = boDemDangNhapSai;
        this.lichSuDangNhapService = lichSuDangNhapService;
    }

    public void thanhCong(String tenDangNhap, String diaChiIp, String kenh) {
        boDemDangNhapSai.xoa(tenDangNhap);
        log.info("Đăng nhập thành công: '{}' qua {} [luồng xử lý: {}] -> giao việc ghi lịch sử cho luồng nền",
                rutGon(tenDangNhap), kenh, Thread.currentThread().getName());
        // Phương thức @Async: trả về ngay, việc ghi xuống CSDL do luồng nền thực hiện
        lichSuDangNhapService.ghiNhan(tenDangNhap, diaChiIp, kenh, true, null);
    }

    /**
     * Phân loại lý do thất bại, cập nhật bộ đếm lần sai và ghi nhật ký.
     *
     * @return lý do để hiển thị cho người dùng
     */
    public LyDoThatBai thatBai(String tenDangNhap, String diaChiIp, String kenh, AuthenticationException loi) {
        LyDoThatBai lyDo;
        if (loi instanceof AuthenticationServiceException) {
            // Lỗi hệ thống (mất kết nối CSDL...): không tính là nhập sai, cũng không cố ghi nhật ký
            log.error("Lỗi hệ thống khi xác thực '{}': {}", rutGon(tenDangNhap), loi.getMessage());
            return LyDoThatBai.LOI_HE_THONG;
        } else if (loi instanceof DisabledException) {
            lyDo = LyDoThatBai.BI_KHOA;
        } else if (loi instanceof LockedException || boDemDangNhapSai.dangBiKhoa(tenDangNhap)) {
            // Đang trong thời gian tạm khóa: từ chối mà không đếm thêm lần sai
            lyDo = LyDoThatBai.TAM_KHOA;
        } else {
            boDemDangNhapSai.ghiNhanSai(tenDangNhap);
            lyDo = boDemDangNhapSai.dangBiKhoa(tenDangNhap) ? LyDoThatBai.TAM_KHOA : LyDoThatBai.SAI_THONG_TIN;
        }
        log.warn("Đăng nhập thất bại: '{}' qua {} - {}", rutGon(tenDangNhap), kenh, lyDo);
        lichSuDangNhapService.ghiNhan(tenDangNhap, diaChiIp, kenh, false, ghiChu(lyDo));
        return lyDo;
    }

    /**
     * Mật khẩu đúng nhưng vai trò không được phép dùng kênh này (ví dụ khách hàng đăng nhập vào client quản lý kho).
     * Không tính là nhập sai mật khẩu, nhưng vẫn ghi vào nhật ký.
     */
    public void khongDuQuyen(String tenDangNhap, String diaChiIp, String kenh) {
        boDemDangNhapSai.xoa(tenDangNhap);
        log.warn("Từ chối '{}' qua {}: vai trò không được phép", rutGon(tenDangNhap), kenh);
        lichSuDangNhapService.ghiNhan(tenDangNhap, diaChiIp, kenh, false, "Vai trò không được phép dùng kênh " + kenh);
    }

    /** Thông báo tiếng Việt hiển thị cho người dùng. */
    public String thongBao(LyDoThatBai lyDo) {
        return switch (lyDo) {
            case SAI_THONG_TIN -> "Tên đăng nhập hoặc mật khẩu không đúng.";
            case TAM_KHOA -> "Bạn đã nhập sai quá " + boDemDangNhapSai.getSoLanSaiToiDa() + " lần. Tài khoản bị tạm khóa "
                    + boDemDangNhapSai.getThoiGianKhoaPhut() + " phút, vui lòng thử lại sau.";
            case BI_KHOA -> "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
            case LOI_HE_THONG -> "Hệ thống đang gặp sự cố, vui lòng thử lại sau ít phút.";
        };
    }

    private static String ghiChu(LyDoThatBai lyDo) {
        return switch (lyDo) {
            case SAI_THONG_TIN -> "Sai tên đăng nhập hoặc mật khẩu";
            case TAM_KHOA -> "Tạm khóa do nhập sai nhiều lần";
            case BI_KHOA -> "Tài khoản đang bị khóa";
            case LOI_HE_THONG -> "Lỗi hệ thống";
        };
    }

    /** Tên đăng nhập do người dùng gõ: bỏ ký tự xuống dòng và cắt ngắn trước khi đưa vào log. */
    private static String rutGon(String tenDangNhap) {
        if (tenDangNhap == null) {
            return "";
        }
        String sach = tenDangNhap.replaceAll("\\p{Cntrl}", "_");
        return sach.length() > 50 ? sach.substring(0, 50) + "..." : sach;
    }
}
