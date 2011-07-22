// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.wireless.speed.speedometer;

import com.google.wireless.speed.speedometer.MeasurementScheduler.SchedulerBinder;
import com.google.wireless.speed.speedometer.util.RuntimeUtil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TabHost;

/**
 * The main UI thread that manages different tabs
 * TODO(Wenjie): Implement button handler to stop all threads and the Speedometer app
 * @author wenjiezeng@google.com (Steve Zeng)
 *
 */
public class SpeedometerApp extends TabActivity {
  
  public static final String TAG = "Speedometer";
  private final int EXIT_DIALOG_ID = 0;
  
  private MeasurementScheduler scheduler;
  private AlertDialog exitConfirmationDialog;
  private boolean isBounded = false;  
  /** Defines callbacks for service binding, passed to bindService() */
  private ServiceConnection serviceConn = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName className,
              IBinder service) {
          // We've bound to LocalService, cast the IBinder and get LocalService instance
          SchedulerBinder binder = (SchedulerBinder) service;
          scheduler = binder.getService();
          isBounded = true;
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
        isBounded = false;
      }
  };
  
  public MeasurementScheduler getScheduler() {
    if (isBounded) {
      return this.scheduler;
    } else {
      return null;
    }
  }
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    Resources res = getResources(); // Resource object to get Drawables
    TabHost tabHost = getTabHost();  // The activity TabHost
    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
    Intent intent;  // Reusable Intent for each tab

    // Create an Intent to launch an Activity for the tab (to be reused)
    intent = new Intent().setClass(this, MeasurementMonitorActivity.class);

    // Initialize a TabSpec for each tab and add it to the TabHost
    spec = tabHost.newTabSpec("measurement_monitor").setIndicator("Console",
      res.getDrawable(R.drawable.tablet)).setContent(intent);
    tabHost.addTab(spec);

    // Do the same for the other tabs
    intent = new Intent().setClass(this, MeasurementCreationActivity.class);
    spec = tabHost.newTabSpec("measurement_creation").setIndicator("Create Measurement",
      res.getDrawable(R.drawable.tablet)).setContent(intent);
    tabHost.addTab(spec);

    tabHost.setCurrentTab(0);
    
    createAlertDialog();
    RuntimeUtil.setActivity(this);
    
    PhoneUtils.setGlobalContext(getApplicationContext());
    
    // We only need one instance of scheduler thread
    Intent schedulerIntent = new Intent(this, MeasurementScheduler.class);
    this.startService(schedulerIntent);
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    // Bind to LocalService
    Intent intent = new Intent(this, MeasurementScheduler.class);
    bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    PhoneUtils.releaseGlobalContext();
    if (isBounded) {
      unbindService(serviceConn);
      isBounded = false;
    }
  }
   
  /* TODO(Wenjie): This is a temporary solution to cleanly stop the app as well as all
   * background threads whenever the BACK button is clicked. 
   * Later should provide explicit buttons for user to stop the background threads and the app
   * in a context menu.
   * */
  
  @Override
  public void onBackPressed() {
    this.showDialog(this.EXIT_DIALOG_ID);
  }
  
  @Override
  protected Dialog onCreateDialog(int id, Bundle args) {
    return this.exitConfirmationDialog;
  }
  
  private void createAlertDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage("Do you want to exit Speedometer?")
           .setCancelable(false)
           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
             @Override
               public void onClick(DialogInterface dialog, int id) {
                 Log.i(TAG, "User requests exit. Stopping all threads");
                 SpeedometerApp.this.scheduler.requestStop();
                 SpeedometerApp.this.finish();
                 System.gc();
                 System.exit(0);
               }
           })
           .setNegativeButton("No", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
               }
           });
    this.exitConfirmationDialog = builder.create();
  } 
}