package com.huawei.agc.photoplaza.viewAndAdapter;

public class CouponItem {

    private final String InviteName;
    private final String BeInvitedName;
    private final String effectiveDate;
    private final String CouponPrice;

    public CouponItem(
            String inviteName,
            String beInvitedName,
            String effectiveTime,
            String price) {
        this.InviteName = inviteName;
        this.BeInvitedName = beInvitedName;
        this.effectiveDate = effectiveTime;
        this.CouponPrice = price;
    }

    public String getInviteName() {
        return InviteName;
    }

    public String getBeInviteName() {
        return BeInvitedName;
    }

    public String getDate() {
        return effectiveDate.substring(0, 10);
    }

    public String getCoupon() {
        return CouponPrice;
    }
}
