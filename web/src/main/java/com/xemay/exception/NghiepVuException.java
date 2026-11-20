package com.xemay.exception;

/**
 * Lỗi do vi phạm quy tắc nghiệp vụ (ví dụ: tự khóa tài khoản của chính mình, mật khẩu cũ không đúng).
 * Thông điệp của ngoại lệ này được viết bằng tiếng Việt và HIỂN THỊ ĐƯỢC cho người dùng.
 */
public class NghiepVuException extends RuntimeException {

    public NghiepVuException(String message) {
        super(message);
    }
}
