package com.example.attask;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.attask.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_attendance)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        loadImage();
    }

    void loadImage()
    {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("attasksession", Context.MODE_PRIVATE);
            String image = sharedPreferences.getString("image", null);
            URL url = new URL(image);
            Bitmap imagebit = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            FileOutputStream outputStream = null;
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File dir = new File(file.getAbsolutePath() + "/face");
            dir.mkdirs();

            String filename = String.format("user.jpg");
            File outFile = new File(dir, filename);
            try {
                outputStream = new FileOutputStream(outFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            imagebit.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            try {
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            Log.d("FAVALResultFile",e.toString());
        }
    }

}