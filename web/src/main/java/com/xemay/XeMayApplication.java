package com.xemay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của ứng dụng Web bán xe máy.
 *
 * <p>Cấu trúc package (theo mô hình Controller - Service - Repository):
 * <ul>
 *   <li>{@code controller} - nhận request, kiểm tra dữ liệu nhập, gọi Service, trả về view</li>
 *   <li>{@code service}    - nghiệp vụ, giao dịch ({@code @Transactional}), xử lý đồng thời</li>
 *   <li>{@code repository} - truy vấn cơ sở dữ liệu (Spring Data JPA)</li>
 *   <li>{@code entity}     - ánh xạ bảng</li>
 *   <li>{@code dto}        - dữ liệu của các form</li>
 *   <li>{@code config}     - cấu hình (bảo mật, đa luồng, dữ liệu khởi tạo)</li>
 *   <li>{@code security}   - đăng nhập, phân quyền</li>
 *   <li>{@code network}    - Socket server phục vụ chương trình client quản lý kho</li>
 *   <li>{@code exception}  - ngoại lệ nghiệp vụ và xử lý lỗi chung</li>
 * </ul>
 */
@SpringBootApplication
public class XeMayApplication {

    public static void main(String[] args) {
        SpringApplication.run(XeMayApplication.class, args);
    }
}
