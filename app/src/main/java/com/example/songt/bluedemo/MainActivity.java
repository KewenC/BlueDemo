package com.example.songt.bluedemo;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.util.ArrayList;
import java.util.Iterator;

import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;
import static java.security.CryptoPrimitive.MAC;

public class MainActivity extends AppCompatActivity {

    private BluetoothClient mClient;
    private ArrayList<String> nameList;
    private ArrayList<String> macList;
    private String mac;
    private ProgressBar progressBar;
    private BluetoothAdapter bluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);

        nameList = new ArrayList<>();
        macList = new ArrayList<>();

        mClient = new BluetoothClient(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                //开启蓝牙
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            } else {
                scan();
                Toast.makeText(this,"蓝牙已打开！",Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,"此设备不支持蓝牙！",Toast.LENGTH_SHORT).show();
            Log.e("TAGF", "此设备不支持蓝牙！");
        }


    }

    private void scan() {
        SearchRequest request = new SearchRequest.Builder()

                .searchBluetoothLeDevice(3000, 3)   // 先扫BLE设备3次，每次3s

                .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s,在实际工作中没用到经典蓝牙的扫描

                .searchBluetoothLeDevice(2000)      // 再扫BLE设备2s

                .build();
        mClient.search(request, new SearchResponse() {

            @Override

            public void onSearchStarted() {//开始搜素
                progressBar.setVisibility(View.VISIBLE);
                Log.e("TAGF","onSearchStarted");
            }

            @Override

            public void onDeviceFounded(SearchResult device) {//找到设备 可通过manufacture过滤
//                Log.e("TAGF","onDeviceFounded");

                Beacon beacon = new Beacon(device.scanRecord);
                nameList.add(device.getName());
                macList.add(device.getAddress());

//                BluetoothLog.v(String.format("beacon for %s\n%s", device.getAddress(), beacon.toString()));
                Log.e("TAGF","onDeviceFounded_"+device.getName()+"_"+String.format("beacon for %s\n%s", device.getAddress(), beacon.toString()));
            }

            @Override

            public void onSearchStopped() {//搜索停止
                progressBar.setVisibility(View.GONE);
                Log.e("TAGF","onSearchStopped");
                if (nameList.size() <= 0) {
                    Toast.makeText(MainActivity.this,"没发现可用设备！",Toast.LENGTH_LONG).show();
                    return;
                }
                String[] dev = new String[nameList.size()];
                for (int i=0;i<dev.length;i++){
                    dev[i] = nameList.get(i);
                }

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                mBuilder.setTitle("设备连接：");
                mBuilder.setItems(dev,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressBar.setVisibility(View.VISIBLE);
                                connect(macList.get(which));
                            }
                        });
                mBuilder.create().show();
            }

            @Override

            public void onSearchCanceled() {//搜索取消
                progressBar.setVisibility(View.GONE);
                Log.e("TAGF","onSearchCanceled");
            }

        });
    }

    private void connect(String mac) {
        this.mac = mac;
        Log.e("TAGF","连接设备MAC："+mac);
        BleConnectOptions options = new BleConnectOptions.Builder()

                .setConnectRetry(3)   // 连接如果失败重试3次

                .setConnectTimeout(30000)   // 连接超时30s

                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次

                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s

                .build();
        mClient.connect(mac, options, new BleConnectResponse() {

            @Override

            public void onResponse(int code, BleGattProfile data) {
                Log.e("TAGF","onResponse_code="+code+"_data="+data.toString());

            }

        });
        mClient.registerConnectStatusListener(mac, mBleConnectStatusListener);
    }

    private final BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {

        @Override

        public void onConnectStatusChanged(String mac, int status) {
//            Log.e("TAGF","onConnectStatusChanged");
            if (status == STATUS_CONNECTED) {
                Log.e("TAGF","STATUS_CONNECTED");
            } else if (status == STATUS_DISCONNECTED) {
                Log.e("TAGF","STATUS_DISCONNECTED");
            }

        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null && !mac.equals("")){
            mClient.unregisterConnectStatusListener(mac, mBleConnectStatusListener);
            mClient.disconnect(mac);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT){
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this,"蓝牙打开失败！",Toast.LENGTH_SHORT).show();
                Log.e("TAGF", "蓝牙打开失败！");
            } else {
                scan();
                Toast.makeText(this,"蓝牙打开成功！",Toast.LENGTH_SHORT).show();
                Log.e("TAGF", "蓝牙打开成功！");
            }
        }
    }
}
