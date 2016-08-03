package com.zrp.gifmakerdemo;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.zrp.gifmakerdemo.gifmaker.AnimatedGifEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * http://blog.csdn.net/loongggdroid/article/details/21166563
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private GridView grid_view;
    private GifView gif_image;
    private EditText file_text;
    private SeekBar delay_bar;
    String defaultPath = Environment.getExternalStorageDirectory().getPath() + "/GIFMakerDemo/demo1.gif";

    public static final String TAG = "MainActivity";
    public static final int START_ALBUM_CODE = 0x21;

    private List<String> pics = new ArrayList<>();
    private PhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        grid_view = (GridView) findViewById(R.id.grid_view);
        file_text = (EditText) findViewById(R.id.file_text);
        delay_bar = (SeekBar) findViewById(R.id.delay_bar);
        gif_image = (GifView) findViewById(R.id.gif_image);
        findViewById(R.id.generate).setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
//        gif_image.setImageBitmap(setGif(defaultPath));
        setMovie(defaultPath);
        gif_image.setMovie(mMovie);
//        gif_image.setImageBitmap(getLocalBitmap(defaultPath));
//        gif_image.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        adapter = new PhotoAdapter(this, null);
        grid_view.setAdapter(adapter);
    }

//    public Bitmap setGif(String path){
//        Uri gif_uri=Uri.parse(path); //图片地址
//        ContentResolver cr=this.getContentResolver();
//        Bitmap bmp = null;
//        try {
//            bmp = BitmapFactory.decodeStream(cr.openInputStream(gif_uri));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        return bmp;
//    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.generate://生成gif图
                Toast.makeText(MainActivity.this, "开始生成Gif图", Toast.LENGTH_SHORT).show();

                final String file_name = file_text.getText().toString();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        createGif(TextUtils.isEmpty(file_name) ? "demo1" : file_name, delay_bar.getProgress());
                    }
                }).start();
                break;
            case R.id.clear:
                clearData();
                break;
        }
    }

    /**
     * 清除当前的数据内容
     */
    private void clearData() {
        pics.clear();
        adapter.setList(null);
        gif_image.setMovie(null);
//        gif_image.setImageDrawable(null);
    }

    /**
     * 生成gif图
     *
     * @param delay 图片之间间隔的时间
     */
    private void createGif(String file_name, int delay) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final AnimatedGifEncoder localAnimatedGifEncoder = new AnimatedGifEncoder();
        localAnimatedGifEncoder.start(baos);//start
        localAnimatedGifEncoder.setRepeat(0);//设置生成gif的开始播放时间。0为立即开始播放
        localAnimatedGifEncoder.setDelay(delay);
        if (pics.isEmpty()) {
            localAnimatedGifEncoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.pic_1));
            localAnimatedGifEncoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.pic_2));
            localAnimatedGifEncoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.pic_3));
        } else {
            for (int i = 0; i < pics.size(); i++) {
                // Bitmap localBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(pics.get(i)), 512, 512);
                BitmapFactory.Options option = new BitmapFactory.Options();
                File f = new File(pics.get(i));
                double size = f.length() / 1024.0;
                double result = size / 100.0;
                int multi;
                if (f.getName().contains(".gif"))
                    if (result < 3)
                        multi = 1;
                    else
                        multi = 2;
                else {
                    if (result < 1)//小于100kb
                        multi = 1;
                    else if (result < 5)//100-500kb
                        multi = 2;
                    else if (result < 15)//500kb-1.5mb
                        multi = 4;
                    else if (result < 60)//1.5-6mb
                        multi = 8;
                    else//6mb以上
                        multi = 16;
                }
                Log.d("Main", "multi " + multi + "/" + size + "kb");
                option.inSampleSize = multi;
                localAnimatedGifEncoder.addFrame(BitmapFactory.decodeFile(pics.get(i), option));
            }
        }
        localAnimatedGifEncoder.finish();//finish

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/GIFMakerDemo");
        if (!file.exists()) file.mkdir();
        final String path = Environment.getExternalStorageDirectory().getPath() + "/GIFMakerDemo/" + file_name + ".gif";
        Log.d(TAG, "createGif: ---->" + path);

        try {
            FileOutputStream fos = new FileOutputStream(path);
            baos.writeTo(fos);
            baos.flush();
            fos.flush();
            baos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setMovie(defaultPath);
                gif_image.setMovie(mMovie);
//                gif_image.setImageBitmap(getLocalBitmap(path));
//                gif_image.setImageURI(Uri.parse(path));//.setImageURI(Uri.parse(path));
                Toast.makeText(MainActivity.this, "Gif已生成。保存路径：\n" + path, Toast.LENGTH_LONG).show();
            }
        });
    }

//    public static Bitmap getLocalBitmap(String url) {
//        try {
//            FileInputStream fis = new FileInputStream(url);
//            return BitmapFactory.decodeStream(fis);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
    private static Movie mMovie;
//    public static Bitmap getLocalBitmap1(String url) {
//        try {
//            FileInputStream fis = new FileInputStream(url);
//            byte[] bytes = streamToBytes(fis);
//            mMovie = Movie.decodeByteArray(bytes, 0, bytes.length);
//            return BitmapFactory.decodeStream(fis);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static void setMovie(String url){
        try {
            FileInputStream fis = new FileInputStream(url);
            byte[] bytes = streamToBytes(fis);
            mMovie = Movie.decodeByteArray(bytes, 0, bytes.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static byte[] streamToBytes(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = is.read(buffer)) >= 0) {
                os.write(buffer, 0, len);
            }
        } catch (java.io.IOException e) {
        }
        return os.toByteArray();
    }

    /**
     * 打开系统图库选择图片
     */
    public void photoPick() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, START_ALBUM_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1) {
            Uri localUri = data.getData();
            String[] arrayOfString = {"_data"};
            Cursor localCursor = getContentResolver().query(localUri, arrayOfString, null, null, null);
            localCursor.moveToFirst();
            String str = localCursor.getString(localCursor.getColumnIndex(arrayOfString[0]));
            localCursor.close();
            pics.add(str);

            Log.d(TAG, "onActivityResult: ----->" + pics.toString());
            adapter.setList(pics);
        }
    }
}
