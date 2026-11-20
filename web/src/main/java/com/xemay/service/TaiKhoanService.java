package com.xemay.service;

import com.xemay.dto.DangKyForm;
import com.xemay.dto.QuyTacNhap;
import com.xemay.dto.TaiKhoanForm;
import com.xemay.dto.ThongTinCaNhanForm;
import com.xemay.entity.TaiKhoan;
import com.xemay.entity.VaiTro;
import com.xemay.exception.DuLieuKhongHopLeException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import com.xemay.repository.LichSuDangNhapRepository;
import com.xemay.repository.TaiKhoanRepository;
import com.xemay.repository.VaiTroRepository;
import com.xemay.security.BoDemDangNhapSai;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Nghiệp vụ về tài khoản: đăng ký, sửa thông tin cá nhân, đổi mật khẩu và các thao tác của quản trị viên.
 *
 * <p>Các phương thức quản trị có {@code @PreAuthorize("hasRole('ADMIN')")}: dù ai đó gọi nhầm từ một
 * Controller không nằm dưới /quan-tri thì người không phải quản trị viên vẫn bị từ chối.
 */
@Service
public class TaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final VaiTroRepository vaiTroRepository;
    private final LichSuDangNhapRepository lichSuDangNhapRepository;
    private final PasswordEncoder passwordEncoder;
    private final BoDemDangNhapSai boDemDangNhapSai;

    public TaiKhoanService(TaiKhoanRepository taiKhoanRepository,
                           VaiTroRepository vaiTroRepository,
                           LichSuDangNhapRepository lichSuDangNhapRepository,
                           PasswordEncoder passwordEncoder,
                           BoDemDangNhapSai boDemDangNhapSai) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.lichSuDangNhapRepository = lichSuDangNhapRepository;
        this.passwordEncoder = passwordEncoder;
        this.boDemDangNhapSai = boDemDangNhapSai;
    }

    // ------------------------------------------------------------------ Tra cứu

    @Transactional(readOnly = true)
    public TaiKhoan timTheoId(Long id) {
        return taiKhoanRepository.findById(id)
                .orElseThrow(() -> new KhongTimThayException("Không tìm thấy tài khoản có mã " + id + "."));
    }

    @Transactional(readOnly = true)
    public TaiKhoan timTheoTenDangNhap(String tenDangNhap) {
        return taiKhoanRepository.findByTenDangNhap(TaiKhoan.chuanHoaTenDangNhap(tenDangNhap))
                .orElseThrow(() -> new KhongTimThayException("Không tìm thấy tài khoản \"" + tenDangNhap + "\"."));
    }

    /** Tìm theo từ khóa (tên đăng nhập, họ tên, email) và lọc theo vai trò; có phân trang và sắp xếp. */
    @Transactional(readOnly = true)
    public Page<TaiKhoan> timKiem(String tuKhoa, Integer vaiTroId, Pageable pageable) {
        String mau = "%" + (tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT)) + "%";
        return taiKhoanRepository.timKiem(mau, vaiTroId, pageable);
    }

    @Transactional(readOnly = true)
    public List<VaiTro> danhSachVaiTro() {
        return vaiTroRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public long demTatCa() {
        return taiKhoanRepository.count();
    }

    @Transactional(readOnly = true)
    public long demTheoVaiTro(String maVaiTro) {
        return taiKhoanRepository.countByVaiTro_Ma(maVaiTro);
    }

    // ------------------------------------------------------------------ Người dùng tự thao tác

    /** Khách tự đăng ký: luôn nhận vai trò KHACHHANG. */
    @Transactional
    public TaiKhoan dangKy(DangKyForm form) {
        return taoMoi(form.getTenDangNhap(), form.getMatKhau(), form.getHoTen(), form.getEmail(),
                form.getSoDienThoai(), timVaiTroTheoMa(VaiTro.KHACHHANG), true);
    }

    @Transactional
    public TaiKhoan capNhatThongTinCaNhan(String tenDangNhap, ThongTinCaNhanForm form) {
        TaiKhoan taiKhoan = timTheoTenDangNhap(tenDangNhap);
        String email = chuanHoaEmail(form.getEmail());
        if (taiKhoanRepository.existsByEmailAndIdNot(email, taiKhoan.getId())) {
            throw new DuLieuKhongHopLeException("email", "Email này đã được tài khoản khác sử dụng");
        }
        taiKhoan.setHoTen(form.getHoTen().trim());
        taiKhoan.setEmail(email);
        taiKhoan.setSoDienThoai(rongThanhNull(form.getSoDienThoai()));
        return luu(taiKhoan);
    }

    @Transactional
    public void doiMatKhau(String tenDangNhap, String matKhauHienTai, String matKhauMoi) {
        TaiKhoan taiKhoan = timTheoTenDangNhap(tenDangNhap);
        if (matKhauHienTai == null || !passwordEncoder.matches(matKhauHienTai, taiKhoan.getMatKhau())) {
            throw new DuLieuKhongHopLeException("matKhauHienTai", "Mật khẩu hiện tại không đúng");
        }
        kiemTraMatKhau(matKhauMoi, "matKhauMoi");
        if (passwordEncoder.matches(matKhauMoi, taiKhoan.getMatKhau())) {
            throw new DuLieuKhongHopLeException("matKhauMoi", "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        taiKhoan.setMatKhau(passwordEncoder.encode(matKhauMoi));
    }

    // ------------------------------------------------------------------ Quản trị viên

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaiKhoan them(TaiKhoanForm form) {
        return taoMoi(form.getTenDangNhap(), form.getMatKhau(), form.getHoTen(), form.getEmail(),
                form.getSoDienThoai(), timVaiTroTheoId(form.getVaiTroId()), form.isKichHoat());
    }

    /**
     * Sửa họ tên, email, số điện thoại, vai trò và trạng thái. Tên đăng nhập và mật khẩu không đổi ở đây.
     *
     * @param nguoiThucHien tên đăng nhập của quản trị viên đang thao tác
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaiKhoan capNhat(Long id, TaiKhoanForm form, String nguoiThucHien) {
        TaiKhoan taiKhoan = timTheoId(id);
        VaiTro vaiTroMoi = timVaiTroTheoId(form.getVaiTroId());

        boolean doiVaiTro = !vaiTroMoi.getId().equals(taiKhoan.getVaiTro().getId());
        if (laChinhMinh(taiKhoan, nguoiThucHien) && (doiVaiTro || !form.isKichHoat())) {
            // Nếu cho phép, quản trị viên duy nhất có thể tự làm mình mất quyền và không ai sửa lại được
            throw new NghiepVuException("Bạn không thể tự khóa hoặc tự đổi vai trò của chính mình.");
        }
        String email = chuanHoaEmail(form.getEmail());
        if (taiKhoanRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuLieuKhongHopLeException("email", "Email này đã được tài khoản khác sử dụng");
        }

        taiKhoan.setHoTen(form.getHoTen().trim());
        taiKhoan.setEmail(email);
        taiKhoan.setSoDienThoai(rongThanhNull(form.getSoDienThoai()));
        taiKhoan.setVaiTro(vaiTroMoi);
        taiKhoan.setKichHoat(form.isKichHoat());
        return luu(taiKhoan);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaiKhoan khoa(Long id, String nguoiThucHien) {
        TaiKhoan taiKhoan = timTheoId(id);
        if (laChinhMinh(taiKhoan, nguoiThucHien)) {
            throw new NghiepVuException("Bạn không thể tự khóa tài khoản của chính mình.");
        }
        taiKhoan.setKichHoat(false);
        return taiKhoan;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaiKhoan moKhoa(Long id) {
        TaiKhoan taiKhoan = timTheoId(id);
        taiKhoan.setKichHoat(true);
        // Bỏ luôn trạng thái tạm khóa do nhập sai mật khẩu, để người dùng đăng nhập lại được ngay
        boDemDangNhapSai.xoa(taiKhoan.getTenDangNhap());
        return taiKhoan;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaiKhoan datLaiMatKhau(Long id, String matKhauMoi) {
        TaiKhoan taiKhoan = timTheoId(id);
        kiemTraMatKhau(matKhauMoi, "matKhauMoi");
        taiKhoan.setMatKhau(passwordEncoder.encode(matKhauMoi));
        boDemDangNhapSai.xoa(taiKhoan.getTenDangNhap());
        return taiKhoan;
    }

    /**
     * Xóa tài khoản. Nhật ký đăng nhập của tài khoản được giữ lại (chỉ gỡ liên kết).
     * Nếu tài khoản đang được bảng khác tham chiếu (đơn hàng, hồ sơ khách hàng...) thì KHÔNG xóa được.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void xoa(Long id, String nguoiThucHien) {
        TaiKhoan taiKhoan = timTheoId(id);
        if (laChinhMinh(taiKhoan, nguoiThucHien)) {
            throw new NghiepVuException("Bạn không thể tự xóa tài khoản của chính mình.");
        }
        String tenDangNhap = taiKhoan.getTenDangNhap();
        lichSuDangNhapRepository.boLienKetTaiKhoan(id);
        try {
            taiKhoanRepository.delete(taiKhoan);
            taiKhoanRepository.flush();   // chạy lệnh DELETE ngay để bắt được lỗi khóa ngoại tại đây
        } catch (DataIntegrityViolationException e) {
            // Ném ngoại lệ -> cả giao dịch bị hủy, kể cả bước gỡ liên kết nhật ký ở trên
            throw new NghiepVuException("Không thể xóa tài khoản \"" + tenDangNhap + "\" vì đang có dữ liệu liên quan "
                    + "(đơn hàng, hồ sơ khách hàng...). Hãy khóa tài khoản thay cho việc xóa.");
        }
        boDemDangNhapSai.xoa(tenDangNhap);
    }

    // ------------------------------------------------------------------ Hàm dùng chung

    private TaiKhoan taoMoi(String tenDangNhap, String matKhau, String hoTen, String email,
                            String soDienThoai, VaiTro vaiTro, boolean kichHoat) {
        String ten = TaiKhoan.chuanHoaTenDangNhap(tenDangNhap);
        String emailChuan = chuanHoaEmail(email);
        if (!ten.matches(QuyTacNhap.TEN_DANG_NHAP)) {
            throw new DuLieuKhongHopLeException("tenDangNhap", QuyTacNhap.TEN_DANG_NHAP_SAI);
        }
        kiemTraMatKhau(matKhau, "matKhau");
        if (taiKhoanRepository.existsByTenDangNhap(ten)) {
            throw new DuLieuKhongHopLeException("tenDangNhap", "Tên đăng nhập này đã có người sử dụng");
        }
        if (taiKhoanRepository.existsByEmail(emailChuan)) {
            throw new DuLieuKhongHopLeException("email", "Email này đã được đăng ký cho tài khoản khác");
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(ten);
        taiKhoan.setMatKhau(passwordEncoder.encode(matKhau));
        taiKhoan.setHoTen(hoTen.trim());
        taiKhoan.setEmail(emailChuan);
        taiKhoan.setSoDienThoai(rongThanhNull(soDienThoai));
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setKichHoat(kichHoat);
        return luu(taiKhoan);
    }

    /**
     * Lưu và đẩy ngay xuống CSDL để bắt lỗi trùng dữ liệu.
     *
     * <p><b>Xử lý đồng thời:</b> hai người có thể đăng ký CÙNG một tên đăng nhập ở cùng một thời điểm.
     * Khi đó cả hai giao dịch đều vượt qua bước kiểm tra {@code existsBy...} (vì chưa ai ghi), nên bước
     * kiểm tra đó không đủ. Ràng buộc UNIQUE của cơ sở dữ liệu là chốt chặn cuối cùng: chỉ một giao dịch
     * ghi được, giao dịch còn lại nhận lỗi và được báo lại cho người dùng một cách thân thiện.
     */
    private TaiKhoan luu(TaiKhoan taiKhoan) {
        try {
            return taiKhoanRepository.saveAndFlush(taiKhoan);
        } catch (DataIntegrityViolationException e) {
            throw new DuLieuKhongHopLeException("tenDangNhap",
                    "Tên đăng nhập hoặc email vừa được người khác sử dụng. Vui lòng thử lại.");
        }
    }

    private void kiemTraMatKhau(String matKhau, String truong) {
        if (matKhau == null || !matKhau.matches(QuyTacNhap.MAT_KHAU)) {
            throw new DuLieuKhongHopLeException(truong, QuyTacNhap.MAT_KHAU_SAI);
        }
    }

    private VaiTro timVaiTroTheoId(Integer vaiTroId) {
        if (vaiTroId == null) {
            throw new DuLieuKhongHopLeException("vaiTroId", "Vui lòng chọn vai trò");
        }
        return vaiTroRepository.findById(vaiTroId)
                .orElseThrow(() -> new DuLieuKhongHopLeException("vaiTroId", "Vai trò không tồn tại"));
    }

    private VaiTro timVaiTroTheoMa(String ma) {
        return vaiTroRepository.findByMa(ma)
                .orElseThrow(() -> new IllegalStateException("Bảng vai_tro chưa có vai trò " + ma));
    }

    private static boolean laChinhMinh(TaiKhoan taiKhoan, String nguoiThucHien) {
        return taiKhoan.getTenDangNhap().equals(TaiKhoan.chuanHoaTenDangNhap(nguoiThucHien));
    }

    private static String chuanHoaEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String rongThanhNull(String chuoi) {
        return (chuoi == null || chuoi.isBlank()) ? null : chuoi.trim();
    }
}
