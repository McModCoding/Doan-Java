# Quy chuẩn giao diện - MotoStore

Tài liệu này là **quy chuẩn giao diện duy nhất** của cả nhóm. Thành viên nào làm trang mới cũng đọc tài liệu này trước.
Các giá trị gốc bên dưới (màu, chữ, bo góc, khoảng cách) đã được cài sẵn trong
`web/src/main/resources/static/css/theme.css` dưới dạng biến CSS và lớp Bootstrap, nên khi làm trang chỉ cần
**dùng lớp có sẵn, không tự đặt màu hay font mới**.

> Tên cửa hàng ("MotoStore"), hotline, địa chỉ đổi ở một chỗ duy nhất (hotline, địa chỉ hiện là dữ liệu tạm): `web/src/main/resources/messages.properties`.
>
> Các trang của khách (trang chủ, danh sách xe, chi tiết xe, giỏ hàng) do thành viên 2 làm trên cùng nền này, thêm các lớp riêng
> trong `static/css/motostore-frontend.css` (thẻ xe `.the-xe-khach`, khung ảnh `.anh-xe-khach`, khung chưa có ảnh `.anh-trong`,
> các lớp `moto-*`). File này nạp sau `theme.css` trong bố cục chung; trang mới cho khách dùng lại các lớp đó.

## Tổng quan

MotoStore là website của một đại lý xe máy chính hãng: khách vào web để **xem xe, so giá, đặt mua**, nhân viên vào để quản lý.
Giao diện phải gợi cảm giác của một showroom sáng sủa, ngăn nắp và nói thật:

- **Sáng và sạch.** Nền trắng, vùng phụ màu xám đá rất nhạt; không nền kem, không nền tối toàn trang.
- **Xe là nhân vật chính.** Ảnh xe lớn, nền ảnh sáng; chữ và khung chỉ làm nền cho ảnh.
- **Giá và hành động luôn rõ.** Giá in đậm, màu đỏ thương hiệu; mỗi màn hình chỉ có một nút chính màu đỏ.
- **Đáng tin hơn là "ngầu".** Không hiệu ứng lòe loẹt; thông tin cụ thể (bảo hành, trả góp, giờ mở cửa) thay cho khẩu hiệu.

Mật độ thông tin ở mức vừa: trang khách hàng thoáng, trang quản trị dày hơn nhưng vẫn theo cùng hệ màu và chữ.

## Màu sắc

### Thương hiệu
- **Đỏ thương hiệu** `primary #C8102E`: nút chính, giá bán, liên kết, chỉ báo đang chọn. Đây là màu nhấn **duy nhất**.
- **Đỏ đậm khi rê chuột** `primary-hover #A30D25`.
- **Đỏ nhạt** `primary-soft #FBE9EC`: nền rất nhỏ cho nhãn khuyến mãi, ô đang chọn. Không phủ cả mảng lớn.

### Bề mặt
- `canvas #FFFFFF`: nền trang, thẻ, ô nhập.
- `surface-soft #F2F3F5` (xám đá): nền khu vực phụ, nền ảnh sản phẩm, hàng tiêu đề bảng.
- `surface-strong #E9EBEE`: vùng được chọn, thanh tiến trình nền.
- `surface-dark #16181D`: chỉ dùng cho thanh thông tin trên cùng và chân trang.

### Chữ
- `ink #15171C`: tiêu đề, tên xe, số liệu quan trọng.
- `body #2B2F36`: chữ nội dung.
- `muted #5E6470`: chữ phụ, mô tả, nhãn cột. Độ tương phản trên nền trắng khoảng 6:1, đủ đọc.
- `muted-soft #6B717D`: chữ gợi ý trong ô nhập và chữ bị vô hiệu hóa. Vẫn đạt tương phản 4,9:1 (chuẩn WCAG AA cần 4,5:1).

### Đường kẻ
- `hairline #E4E6EA`: viền thẻ, đường chia bảng. `hairline-strong #CDD1D7`: viền ô nhập, viền nút phụ.

### Trạng thái
- Thành công `success #157347` trên nền `success-soft #E3F2EA`; cảnh báo `warning #9A5B0B` trên `warning-soft #FDF1DE`;
  lỗi `error #B42318` trên `error-soft #FCE9E7`. Nhãn trạng thái luôn dùng **nền nhạt + chữ đậm**, kèm chữ mô tả, không chỉ dựa vào màu.

## Chữ

### Font
**Be Vietnam Pro** cho mọi chữ: font do người Việt thiết kế, dấu tiếng Việt đặt đúng vị trí, có cá tính riêng (không phải
Inter/Roboto quá quen thuộc). Font được lưu trong dự án (`static/fonts/be-vietnam-pro/`),
chạy được khi không có Internet. Chỉ dùng 4 độ đậm: 400, 500, 600, 700.

### Cấp chữ
| Vai trò | Cỡ / dòng | Đậm | Dùng cho |
|---|---|---|---|
| display | 40 / 1.15 | 700 | Câu chính trên ảnh bìa (tối đa một chỗ mỗi trang) |
| heading-1 | 32 / 1.2 | 700 | Tiêu đề trang (`h1`) |
| heading-2 | 24 / 1.3 | 700 | Tiêu đề khu vực (`h2`) |
| heading-3 | 19 / 1.35 | 600 | Tiêu đề thẻ, tên xe trong danh sách |
| body-md | 16 / 1.6 | 400 | Nội dung |
| body-sm | 14 / 1.55 | 400 | Bảng, chú thích dài |
| label | 14 / 1.4 | 600 | Nhãn ô nhập, tiêu đề cột |
| caption | 13 / 1.45 | 400 | Ghi chú, nguồn ảnh, ngày giờ |
| price | 22 / 1.2 | 700 | Giá bán, màu `primary` |

### Nguyên tắc
- Viết hoa chữ đầu câu, **không** viết hoa toàn bộ đoạn văn hay nhãn dài; không giãn chữ rộng.
- Số liệu (giá, tồn kho, ngày) dùng `font-variant-numeric: tabular-nums` (lớp `.so`) để các cột thẳng hàng.
- Giá viết đủ: `31.900.000 đ` (dấu chấm ngăn cách hàng nghìn, chữ `đ` sau dấu cách).
- Dòng chữ nội dung không dài quá khoảng 75 ký tự.

## Bố cục

- Lưới 12 cột của Bootstrap, khung `container` rộng tối đa 1200 px.
- Khoảng cách theo thang 4 - 8 - 12 - 16 - 24 - 32 - 48 - 72 px. Các phần tử liên quan đứng sát nhau (8-12), các khối khác
  nhau cách xa (32-48), các phần lớn của trang cách 72. Không dùng một khoảng cách cho mọi thứ.
- Căn trái là mặc định. Chỉ căn giữa cho trang lỗi và thông báo trống.
- Trang khách hàng: ảnh và thông tin xe đặt cạnh nhau trên máy tính, xếp chồng trên điện thoại.
- Trang quản trị: tiêu đề trang, thanh thẻ chuyển mục, rồi nội dung; bảng chiếm hết chiều ngang.

## Độ nổi

- Giao diện **phẳng**: phân tầng bằng đường kẻ `hairline` và nền `surface-soft`, không bằng bóng đổ.
- Bóng đổ chỉ dùng cho lớp nổi thật sự: menu thả xuống, hộp xác nhận: `0 12px 32px rgba(21, 23, 28, 0.12)`.
- Không bóng màu, không quầng sáng, không hiệu ứng kính mờ.

## Hình khối

- Nút, ô nhập: bo `md` 6 px. Thẻ, khung ảnh, hộp thoại: bo `lg` 10 px. Nhãn trạng thái: bo `sm` 4 px.
- `pill` chỉ cho ảnh đại diện tròn. Không làm nút dạng viên thuốc.
- Ảnh sản phẩm tỉ lệ 4:3, ảnh bìa tỉ lệ 3:2 hoặc 16:9, luôn `object-fit: cover`, bo `lg`.

## Thành phần

### Thanh thông tin và menu (`fragments/header.html`)
Thanh tối mảnh trên cùng: hotline, địa chỉ showroom, giờ mở cửa. Bên dưới là menu nền trắng cao 72 px, kẻ một đường
`hairline` ở đáy: logo bên trái, mục điều hướng ở giữa, tài khoản ở bên phải. Mục đang mở: chữ `ink` và gạch chân đỏ 2 px.
Menu hiển thị **theo vai trò** (`sec:authorize`).

### Nút
| Loại | Lớp | Khi nào |
|---|---|---|
| Chính | `btn btn-primary` | Hành động chính duy nhất của màn hình: Đặt mua, Đăng nhập, Lưu |
| Phụ | `btn btn-outline-secondary` | Hủy, Quay lại, Bỏ lọc |
| Biểu tượng | `btn btn-icon` | Thao tác trên từng dòng bảng (sửa, khóa, xóa); luôn có `title` và `aria-label` |
| Nguy hiểm | `btn btn-danger` | Chỉ trong hộp xác nhận xóa |

Nhấn nút: thu nhỏ 2% trong 120 ms. Không hiệu ứng nảy.

### Thẻ sản phẩm (`.the-xe`) - dành cho trang danh sách xe
Khung ảnh nền `surface-soft` tỉ lệ 4:3, bên dưới: tên xe (heading-3), dòng phụ (hãng, phân khối - caption, muted),
giá (price), một nút "Xem chi tiết". Rê chuột: viền đậm lên `hairline-strong`, ảnh phóng 3% trong 200 ms. Không bóng đổ.

### Ô nhập và form
Nhãn đặt **trên** ô (label). Ô cao 44 px, viền `hairline-strong`, khi chọn: viền đỏ + vòng sáng đỏ 3 px độ mờ 20%.
Lỗi hiện **ngay dưới ô** bằng chữ `error`. Gợi ý định dạng hiện dưới ô bằng caption muted và ẩn đi khi ô đang báo lỗi.

### Bảng (trang quản trị)
Hàng tiêu đề nền `surface-soft`, chữ label màu muted, viết hoa chữ đầu. Dòng cao tối thiểu 56 px, kẻ `hairline`.
Cột số căn phải, dùng `.so`. Thao tác cuối dòng là các nút biểu tượng màu trung tính; nút xóa đỏ lên khi rê chuột.

### Nhãn trạng thái (`.nhan`)
Nền nhạt + chữ đậm cùng sắc: `nhan-thanh-cong`, `nhan-canh-bao`, `nhan-loi`, `nhan-trung-tinh`, `nhan-dam` (nền ink).

### Thông báo
Thanh ngang nền nhạt (`success-soft`, `error-soft`), biểu tượng bên trái, chữ `body`. Thông báo của form đăng nhập,
đăng ký nằm ngay trên form, không ở đầu trang.

### Chân trang (`fragments/footer.html`)
Nền `surface-dark`, 3-4 cột: giới thiệu ngắn và hotline, liên kết tài khoản, thông tin showroom. Dòng cuối: bản quyền.

## Nên và không nên

### Nên
- Dùng ảnh xe thật, sáng, rõ; ghi nguồn ảnh nếu không phải ảnh của nhóm (`docs/nguon-anh-va-font.md`).
- Viết nội dung cụ thể: "Bảo hành chính hãng 3 năm", "Mở cửa 8:00 - 20:00", "Còn 2 xe" thay cho câu quảng cáo chung chung.
- Mỗi màn hình một nút đỏ chính; các nút còn lại là nút phụ hoặc nút biểu tượng.
- Dùng lớp và biến trong `theme.css`; cần màu mới thì bổ sung vào file này trước.
- Kiểm tra trên điện thoại (rộng 390 px): không tràn ngang, nút bấm cao tối thiểu 44 px.

### Không
- Không gradient tím, xanh tím, xanh ngọc trên nền tối; không chữ tô gradient.
- Không nền kem, nền be làm nền trang.
- Không ô biểu tượng bo tròn đặt phía trên mỗi tiêu đề; không thẻ có viền màu dày một bên.
- Không thẻ lồng trong thẻ; dùng khoảng trắng và đường kẻ để chia nhóm.
- Không bóng đổ màu, quầng sáng, đốm sáng hình tròn sau tiêu đề.
- Không nhãn nhỏ viết hoa giãn chữ hoặc "viên thuốc" đặt ngay trên tiêu đề lớn.
- Không chữ chạy ngang tự động, chấm nhấp nháy, hiệu ứng nảy (bounce, elastic).
- Không chữ xám trên nền màu; không chữ nhỏ hơn 13 px trên giao diện.
- Không emoji làm biểu tượng; chỉ dùng Bootstrap Icons, một kiểu nét.
- Không câu chữ sáo rỗng ("trải nghiệm đột phá", "nâng tầm phong cách"), không lạm dụng gạch ngang dài.
- Không tự thêm font, màu, độ bo góc ngoài file này.

## Chuyển động

- Mọi chuyển động 120-200 ms, đường cong giảm tốc `cubic-bezier(0.23, 1, 0.32, 1)`; vào thì giảm tốc, không dùng `ease-in` cho chuyển động xuất hiện.
- Chỉ chuyển động khi có lý do: phản hồi nhấn nút, mở menu, hộp xác nhận xuất hiện, ảnh sản phẩm phóng nhẹ khi rê chuột.
- Tôn trọng `prefers-reduced-motion`: tắt phóng to và chuyển tiếp.

## Hiển thị trên nhiều cỡ màn hình

### Điểm ngắt
Theo Bootstrap: 576, 768, 992 (menu thu gọn dưới mức này), 1200 px.

### Vùng chạm
Nút, ô nhập, mục menu cao tối thiểu 44 px; nút biểu tượng trong bảng tối thiểu 36 px.

### Thu gọn
- Thanh thông tin trên cùng ẩn dưới 768 px; hotline chuyển vào menu thu gọn.
- Trang đăng nhập, đăng ký: ẩn ảnh bìa dưới 992 px, form chiếm hết chiều ngang.
- Bảng quản trị: ẩn cột phụ (email, ngày tạo) trên màn hình nhỏ, đưa thông tin quan trọng xuống dòng phụ trong ô chính.
- Chân trang: các cột xếp chồng.

## Quy trình làm trang mới

Khi làm một trang mới:

1. Dùng bố cục chung: `th:replace="~{fragments/layout :: trang(~{::title}, ~{::section})}"` (xem `docs/huong-dan-tich-hop.md`).
2. Dùng lớp Bootstrap và lớp trong `theme.css`; không viết `style=""` với màu hay font.
3. Trang danh sách xe: lưới `row g-4` 3 cột trên máy tính, 2 cột trên máy tính bảng, 1 cột trên điện thoại, mỗi ô là một thẻ xe
   (trang của khách dùng mảnh `fragments/the-xe :: theXe(xe)` của thành viên 2).
4. Trang chi tiết xe: ảnh lớn bên trái (`col-lg-7`), bên phải tên xe (h1), giá (`.gia`), lựa chọn màu, nút Đặt mua.
5. Xong thì chụp màn hình rộng 1280 px và 390 px, đối chiếu mục "Không" ở trên.


## Hạn chế hiện tại

- Logo hiện là chữ và một khối đỏ đơn giản, chưa phải logo thiết kế riêng.
- Ảnh trang đăng nhập, đăng ký là ảnh tạm từ Wikimedia Commons; cần ảnh chính thức của cửa hàng.
- Chưa có giao diện tối (dark mode); không cần cho đồ án.
