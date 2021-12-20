package com.aseemsethi.gurupadajobs.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    final String TAG = "Jobs: HomeView";

    private MutableLiveData<String> mText;
    private static boolean loggedin = false;
    private static MutableLiveData<String> status = new MutableLiveData<>();

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        //mText.setValue("Please log in....");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public boolean getLoggedin() { return loggedin; }
    public void setLoggedin(Boolean val) {
        Log.d(TAG, "Loggedin Status:" + val);
        loggedin = val;
    }

    public LiveData<String> getStatus() {
        return status;
    }
    public void setStatus(String val) {
        Log.d(TAG, val);
        status.setValue(val);}
}