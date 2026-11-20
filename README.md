# Web bán xe máy - Đồ án môn Công nghệ Java

Ứng dụng web bán xe máy viết bằng **Spring Boot**, giao diện **Thymeleaf + Bootstrap**, cơ sở dữ liệu **SQL Server**.
Kèm theo là một **chương trình client quản lý kho** chạy tách biệt, trao đổi dữ liệu với ứng dụng web qua **Socket**.

> Phiên bản hiện tại gồm phần **đăng nhập, phân quyền, xử lý đồng thời, lập trình mạng** và **bộ khung chung**
> (cấu trúc dự án, giao diện dùng chung, xử lý lỗi, kiểm thử) để các thành viên khác gắn chức năng của mình vào.
> Cách gắn: xem [docs/huong-dan-tich-hop.md](docs/huong-dan-tich-hop.md).

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
│       ├── entity/              Ánh xạ bảng: VaiTro, TaiKhoan, LichSuDangNhap, XeMay
│       ├── exception/           Ngoại lệ nghiệp vụ + GlobalExceptionHandler (trang lỗi thân thiện)
│       ├── network/             Socket server, giao thức, bộ xử lý lệnh
│       ├── repository/          Spring Data JPA
│       ├── scheduler/           Tác vụ định kỳ (@Scheduled)
│       ├── security/            Đăng nhập, chống dò mật khẩu, xử lý từ chối truy cập
│       ├── service/             Nghiệp vụ: TaiKhoanService, TonKhoService, LichSuDangNhapService
│       └── util/                Hàm tiện ích cho phân trang, sắp xếp
│   └── src/main/resources/
│       ├── templates/           Giao diện Thymeleaf; fragments/ là bố cục, menu, footer dùng chung
│       ├── static/css/theme.css Cài đặt quy chuẩn giao diện trên nền Bootstrap
│       ├── static/fonts/        Font Be Vietnam Pro (lưu sẵn, chạy không cần Internet)
│       ├── static/images/       Ảnh (nguồn, giấy phép: docs/nguon-anh-va-font.md)
│       └── messages.properties  Tên cửa hàng, hotline, địa chỉ (dữ liệu tạm, sửa tại đây)
├── client/                      Chương trình client quản lý kho (Java thuần, java.net.Socket)
├── database/                    Script SQL Server (tạo bảng + dữ liệu mẫu)
└── docs/                        Quy chuẩn giao diện, thiết kế, kiểm thử, hướng dẫn tích hợp, ảnh chụp màn hình
```

## Cài đặt và chạy

### Bước 1 - Tạo cơ sở dữ liệu

Mở **SQL Server Management Studio**, lần lượt mở và bấm **Execute** hai file:

1. `database/01_tai_khoan_phan_quyen.sql` - tạo CSDL `QuanLyXeMay` và 3 bảng `vai_tro`, `tai_khoan`, `lich_su_dang_nhap` kèm dữ liệu mẫu.
2. `database/02_xe_may_tam_thoi.sql` - bảng `xe_may` tạm thời (20 xe) để chạy thử phần tồn kho.

Hoặc dùng dòng lệnh (thay `<mật khẩu>`):

```bash
sqlcmd -S localhost -U sa -P "<mật khẩu>" -C -f 65001 -i database/01_tai_khoan_phan_quyen.sql
```

```bash
sqlcmd -S localhost -U sa -P "<mật khẩu>" -C -f 65001 -i database/02_xe_may_tam_thoi.sql
```

Hai script chạy lại bao nhiêu lần cũng được; mỗi lần chạy dữ liệu trở về như ban đầu.

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

Chạy thử nhanh **không cần SQL Server** (dữ liệu nằm trong bộ nhớ, mất khi tắt):

```bash
./mvnw -pl web spring-boot:run -Dspring-boot.run.profiles=h2
```

### Tài khoản dùng thử (mật khẩu đều là `123456`)

| Tên đăng nhập | Vai trò | Được làm gì |
|---|---|---|
| `admin` | Quản trị viên | Mọi chức năng, gồm quản lý tài khoản và xem lịch sử đăng nhập |
| `nhanvien` | Nhân viên | Khu vực quản trị (trừ quản lý tài khoản), dùng client quản lý kho |
| `khachhang` | Khách hàng | Trang của khách hàng, không vào được khu vực quản trị |
| `tathithu` | Khách hàng | Tài khoản **đang bị khóa** - dùng để demo trường hợp bị chặn đăng nhập |

### Bước 4 - Chạy chương trình client quản lý kho

```bash
./mvnw -pl client package
```

```bash
java -jar client/target/xemay-client.jar localhost 9090
```

Đăng nhập bằng `nhanvien` / `123456` rồi chọn chức năng trong menu (xem danh sách xe, tìm xe, xem tồn kho, nhập kho, xuất kho).
Muốn demo **3 client cùng lúc**: mở 3 cửa sổ dòng lệnh và chạy lệnh trên ở cả 3. Client ở máy khác trong phòng lab
thì thay `localhost` bằng địa chỉ IP của máy chạy ứng dụng web.

Trên **Windows**, gõ `chcp 65001` trước khi chạy client để hiển thị đúng tiếng Việt.

**Chương trình kiểm thử nhiều client** (5 client cùng xuất kho một xe; tham số: máy chủ, cổng, tài khoản, mật khẩu, mã xe, số client, số lần mỗi client):

```bash
java -cp client/target/xemay-client.jar com.xemay.client.KiemThuNhieuClient localhost 9090 nhanvien 123456 1 5 4
```

### Bước 5 - Chạy kiểm thử tự động

```bash
./mvnw -pl web test
```

41 bài kiểm thử chạy trên H2 trong bộ nhớ, khoảng 30 giây, không cần SQL Server. Muốn chạy trên SQL Server thật,
tạo một CSDL **riêng** cho kiểm thử (các bảng trong đó sẽ bị xóa và tạo lại) rồi đặt biến môi trường. Ví dụ trên PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:1433;databaseName=QuanLyXeMay_KiemThu;encrypt=true;trustServerCertificate=true"
$env:SPRING_DATASOURCE_USERNAME="sa"
$env:SPRING_DATASOURCE_PASSWORD="<mật khẩu>"
.\mvnw -pl web test
```

Kết quả kiểm thử và kịch bản kiểm thử: [docs/kiem-thu.md](docs/kiem-thu.md).

## Giao diện

- Phong cách **đại lý xe máy chính hãng**: nền trắng, một màu đỏ đậm làm điểm nhấn, font **Be Vietnam Pro** (font do người Việt
  thiết kế, dấu tiếng Việt chuẩn). Toàn bộ quy chuẩn nằm trong [docs/quy-chuan-giao-dien.md](docs/quy-chuan-giao-dien.md).
- Vẫn là **Thymeleaf + Bootstrap** đúng yêu cầu đề bài; giao diện mới chỉ ghi đè biến CSS của Bootstrap trong `static/css/theme.css`.
- Font, Bootstrap, biểu tượng đều lưu trong dự án: demo ở phòng lab **không cần Internet**.
- Tên "Web Xe Máy", hotline, địa chỉ là **dữ liệu tạm**, đổi trong `messages.properties`.
- Ảnh trang đăng nhập, đăng ký là ảnh tạm có giấy phép tự do từ Wikimedia Commons, đã ghi nguồn ngay trên ảnh và trong
  [docs/nguon-anh-va-font.md](docs/nguon-anh-va-font.md).
- Đã kiểm tra tự động độ tương phản màu chữ và bố cục: các trang thuộc phần đăng nhập, phân quyền
  không còn lỗi nào.

## Chức năng đã làm

| Chức năng | Ở đâu | Hạng mục rubric |
|---|---|---|
| Đăng nhập bằng tài khoản trong CSDL, mật khẩu băm BCrypt | `SecurityConfig`, `TaiKhoanDetailsService` | 2.2, 2.3 |
| Đăng ký tài khoản khách hàng, kiểm tra dữ liệu nhập, báo lỗi cạnh từng ô | `XacThucController`, `DangKyForm` | 2.2, 3.3 |
| Tạm khóa 15 phút khi nhập sai mật khẩu 5 lần (áp dụng cho cả web và client) | `BoDemDangNhapSai`, `TheoDoiDangNhap` | 5.2 |
| Phân quyền 3 vai trò theo URL, theo phương thức (`@PreAuthorize`), menu theo vai trò | `SecurityConfig`, `TaiKhoanService`, `fragments/header.html` | 3.5 |
| Quản lý tài khoản: tìm kiếm, lọc, phân trang, sắp xếp, thêm, sửa, khóa, xóa, đặt lại mật khẩu | `QuanLyTaiKhoanController` | 3.3, 3.4, 4.4, 4.5 |
| Thông tin cá nhân, đổi mật khẩu | `TaiKhoanCaNhanController` | 2.4 |
| Nhật ký đăng nhập ghi bằng tác vụ nền `@Async` | `LichSuDangNhapService` | 5.1 |
| Tác vụ định kỳ `@Scheduled`: quét xe sắp hết hàng, dọn dữ liệu cũ | `TacVuDinhKy` | 5.1 |
| Tồn kho an toàn khi nhiều giao dịch cùng lúc (khóa dòng JPA, chống deadlock) | `TonKhoService` | 5.2, 4.5 |
| Socket server phục vụ nhiều client song song + client tách biệt | `network/`, `client/` | 5.3, 5.4 |
| Trang lỗi thân thiện (403, 404, 500), xử lý ngoại lệ chung | `GlobalExceptionHandler`, `error.html` | 2.5 |
| Bố cục dùng chung, phân trang, sắp xếp, hộp xác nhận trước khi xóa | `templates/fragments/` | 3.1, 3.2, 3.3 |
| Quy chuẩn giao diện chung cho cả nhóm, theme Bootstrap, font tiếng Việt nhúng sẵn | `docs/quy-chuan-giao-dien.md`, `static/css/theme.css` | 3.1, 3.2, 3.5 |

Thiết kế phần đồng thời và phần mạng (dùng cho mục 1.5 của báo cáo): [docs/thiet-ke-dong-thoi-va-mang.md](docs/thiet-ke-dong-thoi-va-mang.md).
Ảnh chụp các màn hình chính (máy tính và điện thoại): [docs/hinh-anh/](docs/hinh-anh/).
