package com.b2w.l.bluetooth2wifi;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.b2w.l.bluetooth2wifi.advertiser;
import android.bluetooth.BluetoothAdapter;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


public class MainActivity extends AppCompatActivity {

    // ui
    TextView text_0, text_ip, text_connect_info;
    Activity activity_uithread;

    final String BLUETOOTH_LIB = "scanner";

    // fastble
    // https://github.com/Jasonchenlijian/FastBle
    BleManager manager;

    // scanner
    // https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library
    BluetoothLeScannerCompat scanner;
    ScanSettings settings;
    List<ScanFilter> filters;

    // wifi
    static int PORT = 11121;
    Socket socket = null;
    BufferedReader reader;
    PrintWriter writer;
    boolean listening;
    String tmp_s;
    byte[] manudata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ui
        text_0 = findViewById(R.id.text_0);
        text_ip = findViewById(R.id.text_ip);
        text_connect_info = findViewById(R.id.text_connect_info);
        activity_uithread = (Activity)text_connect_info.getContext();

        Button button_scan = findViewById(R.id.button_scan);
        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BLUETOOTH_LIB == "fastble") {
                    manager.scan(new BleScanCallback() {
                        @Override
                        public void onScanStarted(boolean success) {
                            // 开始扫描（主线程）
                        }

                        @Override
                        public void onScanning(BleDevice bleDevice) {
                            Log.d("b2wdebug", "scaned device: " + bleDevice.getName() + " " + bleDevice.getRssi());
                        }

                        @Override
                        public void onScanFinished(List<BleDevice> scanResultList) {
                            String s = "";
                            try {
                                for (final BleDevice bleDevice : scanResultList) {
                                    Log.d("b2wdebug", "rssi:" + bleDevice.getName() + " " + bleDevice.getRssi());
                                    s += bleDevice.getName() + " " + bleDevice.getRssi() + "\n";
                                }
                                text_0.setText(s);
                            } catch (Exception e) {
                                Log.d("b2wdebug", e.toString());
                            }
                        }
                    });
                } else if (BLUETOOTH_LIB == "scanner") {
                    scanner.startScan(filters, settings, new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            super.onScanResult(callbackType, result);
                        }

                        @Override
                        public void onBatchScanResults(List<ScanResult> results) {
                            String s = "";
                            for (ScanResult result : results) {
                                Log.d("b2wdebug", result.getDevice().getName() + " " + result.getRssi());
//                                s += result.getDevice().getName() + " " + result.getRssi() + "\n";
//                                parse the report data
                                if (result.getScanRecord().getManufacturerSpecificData(0xffff) != null){
                                    manudata = result.getScanRecord().getManufacturerSpecificData(0xffff);
                                    Log.d("b2wdebug", result.getDevice().getName() + " "+ manudata[0]+ manudata[1]+ manudata[2]+ manudata[3]+ manudata[4]);
                                    String manustr = bytesToHex(manudata);
                                    s += manustr + "\n";
                                    send(manustr);
                                }


                            }
                            text_0.setText(s);
                        }

                        @Override
                        public void onScanFailed(int errorCode) {
                            super.onScanFailed(errorCode);
                        }
                    });
                }
            }
        });

        Button button_connect = findViewById(R.id.button_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket == null) {
                    String ip = text_ip.getText().toString();
                    new NetworkAsyncTask().execute(ip);
                } else {
                    try {
                        listening = false;
                        disconnect();
                    } catch (Exception e) {
                        Log.d("b2wdebug", "button disconnect error: " + e.toString());
                    }
                }
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    send("hello");
                } catch (Exception e) {
                    Log.d("b2wdebug", "button send error: " + e.toString());
                }
            }
        });

        // bluetooth scan
        UUID[] uuid = new UUID[]{UUID.fromString("00001819-0000-1000-8000-00805F9B34FB")};
        if (BLUETOOTH_LIB == "fastble") {
            BleManager.getInstance().init(getApplication());
            manager = BleManager.getInstance();
            Log.d("b2wdebug", "is support ble? " + manager.isSupportBle());
            manager.enableBluetooth();
            BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                    .setServiceUuids(uuid)      // 只扫描指定的服务的设备，可选
                    /*.setDeviceName(true, names)         // 只扫描指定广播名的设备，可选
                    .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                    .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false*/
                    .setScanTimeOut(1000)              // 扫描超时时间，可选，默认10秒；小于等于0表示不限制扫描时间
                    .build();
            manager.initScanRule(scanRuleConfig);
        } else if (BLUETOOTH_LIB == "scanner") {
            scanner = BluetoothLeScannerCompat.getScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(50)
                    .setUseHardwareBatchingIfSupported(false).build();
            filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid[0])).build());
        }

        // bluetooth advertisement
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        // Advertising start
        new advertiser(getApplicationContext(), btAdapter);


    }

//    void parse_manudata(byte[] manudata){
//        for (int i=0; i<manudata.length;i++){
//
//        }
//    }

    void disconnect() {
        try {
            //if (reader != null) reader.close();
            //if (writer != null) writer.close();
            socket.close();
            socket = null;
            text_connect_info.setText("disconnected");
        } catch (Exception e) {
            text_connect_info.setText(e.toString());
        }
    }

    void send(String s) {
        tmp_s = s;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    writer.write(tmp_s);
                    writer.flush();
                }
            }
        }).start();
    }

    void recv(String s) {
        Log.d("b2wdebug", "receive: " + s);
        tmp_s = s;
        activity_uithread.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text_0.setText(tmp_s);
            }
        });
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }


    class NetworkAsyncTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... params) {
            try {
                socket = new Socket(params[0], PORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                Thread.sleep(300);
                writer.print("客户端发送!");
                writer.flush();
                listening = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("b2wdebug", "listening");
                        while (listening) {
                            try {
                                String s = reader.readLine();
                                if (s == null) listening = false;
                                recv(s);
                            } catch (Exception e) {
                                Log.d("b2wdebug", "listen thread error: " + e.toString());
                                listening = false;
                                break;
                            }
                        }
                        activity_uithread.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                disconnect();
                            }
                        });
                    }
                }).start();
                return socket.toString();
            } catch (Exception e) {
                socket = null;
                return e.toString();
            }
        }

        protected void onPostExecute(String string) {
            Log.d("b2wdebug", "connect info: " + string);
            text_connect_info.setText(string);
        }
    }
}
