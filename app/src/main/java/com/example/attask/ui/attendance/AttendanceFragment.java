package com.example.attask.ui.attendance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.attask.LoginActivity;
import com.example.attask.MainActivity;
import com.example.attask.R;
import com.example.attask.databinding.FragmentAttendanceBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class AttendanceFragment extends Fragment {

    private FragmentAttendanceBinding binding;
    DatePicker picker;
    Button btnGet;
    TextView timeIn,timeOut;
    String employee_id;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AttendanceViewModel dashboardViewModel =
                new ViewModelProvider(this).get(AttendanceViewModel.class);

        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences("attasksession", Context.MODE_PRIVATE);
        employee_id = sharedPreferences.getString("employee_id", null);

        picker = binding.datepicker;
        Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        timeIn = binding.timeIn;
        timeOut = binding.timeOut;

        btnGet = binding.button1;
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String date = picker.getYear()+"-"+(picker.getMonth() + 1)+"-"+picker.getDayOfMonth();

                try {
                    /*initiate volley request*/
                    RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                    String backendURL = getResources().getString(R.string.api_link)+"api/attendance";

                    JSONObject postData = new JSONObject();
                    try {
                        postData.put("employee_id", employee_id);
                        postData.put("date", date);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, backendURL, postData, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String time_in = response.getString("time_in");
                                String time_out = response.getString("time_out");
                                timeIn.setText(time_in);
                                timeOut.setText(time_out);
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
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}