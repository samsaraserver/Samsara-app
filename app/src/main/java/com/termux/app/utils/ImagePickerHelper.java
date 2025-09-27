package com.termux.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class ImagePickerHelper {
    public static final int REQUEST_IMAGE_GALLERY = 1001;
    public static final int REQUEST_IMAGE_CAMERA = 1002;
    public static final int REQUEST_CAMERA_PERMISSION = 1003;
    public static final int REQUEST_STORAGE_PERMISSION = 1004;

    public interface ImagePickerCallback {
        void onImageSelected(Uri imageUri);
        void onError(String error);
    }

    private Activity activity;
    private ImagePickerCallback callback;
    private Uri cameraImageUri;

    public ImagePickerHelper(Activity activity, ImagePickerCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Profile Picture");
        builder.setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    requestCameraImage();
                    break;
                case 1:
                    requestGalleryImage();
                    break;
            }
        });
        builder.show();
    }

    private void requestCameraImage() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(activity, "com.termux.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                activity.startActivityForResult(cameraIntent, REQUEST_IMAGE_CAMERA);
            } else {
                callback.onError("Failed to create image file");
            }
        } else {
            callback.onError("Camera not available");
        }
    }

    private void requestGalleryImage() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            return;
        }

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        activity.startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
    }

    private File createImageFile() {
        try {
            String imageFileName = "profile_" + System.currentTimeMillis();
            File storageDir = activity.getExternalFilesDir("Pictures");
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            return null;
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_IMAGE_GALLERY:
                if (data != null && data.getData() != null) {
                    callback.onImageSelected(data.getData());
                } else {
                    callback.onError("Failed to get image from gallery");
                }
                break;
            case REQUEST_IMAGE_CAMERA:
                if (cameraImageUri != null) {
                    callback.onImageSelected(cameraImageUri);
                } else {
                    callback.onError("Failed to get image from camera");
                }
                break;
        }
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestCameraImage();
                } else {
                    callback.onError("Camera permission denied");
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestGalleryImage();
                } else {
                    callback.onError("Storage permission denied");
                }
                break;
        }
    }
}