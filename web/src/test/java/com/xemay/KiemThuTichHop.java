package com.xemay;

import com.xemay.dto.DangKyForm;
import com.xemay.entity.HangXe;
import com.xemay.entity.TaiKhoan;
import com.xemay.entity.XeMay;
import com.xemay.repository.HangXeRepository;
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

    @Autowired
    protected HangXeRepository hangXeRepository;

    /** Số thứ tự tăng dần, không trùng giữa các bài kiểm thử trong cùng một lần chạy. */
    protected static int soThuTu() {
        return SO_THU_TU.incrementAndGet();
    }

    /** Số điện thoại 10 chữ số không trùng (đầu 07 để khác dữ liệu mẫu). */
    protected static String soDienThoaiMoi() {
        return String.format("07%08d", soThuTu());
    }

    /** Tạo một tài khoản khách hàng mới (mật khẩu "matkhau1") với tên, số điện thoại không trùng bài khác. */
    protected TaiKhoan taoKhachHangMoi() {
        String ten = "kiemthu" + soThuTu() + "_" + (System.nanoTime() % 100_000);
        DangKyForm form = new DangKyForm();
        form.setTenDangNhap(ten);
        form.setMatKhau("matkhau1");
        form.setNhapLaiMatKhau("matkhau1");
        form.setHoTen("Người Kiểm Thử");
        form.setEmail(ten + "@kiemthu.vn");
        form.setSoDienThoai(soDienThoaiMoi());
        return taiKhoanService.dangKy(form);
    }

    /** Hãng xe dùng chung cho các bài kiểm thử (tạo ở lần gọi đầu tiên). */
    protected synchronized HangXe hangKiemThu() {
        return hangXeRepository.findByTenHangContainingIgnoreCase("Hãng kiểm thử").stream().findFirst()
                .orElseGet(() -> hangXeRepository.save(new HangXe("Hãng kiểm thử", "Việt Nam")));
    }

    /** Tạo một xe máy mới (đang bán) với số lượng tồn cho trước. */
    protected XeMay taoXe(int soLuongTon) {
        XeMay xe = new XeMay(hangKiemThu(), "Xe kiểm thử " + soThuTu(), "Tay ga",
                new BigDecimal("30000000"), soLuongTon);
        return xeMayRepository.save(xe);
    }

    protected int tonKho(Integer maXe) {
        return xeMayRepository.findById(maXe).orElseThrow().getSoLuong();
    }
}
