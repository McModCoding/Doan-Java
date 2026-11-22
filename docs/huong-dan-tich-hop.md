# Hướng dẫn tích hợp cho các thành viên

Phần của các thành viên 1, 2, 3, 4 và phần giỏ hàng của thành viên 5 đã được ghép vào dự án này
(xem [bao-cao-tich-hop.md](bao-cao-tich-hop.md)). Tài liệu này dành cho người làm **phần còn lại** (hóa đơn, quản lý đơn hàng)
và cho ai sửa tiếp phần đã ghép. Mọi người làm trên **cùng một dự án** này (không tạo dự án riêng), theo các quy ước dưới đây.

## 1. Quy ước chung

| Nội dung | Quy ước |
|---|---|
| Package | `com.xemay.controller`, `.service`, `.repository`, `.entity`, `.dto`, `.config`. Trang quản trị đặt trong `controller/quantri` |
| Tên lớp, biến | Tiếng Việt không dấu, kiểu camelCase (`XeMayService`, `timTheoHang`) |
| Bảng, cột | **Theo script chung** `database/QL_CUAHANG_XEMAY.sql`: bảng `CHU_HOA_GACH_DUOI` (`DON_HANG`), cột `PascalCase` (`MaDonHang`, `NgayDat`) |
| Entity | **Mọi** thuộc tính ghi rõ `@Column(name = "...")`, khóa ngoại ghi `@JoinColumn(name = "...")` (xem lý do ở mục 7). Cột `NVARCHAR` thêm `@Nationalized`. Khóa chính `INT IDENTITY` dùng kiểu `Integer` |
| Nghiệp vụ | Viết trong **Service**, không viết trong Controller. Thao tác nhiều bước có `@Transactional` |
| URL người dùng | `/xe-may`, `/xe-may/{id}`, `/gio-hang` (đặt hàng: `POST /gio-hang/dat-hang`) đã có; `/don-hang` (đơn hàng của tôi) chưa có |
| URL quản trị | `/quan-tri/xe-may`, `/quan-tri/hang-xe`, `/quan-tri/khach-hang` (đã có), `/quan-tri/don-hang`, `/quan-tri/hoa-don` |
| Thêm / sửa / xóa | `GET .../them`, `POST .../them`, `GET .../{id}/sua`, `POST .../{id}/sua`, `POST .../{id}/xoa` (xóa luôn dùng POST) |

## 2. Phân quyền: trang mới của bạn ai được vào?

Quy tắc theo URL nằm trong `config/SecurityConfig.java`:

| URL | Ai được vào |
|---|---|
| `/`, `/dang-nhap`, `/dang-ky`, tài nguyên tĩnh | Tất cả |
| `GET /xe-may`, `GET /xe-may/**`, `GET /hang-xe/**` | Tất cả (khách chưa đăng nhập vẫn xem được xe) |
| `/gio-hang`, `/gio-hang/**` | Chỉ khách hàng (nhân viên vào bị 403) |
| `/quan-tri/tai-khoan/**`, `/quan-tri/lich-su-dang-nhap/**` | Chỉ quản trị viên |
| `/quan-tri/**` | Quản trị viên và nhân viên |
| **Mọi URL khác** (`/don-hang`, `/tai-khoan`...) | Phải đăng nhập |

Đặt trang quản trị dưới `/quan-tri/...`, trang của khách dưới các URL còn lại, thì **không cần sửa gì** trong `SecurityConfig`.

Quyền chi tiết hơn đặt bằng `@PreAuthorize` trên phương thức của Service (đã áp dụng cho phần đã ghép):

| Thao tác | Quản trị viên | Nhân viên |
|---|---|---|
| Xem, tìm, thêm, sửa xe máy; nhập kho | Có | Có |
| Ngừng kinh doanh / mở bán lại xe | Có | Không |
| Xem, thêm, sửa hãng xe | Có | Có |
| Ngừng hoạt động / mở lại hãng xe | Có | Không |
| Xem, tìm, thêm, sửa khách hàng | Có | Có |
| Xóa khách hàng | Có | Không |
| Quản lý tài khoản, xem lịch sử đăng nhập | Có | Không |

Đây là cách chia tạm do người tích hợp đề xuất; nhóm muốn đổi (ví dụ cho nhân viên xóa hóa đơn) thì sửa `@PreAuthorize` tương ứng.

```java
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public void xoa(Integer maDonHang) { ... }
```

## 3. Lấy người dùng đang đăng nhập và hồ sơ khách hàng

```java
@GetMapping("/don-hang")
public String donHangCuaToi(@AuthenticationPrincipal TaiKhoanDetails nguoiDung, Model model) {
    Integer maTaiKhoan = nguoiDung.getId();          // cột MaTaiKhoan
    KhachHang khach = khachHangService.timTheoTaiKhoan(maTaiKhoan)
            .orElseThrow(() -> new NghiepVuException("Tài khoản chưa có hồ sơ khách hàng."));
    Integer maKhachHang = khach.getMaKhachHang();     // dùng cho GIO_HANG, DON_HANG
    model.addAttribute("dsDon", donHangRepository.findByKhachHang_MaKhachHangOrderByMaDonHangDesc(maKhachHang));
    ...
}
```

`GioHangController` là mẫu đầy đủ: lấy khách đang đăng nhập, gọi Service, báo kết quả bằng thông báo flash.

Mỗi tài khoản khách hàng **luôn có** một hồ sơ `KHACH_HANG` (tạo cùng lúc khi đăng ký hoặc khi quản trị viên tạo tài khoản).
Khách mua tại cửa hàng thì có hồ sơ nhưng không có tài khoản (`MaTaiKhoan = NULL`).

Trong template (khai báo `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` ở thẻ `<html>`):

```html
<span sec:authentication="principal.hoTen">Họ tên</span>
<a sec:authorize="hasRole('KHACHHANG')" th:href="@{/gio-hang}">Giỏ hàng</a>
<a sec:authorize="hasAnyRole('ADMIN', 'NHANVIEN')" th:href="@{/quan-tri/xe-may}">Quản lý xe</a>
```

## 4. Giao diện: theo quy chuẩn và bố cục chung

**Đọc `docs/quy-chuan-giao-dien.md` trước khi làm bất kỳ trang nào.** Mọi giá trị trong đó đã được cài sẵn trong
`static/css/theme.css`, nên khi làm trang chỉ cần dùng lớp có sẵn, không tự đặt màu, font hay `style=""`.
**Không tải CSS, font, biểu tượng từ CDN** (demo ở phòng lab không có Internet): Bootstrap và Bootstrap Icons đã có qua WebJars.

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
| Giá bán | `.gia` (đỏ, đậm, chữ số thẳng cột); định dạng `${#numbers.formatDecimal(xe.giaBan, 1, 'POINT', 0, 'COMMA')} + ' đ'` |
| Khung nội dung có tiêu đề | `.card` + `.card-header` (có `h2`) + `.card-body` |
| Bảng | bọc trong `.bang-khung`, bảng `.table .table-hover .align-middle`; cột số thêm `.so` |
| Nhãn trạng thái | `.nhan` + `nhan-thanh-cong` / `nhan-canh-bao` / `nhan-loi` / `nhan-trung-tinh` / `nhan-dam` / `nhan-vien` |
| Nút | `btn btn-primary` (một nút chính mỗi màn hình), `btn btn-outline-secondary` (nút phụ), `btn btn-icon` (thao tác trong bảng; thêm `nguy-hiem` cho nút xóa) |
| Số liệu tổng quan | `.so-lieu`, `.so-lieu-nhan`, `.so-lieu-gia-tri`, `.so-lieu-phu` |
| Thông báo sau khi lưu | Controller: `chuyenHuong.addFlashAttribute("thongBaoThanhCong", "Đã lưu ...")` - bố cục chung tự hiển thị |
| Thông báo lỗi trên trang | `model.addAttribute("thongBaoLoi", "...")` |
| Hộp xác nhận trước khi xóa | thêm `th:data-xac-nhan="|Xóa xe ${xe.tenXe}?|"` vào thẻ `<form>` xóa |
| Phân trang | `<div th:replace="~{fragments/phan-trang :: phanTrang(${trang})}"></div>` với `trang` là `Page<...>` |
| Cột sắp xếp được | `<th><a th:replace="~{fragments/sap-xep :: cot('Giá bán', 'giaBan')}"></a></th>` |
| Lỗi cạnh ô nhập | `th:errorclass="is-invalid"` trên ô và `<div class="invalid-feedback" th:errors="*{tenXe}"></div>` ngay sau |
| Tên cửa hàng, hotline, địa chỉ | `th:text="#{cuahang.ten}"`, `#{cuahang.hotline}`... (sửa giá trị trong `messages.properties`) |

Mẫu đầy đủ để chép: `templates/quan-tri/xe-may/danh-sach.html` + `XeMayController` (danh sách có lọc, phân trang, sắp xếp),
`templates/quan-tri/khach-hang/form.html` + `KhachHangController` (form thêm / sửa có kiểm tra dữ liệu).

**Menu:** khi trang của bạn đã chạy, bỏ chú thích mục tương ứng trong `fragments/header.html` (menu chính),
`fragments/quan-tri.html` (thanh mục quản trị: còn mục Hóa đơn), `fragments/tai-khoan.html` (menu "Tài khoản của tôi")
và `fragments/footer.html` (cột Sản phẩm). Các mục này đã viết sẵn và đặt trong chú thích.

**Ảnh:** đặt trong `static/images/`. Ảnh không phải của nhóm phải ghi nguồn và giấy phép trong `docs/nguon-anh-va-font.md`.

**Tự kiểm tra trước khi gộp:** chụp trang ở bề rộng 1280 px và 390 px, đối chiếu mục "Không" của quy chuẩn giao diện.

**Lưu ý Thymeleaf 3.1:** trong `th:href`, `th:src` không được gọi bean (`${@tenBean...}`) hay đọc tham số URL (`${param...}`).
Hãy tính trước bằng `th:with` rồi dùng biến, hoặc chuẩn bị sẵn trong Controller.

## 5. Phần việc còn lại

### Thành viên làm giao diện cho khách (đã ghép ở lần 2)

Trang chủ, danh sách xe, chi tiết xe, giỏ hàng đã chạy với dữ liệu thật (`GiaoDienXeService`, `GiaoDienXeController`,
`templates/index.html`, `templates/xe-may/`, `templates/gio-hang/`). Những chỗ đã sửa khi ghép: báo cáo tích hợp mục 6.3. Còn lại:

- **Ảnh xe**: cột `XE_MAY.HinhAnh` trong dữ liệu mẫu đang để trống nên mọi xe hiện "Chưa có ảnh thực tế". Theo quy ước của bạn,
  đặt ảnh tên `xe-<mã xe>.jpg` vào `static/images/xe/` (ví dụ `xe-2.jpg` cho Vision) là hiện ngay; hoặc điền cột `HinhAnh`
  (ví dụ `/images/xe/vision.jpg`; nhân viên sửa được ở trang sửa xe). Ảnh không phải của nhóm thì ghi nguồn trong `docs/nguon-anh-va-font.md`.
- **Chân trang**: cột "Sản phẩm" trong `fragments/footer.html` (đang để trong chú thích), ví dụ liên kết tới `/xe-may` và `/xe-may?conHang=true`.
- Giữ các thuộc tính `sec:authorize` trong header, footer.

### Thành viên làm đặt hàng, hóa đơn

Đã có sau lần 2:

| Thành phần | Lớp | Ghi chú |
|---|---|---|
| Entity | `GioHang`, `ChiTietGioHang`, `DonHang`, `ChiTietDonHang` | Theo bảng của script; `ThanhTien` là cột tính toán nên ánh xạ chỉ đọc (`insertable = false, updatable = false`) |
| Repository | `GioHangRepository`, `DonHangRepository` | `findByKhachHang_MaKhachHangOrderByMaDonHangDesc`, `countByTrangThai` |
| Giỏ hàng | `GioHangService`, `GioHangController` (`/gio-hang`) | Lưu trong CSDL, mỗi khách một giỏ |
| Đặt hàng | `DatHangService.datHang`, `POST /gio-hang/dat-hang` | Khóa giỏ, **trừ kho ngay**, tạo đơn `CHO_XAC_NHAN`, làm trống giỏ, trong cùng một giao dịch |

Việc còn lại: Entity `HoaDon`, `ChiTietHoaDon` (bảng `HOA_DON`, `CHI_TIET_HOA_DON`); trang quản lý đơn hàng cho nhân viên
(`/quan-tri/don-hang`: xem, xác nhận, hủy); trang hóa đơn (`/quan-tri/hoa-don`); trang "Đơn hàng của tôi" cho khách (`/don-hang`).

- **Kho đã bị trừ lúc khách đặt.** Xác nhận đơn chỉ đổi trạng thái và tạo hóa đơn, **không** gọi `xuatKho` nữa (gọi là trừ kho
  hai lần). Procedure `sp_XacNhanDonHang` đã được sửa đúng như vậy (khóa đơn, đổi trạng thái, tạo hóa đơn) nên gọi từ Java cũng được.
- Hủy đơn đang chờ xác nhận thì trả lại xe cho kho bằng `TonKhoService.nhapKho`, khóa các xe theo mã tăng dần như lúc đặt hàng:

```java
@Transactional
public HoaDon xacNhanDon(Integer maDonHang) {
    DonHang don = entityManager.find(DonHang.class, maDonHang, LockModeType.PESSIMISTIC_WRITE); // khóa đơn: 2 người không cùng xác nhận 1 đơn
    if (!DonHang.CHO_XAC_NHAN.equals(don.getTrangThai())) throw new NghiepVuException("Đơn không ở trạng thái chờ xác nhận.");
    don.setTrangThai("DA_XAC_NHAN");
    return taoHoaDon(don);                               // HOA_DON + CHI_TIET_HOA_DON, chép từ các dòng của đơn
}

@Transactional
public void huyDon(Integer maDonHang) {
    DonHang don = entityManager.find(DonHang.class, maDonHang, LockModeType.PESSIMISTIC_WRITE);
    if (!DonHang.CHO_XAC_NHAN.equals(don.getTrangThai())) throw new NghiepVuException("Chỉ hủy được đơn đang chờ xác nhận.");
    Map<Integer, Integer> traLai = new TreeMap<>();      // mã xe tăng dần: cùng thứ tự khóa với đặt hàng, không deadlock
    for (ChiTietDonHang dong : don.getDsChiTiet()) traLai.merge(dong.getXeMay().getMaXe(), dong.getSoLuong(), Integer::sum);
    traLai.forEach(tonKhoService::nhapKho);
    don.setTrangThai("DA_HUY");
}
```

- Trong Controller, bắt `NghiepVuException` (gồm cả `HetHangException`) để báo lại: `e.getMessage()` đã là câu tiếng Việt.
- Menu: thêm mục "Đơn hàng" vào `fragments/quan-tri.html`; bỏ chú thích mục "Hóa đơn" ở đó và mục "Đơn hàng của tôi"
  trong `fragments/tai-khoan.html` khi trang tương ứng đã chạy.
- Demo đồng thời đã có: hai khách cùng đặt chiếc xe cuối cùng (`kiem-thu.md` mục 6.1, DH-03). Khi có trang quản lý đơn,
  thêm demo hai nhân viên cùng xác nhận một đơn: một người được, người kia báo đơn không còn chờ xác nhận, chỉ có một hóa đơn.

### Thành viên làm phân tích, khách hàng

- Phần quản lý khách hàng đã ghép nhưng **viết lại khá nhiều** cho khớp bảng `KHACH_HANG` (xem báo cáo tích hợp mục 3):
  đọc lại `KhachHang`, `KhachHangService`, `KhachHangController` để tự trình bày được khi chấm.
- Còn thiếu tài liệu phân tích: bảng chức năng / use case (tối thiểu 10 chức năng, hạng mục 2.4), sơ đồ ERD từ script.

## 6. Xử lý lỗi

Ném đúng loại ngoại lệ, `GlobalExceptionHandler` sẽ hiển thị trang lỗi thân thiện:

| Tình huống | Ném |
|---|---|
| Không tìm thấy theo mã (`/xe-may/999`) | `KhongTimThayException` (trang 404) |
| Vi phạm quy tắc nghiệp vụ | `NghiepVuException` (trang 400 kèm thông báo) |
| Lỗi của một ô nhập, cần hiện cạnh ô đó | `DuLieuKhongHopLeException("tenTruong", "thông báo")`, bắt trong Controller rồi `ketQua.rejectValue(...)` |

Không dùng `IllegalArgumentException` cho lỗi nghiệp vụ (sẽ thành lỗi 500), không bắt `Exception` chung chung trong
`@ControllerAdvice` (sẽ biến lỗi 403/404 thành lỗi 500).

## 7. Script SQL và Entity

- Chỉ có **một** script: `database/QL_CUAHANG_XEMAY.sql`. Thêm bảng, cột thì sửa script này (bảng con xóa trước, tạo sau),
  chạy lại trên máy mình rồi mới viết Entity.
- Ứng dụng chạy với `spring.jpa.hibernate.ddl-auto=validate`: khởi động mà Entity lệch script (thiếu bảng, thiếu cột, sai kiểu)
  thì báo lỗi ngay, ví dụ `Schema-validation: missing column [MaGioHang] in table [GIO_HANG]`. Khởi động được là Entity khớp script.
- Dự án dùng chiến lược đặt tên **giữ nguyên** (`PhysicalNamingStrategyStandardImpl`). Mặc định của Spring Boot sẽ đổi
  `@Column(name = "MaXe")` thành `ma_xe` và không tìm thấy cột; vì vậy mọi thuộc tính phải ghi tên cột đúng như script.
- Cột `NVARCHAR` (tiếng Việt có dấu) thêm `@Nationalized`; cột `VARCHAR` (tên đăng nhập, email, trạng thái) thì không.
- Kiểm thử tự động chạy trên H2 với `create-drop` (tự tạo bảng từ Entity), không cần script.

## 8. Làm việc với Git

Rubric chấm cả lịch sử commit của **từng** thành viên (hạng mục 6.4), nên mỗi người tự commit phần việc của mình:

- Lấy code về: `git clone <địa chỉ repo của nhóm>`, rồi tạo nhánh riêng: `git switch -c gio-hang`, `git switch -c giao-dien`...
- Commit nhỏ và thường xuyên (mỗi chức năng, mỗi lần sửa lỗi), lời nhắn tiếng Việt rõ ràng: `Thêm trang danh sách xe có phân trang`.
- Trước khi gộp vào nhánh chính: `git pull` nhánh chính về, chạy `./mvnw -pl web test` thấy đạt hết rồi mới tạo Pull Request.
- Không commit `target/`, `.idea/`, `application-local.properties` (đã có trong `.gitignore`).
