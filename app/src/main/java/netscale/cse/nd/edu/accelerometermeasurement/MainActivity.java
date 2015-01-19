package netscale.cse.nd.edu.accelerometermeasurement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Directory to save files in is "/sdcard/accelerometerLogs/", but Android
    // takes care of the /sdcard part on its own
    private static final String LOG_DIRECTORY = "/accelerometerLogs/";

    // Declare graphical elements
    TextView xValueTextView;
    TextView yValueTextView;
    TextView zValueTextView;
    TextView rateValueTextView;
    TextView recordingView;
    EditText netidEditText;
    String experimentName;
    String[] activities = new String[] { "Shoes", "Baseline", "Backpack", "Fast", "Outside" };
    Button startButton;
    Button stopButton;

    // Declare variables associated with File operations
    boolean recording = false;
    File sdCard;
    File dir;
    File recordingFile;
    BufferedWriter bw;

    // Event listener that gets called with updated sensor data
    AccelerometerEventListener eventListener;

    // The sample rate that is currently selected, defaults to Fastest
    int sensorSampleRate = SensorManager.SENSOR_DELAY_FASTEST;

    // Sensors used to get data
    Sensor mAccelerometer;
    Sensor mGravity;
    SensorManager mSensorManager;


    @Override
    public void onBackPressed() {
        // DoNothing
        return;
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch( event.getKeyCode() ) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_POWER:
                break;

            default:
                break;
        }
        return true;
    }

    // This is called by Android when the application / activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        // Programatically get access to graphical elements
        xValueTextView = (TextView) findViewById(R.id.textViewXValue);
        yValueTextView = (TextView) findViewById(R.id.textViewYValue);
        zValueTextView = (TextView) findViewById(R.id.textViewZValue);
        rateValueTextView = (TextView) findViewById(R.id.textViewRateValue);
        recordingView = (TextView) findViewById(R.id.textViewSampling);
        recordingView.setVisibility(View.INVISIBLE);
        netidEditText = (EditText) findViewById(R.id.editTextNetid);
        //experimentEditText = (EditText) findViewById(R.id.editTextExperimentName);
        startButton = (Button) findViewById(R.id.button1);
        stopButton = (Button) findViewById(R.id.button2);

        //startButton.setClickable(recording);
        //stopButton.setClickable(!recording);

        // This is the drop down menu
        Spinner spinner = (Spinner) findViewById(R.id.editTextExperimentName);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, activities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // This sets a listener to handle selections in the activities drop down menu
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                Spinner spinner = (Spinner) arg0;
                experimentName = spinner.getItemAtPosition(arg2).toString();

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });

        // Default to 'Baseline' experiment name
        spinner.setSelection(1);



        // Setup listeners for buttons
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Display label that indicates we are recording
                recordingView.setVisibility(View.VISIBLE);

                // Update button clickable status
                startButton.setClickable(recording);
                stopButton.setClickable(!recording);
                startButton.requestFocus();

                // Create the file that we will write to
                // Naming format is
                // timestamp_netid_experimentName_Accelerometer_Log.csv
                String timestamp = String.valueOf(System.currentTimeMillis());

                try {
                    String netid = netidEditText.getText().toString();
                    if (netid.equals("")) {
                        netid = "NA";
                    }
                    //String experimentName = experimentEditText.getText().toString();
                    if (experimentName.equals("")) {
                        experimentName = "NA";
                    }

                    String filename = timestamp + "_" + netid + "_"
                            + experimentName + "_Accelerometer_Log.csv";

                    File recordingFile = new File(dir, filename);
                    FileWriter fw = new FileWriter(recordingFile);
                    bw = new BufferedWriter(fw);
                    bw.write("time,acc_x,acc_y,acc_z,gravity_x,gravity_y,gravity_z\n");

                    // Indicate we are recording
                    recording = true;

                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Stop recording
                recording = false;
                recordingView.setVisibility(View.INVISIBLE);
                startButton.setClickable(!recording);
                stopButton.setClickable(recording);

                try {
                    // Close file
                    bw.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });


        // Create a new event listener
        eventListener = new AccelerometerEventListener();

        // Create sensors to listen for Accelerometer and Gravity updates
        mSensorManager = (SensorManager) this
                .getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // Register sensors with listener
        mSensorManager.registerListener(eventListener, mAccelerometer,
                sensorSampleRate);
        mSensorManager.registerListener(eventListener, mGravity,
                sensorSampleRate);

        // Setup directory for files
        sdCard = Environment.getExternalStorageDirectory();
        dir = new File(sdCard.getAbsolutePath() + LOG_DIRECTORY);
        //dir = new File(LOG_DIRECTORY);
        dir.mkdirs();

    }

    private class AccelerometerEventListener implements SensorEventListener {

        // gravity1 holds calculated gravity values
        private float[] gravity1 = { 0, 0, 0 };
        // gravity2 holds gravity values from sensor, Note: Does not work on all
        // phones
        private float[] gravity2 = { 0, 0, 0 };

        private static final double alpha = 0.8;
        // Keep track of the last 10 accelerometer values
        private long[] lastTimes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        private int count = 0;
        String result = "";

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                // Calculate gravity
                gravity1[0] = (float) (alpha * gravity1[0] + (1 - alpha)
                        * Float.valueOf(event.values[0]));
                gravity1[1] = (float) (alpha * gravity1[1] + (1 - alpha)
                        * Float.valueOf(event.values[1]));
                gravity1[2] = (float) (alpha * gravity1[2] + (1 - alpha)
                        * Float.valueOf(event.values[2]));

                // Update display of values
                xValueTextView.setText(String.valueOf(Float
                        .valueOf(event.values[0]) - gravity1[0]));
                yValueTextView.setText(String.valueOf(Float
                        .valueOf(event.values[1]) - gravity1[1]));
                zValueTextView.setText(String.valueOf(Float
                        .valueOf(event.values[2]) - gravity1[2]));

                // Simple shift of the array. A Queue would be much better here.
                lastTimes[0] = lastTimes[1];
                lastTimes[1] = lastTimes[2];
                lastTimes[2] = lastTimes[3];
                lastTimes[3] = lastTimes[4];
                lastTimes[4] = lastTimes[5];
                lastTimes[5] = lastTimes[6];
                lastTimes[6] = lastTimes[7];
                lastTimes[7] = lastTimes[8];
                lastTimes[8] = lastTimes[9];
                lastTimes[9] = System.currentTimeMillis();

                // Calculate effective sampling rate
                double rate = 10.0 / ((lastTimes[9] - lastTimes[0]) / 1000.0);

                // Only update rate every 50 records
                if (++count % 10 == 0) {
                    count = 0;
                    rateValueTextView.setText(String.valueOf(rate));
                }

                // Save to file
                if (recording) {
                    result = "";
                    // Current Timestamp
                    result += String.valueOf(lastTimes[9]) + ",";
                    // X, Y, Z
                    result += String.valueOf(Float.valueOf(event.values[0]))
                            + ","
                            + String.valueOf(Float.valueOf(event.values[1]))
                            + ","
                            + String.valueOf(Float.valueOf(event.values[2]))
                            + ",";
                    // Gravity
                    result += String.valueOf(gravity2[0]) + ","
                            + String.valueOf(gravity2[1]) + ","
                            + String.valueOf(gravity2[2]);
                    // New line
                    result += "\n";
                    try {
                        bw.write(result);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                // Handle gravity sensor updates
                gravity2[0] = Float.valueOf(event.values[0]);
                gravity2[1] = Float.valueOf(event.values[1]);
                gravity2[2] = Float.valueOf(event.values[2]);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    //@Override
    //public boolean onCreateOptionsMenu(Menu menu) {
    //    // Inflate the menu; this adds items to the action bar if it is present.
    //    getMenuInflater().inflate(R.menu.main, menu);
    //    return true;
    //}

}