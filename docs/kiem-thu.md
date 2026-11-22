# Kiểm thử - bản đã tích hợp

Tài liệu dùng cho **Chương 4** của báo cáo. Mọi kết quả bên dưới là kết quả chạy thật, không phải kết quả dự kiến.
Bản này gồm phần đăng nhập, phân quyền, đồng thời, mạng; phần xe máy, hãng xe, khách hàng (ghép lần 1, 20-21/11/2026);
giao diện cho khách, giỏ hàng, đặt hàng (ghép lần 2, 22/11/2026).

## 1. Môi trường kiểm thử

| Thành phần | Phiên bản |
|---|---|
| JDK | OpenJDK 21 (lần 2) và OpenJDK 25 (lần 1); mã nguồn biên dịch ở mức Java 17 |
| Cơ sở dữ liệu | SQL Server 2025 (phiên bản 17.0) với script `QL_CUAHANG_XEMAY.sql`; H2 2.3 trong bộ nhớ cho kiểm thử nhanh |
| Kiểm thử tự động | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |
| Kiểm thử giao diện | Trình duyệt Chromium, khung hình máy tính 1440×900 và điện thoại iPhone 13 (390×844) |

## 2. Kiểm thử tự động (73 bài)

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
| `GioHangDatHangTest` | 8 | Trang chủ, danh sách xe (tìm theo "hãng + tên", khoảng giá sai), chi tiết xe, xe ngừng bán -> 404; giỏ hàng chỉ cho khách hàng; giỏ lưu trong CSDL, không vượt tồn kho; đặt hàng tạo đơn và trừ kho; thiếu hàng hoặc xe ngừng bán thì không tạo đơn; **hai khách cùng đặt chiếc xe cuối**; **một khách bấm đặt hai lần cùng lúc** |
| `TonKhoDongThoiTest` | 7 | Đồng thời trên tồn kho (mục 4) |
| `KhoSocketServerTest` | 7 | Socket server, giao thức, nhiều client (mục 5) |
| `BoDemDangNhapSaiTest` | 5 | Bộ đếm đăng nhập sai, gồm 50 luồng ghi đồng thời |

| Lần chạy | Kết quả | Thời gian |
|---|---|---|
| Lần 2 (73 bài) trên H2 trong bộ nhớ (JDK 21) | **73/73 đạt** | 18 giây |
| Lần 2 (73 bài) trên SQL Server 2025 (JDK 21), CSDL kiểm thử riêng | **73/73 đạt** | 21 giây |
| Lần 1 (65 bài) trên H2 và trên SQL Server 2025 (JDK 25) | 65/65 đạt ở cả 2 lần | 19-24 giây |
| Bản trước tích hợp (41 bài) trên SQL Server 2022 và 2025, JDK 21 và 25 | 41/41 đạt ở cả 4 lần | 14-16 giây |

**Bài kiểm thử có kiểm chứng ngược:** tạm bỏ `@DynamicUpdate` trên `XeMay` thì bài `suaXeTrongLucCoNguoiMua` báo sai
(mong đợi tồn kho 49, thực tế 50); đặt lại thì đạt. Tức là bài kiểm thử thật sự bắt được lỗi mất cập nhật (mục 4).

**Phải chạy cả trên SQL Server, không chỉ H2:** ở lần 2, bản đầu của giỏ hàng đạt hết trên H2 nhưng trên SQL Server thì
khách mới thêm xe vào giỏ lần đầu bị treo (SQL Server khóa cả khoảng chỉ mục khi đọc có khóa một dòng chưa tồn tại, H2 thì không).
Các bài trong `GioHangDatHangTest` đều dùng khách mới nên chạy trên SQL Server là gặp ngay. Nguyên nhân và cách sửa:
`thiet-ke-dong-thoi-va-mang.md` mục 3.5.

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
| GD-01 | Hiển thị trên điện thoại | iPhone 13, trang quản lý xe máy | Bộ lọc 5 ô xếp dọc; bảng vừa màn hình, không cuộn ngang | Đạt |
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

## 6. Kịch bản cho phần giao diện khách, giỏ hàng, đặt hàng (lần 2)

Chạy với ứng dụng kết nối SQL Server 2025, dữ liệu vừa tạo lại bằng script, thao tác trên trình duyệt Chromium
(kịch bản DH-03 dùng script điều khiển hai cửa sổ trình duyệt để hai lần bấm đi cùng lúc). Ảnh chụp: `docs/hinh-anh/24` đến `30`.

### 6.1 Kịch bản của người tích hợp

| Mã | Kịch bản | Dữ liệu / thao tác | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|
| KX-01 | Trang chủ khi chưa đăng nhập | mở `/` | Hiện 6 xe mới nhất đang bán, tên cửa hàng MotoStore | Đạt |
| KX-02 | Danh sách xe: tìm, lọc, phân trang | từ khóa `honda`; giá từ 100.000.000 đ + còn hàng; trang 2 | 4 xe Honda; 7 xe; mỗi trang 9 xe | Đạt |
| KX-03 | Chi tiết xe | `/xe-may/15`; `/xe-may/9999` | Tiêu đề "Monster \| MotoStore", khách chưa đăng nhập thấy lời mời đăng nhập; mã không có -> 404 | Đạt (trước khi sửa: luôn lỗi 500) |
| GH-01 | Ai được vào giỏ hàng | chưa đăng nhập; `nhanvien1` | Chuyển tới trang đăng nhập; nhân viên bị 403 | Đạt |
| GH-02 | Thêm, sửa số lượng trong giỏ | `khach1`: thêm 2 Vision, sửa thành 3, sửa thành 50 | "Đã thêm xe vào giỏ hàng"; "Đã cập nhật giỏ hàng"; "Mỗi mẫu xe được chọn tối đa 20 chiếc" | Đạt |
| GH-03 | Khách mới đăng ký thêm xe vào giỏ lần đầu | tài khoản vừa đăng ký, chưa có `GIO_HANG`; thêm 1 lần, rồi 3 tab cùng lúc | Thêm được ngay; 3 tab tạo đúng 1 giỏ có 3 dòng | Đạt sau khi sửa (bản đầu bị treo, xem mục 2) |
| DH-01 | Đặt hàng thiếu địa chỉ | xóa trống ô địa chỉ | "Vui lòng nhập địa chỉ giao xe.", không tạo đơn | Đạt |
| DH-02 | Đặt hàng | `khach1`: 3 Vision + 1 Wave Alpha có sẵn trong giỏ mẫu | "Đặt hàng thành công. Mã đơn hàng: 22, tổng tiền 114.000.000 đ..."; `DON_HANG` trạng thái `CHO_XAC_NHAN`, 2 dòng; tồn Vision 24 -> 21, Wave Alpha 29 -> 28; giỏ trống | Đạt |
| DH-03 | Hai khách cùng đặt chiếc xe cuối | `khach2`, `khach3` mỗi người 2 chiếc Monster (còn 2), bấm "Đặt hàng" cùng lúc | Một người đặt được; người kia: "Xe "Ducati Monster" chỉ còn 0 chiếc, không đủ số lượng yêu cầu (2).", giỏ còn nguyên; tồn kho 0 | Đạt |
| DH-04 | Xác nhận đơn đặt trên web bằng procedure | `EXEC sp_XacNhanDonHang 22`, rồi chạy lại lần 2 | Đơn `DA_XAC_NHAN`, có hóa đơn 2 dòng; tồn kho **không đổi** (không trừ lần hai); lần 2 báo "Đơn hàng không ở trạng thái chờ xác nhận." | Đạt |
| GD-03 | Trang cho khách trên điện thoại | iPhone 13: danh sách xe, giỏ hàng | Không cuộn ngang; không có lỗi JavaScript | Đạt |

### 6.2 Bảng kiểm thử giao diện của thành viên 2 (GD01-GD18)

Bảng nộp kèm phần giao diện (`Bang_KiemThu_GiaoDien.csv`, cột "Thực tế" còn trống), chạy trên bản đã ghép với dữ liệu của script.
GD13 ban đầu mong đợi "xóa khỏi session"; giỏ hàng nay lưu trong CSDL nên kiểm tra trong bảng `CHI_TIET_GIO_HANG`.

| Mã | Chức năng | Cách thực hiện | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| GD01 | Trang chủ | `GET /` | Hiển thị xe thật từ CSDL | 6 xe mới nhất: Orla, Vento S, Imperiale 400, Classic 350, Trident 660, Monster | Đạt |
| GD02 | Danh sách xe | `GET /xe-may` | Hiển thị xe từ CSDL | "Tìm thấy 20 mẫu xe", trang 1 có 9 xe (CSDL có 20 xe đang bán) | Đạt |
| GD03 | Tìm theo tên | `/xe-may?q=Vision` | Chỉ hiện xe trùng tên | Chỉ có Vision | Đạt |
| GD04 | Lọc giá | `/xe-may?giaTu=20000000&giaDen=50000000` | Xe trong khoảng giá | 5 xe, giá từ 22.000.000 đến 47.000.000 đ | Đạt |
| GD05 | Còn hàng | `/xe-may?conHang=true` | Chỉ xe còn hàng | Cho Monster về 0 xe: còn 19 mẫu, Monster không xuất hiện | Đạt |
| GD06 | Phân trang | `/xe-may?page=1` | Trang và tổng số bản ghi phù hợp | Trang 2: 9 xe, trang 3: 2 xe, tổng 20 | Đạt |
| GD07 | Sắp xếp | `/xe-may?sapXep=gia-tang` | Giá tăng dần | 20 xe, từ 16.000.000 đến 400.000.000 đ, không có cặp nào ngược thứ tự | Đạt |
| GD08 | Chi tiết | `/xe-may/1` | Tên, giá, tồn kho đúng CSDL | Wave Alpha, 18.000.000 đ, tồn 29, khớp CSDL | Đạt |
| GD09 | Mã không tồn tại | `/xe-may/9999999` | 404, trang lỗi thân thiện | HTTP 404, trang "Thông báo lỗi" | Đạt |
| GD10 | Hình ảnh | Xe có ảnh / chưa có ảnh | Đúng ảnh hoặc dòng "chưa có ảnh" | Gán tạm `HinhAnh` cho Orla: hiện ảnh; các xe khác hiện "Chưa có ảnh thực tế" | Đạt |
| GD11 | Giỏ hàng khách | Đăng nhập `khach5`, thêm 2 Sirius từ trang xe | Có xe trong giỏ | Giỏ có 2 mẫu (1 xe của dữ liệu mẫu + Sirius) | Đạt |
| GD12 | Số lượng trong giỏ | Tăng, giảm số lượng | Không vượt tồn, tổng tiền đúng | Trident 660 còn 3, sửa thành 4: "Không đủ số lượng tồn kho"; Sirius sửa thành 25: "Mỗi mẫu xe được chọn tối đa 20 chiếc"; sửa thành 3: tổng 118.000.000 đ, đúng bằng tính từ CSDL | Đạt |
| GD13 | Xóa khỏi giỏ | Bấm Xóa | Xóa khỏi giỏ | Sirius mất khỏi trang và khỏi bảng `CHI_TIET_GIO_HANG` | Đạt |
| GD14 | CSRF | `POST /gio-hang/them` không có CSRF | Bị chặn 403 | HTTP 403 | Đạt |
| GD15 | Phân quyền | Chưa đăng nhập hoặc nhân viên `GET /gio-hang` | Chặn hoặc yêu cầu đăng nhập | Chưa đăng nhập: chuyển tới `/dang-nhap`; nhân viên: 403 | Đạt |
| GD16 | Tồn kho | Thêm vào giỏ rồi kiểm tra SQL | Không trừ số lượng tồn | Tồn Sirius trước 24, sau khi thêm vào giỏ vẫn 24 (kho chỉ trừ khi đặt hàng) | Đạt |
| GD17 | Hiển thị di động | Bề rộng 390 px | Không tràn ngang, menu hoạt động | Không tràn ngang ở `/`, `/xe-may`, `/xe-may/1`; nút menu mở được danh sách mục | Đạt |
| GD18 | Hợp nhất | Ghép với phần quản lý xe và khách hàng | Không trùng Entity, bảng, URL | Dùng chung Entity `XeMay`, `HangXe`, `KhachHang`; URL khách tách khỏi `/quan-tri`; khởi động với `ddl-auto=validate` thành công | Đạt |

### 6.3 Kịch bản chờ phần còn thiếu

Chưa có chức năng; thành viên phụ trách làm xong thì chạy và điền kết quả:

| Mã | Kịch bản | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| DH-05 | Nhân viên hủy đơn chờ xác nhận | Tồn kho được cộng lại đúng số xe của đơn | Chờ tích hợp |
| DH-06 | Hai nhân viên cùng xác nhận một đơn trên trang quản trị | Một người xác nhận được, người kia báo đơn không còn chờ xác nhận; chỉ có một hóa đơn | Chờ tích hợp |
| HD-01 | Xem, in hóa đơn của đơn đã xác nhận | Đúng các dòng và tổng tiền của đơn | Chờ tích hợp |
| DH-07 | Khách xem "Đơn hàng của tôi" | Chỉ thấy đơn của chính mình, đúng trạng thái | Chờ tích hợp |
