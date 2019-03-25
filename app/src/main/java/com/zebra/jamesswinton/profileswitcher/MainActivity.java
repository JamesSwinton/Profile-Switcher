package com.zebra.jamesswinton.profileswitcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

  // Debugging
  private static final String TAG = "MainActivity";

  // Constants
  private static final String DATAWEDGE_ACTION = "com.symbol.datawedge.api.ACTION";
  private static final String GET_PROFILE_EXTRA = "com.symbol.datawedge.api.GET_ACTIVE_PROFILE";
  private static final String SWITCH_PROFILE_EXTRA = "com.symbol.datawedge.api.SWITCH_TO_PROFILE";

  private static final String SET_DEFAULT_PROFILE_EXTRA = "com.symbol.datawedge.api.SET_DEFAULT_PROFILE";
  private static final String RESET_DEFAULT_PROFILE_EXTRA = "com.symbol.datawedge.api.RESET_DEFAULT_PROFILE";

  private static final String DATAWEDGE_RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
  private static final String DEFAULT_INTENT_CATEGORY = "android.intent.category.DEFAULT";

  private static final String SEND_RESULT_EXTRA = "SEND_RESULT";
  private static final String COMMAND_IDENTIFIER_EXTRA = "COMMAND_IDENTIFIER";
  private static final String COMMAND_IDENTIFIER_VALUE = "59819715";

  private static final String CURRENT_PROFILE = "CURRENT_PROFILE";
  private static final String DEFAULT_PROFILE = "Profile0 (default)";
  private static final String VELOCITY_PROFILE = "Alternative";

  // Static Variables
  private static String mProfileToSwitchTo;
  private static SharedPreferences mSharedPreferences;

  // Non-Static Variables


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Init Shared Prefs
    mSharedPreferences = getPreferences(Context.MODE_PRIVATE);

    // Register Result Receiver
    IntentFilter resultFilter = new IntentFilter();
    resultFilter.addAction(DATAWEDGE_RESULT_ACTION);
    resultFilter.addCategory(DEFAULT_INTENT_CATEGORY);
    registerReceiver(datawedgeResultBroadcastReceiver, resultFilter);
  }

  @Override
  protected void onResume() {
    super.onResume();

    //
    mProfileToSwitchTo = mSharedPreferences.getString(CURRENT_PROFILE, DEFAULT_PROFILE)
        .equalsIgnoreCase(DEFAULT_PROFILE) ? VELOCITY_PROFILE : DEFAULT_PROFILE;

    // Get Active Profile
    switchProfile(mProfileToSwitchTo);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Unregister Receiver
    if (datawedgeResultBroadcastReceiver != null) {
      unregisterReceiver(datawedgeResultBroadcastReceiver);
    }
  }

  private void switchProfile(String profileName) {
    Log.i(TAG, "switchProfile: Switching to Profile: " + profileName);

    Intent switchProfileIntent = new Intent();
    switchProfileIntent.setAction(DATAWEDGE_ACTION);

    if (profileName.equalsIgnoreCase(DEFAULT_PROFILE)) {
      switchProfileIntent.putExtra(RESET_DEFAULT_PROFILE_EXTRA, "");
    } else {
      switchProfileIntent.putExtra(SET_DEFAULT_PROFILE_EXTRA, VELOCITY_PROFILE);
    }
    switchProfileIntent.putExtra(SEND_RESULT_EXTRA, "true");
    switchProfileIntent.putExtra(COMMAND_IDENTIFIER_EXTRA, COMMAND_IDENTIFIER_VALUE);
    sendBroadcast(switchProfileIntent);

    Log.i(TAG, "onResume: Switch Profile Intent sent...");
  }

  private BroadcastReceiver datawedgeResultBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      // Get Data From Intent
      String command = intent.getStringExtra("COMMAND");
      String commandIdentifier = intent.getStringExtra("COMMAND_IDENTIFIER");
      String result = intent.getStringExtra("RESULT");

      // Handle Generic Result
      StringBuilder newProfile = new StringBuilder();
      StringBuilder resultInfo = new StringBuilder();
      if(intent.hasExtra("RESULT_INFO")){
        Bundle bundle = intent.getBundleExtra("RESULT_INFO");
        Set<String> keys = bundle.keySet();
        for (String key: keys) {
          Object object = bundle.get(key);
          if(object instanceof String){
            // Get New Profile Name
            if (key.equalsIgnoreCase("PROFILE_NAME") && result.equalsIgnoreCase("SUCCESS")) {
              if (((String) object).equalsIgnoreCase(DEFAULT_PROFILE)) {
                newProfile.append("Default Profile Set");
              } else {
                newProfile.append("Alternative Profile Set");
              }

              // newProfile.append("Profile Set: ").append(object);
            }
            resultInfo.append(key).append(": ").append(object).append("\n");
          }
          else if(object instanceof String[]){
            String[] codes = (String[])object;
            for(String code : codes){
              resultInfo.append(key).append(": ").append(code).append("\n");
            }
          }
        }
      }

      // Log Data
      Log.i(TAG, "onReceive | "
          + "Command: " + command + " | "
          + "Identifier: " + commandIdentifier + " | "
          + "Result: " + result + " | "
          + "Result Info: " + resultInfo.toString());

      // Show Message
      Toast.makeText(MainActivity.this,
          TextUtils.isEmpty(newProfile.toString()) ? resultInfo.toString() : newProfile.toString(),
          Toast.LENGTH_SHORT).show();

      // Set Shared Pref based on Result
      if (result != null && result.equalsIgnoreCase("SUCCESS") ||
          result != null && result.equalsIgnoreCase("FAILURE") &&
              resultInfo.toString().contains("PROFILE_ALREADY_SET")) {
        // Set Shared Pref
        mSharedPreferences.edit().putString(CURRENT_PROFILE, mProfileToSwitchTo).apply();
        // Hide App
        moveTaskToBack(true);
      }
    }
  };
}
