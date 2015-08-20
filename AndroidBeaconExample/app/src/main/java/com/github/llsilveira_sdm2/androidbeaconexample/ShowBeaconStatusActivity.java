package com.github.llsilveira_sdm2.androidbeaconexample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ShowBeaconStatusActivity extends AppCompatActivity {

    private final static ParcelUuid SERVICE_UUID = ParcelUuid.fromString("E20A39F4-73F5-4BC4-A12F-17D1AD07A961");
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private List<ScanFilter> mScanFilters;
    private ScanSettings mScanSettings;
    private ScanCallback mScanCallback;
    private boolean mBeaconFound = false;
    private boolean mScanActive = false;
    private int mRssi = 0;
    private byte[] mUuid = null;
    private TextView mBeaconFoundView;
    private TextView mScanActiveView;
    private TextView mUuidView;
    private TextView mRssiView;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_beacon_status);

        mBeaconFoundView = (TextView) findViewById(R.id.statusTextView);
        mScanActiveView = (TextView) findViewById(R.id.scanningTextView);
        mUuidView = (TextView) findViewById(R.id.uuidTextView);
        mRssiView = (TextView) findViewById(R.id.rssiTextView);
        mButton = (Button) findViewById(R.id.btn_moodle);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://moodle.ifspsaocarlos.edu.br/course/view.php?id=384";
                Intent openBrowser = new Intent(Intent.ACTION_VIEW);
                openBrowser.setData(Uri.parse(url));
                startActivity(openBrowser);
            }
        });

        _updateUI();

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bm.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Can't enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        mScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();


        mScanFilters = new ArrayList<>();
        mScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build()
        );


        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                mScanner.stopScan(mScanCallback);
                mScanActive = false;
                mBeaconFound = true;
                ScanRecord record = result.getScanRecord();
                if (record != null)
                    mUuid = record.getBytes();
                mRssi = result.getRssi();

                _updateUI();
            }
        };

        mScanner.startScan(mScanFilters, mScanSettings, mScanCallback);

        mScanActive = true;
        _updateUI();
    }


    private void _updateUI() {
        if (mBeaconFound)
            mBeaconFoundView.setText(R.string.txt_status_found);
        else
            mBeaconFoundView.setText(R.string.txt_status_notfound);

        if (mScanActive)
            mScanActiveView.setText(R.string.txt_scanning_active);
        else
            mScanActiveView.setText(R.string.txt_scanning_inactive);

        if (mBeaconFound) {
            StringBuilder sb = new StringBuilder(mUuid.length * 2);
            for (byte b : mUuid)
                sb.append(String.format("%02x", b & 0xFF));
            mUuidView.setText(sb.toString());
            mRssiView.setText(Integer.toString(mRssi));

            mButton.setVisibility(View.VISIBLE);
            mButton.setEnabled(true);
        } else {
            mButton.setVisibility(View.INVISIBLE);
            mButton.setEnabled(false);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_beacon_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
