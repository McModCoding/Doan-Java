package com.xemay.client;

import java.io.Console;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * CHƯƠNG TRÌNH CLIENT QUẢN LÝ KHO XE MÁY - chạy tách biệt với ứng dụng web.
 *
 * <p>Nhân viên kho dùng chương trình này để xem tồn kho, nhập hàng, xuất hàng. Mọi dữ liệu được gửi qua
 * Socket tới server nằm trong ứng dụng web, server đọc/ghi vào cùng cơ sở dữ liệu SQL Server.
 *
 * <pre>
 * Chạy:  java -jar xemay-client.jar [máy chủ] [cổng]
 * Ví dụ: java -jar xemay-client.jar                     (localhost, cổng 9090)
 *        java -jar xemay-client.jar 192.168.1.10 9090   (máy chủ khác trong phòng lab)
 * </pre>
 */
public class KhoClient {

    private static final DecimalFormat DINH_DANG_TIEN =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));

    private final Scanner banPhim;
    private final Console console = System.console();

    private KhoClient() {
        // Đọc bàn phím theo bảng mã của cửa sổ dòng lệnh để gõ được tiếng Việt khi tìm xe
        Charset bangMa = console != null ? console.charset() : Charset.defaultCharset();
        this.banPhim = new Scanner(System.in, bangMa);
    }

    public static void main(String[] args) {
        String mayChu = args.length > 0 ? args[0] : "localhost";
        int cong;
        try {
            cong = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        } catch (NumberFormatException e) {
            System.out.println("Cổng không hợp lệ: " + args[1]);
            return;
        }
        new KhoClient().chay(mayChu, cong);
    }

    private void chay(String mayChu, int cong) {
        System.out.println("==============================================");
        System.out.println("   CHƯƠNG TRÌNH QUẢN LÝ KHO XE MÁY (CLIENT)");
        System.out.println("==============================================");
        System.out.println("Đang kết nối tới " + mayChu + ":" + cong + " ...");

        try (KetNoiKho ketNoi = new KetNoiKho(mayChu, cong)) {
            PhanHoiServer loiChao = ketNoi.docLoiChao();
            if (!loiChao.thanhCong()) {
                System.out.println("Máy chủ từ chối kết nối: " + loiChao.thongBao());
                return;
            }
            System.out.println("Đã kết nối. Máy chủ: " + loiChao.dongDau());

            if (!dangNhap(ketNoi)) {
                return;
            }
            vongLapMenu(ketNoi);
        } catch (UnknownHostException e) {
            System.out.println("Không tìm thấy máy chủ \"" + mayChu + "\".");
        } catch (ConnectException e) {
            System.out.println("Không kết nối được tới " + mayChu + ":" + cong
                    + ". Hãy kiểm tra ứng dụng web đã chạy và cổng Socket đúng chưa.");
        } catch (SocketTimeoutException e) {
            System.out.println("Máy chủ không trả lời (hết thời gian chờ).");
        } catch (EOFException e) {
            System.out.println("Máy chủ đã đóng kết nối.");
        } catch (IOException e) {
            System.out.println("Mất kết nối tới máy chủ: " + e.getMessage());
        } catch (NoSuchElementException e) {
            System.out.println();   // người dùng bấm Ctrl+D / Ctrl+Z: kết thúc chương trình
        }
        System.out.println("Đã đóng kết nối. Tạm biệt!");
    }

    /** @return true nếu đăng nhập thành công */
    private boolean dangNhap(KetNoiKho ketNoi) throws IOException {
        while (true) {
            System.out.println();
            String ten = nhap("Tên đăng nhập: ");
            String matKhau = nhapMatKhau("Mật khẩu: ");
            if (ten.isBlank() || matKhau.isBlank()) {
                System.out.println("Vui lòng nhập đủ tên đăng nhập và mật khẩu.");
                continue;
            }
            PhanHoiServer kq = ketNoi.gui("DANGNHAP " + ten.trim() + " " + matKhau.trim());
            if (kq.thanhCong()) {
                String[] cot = PhanHoiServer.tachCot(kq.dongDau());
                System.out.println("Xin chào " + cot[0] + " (" + (cot.length > 1 ? cot[1] : "") + ")!");
                return true;
            }
            System.out.println("Đăng nhập thất bại: " + kq.thongBao());
            if (kq.maLoi() == 403 || kq.thongBao().contains("ngắt kết nối")) {
                return false;
            }
        }
    }

    private void vongLapMenu(KetNoiKho ketNoi) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("------------------- MENU -------------------");
            System.out.println(" 1. Xem danh sách xe và tồn kho");
            System.out.println(" 2. Tìm xe theo tên");
            System.out.println(" 3. Xem tồn kho của một xe");
            System.out.println(" 4. Nhập kho (cộng thêm xe)");
            System.out.println(" 5. Xuất kho (trừ bớt xe)");
            System.out.println(" 0. Thoát");
            String chon = nhap("Chọn chức năng: ").trim();
            switch (chon) {
                case "1" -> inDanhSach(ketNoi.gui("DSXE"));
                case "2" -> inDanhSach(ketNoi.gui("DSXE " + nhap("Nhập tên xe cần tìm: ").trim()));
                case "3" -> {
                    Long maXe = nhapSo("Mã xe: ");
                    if (maXe != null) {
                        inMotXe(ketNoi.gui("TONKHO " + maXe), null);
                    }
                }
                case "4" -> nhapXuat(ketNoi, "NHAPKHO", "Đã nhập kho.");
                case "5" -> nhapXuat(ketNoi, "XUATKHO", "Đã xuất kho.");
                case "0" -> {
                    ketNoi.gui("THOAT");
                    return;
                }
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void nhapXuat(KetNoiKho ketNoi, String lenh, String thongBaoThanhCong) throws IOException {
        Long maXe = nhapSo("Mã xe: ");
        if (maXe == null) {
            return;
        }
        Long soLuong = nhapSo("Số lượng: ");
        if (soLuong == null) {
            return;
        }
        inMotXe(ketNoi.gui(lenh + " " + maXe + " " + soLuong), thongBaoThanhCong);
    }

    // ------------------------------------------------------------------ Hiển thị

    private static void inDanhSach(PhanHoiServer kq) {
        if (!kq.thanhCong()) {
            inLoi(kq);
            return;
        }
        List<String> cacDong = kq.duLieu();
        if (cacDong.isEmpty()) {
            System.out.println("Không có xe nào.");
            return;
        }
        String mau = "%-6s %-34s %16s %8s%n";
        System.out.printf(mau, "Mã", "Tên xe", "Giá bán (đ)", "Tồn kho");
        System.out.println("-".repeat(67));
        for (String dong : cacDong) {
            String[] cot = PhanHoiServer.tachCot(dong);
            System.out.printf(mau, cot[0], catNgan(cot[1], 34), dinhDangTien(cot[2]), cot[3]);
        }
        System.out.println("(" + cacDong.size() + " xe)");
    }

    private static void inMotXe(PhanHoiServer kq, String tieuDe) {
        if (!kq.thanhCong()) {
            inLoi(kq);
            return;
        }
        String[] cot = PhanHoiServer.tachCot(kq.dongDau());
        if (tieuDe != null) {
            System.out.println(tieuDe);
        }
        System.out.println("  Mã xe   : " + cot[0]);
        System.out.println("  Tên xe  : " + cot[1]);
        System.out.println("  Giá bán : " + dinhDangTien(cot[2]) + " đ");
        System.out.println("  Tồn kho : " + cot[3]);
    }

    private static void inLoi(PhanHoiServer kq) {
        String loai = switch (kq.maLoi()) {
            case 404 -> "Không tìm thấy";
            case 409 -> "Không đủ hàng";
            case 401, 403 -> "Không có quyền";
            case 400 -> "Dữ liệu không hợp lệ";
            default -> "Lỗi " + kq.maLoi();
        };
        System.out.println("[" + loai + "] " + kq.thongBao());
    }

    private static String dinhDangTien(String so) {
        try {
            return DINH_DANG_TIEN.format(new BigDecimal(so));
        } catch (NumberFormatException e) {
            return so;
        }
    }

    private static String catNgan(String chuoi, int doDai) {
        return chuoi.length() <= doDai ? chuoi : chuoi.substring(0, doDai - 1) + "…";
    }

    // ------------------------------------------------------------------ Nhập từ bàn phím

    private String nhap(String loiNhac) {
        System.out.print(loiNhac);
        return banPhim.nextLine();
    }

    /** Nhập mật khẩu không hiện ký tự (nếu chạy trong cửa sổ dòng lệnh thật). */
    private String nhapMatKhau(String loiNhac) {
        if (console != null) {
            char[] kyTu = console.readPassword(loiNhac);
            return kyTu == null ? "" : new String(kyTu);
        }
        return nhap(loiNhac);
    }

    /** @return số nguyên dương người dùng nhập, hoặc null nếu nhập sai */
    private Long nhapSo(String loiNhac) {
        String chuoi = nhap(loiNhac).trim();
        try {
            long so = Long.parseLong(chuoi);
            if (so > 0) {
                return so;
            }
        } catch (NumberFormatException e) {
            // xử lý chung bên dưới
        }
        System.out.println("Vui lòng nhập một số nguyên dương.");
        return null;
    }
}
