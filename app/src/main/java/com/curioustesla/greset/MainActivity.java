package com.curioustesla.greset;

import android.app.DownloadManager;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PROVISION_MANAGED_PROFILE = 1001;
    private static final String ISLAND_PACKAGE = "com.oasisfeng.island";

    private TextView tvDeviceModel;
    private TextView tvAndroidVersion;
    private TextView tvAndroidId;
    private TextView tvGsfId;
    private TextView tvNetworkStatus;
    private TextView tvWorkProfileBadge;

    private TextView tvIslandStatus;
    private MaterialButton btnLaunchOrInstallIsland;
    private MaterialButton btnReinstallIsland;
    private MaterialButton btnExportIsland;
    private MaterialButton btnIslandSettings;
    private boolean pendingInstallIslandAfterPermission = false;

    private ImageButton btnCopyAndroidId;
    private ImageButton btnCopyGsfId;
    private ImageButton btnRefreshNetwork;

    private MaterialButton btnTips;
    private MaterialButton btnSetupWorkProfile;
    private MaterialButton btnManageWorkProfile;
    private MaterialButton btnShelterIsland;

    private MaterialButton btnOpenGsf;
    private MaterialButton btnOpenGms;
    private MaterialButton btnOpenPlayStore;
    private MaterialButton btnOpenAdsSettings;
    private MaterialButton btnRebootAdvice;
    private MaterialButton btnCreateAccount;

    private MaterialButton btnMultipleUsers;
    private MaterialButton btnNetworkSettings;
    private MaterialButton btnAllApps;
    private MaterialButton btnAccountsSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadDeviceInfo();
        checkWorkProfileStatus();
        checkIslandStatus();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkStatus();
        checkWorkProfileStatus();
        checkIslandStatus();

        if (pendingInstallIslandAfterPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    pendingInstallIslandAfterPermission = false;
                    installEmbeddedIslandApk();
                }
            } else {
                pendingInstallIslandAfterPermission = false;
                installEmbeddedIslandApk();
            }
        }
    }

    private void initViews() {
        tvDeviceModel = findViewById(R.id.tvDeviceModel);
        tvAndroidVersion = findViewById(R.id.tvAndroidVersion);
        tvAndroidId = findViewById(R.id.tvAndroidId);
        tvGsfId = findViewById(R.id.tvGsfId);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvWorkProfileBadge = findViewById(R.id.tvWorkProfileBadge);

        tvIslandStatus = findViewById(R.id.tvIslandStatus);
        btnLaunchOrInstallIsland = findViewById(R.id.btnLaunchOrInstallIsland);
        btnReinstallIsland = findViewById(R.id.btnReinstallIsland);
        btnExportIsland = findViewById(R.id.btnExportIsland);
        btnIslandSettings = findViewById(R.id.btnIslandSettings);

        btnCopyAndroidId = findViewById(R.id.btnCopyAndroidId);
        btnCopyGsfId = findViewById(R.id.btnCopyGsfId);
        btnRefreshNetwork = findViewById(R.id.btnRefreshNetwork);

        btnTips = findViewById(R.id.btnTips);
        btnSetupWorkProfile = findViewById(R.id.btnSetupWorkProfile);
        btnManageWorkProfile = findViewById(R.id.btnManageWorkProfile);
        btnShelterIsland = findViewById(R.id.btnShelterIsland);

        btnOpenGsf = findViewById(R.id.btnOpenGsf);
        btnOpenGms = findViewById(R.id.btnOpenGms);
        btnOpenPlayStore = findViewById(R.id.btnOpenPlayStore);
        btnOpenAdsSettings = findViewById(R.id.btnOpenAdsSettings);
        btnRebootAdvice = findViewById(R.id.btnRebootAdvice);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnMultipleUsers = findViewById(R.id.btnMultipleUsers);
        btnNetworkSettings = findViewById(R.id.btnNetworkSettings);
        btnAllApps = findViewById(R.id.btnAllApps);
        btnAccountsSync = findViewById(R.id.btnAccountsSync);
    }

    private void loadDeviceInfo() {
        String model = Build.MANUFACTURER + " " + Build.MODEL;
        tvDeviceModel.setText(model);

        String version = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        tvAndroidVersion.setText(version);

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tvAndroidId.setText(androidId != null ? androidId : "N/A");

        String gsfId = fetchGsfId();
        tvGsfId.setText(gsfId != null ? gsfId : "غير متاح (تلقائي بالنظام)");

        checkNetworkStatus();
    }

    private void checkWorkProfileStatus() {
        boolean isWork = isInsideWorkProfile();
        if (isWork) {
            tvWorkProfileBadge.setText(R.string.work_profile_status_work);
            tvWorkProfileBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.success));
            btnSetupWorkProfile.setEnabled(false);
            btnSetupWorkProfile.setText("أنت حالياً داخل بيئة العمل المعزولة");
        } else {
            tvWorkProfileBadge.setText(R.string.work_profile_status_personal);
            tvWorkProfileBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            btnSetupWorkProfile.setEnabled(true);
            btnSetupWorkProfile.setText(R.string.btn_setup_work_profile);
        }
    }

    private boolean isInsideWorkProfile() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
                if (um != null) {
                    return um.isManagedProfile();
                }
            }
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                return dpm.isProfileOwnerApp(getPackageName());
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isIslandInstalled() {
        try {
            getPackageManager().getPackageInfo(ISLAND_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void checkIslandStatus() {
        boolean installed = isIslandInstalled();
        if (installed) {
            String verName = "6.4.2";
            try {
                PackageInfo pInfo;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pInfo = getPackageManager().getPackageInfo(ISLAND_PACKAGE, PackageManager.PackageInfoFlags.of(0));
                } else {
                    pInfo = getPackageManager().getPackageInfo(ISLAND_PACKAGE, 0);
                }
                if (pInfo != null && pInfo.versionName != null) {
                    verName = pInfo.versionName;
                }
            } catch (Exception ignored) {
            }

            tvIslandStatus.setText(getString(R.string.island_status_installed_version, verName));
            tvIslandStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnLaunchOrInstallIsland.setText(R.string.btn_launch_island);
            btnLaunchOrInstallIsland.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7B1FA2")));
            btnReinstallIsland.setVisibility(View.VISIBLE);
            btnIslandSettings.setVisibility(View.VISIBLE);
        } else {
            tvIslandStatus.setText(R.string.island_status_not_installed);
            tvIslandStatus.setTextColor(ContextCompat.getColor(this, R.color.warning));
            btnLaunchOrInstallIsland.setText(R.string.btn_install_island);
            btnLaunchOrInstallIsland.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7B1FA2")));
            btnReinstallIsland.setVisibility(View.GONE);
            btnIslandSettings.setVisibility(View.GONE);
        }
    }

    private File getCachedIslandApkFile() {
        File outputDir = getExternalCacheDir();
        if (outputDir == null || !outputDir.canWrite()) {
            outputDir = new File(getCacheDir(), "apks");
        } else {
            outputDir = new File(outputDir, "apks");
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return new File(outputDir, "Island_6.4.2.apk");
    }

    private void installEmbeddedIslandApk() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.island_compat_warning_title)
                    .setMessage(getString(R.string.island_compat_warning_msg, Build.VERSION.RELEASE))
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                pendingInstallIslandAfterPermission = true;
                Toast.makeText(this, "يرجى تفعيل خيار (السماح بتثبيت التطبيقات من هذا المصدر) للمتابعة", Toast.LENGTH_LONG).show();
                try {
                    Intent permIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    permIntent.setData(Uri.parse("package:" + getPackageName()));
                    permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(permIntent);
                } catch (Exception e) {
                    try {
                        Intent fallback = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(fallback);
                    } catch (Exception ex) {
                        Intent fallback2 = new Intent(Settings.ACTION_SETTINGS);
                        fallback2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(fallback2);
                    }
                }
                return;
            }
        }

        File targetApk = getCachedIslandApkFile();
        if (targetApk.exists() && targetApk.length() > 3000000L) {
            launchApkInstaller(targetApk);
            return;
        }

        Toast.makeText(this, R.string.island_installing, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                try (InputStream is = getAssets().open("island.apk");
                     FileOutputStream fos = new FileOutputStream(targetApk)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                }

                targetApk.setReadable(true, false);
                runOnUiThread(() -> launchApkInstaller(targetApk));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        getString(R.string.island_install_error) + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void launchApkInstaller(File apkFile) {
        if (apkFile == null || !apkFile.exists() || apkFile.length() == 0) {
            Toast.makeText(this, "تعذر تجهيز ملف التثبيت، يرجى إعادة المحاولة", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (resInfoList != null) {
                    for (ResolveInfo resolveInfo : resInfoList) {
                        if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName != null) {
                            grantUriPermission(resolveInfo.activityInfo.packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                }

                String[] commonInstallers = {
                        "com.google.android.packageinstaller",
                        "com.android.packageinstaller",
                        "com.samsung.android.packageinstaller",
                        "com.miui.packageinstaller",
                        "com.coloros.packageinstaller",
                        "com.vivo.packageinstaller",
                        "com.huawei.appmarket"
                };
                for (String installerPkg : commonInstallers) {
                    try {
                        grantUriPermission(installerPkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                apkUri = Uri.fromFile(apkFile);
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            }

            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(installIntent);
        } catch (Exception e) {
            showManualInstallOffer(apkFile, e.getMessage());
        }
    }

    private void showManualInstallOffer(File apkFile, String errorMsg) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("تعذر بدء المثبت التلقائي")
                .setMessage("السبب: " + errorMsg + "\n\nهل ترغب في حفظ ملف Island 6.4.2.apk في مجلد التنزيلات (Downloads) لتثبيته يدوياً؟")
                .setPositiveButton("حفظ في Downloads", (dialog, which) -> exportIslandApkToDownloads())
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void exportIslandApkToDownloads() {
        Toast.makeText(this, "جارٍ نسخ ملف Island إلى مجلد التنزيلات...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                boolean success = false;
                String displayPath = "";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Island_6.4.2.apk");
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (InputStream is = getAssets().open("island.apk");
                             OutputStream os = getContentResolver().openOutputStream(uri)) {
                            byte[] buffer = new byte[65536];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            os.flush();
                            success = true;
                            displayPath = "مجلد Downloads (التنزيلات)";
                        }
                    }
                } else {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File targetFile = new File(downloadsDir, "Island_6.4.2.apk");
                    try (InputStream is = getAssets().open("island.apk");
                         FileOutputStream fos = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[65536];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.flush();
                        success = true;
                        displayPath = targetFile.getAbsolutePath();
                    }
                }

                final boolean finalSuccess = success;
                final String finalPath = displayPath;
                runOnUiThread(() -> {
                    if (finalSuccess) {
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(R.string.island_saved_to_downloads_title)
                                .setMessage(getString(R.string.island_saved_to_downloads_msg, finalPath))
                                .setPositiveButton(R.string.btn_open_downloads, (dialog, which) -> {
                                    try {
                                        Intent dIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                                        dIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(dIntent);
                                    } catch (Exception e) {
                                        Toast.makeText(MainActivity.this, "افتح تطبيق 'ملفاتي' أو 'مدير الملفات' بجهازك", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(R.string.ok, null)
                                .show();
                    } else {
                        Toast.makeText(MainActivity.this, "فشل حفظ الملف في مجلد التنزيلات", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void launchOrInstallIsland() {
        if (isIslandInstalled()) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ISLAND_PACKAGE);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            } else {
                installEmbeddedIslandApk();
            }
        } else {
            installEmbeddedIslandApk();
        }
    }

    private String fetchGsfId() {
        try {
            Uri uri = Uri.parse("content://com.google.android.gsf.gservices");
            String[] params = new String[]{"android_id"};
            Cursor cursor = getContentResolver().query(uri, null, null, params, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst() && cursor.getColumnCount() >= 2) {
                        String idStr = cursor.getString(1);
                        long val = Long.parseLong(idStr);
                        return Long.toHexString(val).toUpperCase(Locale.US);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Throwable ignored) {
            // Ignored if provider restricted in newer Android versions
        }
        return null;
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

        // Island 6.4.2 actions
        btnLaunchOrInstallIsland.setOnClickListener(v -> launchOrInstallIsland());
        btnReinstallIsland.setOnClickListener(v -> installEmbeddedIslandApk());
        btnExportIsland.setOnClickListener(v -> exportIslandApkToDownloads());
        btnIslandSettings.setOnClickListener(v -> openAppDetails(ISLAND_PACKAGE));

        // Work Profile actions
        btnSetupWorkProfile.setOnClickListener(v -> provisionWorkProfile());
        btnManageWorkProfile.setOnClickListener(v -> openWorkProfileSettings());
        btnShelterIsland.setOnClickListener(v -> showShelterDialog());

        // Step 1: Open Google Services Framework
        btnOpenGsf.setOnClickListener(v -> openAppDetails("com.google.android.gsf"));

        // Step 2: Open Google Play Services
        btnOpenGms.setOnClickListener(v -> openAppDetails("com.google.android.gms"));

        // Step 3: Open Google Play Store
        btnOpenPlayStore.setOnClickListener(v -> openAppDetails("com.android.vending"));

        // Step 4: Open Ads Settings
        btnOpenAdsSettings.setOnClickListener(v -> openAdsSettings());

        // Step 5: Reboot Advice
        btnRebootAdvice.setOnClickListener(v -> showRebootDialog());

        // Step 6: Create Account
        btnCreateAccount.setOnClickListener(v -> openAddAccount());

        // Copy buttons
        btnCopyAndroidId.setOnClickListener(v -> copyToClipboard("Android ID", tvAndroidId.getText().toString()));
        btnCopyGsfId.setOnClickListener(v -> copyToClipboard("GSF ID", tvGsfId.getText().toString()));
        btnRefreshNetwork.setOnClickListener(v -> {
            checkNetworkStatus();
            Toast.makeText(this, "تم تحديث حالة الشبكة", Toast.LENGTH_SHORT).show();
        });

        // Quick Tools
        btnMultipleUsers.setOnClickListener(v -> openSettingsAction("android.settings.USER_SETTINGS"));
        btnNetworkSettings.setOnClickListener(v -> openSettingsAction(Settings.ACTION_DATA_ROAMING_SETTINGS));
        btnAllApps.setOnClickListener(v -> openSettingsAction(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        btnAccountsSync.setOnClickListener(v -> openSettingsAction(Settings.ACTION_SYNC_SETTINGS));
    }

    private void provisionWorkProfile() {
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            ComponentName adminComponent = GResetDeviceAdminReceiver.getComponentName(this);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_PROVISION_MANAGED_PROFILE);
            } else {
                Toast.makeText(this, R.string.work_profile_setup_failed, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openWorkProfileSettings() {
        Intent[] intents = new Intent[]{
                new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES"),
                new Intent(Settings.ACTION_SYNC_SETTINGS),
                new Intent("android.settings.USER_SETTINGS"),
                new Intent(Settings.ACTION_SETTINGS)
        };

        boolean opened = false;
        for (Intent intent : intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                opened = true;
                break;
            } catch (Exception ignored) {
            }
        }

        if (!opened) {
            Toast.makeText(this, "توجه إلى: الإعدادات -> الحسابات -> ملف العمل لإدارته أو حذفه", Toast.LENGTH_LONG).show();
        }
    }

    private void showShelterDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.work_profile_dialog_title)
                .setMessage(R.string.work_profile_dialog_content)
                .setPositiveButton(R.string.btn_open_shelter, (dialog, which) -> launchOrInstallApp("com.katzoft.shelter"))
                .setNegativeButton(R.string.btn_open_island, (dialog, which) -> launchOrInstallIsland())
                .setNeutralButton(R.string.ok, null)
                .show();
    }

    private void launchOrInstallApp(String packageName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            try {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(marketIntent);
            } catch (Exception e) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
            }
        }
    }

    private void openAppDetails(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, getString(R.string.app_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openAdsSettings() {
        Intent[] intents = new Intent[]{
                new Intent("com.google.android.gms.settings.ADS_PRIVACY"),
                new Intent("com.google.android.gms.settings.GOOGLE_SERVICES"),
                new Intent(Settings.ACTION_PRIVACY_SETTINGS),
                new Intent(Settings.ACTION_SETTINGS)
        };

        boolean started = false;
        for (Intent intent : intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                started = true;
                break;
            } catch (Exception ignored) {
            }
        }

        if (!started) {
            Toast.makeText(this, "تعذر فتح إعدادات الإعلانات، توجه إلى: الإعدادات -> Google -> الإعلانات", Toast.LENGTH_LONG).show();
        }
    }

    private void openAddAccount() {
        try {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SYNC_SETTINGS);
                startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(this, "تعذر فتح شاشة إضافة الحسابات", Toast.LENGTH_SHORT).show();
            }
        }
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

    private void showRebootDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reboot_tip_title)
                .setMessage(R.string.reboot_tip_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.copied_to_clipboard) + ": " + text, Toast.LENGTH_SHORT).show();
        }
    }
}
