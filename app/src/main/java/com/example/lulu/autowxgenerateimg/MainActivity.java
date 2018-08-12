package com.example.lulu.autowxgenerateimg;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity implements Handler.Callback{

    public static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    private static final int WX_MAX_PIXEL = 5800000;//微信实际要求不能超过600万
    public static final String ROOT_PATH = "/sdcard/1A生成的Bitmap/";
    private LinearLayout mRootView;
    private EditText topPxEdit;
    private int topPx = 267;
    private int bottomPx = 459;
    private TextView loading;
    private static final int GENERATE_BITMAP = 1;
    private static final int ALL_SUCCESS  = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new WeakReferenceHandler(this);
        mRootView = ((LinearLayout) findViewById(R.id.root_view));
        topPxEdit = findViewById(R.id.top_px);
        loading = findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        //bottomPxEdit = findViewById(R.id.bottom_px);
        topPxEdit.setHint(String.valueOf(topPx));
        //bottomPxEdit.setHint(String.valueOf(bottomPx));
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
        loading.setText("正在生成,请稍后...");
        String s = topPxEdit.getText().toString();
        if (TextUtils.isEmpty(s)) {
            s = String.valueOf(topPx);
        }
        topPx = Integer.parseInt(s);
        //bottomPx = Integer.parseInt(bottomPxEdit.getText().toString());
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
            loading.setVisibility(View.VISIBLE);
            final Uri uri = data.getData();
            String img_url = uri.getPath();//这是本机的图片路径
            final ContentResolver cr = this.getContentResolver();
            try {
                mRootView.removeAllViews();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = null;
                        try {
                            bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                            generateBitmaps(bitmap);
                            Log.d(TAG, "onActivityResult: bitmap.getWidth():" + bitmap.getWidth());
                            Log.d(TAG, "onActivityResult: bitmap.getHeight():" + bitmap.getHeight());
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


            } catch (Exception e) {
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
            int yIndex = i * newH;
            int height = newH;
            if (i == 0) {
                yIndex = topPx;//从顶部设置的距离开始
                height = newH - topPx;
            }
            Bitmap bmp = Bitmap.createBitmap(bitmap, 0, yIndex, w, height, null,false);
            Log.d(TAG, "generateBitmaps: bmp: "+ bmp.getHeight());
            String fileName = String.valueOf(i+1);
            saveMyBitmap(bmp, fileName);
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString("fileName",fileName);
            bundle.putParcelable("bmp",bmp);
            msg.setData(bundle);
            msg.what = GENERATE_BITMAP;
            mHandler.sendMessage(msg);
        }
        mHandler.sendEmptyMessage(ALL_SUCCESS);
    }

    /**
     * 保存裁剪好的Bitmap
     * @param mBitmap
     * @param bitName
     */
    public void saveMyBitmap(Bitmap mBitmap,String bitName)  {
        if (mBitmap == null) {
            return;
        }
        File f = new File( ROOT_PATH + bitName + ".jpg");
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

    private WeakReferenceHandler mHandler;

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case GENERATE_BITMAP:

                LinearLayout.LayoutParams params
                        = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                Bundle bundle = msg.getData();
                String fileName = bundle.getString("fileName");
                Bitmap bmp = bundle.getParcelable("bmp");
                Toast.makeText(this, "成功生成: " + fileName + ".jpg", Toast.LENGTH_SHORT).show();
                ImageView imageView = new ImageView(this);
                imageView.setLayoutParams(params);
                imageView.setImageBitmap(bmp);
                TextView textView = new TextView(this);
                textView.setLayoutParams(params);
                textView.setGravity(Gravity.CENTER);
                textView.setText(fileName + ".jpg");
                textView.setPadding(0, 10, 0, 20);
                textView.setTextColor(Color.RED);
                mRootView.addView(imageView);
                mRootView.addView(textView);
                return true;
            case ALL_SUCCESS:
                loading.setText("已经保存到: " + ROOT_PATH + " 目录下!");
                System.gc();
        }

        return false;
    }
}
