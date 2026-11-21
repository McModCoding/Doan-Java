# Kiểm thử - bản đã tích hợp

Tài liệu dùng cho **Chương 4** của báo cáo. Mọi kết quả bên dưới là kết quả chạy thật, không phải kết quả dự kiến.
Bản này gồm phần đăng nhập, phân quyền, đồng thời, mạng và phần xe máy, hãng xe, khách hàng đã ghép (20-21/11/2026).

## 1. Môi trường kiểm thử

| Thành phần | Phiên bản |
|---|---|
| JDK | OpenJDK 25 (mã nguồn biên dịch ở mức Java 17); bản trước tích hợp đã chạy thêm trên OpenJDK 21 |
| Cơ sở dữ liệu | SQL Server 2025 (phiên bản 17.0) với script `QL_CUAHANG_XEMAY.sql`; H2 2.3 trong bộ nhớ cho kiểm thử nhanh |
| Kiểm thử tự động | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |
| Kiểm thử giao diện | Trình duyệt Chromium, khung hình máy tính 1440×900 và điện thoại iPhone 13 (390×844) |

## 2. Kiểm thử tự động (65 bài)

Chạy tại thư mục gốc: `./mvnw -pl web test`. Cách chạy trên SQL Server: xem README, bước 5.

| Lớp kiểm thử | Số bài | Nội dung |
|---|---|---|
| `DangNhapPhanQuyenTest` > Đăng nhập | 7 | Đăng nhập đúng theo từng vai trò; không phân biệt hoa thường; sai mật khẩu; sai 5 lần bị tạm khóa rồi được quản trị viên mở khóa; tài khoản bị khóa; nhật ký ghi bằng luồng nền |
| `DangNhapPhanQuyenTest` > Đăng ký | 3 | Dữ liệu sai báo lỗi đúng ô; đăng ký hợp lệ, mật khẩu được băm BCrypt; trùng tên đăng nhập |
| `DangNhapPhanQuyenTest` > Phân quyền theo URL | 6 | Khách chưa đăng nhập, khách hàng, nhân viên, quản trị viên; thiếu CSRF token; CSS, font, ảnh vẫn tải được khi chưa đăng nhập |
| `DangNhapPhanQuyenTest` > Menu theo vai trò | 3 | Mỗi vai trò chỉ thấy menu được phép |
| `DangNhapPhanQuyenTest` > Phân quyền theo phương thức | 3 | `@PreAuthorize` chặn nhân viên; quản trị viên không tự khóa/tự xóa; xóa tài khoản vẫn giữ nhật ký và hồ sơ khách hàng |
| `QuanLyXeMayTest` | 10 | Phân quyền trang xe máy; lọc sai kiểu dữ liệu; thêm xe, trùng tên; **sửa xe không ghi đè tồn kho** (tuần tự và đồng thời); ngừng kinh doanh chỉ quản trị viên; nhập kho trên trang; hiển thị trang, mã không tồn tại -> 404 |
| `QuanLyHangXeKhachHangTest` > Hãng xe | 5 | Trùng tên hãng; không ghi đè hãng khác qua form; ngừng hoạt động (403 với nhân viên, chặn khi còn xe); nhân viên không đổi được trạng thái; số mẫu xe mỗi hãng |
| `QuanLyHangXeKhachHangTest` > Khách hàng | 9 | Đăng ký tạo hồ sơ; trùng số điện thoại thì hủy cả tài khoản; email chào mừng chạy trên luồng nền; thêm, tìm khách tại cửa hàng; dữ liệu sai; đồng bộ họ tên, số điện thoại hai chiều; xóa khách; phân quyền |
| `TonKhoDongThoiTest` | 7 | Đồng thời trên tồn kho (mục 4) |
| `KhoSocketServerTest` | 7 | Socket server, giao thức, nhiều client (mục 5) |
| `BoDemDangNhapSaiTest` | 5 | Bộ đếm đăng nhập sai, gồm 50 luồng ghi đồng thời |

| Lần chạy | Kết quả | Thời gian |
|---|---|---|
| Trên H2 trong bộ nhớ (JDK 25) | **65/65 đạt** | 24 giây |
| Trên SQL Server 2025 (JDK 25), CSDL kiểm thử riêng | **65/65 đạt** | 19 giây |
| Bản trước tích hợp (41 bài) trên SQL Server 2022 và 2025, JDK 21 và 25 | 41/41 đạt ở cả 4 lần | 14-16 giây |

**Bài kiểm thử có kiểm chứng ngược:** tạm bỏ `@DynamicUpdate` trên `XeMay` thì bài `suaXeTrongLucCoNguoiMua` báo sai
(mong đợi tồn kho 49, thực tế 50); đặt lại thì đạt. Tức là bài kiểm thử thật sự bắt được lỗi mất cập nhật (mục 4).

## 3. Kịch bản kiểm thử chức năng (thực hiện trên giao diện)

Chạy với ứng dụng kết nối SQL Server 2025, dữ liệu từ script `QL_CUAHANG_XEMAY.sql`, mật khẩu mọi tài khoản mẫu `123456`.

| Mã | Kịch bản | Dữ liệu / thao tác | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|
| DN-01 | Đăng nhập quản trị viên | `admin` | Vào trang `/quan-tri`, thấy đủ 6 mục quản trị | Đạt |
| DN-02 | Đăng nhập khách hàng | `khach1` | Về trang chủ, menu không có mục Quản trị | Đạt |
| DN-03 | Sai mật khẩu | `admin` / `sai` | Ở lại trang đăng nhập, báo "Tên đăng nhập hoặc mật khẩu không đúng." | Đạt (kiểm thử tự động) |
| DN-04 | Gõ tên đăng nhập chữ hoa | `KHACH1` | Đăng nhập được như `khach1` | Đạt (kiểm thử tự động) |
| DN-05 | Sai 5 lần liên tiếp | sai 5 lần, lần 6 nhập đúng | Từ lần 5 báo tạm khóa 15 phút; lần 6 vẫn bị từ chối | Đạt (kiểm thử tự động) |
| DN-06 | Tài khoản bị quản trị viên khóa | `khach16` | Báo "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên." | Đạt |
| DK-01 | Đăng ký dữ liệu sai | tên `ab`, mật khẩu `123`, nhập lại `456`, email sai, SĐT `123` | Mỗi ô báo lỗi riêng ngay bên dưới | Đạt (kiểm thử tự động) |
| DK-02 | Đăng ký hợp lệ | `khachmoi01`, SĐT `0977000001` | Chuyển về đăng nhập; có dòng `TAI_KHOAN` và dòng `KHACH_HANG` (mã 24) cùng họ tên, SĐT; log có "Đã gửi email chào mừng ... [luồng: xemay-nen-1]" | Đạt |
| DK-03 | Đăng ký trùng số điện thoại của khách đã có | SĐT `0900000001` | Báo "Số điện thoại này đã thuộc về một khách hàng khác" dưới ô SĐT; không có tài khoản nào được tạo | Đạt |
| PQ-01 | Chưa đăng nhập vào `/quan-tri` | | Chuyển tới trang đăng nhập | Đạt (kiểm thử tự động) |
| PQ-02 | Khách hàng vào `/quan-tri/xe-may` | `khach1` | Trang lỗi 403 thân thiện | Đạt |
| PQ-03 | Nhân viên vào `/quan-tri/tai-khoan` | `nhanvien1` | Trang lỗi 403 | Đạt |
| PQ-04 | Nhân viên xem danh sách xe | `nhanvien1` | Thấy nút sửa; **không** thấy nút ngừng kinh doanh (quản trị viên thấy 10 nút ở trang 1) | Đạt |
| XM-01 | Thêm xe | Honda, `SH 160i`, Tay ga, 156 cc, 102.000.000 đ, 4 xe | Báo "Đã thêm xe "SH 160i"."; có trong danh sách | Đạt |
| XM-02 | Sửa giá xe, tồn kho không đổi | Ducati Monster: giá 399.000.000 đ | Giá đổi, tồn kho giữ nguyên; câu lệnh chỉ là `update XE_MAY set GiaBan=? where MaXe=?` | Đạt |
| XM-03 | Nhập kho trên trang sửa xe | Ducati Monster, nhập thêm 3 | Báo "Đã nhập thêm 3 xe. Tồn kho hiện tại: 5." | Đạt |
| XM-04 | Lọc, sắp xếp, phân trang | hãng Honda + đang bán; sắp theo giá giảm dần | Chỉ còn xe Honda đang bán; thứ tự giá đúng; chuyển trang giữ điều kiện | Đạt |
| XM-05 | Trang tổng quan | `admin` mở `/quan-tri` | 20 mẫu xe đang bán, 23 khách hàng; cảnh báo "Ducati Monster", "Triumph Trident 660" sắp hết | Đạt |
| TK-01 | Xóa tài khoản khách đã có đơn hàng | xóa `khach2` | Tài khoản mất; hồ sơ khách hàng (mã 2) và nhật ký đăng nhập vẫn còn, `MaTaiKhoan = NULL` | Đạt |
| TK-02 | Quản trị viên tự khóa mình | bấm Khóa ở dòng `admin` | Bị chặn (giao diện cũng ẩn nút) | Đạt (kiểm thử tự động) |
| KH-01 | Xóa khách đã có giỏ hàng, đơn hàng | khách mã 2 | Báo "Không thể xóa khách hàng "Trần Thị Bích" vì đã có giỏ hàng hoặc đơn hàng." | Đạt |
| KH-02 | Xóa khách mua tại cửa hàng chưa có đơn | khách mã 23 (`0913000003`) | Có hộp xác nhận; báo "Đã xóa khách hàng." | Đạt |
| KH-03 | Tìm khách | từ khóa `0913` | Chỉ còn 3 khách mua tại cửa hàng | Đạt |
| GD-01 | Hiển thị trên điện thoại | iPhone 13, trang xe máy | Bộ lọc 5 ô xếp dọc; bảng vừa màn hình, không cuộn ngang | Đạt |
| GD-02 | Kiểm tra độ tương phản màu chữ và bố cục | Quét tự động 9 trang quản trị (tổng quan, xe máy, hãng xe, khách hàng: danh sách, thêm, sửa) ở 1280 px và 390 px | Không có lỗi | Đạt (đã sửa 1 lỗi "thẻ lồng trong thẻ" ở lần quét đầu) |

## 4. Kiểm thử đồng thời

Mỗi bài cho nhiều luồng **xuất phát cùng một lúc** (dùng `CountDownLatch`) rồi kiểm tra kết quả cuối cùng.

| Bài kiểm thử | Cách làm | Kết quả |
|---|---|---|
| Có khóa (`TonKhoService`) | 20 luồng cùng mua 1 xe, kho còn 10 | 10 thành công, 10 báo hết hàng, tồn kho cuối **0** - **đúng** |
| Không khóa (để so sánh) | 20 luồng cùng đọc rồi cùng ghi, không khóa | 20 "thành công", tồn kho cuối **9** - bán vượt 10 xe, mất 19 lần cập nhật - **sai** |
| Nhập và xuất xen kẽ | 25 luồng nhập 1 + 25 luồng xuất 1, kho đầu 50 | Tồn kho cuối 50 - đúng |
| Chống deadlock | 20 luồng × 5 đơn, mỗi đơn 2 xe ghi theo thứ tự ngược nhau | Không treo; mỗi xe từ 100 về 0 - đúng |
| Đơn thiếu hàng một xe | Đơn gồm xe đủ hàng và xe thiếu hàng | Không trừ xe nào (giao dịch bị hủy toàn bộ) |
| Sửa xe trong lúc có người mua (mới) | Mở form sửa (đọc tồn 50), một khách mua 1 xe, rồi lưu form | Tồn kho **49** - đúng. Không có `@DynamicUpdate`: **50** - sai (lần bán bị ghi đè) |
| Bán và sửa cùng lúc (mới) | 20 luồng mua + 10 luồng sửa thông tin cùng một xe, kho đầu 50 | Tồn kho cuối 30 - đúng |
| Đăng ký trùng số điện thoại (mới) | Khách thứ hai dùng số điện thoại đã có | Giao dịch hủy cả tài khoản lẫn hồ sơ; ràng buộc UNIQUE là chốt chặn cuối |
| Bộ đếm đăng nhập sai | 50 luồng × 200 lần ghi nhận sai cùng một tên | Đếm đủ 10.000 lần |

Hai dòng đầu là bằng chứng chính cho hạng mục 5.2: cùng một tình huống, không khóa thì sai, có khóa thì đúng.
Dòng "sửa xe trong lúc có người mua" là lỗi phát hiện khi tích hợp, chi tiết trong `thiet-ke-dong-thoi-va-mang.md` mục 3.4.

## 5. Kiểm thử phần mạng (Socket server và client)

### 5.1 Kiểm thử tự động (`KhoSocketServerTest`)

| Kịch bản | Kết quả mong đợi | Kết quả |
|---|---|---|
| Kết nối xong, chưa đăng nhập đã gửi lệnh | Lời chào; lệnh bị từ chối `ERR 401` | Đạt |
| Khách hàng đăng nhập đúng mật khẩu | `ERR 403` - không được dùng client kho | Đạt |
| Đăng nhập sai 3 lần trong một kết nối | Server báo lỗi rồi ngắt kết nối | Đạt |
| Nhân viên xem, nhập, xuất kho; lệnh sai; xe không tồn tại; xuất quá số tồn | Dữ liệu đúng; `ERR 400`, `ERR 404`, `ERR 409` đúng trường hợp | Đạt |
| 3 client cùng xuất kho một xe (15 yêu cầu, kho có 12) | 12 thành công, 3 hết hàng, tồn kho 0 | Đạt |
| Đã có 5 client (giới hạn khi kiểm thử), client thứ 6 kết nối | Client thứ 6 nhận `ERR 503`; một client thoát thì client mới vào được | Đạt |
| Gửi dòng lệnh dài hơn 512 ký tự | Báo lỗi và ngắt kết nối | Đạt |

### 5.2 Chương trình kiểm thử nhiều client (`KiemThuNhieuClient`)

Chạy với ứng dụng web đang kết nối SQL Server (dữ liệu của script), 5 client cùng xuất kho xe số 15 (Ducati Monster, còn 2 chiếc):

```bash
java -cp client/target/xemay-client.jar com.xemay.client.KiemThuNhieuClient localhost 9090 nhanvien1 123456 15 5 2
```

Kết quả thật:

```
=== KIỂM THỬ 5 CLIENT CÙNG LÚC XUẤT KHO XE #15 ===
Tồn kho trước kiểm thử : 2
Tổng số yêu cầu xuất kho: 5 client x 2 lần x 1 xe = 10

Thời gian chạy          : 54 ms
Xuất kho thành công     : 2 (kỳ vọng 2)
Bị từ chối do hết hàng  : 8
Lỗi khác                : 0
Tồn kho sau kiểm thử    : 0
[ĐẠT] Tồn trước - tồn sau (2) = số lần thành công (2)
[ĐẠT] Tồn kho sau không âm
[ĐẠT] Còn hàng thì xuất được (không từ chối oan)
==> KẾT QUẢ ĐÚNG: không mất lần trừ kho nào, không bán vượt tồn kho.
Đã nhập lại 2 xe để khôi phục tồn kho ban đầu (2).
```

Log của server cùng lúc đó: mỗi client được một luồng riêng phục vụ.

```
[kho-client-2] Client /127.0.0.1:49568 đã kết nối [luồng phục vụ: kho-client-2] - đang phục vụ 1 client
[kho-client-3] Client /127.0.0.1:49578 đã kết nối [luồng phục vụ: kho-client-3] - đang phục vụ 2 client
[kho-client-4] Client /127.0.0.1:49590 đã kết nối [luồng phục vụ: kho-client-4] - đang phục vụ 3 client
[kho-client-5] Client /127.0.0.1:49592 đã kết nối [luồng phục vụ: kho-client-5] - đang phục vụ 4 client
```

### 5.3 Kịch bản demo 3 client bằng tay (hạng mục 5.4)

1. Chạy script `QL_CUAHANG_XEMAY.sql` để có dữ liệu ban đầu, chạy ứng dụng web. Mở 3 cửa sổ dòng lệnh, ở mỗi cửa sổ chạy
   `java -jar client/target/xemay-client.jar`.
2. Ba cửa sổ đăng nhập `nhanvien1`, `nhanvien2`, `nhanvien3` (mật khẩu `123456`), xem tồn kho xe số 16 (Triumph Trident 660, còn 3).
3. Lần lượt ở từng cửa sổ xuất 1 xe số 16: ba lần đầu đều thành công, tồn kho giảm từ 3 về 0; lần thứ tư, ở cửa sổ nào
   cũng vậy, báo "Không đủ hàng".
4. Mở trang Quản trị > Tổng quan: thẻ "Client kho đang kết nối" hiện 3; sau tối đa 1 phút, Triumph Trident 660 hiện "Hết hàng".
5. Tắt ngang một cửa sổ client (Ctrl+C): log server ghi "Kết nối với client ... bị gián đoạn", hai client còn lại vẫn dùng bình thường.
6. Nhập lại 3 xe để trả dữ liệu về như cũ (hoặc chạy lại script).

## 6. Kịch bản chờ phần giỏ hàng, đặt hàng, hóa đơn

Các dòng dưới đây chưa chạy được vì chức năng chưa có; khi thành viên phụ trách làm xong, người kiểm thử chạy và điền kết quả.

| Mã | Kịch bản | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| DH-01 | Hai nhân viên cùng xác nhận hai đơn mua chiếc xe cuối cùng | Một đơn xác nhận được, đơn kia báo hết hàng; tồn kho 0 | Chờ tích hợp |
| DH-02 | Xác nhận đơn gồm nhiều xe, một xe không đủ hàng | Không tạo hóa đơn, không trừ xe nào | Chờ tích hợp |
| DH-03 | Hủy đơn đã xác nhận | Tồn kho được cộng lại đúng số xe | Chờ tích hợp |
| DH-04 | Khách thêm xe đã ngừng kinh doanh vào giỏ | Báo không đặt được | Chờ tích hợp |
| GD-03 | Trang chủ, danh sách xe, chi tiết xe trên điện thoại | Lưới 1 cột, ảnh 4:3, giá đỏ đậm | Chờ tích hợp |
