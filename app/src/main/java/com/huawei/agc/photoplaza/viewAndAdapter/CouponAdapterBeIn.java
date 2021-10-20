package com.huawei.agc.photoplaza.viewAndAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.huawei.agc.photoplaza.R;

import java.util.ArrayList;

public class CouponAdapterBeIn extends ArrayAdapter<CouponItem> {

    private final int resourceId;

    public CouponAdapterBeIn(Context context, int textViewResourceId, ArrayList<CouponItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        // Obtain the fruit instance to be displayed based on position
        CouponItem mCouponItem = getItem(position);
        if (convertView == null) {
            // Obtains subview control instances
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            holder.beInvitedName = convertView.findViewById(R.id.beInvited_name);
            holder.effectiveDate = convertView.findViewById(R.id.effective_time);
            holder.couponPrice = convertView.findViewById(R.id.price);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.beInvitedName.setText(mCouponItem.getInviteName());
        holder.effectiveDate.setText(mCouponItem.getDate());
        holder.couponPrice.setText(mCouponItem.getCoupon());

        return convertView;
    }

    static class ViewHolder {
        TextView beInvitedName;
        TextView effectiveDate;
        TextView couponPrice;
    }
}