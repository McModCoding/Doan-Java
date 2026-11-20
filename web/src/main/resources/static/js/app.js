/*
 * Script dùng chung cho mọi trang.
 *  1. Hộp xác nhận trước khi gửi form: thêm data-xac-nhan="Câu hỏi" vào thẻ <form>.
 *     Tùy chọn: data-nut-xac-nhan="Xóa" (chữ trên nút), data-kieu-nut="btn-warning" (màu nút).
 *  2. Nút hiện/ẩn mật khẩu: <button type="button" data-hien-mat-khau="idCuaOMatKhau">.
 */
document.addEventListener('DOMContentLoaded', function () {
    var hop = document.getElementById('hopXacNhan');
    var coModal = hop && window.bootstrap && window.bootstrap.Modal;
    var modal = coModal ? new bootstrap.Modal(hop) : null;
    var nutDongY = document.getElementById('hopXacNhanDongY');
    var formDangCho = null;

    document.querySelectorAll('form[data-xac-nhan]').forEach(function (form) {
        form.addEventListener('submit', function (suKien) {
            if (form.dataset.daXacNhan === 'true') {
                return;
            }
            suKien.preventDefault();
            if (!modal) {
                // Không tải được Bootstrap: dùng hộp xác nhận có sẵn của trình duyệt
                if (window.confirm(form.dataset.xacNhan)) {
                    form.dataset.daXacNhan = 'true';
                    form.submit();
                }
                return;
            }
            formDangCho = form;
            document.getElementById('hopXacNhanNoiDung').textContent = form.dataset.xacNhan;
            nutDongY.textContent = form.dataset.nutXacNhan || 'Đồng ý';
            nutDongY.className = 'btn ' + (form.dataset.kieuNut || 'btn-danger');
            modal.show();
        });
    });

    if (nutDongY) {
        nutDongY.addEventListener('click', function () {
            if (formDangCho) {
                formDangCho.dataset.daXacNhan = 'true';
                nutDongY.disabled = true;   // tránh bấm hai lần
                formDangCho.submit();
            }
        });
    }
    if (hop) {
        hop.addEventListener('hidden.bs.modal', function () {
            nutDongY.disabled = false;
        });
    }

    document.querySelectorAll('[data-hien-mat-khau]').forEach(function (nut) {
        nut.addEventListener('click', function () {
            var o = document.getElementById(nut.dataset.hienMatKhau);
            if (!o) {
                return;
            }
            var dangAn = o.type === 'password';
            o.type = dangAn ? 'text' : 'password';
            nut.innerHTML = dangAn ? '<i class="bi bi-eye-slash"></i>' : '<i class="bi bi-eye"></i>';
            nut.setAttribute('aria-label', dangAn ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
            nut.title = dangAn ? 'Ẩn mật khẩu' : 'Hiện mật khẩu';
        });
    });
});
