package com.epam.itweek.layoutmanager.adapter;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.View;

import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;

/**
 * Picasso transformation to center crop source bitmap based on dimensions of the target view.
 */
public class DeferredResizeTransformation implements Transformation {

    private WeakReference<View> target;

    public DeferredResizeTransformation(View target) {
        this.target = new WeakReference<View>(target);
    }

    @Override
    public Bitmap transform(Bitmap source) {
        return centerCrop(source);
    }

    private Bitmap centerCrop(Bitmap source) {
        Matrix matrix = new Matrix();

        View v = target.get();
        if (v == null) {
            return source;
        }

        int inWidth = source.getWidth();
        int inHeight = source.getHeight();

        int drawX = 0;
        int drawY = 0;

        int drawWidth = inWidth;
        int drawHeight = inHeight;

        float widthRatio = v.getMeasuredWidth() / (float) inWidth;
        float heightRatio = v.getMeasuredHeight() / (float) inHeight;
        float scale;
        if (widthRatio > heightRatio) {
            scale = widthRatio;
            int newSize = (int) Math.ceil(inHeight * (heightRatio / widthRatio));
            drawY = (inHeight - newSize) / 2;
            drawHeight = newSize;
        } else {
            scale = heightRatio;
            int newSize = (int) Math.ceil(inWidth * (widthRatio / heightRatio));
            drawX = (inWidth - newSize) / 2;
            drawWidth = newSize;
        }

        matrix.preScale(scale, scale);

        Bitmap newResult = Bitmap.createBitmap(source, drawX, drawY, drawWidth, drawHeight, matrix, true);
        if (newResult != source) {
            source.recycle();
            source = newResult;
        }

        return source;
    }

    @Override
    public String key() {
        View v = target.get();
        if (v == null) {
            return "DeferredResizeTransformation_null";
        }
        return "DeferredResizeTransformation_" + v.getMeasuredWidth() + "x" + v.getMeasuredHeight();
    }

}
