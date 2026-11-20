package com.xemay.exception;

/**
 * Dữ liệu của MỘT ô nhập vi phạm quy tắc nghiệp vụ mà chỉ Service mới kiểm tra được
 * (tên đăng nhập đã tồn tại, email bị trùng, mật khẩu hiện tại không đúng...).
 *
 * <p>{@link #getTruong()} là tên thuộc tính của form, để Controller hiển thị lỗi ngay cạnh ô đó:
 * <pre>
 * } catch (DuLieuKhongHopLeException e) {
 *     ketQua.rejectValue(e.getTruong(), "nghiepVu", e.getMessage());
 *     return "ten-view-cua-form";
 * }
 * </pre>
 */
public class DuLieuKhongHopLeException extends NghiepVuException {

    private final String truong;

    public DuLieuKhongHopLeException(String truong, String message) {
        super(message);
        this.truong = truong;
    }

    public String getTruong() {
        return truong;
    }
}
