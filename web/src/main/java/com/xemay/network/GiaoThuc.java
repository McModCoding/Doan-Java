package com.xemay.network;

/**
 * GIAO THỨC giữa chương trình client quản lý kho và Socket server (tầng ứng dụng, chạy trên TCP).
 *
 * <p>Dạng văn bản, mã hóa UTF-8, mỗi thông điệp kết thúc bằng ký tự xuống dòng.
 *
 * <pre>
 * Client gửi 1 dòng:   &lt;LỆNH&gt; [tham số...]
 * Server trả lời:      OK &lt;n&gt;                rồi đúng n dòng dữ liệu, các cột cách nhau bằng dấu |
 *            hoặc:     ERR &lt;mã lỗi&gt; &lt;thông báo&gt;
 *
 * Lệnh                               Dữ liệu trả về khi thành công
 * DANGNHAP &lt;tên&gt; &lt;mật khẩu&gt;          1 dòng: họ tên|vai trò
 * DSXE [từ khóa]                     mỗi xe 1 dòng: mã|tên xe|giá bán|số lượng tồn
 * TONKHO &lt;mã xe&gt;                     1 dòng: mã|tên xe|giá bán|số lượng tồn
 * NHAPKHO &lt;mã xe&gt; &lt;số lượng&gt;         1 dòng: thông tin xe sau khi cộng tồn kho
 * XUATKHO &lt;mã xe&gt; &lt;số lượng&gt;         1 dòng: thông tin xe sau khi trừ tồn kho
 * THOAT                              0 dòng, sau đó server đóng kết nối
 * </pre>
 *
 * <p>Ngay khi kết nối, server gửi trước một lời chào dạng {@code OK 1} + 1 dòng.
 * Mọi lệnh trừ DANGNHAP và THOAT đều yêu cầu đã đăng nhập bằng tài khoản quản trị viên hoặc nhân viên.
 */
public final class GiaoThuc {

    private GiaoThuc() {
    }

    public static final String LOI_CHAO = "XEMAY-KHO/1.0 - Hãy đăng nhập: DANGNHAP <tên đăng nhập> <mật khẩu>";

    /** Ký tự ngăn cách các cột trong một dòng dữ liệu. */
    public static final String NGAN_CACH = "|";

    /** Dòng lệnh dài hơn giới hạn này bị coi là không hợp lệ và server ngắt kết nối. */
    public static final int DO_DAI_DONG_TOI_DA = 512;

    /** Đăng nhập sai quá số lần này trong một kết nối thì server ngắt kết nối. */
    public static final int SO_LAN_DANG_NHAP_SAI_TOI_DA = 3;

    // ---- Tên lệnh ----
    public static final String LENH_DANGNHAP = "DANGNHAP";
    public static final String LENH_DSXE = "DSXE";
    public static final String LENH_TONKHO = "TONKHO";
    public static final String LENH_NHAPKHO = "NHAPKHO";
    public static final String LENH_XUATKHO = "XUATKHO";
    public static final String LENH_THOAT = "THOAT";

    // ---- Mã lỗi (đặt giống mã trạng thái HTTP cho dễ nhớ) ----
    public static final int LOI_CU_PHAP = 400;
    public static final int LOI_CHUA_DANG_NHAP = 401;
    public static final int LOI_KHONG_CO_QUYEN = 403;
    public static final int LOI_KHONG_TIM_THAY = 404;
    public static final int LOI_HET_THOI_GIAN = 408;
    public static final int LOI_KHONG_DU_HANG = 409;
    public static final int LOI_MAY_CHU = 500;
    public static final int LOI_MAY_CHU_BAN = 503;
}
