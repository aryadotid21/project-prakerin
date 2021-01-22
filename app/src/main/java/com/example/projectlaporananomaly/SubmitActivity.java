package com.example.projectlaporananomaly;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SubmitActivity extends AppCompatActivity implements View.OnClickListener {

    EditText keterangan;
    Button btn_submit,btn_choose;
    ImageView imageUpload;
    Bitmap bitmap;
    private Spinner spinner_gardu, spinner_tower, spinner_aksesoris, spinner_anomaly;
    SessionManager sessionManager;
    final int CODE_GALLERY_REQUEST = 999;
    String urlUpload = "http://malwarepwpb.000webhostapp.com/upload.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
        keterangan = (EditText)findViewById(R.id.keterangan);
        btn_submit = (Button)findViewById(R.id.btn_submit);
        btn_submit.setOnClickListener(this);
        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();


        btn_choose = (Button) findViewById(R.id.btn_choose);
        imageUpload = (ImageView) findViewById(R.id.imageUpload);

        btn_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(
                        SubmitActivity.this,
                        new  String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        CODE_GALLERY_REQUEST
                );
            }
        });

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToSheet();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, urlUpload, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), "Uploading Image...",Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Error: " + error.toString(),Toast.LENGTH_SHORT).show();
                    }
                }){
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        String imageData = imageToString(bitmap);
                        params.put("image", imageData);
                        return params;
                    }
                };
                RequestQueue requestQueue = Volley.newRequestQueue(SubmitActivity.this);
                requestQueue.add(stringRequest);
            }
        });
    }

    private void addItemToSheet(){
        final ProgressDialog loading = ProgressDialog.show(this,"Adding Report","Please Wait...");

        Spinner gardu=(Spinner) findViewById(R.id.spinner_gardu);
        Spinner tower=(Spinner) findViewById(R.id.spinner_tower);
        Spinner aksesoris=(Spinner) findViewById(R.id.spinner_aksesoris);
        Spinner anomaly=(Spinner) findViewById(R.id.spinner_anomaly);
        final String SGardu = gardu.getSelectedItem().toString();
        final String STower = tower.getSelectedItem().toString();
        final String SAksesoris = aksesoris.getSelectedItem().toString();
        final String SAnomaly = anomaly.getSelectedItem().toString();
        final String SKeterangan = keterangan.getText().toString().trim();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbyJu2Szohdx-0b7wlhIxIl_bEUNsdWWOHYIKmbnMEhzymKl6QM/exec",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        loading.dismiss();
                        Toast.makeText(SubmitActivity.this, response, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), SubmitActivity.class);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        ){
            @Override
            protected Map<String, String> getParams(){

                HashMap<String, String> user = sessionManager.getUserDetail();
                Map<String, String> parmas = new HashMap<>();

                parmas.put("action", "addItem");
                parmas.put("operator",user.get(SessionManager.NAME));
                parmas.put("gardu",SGardu);
                parmas.put("notower",STower);
                parmas.put("aksesoris",SAksesoris);
                parmas.put("anomaly",SAnomaly);
                parmas.put("keterangan",SKeterangan);

                return parmas;
            }
        };

        int socketTimeOut = 50000;

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(this);

        queue.add(stringRequest);
    }

    @Override
    public void onClick(View v) {
        if (v==btn_submit){
            addItemToSheet();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_GALLERY_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Image"), CODE_GALLERY_REQUEST);
            }else {
                Toast.makeText(getApplicationContext(), "You don't have permission to access gallery", Toast.LENGTH_LONG).show();
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == CODE_GALLERY_REQUEST && resultCode == RESULT_OK && data != null){
            Uri filePath = data.getData();

            try{
                InputStream inputStream = getContentResolver().openInputStream(filePath);
                bitmap = BitmapFactory.decodeStream(inputStream);
                imageUpload.setImageBitmap(bitmap);
        } catch (FileNotFoundException e){
                e.printStackTrace();
            }

        super.onActivityResult(requestCode, resultCode, data);
    }
}

private String imageToString(Bitmap bitmap){
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
    byte[] imageBytes = outputStream.toByteArray();
    String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
    return encodedImage;
    }
}