package com.xemay;

import com.xemay.dto.DangKyForm;
import com.xemay.entity.TaiKhoan;
import com.xemay.entity.XeMay;
import com.xemay.repository.XeMayRepository;
import com.xemay.service.TaiKhoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp cha của mọi bài kiểm thử tích hợp: khởi động toàn bộ ứng dụng (H2 trong bộ nhớ, Socket server ở cổng ngẫu nhiên).
 *
 * <p>Mọi lớp kiểm thử đều kế thừa lớp này để dùng CHUNG một Spring context (khởi động một lần, chạy nhanh hơn).
 * Vì dùng chung cơ sở dữ liệu, mỗi bài kiểm thử tự tạo dữ liệu riêng (tài khoản, xe) bằng các hàm bên dưới
 * thay vì sửa dữ liệu mặc định.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class KiemThuTichHop {

    private static final AtomicInteger SO_THU_TU = new AtomicInteger();

    @Autowired
    protected TaiKhoanService taiKhoanService;

    @Autowired
    protected XeMayRepository xeMayRepository;

    /** Tạo một tài khoản khách hàng mới (mật khẩu "matkhau1") với tên không trùng các bài kiểm thử khác. */
    protected TaiKhoan taoKhachHangMoi() {
        String ten = "kiemthu" + SO_THU_TU.incrementAndGet() + "_" + (System.nanoTime() % 100_000);
        DangKyForm form = new DangKyForm();
        form.setTenDangNhap(ten);
        form.setMatKhau("matkhau1");
        form.setNhapLaiMatKhau("matkhau1");
        form.setHoTen("Người Kiểm Thử");
        form.setEmail(ten + "@kiemthu.vn");
        return taiKhoanService.dangKy(form);
    }

    /** Tạo một xe máy mới với số lượng tồn cho trước. */
    protected XeMay taoXe(int soLuongTon) {
        return xeMayRepository.save(new XeMay("Xe kiểm thử " + SO_THU_TU.incrementAndGet(),
                new BigDecimal("30000000"), soLuongTon));
    }

    protected int tonKho(Long xeId) {
        return xeMayRepository.findById(xeId).orElseThrow().getSoLuongTon();
    }
}
