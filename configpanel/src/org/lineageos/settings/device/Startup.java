/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.device;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.lineageos.internal.util.FileUtils;
import org.lineageos.settings.device.utils.Constants;

public class Startup extends BroadcastReceiver {

    private static final String TAG = Startup.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (lineageos.content.Intent.ACTION_INITIALIZE_LINEAGE_HARDWARE.equals(action)) {
            // Disable button settings if needed
            if (!hasButtonProcs()) {
                disableComponent(context, ButtonSettingsActivity.class.getName());
            } else {
                enableComponent(context, ButtonSettingsActivity.class.getName());
                ButtonSettingsFragment.restoreSliderStates(context);
            }

            // Disable O-Click settings if needed
            if (!hasOClick()) {
                disableComponent(context, BluetoothInputSettings.class.getName());
                disableComponent(context, OclickService.class.getName());
            } else {
                updateOClickServiceState(context);
            }
        } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            if (hasOClick()) {
                updateOClickServiceState(context);
            }
        }
    }

    static boolean hasButtonProcs() {
        return FileUtils.fileExists(Constants.NOTIF_SLIDER_NODE);
    }

    static boolean hasOClick() {
        return Build.MODEL.equals("N1") || Build.MODEL.equals("N3");
    }

    private void disableComponent(Context context, String component) {
        ComponentName name = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void enableComponent(Context context, String component) {
        ComponentName name = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        if (pm.getComponentEnabledSetting(name)
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            pm.setComponentEnabledSetting(name,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    private void updateOClickServiceState(Context context) {
        BluetoothManager btManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = btManager.getAdapter();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean shouldStartService = adapter != null
                && adapter.getState() == BluetoothAdapter.STATE_ON
                && prefs.contains(Constants.OCLICK_DEVICE_ADDRESS_KEY);
        Intent serviceIntent = new Intent(context, OclickService.class);

        if (shouldStartService) {
            context.startService(serviceIntent);
        } else {
            context.stopService(serviceIntent);
        }
    }
}
