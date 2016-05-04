package org.mamkschools.mhs.fbla_mobileapp_2016;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Commands;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Constants;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.PictureItem;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.PictureItemAdapter;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.SecureAPI;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.delight.android.location.SimpleLocation;


public class FragmentMe extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener{
    //Some static request number that is attached to the uploader activity
    public static final int PIC_UPLOAD_REQUEST = 20;
    private static final int PERMISSION_REQUEST_CODE = 2;
    public FloatingActionButton fab;
    private Uri outputFileUri;
    private static final int PICTURE_REQUEST_CODE = 1;
    private SwipeRefreshLayout swipeRefresh;
    private SimpleLocation simpleLocation;
    private ProgressDialog progressDialog;
    private AsyncTask uploadTask;

    private File picLoc;
    private AsyncTask picGet;

    public static FragmentMe newInstance(File picLoc) {
        FragmentMe fragmentMe =  new FragmentMe();
        fragmentMe.picLoc = picLoc;
        return fragmentMe;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fab = (FloatingActionButton) getActivity().findViewById(R.id.cameraButton);
        fab.setOnClickListener(this);

        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.myPicRefresh);
        swipeRefresh.setOnRefreshListener(this);

        simpleLocation = new SimpleLocation(getContext());

        picGet = new GetMyPictureInfo().execute();
    }


    public FragmentMe() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop(){
        super.onStop();
        if(picGet != null && !picGet.getStatus().equals(AsyncTask.Status.FINISHED)){
            picGet.cancel(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_view_me, container, false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.cameraButton:
                uploadPic();
                break;
            default:
                Toast.makeText(getContext(), "No action", Toast.LENGTH_LONG).show();
        }
    }

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PIC_UPLOAD_REQUEST){
            new GetMyPictureInfo().execute((Void) null);
        }
    }*/

    @Override
    public void onRefresh() {
        picGet = new GetMyPictureInfo().execute();
    }

    public void uploadPic(){
        String [] permsToEnable = getDisabledPermissions();
        if(permsToEnable.length != 0){
            ActivityCompat.requestPermissions(getActivity(),
                    permsToEnable,
                    PERMISSION_REQUEST_CODE);
        }
            openImageIntent();
    }

    private void showImageDialog(final Bitmap previewBitmap){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(R.string.upload_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText edit = (EditText) ((AlertDialog) dialog).findViewById(R.id.editTitleText);
                FragmentMe.this.uploadImage(previewBitmap, edit.getText().toString());
                dialog.dismiss();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).setNeutralButton(R.string.retake, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                uploadPic();
            }
        });
        final AlertDialog dialog = builder.create();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_upload, null);
        dialog.setView(dialogLayout);
        dialog.setTitle("Upload Image stuffs");

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                ImageView image = (ImageView) dialog.findViewById(R.id.previewImage);
                image.setImageBitmap(previewBitmap);
                LinearLayout linearLayout = (LinearLayout) dialog.findViewById(R.id.dialogLayout);
                //linearLayout.

                dialog.findViewById(R.id.editTitleText).setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }



    public String [] getDisabledPermissions(){
        String [] permissions = {Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ArrayList<String> disabledPerms = new ArrayList<>();
        for (String perm: permissions) {
            if(ContextCompat.checkSelfPermission(getActivity(),
                    perm)
                    != PackageManager.PERMISSION_GRANTED){
                disabledPerms.add(perm);
            }
        }
        return disabledPerms.toArray(new String[disabledPerms.size()]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    break;
                } else {

                    // permission denied, boo!
                    // must open settings, as these permissions are critical

                    Toast.makeText(getContext(),
                            "Please enable all permissions", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getActivity().getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void legacyUpload(){
        startActivityForResult(new Intent(getContext(), UploadActivity.class),
                PIC_UPLOAD_REQUEST);
    }

    private class GetMyPictureInfo extends AsyncTask<Void, Boolean, Boolean> {

        private ArrayList<PictureItem> ret = new ArrayList<>();
        SecureAPI picture = SecureAPI.getInstance(getContext());


        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                JSONObject response = picture.HTTPSGET("picture/fetch/me?authcode=" + Constants.AUTHCODE);
                Util.log("picture/fetch/me?authcode=" + Constants.AUTHCODE);

                JSONArray array = response.getJSONArray("data");

                for(int i = 0; i < array.length(); i++ ){
                    int pid = array.getJSONObject(i).getInt("pid");
                    String title = array.getJSONObject(i).getString("title");
                    int views = array.getJSONObject(i).getInt("views");
                    int dislikes = array.getJSONObject(i).getInt("dislikes");
                    int likes = array.getJSONObject(i).getInt("likes");

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    long different = new Date().getTime() - simpleDateFormat.parse(
                            array.getJSONObject(i).getString("created")).getTime();
                    long elapsedHours = 3 + (different / (1000 * 60 * 60));

                    ret.add(new PictureItem(title, elapsedHours, likes, dislikes, views, pid));
                }
            }catch (Exception e){
                if(Util.DEBUG_MODE){
                    Util.log("mypics error " + e.getMessage());
                }
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean v) {
            if(v){
                Util.log("Finished getting my pics");
                //noinspection ConstantConditions
                RecyclerView picList = (RecyclerView) getView().findViewById(R.id.picList);
                LinearLayoutManager layoutManager=new LinearLayoutManager(getContext());
                picList.setLayoutManager(layoutManager);

                picList.addItemDecoration(new SimpleDividerItemDecoration(getContext()));

                PictureItemAdapter adapter=new PictureItemAdapter(ret,getContext());
                picList.setAdapter(adapter);
            }else{
                Util.log("Did not work_111");
            }
            swipeRefresh.setRefreshing(false);
        }
    }


    private void openImageIntent() {

        File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //noinspection ResultOfMethodCallIgnored
        filePath.mkdirs();
        String fname = "img_" + System.currentTimeMillis() + ".jpg";
        final File sdImageMainDirectory = new File(filePath, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera
        final List<Intent> cameraIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        startActivityForResult(chooserIntent, PICTURE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICTURE_REQUEST_CODE) {
            final boolean isCamera;
            if (data == null) {
                isCamera = true;
            } else {
                final String action = data.getAction();
                isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
            }



            if (!isCamera) {
                outputFileUri = data.getData();
                Util.log("Using data.getData");
            } else {
                Util.log("Using filename");
            }


            showProgress(true);
            try {
                Bitmap b;
                {
                    byte[] imageBytes = getBytes(getActivity().getContentResolver().openInputStream(outputFileUri));
                    b = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            }


                int maxDim = Math.max(b.getWidth(), b.getHeight());
                b = Bitmap.createScaledBitmap(b,Util.map(b.getWidth(),0,maxDim, 0, 4096),
                        Util.map(b.getHeight(), 0, maxDim, 0, 4096), false);

                showImageDialog(b);
            } catch (IOException | NullPointerException e) {
                if(Util.DEBUG_MODE) {
                    e.printStackTrace();
                }

            } catch (OutOfMemoryError outOfMemoryError){
                if(Util.DEBUG_MODE){
                    outOfMemoryError.printStackTrace();
                }
                Toast.makeText(getContext(), "Out of memory, please increase emulator ram or close other apps. If the issue persists, please contact teh developer.", Toast.LENGTH_LONG).show();
            }
            showProgress(false);
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }


    private void showProgress(boolean show){
        if(show){
            progressDialog = ProgressDialog.show(getActivity(), "Loading",
                    "Please wait, uploading picture", true);
        } else {
            progressDialog.dismiss();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static class PicUploadParams {
        public Map<String, String> paramMap = new HashMap<>();
        public Map<String, ByteArrayOutputStream> pics = new HashMap<>();
    }

    private class PicUpload extends AsyncTask<PicUploadParams, Void, Boolean> {
        SecureAPI picture = SecureAPI.getInstance(getContext());

        @Override
        protected Boolean doInBackground(PicUploadParams... params) {
            try {
                JSONObject response = picture.HTTPSPOSTMULTI(Commands.Post.POSTPIC +
                                Commands.AUTHCODE_BASE + Constants.AUTHCODE,
                        params[0].paramMap, params[0].pics, getContext());
                if (response.getString("status").equals("success")) {
                    return true;
                } else if (response.getString("status").equals("error")) {
                    return false;
                } else {
                    throw new Exception("Impossible status");
                }
            } catch (Exception e) {
                if (Util.DEBUG_MODE) {
                    Util.log(e.getMessage());
                    e.printStackTrace();
                }
                return false;
            }
        }



        @Override
        protected void onPostExecute(Boolean success) {
            showProgress(false);
            if (success) {
                Util.log("Upload worked");
                onRefresh();
            } else {
                Util.log("Upload failed");
                Toast.makeText(getContext(), "Upload failed, please try again", Toast.LENGTH_SHORT).show();
            }
        }
    }




    public void uploadImage(Bitmap picBitmap, String title){
        if(uploadTask != null){
            uploadTask.cancel(true);
        }
        PicUploadParams uploadPic = new PicUploadParams();
        ByteArrayOutputStream picOutputStream = new ByteArrayOutputStream();
        picBitmap.compress(Bitmap.CompressFormat.JPEG, 70, picOutputStream);
        uploadPic.pics.put("picture", picOutputStream);

        uploadPic.paramMap.put("title" , title);

        Constants.LATITUDE = simpleLocation.getLatitude();
        Constants.LONGITUDE = simpleLocation.getLongitude();

        Util.log("lat" + Constants.LATITUDE + "lon" + Constants.LONGITUDE);

        uploadPic.paramMap.put("geolong", "" + Constants.LONGITUDE);
        uploadPic.paramMap.put("geolat", "" + Constants.LATITUDE);
        showProgress(true);
        uploadTask = new PicUpload().execute(uploadPic);
    }
}