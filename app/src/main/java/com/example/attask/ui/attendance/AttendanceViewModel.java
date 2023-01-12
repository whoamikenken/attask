package com.example.attask.ui.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AttendanceViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AttendanceViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is attendance fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}