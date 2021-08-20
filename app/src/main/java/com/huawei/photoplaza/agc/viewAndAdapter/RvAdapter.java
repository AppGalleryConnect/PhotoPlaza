package com.huawei.photoplaza.agc.viewAndAdapter;

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
import com.huawei.photoplaza.agc.ImageListActivity;
import com.huawei.photoplaza.agc.viewAndAdapter.ImageObj;
import com.huawei.photoplaza.agc.PersonalDetailActivity;
import com.huawei.photoplaza.agc.R;
import com.huawei.photoplaza.agc.callbacklist.IOnItemLongClickListener;

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
        Log.e("RvAdapter", "this is --struct");
    }

    // 提供setter方法
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
        // 获得屏幕宽和高。
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        int space = (int) (width / 3.0);
        int spaceM = (int) mContext.getResources().getDimension(R.dimen.space_m);
        int space1 = space - 2 * spaceM;

        // 设置父布局尺寸
        ImageView imageview = view.findViewById(R.id.item_imageview);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                space,
                space);
        view.setLayoutParams(params);

        // 设置imageview尺寸
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(
                space1,
                space1);
        imageview.setLayoutParams(params1);

       // 设置删除图标尺寸。
        deleteView = view.findViewById(R.id.delete);
        RelativeLayout.LayoutParams marginLayoutParas = new RelativeLayout.LayoutParams(space1 / 4, space1 / 4);
        deleteView.setLayoutParams(marginLayoutParas);

        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        DataViewHolder holder = new DataViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        // 绑定数据，这里使用的Uri直接设置图片，后续可以替换成网络imageview控件直接在家url展示
        Glide.with(mContext).load(mList.get(position).getImageUrl()).into(holder.Rv_imageview);
        holder.deleteView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (holder.deleteView.getVisibility() == View.VISIBLE) {
                    holder.deleteView.setVisibility(View.GONE);
                }
                onDeleteClickListener.onDeleteClick(position, mList.get(position));
            }

        });

//        if (mList.get(position).getLocalPath() == null) {
//
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
        //根据RecyclerView获得当前View的位置
        int position = mrecyclerView.getChildAdapterPosition(v);
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, position, mList.get(position));
        }
        ImageButton deleteView1 = v.findViewById(R.id.delete);
        if (deleteView1.getVisibility() == View.VISIBLE) {
            deleteView1.setVisibility(View.GONE);
        }
        //程序执行到此，会去执行具体实现的onItemClick()方法


    }

    @Override
    public boolean onLongClick(View v) {
        int position = mrecyclerView.getChildAdapterPosition(v);
        if (onItemLongClickListener != null) {
            onItemLongClickListener.onItemLongClick(v, position, mList.get(position));
        }
//        return (onItemClickListener == null);
        return true;
    }


    //创建ViewHolder
    public static class DataViewHolder extends RecyclerView.ViewHolder {
        ImageView Rv_imageview;
        ImageButton deleteView;

        public DataViewHolder(View itemView) {
            super(itemView);
            Rv_imageview = (ImageView) itemView.findViewById(R.id.item_imageview);
            deleteView = (ImageButton) itemView.findViewById(R.id.delete);
        }
    }
}



