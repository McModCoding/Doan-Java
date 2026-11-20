package com.xemay.network;

import com.xemay.security.TaiKhoanDetails;

/**
 * Trạng thái của MỘT kết nối client: đã đăng nhập chưa, đăng nhập bằng tài khoản nào.
 * Mỗi kết nối có một đối tượng riêng và chỉ do luồng phục vụ kết nối đó sử dụng, nên không cần đồng bộ.
 */
public class PhienKetNoi {

    private final String diaChiIp;
    private TaiKhoanDetails nguoiDung;
    private int soLanDangNhapSai;

    public PhienKetNoi(String diaChiIp) {
        this.diaChiIp = diaChiIp;
    }

    public String getDiaChiIp() {
        return diaChiIp;
    }

    public boolean daDangNhap() {
        return nguoiDung != null;
    }

    public TaiKhoanDetails getNguoiDung() {
        return nguoiDung;
    }

    public void setNguoiDung(TaiKhoanDetails nguoiDung) {
        this.nguoiDung = nguoiDung;
        this.soLanDangNhapSai = 0;
    }

    /** @return số lần đăng nhập sai liên tiếp tính đến lúc này */
    public int themLanDangNhapSai() {
        return ++soLanDangNhapSai;
    }
}
