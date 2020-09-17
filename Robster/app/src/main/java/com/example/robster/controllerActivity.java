package com.example.robster;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class controllerActivity extends AppCompatActivity {

    private int Magnitude;

    private ConnectedThread connectedThread;
    private BluetoothSocket btSocket = null;
    private BluetoothAdapter BlueAdapter;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_activity);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        String buffer;

        Bundle bundle = getIntent().getExtras();
        //Reload info from HomeActivity

        final String address = bundle.getString("Address");
        final String name = bundle.getString("Name");

        TextView connectStatus = findViewById(R.id.ConnectionStatus);
        Button turnLeftBtn = findViewById(R.id.TurnLeftBtn);
        Button turnRightBtn = findViewById(R.id.TurnRightBtn);
        SwitchCompat disconnectSwitch = findViewById(R.id.DisconnectSwitch);
        SeekBar powerBar = findViewById(R.id.PowerBar);
        //Thread used for connection purposes
        new Thread(){
            public void run(){
                boolean fail = false;

                BluetoothDevice device = BlueAdapter.getRemoteDevice(address);

                try{
                    btSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
                }catch(IOException e){
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }

                try{
                    btSocket.connect();
                }catch(IOException e){
                    //Close socket if failed connection
                    try{
                        fail = true;
                        btSocket.close();
                    }catch(IOException e2){
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if(!fail){
                    connectedThread = new ConnectedThread(btSocket);
                    connectedThread.start();
                }


            }
        }.start();

        if(connectedThread != null){
            powerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    Magnitude = i;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            connectStatus.setText("Connected");
            connectStatus.setTextColor(Color.GREEN);

        }else{

        }


    }

    private class ConnectedThread extends Thread{
        private BluetoothSocket blueSocket;
        private InputStream Instream;
        private OutputStream OutStream;
    ConnectedThread(BluetoothSocket sock){
        blueSocket = sock;
        InputStream tmpin = null;
        OutputStream tmpout = null;

        //Trycatch for connection
        try{
            tmpin = sock.getInputStream();
            tmpout = sock.getOutputStream();
        }catch(IOException ignored){

        }

        Instream = tmpin;
        OutStream = tmpout;

    }

    public void write(String input){
        byte[] bytes = input.getBytes();
        try{
            OutStream.write(bytes);
        }catch (IOException ignored) {}
    }

    public void endConnection(){
        try{
            blueSocket.close();
        } catch(IOException ignored){

        }
    }

}
    private BluetoothSocket creatSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }
}