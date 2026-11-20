package com.xemay.exception;

/** Không tìm thấy dữ liệu theo mã được yêu cầu - hiển thị trang lỗi 404. */
public class KhongTimThayException extends RuntimeException {

    public KhongTimThayException(String message) {
        super(message);
    }
}
