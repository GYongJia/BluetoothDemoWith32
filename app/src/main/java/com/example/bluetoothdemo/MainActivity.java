package com.example.bluetoothdemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION = 1;
    private static final int REQUEST_DISCOVERABLE = 2;

    private ListView listView;
    private Button mButton;
    private TextView status;
    private TextView address;
    private List<String> listDevice;
    private ArrayAdapter<String> mAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothReceiver receiver;
    private BlueToothTool client;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPeimission();
        initView();

    }

    private void initPeimission() {
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION);
        }else{

        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case LOCATION_PERMISSION:
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"权限开启成功",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this,"权限开启失败",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    private void initView() {
        status = (TextView)findViewById(R.id.connectiontext);
        address = (TextView)findViewById(R.id.address);
        mButton = (Button)findViewById(R.id.button_connect);
        listView = (ListView)findViewById(R.id.listview);
        relativeLayout = (RelativeLayout)findViewById(R.id.layout);

        listDevice = new ArrayList<>();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnableBluetooth();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = listDevice.get(position);
                String macAdress = str.split("\\|")[1];
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAdress);
                client = new BlueToothTool(device,handler);
                try{
                    client.connect();
                }catch (Exception e){
                    Log.e("TAG", e.toString());
                }
            }
        });
    }

    private void EnableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        enable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivityForResult(enable,REQUEST_DISCOVERABLE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case REQUEST_DISCOVERABLE:
                if(resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText(this,"蓝牙未开启",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this,"蓝牙开启成功",Toast.LENGTH_SHORT).show();
                    mBluetoothAdapter.startDiscovery();
                }
                break;
            default:
                break;
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String str = device.getName() + "|" +device.getAddress();
                if(listDevice.indexOf(str) == -1){  //防止重复添加
                    listDevice.add(str);
                }
                if(mAdapter != null){
                    mAdapter.notifyDataSetChanged();
                }
                showDevices();
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

            }
        }

    }

    private void showDevices(){
        mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,
                listDevice);
        listView.setAdapter(mAdapter);
    }

    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case BlueToothTool.CONNECT_FAILED:
                    Toast.makeText(MainActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
                    break;
                case BlueToothTool.CONNECT_SUCCESS:
                    Toast.makeText(MainActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                    listView.setVisibility(View.GONE);  //连接成功后移除listview 设置其他布局可见
                    status.setText("已连接");
                    address.setText(client.getAddress());
                    relativeLayout.setVisibility(View.VISIBLE);
                    break;
                case BlueToothTool.READ_FAILED:
                    Toast.makeText(MainActivity.this,"读取失败",Toast.LENGTH_SHORT).show();
                    break;
                case BlueToothTool.WRITE_FAILED:
                    Toast.makeText(MainActivity.this,"发送失败",Toast.LENGTH_SHORT).show();
                    break;
                case BlueToothTool.PIPEI_SUCCESS:
                    Toast.makeText(MainActivity.this,"正在连接",Toast.LENGTH_SHORT).show();
                    break;
                case BlueToothTool.PIPEI_FAILED:
                    Toast.makeText(MainActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
                    break;
                case BlueToothTool.DATA:

                    break;
            }
        }
    };

    @Override
    protected void onResume() {  //注册广播接收器
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BluetoothReceiver();
        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);
    }

    @Override
    protected void onDestroy() {  //注销广播接收器,关闭蓝牙
        unregisterReceiver(receiver);
        mBluetoothAdapter.disable();
        super.onDestroy();
    }

}
