package com.xemay.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Một câu trả lời của server cho client.
 *
 * @param thanhCong   true: dòng đầu là "OK n"; false: dòng đầu là "ERR mã thông-báo"
 * @param maLoi       mã lỗi khi thất bại (xem {@link GiaoThuc})
 * @param thongBao    thông báo lỗi khi thất bại
 * @param duLieu      các dòng dữ liệu khi thành công
 * @param dongKetNoi  true: sau khi gửi câu trả lời này server sẽ đóng kết nối
 */
public record PhanHoi(boolean thanhCong, int maLoi, String thongBao, List<String> duLieu, boolean dongKetNoi) {

    public static PhanHoi ok(List<String> duLieu) {
        return new PhanHoi(true, 0, null, List.copyOf(duLieu), false);
    }

    public static PhanHoi ok(String... duLieu) {
        return ok(List.of(duLieu));
    }

    public static PhanHoi loi(int maLoi, String thongBao) {
        return new PhanHoi(false, maLoi, thongBao, List.of(), false);
    }

    /** Bản sao của câu trả lời này, kèm yêu cầu đóng kết nối sau khi gửi. */
    public PhanHoi roiDongKetNoi() {
        return new PhanHoi(thanhCong, maLoi, thongBao, duLieu, true);
    }

    /** Chuyển thành các dòng văn bản đúng theo giao thức. */
    public List<String> thanhCacDong() {
        if (!thanhCong) {
            return List.of("ERR " + maLoi + " " + motDong(thongBao));
        }
        List<String> cacDong = new ArrayList<>(duLieu.size() + 1);
        cacDong.add("OK " + duLieu.size());
        duLieu.forEach(dong -> cacDong.add(motDong(dong)));
        return cacDong;
    }

    /** Một thông điệp của giao thức không được chứa ký tự xuống dòng. */
    private static String motDong(String chuoi) {
        return chuoi == null ? "" : chuoi.replace('\r', ' ').replace('\n', ' ');
    }
}
