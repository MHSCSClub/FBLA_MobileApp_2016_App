package org.mamkschools.mhs.fbla_mobileapp_2016;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.design.widget.FloatingActionButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.mamkschools.mhs.fbla_mobileapp_2016.lib.PictureContract;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.util;

import java.io.File;

/**
 * Created by jackphillips on 2/16/16.
 */
public class EvaluationFragment extends Fragment implements View.OnClickListener {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    private SQLiteDatabase db;
    private int picNumber;
    private File location;
    LayoutInflater inflater;
    ViewGroup container;
    Bundle savedInstanceState;
    public static EvaluationFragment newInstance(SQLiteDatabase db, int picNumber, File location) {
        EvaluationFragment fragment = new EvaluationFragment();
        fragment.db = db;
        fragment.picNumber = picNumber;
        fragment.location = location;
        return fragment;
    }

    public EvaluationFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;



        String[] projection = {
                PictureContract.PictureEntry._ID,
                PictureContract.PictureEntry.COLUMN_NAME_PICTURE_ID,
                PictureContract.PictureEntry.COLUMN_NAME_GEOLONG,
                PictureContract.PictureEntry.COLUMN_NAME_GEOLAT,
                PictureContract.PictureEntry.COLUMN_NAME_USERNAME,
                PictureContract.PictureEntry.COLUMN_NAME_VIEWS,
                PictureContract.PictureEntry.COLUMN_NAME_TITLE
        };

        String sortOrder =
                PictureContract.PictureEntry.COLUMN_NAME_PICTURE_ID + " ASC";

        Cursor c = db.query(
                PictureContract.PictureEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        View rootView = inflater.inflate(R.layout.fragment_main_activity_swipes, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText("No More Pictures");

        View imageFrame = rootView.findViewById(R.id.imageFrame);
       // imageFrame.getLayoutParams().height = imageFrame.getLayoutParams().width;
        //util.log(""+ imageFrame.getLayoutParams().width);

        ImageView image = (ImageView) rootView.findViewById(R.id.imageView);


        FloatingActionButton yes = (FloatingActionButton) rootView.findViewById(R.id.yesButton);
        yes.setOnClickListener(this);

        FloatingActionButton no = (FloatingActionButton) rootView.findViewById(R.id.noButton);
        no.setOnClickListener(this);

        if(c.getCount() > 0 && picNumber < c.getCount()){
            util.log("From: " + picNumber);
            c.moveToPosition(picNumber);
            int itemId = c.getInt(c.getColumnIndexOrThrow(PictureContract.PictureEntry.COLUMN_NAME_PICTURE_ID));
            String title = c.getString(c.getColumnIndexOrThrow(PictureContract.PictureEntry.COLUMN_NAME_TITLE));
            String user = c.getString(c.getColumnIndexOrThrow(PictureContract.PictureEntry.COLUMN_NAME_USERNAME));
            int views = c.getInt(c.getColumnIndexOrThrow(PictureContract.PictureEntry.COLUMN_NAME_VIEWS));
            textView.setText(title + "\n" + user + "\n" + "Views: " + views);
            image.setImageURI(Uri.fromFile(new File(location, "picture" + itemId + ".jpg")));
        }

        return rootView;
    }
    public void changePicture(int picture){
        this.picNumber = picture;
        onCreateView(inflater, container, savedInstanceState);


    }

    @Override
    public void onClick(View v) {
        FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
        switch(v.getId()){
            case R.id.noButton:
                this.picNumber += 1;
                fragTransaction.detach(this);
                fragTransaction.attach(this);
                fragTransaction.commit();
                break;
            case R.id.yesButton:
                this.picNumber += 1;
                fragTransaction.detach(this);
                fragTransaction.attach(this);
                fragTransaction.commit();
                break;
        }
    }
}