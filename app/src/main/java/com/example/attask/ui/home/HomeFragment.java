package com.example.attask.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attask.DetectorActivity;
import com.example.attask.WorkRequest;
import com.example.attask.databinding.FragmentHomeBinding;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private LinearLayout submitTask;
    private LinearLayout startWork;

    private TextView fullName;

    private CircleImageView user_image;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences("attasksession", Context.MODE_PRIVATE);
        String name = sharedPreferences.getString("name", null);
        String imageUrl = sharedPreferences.getString("image", null);

        submitTask = binding.submitTask;
        startWork = binding.startWork;

        fullName = binding.userFullname;
        user_image = binding.profileImage;

        Picasso.get().load(imageUrl).into(user_image);
        fullName.setText("Welcome Home,\n "+name);

        submitTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), WorkRequest.class));
            }
        });

        startWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), DetectorActivity.class));
            }
        });
//        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}