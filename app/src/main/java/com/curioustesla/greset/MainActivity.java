package com.curioustesla.greset;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class MainActivity extends AppCompatActivity {

    private TextView tvDeviceModel;
    private TextView tvAndroidVersion;
    private TextView tvNetworkStatus;

    private ImageButton btnRefreshNetwork;

    private MaterialButton btnTips;


    private MaterialButton btnNetworkSettings;
    private MaterialButton btnAllApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadDeviceInfo();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkStatus();

    }

    private void initViews() {
        tvDeviceModel = findViewById(R.id.tvDeviceModel);
        tvAndroidVersion = findViewById(R.id.tvAndroidVersion);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);

        btnRefreshNetwork = findViewById(R.id.btnRefreshNetwork);

        btnTips = findViewById(R.id.btnTips);


        btnNetworkSettings = findViewById(R.id.btnNetworkSettings);
        btnAllApps = findViewById(R.id.btnAllApps);
    }

    private void loadDeviceInfo() {
        String model = Build.MANUFACTURER + " " + Build.MODEL;
        tvDeviceModel.setText(model);

        String version = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        tvAndroidVersion.setText(version);


        checkNetworkStatus();
    }

    @SuppressWarnings("deprecation")
    private void checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null) {
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            tvNetworkStatus.setText(getString(R.string.net_mobile));
                            tvNetworkStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.success));
                            return;
                        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            tvNetworkStatus.setText(getString(R.string.net_wifi));
                            tvNetworkStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.warning));
                            return;
                        }
                    }
                }
            } else {
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) {
                    if (ni.getType() == ConnectivityManager.TYPE_MOBILE) {
                        tvNetworkStatus.setText(getString(R.string.net_mobile));
                        tvNetworkStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.success));
                        return;
                    } else if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                        tvNetworkStatus.setText(getString(R.string.net_wifi));
                        tvNetworkStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.warning));
                        return;
                    }
                }
            }
        }
        tvNetworkStatus.setText(getString(R.string.net_none));
        tvNetworkStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void setupClickListeners() {
        btnTips.setOnClickListener(v -> showTipsDialog());

        btnRefreshNetwork.setOnClickListener(v -> {
            checkNetworkStatus();
            Toast.makeText(this, "تم تحديث حالة الشبكة", Toast.LENGTH_SHORT).show();
        });

        // General system shortcuts
        btnNetworkSettings.setOnClickListener(v -> openSettingsAction(Settings.ACTION_DATA_ROAMING_SETTINGS));
        btnAllApps.setOnClickListener(v -> openSettingsAction(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
    }

    private void openSettingsAction(String action) {
        try {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ex) {
                Toast.makeText(this, "تعذر فتح الإعدادات", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTipsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tips_title)
                .setMessage(R.string.tips_content)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

}
