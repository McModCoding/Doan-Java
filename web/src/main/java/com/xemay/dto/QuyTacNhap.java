package com.xemay.dto;

/** Quy tắc kiểm tra dữ liệu nhập dùng chung cho các form về tài khoản. */
public final class QuyTacNhap {

    private QuyTacNhap() {
    }

    public static final String TEN_DANG_NHAP = "^[a-zA-Z0-9._]{4,30}$";
    public static final String TEN_DANG_NHAP_SAI =
            "Tên đăng nhập dài 4-30 ký tự, chỉ gồm chữ cái không dấu, chữ số, dấu chấm và dấu gạch dưới";

    /** 6-50 ký tự ASCII in được, không có khoảng trắng: tránh lỗi do bộ gõ tiếng Việt tự đổi ký tự khi gõ mật khẩu. */
    public static final String MAT_KHAU = "^[\\x21-\\x7E]{6,50}$";
    public static final String MAT_KHAU_SAI =
            "Mật khẩu dài 6-50 ký tự, không chứa khoảng trắng và chữ có dấu";

    /** Để trống hoặc đúng 10 chữ số bắt đầu bằng 0. */
    public static final String SO_DIEN_THOAI = "^$|^0\\d{9}$";
    public static final String SO_DIEN_THOAI_SAI = "Số điện thoại gồm 10 chữ số và bắt đầu bằng số 0";
}
