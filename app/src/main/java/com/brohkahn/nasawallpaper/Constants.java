package com.brohkahn.nasawallpaper;

import android.graphics.BitmapFactory;

public final class Constants {
    public static final String SET_WALLPAPER_ACTION = "set_wallpaper";
    public static final String WALLPAPER_UPDATED = "wallpaper_updated";

    public static int getImageScale(String imagePath, int outputWidth, int outputHeight) {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bitmapOptions);

        // Calculate inSampleSize
        int imageHeight = bitmapOptions.outHeight;
        int imageWidth = bitmapOptions.outWidth;
        int inSampleSize = 1;

        while (imageWidth > outputWidth && imageHeight > outputHeight) {
            imageHeight /= 2;
            imageWidth /= 2;
            inSampleSize *= 2;
        }

        return inSampleSize;
    }
}
