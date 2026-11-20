# Kiểm thử - phần đăng nhập, phân quyền, đồng thời và mạng

Tài liệu dùng cho **Chương 4** của báo cáo. Mọi kết quả bên dưới là kết quả chạy thật, không phải kết quả dự kiến.

## 1. Môi trường kiểm thử

| Thành phần | Phiên bản |
|---|---|
| JDK | OpenJDK 21 và OpenJDK 25 (mã nguồn biên dịch ở mức Java 17) |
| Cơ sở dữ liệu | SQL Server 2022 (phiên bản 16.0) và SQL Server 2025 (phiên bản 17.0); H2 2.3 trong bộ nhớ cho kiểm thử nhanh |
| Kiểm thử tự động | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |
| Kiểm thử giao diện | Trình duyệt Chromium, khung hình máy tính 1280×800 và điện thoại iPhone 13 (390×844) |

## 2. Kiểm thử tự động (41 bài)

Chạy tại thư mục gốc: `./mvnw -pl web test`. Cách chạy trên SQL Server: xem README, bước 5.

| Lớp kiểm thử | Số bài | Nội dung |
|---|---|---|
| `DangNhapPhanQuyenTest` > Đăng nhập | 7 | Đăng nhập đúng theo từng vai trò; không phân biệt hoa thường; sai mật khẩu; sai 5 lần bị tạm khóa rồi được quản trị viên mở khóa; tài khoản bị khóa; nhật ký ghi bằng luồng nền |
| `DangNhapPhanQuyenTest` > Đăng ký | 3 | Dữ liệu sai báo lỗi đúng ô; đăng ký hợp lệ, mật khẩu được băm BCrypt; trùng tên đăng nhập |
| `DangNhapPhanQuyenTest` > Phân quyền theo URL | 6 | Khách chưa đăng nhập, khách hàng, nhân viên, quản trị viên; thiếu CSRF token; CSS, font, ảnh vẫn tải được khi chưa đăng nhập |
| `DangNhapPhanQuyenTest` > Menu theo vai trò | 3 | Mỗi vai trò chỉ thấy menu được phép |
| `DangNhapPhanQuyenTest` > Phân quyền theo phương thức | 3 | `@PreAuthorize` chặn nhân viên; quản trị viên không tự khóa/tự xóa; xóa tài khoản vẫn giữ nhật ký |
| `TonKhoDongThoiTest` | 7 | Đồng thời trên tồn kho (mục 4) |
| `KhoSocketServerTest` | 7 | Socket server, giao thức, nhiều client (mục 5) |
| `BoDemDangNhapSaiTest` | 5 | Bộ đếm đăng nhập sai, gồm 50 luồng ghi đồng thời |

| Lần chạy | Kết quả | Thời gian |
|---|---|---|
| Trên H2 trong bộ nhớ | **41/41 đạt** | 21 giây |
| Trên SQL Server 2022 (JDK 21) | **41/41 đạt** | 16 giây |
| Trên SQL Server 2025 (JDK 21) | **41/41 đạt** | 14 giây |
| Trên SQL Server 2025 (JDK 25) | **41/41 đạt** | 15 giây |

## 3. Kịch bản kiểm thử chức năng (thực hiện trên giao diện)

| Mã | Kịch bản | Dữ liệu / thao tác | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|
| DN-01 | Đăng nhập đúng (quản trị viên) | `admin` / `123456` | Vào trang `/quan-tri` | Đạt |
| DN-02 | Đăng nhập đúng (khách hàng) | `khachhang` / `123456` | Về trang chủ, menu không có mục Quản trị | Đạt |
| DN-03 | Sai mật khẩu | `admin` / `sai` | Ở lại trang đăng nhập, báo "Tên đăng nhập hoặc mật khẩu không đúng." | Đạt |
| DN-04 | Gõ tên đăng nhập chữ hoa | `NGUYEN.AN` | Đăng nhập được như `nguyen.an` | Đạt |
| DN-05 | Sai 5 lần liên tiếp | `khachhang` sai 5 lần, lần 6 nhập đúng | Từ lần 5 báo tạm khóa 15 phút; lần 6 vẫn bị từ chối; trang quản trị hiện nhãn "Tạm khóa" | Đạt |
| DN-06 | Tài khoản bị quản trị viên khóa | `tathithu` / `123456` | Báo "Tài khoản đã bị khóa" | Đạt |
| DK-01 | Đăng ký dữ liệu sai | tên `ab`, mật khẩu `123`, nhập lại `456`, email sai, SĐT `123` | Mỗi ô báo lỗi riêng ngay bên dưới | Đạt |
| DK-02 | Đăng ký hợp lệ | `Nguyen.An`, `matkhau1`, `An@Gmail.com` | Chuyển về đăng nhập, báo thành công; lưu tên và email chữ thường | Đạt |
| DK-03 | Trùng tên đăng nhập | đăng ký lại `nguyen.an` | Báo "Tên đăng nhập này đã có người sử dụng" cạnh ô tên | Đạt |
| PQ-01 | Chưa đăng nhập vào `/quan-tri` | | Chuyển tới trang đăng nhập | Đạt |
| PQ-02 | Khách hàng vào `/quan-tri` | | Trang lỗi 403 thân thiện | Đạt |
| PQ-03 | Nhân viên vào `/quan-tri/tai-khoan` | | Trang lỗi 403 | Đạt |
| PQ-04 | Gửi form không có CSRF token | POST `/quan-tri/tai-khoan/2/khoa` | Bị từ chối 403 | Đạt |
| TK-01 | Thêm nhân viên | `nv.binh`, vai trò Nhân viên | Có trong danh sách, báo "Đã thêm tài khoản" | Đạt |
| TK-02 | Quản trị viên tự khóa mình | bấm Khóa ở dòng `admin` | Bị chặn, báo "Bạn không thể tự khóa..." (giao diện cũng ẩn nút) | Đạt |
| TK-03 | Khóa rồi xóa tài khoản | khóa `nguyen.an`, thử đăng nhập, rồi xóa | Không đăng nhập được; có hộp xác nhận trước khi xóa; xóa xong nhật ký vẫn còn | Đạt |
| TK-04 | Tìm kiếm, lọc, phân trang, sắp xếp | từ khóa `a`, sắp theo họ tên giảm dần, trang 2 | Kết quả đúng; chuyển trang giữ nguyên điều kiện lọc | Đạt |
| TK-05 | Sửa URL sắp xếp theo cột không tồn tại | `?sort=khongTonTai,asc` | Không lỗi, dùng thứ tự mặc định | Đạt |
| GD-01 | Hiển thị trên điện thoại | iPhone 13 | Menu thu gọn; bảng tài khoản vừa màn hình, không cần cuộn ngang | Đạt |
| GD-02 | Kiểm tra độ tương phản màu chữ và bố cục | Quét tự động 11 trang (đăng nhập, đăng ký, trang chủ, quản trị, tài khoản, lịch sử, trang lỗi 403/404...) | Không có lỗi | Đạt (đã sửa 2 lỗi tương phản phát hiện ở lần quét đầu) |

## 4. Kiểm thử đồng thời (tồn kho)

Mỗi bài cho nhiều luồng **xuất phát cùng một lúc** (dùng `CountDownLatch`) rồi kiểm tra kết quả cuối cùng.

| Bài kiểm thử | Cách làm | Kết quả trên SQL Server |
|---|---|---|
| Có khóa (`TonKhoService`) | 20 luồng cùng mua 1 xe, kho còn 10 | 10 thành công, 10 báo hết hàng, tồn kho cuối **0** - **đúng** |
| Không khóa (để so sánh) | 20 luồng cùng đọc rồi cùng ghi, không khóa | 20 "thành công", tồn kho cuối **9** - bán vượt 10 xe, mất 19 lần cập nhật - **sai** |
| Nhập và xuất xen kẽ | 25 luồng nhập 1 + 25 luồng xuất 1, kho đầu 50 | Tồn kho cuối 50 - đúng |
| Chống deadlock | 20 luồng × 5 đơn, mỗi đơn 2 xe ghi theo thứ tự ngược nhau | Không treo; mỗi xe từ 100 về 0 - đúng |
| Đơn thiếu hàng một xe | Đơn gồm xe đủ hàng và xe thiếu hàng | Không trừ xe nào (giao dịch bị hủy toàn bộ) |
| Bộ đếm đăng nhập sai | 50 luồng × 200 lần ghi nhận sai cùng một tên | Đếm đủ 10.000 lần |

Hai dòng đầu là bằng chứng chính cho hạng mục 5.2: cùng một tình huống, không khóa thì sai, có khóa thì đúng.

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

Chạy với ứng dụng web đang kết nối SQL Server, 5 client cùng xuất kho xe số 1 (Honda Vision 2024):

```bash
java -cp client/target/xemay-client.jar com.xemay.client.KiemThuNhieuClient localhost 9090 nhanvien 123456 1 5 4
```

Kết quả thật:

```
=== KIỂM THỬ 5 CLIENT CÙNG LÚC XUẤT KHO XE #1 ===
Tồn kho trước kiểm thử : 12
Tổng số yêu cầu xuất kho: 5 client x 4 lần x 1 xe = 20

Thời gian chạy          : 157 ms
Xuất kho thành công     : 12 (kỳ vọng 12)
Bị từ chối do hết hàng  : 8
Lỗi khác                : 0
Tồn kho sau kiểm thử    : 0
[ĐẠT] Tồn trước - tồn sau (12) = số lần thành công (12)
[ĐẠT] Tồn kho sau không âm
[ĐẠT] Còn hàng thì xuất được (không từ chối oan)
==> KẾT QUẢ ĐÚNG: không mất lần trừ kho nào, không bán vượt tồn kho.
Đã nhập lại 12 xe để khôi phục tồn kho ban đầu (12).
```

Log của server cùng lúc đó cho thấy mỗi client được một luồng riêng phục vụ và số tồn giảm đúng từng đơn vị:

```
[kho-client-1] Client /127.0.0.1:47086 đã kết nối [luồng phục vụ: kho-client-1] - đang phục vụ 1 client
[kho-client-2] Client /127.0.0.1:47094 đã kết nối [luồng phục vụ: kho-client-2] - đang phục vụ 2 client
...
[kho-client-4] Xuất kho xe #1 'Honda Vision 2024': -1 -> còn 9 [luồng: kho-client-4]
[kho-client-7] Xuất kho xe #1 'Honda Vision 2024': -1 -> còn 8 [luồng: kho-client-7]
[kho-client-5] Xuất kho xe #1 'Honda Vision 2024': -1 -> còn 7 [luồng: kho-client-5]
```

### 5.3 Kịch bản demo 3 client bằng tay (hạng mục 5.4)

1. Chạy ứng dụng web. Mở 3 cửa sổ dòng lệnh, ở mỗi cửa sổ chạy `java -jar client/target/xemay-client.jar`.
2. Cả 3 đăng nhập `nhanvien` / `123456`, chọn chức năng 3 xem tồn kho xe số 4 (Honda Winner X, còn 3).
3. Lần lượt ở từng cửa sổ chọn chức năng 5, xuất 1 xe số 4: ba lần đầu (mỗi cửa sổ một lần) đều thành công và tồn kho
   giảm từ 3 về 0; lần thứ tư, ở cửa sổ nào cũng vậy, báo "Không đủ hàng".
4. Mở trang Quản trị > Tổng quan: thẻ "Client kho đang kết nối" hiện 3; sau tối đa 1 phút, Honda Winner X hiện "Hết hàng".
5. Tắt ngang một cửa sổ client (Ctrl+C): log server ghi "Kết nối với client ... bị gián đoạn", hai client còn lại vẫn dùng bình thường.
6. Nhập lại 3 xe (chức năng 4) để trả dữ liệu về như cũ.

## 6. Kịch bản cho phần của các thành viên khác (thực hiện khi tích hợp)

Các dòng dưới đây chưa chạy được vì chức năng chưa có; khi tích hợp, người phụ trách kiểm thử chạy và điền kết quả.

| Mã | Kịch bản | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| DH-01 | Hai khách cùng đặt chiếc xe cuối cùng | Một người đặt được, người kia báo hết hàng; tồn kho 0 | Chờ tích hợp |
| DH-02 | Đặt hàng gồm nhiều xe, một xe không đủ hàng | Không tạo hóa đơn, không trừ xe nào | Chờ tích hợp |
| DH-03 | Hủy hóa đơn | Tồn kho được cộng lại đúng số xe | Chờ tích hợp |
| XM-01 | Sửa thông tin xe trong lúc có người đang mua xe đó | Số tồn không bị form sửa xe ghi đè | Chờ tích hợp |
| XM-02 | Xóa hãng xe đang có xe tham chiếu | Báo không xóa được, dữ liệu không đổi | Chờ tích hợp |
| KH-01 | Khách đăng ký tài khoản | Có cả dòng `tai_khoan` và dòng `khach_hang` | Chờ tích hợp |
| KH-02 | Xóa tài khoản khách đã có hóa đơn | Báo không xóa được (nên khóa thay vì xóa) | Chờ tích hợp |
