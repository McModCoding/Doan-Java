/* =====================================================================================
   SCRIPT PHẦN ĐĂNG NHẬP - PHÂN QUYỀN
   Bảng: vai_tro (3 dòng), tai_khoan (25 dòng), lich_su_dang_nhap (25 dòng)

   Cách chạy (SQL Server 2019 trở lên):
     - SSMS / Azure Data Studio: mở file, bấm Execute.
     - Dòng lệnh: sqlcmd -S localhost -U sa -P <mật khẩu> -C -f 65001 -i 01_tai_khoan_phan_quyen.sql
   Script XÓA và TẠO LẠI 3 bảng trên nên chạy lại bao nhiêu lần cũng được (dữ liệu trở về như ban đầu).

   Tài khoản mẫu - mật khẩu đều là 123456 (đã băm BCrypt, không lưu mật khẩu gốc):
     admin, quantri.lan            -> Quản trị viên
     nhanvien, nv.hung, nv.mai...  -> Nhân viên
     khachhang, nguyenvanan...     -> Khách hàng   (tathithu: tài khoản đang bị khóa)
   ===================================================================================== */

IF DB_ID(N'QuanLyXeMay') IS NULL
    CREATE DATABASE QuanLyXeMay COLLATE Vietnamese_CI_AS;
GO
USE QuanLyXeMay;
GO

/* Nếu bảng của thành viên khác (khach_hang, don_hang...) đang tham chiếu tai_khoan thì KHÔNG xóa được
   tai_khoan. Khi đó script dừng lại và không thay đổi gì: hãy chạy script gộp của nhóm (xóa bảng con trước). */
IF EXISTS (SELECT 1 FROM sys.foreign_keys
           WHERE referenced_object_id IN (OBJECT_ID(N'dbo.tai_khoan'), OBJECT_ID(N'dbo.vai_tro'))
             AND parent_object_id NOT IN (OBJECT_ID(N'dbo.tai_khoan'), OBJECT_ID(N'dbo.lich_su_dang_nhap')))
BEGIN
    RAISERROR(N'Đang có bảng khác tham chiếu tai_khoan/vai_tro. Hãy xóa các bảng đó trước hoặc chạy script gộp của nhóm. Script dừng, không thay đổi dữ liệu.', 16, 1);
    SET NOEXEC ON;
END
GO

-- Xóa bảng cũ: bảng con (có khóa ngoại) trước, bảng cha sau
IF OBJECT_ID(N'dbo.lich_su_dang_nhap', N'U') IS NOT NULL DROP TABLE dbo.lich_su_dang_nhap;
IF OBJECT_ID(N'dbo.tai_khoan', N'U') IS NOT NULL DROP TABLE dbo.tai_khoan;
IF OBJECT_ID(N'dbo.vai_tro', N'U') IS NOT NULL DROP TABLE dbo.vai_tro;
GO

/* ---------------------------------------------------------------- vai_tro
   Danh mục vai trò. Cột ma được Spring Security dùng làm tên quyền: ROLE_ + ma. */
CREATE TABLE dbo.vai_tro (
    id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_vai_tro PRIMARY KEY,
    ma   NVARCHAR(20)      NOT NULL CONSTRAINT UQ_vai_tro_ma UNIQUE,
    ten  NVARCHAR(50)      NOT NULL
);
GO

/* ---------------------------------------------------------------- tai_khoan
   Tài khoản đăng nhập của mọi người dùng. Quan hệ NHIỀU - MỘT với vai_tro. */
CREATE TABLE dbo.tai_khoan (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tai_khoan PRIMARY KEY,
    ten_dang_nhap  NVARCHAR(30)  NOT NULL CONSTRAINT UQ_tai_khoan_ten_dang_nhap UNIQUE,
    mat_khau       NVARCHAR(100) NOT NULL,                -- chuỗi băm BCrypt, 60 ký tự
    ho_ten         NVARCHAR(100) NOT NULL,
    email          NVARCHAR(100) NOT NULL CONSTRAINT UQ_tai_khoan_email UNIQUE,
    so_dien_thoai  NVARCHAR(15)  NULL,
    kich_hoat      BIT           NOT NULL CONSTRAINT DF_tai_khoan_kich_hoat DEFAULT (1),
    ngay_tao       DATETIME2(6)  NOT NULL CONSTRAINT DF_tai_khoan_ngay_tao DEFAULT (SYSDATETIME()),
    vai_tro_id     INT           NOT NULL CONSTRAINT FK_tai_khoan_vai_tro REFERENCES dbo.vai_tro (id),
    CONSTRAINT CK_tai_khoan_ten_dang_nhap CHECK (LEN(ten_dang_nhap) >= 4),
    CONSTRAINT CK_tai_khoan_so_dien_thoai CHECK (so_dien_thoai IS NULL
        OR so_dien_thoai LIKE N'0[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]')
);
CREATE INDEX IX_tai_khoan_vai_tro_id ON dbo.tai_khoan (vai_tro_id);
GO

/* ---------------------------------------------------------------- lich_su_dang_nhap
   Nhật ký đăng nhập (web và client Socket). Xóa tài khoản thì giữ nhật ký: tai_khoan_id về NULL. */
CREATE TABLE dbo.lich_su_dang_nhap (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_lich_su_dang_nhap PRIMARY KEY,
    ten_dang_nhap  NVARCHAR(100) NOT NULL,                -- tên người dùng đã gõ, có thể không tồn tại
    tai_khoan_id   BIGINT        NULL CONSTRAINT FK_lich_su_dang_nhap_tai_khoan
                       REFERENCES dbo.tai_khoan (id) ON DELETE SET NULL,
    thoi_gian      DATETIME2(6)  NOT NULL,
    dia_chi_ip     NVARCHAR(45)  NULL,
    kenh           NVARCHAR(10)  NOT NULL CONSTRAINT CK_lich_su_dang_nhap_kenh CHECK (kenh IN (N'WEB', N'SOCKET')),
    thanh_cong     BIT           NOT NULL,
    ghi_chu        NVARCHAR(255) NULL
);
CREATE INDEX IX_lich_su_dang_nhap_thoi_gian ON dbo.lich_su_dang_nhap (thoi_gian);
CREATE INDEX IX_lich_su_dang_nhap_ten_dang_nhap ON dbo.lich_su_dang_nhap (ten_dang_nhap);
GO

/* ================================================================ DỮ LIỆU MẪU */
INSERT INTO dbo.vai_tro (ma, ten) VALUES
    (N'ADMIN',     N'Quản trị viên'),
    (N'NHANVIEN',  N'Nhân viên'),
    (N'KHACHHANG', N'Khách hàng');
GO

-- Mật khẩu của mọi tài khoản dưới đây: 123456
INSERT INTO dbo.tai_khoan (ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, kich_hoat, ngay_tao, vai_tro_id)
SELECT v.ten_dang_nhap, v.mat_khau, v.ho_ten, v.email, v.so_dien_thoai, v.kich_hoat, v.ngay_tao, vt.id
FROM (VALUES
    (N'admin', N'$2a$10$8jCfBwMpUHtYt5sa8UbXa.XL5A1WzSVPq6zldKYoS0SzZV.v1hLli', N'Nguyễn Minh Quân', N'admin@xemay.vn', N'0901000001', 1, '2026-01-05T08:00:00', N'ADMIN'),
    (N'quantri.lan', N'$2a$10$CuV0pm48usQD2Wvv4Ri/GugPmEIr3S1bnauHyneZb.I2xZ25/WXHK', N'Phạm Thị Lan', N'lan.pham@xemay.vn', N'0901000004', 1, '2026-01-06T08:30:00', N'ADMIN'),
    (N'nhanvien', N'$2a$10$JVOWmeGfDb8USmImHfLdA.a3oBos9P5tzAhjt72ukPKeYw2GzVi4u', N'Nhân viên bán hàng', N'nhanvien@xemay.vn', N'0901000002', 1, '2026-01-05T08:05:00', N'NHANVIEN'),
    (N'nv.hung', N'$2a$10$6ArOo/WoqrdpTTCKnIae1uHulu70EtBpCcsaeDYJvjlyim0yIAEki', N'Lê Văn Hùng', N'hung.le@xemay.vn', N'0902000001', 1, '2026-01-10T09:00:00', N'NHANVIEN'),
    (N'nv.mai', N'$2a$10$.ktWhzQk852uUC42Pw5l9OINW29FhM3EQcs24nS0QHlhlizOUc6Yi', N'Nguyễn Thị Mai', N'mai.nguyen@xemay.vn', N'0902000002', 1, '2026-01-12T09:15:00', N'NHANVIEN'),
    (N'nv.tuan', N'$2a$10$bdNWi69LDDqnFbF7Fty//.tAXUXA9IdQ/sKboiPzVR5zQVpteuELS', N'Trần Minh Tuấn', N'tuan.tran@xemay.vn', N'0902000003', 1, '2026-02-01T10:00:00', N'NHANVIEN'),
    (N'nv.thao', N'$2a$10$U92oRFpVRjO8oXQOSkLiROiqaeRpcJVWXUnPHmYlseAEnOwdfAF.G', N'Võ Thu Thảo', N'thao.vo@xemay.vn', N'0902000004', 1, '2026-03-15T14:20:00', N'NHANVIEN'),
    (N'khachhang', N'$2a$10$61c3uwyxItLAOSryfsSnOe1K0eGsMDxxgMcyogw2T8cBcWW2bPPnm', N'Khách hàng mẫu', N'khachhang@xemay.vn', N'0901000003', 1, '2026-01-05T08:10:00', N'KHACHHANG'),
    (N'nguyenvanan', N'$2a$10$bcEf6/9LQyFcgSWIj/OnFeeq7RoIK1c3rZWHWgC1l5PnOcL9aWWla', N'Nguyễn Văn An', N'an.nguyen@gmail.com', N'0912000001', 1, '2026-02-03T19:12:00', N'KHACHHANG'),
    (N'tranthibich', N'$2a$10$P3zn1EvQd6zwKSWKyr0SJOcZlS38YxyZXfaDITFavRTqxqike8Jru', N'Trần Thị Bích', N'bich.tran@gmail.com', N'0912000002', 1, '2026-02-14T20:45:00', N'KHACHHANG'),
    (N'lequangcuong', N'$2a$10$Ss2InCNQzYeRQPXgEer6MOMHIDnN6BEdyHdkBqjVvg3pljnBRUiai', N'Lê Quang Cường', N'cuong.le@gmail.com', N'0912000003', 1, '2026-03-02T07:30:00', N'KHACHHANG'),
    (N'phamthidung', N'$2a$10$IJfx1xBBnE7Se9eVDEHQVO19rD1gjJ2x5ERi.uL7c67mvNixofvD2', N'Phạm Thị Dung', N'dung.pham@gmail.com', N'0912000004', 1, '2026-03-20T21:05:00', N'KHACHHANG'),
    (N'hoangvanem', N'$2a$10$BQZytsQ8sDrmMSc/3J6ssO2rtPIzE81Vq7HWB4RhJKNZcu/cIXPz2', N'Hoàng Văn Em', N'em.hoang@gmail.com', N'0912000005', 1, '2026-04-08T12:00:00', N'KHACHHANG'),
    (N'dangthiphuong', N'$2a$10$mr3DxZjbumH6PPCE/0DV5.GrtYfcialeMY1TO85nw4hXysHrQZC2q', N'Đặng Thị Phương', N'phuong.dang@gmail.com', N'0912000006', 1, '2026-04-25T16:40:00', N'KHACHHANG'),
    (N'buiducgiang', N'$2a$10$SDS3RBPvXYV1g.Q3GNvWmuJWLgV6Uz0GRP3jnoaL4tXvzfJ84SrRS', N'Bùi Đức Giang', N'giang.bui@gmail.com', N'0912000007', 1, '2026-05-11T09:25:00', N'KHACHHANG'),
    (N'dothihoa', N'$2a$10$QOZItLfG/NDvNDV9ynIEyOXZWKsz3STbpDgdy7/He.2oTZXmJZi7C', N'Đỗ Thị Hoa', N'hoa.do@gmail.com', N'0912000008', 1, '2026-05-30T18:10:00', N'KHACHHANG'),
    (N'ngoquockhanh', N'$2a$10$dJWDnyBHdcVI4tL8FxFfeuTGHcABpmTrscjpxaY/j3OPRM9ZNbCJm', N'Ngô Quốc Khánh', N'khanh.ngo@gmail.com', N'0912000009', 1, '2026-06-17T08:55:00', N'KHACHHANG'),
    (N'duongthilinh', N'$2a$10$JBMjN6l2XQGYXy/CI.9FyOgoopoUPwH4aGdre/qPAeQuTOBSLbXAu', N'Dương Thị Linh', N'linh.duong@gmail.com', N'0912000010', 1, '2026-07-04T13:35:00', N'KHACHHANG'),
    (N'lyvanminh', N'$2a$10$J7b6xg2zEnsrWaQ/.eRdrOKYRwBStBxf3GNz25yCfN0qwQq1fzkRy', N'Lý Văn Minh', N'minh.ly@gmail.com', N'0912000011', 1, '2026-07-22T20:00:00', N'KHACHHANG'),
    (N'vuthingoc', N'$2a$10$CC4qQUV1nWNLm.2lvvZUd.e67Jjv5FXN7dmdnQhK993C8Dl0crojm', N'Vũ Thị Ngọc', N'ngoc.vu@gmail.com', N'0912000012', 1, '2026-08-09T11:15:00', N'KHACHHANG'),
    (N'trinhvanphuc', N'$2a$10$7soD1v5WTk9M4j6YSa11v.adkgyvsJlhfESRLtw1WQTULcnh5gHXu', N'Trịnh Văn Phúc', N'phuc.trinh@gmail.com', NULL, 1, '2026-08-28T15:50:00', N'KHACHHANG'),
    (N'maithiquynh', N'$2a$10$L8l7WWfKcdtJLHMPCzVtUudpM7XHivcax6Av4gl1fm9jHMFucdfhW', N'Mai Thị Quỳnh', N'quynh.mai@gmail.com', N'0912000014', 1, '2026-09-13T10:05:00', N'KHACHHANG'),
    (N'caovanson', N'$2a$10$q8p9x9MC4TchDfG99Jx8TuGALYiu/wGAxfvCsP8S6luUlXTQ.0eZy', N'Cao Văn Sơn', N'son.cao@gmail.com', N'0912000015', 1, '2026-10-01T19:40:00', N'KHACHHANG'),
    (N'tathithu', N'$2a$10$12QPIQVYql/ibYV/Y.npK./4CkpxMXgJCruKw419v17Yj0XjqWHDa', N'Tạ Thị Thu', N'thu.ta@gmail.com', N'0912000016', 0, '2026-10-19T17:25:00', N'KHACHHANG'),
    (N'luongvantoan', N'$2a$10$w079K8SHoAz8DDt/Ks9T.eqbEhNp1t3dVWDGslTSoNZk18wj.HRoG', N'Lương Văn Toàn', N'toan.luong@gmail.com', N'0912000017', 1, '2026-11-02T09:30:00', N'KHACHHANG')
) AS v (ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, kich_hoat, ngay_tao, ma_vai_tro)
JOIN dbo.vai_tro vt ON vt.ma = v.ma_vai_tro;
GO

-- Nhật ký đăng nhập mẫu, thời gian tính lùi từ lúc chạy script (phut_truoc = số phút trước)
INSERT INTO dbo.lich_su_dang_nhap (ten_dang_nhap, tai_khoan_id, thoi_gian, dia_chi_ip, kenh, thanh_cong, ghi_chu)
SELECT v.ten_dang_nhap, tk.id, DATEADD(MINUTE, -v.phut_truoc, SYSDATETIME()), v.dia_chi_ip, v.kenh, v.thanh_cong, v.ghi_chu
FROM (VALUES
    (N'admin', 10080, N'192.168.1.10', N'WEB', 1, NULL),
    (N'nhanvien', 9000, N'192.168.1.21', N'WEB', 1, NULL),
    (N'nhanvien', 8990, N'192.168.1.21', N'SOCKET', 1, NULL),
    (N'nguyenvanan', 8500, N'113.161.20.5', N'WEB', 1, NULL),
    (N'tranthibich', 8000, N'14.232.11.8', N'WEB', 0, N'Sai tên đăng nhập hoặc mật khẩu'),
    (N'tranthibich', 7995, N'14.232.11.8', N'WEB', 1, NULL),
    (N'admin123', 7000, N'45.77.10.99', N'WEB', 0, N'Sai tên đăng nhập hoặc mật khẩu'),
    (N'admin123', 6999, N'45.77.10.99', N'WEB', 0, N'Sai tên đăng nhập hoặc mật khẩu'),
    (N'nv.hung', 6500, N'192.168.1.22', N'SOCKET', 1, NULL),
    (N'khachhang', 6000, N'192.168.1.50', N'SOCKET', 0, N'Vai trò không được phép dùng kênh SOCKET'),
    (N'lequangcuong', 5500, N'171.244.3.17', N'WEB', 1, NULL),
    (N'phamthidung', 5000, N'27.72.98.4', N'WEB', 1, NULL),
    (N'tathithu', 4500, N'118.70.12.6', N'WEB', 0, N'Tài khoản đang bị khóa'),
    (N'hoangvanem', 4000, N'42.112.7.1', N'WEB', 0, N'Sai tên đăng nhập hoặc mật khẩu'),
    (N'hoangvanem', 3998, N'42.112.7.1', N'WEB', 1, NULL),
    (N'nv.mai', 3500, N'192.168.1.23', N'WEB', 1, NULL),
    (N'dangthiphuong', 3000, N'113.185.40.2', N'WEB', 1, NULL),
    (N'quantri.lan', 2500, N'192.168.1.11', N'WEB', 1, NULL),
    (N'buiducgiang', 2000, N'1.54.22.9', N'WEB', 1, NULL),
    (N'nv.tuan', 1500, N'192.168.1.24', N'SOCKET', 1, NULL),
    (N'dothihoa', 1000, N'116.96.45.3', N'WEB', 1, NULL),
    (N'ngoquockhanh', 600, N'103.7.36.12', N'WEB', 0, N'Sai tên đăng nhập hoặc mật khẩu'),
    (N'nhanvien', 300, N'192.168.1.21', N'SOCKET', 1, NULL),
    (N'admin', 120, N'192.168.1.10', N'WEB', 1, NULL),
    (N'duongthilinh', 30, N'14.161.2.20', N'WEB', 1, NULL)
) AS v (ten_dang_nhap, phut_truoc, dia_chi_ip, kenh, thanh_cong, ghi_chu)
LEFT JOIN dbo.tai_khoan tk ON tk.ten_dang_nhap = v.ten_dang_nhap;
GO

SET NOEXEC OFF;
GO

-- Kiểm tra nhanh
SELECT N'vai_tro' AS bang, COUNT(*) AS so_dong FROM dbo.vai_tro
UNION ALL SELECT N'tai_khoan', COUNT(*) FROM dbo.tai_khoan
UNION ALL SELECT N'lich_su_dang_nhap', COUNT(*) FROM dbo.lich_su_dang_nhap;
GO
