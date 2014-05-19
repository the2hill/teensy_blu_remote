package com.toohill.blurcremote;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.opengl.Visibility;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;
import com.toohill.gopro.client.GoProClientTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class BluRCActivity extends Activity implements
        OnSharedPreferenceChangeListener, SurfaceHolder.Callback {

    // debug / logs
    private final boolean D = false;
    private static final String TAG = BluRCActivity.class.getSimpleName();

    // Message types sent from the BluetoothRfcommClient Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothRfcommClient Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    SharedPreferences prefs;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfcommClient mRfcommClient = null;

    // Layout View
    DualJoystickView mDualJoystick;
    private TextView mTxtStatus;

    // Menu
    private MenuItem mItemConnect;
    private MenuItem mServerConnect;
    private MenuItem mItemOptions;
    private MenuItem mItemAbout;

    // polar coordinates
    private double mRadiusL = 0, mRadiusR = 0;
    private double mAngleL = 0, mAngleR = 0;
    private boolean mCenterL = true, mCenterR = true;
    private int mDataFormat;
    private int mPanL;
    private int mTiltL;
    private int mPanR;
    private int mTiltR;

    // timer task
    private Timer mUpdateTimer;
    private int mTimeoutCounter = 0;
    private int mMaxTimeoutCount; // actual timeout = count * updateperiod
    private long mUpdatePeriod;

    // Configurations
    private String mHostAddress;
    private String mHostPort;
    private String mFeedURL;
    private String mPreceedingChar;
    private String mTrailingChar;
    private String mLeftYRange;
    private String mRightXRange;
    private String mLeftIdentifier;
    private String mRightIdentifier;

    // Camera configs
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mMediaPlayer;
    private ImageView imageView;
    private VideoView mVideoView;
    private Uri mGoproUri;
    private TextView mCamBattStatus;
    private boolean videoRunning;

    private Thread mImageViewThread;
    private ImageButton cameraButton;
    private Dialog serverDownDialog;

    // Handlers
    private Handler mUpdateTickerHandler;
    private Handler mUpdateStatsHandler;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = new BluetoothRfcommClient(this, mHandler);

        mTxtStatus = (TextView) findViewById(R.id.txt_status);
        mCamBattStatus = (TextView) findViewById(R.id.cam_batt_status);

        mDualJoystick = (DualJoystickView) findViewById(R.id.dualjoystickView);
        mDualJoystick.setOnJostickMovedListener(_listenerLeft, _listenerRight);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mUpdatePeriod = Long.parseLong(prefs.getString("updates_interval",
                "200"));
        mMaxTimeoutCount = Integer.parseInt(prefs.getString("maxtimeout_count",
                "20"));
        mDataFormat = Integer.parseInt(prefs.getString("data_format", "5"));

        mHostAddress = prefs.getString("host_address", "http://192.168.1.102");
        mHostPort = prefs.getString("host_port", "8080");
        mFeedURL = prefs.getString("feed_url", "photo.jpg");
        mPreceedingChar = prefs.getString("preceeding_char", "");
        mTrailingChar = prefs.getString("trailing_char", "&");
        mLeftYRange = prefs.getString("left_x_range", "1000-2100");
        mRightXRange = prefs.getString("right_x_range", "0-180");
        mLeftIdentifier = prefs.getString("left_identifier", "A");
        mRightIdentifier = prefs.getString("right_identifier", "B");

        videoRunning = false;
        mVideoView = (VideoView) findViewById(R.id.vid_view);
        mGoproUri = Uri.parse("http://10.5.5.9:8080/live/amba.m3u8");

        cameraButton = (ImageButton) findViewById(R.id.cam);
        OnClickListener ocl = new OnClickListener() {
            @Override
            public void onClick(View button) {
                videoViewHandler();
            }
        };
        cameraButton.setOnClickListener(ocl);

        mUpdateTickerHandler = new Handler();
        mUpdateTickerHandler.post(updateTickerRunner);

        mUpdateStatsHandler = new Handler();
        mUpdateStatsHandler.post(updateStatsRunner);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
        mMediaPlayer.setDisplay(holder);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Menus
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mItemConnect = menu.add("Connect Btooth");
        mServerConnect = menu.add("Connect Camera");
        mItemOptions = menu.add("Options");
        mItemAbout = menu.add("About");
        return (super.onCreateOptionsMenu(menu));
    }

    /**
     * Menus selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mItemConnect) {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } else if (item == mItemOptions) {
            startActivity(new Intent(this, OptionsActivity.class));
        } else if (item == mItemAbout) {
            AlertDialog about = new AlertDialog.Builder(this).create();
            about.setCancelable(false);
            about.setMessage("Blu RC Remote");
            about.setButton(1, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            about.show();
        } else if (item == mServerConnect) {
            // imageViewThreadHandle(true);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates to user preferences.
     */
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("updates_interval")) {
            // reschedule task
            mUpdateTimer.cancel();
            mUpdateTimer.purge();
            mUpdatePeriod = Long.parseLong(prefs.getString("updates_interval",
                    "200"));
            mUpdateTimer = new Timer();
            mUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateTicker();
                }
            }, mUpdatePeriod, mUpdatePeriod);
        } else if (key.equals("maxtimeout_count")) {
            mMaxTimeoutCount = Integer.parseInt(prefs.getString(
                    "maxtimeout_count", "20"));
        } else if (key.equals("data_format")) {
            mDataFormat = Integer.parseInt(prefs.getString("data_format", "5"));
        } else if (key.equals("preceeding_char")) {
            mPreceedingChar = prefs.getString("preceeding_char", "");
        } else if (key.equals("trailing_char")) {
            mTrailingChar = prefs.getString("trailing_char", "&");
        } else if (key.equals("left_x_range")) {
            mLeftYRange = prefs.getString("left_x_range", "1000-2100");
        } else if (key.equals("right_x_range")) {
            mRightXRange = prefs.getString("right_x_range", "0-180");
        } else if (key.equals("left_identifier")) {
            mLeftIdentifier = prefs.getString("left_identifier", "A");
        } else if (key.equals("right_identifier")) {
            mRightIdentifier = prefs.getString("right_identifier", "B");
        }
    }

    /**
     * Called when the activity is first created and on resume
     */
    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mRfcommClient != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (mRfcommClient.getState() == BluetoothRfcommClient.STATE_NONE) {
                // Start the Bluetooth RFCOMM services
                mRfcommClient.start();
            }

            // if (imageView != null) {
            // imageViewThreadHandle(true);
            // }
        }
    }

    /**
     * Called when the activity is destroyed
     */
    @Override
    public void onDestroy() {
        // mUpdateTimer.cancel();
        // Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null)
            mRfcommClient.stop();
        // Stop the ImageView Thread
        // imageViewThreadHandle(false);
        mUpdateTickerHandler.removeCallbacksAndMessages(null);
        mUpdateStatsHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * Called when the activity is paused or not in focus.
     */
    @Override
    public void onPause() {
        if (serverDownDialog != null)
            serverDownDialog.dismiss();
        super.onPause();
    }

    /**
     * Called when the user is trying to escape.
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Bluetooth Joystick")
                .setMessage("Close this controller?")
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).setNegativeButton("No", null).show();
    }

    private void videoViewHandler() {
        if (mVideoView.isPlaying()) {
            videoRunning = false;
            mVideoView.stopPlayback();
            mVideoView.setVisibility(View.GONE);
            cameraButton.setSelected(false);
        } else {
            cameraButton.setSelected(true);
            mVideoView.setVisibility(View.VISIBLE);
            mVideoView.setVideoURI(mGoproUri);
            mVideoView.requestFocus();
            mVideoView.postInvalidateDelayed(100);
            new Thread(new Runnable() {
                public void run() {
                    mVideoView.start();

                }
            }).start();
        }
    }

    Runnable updateTickerRunner = new Runnable() {
        public void run() {
            updateTicker();
            mUpdateTickerHandler.postDelayed(this, mUpdatePeriod);
        }
    };

    Runnable updateStatsRunner = new Runnable() {
        public void run() {
//            updateStatsGopro();
            // TODO: make update setting
            mUpdateTickerHandler.postDelayed(this, 10000);
        }
    };

    private void imageViewThreadHandle(boolean isEnabled) {
        if (serverDownDialog != null)
            serverDownDialog.dismiss();

        try {
            if (isEnabled) {
                if (mImageViewThread == null || !mImageViewThread.isAlive()) {
                    mImageViewThread = new ImageViewThread(this, imageView,
                            cameraButton, String.format("%s:%s/%s", prefs
                            .getString("host_address",
                                    "http://192.168.1.102"), prefs
                            .getString("host_port", "8080"), prefs
                            .getString("feed_url", "photo.jpg")));
                    mImageViewThread.start();

                }
            } else {
                ((ImageViewThread) mImageViewThread).isRunning = false;
                mImageViewThread.interrupt();
                cameraButton.setSelected(false);
            }
        } catch (Exception ex) {
            Log.d("BLURC",
                    "There was a problem communicating with the service." + ex);
            serverNotConnectedPopup();
        }
    }

    /**
     * Display dialog alerting the video server is no longer running.
     */
    private void serverNotConnectedPopup() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Image Server Down");
        alertDialogBuilder
                .setMessage(
                        "Looks like the image feed is not up, would you like to try again?")
                .setCancelable(false)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // imageViewThreadHandle(true);
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.dismiss();
                    }
                });
        serverDownDialog = alertDialogBuilder.create();
        serverDownDialog.show();
    }

    /**
     * Joystick movement *
     */
    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        public void OnMoved(int pan, int tilt) {
            mPanL = pan;
            mTiltL = tilt;
            mRadiusL = Math.sqrt((pan * pan) + (tilt * tilt));
            // mAngleL = Math.atan2(pan, tilt);
            mAngleL = Math.atan2(-pan, -tilt);
            // mTxtDataL.setText(String.format("( r%.0f, %.0f\u00B0 )",
            // Math.min(mRadiusL, 10), mAngleL * 180 / Math.PI));
            mCenterL = false;
        }

        public void OnReleased() {
            // Unused
        }

        public void OnReturnedToCenter() {
            mRadiusL = mAngleL = 0;
            mPanL = 0;
            mTiltL = 0;
            updateTicker();
            mCenterL = true;
        }
    };

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        public void OnMoved(int pan, int tilt) {
            mPanR = pan;
            mTiltR = tilt;
            mRadiusR = Math.sqrt((pan * pan) + (tilt * tilt));
            // mAngleR = Math.atan2(pan, tilt);
            mAngleR = Math.atan2(-pan, -tilt);
            // mTxtDataR.setText(String.format("( r%.0f, %.0f\u00B0 )",
            // Math.min(mRadiusR, 10), mAngleR * 180 / Math.PI));
            mCenterR = false;
        }

        public void OnReleased() {
            // Unused
        }

        public void OnReturnedToCenter() {
            mRadiusR = mAngleR = 0;
            mPanR = 0;
            mTiltR = 0;
            updateTicker();
            mCenterR = true;
        }
    };

    /**
     * Sends a message to the defined bluetooth device.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        Log.d(TAG, "Message to send: " + message);
        // Check that we're actually connected before trying anything
        if (mRfcommClient.getState() != BluetoothRfcommClient.STATE_CONNECTED) {
            // Toast.makeText(this, R.string.not_connected,
            // Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothRfcommClient to write
            byte[] send = message.getBytes();
            mRfcommClient.write(send);
        }
    }

    /**
     * Updates values and sends messages based on timer or when directly calling
     * the method itself.
     */
    private void updateTicker() {

        // if either of the joysticks is not on the center, or timeout occurred
        if (!mCenterL
                || !mCenterR
                || (mTimeoutCounter >= mMaxTimeoutCount && mMaxTimeoutCount > -1)) {
            // limit to {0..10}
            byte radiusL = (byte) (Math.min(mRadiusL, 10.0));
            byte radiusR = (byte) (Math.min(mRadiusR, 10.0));
            // scale to {0..35}
            byte angleL = (byte) (mAngleL * 18.0 / Math.PI + 36.0 + 0.5);
            byte angleR = (byte) (mAngleR * 18.0 / Math.PI + 36.0 + 0.5);
            if (angleL >= 36)
                angleL = (byte) (angleL - 36);
            if (angleR >= 36)
                angleR = (byte) (angleR - 36);

            if (D) {
                Log.d(TAG, String.format("%d, %d, %d, %d", radiusL, angleL,
                        radiusR, angleR));
            }

            // Left JoyStick
            String[] lYrange = mLeftYRange.split("-");
            sendMessage(String.format("%s%s%s", mLeftIdentifier, String
                    .valueOf(mapValues(mTiltL, -300, 300, lYrange[0],
                            lYrange[1])), mTrailingChar));

            // RightJoyStick
            String[] rXrange = mRightXRange.split("-");
            sendMessage(String.format("%s%s%s", mRightIdentifier,
                    String.valueOf(mapValues(mPanR, -300, 300, rXrange[0],
                            lYrange[1])), mTrailingChar));

            mTimeoutCounter = 0;
        } else {
            if (mMaxTimeoutCount > -1)
                mTimeoutCounter++;
        }
    }

    private void updateStatsGopro() {
        // Update Camera info
        // GoProClient gp = new GoProClient("10.5.5.9", "8080", "booger11");
        new GoProClientTask(mCamBattStatus).execute("10.5.5.9", "8080",
                "booger11");
        // try {
        // // mCamBattStatus.setText(String.valueOf(gp.getStats().battPercent) +
        // "%");
        // } catch (Exception e) {
        // Log.d(TAG, e.getMessage(), e);
        // }
    }

    /**
     * Maps a value to a range of expected values.
     *
     * @param x
     * @param in_min
     * @param in_max
     * @param out_min
     * @param out_max
     * @return
     */
    private long mapValues(long x, long in_min, long in_max, String out_min,
                           String out_max) {
        return mapValues(x, in_min, in_max, Integer.valueOf(out_min),
                Integer.valueOf(out_max));
    }

    private long mapValues(long x, long in_min, long in_max, long out_min,
                           long out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    /**
     * The handler that waits for results from external activity.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(
                            DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter
                            .getRemoteDevice(address);
                    // Attempt to connect to the device
                    mRfcommClient.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    /**
     * The Handler that gets information back from the BluetoothRfcommClient
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothRfcommClient.STATE_CONNECTED:
                            mTxtStatus.setText(R.string.title_connected_to);
                            mTxtStatus.append(" " + mConnectedDeviceName);
                            break;
                        case BluetoothRfcommClient.STATE_CONNECTING:
                            mTxtStatus.setText(R.string.title_connecting);
                            break;
                        case BluetoothRfcommClient.STATE_NONE:
                            mTxtStatus.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    // byte[] readBuf = (byte[]) msg.obj;
                    // int data_length = msg.arg1;
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "Connected to " + mConnectedDeviceName,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };
}
