package com.example.lulu.autowxgenerateimg;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    private static final int WX_MAX_PIXEL = 5800000;//微信实际要求不能超过600万

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //申请权限
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

//    /**
//     * 动态申请权限
//     */
//    private void requestPermission() {
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE )
//                != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "请先赋予该应用权限", Toast.LENGTH_SHORT).show();
//        } else {
//
//        }
//
//    }

    /**
     * 权限回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 选取图片
     * @param view
     */
    public void selectPhoto(View view) {
        Intent intent = new Intent();
        /* 开启Pictures画面Type设定为image */
        intent.setType("image/*");
        /* 使用Intent.ACTION_GET_CONTENT这个Action */
        intent.setAction(Intent.ACTION_GET_CONTENT);
        /* 取得相片后返回本画面 */
        startActivityForResult(intent, 1);
    }



    //获取本地图片
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String img_url = uri.getPath();//这是本机的图片路径
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                generateBitmaps(bitmap);
                Log.d(TAG, "onActivityResult: bitmap.getWidth():" + bitmap.getWidth());
                Log.d(TAG, "onActivityResult: bitmap.getHeight():" + bitmap.getHeight());
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage(),e);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 生成适合微信公众号使用的图片
     * @param bitmap
     */
    private void generateBitmaps(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w * h < WX_MAX_PIXEL || w > h) {
            Toast.makeText(this, "图片无需处理", Toast.LENGTH_SHORT).show();
            return;
        }
        int bitmapCount = h / (WX_MAX_PIXEL / w) + 1;
        int newH = h / bitmapCount;//每个bitmap新的高度
        for (int i = 0; i < bitmapCount; i++) {
            Bitmap bmp = Bitmap.createBitmap(bitmap, 0, i * newH, w, newH, null,false);
            Log.d(TAG, "generateBitmaps: bmp: "+ bmp.getHeight());
            saveMyBitmap(bmp, String.valueOf(i+1));
            if (i == 0) {
                ((ImageView) findViewById(R.id.preview)).setImageBitmap(bmp);
            }
        }
    }
    public void saveMyBitmap(Bitmap mBitmap,String bitName)  {
        if (mBitmap == null) {
            return;
        }
        File f = new File( "/sdcard/1A生成的Bitmap/"+bitName + ".jpg");
        FileOutputStream fOut = null;
        try {
            if (!f.exists()) {
                f.getParentFile().mkdirs();
            }
            fOut = new FileOutputStream(f);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
