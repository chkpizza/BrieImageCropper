package com.wantique.cropper

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Locale

fun getDeviceDensity(context: Context): Float {
    val displayMetrics = DisplayMetrics()
    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.density
}


fun getExifOrientation(context: Context, imageUri: Uri): Int {
    val authority = imageUri.authority?.lowercase(Locale.getDefault()) ?: return 0

    val orientation = if(authority.endsWith("media")) {
        getExifRotation(context, imageUri)
    } else {
        -1
    }
    return orientation
}

fun getExifRotation(context: Context, imageUri: Uri): Int {
    val projection = arrayOf(MediaStore.Images.ImageColumns.ORIENTATION)
    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(imageUri, projection, null, null, null)
        cursor?.let {
            if(!it.moveToFirst()) {
                return 0
            }
            return cursor.getInt(0)
        }
    } catch (e: Exception) {

    } finally {
        cursor?.close()
    }
    return 0
}