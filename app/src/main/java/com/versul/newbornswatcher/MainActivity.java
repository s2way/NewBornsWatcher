package com.versul.newbornswatcher;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private CardListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setVisibility(TextView.GONE);
                } else {
                    mInformationTextView.setVisibility(TextView.VISIBLE);
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };

        mInformationTextView = (TextView) findViewById(R.id.informationTextView);

        try {
            showCards();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
        try {
            showCards();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getApplicationContext().registerReceiver(mMessageReceiver, new IntentFilter("receiver"));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                showCards();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private RecyclerView mRecyclerView;

    private void showCards() throws JSONException {
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CardListAdapter(getDataSet());
        mRecyclerView.setAdapter(mAdapter);
        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(mRecyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    try {
                                        removeAlert(position);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    mAdapter.notifyItemRemoved(position);
                                }
                                try {
                                    datasetChanged();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    try {
                                        removeAlert(position);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    mAdapter.notifyItemRemoved(position);
                                }
                                try {
                                    datasetChanged();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

        mRecyclerView.addOnItemTouchListener(swipeTouchListener);
    }

    private void datasetChanged() throws JSONException {
        showCards();
    }

    class CardListAdapter extends RecyclerView.Adapter<CardViewHolder> {
        private final List<Alert> alerts;

        public CardListAdapter(List<Alert> alerts) {
            this.alerts = alerts;
        }

        @Override
        public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            final View v = layoutInflater.inflate(R.layout.card_view, viewGroup, false);
            return new CardViewHolder(v);
        }

        @Override
        public void onBindViewHolder(CardViewHolder quizViewHolder, int i) {
            Alert alert = alerts.get(i);
            quizViewHolder.origin.setText(alert.getOrigin());
            quizViewHolder.message.setText(alert.getMessage());
        }

        @Override
        public int getItemCount() {
            return alerts.size();
        }
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        TextView origin;
        TextView message;

        CardViewHolder(View itemView) {
            super(itemView);
            origin = (TextView) itemView.findViewById(R.id.textView);
            message = (TextView) itemView.findViewById(R.id.textView2);
        }
    }

    public ArrayList<Alert> getDataSet() throws JSONException {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.watcher), Context.MODE_PRIVATE);
        String key = getResources().getString(R.string.alerts);
        JSONArray alerts = new JSONArray(sharedPref.getString(key, "[]"));

        ArrayList<Alert> dataSet = new ArrayList<>();
        int i = 0, length = alerts.length();
        for (i = 0; i < length; i++) {
            JSONObject alert = alerts.getJSONObject(i);
            dataSet.add(new Alert(alert.getString(getString(R.string.origin)), alert.getString(getString(R.string.message))));
        }

        return dataSet;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void removeAlert(int position) throws JSONException {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.watcher), Context.MODE_PRIVATE);
        String key = getResources().getString(R.string.alerts);
        JSONArray alerts = new JSONArray(sharedPref.getString(key, "[]"));
        alerts.remove(position);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, alerts.toString());
        editor.commit();
    }


    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        getApplicationContext().unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}

