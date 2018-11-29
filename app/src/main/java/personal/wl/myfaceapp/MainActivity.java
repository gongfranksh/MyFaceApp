package personal.wl.myfaceapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.aip.face.AipFace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Button getImage, detect, Camera;
    private ImageView myPhoto;
    private Bitmap myBitmapImage = null;
    private String ImagePath = null;
    private TextView detect_tip;
    private String face_resultNum = null, face_age = null, face_gender = null, face_race = null, face_beauty = null, face_expression = null;
    private Uri imageUri;
    private int PHOTO_ALBUM = 1, CAMERA = 2;
    private JSONObject res = null;
    private Bitmap bitmapSmall;
    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = MainActivity.this;
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        readRequest();


        getImage = (Button) findViewById(R.id.getImage);
        myPhoto = (ImageView) findViewById(R.id.myPhoto);
        detect = (Button) findViewById(R.id.detect);
        Camera = (Button) findViewById(R.id.Camera);
        detect_tip = (TextView) findViewById(R.id.detect_tip);
        getImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(Intent.ACTION_PICK);
                in.setType("image/*");
                startActivityForResult(in, PHOTO_ALBUM);
            }
        });


        Camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File outputImage = new File(Environment.getExternalStorageDirectory() + File.separator + "face.jpg");
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(outputImage);
                ImagePath = outputImage.getAbsolutePath();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, CAMERA);
//                detect_tip.setVisibility(View.VISIBLE);

            }
        });


        detect_tip.setVisibility(View.GONE);
        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                res = null;
                detect_tip.setVisibility(View.VISIBLE);
                detect_tip.setText("识别中...");
                if (myBitmapImage == null) {
                    myBitmapImage = BitmapFactory.decodeResource(getResources(), R.mipmap.face2);
                    bitmapSmall = Bitmap.createBitmap(myBitmapImage, 0, 0, myBitmapImage.getWidth(), myBitmapImage.getHeight());
                } else {
                    int degree = getPicRotate(ImagePath);
                    Matrix m = new Matrix();
                    m.setRotate(degree);
                    bitmapSmall = Bitmap.createBitmap(myBitmapImage, 0, 0, myBitmapImage.getWidth(), myBitmapImage.getHeight(), m, true);
                }
                //将图片由路径转为二进制数据流
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                //图片转数据流
                bitmapSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                final byte[] arrays = stream.toByteArray();

                final String strbm = Base64.encodeToString(arrays, Base64.DEFAULT);


                //网络申请调用函数进行人脸识别
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> options = new HashMap<>();
                        options.put("face_field", "age,gender,race,beauty,expression,type");
                        AipFace client = new AipFace("14967631", "zUrmexSXf8vOciCrDSKwHCW8", "qEFCo8bXbjNqTQUtZj9njtnkXXUfGgKq");
                        client.setConnectionTimeoutInMillis(2000);
                        client.setSocketTimeoutInMillis(6000);
                        res = client.detect(strbm, "BASE64", options);
                        try {
                            Message message = Message.obtain();
                            message.what = 1;
                            message.obj = res;
                            handler.sendMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Message message = Message.obtain();
                            message.what = 2;
                            handler.sendMessage(message);
                        }
                    }
                }).start();
            }
        });


    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            JSONObject tmp_result=null;
            if (msg.what == 1) {
                JSONObject res = (JSONObject) msg.obj;

                try {
                    tmp_result= new JSONObject(res.optString("result"));
                    face_resultNum = tmp_result.optString("face_num");
                } catch (JSONException e) {
                    e.printStackTrace();
                }


//                face_resultNum = res.optString("face_num");
//                JSONObject pp= face_resultNum.optString("face_list");
                if (Integer.parseInt(face_resultNum) >= 1) {
                    try {
                        JSONArray js = new JSONArray(tmp_result.optString("face_list"));
                        face_age = js.optJSONObject(0).optString("age");
                        face_gender = js.optJSONObject(0).optString("gender");
                        JSONObject tmp_face_gender = new JSONObject(face_gender);
                        String tmp_face_gender_str = tmp_face_gender.optString("type");

                        if (tmp_face_gender_str.equals("female")) {
                            face_gender = "女";
                        } else {
                            face_gender = "男";
                        }
                        face_race = js.optJSONObject(0).optString("race");

                        JSONObject tmp_face_race = new JSONObject(face_race);
                        String tmp_face_race_str = tmp_face_race.optString("type");


                        if (tmp_face_race_str.equals("yellow")) {
                            face_race = "黄种人";
                        } else if (tmp_face_race_str.equals("white")) {
                            face_race = "白种人";
                        } else if (tmp_face_race_str.equals("black")) {
                            face_race = "黑种人";
                        } else if (tmp_face_race_str.equals("arabs")) {
                            face_race = "阿拉伯人";
                        }
                        face_expression = js.optJSONObject(0).optString("expression");

                        JSONObject tmp_face_expression= new JSONObject(face_expression);
                        String tmp_face_expression_str = tmp_face_expression.optString("type");



                        if (tmp_face_expression_str.equals("none")) {
                            face_expression = "无";
                        } else if (tmp_face_expression_str.equals("smile")) {
                            face_expression = "微笑";
                        } else {
                            face_expression = "大笑";
                        }
                        face_beauty = js.optJSONObject(0).optString("beauty");
                        double beauty = Math.ceil(Double.parseDouble(face_beauty) + 25);
                        if (beauty >= 100) {
                            beauty = 99.0;
                        } else if (beauty < 70) {
                            beauty += 10;
                        } else if (beauty > 80 && beauty < 90) {
                            beauty += 5;
                        } else if (beauty >= 90 && beauty < 95) {
                            beauty += 2;
                        }
                        face_beauty = String.valueOf(beauty);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    detect_tip.setVisibility(View.GONE);
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    String[] mItems = {"性别：" + face_gender, "年龄：" + face_age, "肤色：" + face_race, "颜值：" + face_beauty, "笑容：" + face_expression};
                    alertDialog.setTitle("人脸识别报告").setItems(mItems, null).create().show();
                } else {
                    detect_tip.setVisibility(View.VISIBLE);
                    detect_tip.setText("图片不够清晰，请重新选择");
                }
            } else {
                detect_tip.setVisibility(View.VISIBLE);
                detect_tip.setText("图片不够清晰，请重新选择");
            }
        }
    };


    public int getPicRotate(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //从相册中选择的图片
        if (requestCode == PHOTO_ALBUM) {
            if (data != null) {
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToNext();
                ImagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                cursor.close();
                resizePhoto();
                myPhoto.setImageBitmap(myBitmapImage);
                Log.i("图片路径", ImagePath);
            }
        }
        //相机拍摄的图片
        else if (requestCode == CAMERA) {
            try {
                resizePhoto();
                myPhoto.setImageBitmap(myBitmapImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //调整图片的比例，使其大小小于1M,能够显示在手机屏幕上
    public void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//返回图片宽高信息
        BitmapFactory.decodeFile(ImagePath, options);
        //让图片小于1024
        double radio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        options.inSampleSize = (int) Math.ceil(radio);//向上取整倍数
        options.inJustDecodeBounds = false;//显示图片
        myBitmapImage = BitmapFactory.decodeFile(ImagePath, options);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void readRequest() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
    }


}
