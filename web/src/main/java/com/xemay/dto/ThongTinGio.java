package com.xemay.dto;

import java.math.BigDecimal;
import java.util.List;

public record ThongTinGio(List<MatHangGio> dsMatHang, int soLuongXe, BigDecimal tongTien) {
    public boolean rong() {return dsMatHang.isEmpty();}
}
