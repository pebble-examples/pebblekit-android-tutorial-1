package com.getpebble.pkat1;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


public class MainActivity extends Activity {

    private Handler mHandler = new Handler();

    private PebbleKit.PebbleDataReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button launchButton = (Button)findViewById(R.id.launch_button);
        launchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
            final Context context = getApplicationContext();

            boolean isConnected = PebbleKit.isWatchConnected(context);

            if(isConnected) {
                // Launch the sports app
                PebbleKit.startAppOnPebble(context, Constants.SPORTS_UUID);

                // Send data 5s after launch
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                    // Send a time and distance to the sports app
                    PebbleDictionary outgoing = new PebbleDictionary();
                    outgoing.addString(Constants.SPORTS_TIME_KEY, "12:52");
                    outgoing.addString(Constants.SPORTS_DISTANCE_KEY, "23.8");

                    // Show metric or imperial units
                    boolean metric = true;
                    if(metric) {
                        outgoing.addUint8(Constants.SPORTS_UNITS_KEY, (byte) Constants.SPORTS_UNITS_METRIC);
                    } else {
                        outgoing.addUint8(Constants.SPORTS_UNITS_KEY, (byte) Constants.SPORTS_UNITS_IMPERIAL);
                    }

                    // Set speed or pace display
                    boolean speed = true;
                    if(speed) {
                        outgoing.addUint8(Constants.SPORTS_LABEL_KEY, (byte) Constants.SPORTS_DATA_SPEED);
                    } else {
                        outgoing.addUint8(Constants.SPORTS_LABEL_KEY, (byte) Constants.SPORTS_DATA_PACE);
                    }
                    outgoing.addString(Constants.SPORTS_DATA_KEY, (speed ? "6.28" : "3:00"));

                    PebbleKit.sendDataToPebble(getApplicationContext(), Constants.SPORTS_UUID, outgoing);
                    }

                }, 5000L);

                Toast.makeText(context, "Launching...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Watch is not connected!", Toast.LENGTH_LONG).show();
            }
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Construct output String
        StringBuilder builder = new StringBuilder();
        builder.append("Pebble Info\n\n");

        // Is the watch connected?
        boolean isConnected = PebbleKit.isWatchConnected(this);
        builder.append("Watch connected: " + (isConnected ? "true" : "false")).append("\n");

        // What is the firmware version?
        PebbleKit.FirmwareVersionInfo info = PebbleKit.getWatchFWVersion(this);
        builder.append("Firmware version: ");
        builder.append(info.getMajor()).append(".");
        builder.append(info.getMinor()).append("\n");

        // Is AppMessage supported?
        boolean appMessageSupported = PebbleKit.areAppMessagesSupported(this);
        builder.append("AppMessage supported: " + (appMessageSupported ? "true" : "false"));

        TextView textView = (TextView)findViewById(R.id.text_view);
        textView.setText(builder.toString());

        // Get information back from the watchapp
        if(mReceiver == null) {
            mReceiver = new PebbleKit.PebbleDataReceiver(Constants.SPORTS_UUID) {

                @Override
                public void receiveData(Context context, int id, PebbleDictionary data) {
                    // Always ACKnowledge the last message to prevent timeouts
                    PebbleKit.sendAckToPebble(getApplicationContext(), id);

                    // Get action and display
                    int state = data.getUnsignedIntegerAsLong(Constants.SPORTS_STATE_KEY).intValue();
                    Toast.makeText(getApplicationContext(),
                            (state == Constants.SPORTS_STATE_PAUSED ? "Resumed!" : "Paused!"), Toast.LENGTH_SHORT).show();
                }

            };
        }
        PebbleKit.registerReceivedDataHandler(this, mReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

    }
}
