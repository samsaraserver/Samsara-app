package com.termux.app.database.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    public static final int PROFILE_IMAGE_SIZE = 512;
    private static final int JPEG_QUALITY = 85;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public static byte[] processImageFromUri(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream from URI");
                return null;
            }

            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from stream");
                return null;
            }

            Bitmap rotatedBitmap = rotateImageIfRequired(context, originalBitmap, imageUri);
            Bitmap squared = cropCenterSquare(rotatedBitmap);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(squared, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE, true);

            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle();
            }
            if (squared != rotatedBitmap && squared != originalBitmap && squared != resizedBitmap) {
                squared.recycle();
            }
            if (originalBitmap != resizedBitmap) originalBitmap.recycle();

            byte[] imageData = compressImage(resizedBitmap, JPEG_QUALITY);
            resizedBitmap.recycle();

            if (imageData != null && imageData.length > MAX_FILE_SIZE) {
                Log.e(TAG, "Image size exceeds 5MB limit: " + imageData.length + " bytes");
                return null;
            }

            return imageData;

        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage(), e);
            return null;
        }
    }

    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) {
        try {
            InputStream input = context.getContentResolver().openInputStream(selectedImage);
            if (input == null) return img;

            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            input.close();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading EXIF data: " + e.getMessage());
            return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        if (rotatedImg != img) {
            img.recycle();
        }
        return rotatedImg;
    }

    private static Bitmap cropCenterSquare(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        return Bitmap.createBitmap(image, x, y, size, size);
    }

    private static byte[] compressImage(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    public static boolean isValidImageSize(byte[] imageData) {
        return imageData != null && imageData.length <= MAX_FILE_SIZE;
    }
}