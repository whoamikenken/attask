package com.example.attask;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WorkRequest extends AppCompatActivity implements View.OnClickListener{

    private static final int PICK_FILE_REQUEST = 0xF0F0;
    private static final String CHANNEL_ID = "r21421124";

    Button submit, uploadButton;
    private EditText purpose, dateTxt, workdone;
    private byte[] fileBytes;

    final Calendar myCalendar= Calendar.getInstance();

    private String fileName,fileExtension;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_task);
        uploadButton = findViewById(R.id.uploadFile);
        uploadButton.setOnClickListener(this);

        submit = findViewById(R.id.Submit);
        submit.setOnClickListener(this);

        purpose = findViewById(R.id.editTextPurpose);
        workdone = findViewById(R.id.editTextTextWorkDone);
        dateTxt = findViewById(R.id.editTextDate);

        DatePickerDialog.OnDateSetListener date =new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH,month);
                myCalendar.set(Calendar.DAY_OF_MONTH,day);
                updateLabel();
            }
        };

        dateTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(WorkRequest.this,date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.uploadFile:
                uploadFile();
                break;
            case R.id.Submit:
                submitData();
                break;
        }
    }

    private void submitData() {
        if (TextUtils.isEmpty(purpose.getText().toString())){
            Toast.makeText(WorkRequest.this, "Purpose is required", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(dateTxt.getText().toString())){
            Toast.makeText(WorkRequest.this, "Date is required", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(workdone.getText().toString())){
            Toast.makeText(WorkRequest.this, "Work Done is required", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(fileName)){
            Toast.makeText(WorkRequest.this, "Please Upload A File", Toast.LENGTH_LONG).show();
            return;
        }

        try {

            SharedPreferences sharedPreferences = getSharedPreferences("attasksession", Context.MODE_PRIVATE);
            String userEmployeeid = sharedPreferences.getString("employee_id", null);

            /*initiate volley request*/
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String backendURL = getResources().getString(R.string.api_link)+"api/task";

            JSONObject postData = new JSONObject();
            long timestampDevice = System.currentTimeMillis() / 1000;
            try {
                postData.put("employee_id", userEmployeeid);
                postData.put("purpose", purpose.getText());
                postData.put("work_done", workdone.getText());
                postData.put("date", dateTxt.getText());
                postData.put("file_name", fileName);
                postData.put("file_ext", fileExtension);
                postData.put("base_64", Base64.encodeToString(fileBytes, Base64.DEFAULT | Base64.NO_WRAP));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, backendURL, postData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String status = response.getString("status");
                        if(status.equals("over")){

                            Toast.makeText(WorkRequest.this, "You already submitted task today", Toast.LENGTH_LONG).show();

                            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
                            channel.setDescription("Channel Description");

                            // Create a notification builder object
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(WorkRequest.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.logo)
                                    .setContentTitle("Info!")
                                    .setContentText("You already submitted task today.")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                            // Create a notification manager object
                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.createNotificationChannel(channel);

                            // Show the notification
                            notificationManager.notify(41251, builder.build());
                        }else{
                            Toast.makeText(WorkRequest.this, "Task Request Submitted", Toast.LENGTH_LONG).show();

                            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
                            channel.setDescription("Channel Description");

                            // Create a notification builder object
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(WorkRequest.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.logo)
                                    .setContentTitle("Success!")
                                    .setContentText("Task Request Submitted.")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                            // Create a notification manager object
                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.createNotificationChannel(channel);

                            // Show the notification
                            notificationManager.notify(5156, builder.build());
                        }

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            });

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            requestQueue.add(jsonObjectRequest);

            Intent returnBtn = new Intent(this,
                    MainActivity.class);
            startActivity(returnBtn);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLabel(){
        String myFormat="yy-MM-dd";
        SimpleDateFormat dateFormat=new SimpleDateFormat(myFormat, Locale.US);
        dateTxt.setText(dateFormat.format(myCalendar.getTime()));
    }

    public void uploadFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //sets the select file to all types of files
        intent.setType("*/*");
        // Only get openable files
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //starts new activity to select file and return data
        startActivityForResult(Intent.createChooser(intent,
                "Choose File to Upload.."),PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();

            Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                // Get the column index of the data column
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                // Get the file name
                fileName = cursor.getString(columnIndex);
                // Get the file extension
                fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                cursor.close();
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                fileBytes = IOUtils.toByteArray(inputStream);
                // ...
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
