package com.termux.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import com.squareup.picasso.Transformation;

public class CircleTransform implements Transformation {
    private final float insetPx;

    // #COMPLETION_DRIVE: Allow a small inset so content doesn’t collide with the stroke outline.
    // #SUGGEST_VERIFY: Visually inspect on multiple densities; tweak inset if content still touches the stroke.
    public CircleTransform(float insetPx) {
        this.insetPx = Math.max(0, insetPx);
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
        if (squared != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        float scale = 1f;
        if (squared.getWidth() != size || squared.getHeight() != size) {
            float sx = size / (float) squared.getWidth();
            float sy = size / (float) squared.getHeight();
            scale = Math.max(sx, sy);
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);

        float radius = (size / 2f) - insetPx;
        if (radius < 1f) radius = size / 2f;
        canvas.drawCircle(size / 2f, size / 2f, radius, paint);

        squared.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "circle(inset=" + insetPx + ")";
    }
}
