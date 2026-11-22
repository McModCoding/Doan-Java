# Báo cáo tích hợp

Người tích hợp: thành viên 1 (đăng nhập, phân quyền, xử lý đồng thời, tích hợp hệ thống, kiểm thử).
Tài liệu này ghi lại từng lần tích hợp: đã nhận được gì, phân công đã đủ chưa, lỗi phát hiện khi ghép và cách sửa.

| Lần | Ngày | Nội dung | Mục |
|---|---|---|---|
| 1 | 20/11/2026 | Script CSDL chung, quản lý xe máy - hãng xe (thành viên 4), quản lý khách hàng (thành viên 3) | 1-5 |
| 2 | 22/11/2026 | Giao diện cho khách (thành viên 2), giỏ hàng (thành viên 5), đặt hàng | 6 |
| | | Việc còn lại của cả nhóm sau lần 2 | 7 |

## 1. Lần 1 - Đã nhận được

| Phần nộp | Nội dung | Nhận xét khi nhận |
|---|---|---|
| `QL_CUAHANG_XEMAY.sql` | Script SQL Server: 11 bảng, dữ liệu mẫu (20 dòng mỗi bảng chính), procedure `sp_XacNhanDonHang` | Thiết kế tốt, đủ quan hệ một-nhiều. Mật khẩu mẫu là chuỗi giữ chỗ nên không đăng nhập được; thiếu bảng nhật ký đăng nhập |
| `quanlyxemay.rar` | Dự án Spring Boot 4.1.2: Entity `HangXe`, `XeMay`; Repository (tìm kiếm, lọc nhiều điều kiện); Service có kiểm tra dữ liệu, ngừng kinh doanh thay cho xóa; Controller `/xemay`, `/hangxe`; `XeMayDTO` | Phần Java làm kỹ. **9 file giao diện HTML đều rỗng (0 byte)**; cấu hình kết nối CSDL thiếu phần `jdbc:sqlserver://...` |
| `QLKH.rar` | Dự án Spring Boot 4.1.2: `Customer`, `CustomerService` (tìm theo tên hoặc số điện thoại, gửi email chào mừng `@Async`), `CustomerController` `/customers`, 2 trang giao diện | Entity dùng bảng `Customers` của một CSDL khác (`MotorcycleStoreDB`), không khớp script chung; trang danh sách là trang tĩnh |

## 2. Lần 1 - Phân công đã đủ chưa

| Thành viên | Phân công | Đã nhận | Còn thiếu |
|---|---|---|---|
| 1 | Đăng nhập, phân quyền, đồng thời, tích hợp, kiểm thử | Đủ (kèm lập trình mạng) | - |
| 2 | Trang chủ, danh sách xe, chi tiết xe, giỏ hàng | **Chưa nhận được gì** | Toàn bộ |
| 3 | Phân tích, use case, thiết kế CSDL, quản lý khách hàng | Code quản lý khách hàng | Tài liệu phân tích, bảng chức năng / use case, sơ đồ ERD |
| 4 | CSDL SQL Server, quản lý xe máy, hãng xe, tìm kiếm/lọc | Script CSDL, phần Java xe máy - hãng xe | Giao diện (file rỗng) - người tích hợp đã làm tạm, xem mục 3.2 |
| 5 | Giỏ hàng, đặt hàng, hóa đơn, chi tiết hóa đơn | Bảng và procedure trong script | **Toàn bộ code Java** (Entity, Service, Controller, giao diện) |

Theo thống nhất với nhóm, lần tích hợp này **ghép phần đã nộp và làm các trang quản trị đang rỗng** để chạy và kiểm thử được;
giỏ hàng, đặt hàng, hóa đơn và các trang cho khách để thành viên phụ trách tự làm (rubric chấm lịch sử Git của từng người).

## 3. Lần 1 - Lỗi phát hiện khi ghép và cách sửa

### 3.1 Script CSDL

| # | Vấn đề | Nếu để nguyên | Đã sửa |
|---|---|---|---|
| 1 | `KHACH_HANG.MaTaiKhoan INT NULL UNIQUE` | SQL Server coi các giá trị NULL là trùng nhau: chỉ lưu được **một** khách chưa có tài khoản, khách thứ hai báo lỗi trùng khóa | Bỏ `UNIQUE`, thay bằng chỉ mục có điều kiện `UX_KHACHHANG_TAIKHOAN ... WHERE MaTaiKhoan IS NOT NULL` |
| 2 | Bảng có cột tính toán `PERSISTED` (`ThanhTien`) cần `QUOTED_IDENTIFIER ON` | Chạy bằng `sqlcmd` báo lỗi 1934 ngay lệnh tạo bảng `CHI_TIET_DON_HANG` (SSMS thì bật sẵn nên không thấy) | Thêm `SET QUOTED_IDENTIFIER ON` ở đầu script |
| 3 | Mật khẩu mẫu `$2a$10$REPLACE_WITH_BCRYPT_HASH` | Không tài khoản mẫu nào đăng nhập được | Thay bằng mã BCrypt thật của `123456` |
| 4 | `TAI_KHOAN` không có họ tên; không có bảng nhật ký đăng nhập | Giao diện không hiện được tên nhân viên; mất chức năng lịch sử đăng nhập | Thêm cột `HoTen`, `SoDienThoai` và bảng `LICH_SU_DANG_NHAP`; khách mẫu có tên thật thay cho "Khách hàng N" |
| 5 | File lưu UTF-8 không có BOM, CSDL dùng collation mặc định | Một số bản SSMS đọc sai chữ có dấu; sắp xếp tên tiếng Việt chưa đúng | Lưu UTF-8 có BOM; tạo CSDL với `COLLATE Vietnamese_CI_AS` |

Mọi chỗ sửa trong script có đánh dấu `[Tich hop]`. Dữ liệu mẫu, thiết kế bảng, procedure của nhóm giữ nguyên.

### 3.2 Quản lý xe máy, hãng xe

| # | Vấn đề | Nếu để nguyên | Đã sửa |
|---|---|---|---|
| 6 | Cột đặt tên `MaXe`, `TenXe`... nhưng Spring Boot mặc định đổi tên cột sang `ma_xe`, `ten_xe` | Mọi truy vấn lỗi `Invalid column name 'ma_xe'` (phần nộp chưa từng chạy được với script) | Dùng chiến lược đặt tên giữ nguyên `PhysicalNamingStrategyStandardImpl`; mọi Entity ghi rõ tên cột |
| 7 | 9 file giao diện rỗng | Các trang xe máy, hãng xe trắng trơn | Viết trang danh sách (lọc theo tên, hãng, loại, giá, trạng thái; phân trang; sắp xếp) và form thêm / sửa theo quy chuẩn giao diện |
| 8 | Form sửa xe ghi lại `SoLuong` từ form | Đang sửa xe mà có khách mua, số tồn bị ghi đè bằng số cũ (mất cập nhật) | Bỏ dòng `setSoLuong` khi sửa; tồn kho chỉ đổi qua nút **Nhập kho** (gọi `TonKhoService`, có khóa dòng) |
| 9 | Dù không gọi `setSoLuong`, Hibernate mặc định vẫn ghi **mọi** cột khi UPDATE | Vẫn mất cập nhật như dòng 8 (kiểm thử `suaXeTrongLucCoNguoiMua` tái hiện: mong đợi 49, thực tế 50) | `@DynamicUpdate` trên `XeMay`: câu lệnh chỉ còn `update XE_MAY set GiaBan=? where MaXe=?` |
| 10 | Dự án gốc không có phân quyền; URL `/xemay`, `/hangxe` nằm ngoài khu vực quản trị | Ghép nguyên vào dự án chung thì cả **khách hàng** đăng nhập cũng thêm, sửa, ngừng bán xe được | Chuyển sang `/quan-tri/xe-may`, `/quan-tri/hang-xe`; ngừng kinh doanh, ngừng hoạt động hãng chỉ dành cho quản trị viên (`@PreAuthorize`) |
| 11 | Form hãng xe gắn thẳng vào Entity | Gửi thêm tham số `maHang=1` khi "thêm" sẽ ghi đè hãng số 1 | Chỉ nhận các ô của form (`@InitBinder`), luôn tạo mới khi thêm |
| 12 | Mọi lỗi dùng `IllegalArgumentException` và hiện một thông báo chung; thêm / sửa hãng xe bị lỗi thì chuyển về form trống | Không biết lỗi ở ô nào; nhập sai phải gõ lại từ đầu; nơi khác gọi Service mà quên bắt lỗi thì ra trang lỗi 500 | Dùng ngoại lệ chung của dự án: lỗi hiện ngay dưới ô nhập, mã không tồn tại ra trang 404; giữ lại dữ liệu đã gõ |
| 13 | Sửa hãng xe không gửi trạng thái thì tự bật lại hoạt động | Nhân viên sửa thông tin vô tình mở lại hãng đã ngừng | Không gửi trạng thái thì giữ nguyên |
| 14 | `@Table(schema = "dbo")`, dự án Spring Boot 4.1.2 | Kiểm thử tự động trên H2 không chạy; khác phiên bản dự án chung (3.5) | Bỏ `schema`; chuyển code vào module `web` (API dùng đều tương thích Spring Boot 3.5, không phải sửa) |

### 3.3 Quản lý khách hàng

| # | Vấn đề | Nếu để nguyên | Đã sửa |
|---|---|---|---|
| 15 | `Customer` ánh xạ bảng `Customers` (cùng `Employees`, `Invoices`, `Motorcycles`) của CSDL khác | Không dùng được với CSDL chung | Viết lại thành `KhachHang` theo bảng `KHACH_HANG`; bỏ 3 Entity thừa và Repository trùng (`layer/repository.java`) |
| 16 | `customer/list.html` là trang tĩnh: 12 khách viết sẵn trong JavaScript, tên quản trị viên ghi cứng, tải font và biểu tượng từ CDN | Không hiện dữ liệu thật; demo ở phòng lab không có mạng thì vỡ giao diện | Trang mới đọc dữ liệu thật qua `KhachHangController`, dùng bố cục chung, tìm theo tên hoặc số điện thoại, phân trang |
| 17 | Xóa bằng liên kết `GET /customers/delete/{id}`, không hỏi lại | Chỉ cần mở một đường link (kể cả link do người khác gửi) là xóa khách; bấm nhầm là mất. Xóa khách đã có đơn hàng ra lỗi 500 | Xóa bằng POST kèm mã CSRF và hộp xác nhận, chỉ quản trị viên; khách đã có giỏ hàng / đơn hàng thì báo lý do |
| 18 | `@Async` gọi từ chính lớp chứa nó (`this.sendWelcomeEmailAsync`); dự án gốc không bật `@EnableAsync` | Không chạy nền: request đứng chờ 2 giây | Tách sang `GuiEmailService`, chạy trên luồng `xemay-nen-N`, chỉ gửi **sau khi** giao dịch đã lưu (`@TransactionalEventListener(AFTER_COMMIT)`) |
| 19 | Kiểm tra dữ liệu bằng `if` trong Controller | Không kiểm tra định dạng số điện thoại, email, ngày sinh | Form riêng `KhachHangForm` với `@Valid` |
| 20 | Khách đăng ký trên web không có hồ sơ khách hàng | Phần đặt hàng không biết khách là ai (`DON_HANG` cần `MaKhachHang`) | Đăng ký tạo tài khoản và hồ sơ trong **cùng một giao dịch** (số điện thoại trùng thì hủy cả hai); họ tên, số điện thoại đồng bộ hai chiều; xóa tài khoản thì giữ hồ sơ |

## 4. Quy ước mới cho cả nhóm (sau tích hợp)

- Tên bảng, tên cột **theo script chung** (`XE_MAY`, `MaXe`...); mọi thuộc tính Entity ghi rõ `@Column(name = ...)`,
  cột `NVARCHAR` thêm `@Nationalized`, khóa chính kiểu `Integer`.
- Ứng dụng chạy với `ddl-auto=validate`: thêm bảng, cột thì sửa script trước rồi mới viết Entity.
- Trang quản trị đặt dưới `/quan-tri/...`; quyền chi tiết bằng `@PreAuthorize` (bảng quyền trong `huong-dan-tich-hop.md`).
- Số lượng tồn kho chỉ đổi qua `TonKhoService`.

## 5. Lần 1 - Kiểm tra sau khi ghép

| Hạng mục | Kết quả |
|---|---|
| Kiểm thử tự động (65 bài, trong đó 24 bài mới cho xe máy, hãng xe, khách hàng) | 65/65 đạt trên H2 và trên SQL Server 2025 |
| Khởi động ứng dụng với script `QL_CUAHANG_XEMAY.sql` và `ddl-auto=validate` | Thành công: mọi Entity khớp script |
| Đăng nhập 4 loại tài khoản mẫu (`admin`, `nhanvien1`, `khach1`, `khach16` bị khóa) | Đúng quyền, đúng thông báo |
| Thêm, sửa xe, nhập kho, đăng ký khách mới, xóa tài khoản, xóa khách trên dữ liệu của nhóm | Đúng; chi tiết trong `kiem-thu.md` mục 3 |
| Client quản lý kho với dữ liệu của nhóm: 5 client cùng xuất kho xe chỉ còn 2 chiếc | 2 thành công, 8 bị từ chối, tồn kho không âm |
| Kiểm tra tự động độ tương phản và bố cục: 8 trang quản trị mới và trang tổng quan, bề rộng máy tính và điện thoại | 0 lỗi (đã sửa 1 lỗi "thẻ lồng trong thẻ") |

## 6. Lần 2 (22/11/2026)

### 6.1 Đã nhận được

| Phần nộp | Nội dung | Nhận xét khi nhận |
|---|---|---|
| `QL_CUAHANG_XEMAY.sql`, `quanlyxemay.rar`, `QLKH.rar` | Gửi lại | Giống hệt bản đã ghép ở lần 1 |
| `TongHop.zip` | Hai file `.rar` trên, bản nộp trước của thành viên 1, `MotoStore_GiaoDien.zip`, `gio hang/demo4.zip` | |
| `MotoStore_GiaoDien.zip` (thành viên 2) | Trang chủ, danh sách xe (tìm theo tên, lọc giá, còn hàng, sắp xếp, phân trang), chi tiết xe, giỏ hàng; `GiaoDienXeService`; giỏ hàng lưu trong session; `motostore-frontend.css`; bảng kiểm thử giao diện 18 dòng | Làm trên bố cục chung, đúng phân quyền, có xử lý xe chưa có ảnh. Viết theo Entity `XeMay` của bản cũ (trước lần 1); trang chi tiết xe luôn lỗi 500 |
| `demo4` (thành viên 5) | Dự án Spring Boot riêng: Entity `GioHang`, `ChiTietGioHang`, `XeMay`, `KhachHang`; thêm, sửa, xóa trong giỏ, thanh toán | Dùng MySQL và bảng tự đặt, không theo script chung; chưa có đơn hàng, hóa đơn |

### 6.2 Phân công đã đủ chưa (sau lần 2)

| Thành viên | Phân công | Trạng thái |
|---|---|---|
| 1 | Đăng nhập, phân quyền, đồng thời, tích hợp, kiểm thử | Đủ |
| 2 | Trang chủ, danh sách xe, chi tiết xe, giỏ hàng | **Đủ**, đã ghép |
| 3 | Phân tích, use case, thiết kế CSDL, quản lý khách hàng | Thiếu tài liệu phân tích, use case, ERD |
| 4 | CSDL, quản lý xe máy, hãng xe, tìm kiếm/lọc | Đủ (giao diện quản trị do người tích hợp làm ở lần 1) |
| 5 | Giỏ hàng, đặt hàng, hóa đơn, chi tiết hóa đơn | Giỏ hàng đã ghép (viết lại theo bảng của script); đặt hàng do người tích hợp làm (mục 6.4); **còn thiếu hóa đơn, chi tiết hóa đơn, trang quản lý đơn hàng** |

### 6.3 Lỗi phát hiện khi ghép và cách sửa

Giao diện cho khách (thành viên 2). Giao diện (bố cục, màu, chữ, câu thông báo) giữ nguyên như bạn làm; chỉ sửa phần dưới đây.

| # | Vấn đề | Nếu để nguyên | Đã sửa |
|---|---|---|---|
| 21 | Viết theo Entity `XeMay` của bản cũ: 4 cột, mã kiểu `Long`, `soLuongTon`; hãng, loại, mô tả ghi cứng "Đang cập nhật" | Không biên dịch được với Entity chung sau lần 1 | `GiaoDienXeService` đổi theo Entity chung (`maXe`, `soLuong`, hãng, loại, mô tả, ảnh); trang hiện dữ liệu thật |
| 22 | Trang chủ, danh sách, chi tiết lấy cả xe đã ngừng kinh doanh (`findAll`, `findById`) | Khách thấy và cho vào giỏ xe cửa hàng không còn bán | Chỉ lấy xe đang bán (`TrangThai = 1`); mở chi tiết xe ngừng bán ra trang 404 |
| 23 | Tìm kiếm chỉ theo tên xe | Gõ "Honda" hay "Honda Vision" không ra xe nào | Tìm theo tên xe, tên hãng hoặc "hãng + tên", không phân biệt hoa thường |
| 24 | Tiêu đề trang chi tiết `th:text="\|${xe.tenXe()} \| MotoStore\|"`: dấu `\|` nằm trong chuỗi ghép `\|...\|` của Thymeleaf | **Mọi** trang `/xe-may/{id}` lỗi 500 | `th:text="${xe.tenXe()} + ' \| MotoStore'"` |
| 25 | Giỏ hàng lưu trong session (`GioHangSession`) | Đăng xuất là mất giỏ; bảng `GIO_HANG` của script không được dùng | Giỏ lưu trong `GIO_HANG`, `CHI_TIET_GIO_HANG`; giữ giới hạn 20 xe mỗi mẫu và các câu thông báo của bạn |

Giỏ hàng (thành viên 5, `demo4`):

| # | Vấn đề | Nếu để nguyên | Đã sửa |
|---|---|---|---|
| 26 | MySQL, `ddl-auto=update`, bảng và cột tự đặt | Không chạy với SQL Server và script chung | Viết lại Entity `GioHang`, `ChiTietGioHang` theo bảng của script |
| 27 | Luôn dùng giỏ số 1 (`findById(1)`) | Mọi khách dùng chung một giỏ | Mỗi khách một giỏ, lấy theo khách đang đăng nhập |
| 28 | Thêm, xóa, thanh toán bằng `GET` (`/them/{maXe}`, `/xoa/{maCT}`, `/thanhtoan`); sửa, xóa theo mã dòng mà không kiểm tra giỏ của ai | Mở một đường link là giỏ bị đổi; sửa được giỏ của người khác | Mọi thay đổi dùng POST kèm CSRF, chỉ trên giỏ của chính khách |
| 29 | Thanh toán trừ kho không kiểm tra, không khóa, rồi xóa giỏ mà không tạo đơn | Bán vượt số xe đang có (SQL Server chặn bằng CHECK thì ra lỗi 500); không còn dấu vết đơn nào | Đặt hàng mới, mục 6.4 |
| 30 | Controller trả về view `giohang` nhưng file tên `GioHang.html` | Chạy bằng file jar báo không tìm thấy trang | Dùng trang giỏ hàng của thành viên 2 |
| 31 | `application.properties` có mật khẩu MySQL của máy cá nhân | Đẩy lên Git là lộ mật khẩu | Không chép cấu hình này; mật khẩu để trong `application-local.properties` (đã nằm trong `.gitignore`) |

Do người tích hợp phát hiện khi ghép:

| # | Vấn đề | Nếu để nguyên | Đã sửa |
|---|---|---|---|
| 32 | `sp_XacNhanDonHang` trừ kho lúc xác nhận, trong khi đặt hàng trên web đã trừ kho | Đơn đặt trên web bị trừ kho **hai lần** | Phần trừ kho trong procedure đặt trong chú thích `[Tich hop]`; đơn mẫu trừ kho lúc chèn nên tồn kho mẫu sau script không đổi (Monster vẫn còn 2) |
| 33 | Bản đầu của giỏ hàng khóa dòng giỏ rồi mới tạo giỏ cho khách mới | Trên SQL Server, khách mới thêm xe vào giỏ lần đầu thì trang **treo mãi** (trên H2 không bị nên kiểm thử không bắt được) | Tạo giỏ trước rồi mới khóa; chi tiết ở `thiet-ke-dong-thoi-va-mang.md` mục 3.5 |

### 6.4 Đặt hàng

Đặt hàng thuộc phần của thành viên 5 nhưng chưa có trong bài nộp; người tích hợp làm vì đây là chỗ dễ sai nhất về đồng thời
(hai khách cùng mua chiếc xe cuối cùng). `DatHangService.datHang` làm trong **một** giao dịch: khóa giỏ, khóa và trừ kho từng
xe theo mã tăng dần, tạo `DON_HANG` (trạng thái `CHO_XAC_NHAN`) và `CHI_TIET_DON_HANG` theo giá bán hiện tại, làm trống giỏ.
Thiếu hàng hoặc xe đã ngừng bán thì không tạo đơn, không trừ xe nào. Trang giỏ hàng của thành viên 2 có khung "Tạm tính"; ô
địa chỉ giao xe (điền sẵn địa chỉ trong hồ sơ), ô ghi chú và nút "Đặt hàng" được thêm vào đúng khung đó, thay cho dòng
"chưa ghép chức năng tạo đơn hàng".

Kho bị trừ **ngay khi khách đặt**, theo cách làm trong demo của thành viên 5: người đặt trước giữ được xe, không cần chờ trang
xác nhận đơn (chưa có). Vì vậy procedure xác nhận đơn không trừ kho nữa (lỗi 32), và hủy đơn phải trả hàng bằng
`TonKhoService.nhapKho`.

### 6.5 Quyết định khi ghép

| Vấn đề | Quyết định | Lý do |
|---|---|---|
| Tên cửa hàng | **MotoStore**, theo giao diện của thành viên 2 | Giao diện của bạn dùng tên này; tên chỉ đổi ở một chỗ (`messages.properties`) |
| `header.html`, `layout.html` | Dùng header của bạn; giữ layout chung, chỉ thêm dòng nạp `motostore-frontend.css` | Layout của bạn làm từ bản cũ, thiếu dòng nạp font Be Vietnam Pro |
| Phần không chép | `GioHangSession`, project mẫu `02_Project_Mau_Ghep_San` cùng 2 script SQL tạm, mẫu adapter, `HUONG_DAN_ANH.md` | Đã có bản chung tương ứng; file `.md` đặt trong `static/` sẽ bị phục vụ công khai (quy ước ảnh ghi trong `huong-dan-tich-hop.md`) |
| URL giỏ hàng | `/gio-hang`, chỉ vai trò khách hàng; đặt hàng `POST /gio-hang/dat-hang` | Theo `SecurityConfig` của thành viên 2 |

### 6.6 Kiểm tra sau khi ghép

| Hạng mục | Kết quả |
|---|---|
| Kiểm thử tự động (73 bài, trong đó 8 bài mới cho trang khách, giỏ hàng, đặt hàng) | 73/73 đạt trên H2 và trên SQL Server 2025 |
| Khởi động với script `QL_CUAHANG_XEMAY.sql` và `ddl-auto=validate` | Thành công: 4 Entity mới (`GioHang`, `ChiTietGioHang`, `DonHang`, `ChiTietDonHang`) khớp script |
| Bảng kiểm thử giao diện GD01-GD18 của thành viên 2 | 18/18 đạt (`kiem-thu.md` mục 6.2) |
| Trên trình duyệt: giỏ hàng, đặt hàng, hai khách cùng đặt chiếc xe cuối, xác nhận đơn web bằng procedure | Đạt (`kiem-thu.md` mục 6.1) |
| Ảnh chụp màn hình | Chụp lại 23 ảnh cũ (tên cửa hàng và header mới), thêm 7 ảnh trang khách, giỏ hàng, đặt hàng (`docs/hinh-anh/`) |

## 7. Việc còn lại (sau lần 2)

| Thành viên | Việc | Ghi chú |
|---|---|---|
| 5 | Hóa đơn, chi tiết hóa đơn; trang quản lý đơn hàng cho nhân viên (xem, xác nhận, hủy đơn); trang "Đơn hàng của tôi" cho khách | Bảng và đơn hàng đã có; hướng dẫn ở `huong-dan-tich-hop.md` mục 5 |
| 2 | Ảnh xe thật, ghi nguồn ảnh; cột "Sản phẩm" ở chân trang | Hiện mọi xe đều hiện "Chưa có ảnh thực tế" |
| 3 | Bảng chức năng / use case, ERD; đọc lại phần khách hàng đã viết lại để tự trình bày | |
| 4 | Xem lại các trang quản trị xe máy, hãng xe (người tích hợp làm tạm) | Có thể làm thêm trang thống kê theo hãng (truy vấn mẫu đã có trong script) |
| Cả nhóm | Chốt bảng quyền nhân viên / quản trị viên; mỗi người clone repo và tự commit phần của mình | Rubric 6.4 chấm lịch sử Git từng người |
