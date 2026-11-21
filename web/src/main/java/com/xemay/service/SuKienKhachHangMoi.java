package com.xemay.service;

/**
 * Sự kiện "vừa có hồ sơ khách hàng mới" (khách tự đăng ký hoặc nhân viên nhập khách mua tại cửa hàng).
 * {@link GuiEmailService} nghe sự kiện này để gửi email chào mừng trên luồng nền.
 *
 * @param maKhachHang mã hồ sơ vừa tạo
 * @param hoTen       họ tên khách
 * @param email       email nhận thư; {@code null} nếu khách không có email
 */
public record SuKienKhachHangMoi(Integer maKhachHang, String hoTen, String email) {
}
