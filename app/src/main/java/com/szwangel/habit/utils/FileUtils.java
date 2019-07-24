package com.szwangel.habit.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class FileUtils {
    public static String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "habit" + File.separator + "Habit";

    public static File createFile(Context context, String fileName) {
        String state = Environment.getExternalStorageState();

        File file, dir;
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            dir = new File(dirPath);
            file = new File(dirPath + File.separator + fileName);
            try {
                dir.mkdirs();
                if (file.exists()) {
                    if (file.length() != 0)
                        return file;
                    else
                        file.delete();
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(dir.getAbsolutePath() + "\n" + file.getAbsolutePath());
            }
        } else {
//            file = new File(context.getCacheDir().getAbsolutePath() + "/habit/pic/" + fileName);
            file = null;
        }
        return file;
    }

    public static void writeFile2Disk(Response<ResponseBody> response, File file) {
        OutputStream os = null;
        InputStream is = response.body().byteStream();
        try {
            os = new FileOutputStream(file);
            int len;
            byte[] buff = new byte[1024];
            while ((len = is.read(buff)) != -1) {
                os.write(buff, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File createImageFile() {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File image = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
            String imageFileName = "Capture_" + timeStamp;
            image = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
