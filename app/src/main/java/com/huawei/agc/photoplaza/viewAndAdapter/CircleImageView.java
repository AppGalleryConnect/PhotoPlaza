package com.huawei.agc.photoplaza.viewAndAdapter;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.huawei.agc.photoplaza.R;

public class CircleImageView extends AppCompatImageView {
    private final Paint mPaintBitmap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mRawBitmap;
    private BitmapShader mShader;
    private final Matrix mMatrix = new Matrix();
    private final float mBorderWidth = dip2px(4);
    private final int mBorderColor = getResources().getColor(R.color.colorMilkWite);

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap rawBitmap = getBitmap(getDrawable());
        if (rawBitmap != null) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int viewMinSize = Math.min(viewWidth, viewHeight);
            if (mShader == null || !rawBitmap.equals(mRawBitmap)) {
                mRawBitmap = rawBitmap;
                mShader = new BitmapShader(mRawBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            }
            if (mShader != null) {
                mMatrix.setScale(((float) viewMinSize - mBorderWidth * 2) / rawBitmap.getWidth(), ((float) viewMinSize - mBorderWidth * 2) / rawBitmap.getHeight());
                mShader.setLocalMatrix(mMatrix);
            }
            mPaintBitmap.setShader(mShader);
            mPaintBorder.setStyle(Paint.Style.STROKE);
            mPaintBorder.setStrokeWidth(mBorderWidth);
            mPaintBorder.setColor(mBorderColor);
            mPaintBorder.setAntiAlias(true);
            BlurMaskFilter maskFilter = new BlurMaskFilter(3, BlurMaskFilter.Blur.SOLID);
            mPaintBorder.setMaskFilter(maskFilter);

            PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
            mPaintBorder.setXfermode(xfermode);

            float radius = viewMinSize / 2.0f;
            canvas.drawCircle(radius, radius, radius - mBorderWidth / 2.0f, mPaintBorder);

            canvas.translate(mBorderWidth, mBorderWidth);
            canvas.drawCircle(radius - mBorderWidth, radius - mBorderWidth, radius - mBorderWidth, mPaintBitmap);
        } else {
            super.onDraw(canvas);
        }
    }

    private Bitmap getBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof ColorDrawable) {
            Rect rect = drawable.getBounds();
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;
            int color = ((ColorDrawable) drawable).getColor();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
            return bitmap;
        } else {
            return null;
        }
    }

    private int dip2px(int dipVal) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dipVal * scale + 0.5f);
    }
}







