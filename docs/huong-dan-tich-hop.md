# Hướng dẫn tích hợp cho các thành viên

Tài liệu này dành cho các thành viên làm giao diện, khách hàng, xe máy - hãng xe, giỏ hàng - hóa đơn.
Mọi người làm trên **cùng một dự án** này (không tạo dự án riêng), theo các quy ước dưới đây để khi gộp không phải sửa lại.

## 1. Quy ước chung

| Nội dung | Quy ước |
|---|---|
| Package | `com.xemay.controller`, `.service`, `.repository`, `.entity`, `.dto`, `.config`. Trang quản trị đặt trong `controller/quantri` |
| Tên lớp, biến | Tiếng Việt không dấu, kiểu camelCase (`XeMayService`, `timTheoHang`) |
| Bảng, cột | chữ thường, gạch dưới (`hang_xe`, `ngay_dat`); chuỗi dùng `NVARCHAR` để lưu tiếng Việt |
| Nghiệp vụ | Viết trong **Service**, không viết trong Controller. Thao tác nhiều bước có `@Transactional` |
| URL người dùng | `/xe-may`, `/xe-may/{id}`, `/hang-xe`, `/gio-hang`, `/dat-hang`, `/don-hang` |
| URL quản trị | `/quan-tri/xe-may`, `/quan-tri/hang-xe`, `/quan-tri/khach-hang`, `/quan-tri/hoa-don` |
| Thêm / sửa / xóa | `GET .../them`, `POST .../them`, `GET .../{id}/sua`, `POST .../{id}/sua`, `POST .../{id}/xoa` (xóa luôn dùng POST) |

## 2. Phân quyền: trang mới của bạn ai được vào?

Quy tắc nằm trong `config/SecurityConfig.java`. Hiện tại:

| URL | Ai được vào |
|---|---|
| `/`, `/dang-nhap`, `/dang-ky`, tài nguyên tĩnh | Tất cả |
| `GET /xe-may/**`, `GET /hang-xe/**` | Tất cả (khách chưa đăng nhập vẫn xem được xe) |
| `/quan-tri/tai-khoan/**`, `/quan-tri/lich-su-dang-nhap/**` | Chỉ quản trị viên |
| `/quan-tri/**` | Quản trị viên và nhân viên |
| **Mọi URL khác** (`/gio-hang`, `/dat-hang`, `/don-hang`, `/tai-khoan`...) | Phải đăng nhập |

Nghĩa là: đặt trang quản trị dưới `/quan-tri/...`, trang của khách dưới các URL còn lại, thì **không cần sửa gì** trong `SecurityConfig`.
Chỉ khi muốn một trang mới được xem công khai thì mới thêm đường dẫn của nó vào nhóm `permitAll()`.

Muốn chặn chặt hơn ở tầng Service (ví dụ chỉ quản trị viên được xóa xe), thêm annotation lên phương thức:

```java
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public void xoa(Long id) { ... }
```

## 3. Lấy người dùng đang đăng nhập

Trong Controller:

```java
@GetMapping("/gio-hang")
public String gioHang(@AuthenticationPrincipal TaiKhoanDetails nguoiDung, Model model) {
    Long taiKhoanId = nguoiDung.getId();          // mã trong bảng tai_khoan
    String hoTen = nguoiDung.getHoTen();
    boolean laNhanSu = nguoiDung.laNhanSu();      // quản trị viên hoặc nhân viên
    ...
}
```

Trong template (cần khai báo `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` ở thẻ `<html>`):

```html
<span sec:authentication="principal.hoTen">Họ tên</span>
<a sec:authorize="hasRole('KHACHHANG')" th:href="@{/gio-hang}">Giỏ hàng</a>
<a sec:authorize="hasAnyRole('ADMIN', 'NHANVIEN')" th:href="@{/quan-tri/xe-may}">Quản lý xe</a>
```

## 4. Giao diện: theo quy chuẩn và bố cục chung

**Đọc `docs/quy-chuan-giao-dien.md` trước khi làm bất kỳ trang nào.** File này quy định màu, font, khoảng cách, các thành phần
và danh sách những thứ không được làm. Mọi giá trị trong đó đã được cài sẵn trong `static/css/theme.css`, nên khi làm trang
chỉ cần dùng lớp có sẵn, không tự đặt màu, font hay `style=""`.

Mỗi trang chỉ viết phần nội dung riêng, đặt trong **một** thẻ `<section>`; header, menu, footer, thông báo do bố cục chung lo:

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: trang(~{::title}, ~{::section})}">
<head><title th:text="'Danh sách xe | ' + #{cuahang.ten}">Danh sách xe</title></head>
<body>
<section>
    <div class="dau-trang">
        <div>
            <h1>Danh sách xe</h1>
            <p class="dau-trang-mo-ta">Một câu mô tả ngắn, cụ thể.</p>
        </div>
    </div>
    <!-- Trang quản trị: thêm dòng dưới để có thanh mục của khu vực quản trị -->
    <div th:replace="~{fragments/quan-tri :: menu}"></div>
    ... nội dung trang ...
</section>
</body>
</html>
```

Các lớp và mảnh dùng lại được:

| Cần | Dùng |
|---|---|
| Tiêu đề trang + mô tả + nút bên phải | `.dau-trang`, `.dau-trang-mo-ta` |
| Thẻ xe trong danh sách | `.the-xe` gồm `.the-xe-anh` (ảnh 4:3), `.the-xe-ten`, `.the-xe-phu`, `.gia` (xem mục Thành phần của quy chuẩn giao diện) |
| Giá bán | `.gia` (đỏ, đậm, chữ số thẳng cột) |
| Khung nội dung có tiêu đề | `.card` + `.card-header` (có `h2`) + `.card-body` |
| Bảng | bọc trong `.bang-khung`, bảng `.table .table-hover .align-middle`; cột số thêm `.so` |
| Nhãn trạng thái | `.nhan` + `nhan-thanh-cong` / `nhan-canh-bao` / `nhan-loi` / `nhan-dam` / `nhan-vien` |
| Nút | `btn btn-primary` (một nút chính mỗi màn hình), `btn btn-outline-secondary` (nút phụ), `btn btn-icon` (thao tác trong bảng; thêm `nguy-hiem` cho nút xóa) |
| Số liệu tổng quan | `.so-lieu`, `.so-lieu-nhan`, `.so-lieu-gia-tri`, `.so-lieu-phu` |
| Thông báo sau khi lưu | Controller: `chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã lưu ...")` - bố cục chung tự hiển thị |
| Thông báo lỗi trên trang | `model.addAttribute("thongBaoLoi", "...")` |
| Hộp xác nhận trước khi xóa | thêm `th:data-xac-nhan="|Xóa xe ${xe.tenXe}?|"` vào thẻ `<form>` xóa |
| Phân trang | `<div th:replace="~{fragments/phan-trang :: phanTrang(${trang})}"></div>` với `trang` là `Page<...>` |
| Cột sắp xếp được | `<th><a th:replace="~{fragments/sap-xep :: cot('Giá bán', 'giaBan')}"></a></th>` |
| Lỗi cạnh ô nhập | `th:errorclass="is-invalid"` trên ô và `<div class="invalid-feedback" th:errors="*{tenXe}"></div>` ngay sau |
| Tên cửa hàng, hotline, địa chỉ | `th:text="#{cuahang.ten}"`, `#{cuahang.hotline}`... (sửa giá trị trong `messages.properties`) |

Mẫu đầy đủ để chép: trang danh sách `templates/quan-tri/tai-khoan/danh-sach.html`, controller `QuanLyTaiKhoanController`.

**Menu:** khi trang của bạn đã chạy, bỏ chú thích mục tương ứng trong `fragments/header.html` (menu chính),
`fragments/quan-tri.html` (thanh mục quản trị), `fragments/tai-khoan.html` (menu "Tài khoản của tôi") và `fragments/footer.html`.
Các mục này đã được viết sẵn và đặt trong chú thích.

**Ảnh:** đặt trong `static/images/`. Ảnh không phải của nhóm phải ghi nguồn và giấy phép trong `docs/nguon-anh-va-font.md`.

**Tự kiểm tra trước khi gộp:** chụp trang ở bề rộng 1280 px và 390 px, đối chiếu mục "Không" của quy chuẩn giao diện.

**Lưu ý Thymeleaf 3.1:** trong `th:href`, `th:src` không được gọi bean (`${@tenBean...}`) hay đọc tham số URL (`${param...}`).
Hãy tính trước bằng `th:with` rồi dùng biến, hoặc chuẩn bị sẵn trong Controller.

## 5. Phần việc của từng thành viên cần nối vào

### Thành viên làm khách hàng (bảng `khach_hang`)

- Bảng `khach_hang` nối **một - một** với `tai_khoan`: cột `tai_khoan_id BIGINT NOT NULL UNIQUE` + khóa ngoại tới `tai_khoan(id)`.
  Họ tên, email, số điện thoại đã có ở `tai_khoan`, không lặp lại; `khach_hang` chỉ giữ địa chỉ, ngày sinh, giới tính...
- Khi khách tự đăng ký, tạo hồ sơ khách hàng **trong cùng giao dịch** với tài khoản. Trong `TaiKhoanService.dangKy(...)`:

```java
@Transactional
public TaiKhoan dangKy(DangKyForm form) {
    TaiKhoan taiKhoan = taoMoi(...);            // đã có sẵn
    khachHangService.taoHoSo(taiKhoan);          // thêm dòng này - lỗi thì cả hai cùng bị hủy
    return taiKhoan;
}
```

- Khi quản trị viên xóa một tài khoản đã có hồ sơ khách hàng hoặc đơn hàng, `TaiKhoanService.xoa` đã tự báo
  "không thể xóa vì đang có dữ liệu liên quan" (nhờ khóa ngoại) - không cần viết thêm.

### Thành viên làm xe máy, hãng xe (bảng `xe_may`, `hang_xe`)

- Entity `XeMay` hiện chỉ có 4 thuộc tính `id`, `tenXe`, `giaBan`, `soLuongTon`. Hãy **bổ sung** thêm thuộc tính
  (`@ManyToOne HangXe hangXe`, màu sắc, hình ảnh...) nhưng **giữ nguyên 4 thuộc tính này** và tên cột của chúng.
- Thay file `database/02_xe_may_tam_thoi.sql` bằng script đầy đủ của bạn (giữ đủ 4 cột trên).
- **Không** cho form "sửa xe" ghi đè `soLuongTon`. Số tồn chỉ thay đổi qua `TonKhoService`:

```java
tonKhoService.nhapKho(xeId, soLuong);   // nhập hàng
tonKhoService.xuatKho(xeId, soLuong);   // xuất hàng, ném HetHangException nếu không đủ
```

  Lý do: nếu form sửa xe lưu lại con số đọc từ lúc mở form, các lần bán xe diễn ra trong lúc đó sẽ bị mất.

### Thành viên làm giỏ hàng, đặt hàng, hóa đơn

Trừ kho phải nằm **trong cùng giao dịch** với việc tạo hóa đơn: hết hàng thì hủy cả hóa đơn, không còn hóa đơn dở dang.

```java
@Transactional
public HoaDon datHang(Long taiKhoanId, Map<Long, Integer> soLuongTheoXe) {
    tonKhoService.xuatKhoNhieuXe(soLuongTheoXe);   // khóa từng xe theo thứ tự mã tăng dần, trừ kho
    HoaDon hoaDon = ...;                            // tạo hóa đơn và chi tiết hóa đơn
    return hoaDonRepository.save(hoaDon);
}
```

- Trong Controller, bắt `HetHangException` để báo lại cho khách: `e.getMessage()` đã là câu tiếng Việt
  ("Xe ... chỉ còn 2 chiếc, không đủ số lượng yêu cầu (3).").
- Hủy hóa đơn thì trả hàng về kho bằng `tonKhoService.nhapKho(...)`, cũng trong `@Transactional`.
- Lấy khách đang đặt hàng bằng `@AuthenticationPrincipal TaiKhoanDetails` (mục 3).

### Thành viên làm giao diện trang chủ, danh sách xe, chi tiết xe

- Thay nội dung `templates/index.html` bằng trang chủ thật theo quy chuẩn giao diện, giữ dòng `th:replace` bố cục chung.
- Trang danh sách xe dùng thẻ `.the-xe` đã có sẵn trong `theme.css`; trang chi tiết xe theo mục Quy trình làm trang mới của quy chuẩn giao diện.
- Muốn đổi màu, font hay kiểu thành phần: sửa quy chuẩn giao diện trước, rồi sửa biến ở đầu `static/css/theme.css`, để cả nhóm dùng chung.
- **Giữ các thuộc tính `sec:authorize`** trong header, footer (đó là phần phân quyền hiển thị menu, hạng mục 3.5 của rubric).

## 6. Xử lý lỗi

Ném đúng loại ngoại lệ, `GlobalExceptionHandler` sẽ hiển thị trang lỗi thân thiện:

| Tình huống | Ném |
|---|---|
| Không tìm thấy theo mã (`/xe-may/999`) | `KhongTimThayException` (trang 404) |
| Vi phạm quy tắc nghiệp vụ | `NghiepVuException` (trang 400 kèm thông báo) |
| Lỗi của một ô nhập, cần hiện cạnh ô đó | `DuLieuKhongHopLeException("tenTruong", "thông báo")`, bắt trong Controller rồi `ketQua.rejectValue(...)` |

Không bắt `Exception` chung chung trong `@ControllerAdvice`: làm vậy sẽ biến lỗi 403/404 thành lỗi 500.

## 7. Gộp script SQL của cả nhóm

File gộp cuối cùng phải **xóa bảng con trước, bảng cha sau** và **tạo bảng cha trước, bảng con sau**:

```
Xóa:  chi_tiet_hoa_don -> hoa_don -> gio_hang -> khach_hang -> xe_may -> hang_xe -> lich_su_dang_nhap -> tai_khoan -> vai_tro
Tạo:  vai_tro -> tai_khoan -> lich_su_dang_nhap -> hang_xe -> xe_may -> khach_hang -> gio_hang -> hoa_don -> chi_tiet_hoa_don
```

Kiểm tra script có khớp với các Entity không: chạy ứng dụng với `spring.jpa.hibernate.ddl-auto=validate`.
Nếu khởi động không lỗi thì Entity và script khớp nhau (phần đăng nhập - phân quyền đã kiểm tra như vậy trên SQL Server 2022).

## 8. Làm việc với Git

Rubric chấm cả lịch sử commit của **từng** thành viên (hạng mục 6.4), nên:

- Mỗi người làm trên nhánh riêng: `git switch -c xe-may`, `git switch -c gio-hang`...
- Commit nhỏ và thường xuyên (mỗi chức năng, mỗi lần sửa lỗi), lời nhắn tiếng Việt rõ ràng: `Thêm trang danh sách xe có phân trang`.
- Trước khi gộp vào nhánh chính: `git pull` nhánh chính về, chạy `./mvnw -pl web test` thấy đạt hết rồi mới tạo Pull Request.
- Không commit `target/`, `application-local.properties` (đã có trong `.gitignore`).
