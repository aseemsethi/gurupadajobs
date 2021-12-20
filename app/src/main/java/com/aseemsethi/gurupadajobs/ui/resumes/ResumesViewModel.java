package com.aseemsethi.gurupadajobs.ui.resumes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ResumesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ResumesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}