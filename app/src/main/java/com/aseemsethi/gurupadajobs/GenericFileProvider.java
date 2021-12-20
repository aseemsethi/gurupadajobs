package com.aseemsethi.gurupadajobs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

// /data/user/0/com.aseemsethi.inventory/files
// explains file paths -
// https://stackoverflow.com/questions/37074872/android-fileprovider-on-custom-external-storage-folder
public class GenericFileProvider extends FileProvider {
    private static String TAG = "Resumes: FP";
    private static File FilesDir;

    public static void sendData(Context ctx, String file) {
        FilesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        File file1 = ctx.getFileStreamPath("out.txt");
        if (file1 == null || !file1.exists()) {
            Log.d(TAG, "out.txt File not found !!!");
        }
        //Uri fileUri = Uri.fromFile(file1);
        Uri fileUri = FileProvider.getUriForFile(
                ctx,
                ctx.getApplicationContext()
                        .getPackageName() + ".provider", file1);
        Intent share1 = new Intent(Intent.ACTION_SEND);
        share1.putExtra(Intent.EXTRA_STREAM, fileUri);
        share1.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        share1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        share1.setPackage("com.whatsapp");
        share1.setType("*/*");
        ctx.startActivity(share1);
    }
}
