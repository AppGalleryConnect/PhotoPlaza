package com.huawei.agc.photoplaza.viewAndAdapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.huawei.agc.photoplaza.ImageListActivity;
import com.huawei.agc.photoplaza.PersonalDetailActivity;
import com.huawei.agc.photoplaza.R;
import com.huawei.agc.photoplaza.callbacklist.IOnItemLongClickListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RvAdapter extends RecyclerView.Adapter<RvAdapter.DataViewHolder> implements View.OnClickListener, View.OnLongClickListener {
    private Context mContext;
    private ArrayList<ImageObj> mList;
    private RecyclerView mrecyclerView;
    private ImageListActivity.OnItemClickListener onItemClickListener;
    private IOnItemLongClickListener onItemLongClickListener;
    private ImageButton deleteView;
    private PersonalDetailActivity.OnDeleteClickListener onDeleteClickListener;

    public RvAdapter() {
    }

    public RvAdapter(Context mContext, ArrayList<ImageObj> mList, RecyclerView recyclerView) {
        this.mContext = mContext;
        this.mList = mList;
        this.mrecyclerView = recyclerView;
        Log.d("RvAdapter", "this is --struct");
    }

    // Provides the setter method.
    public void setOnItemClickListener(ImageListActivity.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnDeleteClickListener(PersonalDetailActivity.OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public void setOnItemLongClickListener(IOnItemLongClickListener onItemlongClickListener) {
        this.onItemLongClickListener = onItemlongClickListener;
    }

    @NonNull
    @Override
    public DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.item_imageview, null);
        // Get screen width and height
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        int space = (int) (width / 3.0);
        int spaceM = (int) mContext.getResources().getDimension(R.dimen.space_m);
        int space1 = space - 2 * spaceM;

        // Set Parent Layout Size
        ImageView imageview = view.findViewById(R.id.item_imageview);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                space,
                space);
        view.setLayoutParams(params);

        // Setting the imageView size
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(
                space1,
                space1);
        imageview.setLayoutParams(params1);

        // Set Delete Icon Size
        deleteView = view.findViewById(R.id.delete);
        RelativeLayout.LayoutParams marginLayoutParas = new RelativeLayout.LayoutParams(space1 / 4, space1 / 4);
        deleteView.setLayoutParams(marginLayoutParas);

        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        return new DataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        // 绑定数据，这里使用的Uri直接设置图片，后续可以替换成网络imageView控件直接在家url展示
        Glide.with(mContext).load(mList.get(position).getShortUrl()).into(holder.Rv_imageView);
        holder.deleteView.setOnClickListener(v -> {
            if (holder.deleteView.getVisibility() == View.VISIBLE) {
                holder.deleteView.setVisibility(View.GONE);
            }
            onDeleteClickListener.onDeleteClick(position, mList.get(position));
        });
//        if (mList.get(position).getLocalPath() == null) {
//            // 对于下载失败的情况，设置加载失败的样式图 直接放在mipmap资源中
//            holder.Rv_imageview.setImageResource(R.mipmap.ic_loadfail);
//        } else {
//            try {
//                holder.Rv_imageview.setImageBitmap(UtilTool.getBitmapFromFile(mList.get(position).getLocalPath()));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void onClick(View v) {
        // Obtain the position of the current view based on the RecyclerView.
        int position = mrecyclerView.getChildAdapterPosition(v);
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, position, mList.get(position));
        }
        ImageButton deleteView1 = v.findViewById(R.id.delete);
        if (deleteView1.getVisibility() == View.VISIBLE) {
            deleteView1.setVisibility(View.GONE);
        }
        // The program executes the onItemClick() method.
    }

    @Override
    public boolean onLongClick(View v) {
        int position = mrecyclerView.getChildAdapterPosition(v);
        if (onItemLongClickListener != null) {
            onItemLongClickListener.onItemLongClick(v, position, mList.get(position));
        }
        return true;
    }

    // Create ViewHolder
    public static class DataViewHolder extends RecyclerView.ViewHolder {
        ImageView Rv_imageView;
        ImageButton deleteView;

        public DataViewHolder(View itemView) {
            super(itemView);
            Rv_imageView = itemView.findViewById(R.id.item_imageview);
            deleteView = itemView.findViewById(R.id.delete);
        }
    }
}



