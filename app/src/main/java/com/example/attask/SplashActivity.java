package com.example.attask;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

public class SplashActivity extends AppCompatActivity {
    private static  int SPLASH_SCREEN = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_init);

        SharedPreferences sharedPreferences = getSharedPreferences("attasksession", Context.MODE_PRIVATE);
        String userEmployeeid = sharedPreferences.getString("employee_id", null);

        if (userEmployeeid == null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent mainIntent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(mainIntent);
                    finish();
                }
            }, 3000); // 3000 milliseconds = 3 seconds
        }else{

            /*initiate volley request*/
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String backendURL = getResources().getString(R.string.api_link)+"api/getEmpData";

            JSONObject postData = new JSONObject();
            try {
                postData.put("employeeid", userEmployeeid);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, backendURL, postData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String status = response.getString("status");
                        String employee_id = response.getString("employee_id");
                        String image = response.getString("image");
                        String name = response.getString("name");
                        String latitude = response.getString("latitude");
                        String longitude = response.getString("longitude");

                        if(status.equals("success")){

                            saveSession(employee_id, image, name, latitude, longitude);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                                    startActivity(mainIntent);
                                    finish();
                                }
                            }, 3000); // 3000 milliseconds = 3 seconds
                        }else{
                            Toast.makeText(SplashActivity.this, "Server Down", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            });

            requestQueue.add(jsonObjectRequest);
        }
    }

    private void saveSession(String employee_id, String image, String name, String latitude, String longitude) {
        SharedPreferences sharedPreferences = getSharedPreferences("attasksession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("employee_id", employee_id);
        editor.putString("image", image);
        editor.putString("name", name);
        editor.putString("latitude", latitude);
        editor.putString("longitude", longitude);
        editor.commit();
    }
}