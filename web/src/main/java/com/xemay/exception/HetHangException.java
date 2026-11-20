package com.xemay.exception;

/** Số lượng tồn kho không đủ đáp ứng yêu cầu xuất kho / đặt hàng. */
public class HetHangException extends NghiepVuException {

    private final int soLuongCon;
    private final int soLuongYeuCau;

    public HetHangException(String tenXe, int soLuongCon, int soLuongYeuCau) {
        super("Xe \"" + tenXe + "\" chỉ còn " + soLuongCon + " chiếc, không đủ số lượng yêu cầu (" + soLuongYeuCau + ").");
        this.soLuongCon = soLuongCon;
        this.soLuongYeuCau = soLuongYeuCau;
    }

    public int getSoLuongCon() {
        return soLuongCon;
    }

    public int getSoLuongYeuCau() {
        return soLuongYeuCau;
    }
}
