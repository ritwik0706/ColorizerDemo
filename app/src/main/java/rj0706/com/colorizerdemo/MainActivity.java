package rj0706.com.colorizerdemo;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button buChoose,buColorize;
    private ImageView img;
    private static final int IMG_REQUEST=123;
    private Bitmap bitmap;
    private Uri path;
    private ResponseBody filename;

    int serverResponseCode = 0;
    ProgressDialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buChoose=findViewById(R.id.buChoose);
        buColorize=findViewById(R.id.buColorize);
        img=findViewById(R.id.selectedImage);
        buChoose.setOnClickListener(this);
        buColorize.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        switch(view.getId())
        {
            case R.id.buChoose:
                SelectImage();
                break;

            case R.id.buColorize:
                UploadImage();
                break;
        }
    }

    private void UploadImage(){

        try {
            String filePath="";
            if(Build.VERSION.SDK_INT>=26){
                final String[] split = path.getPath().split(":");//split the path.
                filePath = split[1];
            }else{
                filePath=PathUtil.getPath(this,path);
            }
            File originalfile=new File(filePath);
            RequestBody filepart=RequestBody.create(
                    MediaType.parse(getContentResolver().getType(path)),
                    originalfile
            );

            MultipartBody.Part file=MultipartBody.Part.createFormData("photo",originalfile.getName(), filepart);

            String baseUrl="http://76ea8bf6.ngrok.io/";
            Retrofit retrofit= new Retrofit.Builder().baseUrl(baseUrl).
                    addConverterFactory(GsonConverterFactory.create()).build();

            ApiInterface apiInterface=retrofit.create(ApiInterface.class);

            Call<ResponseBody> call= apiInterface.uploadImage(file);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    filename=response.body();
                    Toast.makeText(MainActivity.this,filename.toString(),Toast.LENGTH_SHORT).show();
                    String DownloadUrl="http://610cddab.ngrok.io/colored/"+filename.toString();
                    Picasso.get().load(DownloadUrl).into(img);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(MainActivity.this,"no"+t.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }

    private void SelectImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMG_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==IMG_REQUEST && resultCode==RESULT_OK && data!=null){
            path= data.getData();

            try {
                bitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),path);
                img.setImageBitmap(bitmap);
                img.setVisibility(View.VISIBLE);
                buChoose.setEnabled(false);
                buChoose.setVisibility(View.INVISIBLE);
                buColorize.setEnabled(true);
                buColorize.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(contentUri, proj,
                null, null, null);
        assert cursor != null;
        int column_index;
        column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
