package com.aseemsethi.gurupadajobs.ui.resumes;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.CollapsibleActionView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.gurupadajobs.MainActivity;
import com.aseemsethi.gurupadajobs.R;
import com.aseemsethi.gurupadajobs.databinding.FragmentResumesBinding;
import com.aseemsethi.gurupadajobs.ui.home.HomeViewModel;
import com.aseemsethi.gurupadajobs.ui.read.ReadFragment;
import com.aseemsethi.gurupadajobs.ui.settings.SettingsViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.OnProgressListener;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class ResumesFragment extends Fragment implements EasyPermissions.PermissionCallbacks {
    //private HomeViewModel homeViewModel;
    final String TAG = "Resumes: read";
    FirebaseFirestore db;
    String cid;
    public static final int PICKFILE_RESULT_CODE = 1;
    private static final int READ_STORAGE_PERMISSION_REQUEST = 123;
    File resumeFile;
    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;
    String filePath;
    Uri fileUri;

    private ResumesViewModel inventoryViewModel;
    private SettingsViewModel settingsViewModel;
    private FragmentResumesBinding binding;
    String currentDate;
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        inventoryViewModel = new ViewModelProvider(this).get(ResumesViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        //homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        if (HomeViewModel.getLoggedin() == false) {
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

        if (!EasyPermissions.hasPermissions(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
            EasyPermissions.requestPermissions(this,
                    "App Requires a permission to access your storage",
                    READ_STORAGE_PERMISSION_REQUEST
                    ,Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        db = FirebaseFirestore.getInstance();
        currentDate = new SimpleDateFormat("dd-MM-yyyy",
                Locale.getDefault()).format(new Date());
        Log.d(TAG, "Date: " + currentDate + " cid: " + cid);
        binding.nameTxt.setText(HomeViewModel.getUsername());
        binding.emailTxt.setText(HomeViewModel.getEmail());
        Log.d(TAG, "User: " + HomeViewModel.getUsername());

        final Button btn1 = binding.resumeBtn;
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
            }
        });

        final Button btn = binding.saveItem;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                hideKeyboard(getActivity());
                uploadFile();
            }
        });
        return root;
    }

    @Override
    public void onPermissionsGranted(int requestCode, List perms) {
        // Add your logic here
    }

    @Override
    public void onPermissionsDenied(int requestCode, List perms) {
        // Add your logic here
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == -1) {
                    fileUri = data.getData();
                    filePath = fileUri.getPath();
                    binding.resumeFile.setText(filePath);
                    resumeFile = new File(fileUri.getPath());
                }

                break;
        }
    }

    private void uploadFile() {
        if (fileUri == null) {
            Log.d(TAG, "File uri is null");
            Toast.makeText(getContext(),
                    "Please enter resume file", Toast.LENGTH_SHORT).show();
            return;
        }
        // Code for showing progressDialog while uploading
        ProgressDialog progressDialog
                = new ProgressDialog(getContext());
        progressDialog.setTitle("Uploading...");
        progressDialog.show();

        // Defining the child of storageReference
        StorageReference ref
                = storageReference
                .child("resumes/" + binding.nameTxt.getText().toString());

        // adding listeners on upload
        // or failure of image
        ref.putFile(fileUri)
                .addOnSuccessListener(
                        new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(),
                                        "Image Uploaded!!", Toast.LENGTH_SHORT).show();
                                Uri downloadUri = taskSnapshot.getUploadSessionUri();
                                Log.d(TAG, "url: " + downloadUri);
                                int selectedId = binding.radioGroup.getCheckedRadioButtonId();
                                RadioButton rb = binding.getRoot().findViewById(selectedId);
                                String cat = rb.getText().toString();
                                addData(binding.nameTxt.getText().toString(),
                                        downloadUri, binding.expTxt.getText().toString(),
                                        cat, ref);
                                Log.d(TAG, "Storage Ref:" + ref);
                                //getFile(ref);
                            }
                        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(),
                                "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void getFile(StorageReference ref) {
        File FilesDir;
        //StorageReference ref = storage.getReferenceFromUrl(ref1);
        Log.d(TAG, "getFile: getRefFromURL: " + ref);

        FilesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        File rootPath = new File(FilesDir, "resumes");
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }
        Log.d(TAG, "Filename: " + HomeViewModel.getUsername());

        final File localFile1 = new File(rootPath,
                HomeViewModel.getUsername());

        ref.getFile(localFile1).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Resume downloaded..");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Resume not downloaded !!: " + exception.getMessage());
            }
        });
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void addData(String name, Uri uri, String exp, String cat, StorageReference ref) {
        DocumentReference docref = db.collection("resumes").document();
        //Map<String, Object> data = new HashMap<>();
        //data.put("item", itemName);
        //data.put("num", num);
        //data.put("code", code);
        Map<String, ArrayList<String>> data =
                new HashMap<String, ArrayList<String>>();
        data.put(name, new ArrayList<String>());
        data.get(name).add(uri.toString());
        data.get(name).add(HomeViewModel.getEmail());
        data.get(name).add(exp);
        data.get(name).add(cat);
        data.get(name).add(ref.toString());

        docref.set(data, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "New Doc successfully uploaded!");
                        if (isAdded())
                            Toast.makeText(getContext(),"New upload ok..",
                                    Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        if (isAdded())
                            Toast.makeText(getContext(),"New upload failed..",
                                    Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}