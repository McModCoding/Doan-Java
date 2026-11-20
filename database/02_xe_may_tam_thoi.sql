/* =====================================================================================
   BẢNG xe_may TẠM THỜI - CHỈ ĐỦ CỘT ĐỂ CHẠY THỬ PHẦN TỒN KHO (xử lý đồng thời) VÀ CLIENT SOCKET.

   Thành viên phụ trách xe máy / hãng xe sẽ thay bảng này bằng bảng đầy đủ (hãng xe, màu, mô tả, hình ảnh...).
   Khi thay, xin GIỮ NGUYÊN 4 cột: id, ten_xe, gia_ban, so_luong_ton (TonKhoService và Socket server đang dùng).

   Chạy SAU file 01_tai_khoan_phan_quyen.sql:
     sqlcmd -S localhost -U sa -P <mật khẩu> -C -f 65001 -i 02_xe_may_tam_thoi.sql
   ===================================================================================== */
USE QuanLyXeMay;
GO

IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE referenced_object_id = OBJECT_ID(N'dbo.xe_may'))
BEGIN
    RAISERROR(N'Đang có bảng khác tham chiếu xe_may (bảng xe_may đầy đủ của nhóm đã có). Không chạy script tạm thời này.', 16, 1);
    SET NOEXEC ON;
END
GO

IF OBJECT_ID(N'dbo.xe_may', N'U') IS NOT NULL DROP TABLE dbo.xe_may;
GO

CREATE TABLE dbo.xe_may (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_xe_may PRIMARY KEY,
    ten_xe        NVARCHAR(150)  NOT NULL,
    gia_ban       NUMERIC(18, 0) NOT NULL CONSTRAINT CK_xe_may_gia_ban CHECK (gia_ban >= 0),
    -- Tài nguyên dùng chung của phần xử lý đồng thời: chỉ thay đổi qua TonKhoService (có khóa dòng)
    so_luong_ton  INT            NOT NULL CONSTRAINT CK_xe_may_so_luong_ton CHECK (so_luong_ton >= 0)
);
GO

INSERT INTO dbo.xe_may (ten_xe, gia_ban, so_luong_ton) VALUES
    (N'Honda Vision 2024', 31900000, 12),
    (N'Honda Air Blade 125', 42790000, 8),
    (N'Honda Wave Alpha 110', 18900000, 20),
    (N'Honda Winner X', 46160000, 3),
    (N'Honda SH Mode 125', 57130000, 6),
    (N'Honda Lead 125', 39560000, 9),
    (N'Honda Future 125 FI', 30520000, 11),
    (N'Honda SH 160i', 92490000, 2),
    (N'Yamaha Exciter 155 VVA', 48000000, 5),
    (N'Yamaha Sirius FI', 21500000, 15),
    (N'Yamaha Grande Hybrid', 46000000, 2),
    (N'Yamaha Janus 125', 29150000, 10),
    (N'Yamaha NVX 155 VVA', 53290000, 4),
    (N'Yamaha Jupiter Finn', 27500000, 7),
    (N'Suzuki Raider R150', 51190000, 3),
    (N'Suzuki Satria F150', 53490000, 1),
    (N'SYM Attila 50', 22800000, 6),
    (N'Piaggio Vespa Sprint 125', 79800000, 1),
    (N'Piaggio Liberty 125', 57900000, 0),
    (N'VinFast Evo 200', 22000000, 14);
GO

SET NOEXEC OFF;
GO

SELECT N'xe_may' AS bang, COUNT(*) AS so_dong FROM dbo.xe_may;
GO
