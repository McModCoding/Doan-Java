# Web bán xe máy - Đồ án môn Công nghệ Java

Ứng dụng web bán xe máy viết bằng **Spring Boot**, giao diện **Thymeleaf + Bootstrap**, cơ sở dữ liệu **SQL Server**.
Kèm theo là một **chương trình client quản lý kho** chạy tách biệt, trao đổi dữ liệu với ứng dụng web qua **Socket**.

> Bản hiện tại đã **ghép phần của các thành viên** vào một dự án và một CSDL chung: đăng nhập, phân quyền, xử lý đồng thời,
> lập trình mạng; quản lý xe máy, hãng xe, tìm kiếm và lọc; quản lý khách hàng; script CSDL của nhóm.
> Còn thiếu: giỏ hàng, đặt hàng, hóa đơn và các trang cho khách (trang chủ, danh sách xe, chi tiết xe).
> Chi tiết từng phần, lỗi đã sửa khi ghép: [docs/bao-cao-tich-hop.md](docs/bao-cao-tich-hop.md).
> Cách làm tiếp phần còn thiếu: [docs/huong-dan-tich-hop.md](docs/huong-dan-tich-hop.md).

## Công nghệ và phiên bản

| Thành phần | Phiên bản |
|---|---|
| JDK | **17 trở lên** (đã chạy thử trên JDK 21 và JDK 25) |
| Spring Boot | 3.5.16 (Spring MVC, Spring Security 6.5, Spring Data JPA / Hibernate 6.6) |
| Giao diện | Thymeleaf 3.1, Bootstrap 5.3.8, Bootstrap Icons (đóng gói sẵn bằng WebJars, **demo không cần Internet**) |
| Cơ sở dữ liệu | SQL Server 2019 trở lên (đã chạy thử trên SQL Server 2022 và SQL Server 2025) |
| Build | Maven 3.9, có sẵn Maven Wrapper (`mvnw`) nên máy chưa cài Maven vẫn chạy được |

## Cấu trúc thư mục

```
web-xe-may/
├── web/                         Ứng dụng web Spring Boot (cổng 8080) + Socket server (cổng 9090)
│   └── src/main/java/com/xemay/
│       ├── config/              Cấu hình bảo mật (SecurityConfig), đa luồng, dữ liệu khởi tạo
│       ├── controller/          Controller cho người dùng; controller/quantri/ cho khu vực quản trị
│       ├── dto/                 Dữ liệu của các form (có @Valid)
│       ├── entity/              Ánh xạ bảng: VaiTro, TaiKhoan, LichSuDangNhap, HangXe, XeMay, KhachHang
│       ├── exception/           Ngoại lệ nghiệp vụ + GlobalExceptionHandler (trang lỗi thân thiện)
│       ├── network/             Socket server, giao thức, bộ xử lý lệnh
│       ├── repository/          Spring Data JPA
│       ├── scheduler/           Tác vụ định kỳ (@Scheduled)
│       ├── security/            Đăng nhập, chống dò mật khẩu, xử lý từ chối truy cập
│       ├── service/             Nghiệp vụ: tài khoản, xe máy, hãng xe, khách hàng, tồn kho, email nền
│       └── util/                Hàm tiện ích cho phân trang, sắp xếp
│   └── src/main/resources/
│       ├── templates/           Giao diện Thymeleaf; fragments/ là bố cục, menu, footer dùng chung
│       ├── static/css/theme.css Cài đặt quy chuẩn giao diện trên nền Bootstrap
│       ├── static/fonts/        Font Be Vietnam Pro (lưu sẵn, chạy không cần Internet)
│       ├── static/images/       Ảnh (nguồn, giấy phép: docs/nguon-anh-va-font.md)
│       └── messages.properties  Tên cửa hàng, hotline, địa chỉ (dữ liệu tạm, sửa tại đây)
├── client/                      Chương trình client quản lý kho (Java thuần, java.net.Socket)
├── database/QL_CUAHANG_XEMAY.sql  Script SQL Server: 12 bảng, dữ liệu mẫu, procedure xác nhận đơn hàng
└── docs/                        Quy chuẩn giao diện, báo cáo tích hợp, thiết kế, kiểm thử, hướng dẫn, ảnh chụp màn hình
```

## Cài đặt và chạy

### Bước 1 - Tạo cơ sở dữ liệu

Mở **SQL Server Management Studio**, mở file `database/QL_CUAHANG_XEMAY.sql` rồi bấm **Execute** (F5).
Script tạo CSDL `QL_CUAHANG_XEMAY`, 12 bảng, dữ liệu mẫu (mỗi bảng chính từ 20 dòng) và procedure `sp_XacNhanDonHang`.
Chạy lại bao nhiêu lần cũng được; mỗi lần chạy dữ liệu trở về như ban đầu.

Hoặc dùng dòng lệnh (thay `<mật khẩu>`):

```bash
sqlcmd -S localhost -U sa -P "<mật khẩu>" -C -f 65001 -i database/QL_CUAHANG_XEMAY.sql
```

Ứng dụng chạy với `spring.jpa.hibernate.ddl-auto=validate`: khi khởi động, Hibernate đối chiếu Entity với bảng thật.
Nếu log báo `Schema-validation: missing table ...` nghĩa là chưa chạy script (hoặc đang dùng script cũ) - chạy lại Bước 1.

### Bước 2 - Khai báo tài khoản SQL Server của máy bạn

Sao chép `web/src/main/resources/application-local.properties.example` thành `application-local.properties` (cùng thư mục)
rồi sửa tên đăng nhập, mật khẩu. File này nằm trong `.gitignore` nên mật khẩu của bạn không bị đẩy lên Git.

Nếu không kết nối được SQL Server, kiểm tra 3 điều hay gặp nhất:

- **SQL Server Configuration Manager** > *SQL Server Network Configuration* > *Protocols*: bật **TCP/IP**, đặt cổng **1433**, khởi động lại dịch vụ SQL Server.
- SQL Server phải bật chế độ đăng nhập **SQL Server and Windows Authentication** (để dùng tài khoản `sa`).
- Bản **SQL Server Express** thường có tên instance `SQLEXPRESS`: sửa URL như ví dụ trong file `.example`.

### Bước 3 - Chạy ứng dụng web

- **Bằng IDE** (IntelliJ IDEA, Eclipse, NetBeans): mở thư mục gốc (file `pom.xml`), chạy lớp `com.xemay.XeMayApplication`.
- **Bằng dòng lệnh** tại thư mục gốc (Windows gõ `mvnw` thay cho `./mvnw`):

```bash
./mvnw -pl web spring-boot:run
```

Mở trình duyệt: <http://localhost:8080>. Khi khởi động thành công, log có dòng
`Socket server quản lý kho đang lắng nghe ở cổng 9090`.

Chạy thử nhanh **không cần SQL Server** (dữ liệu nằm trong bộ nhớ, mất khi tắt; tài khoản `admin`, `nhanvien`, `khachhang`):

```bash
./mvnw -pl web spring-boot:run -Dspring-boot.run.profiles=h2
```

### Tài khoản dùng thử trong script (mật khẩu đều là `123456`)

| Tên đăng nhập | Vai trò | Được làm gì |
|---|---|---|
| `admin` | Quản trị viên | Mọi chức năng: xe máy, hãng xe, khách hàng, tài khoản, lịch sử đăng nhập |
| `nhanvien1` ... `nhanvien4` | Nhân viên | Quản lý xe máy, hãng xe, khách hàng, nhập kho; không ngừng bán xe, không xóa khách, không quản lý tài khoản |
| `khach1` ... `khach20` | Khách hàng | Trang của khách hàng, không vào được khu vực quản trị |
| `khach16` | Khách hàng | Tài khoản **đang bị khóa** - dùng để demo trường hợp bị chặn đăng nhập |

### Bước 4 - Chạy chương trình client quản lý kho

```bash
./mvnw -pl client package
```

```bash
java -jar client/target/xemay-client.jar localhost 9090
```

Đăng nhập bằng `nhanvien1` / `123456` rồi chọn chức năng trong menu (xem danh sách xe, tìm xe theo tên hoặc hãng,
xem tồn kho, nhập kho, xuất kho). Muốn demo **3 client cùng lúc**: mở 3 cửa sổ dòng lệnh và chạy lệnh trên ở cả 3.
Client ở máy khác trong phòng lab thì thay `localhost` bằng địa chỉ IP của máy chạy ứng dụng web.

Trên **Windows**, gõ `chcp 65001` trước khi chạy client để hiển thị đúng tiếng Việt.

**Chương trình kiểm thử nhiều client**: 5 client cùng xuất kho xe số 15 (Ducati Monster, trong script còn 2 chiếc), mỗi client 2 lần;
kết quả đúng là chỉ 2 lần thành công, 8 lần bị từ chối vì hết hàng. Chương trình tự nhập lại số đã xuất để trả kho về như cũ.
Tham số: máy chủ, cổng, tài khoản, mật khẩu, mã xe, số client, số lần mỗi client.

```bash
java -cp client/target/xemay-client.jar com.xemay.client.KiemThuNhieuClient localhost 9090 nhanvien1 123456 15 5 2
```

### Bước 5 - Chạy kiểm thử tự động

```bash
./mvnw -pl web test
```

65 bài kiểm thử chạy trên H2 trong bộ nhớ, khoảng 30 giây, không cần SQL Server. Muốn chạy trên SQL Server thật,
tạo một CSDL **riêng** cho kiểm thử (các bảng trong đó sẽ bị xóa và tạo lại) rồi đặt biến môi trường. Ví dụ trên PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:1433;databaseName=QL_CUAHANG_XEMAY_KiemThu;encrypt=true;trustServerCertificate=true"
$env:SPRING_DATASOURCE_USERNAME="sa"
$env:SPRING_DATASOURCE_PASSWORD="<mật khẩu>"
.\mvnw -pl web test
```

Kết quả kiểm thử và kịch bản kiểm thử: [docs/kiem-thu.md](docs/kiem-thu.md).

## Giao diện

- Phong cách **đại lý xe máy chính hãng**: nền trắng, một màu đỏ đậm làm điểm nhấn, font **Be Vietnam Pro** (font do người Việt
  thiết kế, dấu tiếng Việt chuẩn). Toàn bộ quy chuẩn nằm trong [docs/quy-chuan-giao-dien.md](docs/quy-chuan-giao-dien.md).
- Vẫn là **Thymeleaf + Bootstrap** đúng yêu cầu đề bài; giao diện chỉ ghi đè biến CSS của Bootstrap trong `static/css/theme.css`.
- Font, Bootstrap, biểu tượng đều lưu trong dự án: demo ở phòng lab **không cần Internet**.
- Tên "Web Xe Máy", hotline, địa chỉ là **dữ liệu tạm**, đổi trong `messages.properties`.
- Ảnh trang đăng nhập, đăng ký là ảnh tạm có giấy phép tự do từ Wikimedia Commons, đã ghi nguồn ngay trên ảnh và trong
  [docs/nguon-anh-va-font.md](docs/nguon-anh-va-font.md).

## Phân công và trạng thái

| Thành viên | Phân công | Trạng thái trong bản này |
|---|---|---|
| 1 | Đăng nhập, phân quyền, xử lý đồng thời, tích hợp hệ thống, kiểm thử (+ lập trình mạng) | Xong, đã ghép phần của các bạn 3, 4 |
| 2 | Giao diện trang chủ, danh sách xe, chi tiết xe, giỏ hàng | **Chưa nhận được** |
| 3 | Phân tích, use case, thiết kế CSDL, quản lý khách hàng | Đã ghép quản lý khách hàng; chưa có tài liệu use case |
| 4 | CSDL SQL Server, quản lý xe máy, hãng xe, tìm kiếm/lọc | Đã ghép (giao diện quản trị do người tích hợp làm vì file nộp rỗng) |
| 5 | Giỏ hàng, đặt hàng, hóa đơn, chi tiết hóa đơn | Mới có bảng và procedure trong script, **chưa có code Java** |

## Chức năng đã làm

| Chức năng | Ở đâu | Phần của | Hạng mục rubric |
|---|---|---|---|
| Đăng nhập bằng tài khoản trong CSDL, mật khẩu băm BCrypt | `SecurityConfig`, `TaiKhoanDetailsService` | 1 | 2.2, 2.3 |
| Đăng ký khách hàng (tạo luôn hồ sơ khách hàng), kiểm tra dữ liệu, báo lỗi cạnh từng ô | `XacThucController`, `TaiKhoanService` | 1, 3 | 2.2, 3.3 |
| Tạm khóa 15 phút khi nhập sai mật khẩu 5 lần (cả web và client) | `BoDemDangNhapSai`, `TheoDoiDangNhap` | 1 | 5.2 |
| Phân quyền 3 vai trò theo URL, theo phương thức (`@PreAuthorize`), menu và nút theo vai trò | `SecurityConfig`, các Service, template | 1 | 3.5 |
| Quản lý tài khoản: tìm kiếm, lọc, phân trang, sắp xếp, thêm, sửa, khóa, xóa | `QuanLyTaiKhoanController` | 1 | 3.3, 3.4, 4.4 |
| Quản lý xe máy: tìm theo tên, lọc theo hãng, loại, giá, trạng thái; phân trang, sắp xếp; thêm, sửa, ngừng kinh doanh, nhập kho | `XeMayController`, `XeMayService` | 4 (+1 giao diện) | 2.4, 3.3, 4.4 |
| Quản lý hãng xe: tìm, thêm, sửa, ngừng hoạt động, số mẫu xe mỗi hãng | `HangXeController`, `HangXeService` | 4 (+1 giao diện) | 2.4, 3.3 |
| Quản lý khách hàng: tìm theo tên hoặc số điện thoại, thêm khách tại cửa hàng, sửa, xóa | `KhachHangController`, `KhachHangService` | 3 | 2.4, 3.3, 4.4 |
| Email chào mừng khách mới gửi trên luồng nền, chỉ sau khi giao dịch đã lưu | `GuiEmailService` | 3 (+1 sửa) | 5.1 |
| Nhật ký đăng nhập ghi bằng tác vụ nền `@Async` | `LichSuDangNhapService` | 1 | 5.1 |
| Tác vụ định kỳ `@Scheduled`: quét xe sắp hết hàng, dọn dữ liệu cũ | `TacVuDinhKy` | 1 | 5.1 |
| Tồn kho an toàn khi nhiều giao dịch cùng lúc (khóa dòng JPA, chống deadlock, sửa xe không ghi đè tồn kho) | `TonKhoService`, `XeMay` (`@DynamicUpdate`) | 1 | 5.2, 4.5 |
| Socket server phục vụ nhiều client song song + client tách biệt | `network/`, `client/` | 1 | 5.3, 5.4 |
| Trang lỗi thân thiện (403, 404, 500), xử lý ngoại lệ chung | `GlobalExceptionHandler`, `error.html` | 1 | 2.5 |
| CSDL 12 bảng, quan hệ một-nhiều, dữ liệu mẫu, procedure xác nhận đơn | `database/QL_CUAHANG_XEMAY.sql` | 3, 4 (+1 bổ sung) | 4.1, 4.2, 4.3 |
| Quy chuẩn giao diện chung, theme Bootstrap, font tiếng Việt nhúng sẵn | `docs/quy-chuan-giao-dien.md`, `static/css/theme.css` | 1 | 3.1, 3.2 |

Thiết kế phần đồng thời và phần mạng (dùng cho mục 1.5 của báo cáo): [docs/thiet-ke-dong-thoi-va-mang.md](docs/thiet-ke-dong-thoi-va-mang.md).
Ảnh chụp các màn hình chính (máy tính và điện thoại): [docs/hinh-anh/](docs/hinh-anh/).
