/*
  QL_CUAHANG_XEMAY - SQL Server
  Script tao database, 12 bang, du lieu mau va procedure xac nhan don hang.
  Chay trong SQL Server Management Studio (SSMS): mo file, bam Execute (F5).

  Nguon goc: script CSDL chung cua nhom (11 bang). Khi tich hop voi phan dang nhap, phan quyen
  da bo sung cac cho danh dau [Tich hop]:
    - TAI_KHOAN them cot HoTen, SoDienThoai (ho ten hien tren giao dien, ke ca tai khoan nhan vien)
    - Bang moi LICH_SU_DANG_NHAP (nhat ky dang nhap tu web va client Socket)
    - Mat khau mau la ma BCrypt that cua "123456" (truoc day la chuoi giu cho, khong dang nhap duoc)
    - 3 khach mua tai cua hang chua co tai khoan (MaTaiKhoan = NULL)
    - Sua loi: KHACH_HANG.MaTaiKhoan "NULL UNIQUE" chi cho 1 dong NULL -> doi sang chi muc co dieu kien
    - SET QUOTED_IDENTIFIER ON de chay duoc ca bang sqlcmd (cot tinh toan PERSISTED can tuy chon nay)
    - Kho tru ngay luc khach dat hang; sp_XacNhanDonHang chi doi trang thai va tao hoa don (khong tru kho lan hai)
  Ung dung chay voi spring.jpa.hibernate.ddl-auto=validate: Entity phai khop dung voi script nay.
*/
-- [Tich hop] Bat buoc cho cot tinh toan PERSISTED (ThanhTien). SSMS bat san, sqlcmd thi khong.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

IF DB_ID(N'QL_CUAHANG_XEMAY') IS NULL
    EXEC(N'CREATE DATABASE QL_CUAHANG_XEMAY COLLATE Vietnamese_CI_AS');   -- [Tich hop] sap xep tieng Viet dung
GO
USE QL_CUAHANG_XEMAY;
GO

IF OBJECT_ID('dbo.sp_XacNhanDonHang','P') IS NOT NULL DROP PROCEDURE dbo.sp_XacNhanDonHang;
GO
DROP TABLE IF EXISTS dbo.CHI_TIET_HOA_DON;
DROP TABLE IF EXISTS dbo.HOA_DON;
DROP TABLE IF EXISTS dbo.CHI_TIET_DON_HANG;
DROP TABLE IF EXISTS dbo.DON_HANG;
DROP TABLE IF EXISTS dbo.CHI_TIET_GIO_HANG;
DROP TABLE IF EXISTS dbo.GIO_HANG;
DROP TABLE IF EXISTS dbo.XE_MAY;
DROP TABLE IF EXISTS dbo.HANG_XE;
DROP TABLE IF EXISTS dbo.KHACH_HANG;
DROP TABLE IF EXISTS dbo.LICH_SU_DANG_NHAP;   -- [Tich hop]
DROP TABLE IF EXISTS dbo.TAI_KHOAN;
DROP TABLE IF EXISTS dbo.VAI_TRO;
GO

CREATE TABLE dbo.VAI_TRO (
 MaVaiTro INT IDENTITY PRIMARY KEY,
 TenVaiTro VARCHAR(30) NOT NULL UNIQUE,
 MoTa NVARCHAR(255) NULL
);
CREATE TABLE dbo.TAI_KHOAN (
 MaTaiKhoan INT IDENTITY PRIMARY KEY,
 MaVaiTro INT NOT NULL,
 TenDangNhap VARCHAR(50) NOT NULL UNIQUE,
 MatKhau VARCHAR(255) NOT NULL, -- Chi luu BCrypt/Argon2 hash
 Email VARCHAR(100) NOT NULL UNIQUE,
 TrangThai BIT NOT NULL DEFAULT 1,
 NgayTao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
 HoTen NVARCHAR(100) NOT NULL,      -- [Tich hop]
 SoDienThoai VARCHAR(15) NULL,      -- [Tich hop]
 CONSTRAINT FK_TAIKHOAN_VAITRO FOREIGN KEY(MaVaiTro) REFERENCES dbo.VAI_TRO(MaVaiTro)
);
-- [Tich hop] Nhat ky dang nhap. Xoa tai khoan thi giu nhat ky, MaTaiKhoan ve NULL.
CREATE TABLE dbo.LICH_SU_DANG_NHAP (
 MaLichSu BIGINT IDENTITY PRIMARY KEY,
 TenDangNhap NVARCHAR(100) NOT NULL,   -- ten nguoi dung da go, co the khong ton tai
 MaTaiKhoan INT NULL,
 ThoiGian DATETIME2(0) NOT NULL,
 DiaChiIp VARCHAR(45) NULL,
 Kenh VARCHAR(10) NOT NULL,
 ThanhCong BIT NOT NULL,
 GhiChu NVARCHAR(255) NULL,
 CONSTRAINT FK_LICHSU_TAIKHOAN FOREIGN KEY(MaTaiKhoan) REFERENCES dbo.TAI_KHOAN(MaTaiKhoan) ON DELETE SET NULL,
 CONSTRAINT CK_LICHSU_KENH CHECK(Kenh IN ('WEB','SOCKET'))
);
CREATE TABLE dbo.KHACH_HANG (
 MaKhachHang INT IDENTITY PRIMARY KEY,
 MaTaiKhoan INT NULL,   -- [Tich hop] bo UNIQUE o day, xem chi muc UX_KHACHHANG_TAIKHOAN ben duoi
 HoTen NVARCHAR(100) NOT NULL,
 SoDienThoai VARCHAR(15) NOT NULL UNIQUE,
 Email VARCHAR(100) NULL,
 DiaChi NVARCHAR(255) NULL,
 NgaySinh DATE NULL,
 NgayTao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
 CONSTRAINT FK_KHACHHANG_TAIKHOAN FOREIGN KEY(MaTaiKhoan) REFERENCES dbo.TAI_KHOAN(MaTaiKhoan),
 CONSTRAINT CK_KHACHHANG_NGAYSINH CHECK(NgaySinh IS NULL OR NgaySinh <= CONVERT(date,GETDATE()))
);
CREATE TABLE dbo.HANG_XE (
 MaHang INT IDENTITY PRIMARY KEY,
 TenHang NVARCHAR(100) NOT NULL UNIQUE,
 QuocGia NVARCHAR(100) NULL,
 MoTa NVARCHAR(500) NULL,
 TrangThai BIT NOT NULL DEFAULT 1
);
CREATE TABLE dbo.XE_MAY (
 MaXe INT IDENTITY PRIMARY KEY,
 MaHang INT NOT NULL,
 TenXe NVARCHAR(150) NOT NULL,
 LoaiXe NVARCHAR(50) NOT NULL,
 PhanKhoi INT NULL,
 MauSac NVARCHAR(50) NULL,
 GiaBan DECIMAL(18,2) NOT NULL,
 SoLuong INT NOT NULL DEFAULT 0,
 HinhAnh NVARCHAR(500) NULL,
 MoTa NVARCHAR(1000) NULL,
 TrangThai BIT NOT NULL DEFAULT 1,
 NgayTao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
 CONSTRAINT FK_XEMAY_HANGXE FOREIGN KEY(MaHang) REFERENCES dbo.HANG_XE(MaHang),
 CONSTRAINT CK_XEMAY_GIA CHECK(GiaBan > 0),
 CONSTRAINT CK_XEMAY_SOLUONG CHECK(SoLuong >= 0),
 CONSTRAINT CK_XEMAY_PHANKHOI CHECK(PhanKhoi IS NULL OR PhanKhoi > 0)
);
CREATE TABLE dbo.GIO_HANG (
 MaGioHang INT IDENTITY PRIMARY KEY,
 MaKhachHang INT NOT NULL UNIQUE,
 NgayTao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
 TrangThai VARCHAR(20) NOT NULL DEFAULT 'DANG_MUA',
 CONSTRAINT FK_GIOHANG_KHACHHANG FOREIGN KEY(MaKhachHang) REFERENCES dbo.KHACH_HANG(MaKhachHang),
 CONSTRAINT CK_GIOHANG_TRANGTHAI CHECK(TrangThai IN ('DANG_MUA','DA_DAT_HANG','DA_HUY'))
);
CREATE TABLE dbo.CHI_TIET_GIO_HANG (
 MaChiTietGioHang INT IDENTITY PRIMARY KEY,
 MaGioHang INT NOT NULL,
 MaXe INT NOT NULL,
 SoLuong INT NOT NULL,
 DonGia DECIMAL(18,2) NOT NULL,
 CONSTRAINT FK_CTG_GIOHANG FOREIGN KEY(MaGioHang) REFERENCES dbo.GIO_HANG(MaGioHang),
 CONSTRAINT FK_CTG_XEMAY FOREIGN KEY(MaXe) REFERENCES dbo.XE_MAY(MaXe),
 CONSTRAINT UQ_CTG_GIO_XE UNIQUE(MaGioHang,MaXe),
 CONSTRAINT CK_CTG_SOLUONG CHECK(SoLuong > 0),
 CONSTRAINT CK_CTG_DONGIA CHECK(DonGia > 0)
);
CREATE TABLE dbo.DON_HANG (
 MaDonHang INT IDENTITY PRIMARY KEY,
 MaKhachHang INT NOT NULL,
 NgayDat DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
 TongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
 TrangThai VARCHAR(20) NOT NULL DEFAULT 'CHO_XAC_NHAN',
 DiaChiGiaoHang NVARCHAR(255) NOT NULL,
 GhiChu NVARCHAR(500) NULL,
 CONSTRAINT FK_DONHANG_KHACHHANG FOREIGN KEY(MaKhachHang) REFERENCES dbo.KHACH_HANG(MaKhachHang),
 CONSTRAINT CK_DONHANG_TONGTIEN CHECK(TongTien >= 0),
 CONSTRAINT CK_DONHANG_TRANGTHAI CHECK(TrangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN','DANG_GIAO','HOAN_TAT','DA_HUY'))
);
CREATE TABLE dbo.CHI_TIET_DON_HANG (
 MaChiTietDonHang INT IDENTITY PRIMARY KEY,
 MaDonHang INT NOT NULL,
 MaXe INT NOT NULL,
 SoLuong INT NOT NULL,
 DonGia DECIMAL(18,2) NOT NULL,
 ThanhTien AS CONVERT(DECIMAL(18,2),SoLuong*DonGia) PERSISTED,
 CONSTRAINT FK_CTDH_DONHANG FOREIGN KEY(MaDonHang) REFERENCES dbo.DON_HANG(MaDonHang),
 CONSTRAINT FK_CTDH_XEMAY FOREIGN KEY(MaXe) REFERENCES dbo.XE_MAY(MaXe),
 CONSTRAINT UQ_CTDH_DON_XE UNIQUE(MaDonHang,MaXe),
 CONSTRAINT CK_CTDH_SOLUONG CHECK(SoLuong > 0),
 CONSTRAINT CK_CTDH_DONGIA CHECK(DonGia > 0)
);
CREATE TABLE dbo.HOA_DON (
 MaHoaDon INT IDENTITY PRIMARY KEY,
 MaDonHang INT NOT NULL UNIQUE,
 NgayLap DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
 TongTien DECIMAL(18,2) NOT NULL,
 TrangThai VARCHAR(20) NOT NULL DEFAULT 'CHUA_THANH_TOAN',
 CONSTRAINT FK_HOADON_DONHANG FOREIGN KEY(MaDonHang) REFERENCES dbo.DON_HANG(MaDonHang),
 CONSTRAINT CK_HOADON_TONGTIEN CHECK(TongTien >= 0),
 CONSTRAINT CK_HOADON_TRANGTHAI CHECK(TrangThai IN ('CHUA_THANH_TOAN','DA_THANH_TOAN','DA_HUY'))
);
CREATE TABLE dbo.CHI_TIET_HOA_DON (
 MaChiTietHoaDon INT IDENTITY PRIMARY KEY,
 MaHoaDon INT NOT NULL,
 MaXe INT NOT NULL,
 SoLuong INT NOT NULL,
 DonGia DECIMAL(18,2) NOT NULL,
 ThanhTien AS CONVERT(DECIMAL(18,2),SoLuong*DonGia) PERSISTED,
 CONSTRAINT FK_CTHD_HOADON FOREIGN KEY(MaHoaDon) REFERENCES dbo.HOA_DON(MaHoaDon),
 CONSTRAINT FK_CTHD_XEMAY FOREIGN KEY(MaXe) REFERENCES dbo.XE_MAY(MaXe),
 CONSTRAINT UQ_CTHD_HD_XE UNIQUE(MaHoaDon,MaXe),
 CONSTRAINT CK_CTHD_SOLUONG CHECK(SoLuong > 0),
 CONSTRAINT CK_CTHD_DONGIA CHECK(DonGia > 0)
);
GO

CREATE INDEX IX_XEMAY_TEN ON dbo.XE_MAY(TenXe);
CREATE INDEX IX_XEMAY_HANG_LOAI ON dbo.XE_MAY(MaHang,LoaiXe);
CREATE INDEX IX_XEMAY_GIA ON dbo.XE_MAY(GiaBan);
CREATE INDEX IX_DONHANG_KHACH_NGAY ON dbo.DON_HANG(MaKhachHang,NgayDat);
-- [Tich hop] Moi tai khoan gan voi toi da 1 ho so khach hang. Rang buoc UNIQUE thuong cua SQL Server chi cho
-- DUNG MOT dong NULL, nen chi luu duoc 1 khach chua co tai khoan; chi muc co dieu kien bo qua cac dong NULL.
CREATE UNIQUE INDEX UX_KHACHHANG_TAIKHOAN ON dbo.KHACH_HANG(MaTaiKhoan) WHERE MaTaiKhoan IS NOT NULL;
CREATE INDEX IX_LICHSU_THOIGIAN ON dbo.LICH_SU_DANG_NHAP(ThoiGian);          -- [Tich hop]
CREATE INDEX IX_LICHSU_TENDANGNHAP ON dbo.LICH_SU_DANG_NHAP(TenDangNhap);    -- [Tich hop]
GO

-- [Tich hop] MoTa la ten vai tro hien tren giao dien
INSERT dbo.VAI_TRO(TenVaiTro,MoTa) VALUES
('ADMIN',N'Quản trị viên'),('NHANVIEN',N'Nhân viên'),('KHACHHANG',N'Khách hàng');

-- [Tich hop] Giu nguyen ten dang nhap, email cua nhom; them ho ten, so dien thoai.
-- Mat khau cua MOI tai khoan mau: 123456 (ma BCrypt ben duoi). khach16 bi khoa de demo.
DECLARE @MatKhau VARCHAR(255) = '$2a$10$4TTT8WVNnabVNDbEcGPN8u9e4CsI0.39.IFTARAEkpwDatsjT5jca';
INSERT dbo.TAI_KHOAN(MaVaiTro,TenDangNhap,MatKhau,Email,HoTen,SoDienThoai,TrangThai) VALUES
(1,'admin',@MatKhau,'admin@xemay.local',N'Nguyễn Minh Quân','0901000001',1),
(2,'nhanvien1',@MatKhau,'nv1@xemay.local',N'Lê Văn Hùng','0902000001',1),
(2,'nhanvien2',@MatKhau,'nv2@xemay.local',N'Nguyễn Thị Mai','0902000002',1),
(2,'nhanvien3',@MatKhau,'nv3@xemay.local',N'Trần Minh Tuấn','0902000003',1),
(2,'nhanvien4',@MatKhau,'nv4@xemay.local',N'Võ Thu Thảo','0902000004',1),
(3,'khach1',@MatKhau,'khach1@xemay.local',N'Nguyễn Văn An','0900000001',1),
(3,'khach2',@MatKhau,'khach2@xemay.local',N'Trần Thị Bích','0900000002',1),
(3,'khach3',@MatKhau,'khach3@xemay.local',N'Lê Quang Cường','0900000003',1),
(3,'khach4',@MatKhau,'khach4@xemay.local',N'Phạm Thị Dung','0900000004',1),
(3,'khach5',@MatKhau,'khach5@xemay.local',N'Hoàng Văn Em','0900000005',1),
(3,'khach6',@MatKhau,'khach6@xemay.local',N'Đặng Thị Phương','0900000006',1),
(3,'khach7',@MatKhau,'khach7@xemay.local',N'Bùi Đức Giang','0900000007',1),
(3,'khach8',@MatKhau,'khach8@xemay.local',N'Đỗ Thị Hoa','0900000008',1),
(3,'khach9',@MatKhau,'khach9@xemay.local',N'Ngô Quốc Khánh','0900000009',1),
(3,'khach10',@MatKhau,'khach10@xemay.local',N'Dương Thị Linh','0900000010',1),
(3,'khach11',@MatKhau,'khach11@xemay.local',N'Lý Văn Minh','0900000011',1),
(3,'khach12',@MatKhau,'khach12@xemay.local',N'Vũ Thị Ngọc','0900000012',1),
(3,'khach13',@MatKhau,'khach13@xemay.local',N'Trịnh Văn Phúc','0900000013',1),
(3,'khach14',@MatKhau,'khach14@xemay.local',N'Mai Thị Quỳnh','0900000014',1),
(3,'khach15',@MatKhau,'khach15@xemay.local',N'Cao Văn Sơn','0900000015',1),
(3,'khach16',@MatKhau,'khach16@xemay.local',N'Tạ Thị Thu','0900000016',0),
(3,'khach17',@MatKhau,'khach17@xemay.local',N'Lương Văn Toàn','0900000017',1),
(3,'khach18',@MatKhau,'khach18@xemay.local',N'Hồ Thị Uyên','0900000018',1),
(3,'khach19',@MatKhau,'khach19@xemay.local',N'Châu Văn Vĩnh','0900000019',1),
(3,'khach20',@MatKhau,'khach20@xemay.local',N'Kiều Thị Xuân','0900000020',1);

-- [Tich hop] Ho ten, so dien thoai cua ho so khach hang lay tu tai khoan (truoc day la "Khach hang N")
INSERT dbo.KHACH_HANG(MaTaiKhoan,HoTen,SoDienThoai,Email,DiaChi,NgaySinh)
SELECT MaTaiKhoan,HoTen,SoDienThoai,
       Email,CONCAT(N'Địa chỉ ',ROW_NUMBER() OVER(ORDER BY MaTaiKhoan),N', TP. Hồ Chí Minh'),
       DATEADD(YEAR,-(20+ROW_NUMBER() OVER(ORDER BY MaTaiKhoan)),CONVERT(date,GETDATE()))
FROM dbo.TAI_KHOAN WHERE MaVaiTro=3;

INSERT dbo.HANG_XE(TenHang,QuocGia,MoTa) VALUES
(N'Honda',N'Nhật Bản',N'Hãng xe Nhật Bản'),(N'Yamaha',N'Nhật Bản',N'Hãng xe Nhật Bản'),
(N'Suzuki',N'Nhật Bản',N'Hãng xe Nhật Bản'),(N'Piaggio',N'Ý',N'Hãng xe Ý'),
(N'Vespa',N'Ý',N'Dòng xe tay ga'),(N'Kawasaki',N'Nhật Bản',N'Mô tô thể thao'),
(N'KTM',N'Áo',N'Hãng mô tô'),(N'BMW Motorrad',N'Đức',N'Hãng mô tô'),
(N'Ducati',N'Ý',N'Hãng mô tô'),(N'Triumph',N'Anh',N'Hãng mô tô'),
(N'Harley-Davidson',N'Mỹ',N'Hãng mô tô'),(N'Royal Enfield',N'Ấn Độ',N'Hãng mô tô'),
(N'GPX',N'Thái Lan',N'Hãng xe'),(N'Benelli',N'Ý',N'Hãng mô tô'),
(N'CFMoto',N'Trung Quốc',N'Hãng mô tô'),(N'SYM',N'Đài Loan',N'Hãng xe'),
(N'Kymco',N'Đài Loan',N'Hãng xe'),(N'VinFast',N'Việt Nam',N'Hãng xe điện'),
(N'Yadea',N'Trung Quốc',N'Hãng xe điện'),(N'Pega',N'Việt Nam',N'Hãng xe điện');

INSERT dbo.XE_MAY(MaHang,TenXe,LoaiXe,PhanKhoi,MauSac,GiaBan,SoLuong,MoTa) VALUES
(1,N'Wave Alpha',N'Xe số',110,N'Đỏ',18000000,30,N'Xe số phổ thông'),
(1,N'Vision',N'Tay ga',110,N'Trắng',32000000,25,N'Xe tay ga đô thị'),
(1,N'Air Blade 160',N'Tay ga',160,N'Đen',56000000,20,N'Xe tay ga thể thao'),
(1,N'Winner X',N'Côn tay',150,N'Đỏ đen',47000000,15,N'Xe côn tay'),
(2,N'Exciter 155',N'Côn tay',155,N'Xanh',52000000,18,N'Xe côn tay thể thao'),
(2,N'Grande',N'Tay ga',125,N'Trắng',46000000,20,N'Xe tay ga'),
(2,N'Sirius',N'Xe số',115,N'Đen',22000000,25,N'Xe số phổ thông'),
(3,N'Raider R150',N'Côn tay',150,N'Xanh',51000000,12,N'Xe côn tay'),
(3,N'Address 125',N'Tay ga',125,N'Đen',29000000,16,N'Xe tay ga'),
(4,N'Liberty 125',N'Tay ga',125,N'Trắng',58000000,10,N'Xe tay ga'),
(5,N'Vespa Sprint',N'Tay ga',150,N'Xanh',80000000,10,N'Xe tay ga phong cách'),
(6,N'Ninja 400',N'Mô tô',400,N'Xanh lá',160000000,8,N'Mô tô thể thao'),
(7,N'Duke 390',N'Mô tô',390,N'Cam',190000000,6,N'Mô tô naked'),
(8,N'G 310 R',N'Mô tô',310,N'Trắng',180000000,7,N'Mô tô naked'),
(9,N'Monster',N'Mô tô',937,N'Đỏ',400000000,3,N'Mô tô phân khối lớn'),
(10,N'Trident 660',N'Mô tô',660,N'Đen',280000000,4,N'Mô tô roadster'),
(12,N'Classic 350',N'Mô tô',350,N'Bạc',130000000,9,N'Mô tô cổ điển'),
(14,N'Imperiale 400',N'Mô tô',400,N'Đen',120000000,5,N'Mô tô cổ điển'),
(18,N'Vento S',N'Xe điện',NULL,N'Đen',18000000,20,N'Xe máy điện'),
(19,N'Orla',N'Xe điện',NULL,N'Trắng',16000000,20,N'Xe máy điện');

INSERT dbo.GIO_HANG(MaKhachHang) SELECT MaKhachHang FROM dbo.KHACH_HANG;
INSERT dbo.CHI_TIET_GIO_HANG(MaGioHang,MaXe,SoLuong,DonGia)
SELECT MaGioHang,MaGioHang,1,(SELECT GiaBan FROM dbo.XE_MAY WHERE MaXe=MaGioHang)
FROM dbo.GIO_HANG;

INSERT dbo.DON_HANG(MaKhachHang,TrangThai,DiaChiGiaoHang,GhiChu)
SELECT MaKhachHang,'CHO_XAC_NHAN',DiaChi,N'Đơn hàng dữ liệu mẫu'
FROM dbo.KHACH_HANG;
INSERT dbo.CHI_TIET_DON_HANG(MaDonHang,MaXe,SoLuong,DonGia)
SELECT MaDonHang,MaDonHang,1,(SELECT GiaBan FROM dbo.XE_MAY WHERE MaXe=MaDonHang)
FROM dbo.DON_HANG;
UPDATE d SET TongTien=x.Tong
FROM dbo.DON_HANG d
CROSS APPLY(SELECT SUM(ThanhTien) Tong FROM dbo.CHI_TIET_DON_HANG c WHERE c.MaDonHang=d.MaDonHang)x;
-- [Tich hop] Don mau tru kho ngay luc dat, giong don dat tren web (ton kho mau sau script khong doi)
UPDATE x SET SoLuong=x.SoLuong-c.SoLuong
FROM dbo.XE_MAY x JOIN dbo.CHI_TIET_DON_HANG c ON c.MaXe=x.MaXe;
GO

/* Xac nhan don: khoa don va dong xe, tru ton, tao hoa don/chi tiet trong mot transaction */
/* [Tich hop] Kho da tru ngay luc khach dat hang (DatHangService, nhu ban demo gio hang cua nhom).
   Procedure khong kiem tra, khong tru ton lan nua: neu tru o day thi don dat tren web bi tru kho hai lan.
   Van khoa dong don hang (UPDLOCK, HOLDLOCK): hai nguoi cung xac nhan mot don thi chi mot nguoi thanh cong. */
CREATE PROCEDURE dbo.sp_XacNhanDonHang @MaDonHang INT
AS
BEGIN
 SET NOCOUNT ON;
 SET XACT_ABORT ON;
 BEGIN TRY
  BEGIN TRANSACTION;
  DECLARE @TrangThai VARCHAR(20);
  SELECT @TrangThai=TrangThai FROM dbo.DON_HANG WITH(UPDLOCK,HOLDLOCK)
  WHERE MaDonHang=@MaDonHang;
  IF @TrangThai IS NULL THROW 50001,N'Không tìm thấy đơn hàng.',1;
  IF @TrangThai<>'CHO_XAC_NHAN' THROW 50002,N'Đơn hàng không ở trạng thái chờ xác nhận.',1;
  IF NOT EXISTS(SELECT 1 FROM dbo.CHI_TIET_DON_HANG WHERE MaDonHang=@MaDonHang)
    THROW 50003,N'Đơn hàng chưa có sản phẩm.',1;
  /* [Tich hop] Bo kiem tra va tru ton kho (da tru luc dat hang):
  IF EXISTS(
    SELECT 1 FROM dbo.CHI_TIET_DON_HANG c
    JOIN dbo.XE_MAY x WITH(UPDLOCK,HOLDLOCK) ON x.MaXe=c.MaXe
    WHERE c.MaDonHang=@MaDonHang AND (x.TrangThai=0 OR x.SoLuong<c.SoLuong)
  ) THROW 50004,N'Sản phẩm ngừng kinh doanh hoặc không đủ tồn kho.',1;

  UPDATE x SET SoLuong=x.SoLuong-c.SoLuong
  FROM dbo.XE_MAY x JOIN dbo.CHI_TIET_DON_HANG c ON c.MaXe=x.MaXe
  WHERE c.MaDonHang=@MaDonHang;
  */

  UPDATE dbo.DON_HANG
  SET TongTien=(SELECT SUM(ThanhTien) FROM dbo.CHI_TIET_DON_HANG WHERE MaDonHang=@MaDonHang),
      TrangThai='DA_XAC_NHAN'
  WHERE MaDonHang=@MaDonHang;

  INSERT dbo.HOA_DON(MaDonHang,TongTien,TrangThai)
  SELECT MaDonHang,TongTien,'CHUA_THANH_TOAN'
  FROM dbo.DON_HANG WHERE MaDonHang=@MaDonHang;
  DECLARE @MaHoaDon INT=SCOPE_IDENTITY();

  INSERT dbo.CHI_TIET_HOA_DON(MaHoaDon,MaXe,SoLuong,DonGia)
  SELECT @MaHoaDon,MaXe,SoLuong,DonGia
  FROM dbo.CHI_TIET_DON_HANG WHERE MaDonHang=@MaDonHang;

  COMMIT TRANSACTION;
  SELECT N'Xác nhận đơn hàng thành công' ThongBao,@MaDonHang MaDonHang,@MaHoaDon MaHoaDon;
 END TRY
 BEGIN CATCH
  IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
  THROW;
 END CATCH
END;
GO

/* Tao hoa don mau thong qua dung quy trinh xac nhan (20 don mau) */
DECLARE @OrderId INT=1;
WHILE @OrderId<=20
BEGIN
    EXEC dbo.sp_XacNhanDonHang @MaDonHang=@OrderId;
    SET @OrderId+=1;
END;
GO

/* [Tich hop] Nhat ky dang nhap mau (25 dong), thoi gian tinh lui tu luc chay script */
INSERT dbo.LICH_SU_DANG_NHAP(TenDangNhap,MaTaiKhoan,ThoiGian,DiaChiIp,Kenh,ThanhCong,GhiChu)
SELECT v.TenDangNhap,tk.MaTaiKhoan,DATEADD(MINUTE,-v.PhutTruoc,SYSDATETIME()),v.DiaChiIp,v.Kenh,v.ThanhCong,v.GhiChu
FROM (VALUES
 (N'admin',10080,'192.168.1.10','WEB',1,NULL),
 (N'nhanvien1',9000,'192.168.1.21','WEB',1,NULL),
 (N'nhanvien1',8990,'192.168.1.21','SOCKET',1,NULL),
 (N'khach1',8500,'113.161.20.5','WEB',1,NULL),
 (N'khach2',8000,'14.232.11.8','WEB',0,N'Sai tên đăng nhập hoặc mật khẩu'),
 (N'khach2',7995,'14.232.11.8','WEB',1,NULL),
 (N'admin123',7000,'45.77.10.99','WEB',0,N'Sai tên đăng nhập hoặc mật khẩu'),
 (N'admin123',6999,'45.77.10.99','WEB',0,N'Sai tên đăng nhập hoặc mật khẩu'),
 (N'nhanvien2',6500,'192.168.1.22','SOCKET',1,NULL),
 (N'khach3',6000,'192.168.1.50','SOCKET',0,N'Vai trò không được phép dùng kênh SOCKET'),
 (N'khach4',5500,'171.244.3.17','WEB',1,NULL),
 (N'khach5',5000,'27.72.98.4','WEB',1,NULL),
 (N'khach16',4500,'118.70.12.6','WEB',0,N'Tài khoản đang bị khóa'),
 (N'khach6',4000,'42.112.7.1','WEB',0,N'Sai tên đăng nhập hoặc mật khẩu'),
 (N'khach6',3998,'42.112.7.1','WEB',1,NULL),
 (N'nhanvien3',3500,'192.168.1.23','WEB',1,NULL),
 (N'khach7',3000,'113.185.40.2','WEB',1,NULL),
 (N'nhanvien4',2500,'192.168.1.24','WEB',1,NULL),
 (N'khach8',2000,'1.54.22.9','WEB',1,NULL),
 (N'nhanvien2',1500,'192.168.1.22','SOCKET',1,NULL),
 (N'khach9',1000,'116.96.45.3','WEB',1,NULL),
 (N'khach10',600,'103.7.36.12','WEB',0,N'Sai tên đăng nhập hoặc mật khẩu'),
 (N'nhanvien1',300,'192.168.1.21','SOCKET',1,NULL),
 (N'admin',120,'192.168.1.10','WEB',1,NULL),
 (N'khach11',30,'14.161.2.20','WEB',1,NULL)
) AS v(TenDangNhap,PhutTruoc,DiaChiIp,Kenh,ThanhCong,GhiChu)
LEFT JOIN dbo.TAI_KHOAN tk ON tk.TenDangNhap=v.TenDangNhap;

/* [Tich hop] Khach mua tai cua hang, chua co tai khoan web (MaTaiKhoan = NULL) */
INSERT dbo.KHACH_HANG(MaTaiKhoan,HoTen,SoDienThoai,Email,DiaChi,NgaySinh) VALUES
(NULL,N'Phan Văn Lộc','0913000001',NULL,N'45 Lê Văn Việt, phường Tăng Nhơn Phú, TP. Hồ Chí Minh','1988-03-14'),
(NULL,N'Huỳnh Thị Mỹ','0913000002','my.huynh@gmail.com',N'12 Đỗ Xuân Hợp, phường Phước Long, TP. Hồ Chí Minh','1995-11-02'),
(NULL,N'Tôn Thất Nam','0913000003',NULL,NULL,NULL);
GO

/* Hai truy van tuy chinh tham khao cho module XE_MAY */
-- Tim kiem/loc: SELECT x.*,h.TenHang FROM dbo.XE_MAY x JOIN dbo.HANG_XE h ON h.MaHang=x.MaHang
-- WHERE x.TenXe LIKE N'%vision%' AND (@MaHang IS NULL OR x.MaHang=@MaHang)
-- AND (@LoaiXe IS NULL OR x.LoaiXe=@LoaiXe) AND x.GiaBan BETWEEN @GiaMin AND @GiaMax;
-- Thong ke: SELECT h.TenHang,COUNT(x.MaXe) SoMauXe,AVG(x.GiaBan) GiaTrungBinh
-- FROM dbo.HANG_XE h LEFT JOIN dbo.XE_MAY x ON x.MaHang=h.MaHang GROUP BY h.TenHang;

SELECT 'VAI_TRO' Bang,COUNT(*) SoBanGhi FROM dbo.VAI_TRO
UNION ALL SELECT 'TAI_KHOAN',COUNT(*) FROM dbo.TAI_KHOAN
UNION ALL SELECT 'LICH_SU_DANG_NHAP',COUNT(*) FROM dbo.LICH_SU_DANG_NHAP
UNION ALL SELECT 'KHACH_HANG',COUNT(*) FROM dbo.KHACH_HANG
UNION ALL SELECT 'HANG_XE',COUNT(*) FROM dbo.HANG_XE
UNION ALL SELECT 'XE_MAY',COUNT(*) FROM dbo.XE_MAY
UNION ALL SELECT 'GIO_HANG',COUNT(*) FROM dbo.GIO_HANG
UNION ALL SELECT 'CHI_TIET_GIO_HANG',COUNT(*) FROM dbo.CHI_TIET_GIO_HANG
UNION ALL SELECT 'DON_HANG',COUNT(*) FROM dbo.DON_HANG
UNION ALL SELECT 'CHI_TIET_DON_HANG',COUNT(*) FROM dbo.CHI_TIET_DON_HANG
UNION ALL SELECT 'HOA_DON',COUNT(*) FROM dbo.HOA_DON
UNION ALL SELECT 'CHI_TIET_HOA_DON',COUNT(*) FROM dbo.CHI_TIET_HOA_DON;
GO

/*
  Ghi chu:
  - Du lieu mau: 25 tai khoan (1 admin, 4 nhan vien, 20 khach - mat khau deu la 123456),
    23 khach hang (20 co tai khoan + 3 khach mua tai cua hang), 20 hang xe, 20 xe,
    20 gio hang, 20 chi tiet gio, 20 don hang, 20 chi tiet don, 25 dong nhat ky dang nhap.
  - Hoa don chi tao khi goi sp_XacNhanDonHang. De dam bao rubric co du lieu
    mau cho bang hoa don, hay tao don mau va goi procedure cho tung don.
  - Khi xac nhan don, app nen goi procedure nay trong transaction/service,
    khong tu tru kho o Controller.
  - [Tich hop] Trong ung dung Java, so luong ton (XE_MAY.SoLuong) chi duoc thay doi qua
    TonKhoService (khoa dong bang PESSIMISTIC_WRITE, tuong tu UPDLOCK trong SQL Server).
    Dat hang tru kho ngay (DatHangService); huy don thi tra hang bang TonKhoService.nhapKho.
*/
