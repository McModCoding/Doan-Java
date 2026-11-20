package com.xemay.security;

/** Lý do một lần đăng nhập thất bại. {@link #getMa()} được đưa lên URL: /dang-nhap?loi=... */
public enum LyDoThatBai {

    /** Sai tên đăng nhập hoặc mật khẩu (không nói rõ sai cái nào để tránh lộ tài khoản nào tồn tại). */
    SAI_THONG_TIN("sai"),
    /** Tạm khóa do nhập sai quá nhiều lần. */
    TAM_KHOA("tam-khoa"),
    /** Tài khoản bị quản trị viên khóa. */
    BI_KHOA("bi-khoa"),
    /** Lỗi phía máy chủ (ví dụ mất kết nối cơ sở dữ liệu) - không phải lỗi của người dùng. */
    LOI_HE_THONG("he-thong");

    private final String ma;

    LyDoThatBai(String ma) {
        this.ma = ma;
    }

    public String getMa() {
        return ma;
    }

    /** Tìm lý do theo mã trên URL; mã lạ thì coi như sai thông tin đăng nhập. */
    public static LyDoThatBai tuMa(String ma) {
        for (LyDoThatBai lyDo : values()) {
            if (lyDo.ma.equals(ma)) {
                return lyDo;
            }
        }
        return SAI_THONG_TIN;
    }
}
