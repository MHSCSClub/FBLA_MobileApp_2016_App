package org.mamkschools.mhs.fbla_mobileapp_2016;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.design.widget.FloatingActionButton;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Commands;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Constants;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Picture;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.PictureEntry;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.PictureHelper;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.SecureAPI;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.Util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import im.delight.android.location.SimpleLocation;

/**
 * Fragment for evaluating pictures. This is on the main screen when the app is launched.
 * Created by jackphillips on 2/16/16.
 */
public class FragmentEvaluate extends Fragment implements View.OnClickListener{
    private int picNumber;
    private File location;
    private SimpleLocation simpleLocation;
    private boolean runOnce = true;

    private View rootView;

    private ImageView image;
    private TextView descriptionLabel;
    private TextView titleLabel;

    private int currentRating;

    private ArrayList<Picture> pics = new ArrayList<Picture>();

    private GetPicture picDl;

    private RelativeLayout [] bottomBars = new RelativeLayout[3];



    public static FragmentEvaluate newInstance(int picNumber, File location, SimpleLocation simpleLocation) {
        FragmentEvaluate fragment = new FragmentEvaluate();
        fragment.picNumber = picNumber;
        fragment.location = location;
        fragment.simpleLocation = simpleLocation;
        return fragment;
    }

    public FragmentEvaluate() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_evaluate, container, false);
        titleLabel = (TextView) rootView.findViewById(R.id.title_label);
        titleLabel.setText(R.string.no_pics);



        image = (ImageView) rootView.findViewById(R.id.imageView);
        image.setOnClickListener(this);
        descriptionLabel = (TextView) rootView.findViewById(R.id.description_label);

        Button up_button = (Button) rootView.findViewById(R.id.up_button);
        up_button.setOnClickListener(this);

        Button down_button = (Button) rootView.findViewById(R.id.down_button);
        down_button.setOnClickListener(this);

        Button back_button = (Button) rootView.findViewById(R.id.back_button);
        back_button.setOnClickListener(this);

        Button comment_button = (Button) rootView.findViewById(R.id.comment_button);
        comment_button.setOnClickListener(this);

        Button cancel_button = (Button) rootView.findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(this);

        Button refresh_button = (Button) rootView.findViewById(R.id.refresh_button);
        refresh_button.setOnClickListener(this);

        bottomBars[0] = (RelativeLayout) rootView.findViewById(R.id.primary_layout);
        bottomBars[1] = (RelativeLayout) rootView.findViewById(R.id.comment_layout);
        bottomBars[2] = (RelativeLayout) rootView.findViewById(R.id.refresh_layout);


        runFetch(picNumber);

        if(pics.size() > 0 && picNumber < pics.size()) {
            Util.log("From: " + picNumber);
            int itemId = pics.get(picNumber).entryid;
            String title = pics.get(picNumber).title;
            String user = pics.get(picNumber).username;
            titleLabel.setText(title.length() > 20 ? title.substring(0, 20) : title);
            descriptionLabel.setText(user);
        }

        hideKeyboard();

        return rootView;
    }

    public int getPictureId(int picture){
        if(pics.size() > 0 && picture < pics.size()) {

            return pics.get(picture).entryid;
        }
        return -1;

    }

    public String[] getData(int picture){
        String[] data = new String[3];
        if(pics.size() > 0 && picture < pics.size()) {
            data[0] = pics.get(picture).title;
            data[1] = pics.get(picture).username;
            double hours = pics.get(picture).hours;
            double miles = pics.get(picture).dist;
            data[2] = hours + (hours == 1 ? " hour ago, " : " hours ago, ");
            data[2] += miles + (miles == 1 ? " mile away" : " miles away");
            switchBottomBar(0);
            return data;
        }
        data[0] = "No More Pictures";
        switchBottomBar(2);
        data[1] = "";
        data[2] = "";
        return data;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (picDl != null && !picDl.getStatus().equals(AsyncTask.Status.FINISHED)) {
            picDl.cancel(true);
        }
    }

    public void runFetch(int itemId) {
        int picID = getPictureId(itemId);
        String[] data = getData(itemId);
        titleLabel.setText(data[0].length() > 20 ? data[0].substring(0, 20) : data[0]);
        descriptionLabel.setText(data[1]);
        //additionalLabel.setText(data[2]);
        if(picID > 0) {
            picDl = new GetPicture();
            picDl.execute(picID);
        }else{
            if(runOnce) {
                image.setImageResource(R.drawable.finish);
                new GetPictureInfo().execute((Void) null);
                switchBottomBar(2);
                runOnce = false;
            }
        }
    }
    @Override
    public void onClick(View v) {
        Map<String, String> postParams = new HashMap<>();
        int pid = getPictureId(picNumber);
        postParams.put("pid", "" + pid);

        if(pid == -1){
            Toast.makeText(getContext(), "No pictures left to rate", Toast.LENGTH_SHORT).show();
            return;
        }

        switch(v.getId()){

            case R.id.up_button:
                currentRating = 1;
                Util.log("up");
                switchBottomBar(1);
                //new SubmitRating().execute(postParams);
                break;

            case R.id.down_button:
                currentRating = 0;
                Util.log("down");
                switchBottomBar(1);
                //new SubmitRating().execute(postParams);
                break;
            case R.id.back_button:
                switchBottomBar(0);
                break;
            case R.id.comment_button:
                switchBottomBar(0);
                Toast.makeText(getContext(), "Add Comment", Toast.LENGTH_SHORT);
                postParams.put("like", "" + currentRating);
                new SubmitRating().execute(postParams);
                break;
            case R.id.cancel_button:
                switchBottomBar(0);
                postParams.put("like", "" + currentRating);
                new SubmitRating().execute(postParams);
                break;
            case R.id.refresh_button:
                new GetPictureInfo().execute((Void) null);
                break;



            /*case R.id.cancel_button:
                hideKeyboard();
                new SubmitRating().execute(postParams);
                this.picNumber += 1;
                runFetch(picNumber);
                break;
            case R.id.submit_button:
                hideKeyboard();
                new SubmitRating().execute(getRateParams(postParams));
                this.picNumber += 1;
                runFetch(picNumber);
                break; */
            case R.id.imageView:
                new PictureDialog(getContext(),
                        Constants.imageBitmap, titleLabel.getText().toString()).show();
                break;
        }
    }
    private void switchBottomBar(int bar){
        for(int i = 0; i < bottomBars.length; i++){
            if(i == bar){
                bottomBars[i].setVisibility(bottomBars[i].VISIBLE);
            }else{
                bottomBars[i].setVisibility(bottomBars[i].GONE);
            }
        }
    }
    /*private Map<String, String> getRateParams(Map<String, String> params) {
        SeekBar style = (SeekBar) rootView.findViewById(R.id.styleRating);
        int srating = 1 + (int) Math.round((double) style.getProgress() / (double) style.getMax() * 9.0);
        params.put("style", "" + srating);
        style.setProgress(style.getMax() / 2);

        EditText commentView = (EditText) rootView.findViewById(R.id.commentText);

        String comment = commentView.getText().toString();
        Util.log(comment);
        commentView.setText("");
        if(comment.length() > 0)
            params.put("comment", comment);
        return params;
    } */

    private class GetPicture extends AsyncTask<Integer, Void, Boolean> {
        SecureAPI picture = SecureAPI.getInstance(getContext());

        @Override
        protected Boolean doInBackground(Integer... params) {
            try{
                int pid = params[0];
                Util.log("picture/" + pid + "?authcode=" + Constants.AUTHCODE);
                picture.HTTPSFETCHPIC("picture/" + pid + "?authcode=" + Constants.AUTHCODE, new File(location, "picture.jpg"));
                Constants.imageBitmap = PictureHelper.getPictureBitmap(new File(location, "picture.jpg"));
            }catch(Exception e){
                Util.log(e.getMessage());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean v) {
            if(v){
                image.setImageBitmap(Constants.imageBitmap);
                //ViewGroup.MarginLayoutParams imageViewParams = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
                //image.setLayoutParams(imageViewParams);
            }else{
                Util.log("Life will go on");
            }
        }
    }

    private class SubmitRating extends AsyncTask<Map<String, String>, Void, Boolean> {
        SecureAPI picture = SecureAPI.getInstance(getContext());
        JSONObject result;

        @SafeVarargs
        @Override
        protected final Boolean doInBackground(Map<String, String>... params) {

            Map<String, String> finalParams = params[0];
            try {
                result = picture.HTTPSPOST("picture/" + finalParams.get("pid")
                        + "/comment?authcode=" + Constants.AUTHCODE, finalParams);
            } catch(Exception e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean v) {
            if(v) {
                Util.log("Rating worked");
                picNumber += 1;
                runFetch(picNumber);

            } else {
                Toast.makeText(rootView.getContext(), "Rating failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private class GetPictureInfo extends AsyncTask<Void, Void, Void> {

        private ArrayList<JSONObject> ret = new ArrayList<>();
        SecureAPI picture = SecureAPI.getInstance(getContext());
        ArrayList<Picture> pictures = new ArrayList<Picture>();


        @Override
        protected Void doInBackground(Void... params) {
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;


            int dist = 10000;
            int view = 15;

            //debug added actual locations
            Constants.LATITUDE = simpleLocation.getLatitude();
            Constants.LONGITUDE = simpleLocation.getLongitude();



            Util.log(Constants.LATITUDE + " " + Constants.LONGITUDE);

            try {
                JSONObject response = picture.HTTPSGET(Commands.Get.FETCH + Constants.AUTHCODE + "&ft_me=1" + "&ft_views=" + view
                        + "&geolong=" + Constants.LONGITUDE + "&geolat=" + Constants.LATITUDE + "&ft_dist=" + dist);

                JSONArray array = response.getJSONArray("data");

                for(int i = 0; i < array.length(); i++ ){

                    int views = array.getJSONObject(i).getInt("views");
                    int pid = array.getJSONObject(i).getInt("pid");
                    double geolat =  array.getJSONObject(i).getDouble("geolat");
                    double geolong = array.getJSONObject(i).getDouble("geolong");
                    double distance =  Math.round(array.getJSONObject(i).getDouble("dist"));
                    String title = array.getJSONObject(i).getString("title");
                    String username = array.getJSONObject(i).getString("username");
                    String created = array.getJSONObject(i).getString("created");

                    //Calculates priority
                    int p;
                    if(views <= 10)
                        p = (int) ((30 * Math.log(11 - views))/(Math.log(11)) + 40);
                    else
                        p = 30/(views - 10);

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    long different = new Date().getTime() -
                            simpleDateFormat.parse(array.getJSONObject(i)
                                    .getString("created")).getTime();
                    long elapsedHours = different / hoursInMilli;
                    if(elapsedHours < 10) {
                        p += 3 * elapsedHours;
                    } else {
                        p += 30;
                    }
                    double priority = p;
                    pictures.add(new Picture(pid, geolat, geolong, created, title, username, priority, (double) elapsedHours));

                }
            }catch (Exception e){
                if(Constants.DEBUG_MODE){
                    Util.log(e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            Util.log("Finished getting Picture Information");
            Collections.sort(pictures, new Comparator<Picture>() {
                public int compare(Picture p1, Picture p2) {
                    return (int) (p2.priority - p1.priority);
                }
            });

            pics = pictures;
            runOnce = pics.size() > 0;
            picNumber = 0;
            runFetch(picNumber);
        }
    }
    public void hideKeyboard(){
        try {
            InputMethodManager inputManager = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(
                    getActivity().getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }catch (Exception ignored){

        }
    }

}