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

    private final static String STRUUID = "00112233445566778899AABBCCDDEEFF";

    private final static int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private List<ScanFilter> mScanFilters;
    private ScanSettings mScanSettings;
    private ScanCallback mScanCallback;

    private boolean mBeaconFound = false;
    private boolean mScanActive = false;
    private byte[] mReadData;
    private byte[] mIBeaconPrefix;
    private byte[] mUuid;
    private byte[] mMajorNumber;
    private byte[] mMinorNumber;
    private byte mTxPower;
    private int mRssi = 0;

    private TextView mBeaconFoundView;
    private TextView mScanActiveView;
    private TextView mRawDataView;
    private TextView mIBeaconPrefixView;
    private TextView mUuidView;
    private TextView mMajorNumberView;
    private TextView mMinorNumberView;
    private TextView mTxPowerView;
    private TextView mRssiView;
    private Button mButton;

    public static byte[] getUuid() {
        byte[] uuid = new byte[16];
        for (int i = 0; i < 16; ++i) {
            int val = Character.digit(STRUUID.charAt(2 * i), 16) << 4;
            val += Character.digit(STRUUID.charAt(2 * i + 1), 16);
            uuid[i] = (byte) val;
        }
        return uuid;
    }

    public static String byteArrayToString(byte[] array) {
        StringBuilder sb = new StringBuilder(2 * array.length);

        for (int i = 0; i < array.length; ++i) {
            sb.append(String.format("%02X", array[i] & 0xFF));
        }

        return sb.toString();
    }

    public static byte[] copySubArray(byte[] array, int start, int end) {
        byte[] ba = new byte[end - start];

        for (int i = start; i < end; ++i)
            ba[i - start] = array[i];

        return ba;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_beacon_status);

        mBeaconFoundView = (TextView) findViewById(R.id.statusTextView);
        mScanActiveView = (TextView) findViewById(R.id.scanningTextView);
        mRawDataView = (TextView) findViewById(R.id.rawDataTextView);
        mIBeaconPrefixView = (TextView) findViewById(R.id.iBeaconPrefixTextView);
        mUuidView = (TextView) findViewById(R.id.uuiTextView);
        mMajorNumberView = (TextView) findViewById(R.id.majorNumberTextView);
        mMinorNumberView = (TextView) findViewById(R.id.minorNumberTextView);
        mTxPowerView = (TextView) findViewById(R.id.txPowerNumberTextView);
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
            Toast.makeText(this, "Bluetooth not available!", Toast.LENGTH_SHORT).show();
            finish();
        }


        mScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(0)
                .build();

        byte[] uuid = getUuid();
        byte[] scanDataFilter = new byte[18];
        byte[] scanDataFilterMask = new byte[18];

        scanDataFilter[0] = scanDataFilter[1] = (byte) 0x00;
        scanDataFilterMask[0] = scanDataFilterMask[1] = (byte) 0x00;

        for (int i = 0; i < 16; ++i) {
            scanDataFilter[i + 2] = uuid[i];
            scanDataFilterMask[i + 2] = (byte) 0x01;
        }

        ScanFilter scanFilter =
                new ScanFilter.Builder()
                        .setManufacturerData(76, scanDataFilter, scanDataFilterMask).build();

        mScanFilters = new ArrayList<>();
        mScanFilters.add(scanFilter);


        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                mScanner.stopScan(mScanCallback);
                mScanActive = false;
                mBeaconFound = true;
                ScanRecord record = result.getScanRecord();
                if (record != null) {
                    mReadData = record.getManufacturerSpecificData(76);
                    mIBeaconPrefix = copySubArray(mReadData, 0, 2);
                    mUuid = copySubArray(mReadData, 2, 18);
                    mMajorNumber = copySubArray(mReadData, 18, 20);
                    mMinorNumber = copySubArray(mReadData, 20, 22);
                    mTxPower = mReadData[22];
                    mRssi = result.getRssi();
                }

                _updateUI();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBeaconFound) {
            mScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
            mScanActive = true;
            _updateUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mScanActive) {
            mScanner.stopScan(mScanCallback);
            mScanActive = false;
            _updateUI();
        }
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

            String rawDataStr = byteArrayToString(mReadData);
            mRawDataView.setText(
                    rawDataStr.substring(0, 16) + "\n"
                            + rawDataStr.substring(16, 32) + "\n"
                            + rawDataStr.substring(32));
            mIBeaconPrefixView.setText(byteArrayToString(mIBeaconPrefix));
            String uuidStr = byteArrayToString(mUuid);
            mUuidView.setText(
                    uuidStr.substring(0, 16) + "\n"
                            + uuidStr.substring(16));
            mMajorNumberView.setText(byteArrayToString(mMajorNumber));
            mMinorNumberView.setText(byteArrayToString(mMinorNumber));
            mTxPowerView.setText(Byte.toString(mTxPower) + " (" + String.format("%02X", mTxPower & 0xFF) + ")");
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
