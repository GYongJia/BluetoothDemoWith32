package com.example.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BlueToothTool {

    private BluetoothDevice device;
    private Handler mhandler;
    BluetoothSocket socket;
    private BluetoothAdapter adapter;
    static final int CONNECT_FAILED = -1;
    static final int CONNECT_SUCCESS = 1;
    static final int PIPEI_SUCCESS = 2;
    static final int PIPEI_FAILED = -2;
    static final int READ_FAILED = -11;
    static final int WRITE_FAILED = -22;
    static final int DATA = 99;
    private boolean isConnect = false;
    private static final String MY_UUID ="00001101-0000-1000-8000-00805F9B34FB";

    public BlueToothTool(BluetoothDevice device, Handler handler){
        this.device = device;
        this.mhandler = handler;
    }

    public String getAddress(){
        return device.getAddress();
    }

    /**
    * 开辟连接线程任务
    */

    public void connect(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothSocket tmp = null;
                adapter = BluetoothAdapter.getDefaultAdapter();
                try{
                    tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                }catch (Exception e){
                    setState(PIPEI_FAILED);
                    Log.d("TAG", e.toString());
                }
                setState(PIPEI_SUCCESS);
                socket = tmp;
                adapter.cancelDiscovery();
                try{
                    socket.connect();
                    isConnect = true;
                    setState(CONNECT_SUCCESS);
                    //Readtask readtask = new Readtask(); //连接之后开启立即开启读取线程
                    //readtask.start();
                }catch (Exception e){
                    setState(CONNECT_FAILED);
                    Log.d("TAG", e.toString());
                }
            }
        });
        new Thread(thread).start();
    }

    /**
     * 开辟线程读任务
     */

    public class Readtask extends Thread{
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream;
            while(true){
                try{
                    inputStream = socket.getInputStream();
                    if((bytes = inputStream.read(buffer)) > 0){
                        byte[] buf_data = new byte[bytes];
                        for(int i = 0;i < bytes;i++){
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        //TODO
                        //读取的数据进行处理
                        Message msg = mhandler.obtainMessage();
                        msg.what = DATA;
                        msg.obj = s;
                        mhandler.sendMessage(msg);
                    }
                }catch (IOException e){
                    setState(READ_FAILED);
                    Log.d("TAG", e.toString());
                    break;
                }
            }

            if(socket!= null){
                try{
                    socket.close();
                }catch (IOException e){
                    Log.d("TAG", e.toString());
                }
            }
        }
    }

    /**
     * 开辟线程写任务
     */
    public class Writetask extends Thread{  //将字符串参数转换为字节数组发送出去
        private String srt;
        public Writetask(String str){
            this.srt = str;
        }
        @Override
        public void run() {
            OutputStream outputStream = null;
            byte[] st = srt.getBytes();
            try{
                outputStream = socket.getOutputStream();
                outputStream.write(st);
            }catch(Exception e){
                setState(WRITE_FAILED);
                Log.d("TAG", e.toString());
            }
        }
    }

    private void setState(int mes){
        Message message = new Message();
        message.what = mes;
        mhandler.sendMessage(message);
    }

}
