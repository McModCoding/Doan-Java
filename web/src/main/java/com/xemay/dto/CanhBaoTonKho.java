package com.xemay.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả một lần quét tồn kho của tác vụ định kỳ: danh sách xe sắp hết hàng tại thời điểm quét.
 *
 * <p>Đối tượng này BẤT BIẾN (record, danh sách không sửa được) nên luồng định kỳ có thể công bố nó
 * cho các luồng xử lý request đọc mà không cần khóa.
 *
 * @param thoiDiemQuet thời điểm quét; {@code null} nếu chưa quét lần nào
 * @param nguong       ngưỡng cảnh báo đang áp dụng
 * @param danhSach     các xe có số lượng tồn nhỏ hơn hoặc bằng ngưỡng
 */
public record CanhBaoTonKho(LocalDateTime thoiDiemQuet, int nguong, List<Muc> danhSach) {

    /** Một xe sắp hết hàng: mã xe, tên kèm hãng, số lượng tồn. */
    public record Muc(Integer id, String tenXe, int soLuongTon) {
    }

    public CanhBaoTonKho {
        danhSach = List.copyOf(danhSach);
    }

    public static CanhBaoTonKho chuaQuet(int nguong) {
        return new CanhBaoTonKho(null, nguong, List.of());
    }
}
