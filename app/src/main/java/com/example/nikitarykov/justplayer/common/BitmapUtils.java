package com.example.nikitarykov.justplayer.common;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.nikitarykov.justplayer.R;

import java.io.IOException;

public class BitmapUtils {

    public static Bitmap loadBitmap(Context context, Long id) {
        Uri uri = ContentUris.withAppendedId(Uri.parse(context.getString(R.string.albumart_uri)), id);

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_music);
        }
        return bitmap;
    }
    
}
