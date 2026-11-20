package com.xemay.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

/** Hàm tiện ích cho các trang danh sách có phân trang và sắp xếp. */
public final class PhanTrangUtils {

    private PhanTrangUtils() {
    }

    /**
     * Chỉ cho phép sắp xếp theo các cột đã khai báo.
     *
     * <p>Tham số {@code ?sort=...} do người dùng gửi lên; nếu ai đó sửa URL thành một cột không tồn tại thì truy vấn
     * sẽ lỗi và người dùng thấy trang lỗi 500. Hàm này loại bỏ các cột lạ; nếu không còn cột nào thì dùng
     * cách sắp xếp mặc định. Cuối cùng luôn sắp thêm theo {@code id} để thứ tự giữa các trang ổn định.
     *
     * @param pageable   thông tin phân trang Spring đọc từ URL (?page=&size=&sort=)
     * @param macDinh    cách sắp xếp khi người dùng không chọn (hoặc chọn sai)
     * @param cotChoPhep tên các thuộc tính của Entity được phép sắp xếp
     */
    public static Pageable chiSapXepTheo(Pageable pageable, Sort macDinh, Set<String> cotChoPhep) {
        List<Sort.Order> hopLe = pageable.getSort().stream()
                .filter(thuTu -> cotChoPhep.contains(thuTu.getProperty()))
                .toList();
        Sort sapXep = hopLe.isEmpty() ? macDinh : Sort.by(hopLe);
        if (sapXep.getOrderFor("id") == null) {
            sapXep = sapXep.and(Sort.by("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sapXep);
    }
}
