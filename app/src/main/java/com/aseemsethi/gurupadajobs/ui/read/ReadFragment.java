package com.aseemsethi.gurupadajobs.ui.read;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.style.AlignmentSpan;
import android.util.Log;
import android.view.CollapsibleActionView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.gurupadajobs.GenericFileProvider;
import com.aseemsethi.gurupadajobs.MainActivity;
import com.aseemsethi.gurupadajobs.R;
import com.aseemsethi.gurupadajobs.databinding.FragmentResumesBinding;
import com.aseemsethi.gurupadajobs.databinding.FragmentReadBinding;
import com.aseemsethi.gurupadajobs.ui.home.HomeViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

public class ReadFragment extends Fragment implements View.OnClickListener {
    private HomeViewModel homeViewModel;
    final String TAG = "Resumes: read";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    String cid;
    FirebaseFirestore db;
    String currentDate;
    TableLayout stk;
    Integer rowNum;
    File FilesDir;
    OutputStreamWriter outputStreamWriter;
    boolean download = false;
    FirebaseStorage storage;
    StorageReference storageReference;
    HashMap<Integer, String> allRefMap;
    HashMap<Integer, String> allNameMap;


    private ReadViewModel readViewModel;
    private FragmentReadBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        readViewModel =
                new ViewModelProvider(this).get(ReadViewModel.class);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        if (homeViewModel.getLoggedin() == false) {
            Log.d(TAG, "Not logged in..");
            if (isAdded())
                Toast.makeText(getContext(), "Please login first...",
                        Toast.LENGTH_SHORT).show();
            return null;
        } else {
            Log.d(TAG, "logged in..");
        }
        SharedPreferences sharedPref = getActivity().
                getPreferences(Context.MODE_PRIVATE);
        String nm = sharedPref.getString("cid", "10000");
        cid = nm;
        db = FirebaseFirestore.getInstance();
        currentDate = new SimpleDateFormat("dd-MM-yyyy",
                Locale.getDefault()).format(new Date());

        // /storage/emulated/0/Download
        FilesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        Log.d(TAG, "FilesDir Path1: " + FilesDir);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        binding = FragmentReadBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final Button btn = binding.viewBtn;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                hideKeyboard(getActivity());
                int selectedId = binding.radioGroup.getCheckedRadioButtonId();
                RadioButton rb = root.findViewById(selectedId);
                String duration = rb.getText().toString();
                if (duration.equals("Search")) {
                    //searchResume();
                } else {
                    getAllData();
                }
            }
        });
        return root;
    }

    public void getFile(String name, String ref1) {
        File FilesDir;
        ref1 = ref1.replace("%20", " ");
        StorageReference ref = storage.getReferenceFromUrl(ref1);
        Log.d(TAG, "getFile: getRefFromURL: " + ref);

        FilesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        File rootPath = new File(FilesDir, "resumes");
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }
        Log.d(TAG, "Filename: " + name);
        final File localFile1 = new File(rootPath, name);

        ref.getFile(localFile1).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Resume downloaded..");
                Toast.makeText(getContext(), "Resume downloaded to Downloads/resume folder...",
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Resume not downloaded !!: " + exception.getMessage());
            }
        });
    }

    public void getAllData() {
        binding.tableD.removeAllViews();
        setupTable();
        db.collection("resumes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Success...");
                            allRefMap = new HashMap<>();
                            allNameMap = new HashMap<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "Doc data: " + document.getData());
                                parseData(document.getData());
                            }
                        } else {
                            Log.w(TAG, "DB Access Error", task.getException());
                        }
                    }
                });
    }

    // uri, email, exp
    public void parseData(Map<String, Object> dataR) {
        String name, email, exp, cat, link, ref;

        for (Map.Entry<String, Object> entry : dataR.entrySet()) {
            //Log.d(TAG, "Name = " + entry.getKey() +
            //        ", Data = " + entry.getValue());
            name = entry.getKey();
            ArrayList<String> val = (ArrayList<String>) entry.getValue();
            for (String entry1 : val) {
            //    Log.d(TAG, ", Value = " + entry1);
            }
            link = val.get(0);
            email = val.get(1);
            exp = val.get(2);
            cat = val.get(3);
            ref = val.get(4);
            Log.d(TAG, "name: " + name + ", email: " + email +
                    ", Experience: " + exp + ", cat: " + cat + ", link: " + link +
                    " Ref: " + ref);
            addToTable(name, email, exp, cat, link, ref);
        }
    }

    public void addToTable(String name, String email, String exp, String cat,
                           String link, String ref) {
        allRefMap.put(rowNum, ref);
        allNameMap.put(rowNum, name);

        TableRow tbrow = new TableRow(getContext());
        TextView t1v = new TextView(getContext());
        t1v.setText(rowNum.toString());
        t1v.setTextColor(Color.WHITE);
        t1v.setGravity(Gravity.CENTER);
        tbrow.addView(t1v);

        TextView t2v = new TextView(getActivity());
        t2v.setText(name);
        t2v.setTextColor(Color.WHITE);
        t2v.setGravity(Gravity.CENTER);
        tbrow.addView(t2v);

        TextView t3v = new TextView(getActivity());
        t3v.setText(email);
        t3v.setTextColor(Color.WHITE);
        t3v.setGravity(Gravity.CENTER);
        tbrow.addView(t3v);
        TextView t4v = new TextView(getActivity());

        t4v.setText(exp);
        t4v.setTextColor(Color.WHITE);
        t4v.setGravity(Gravity.CENTER);
        tbrow.addView(t4v);
        TextView t5v = new TextView(getActivity());

        t5v.setText(cat);
        t5v.setTextColor(Color.WHITE);
        t5v.setGravity(Gravity.CENTER);
        tbrow.addView(t5v);

        tbrow.setOnClickListener(this); // set TableRow onClickListner
        tbrow.setId(rowNum);
        stk.addView(tbrow);

        rowNum++;
    }

    public void setupTable() {
        TableRow tbrow0;
        rowNum = 1;
        stk = binding.tableD;
        tbrow0 = new TableRow(getContext());
        TextView tv0 = new TextView(getActivity());
        tv0.setText(" No ");
        tv0.setTextColor(Color.WHITE);
        tbrow0.addView(tv0);
        TextView tv1 = new TextView(getActivity());
        tv1.setText(" Name ");
        tv1.setTextColor(Color.WHITE);
        tbrow0.addView(tv1);
        TextView tv2 = new TextView(getActivity());
        tv2.setText(" Email ");
        tv2.setTextColor(Color.WHITE);
        tbrow0.addView(tv2);
        TextView tv3 = new TextView(getActivity());
        tv3.setText(" Exp ");
        tv3.setTextColor(Color.WHITE);
        tbrow0.addView(tv3);
        TextView tv4 = new TextView(getActivity());
        tv4.setText(" Cat: ");
        tv4.setTextColor(Color.WHITE);
        tbrow0.addView(tv4);
        stk.addView(tbrow0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: " + v.getId());
        String ref = allRefMap.get(v.getId());
        String name = allNameMap.get(v.getId());

        Log.d(TAG, "Name: " + name + ", Ref from hashMap: " + ref);
        getFile(name, ref);
    }
}
