package com.example.attask;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    Button login;
    private EditText username, pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        login = findViewById(R.id.btn_login);
        login.setOnClickListener(this);
        username = findViewById(R.id.et_login_username);
        pass = findViewById(R.id.et_login_password);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_login:
                validateUser();
                break;
        }
    }

    private void validateUser() {
        String Username = username.getText().toString().trim();
        String Password = pass.getText().toString().trim();

        if (Username.isEmpty()){
            username.setError("Username is required!");
            username.requestFocus();
            Toast.makeText(LoginActivity.this, "Email Is Required", Toast.LENGTH_LONG).show();
            return;
        }

        if (Password.isEmpty()){
            pass.setError("Password is required!");
            pass.requestFocus();
            Toast.makeText(LoginActivity.this, "Password is required", Toast.LENGTH_LONG).show();
            return;
        }

        try {

            /*initiate volley request*/
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String backendURL = getResources().getString(R.string.api_link)+"api/login";

            JSONObject postData = new JSONObject();
            try {
                postData.put("username", Username);
                postData.put("password", Password);
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
                            Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_LONG).show();
                            Intent returnBtn = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(returnBtn);
                            finish();
                        }else{
                            Toast.makeText(LoginActivity.this, "Wrong Credentials", Toast.LENGTH_LONG).show();
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

        } catch (Exception e) {
            e.printStackTrace();
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
