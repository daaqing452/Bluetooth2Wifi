package com.b2w.l.bluetooth2wifi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.UUID;
/**
 * Created by peter on 21.04.2017.
 * https://github.com/CodingPete/BluetoothLowEnergy.git

 */
public class advertiser {

    public advertiser(Context context, BluetoothAdapter btAdapter) {

        BluetoothLeAdvertiser btAdvertiser = btAdapter.getBluetoothLeAdvertiser();

        ParcelUuid puuid = new ParcelUuid(UUID.fromString("00001819-0000-1000-8000-00805F9B34FB"));
        //ParcelUuid puuid = new ParcelUuid(UUID.fromString("1819"));
        //ParcelUuid puuid = new ParcelUuid(UUID.fromString(context.getString(R.string.btmesh_uuid)));
        byte beacon_index = 3;
        btAdapter.setName("B3");
        byte[] rssi_data = {beacon_index,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(puuid)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(0xff,rssi_data)
                .build();

        Log.d("b2wdebug", "service uuid is " + data.getServiceUuids());
        Log.d("b2wdebug", "manu data is " + data.getManufacturerSpecificData().toString());
        Log.d("b2wdebug", "service data is " + data.getServiceData().toString());
        Log.d("b2wdebug", "all data is " + data.toString());

        btAdvertiser.startAdvertising(
                settings,
                data,
                callback
        );

    }
    private static AdvertiseCallback callback = new AdvertiseCallback() {

        private String TAG = "Advertiser";

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Success");
        }

        @Override
        public void onStartFailure(int errorCode) {
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "Failed : Already started");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.d(TAG, "Failed : Data too large");
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "Failed : Feature unsupported");
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "Failed : Internal Error");
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.d(TAG, "Failed : Too many advertisers");
                    break;
            }
        }
    };

}
