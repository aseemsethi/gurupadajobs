package com.aseemsethi.gurupadajobs.ui.resumes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.CollapsibleActionView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.gurupadajobs.R;
import com.aseemsethi.gurupadajobs.databinding.FragmentResumesBinding;
import com.aseemsethi.gurupadajobs.ui.home.HomeViewModel;
import com.aseemsethi.gurupadajobs.ui.settings.SettingsViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ResumesFragment extends Fragment {
    private HomeViewModel homeViewModel;
    final String TAG = "Inventory: inventory";
    FirebaseFirestore db;
    String cid;

    private ResumesViewModel inventoryViewModel;
    private SettingsViewModel settingsViewModel;
    private FragmentResumesBinding binding;
    String currentDate;
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        inventoryViewModel = new ViewModelProvider(this).get(ResumesViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        if (homeViewModel.getLoggedin() == false) {
            Log.d(TAG, "Not logged in..");
            Toast.makeText(getContext(),"Please login first...",
                    Toast.LENGTH_SHORT).show();
            return null;
        } else {
            Log.d(TAG, "logged in..");
        }

        binding = FragmentResumesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        /*
        final TextView textView = binding.textInventory;
        inventoryViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
         */
        SharedPreferences sharedPref = getActivity().
                getPreferences(Context.MODE_PRIVATE);
        String nm = sharedPref.getString("cid", "10000");
        cid = nm;

        db = FirebaseFirestore.getInstance();
        currentDate = new SimpleDateFormat("dd-MM-yyyy",
                Locale.getDefault()).format(new Date());
        Log.d(TAG, "Date: " + currentDate + " cid: " + cid);

        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a barcode");
        options.setCameraId(0);  // Use a specific camera of the device
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(true);
        final Button btnBR = binding.barcode;
        btnBR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                hideKeyboard(getActivity());
                options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
                barcodeLauncher.launch(options);
            }
        });
        final Button btnQR = binding.qrcode;
        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                hideKeyboard(getActivity());
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                barcodeLauncher.launch(options);
            }
        });

        final Button btn = binding.saveItem;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                hideKeyboard(getActivity());
                String code = binding.barCodeID.getText().toString();
                Log.d(TAG, "Saving Item no: " + binding.itemNo.getText().toString() +
                        ", Code: " + code);
                String str = binding.itemNo.getText().toString();
                if(str == null || str.trim().isEmpty()) {
                    if (isAdded())
                        Toast.makeText(getContext(),"Enter Quantity",Toast.LENGTH_SHORT).show();
                } else {
                    addData(binding.itemID.getText().toString(),
                            binding.itemNo.getText().toString(),
                            code);
                    /*
                    try {
                        Integer val = Integer.valueOf(binding.itemNo.getText().toString());
                        addData(binding.itemID.getText().toString(), val, code);
                    } catch(Exception e) {
                        if (isAdded())
                            Toast.makeText(getContext(), "Input error",
                                Toast.LENGTH_SHORT).show();
                    }
                     */
                }
            }
        });
        return root;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // Database looks like
    //      /cid/date/<items:count>
    //      /100/08-12-2021/<items, count>
    public void addData(String itemName, String num, String code) {
        DocumentReference docref = db.collection(cid).document(currentDate);
        //Map<String, Object> data = new HashMap<>();
        //data.put("item", itemName);
        //data.put("num", num);
        //data.put("code", code);
        Map<String, ArrayList<String>> data =
                new HashMap<String, ArrayList<String>>();
        data.put(itemName, new ArrayList<String>());
        data.get(itemName).add(num);
        data.get(itemName).add(code);

        Log.d(TAG, "Adding: " + itemName + ", num: " + num +
                " code: " + code + " to cid: " +  cid);

        docref.set(data, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "New Doc successfully written!");
                        if (isAdded())
                            Toast.makeText(getContext(),"New Doc Write ok..",
                                    Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        if (isAdded())
                            Toast.makeText(getContext(),"New Doc Write failed..",
                                    Toast.LENGTH_SHORT).show();
                    }
                });

        /*
        docref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "Document exists!");
                        docref.update(
                                "item", itemName,
                                "num", num, "code", code)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Doc successfully written!");
                                        if (isAdded())
                                            Toast.makeText(getContext(), "Write ok..",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error writing document", e);
                                        if (isAdded())
                                            Toast.makeText(getContext(), "Write failed..",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.d(TAG, "Document does not exist!");
                        docref
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "New Doc successfully written!");
                                        if (isAdded())
                                            Toast.makeText(getContext(),"New Doc Write ok..",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error writing document", e);
                                        if (isAdded())
                                            Toast.makeText(getContext(),"New Doc Write failed..",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    });
                    }
                } else {
                    Log.d(TAG, "Failed with: ", task.getException());
                    if (isAdded())
                        Toast.makeText(getContext(),"DB Access Error",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
         */
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Register the launcher and result handler
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(),
                    result -> {
                        if(result.getContents() == null) {
                            Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_LONG).show();
                        } else {
                            TextView tv = binding.barCodeID;
                            Toast.makeText(getContext(), "Scanned: " +
                                    result.getContents(), Toast.LENGTH_LONG).show();
                            tv.setText(result.getContents());
                        }
                    });

}