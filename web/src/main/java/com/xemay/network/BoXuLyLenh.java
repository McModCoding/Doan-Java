package com.xemay.network;

import com.xemay.entity.LichSuDangNhap;
import com.xemay.entity.XeMay;
import com.xemay.exception.HetHangException;
import com.xemay.exception.KhongTimThayException;
import com.xemay.exception.NghiepVuException;
import com.xemay.security.LyDoThatBai;
import com.xemay.security.TaiKhoanDetails;
import com.xemay.security.TheoDoiDangNhap;
import com.xemay.service.TonKhoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static com.xemay.network.GiaoThuc.*;

/**
 * Hiểu và thực hiện MỘT dòng lệnh của client, trả về câu trả lời theo giao thức.
 *
 * <p>Lớp này không biết gì về Socket: nó nhận một chuỗi và trả về {@link PhanHoi}. Nhờ tách riêng như vậy,
 * phần nghiệp vụ được dùng lại y nguyên các Service của ứng dụng web (đăng nhập qua cùng
 * {@link AuthenticationManager}, nhập/xuất kho qua cùng {@link TonKhoService}), và kiểm thử được dễ dàng.
 *
 * <p>Một đối tượng duy nhất được dùng chung cho mọi luồng phục vụ client. Lớp KHÔNG có trường nào thay đổi
 * được; trạng thái riêng của từng kết nối nằm trong {@link PhienKetNoi} do mỗi luồng tự giữ.
 */
@Component
public class BoXuLyLenh {

    private static final Logger log = LoggerFactory.getLogger(BoXuLyLenh.class);

    private static final String CAC_LENH = "DANGNHAP, DSXE, TONKHO, NHAPKHO, XUATKHO, THOAT";

    private final AuthenticationManager authenticationManager;
    private final TheoDoiDangNhap theoDoiDangNhap;
    private final TonKhoService tonKhoService;

    public BoXuLyLenh(AuthenticationManager authenticationManager,
                      TheoDoiDangNhap theoDoiDangNhap,
                      TonKhoService tonKhoService) {
        this.authenticationManager = authenticationManager;
        this.theoDoiDangNhap = theoDoiDangNhap;
        this.tonKhoService = tonKhoService;
    }

    public PhanHoi xuLy(String dongLenh, PhienKetNoi phien) {
        String noiDung = dongLenh == null ? "" : dongLenh.strip();
        if (noiDung.isEmpty()) {
            return PhanHoi.loi(LOI_CU_PHAP, "Lệnh rỗng. Các lệnh hỗ trợ: " + CAC_LENH);
        }
        String[] phan = noiDung.split("\\s+", 2);
        String lenh = phan[0].toUpperCase(Locale.ROOT);
        String thamSo = phan.length > 1 ? phan[1].strip() : "";

        // Không ghi tham số vào log: lệnh DANGNHAP có chứa mật khẩu
        log.info("Nhận lệnh {} từ {} [luồng: {}]", lenh,
                phien.daDangNhap() ? phien.getNguoiDung().getUsername() : phien.getDiaChiIp(),
                Thread.currentThread().getName());
        try {
            return switch (lenh) {
                case LENH_DANGNHAP -> dangNhap(thamSo, phien);
                case LENH_THOAT -> PhanHoi.ok(List.of()).roiDongKetNoi();
                case LENH_DSXE -> chiNhanSu(phien, () -> danhSachXe(thamSo));
                case LENH_TONKHO -> chiNhanSu(phien, () -> tonKho(thamSo));
                case LENH_NHAPKHO -> chiNhanSu(phien, () -> nhapKho(thamSo));
                case LENH_XUATKHO -> chiNhanSu(phien, () -> xuatKho(thamSo));
                default -> PhanHoi.loi(LOI_CU_PHAP, "Lệnh không hợp lệ: " + lenh + ". Các lệnh hỗ trợ: " + CAC_LENH);
            };
        } catch (HetHangException e) {
            return PhanHoi.loi(LOI_KHONG_DU_HANG, e.getMessage());
        } catch (KhongTimThayException e) {
            return PhanHoi.loi(LOI_KHONG_TIM_THAY, e.getMessage());
        } catch (NghiepVuException e) {
            return PhanHoi.loi(LOI_CU_PHAP, e.getMessage());
        } catch (RuntimeException e) {
            // Lỗi bất ngờ (mất kết nối CSDL...): báo lỗi chung cho client, chi tiết chỉ ghi vào log của server
            log.error("Lỗi khi xử lý lệnh {} [luồng: {}]", lenh, Thread.currentThread().getName(), e);
            return PhanHoi.loi(LOI_MAY_CHU, "Lỗi máy chủ, vui lòng thử lại sau.");
        }
    }

    // ------------------------------------------------------------------ Các lệnh

    private PhanHoi dangNhap(String thamSo, PhienKetNoi phien) {
        String[] phan = thamSo.isEmpty() ? new String[0] : thamSo.split("\\s+");
        if (phan.length != 2) {
            return PhanHoi.loi(LOI_CU_PHAP, "Cú pháp: DANGNHAP <tên đăng nhập> <mật khẩu>");
        }
        String ten = phan[0];
        String ip = phien.getDiaChiIp();

        Authentication ketQua;
        try {
            // Dùng CHUNG bộ xác thực với trang web: cùng bảng tai_khoan, cùng BCrypt, cùng quy tắc khóa
            ketQua = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(ten, phan[1]));
        } catch (AuthenticationException e) {
            LyDoThatBai lyDo = theoDoiDangNhap.thatBai(ten, ip, LichSuDangNhap.KENH_SOCKET, e);
            int maLoi = lyDo == LyDoThatBai.LOI_HE_THONG ? LOI_MAY_CHU : LOI_CHUA_DANG_NHAP;
            String thongBao = theoDoiDangNhap.thongBao(lyDo);
            if (phien.themLanDangNhapSai() >= SO_LAN_DANG_NHAP_SAI_TOI_DA) {
                return PhanHoi.loi(maLoi, thongBao + " Bạn đã đăng nhập sai " + SO_LAN_DANG_NHAP_SAI_TOI_DA
                        + " lần, máy chủ ngắt kết nối.").roiDongKetNoi();
            }
            return PhanHoi.loi(maLoi, thongBao);
        }

        TaiKhoanDetails nguoiDung = (TaiKhoanDetails) ketQua.getPrincipal();
        if (!nguoiDung.laNhanSu()) {
            theoDoiDangNhap.khongDuQuyen(ten, ip, LichSuDangNhap.KENH_SOCKET);
            return PhanHoi.loi(LOI_KHONG_CO_QUYEN, "Chỉ quản trị viên và nhân viên được dùng chương trình quản lý kho.");
        }
        theoDoiDangNhap.thanhCong(nguoiDung.getUsername(), ip, LichSuDangNhap.KENH_SOCKET);
        phien.setNguoiDung(nguoiDung);
        return PhanHoi.ok(cot(nguoiDung.getHoTen()) + NGAN_CACH + nguoiDung.getMaVaiTro());
    }

    private PhanHoi danhSachXe(String tuKhoa) {
        List<String> cacDong = tonKhoService.timXeTheoTen(tuKhoa).stream().map(BoXuLyLenh::dongXe).toList();
        return PhanHoi.ok(cacDong);
    }

    private PhanHoi tonKho(String thamSo) {
        long[] so = docSo(thamSo, 1, "TONKHO <mã xe>");
        return PhanHoi.ok(dongXe(tonKhoService.timXe(so[0])));
    }

    private PhanHoi nhapKho(String thamSo) {
        long[] so = docSo(thamSo, 2, "NHAPKHO <mã xe> <số lượng>");
        int soLuongTon = tonKhoService.nhapKho(so[0], soLuong(so[1]));
        return PhanHoi.ok(dongXe(tonKhoService.timXe(so[0]), soLuongTon));
    }

    private PhanHoi xuatKho(String thamSo) {
        long[] so = docSo(thamSo, 2, "XUATKHO <mã xe> <số lượng>");
        int soLuongTon = tonKhoService.xuatKho(so[0], soLuong(so[1]));
        return PhanHoi.ok(dongXe(tonKhoService.timXe(so[0]), soLuongTon));
    }

    // ------------------------------------------------------------------ Hàm phụ

    /** Chỉ thực hiện lệnh khi kết nối đã đăng nhập bằng tài khoản quản trị viên hoặc nhân viên. */
    private static PhanHoi chiNhanSu(PhienKetNoi phien, Supplier<PhanHoi> lenh) {
        if (!phien.daDangNhap()) {
            return PhanHoi.loi(LOI_CHUA_DANG_NHAP, "Bạn cần đăng nhập trước: DANGNHAP <tên đăng nhập> <mật khẩu>");
        }
        if (!phien.getNguoiDung().laNhanSu()) {
            return PhanHoi.loi(LOI_KHONG_CO_QUYEN, "Tài khoản không có quyền quản lý kho.");
        }
        return lenh.get();
    }

    /** Đọc đúng {@code soLuongThamSo} số nguyên từ phần tham số của lệnh. */
    private static long[] docSo(String thamSo, int soLuongThamSo, String cuPhap) {
        String[] phan = thamSo.isEmpty() ? new String[0] : thamSo.split("\\s+");
        if (phan.length != soLuongThamSo) {
            throw new NghiepVuException("Cú pháp: " + cuPhap);
        }
        long[] ketQua = new long[soLuongThamSo];
        try {
            for (int i = 0; i < soLuongThamSo; i++) {
                ketQua[i] = Long.parseLong(phan[i]);
            }
        } catch (NumberFormatException e) {
            throw new NghiepVuException("Tham số phải là số nguyên. Cú pháp: " + cuPhap);
        }
        return ketQua;
    }

    private static int soLuong(long giaTri) {
        if (giaTri < 1 || giaTri > TonKhoService.SO_LUONG_TOI_DA_MOT_LAN) {
            throw new NghiepVuException("Số lượng phải từ 1 đến " + TonKhoService.SO_LUONG_TOI_DA_MOT_LAN + ".");
        }
        return (int) giaTri;
    }

    private static String dongXe(XeMay xe) {
        return dongXe(xe, xe.getSoLuongTon());
    }

    /** Một dòng dữ liệu xe: mã|tên xe|giá bán|số lượng tồn. */
    private static String dongXe(XeMay xe, int soLuongTon) {
        String gia = xe.getGiaBan() == null ? "0" : xe.getGiaBan().toPlainString();
        return xe.getId() + NGAN_CACH + cot(xe.getTenXe()) + NGAN_CACH + gia + NGAN_CACH + soLuongTon;
    }

    /** Dữ liệu trong một cột không được chứa ký tự ngăn cách cột. */
    private static String cot(String giaTri) {
        return giaTri == null ? "" : giaTri.replace(NGAN_CACH, "/");
    }
}
