package com.curioustesla.greset;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class GResetDeviceAdminReceiver extends DeviceAdminReceiver {

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), GResetDeviceAdminReceiver.class);
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        super.onProfileProvisioningComplete(context, intent);

        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = getComponentName(context);

        if (dpm != null) {
            try {
                // Set the profile name and enable it
                dpm.setProfileName(adminComponent, "byhantools Work Profile");
                dpm.setProfileEnabled(adminComponent);

                // Enable the main activity in the work profile
                Intent launchIntent = new Intent(context, MainActivity.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            } catch (Exception e) {
                // Log or ignore
            }
        }

        Toast.makeText(context, "تم تجهيز بيئة العمل المعزولة بنجاح!", Toast.LENGTH_LONG).show();
    }
}
