package com.huawei.agc.photoplaza.viewAndAdapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.LayoutInflater;

import com.huawei.agc.photoplaza.R;

import java.util.ArrayList;

public class CommentAdapter extends ArrayAdapter<CommentItem> {

    private final int resourceId;

    public CommentAdapter(Context context, int textViewResourceId, ArrayList<CommentItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        // Obtain the fruit instance to be displayed based on position
        CommentItem mCommentItem = getItem(position);
        if (convertView == null) {
            // Obtains subview control instances
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            holder.userName = convertView.findViewById(R.id.item_user);
            holder.commentContent = convertView.findViewById(R.id.user_comment);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.userName.setText(mCommentItem.getName());

        holder.commentContent.setText(mCommentItem.getComment());
        return convertView;
    }

    static class ViewHolder {
        TextView userName;
        TextView commentContent;
    }
}