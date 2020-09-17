package com.example.robster;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity {

    private TextView bluetoothStatus;
    private BluetoothAdapter BlueAdapter;
    private ArrayAdapter<String> BTArrayAdapter;

    //Snackbar snackbar = Snackbar.make(findViewById(R.id.relativeLayout), R.string.bluetooth_snack,Snackbar.LENGTH_INDEFINITE);
    private Handler mandler;
    private ConnectedThread connectThread;
    private BluetoothSocket btSocket = null;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
               super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);
        Snackbar snackbar = Snackbar.make(findViewById(R.id.relativeLayout), R.string.bluetooth_snack,Snackbar.LENGTH_INDEFINITE);

        Button discoverBtn = (Button) findViewById(R.id.findNewDevice);
        final Button listPairedDevices = (Button) findViewById(R.id.PairedDevices);

        ListView deviceListView = (ListView) findViewById(R.id.deviceListView);
        SwitchCompat blueToothSwitch = (SwitchCompat) findViewById(R.id.BluetoothOn);

        deviceListView.setAdapter(BTArrayAdapter);
        deviceListView.setOnItemClickListener(DeviceClickListener);

        BlueAdapter = BluetoothAdapter.getDefaultAdapter();

        if(BlueAdapter == null){
            snackbar.setAction(R.string.bluesnack_action, new SnackbarAction());
            snackbar.show();
        }else if(!BlueAdapter.isEnabled()){
            blueToothSwitch.setChecked(false);
        }else{
            blueToothSwitch.setChecked(true);
        }

        blueToothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    BlueAdapter.enable();
                }else{
                    BlueAdapter.disable();
                }
            }
        });

                listPairedDevices.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displayPairedDevices(view);
                    }
                });

        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discover(view);
            }
        });

    }

    private void bluetoothOn(View view){
        if(!BlueAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
            bluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth On", Toast.LENGTH_SHORT).show();
        }
    }

    public class SnackbarAction implements View.OnClickListener{
        @Override
        public void onClick(View v){
            System.exit(1);
        }
    }

    private void turnBlueOff(View view){
        BlueAdapter.disable();
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        if(BlueAdapter.isDiscovering()){
            BlueAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery Cancelled", Toast.LENGTH_SHORT).show();
        }else{
            if(BlueAdapter.isEnabled()){
                BTArrayAdapter.clear();
                BlueAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Add to the list
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                BTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void displayPairedDevices(View newView){
        Set<BluetoothDevice> pairedDevices = BlueAdapter.getBondedDevices();
        if(BlueAdapter.isEnabled()){
            for(BluetoothDevice btdevice : pairedDevices){
                BTArrayAdapter.add(btdevice.getName() + "\n" + btdevice.getAddress());
            }

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();

        }else{
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }
    }

    private AdapterView.OnItemClickListener DeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            if(BlueAdapter.isEnabled()){
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            Bundle bundle = new Bundle();
            bundle.putString("Address",address);
            bundle.putString("Name", name);

            Intent newIntent = new Intent(HomeActivity.this, controllerActivity.class);

            newIntent.putExtras(bundle);

            startActivity(newIntent);
        }

    };

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