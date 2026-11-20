package com.xemay.security;

import com.xemay.entity.TaiKhoan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống dò mật khẩu: đếm số lần đăng nhập sai liên tiếp của từng tên đăng nhập và TẠM KHÓA
 * khi vượt quá giới hạn (mặc định sai 5 lần thì khóa 15 phút).
 *
 * <p><b>Xử lý đồng thời:</b> {@link #bangDem} là tài nguyên dùng chung trong bộ nhớ. Mỗi request là một
 * luồng của Tomcat, cộng thêm các luồng của Socket server, nên nhiều luồng có thể cùng lúc ghi nhận
 * lần sai của CÙNG một tên đăng nhập. Nếu dùng HashMap thường và viết "đọc số cũ, cộng 1, ghi lại"
 * thì hai luồng có thể cùng đọc được 3 và cùng ghi 4, tức là mất một lần đếm. Ở đây dùng
 * {@link ConcurrentHashMap#compute}: toàn bộ thao tác đọc - tính - ghi cho một khóa là NGUYÊN TỬ.
 */
@Component
public class BoDemDangNhapSai {

    /** Độ dài tối đa của khóa trong bảng đếm, để kẻ tấn công không thể làm đầy bộ nhớ bằng tên rất dài. */
    private static final int DO_DAI_KHOA_TOI_DA = 100;

    /** Trạng thái của một tên đăng nhập. Bất biến: mỗi lần cập nhật sẽ tạo một đối tượng mới. */
    private record TrangThai(int soLanSai, Instant lanSaiGanNhat, Instant khoaDen) {
    }

    private final int soLanSaiToiDa;
    private final Duration thoiGianKhoa;
    private final ConcurrentHashMap<String, TrangThai> bangDem = new ConcurrentHashMap<>();

    @Autowired
    public BoDemDangNhapSai(@Value("${xemay.dang-nhap.so-lan-sai-toi-da:5}") int soLanSaiToiDa,
                            @Value("${xemay.dang-nhap.thoi-gian-khoa-phut:15}") long thoiGianKhoaPhut) {
        this(soLanSaiToiDa, Duration.ofMinutes(thoiGianKhoaPhut));
    }

    public BoDemDangNhapSai(int soLanSaiToiDa, Duration thoiGianKhoa) {
        this.soLanSaiToiDa = soLanSaiToiDa;
        this.thoiGianKhoa = thoiGianKhoa;
    }

    /** Ghi nhận một lần đăng nhập sai. Đủ số lần cho phép thì bắt đầu tạm khóa. */
    public void ghiNhanSai(String tenDangNhap) {
        Instant bayGio = Instant.now();
        bangDem.compute(khoa(tenDangNhap), (k, cu) -> {
            // Chuỗi lần sai trước đã quá cũ thì đếm lại từ đầu
            int soLanSai = (cu == null || daHetHan(cu, bayGio)) ? 1 : cu.soLanSai() + 1;
            Instant khoaDen = soLanSai >= soLanSaiToiDa ? bayGio.plus(thoiGianKhoa) : null;
            return new TrangThai(soLanSai, bayGio, khoaDen);
        });
    }

    /** Xóa bộ đếm của một tên đăng nhập: gọi khi đăng nhập thành công hoặc khi quản trị viên mở khóa. */
    public void xoa(String tenDangNhap) {
        bangDem.remove(khoa(tenDangNhap));
    }

    public boolean dangBiKhoa(String tenDangNhap) {
        TrangThai trangThai = bangDem.get(khoa(tenDangNhap));
        return trangThai != null && trangThai.khoaDen() != null && Instant.now().isBefore(trangThai.khoaDen());
    }

    /** Số lần sai đang được ghi nhận (0 nếu chưa sai lần nào hoặc chuỗi lần sai đã hết hạn). */
    public int soLanSai(String tenDangNhap) {
        TrangThai trangThai = bangDem.get(khoa(tenDangNhap));
        return (trangThai == null || daHetHan(trangThai, Instant.now())) ? 0 : trangThai.soLanSai();
    }

    /**
     * Xóa các mục đã hết hạn để bảng đếm không phình to. Được tác vụ định kỳ gọi mỗi vài phút.
     *
     * @return số mục đã xóa
     */
    public int donDep() {
        Instant bayGio = Instant.now();
        int truocKhiDon = bangDem.size();
        // removeIf của ConcurrentHashMap chỉ xóa mục nếu giá trị chưa bị luồng khác thay đổi
        bangDem.values().removeIf(trangThai -> daHetHan(trangThai, bayGio));
        return Math.max(0, truocKhiDon - bangDem.size());
    }

    public int getSoLanSaiToiDa() {
        return soLanSaiToiDa;
    }

    public long getThoiGianKhoaPhut() {
        return thoiGianKhoa.toMinutes();
    }

    /** Hết hạn khi đã qua thời điểm mở khóa, hoặc (nếu chưa bị khóa) lần sai gần nhất đã quá lâu. */
    private boolean daHetHan(TrangThai trangThai, Instant bayGio) {
        Instant moc = trangThai.khoaDen() != null
                ? trangThai.khoaDen()
                : trangThai.lanSaiGanNhat().plus(thoiGianKhoa);
        return !bayGio.isBefore(moc);
    }

    private static String khoa(String tenDangNhap) {
        String ten = TaiKhoan.chuanHoaTenDangNhap(tenDangNhap);
        return ten.length() > DO_DAI_KHOA_TOI_DA ? ten.substring(0, DO_DAI_KHOA_TOI_DA) : ten;
    }
}
