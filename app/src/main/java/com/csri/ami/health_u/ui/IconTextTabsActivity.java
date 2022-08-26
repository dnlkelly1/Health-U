package com.csri.ami.health_u.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.analyze.graphing.StoredDataManager;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import java.util.ArrayList;
import java.util.List;

public class IconTextTabsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{

    public StoredDataManager storedDataManager;
    SensorFragment senserFrag;
    HistoryFragment historyFragment;
    FutureFragment futureFragment;

    public Switch sensorToggleSwitch;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private boolean[] tabsHighlighted = {false,false,false};
    //private Drawable[] tabIcons_highlight;
    //private Drawable[] tabIcons;

    private int[] tabIcons_highlight = {
            R.drawable.histroy_h,
            R.drawable.pie_h,
            R.drawable.doctor_h
    };

    private int[] tabIcons = {
           R.drawable.ic_history_white_36dp,
           R.drawable.pie,
           R.drawable.doctor1
   };

    @Override
    protected void onResume()
    {
        super.onResume();

        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_icon_text_tabs);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        setupTabIcons();

        if(shouldAskPermission())
        {
            AskPermissions();

        }
        else
        {
            InformedConsent ic = new InformedConsent(this);
            ic.show();
        }

        ReminderQuestionnaire.SetAnalysisAlarm(this);

    }

    private void AskPermissions()
    {
        String storagePerm = "android.permission.READ_EXTERNAL_STORAGE";
        String locationPerm = "android.permission.ACCESS_FINE_LOCATION";
        String micPerm = "android.permission.RECORD_AUDIO";

        ArrayList<String> perms = new ArrayList<String>();


        int hasReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(hasReadPermission != PackageManager.PERMISSION_GRANTED)
        {
            perms.add(storagePerm);
        }

        int hasGPSPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if(hasGPSPermission != PackageManager.PERMISSION_GRANTED)
        {
            perms.add(locationPerm);
        }

        int hasMICPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        if(hasMICPermission != PackageManager.PERMISSION_GRANTED)
        {
            perms.add(micPerm);
        }

        if(perms.size() > 0)
        {
            requestPermissions(perms.toArray(new String[perms.size()]), PERMISSIONREQUEST_READ);
        }
    }


    final private int PERMISSIONREQUEST_READ = 202;


    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSIONREQUEST_READ:
                int count =0;
                for(int i=0;i<grantResults.length;i++)
                {
                    if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    {
                        count++;
                    }
                }
                if(count == grantResults.length)
                {
                    InformedConsent ic = new InformedConsent(this);
                    ic.show();
                }
                else
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);//, R.style.MyDialogTheme);
                    builder.setTitle("Android Permissions");
                    String line = "In order to take part in this research study, Android requires that you Allow permissions to use File Storage, Location and Audio. Do you want to reconsider allowing access to these permissions?";
                    builder.setMessage(line);

                    builder.setPositiveButton("Yes: Change Permissions", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialogInterface,int id)
                        {
                            dialogInterface.dismiss();
                            AskPermissions();
                        }
                    });

                    builder.setNegativeButton("No: Leave App", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialogInterface,int id)
                        {
                            finish();
                        }
                    });
                    builder.show();
                }

            default:
                super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        }
    }

    private boolean shouldAskPermission()
    {
        return (Build.VERSION.SDK_INT  > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView,boolean isChecked)
    {
//        ViewPagerAdapter a = (ViewPagerAdapter)viewPager.getAdapter();
//        SensorFragment f = (SensorFragment)a.getItem(1);
////        if(senserFrag == null)
////        {
////            senserFrag = new SensorFragment();
////        }
//        if(f != null) {
//
//            f.CheckChanged(isChecked);
//        }
    }

    public void ConsentFinished(boolean newlyAccepted)
    {
        if(newlyAccepted && senserFrag != null)
        {
            senserFrag.InitSensor();
            //Intent backgroundServiceIntent = new Intent(this, BackgroundService.class);
            //startService(backgroundServiceIntent);
            //Register r = new Register(getBaseContext(), true);
        }
    }



    public void sendMessage_ToggleHistoryDay(View view)
    {


       // historyFragment.DayButtonClicked();
    }

    public void sendMessage_ToggleHistoryWeek(View view)
    {
       // historyFragment.WeekButtonCLicked();
    }

    public void fabClick(View view)
    {
        if(futureFragment != null)
        {
//            AlertDialog.Builder dialog = new AlertDialog.Builder(IconTextTabsActivity.this);//, R.style.MyDialogTheme);
//            dialog.setTitle(getResources().getString(R.string.app_name) + " Information");
//
//
//            View v = (View)getLayoutInflater().inflate(R.layout.experiment_info_dialog,null);
//            dialog.setView(v);
//
//            dialog.setNeutralButton(getResources().getString(R.string.close), null);
//
//            AlertDialog b = dialog.create();
//            b.show();
            String url = "http://healthuexperiment.com";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    }

    public void sendMessage(View view)
    {
        boolean connectionFound = SummaryDataUploadAlarm.ConnectivityStatus(this);
        if(connectionFound) {
            Intent intent = new Intent(this, Questionnaire.class);

            startActivity(intent);
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
            builder.setTitle(getResources().getString(R.string.noconnection));
            builder.setMessage(getResources().getString(R.string.noconnection_mes));
            builder.setNeutralButton(getResources().getString(R.string.close), null);
            builder.show();
        }
    }

    private void setupTabIcons() {

        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
    }

    private static String FRAGMENT_TAG = "fragmenttag";

    private void setupViewPager(ViewPager viewPager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        //if(senserFrag == null)
        {
            senserFrag = new SensorFragment();
           // senserFrag.setRetainInstance(true);
        }

//        FragmentManager fm = getSupportFragmentManager();
//        historyFragment = (HistoryFragment)fm.findFragmentByTag(FRAGMENT_TAG);
//
//        if(historyFragment== null)
//        {
//            historyFragment = new HistoryFragment();
//
//            fm.beginTransaction().add(historyFragment,FRAGMENT_TAG).commit();
//        }
        //if(historyFragment == null)
        {
            historyFragment = new HistoryFragment();
           // historyFragment.setRetainInstance(true);
        }


        //if(futureFragment == null)
        {
            futureFragment = new FutureFragment();
        }
        adapter.addFrag(historyFragment, "");//, getResources().getString(R.string.past));
        adapter.addFrag(senserFrag, "");// getResources().getString(R.string.present));
        adapter.addFrag(futureFragment, "");// getResources().getString(R.string.future));

        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(adapter);

        final SharedPreferences prefs = getApplicationContext().getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
        boolean alreadyTakenQ = prefs.getBoolean(InformedConsent.PREFS_INFORMED_TAKEN_QUESTIONNAIRE, false);

        if(alreadyTakenQ)
        {
            viewPager.setCurrentItem(0, true);
        }
        else
        {
            viewPager.setCurrentItem(2, true);
        }
    }

    private void UpdatedHighlightedTab(int pos)
    {
        for(int i=0;i<tabsHighlighted.length;i++)
        {
            if(i == pos && !tabsHighlighted[i])
            {
                tabLayout.getTabAt(i).setIcon(tabIcons_highlight[i]);
                tabsHighlighted[i] = true;
            }
            else if(i != pos && tabsHighlighted[i])
            {
                tabLayout.getTabAt(i).setIcon(tabIcons[i]);
                tabsHighlighted[i] = false;
                //do nothing
            }
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager)
        {
            super(manager);


        }

        @Override
        public void setPrimaryItem(ViewGroup v,int pos,Object o)
        {
            super.setPrimaryItem(v, pos, o);

            UpdatedHighlightedTab(pos);


        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
