package com.xemay.client;

import java.util.List;

/**
 * Câu trả lời của server, đã được đọc và tách theo giao thức.
 *
 * @param thanhCong true nếu dòng đầu là "OK n"
 * @param maLoi     mã lỗi khi thất bại (401 chưa đăng nhập, 403 không có quyền, 404 không tìm thấy,
 *                  409 không đủ hàng, 503 server bận...)
 * @param thongBao  thông báo lỗi khi thất bại
 * @param duLieu    các dòng dữ liệu khi thành công
 */
public record PhanHoiServer(boolean thanhCong, int maLoi, String thongBao, List<String> duLieu) {

    /** Dòng dữ liệu đầu tiên, hoặc chuỗi rỗng nếu không có. */
    public String dongDau() {
        return duLieu.isEmpty() ? "" : duLieu.get(0);
    }

    /** Tách một dòng dữ liệu thành các cột (các cột cách nhau bằng dấu |). */
    public static String[] tachCot(String dong) {
        return dong.split("\\|", -1);
    }
}
