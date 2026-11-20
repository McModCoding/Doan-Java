# Nguồn ảnh và font

Mọi ảnh và font dùng trong giao diện đều có giấy phép cho phép sử dụng. Khi đưa ảnh chụp màn hình vào báo cáo,
giữ nguyên dòng ghi nguồn trên ảnh hoặc trích lại bảng dưới đây ở mục Tài liệu tham khảo.

## Ảnh

| Tệp trong dự án | Nội dung | Tác giả | Giấy phép | Nguồn |
|---|---|---|---|---|
| `static/images/bia-dang-nhap.jpg` | Honda SH 350 (2026) tại khu trưng bày | MotorideSA | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Honda_SH_350_2026.jpg) |
| `static/images/bia-dang-ky.jpg` | Honda SH (2025) tại khu trưng bày | AVMOTO | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Honda_SH_2025.jpg) |

Hai ảnh được thu nhỏ còn chiều ngang 1280 px và nén lại để trang tải nhanh; nội dung ảnh không bị chỉnh sửa.
Giấy phép CC BY-SA 4.0 yêu cầu ghi tên tác giả, ghi giấy phép và dẫn link nguồn: giao diện đã ghi ngay dưới góc ảnh.

**Đây là ảnh tạm.** Khi nhóm có ảnh chính thức (ảnh xe do cửa hàng tự chụp, hoặc ảnh lấy từ website hãng có ghi nguồn),
chỉ cần thay file cùng tên và sửa dòng ghi nguồn trong `templates/xac-thuc/dang-nhap.html`, `dang-ky.html`.

## Font

| Font | Phiên bản | Giấy phép | Nguồn |
|---|---|---|---|
| Be Vietnam Pro (Lâm Bảo, Tony Le, Việt Anh Nguyễn) | 5.3.0, gói Fontsource | SIL Open Font License 1.1 (`static/fonts/be-vietnam-pro/OFL-LICENSE.txt`) | [Google Fonts](https://fonts.google.com/specimen/Be+Vietnam+Pro), [Fontsource](https://fontsource.org/fonts/be-vietnam-pro) |

Font được lưu ngay trong dự án (`static/fonts/`), không tải từ Google Fonts, để buổi demo ở phòng lab không cần Internet.

## Thư viện giao diện

| Thư viện | Giấy phép |
|---|---|
| Bootstrap 5.3.8 | MIT |
| Bootstrap Icons 1.13.1 | MIT |
