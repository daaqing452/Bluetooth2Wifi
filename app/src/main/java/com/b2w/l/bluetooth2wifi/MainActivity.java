package com.b2w.l.bluetooth2wifi;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ui
    TextView text_0, text_ip, text_connect_info;
    Activity activity_uithread;

    // ble
    // https://github.com/Jasonchenlijian/FastBle
    BleManager manager;
    BleScanRuleConfig scanRuleConfig;

    // wifi
    static int PORT = 11121;
    Socket socket = null;
    BufferedReader reader;
    PrintWriter writer;
    boolean listening;
    Thread listenThread;

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
                manager.initScanRule(scanRuleConfig);
                manager.scan(new BleScanCallback() {
                    @Override
                    public void onScanStarted(boolean success) {
                        // 开始扫描（主线程）
                    }

                    @Override
                    public void onScanning(BleDevice bleDevice) {
                        Log.d("b2w", "scaned device: " + bleDevice.getName());
                    }

                    @Override
                    public void onScanFinished(List<BleDevice> scanResultList) {
                        String s = "";
                        for (BleDevice bleDevice : scanResultList) {
                            s += bleDevice.getName() + ",";
                        }
                        text_0.setText(s);
                    }
                });
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
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (socket != null) {
                                writer.write("hello");
                                writer.flush();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    Log.d("b2wdebug", "button send error: " + e.toString());
                }
            }
        });

        // ble
        BleManager.getInstance().init(getApplication());
        manager = BleManager.getInstance();
        Log.d("b2wdebug", "is support ble? " + manager.isSupportBle());
        manager.enableBluetooth();
        scanRuleConfig = new BleScanRuleConfig.Builder()
                /*.setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, names)         // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒；小于等于0表示不限制扫描时间*/
                .build();
    }

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

    void receive(String s) {
        Log.d("b2wdebug", "receive: " + s);
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
                                receive(s);
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
