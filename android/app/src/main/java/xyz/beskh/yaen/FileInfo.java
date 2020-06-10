package xyz.beskh.yaen;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.content.ContentResolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FileInfo {
    private String fileName;
    private String fullFileName;
    private Uri uri;

    public FileInfo() {
        super();
    }

    public FileInfo(Uri uri, @Nullable ContentResolver resolver) {
        super();
        setUri(uri, resolver);
    }

    public static String getFileName(@NonNull Uri uri, @Nullable ContentResolver resolver) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content") && resolver != null) {
            try {
                Cursor cursor = resolver.query(uri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            } catch (Exception e) {
                result = null;
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFullFileName() {
        return fullFileName;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri, @Nullable ContentResolver resolver) {
        this.uri = uri;
        this.fileName = getFileName(uri, resolver);
        this.fullFileName = uri.getEncodedPath();
    }
}
