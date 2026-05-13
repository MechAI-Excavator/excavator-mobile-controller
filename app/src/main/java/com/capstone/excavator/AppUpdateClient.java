package com.capstone.excavator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 与后台 {@code GET /api/public/check-update} 交互：检查更新、下载 APK、触发安装。
 */
public final class AppUpdateClient {

    private static final String TAG = "AppUpdateClient";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private AppUpdateClient() {
    }

    /** 解析后的检查更新结果（仅当 {@link #httpOk} 为 true 时其它字段有意义）。 */
    public static final class CheckUpdateResult {
        public final boolean httpOk;
        public final int httpCode;
        @Nullable public final String httpError;

        public final boolean hasUpdate;
        public final int remoteVersionCode;
        @NonNull public final String versionName;
        public final boolean forceUpdate;
        public final int minSupportedVersion;
        @NonNull public final String apkUrl;
        @NonNull public final String sha256;
        public final long size;
        @NonNull public final List<String> releaseNotes;
        @Nullable public final String publishedAt;

        CheckUpdateResult(boolean httpOk, int httpCode, @Nullable String httpError,
                boolean hasUpdate, int remoteVersionCode, @NonNull String versionName,
                boolean forceUpdate, int minSupportedVersion, @NonNull String apkUrl,
                @NonNull String sha256, long size, @NonNull List<String> releaseNotes,
                @Nullable String publishedAt) {
            this.httpOk = httpOk;
            this.httpCode = httpCode;
            this.httpError = httpError;
            this.hasUpdate = hasUpdate;
            this.remoteVersionCode = remoteVersionCode;
            this.versionName = versionName;
            this.forceUpdate = forceUpdate;
            this.minSupportedVersion = minSupportedVersion;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.size = size;
            this.releaseNotes = releaseNotes;
            this.publishedAt = publishedAt;
        }

        static CheckUpdateResult httpFail(int code, String err) {
            return new CheckUpdateResult(false, code, err,
                    false, 0, "", false, 0, "", "", 0L,
                    new ArrayList<>(), null);
        }
    }

    /**
     * 同步请求检查更新（请在后台线程调用）。
     * 使用 {@link BuildConfig#VERSION_CODE} 作为当前版本号。
     */
    @NonNull
    public static CheckUpdateResult checkUpdateSync() throws IOException {
        String base = BuildConfig.UPDATE_API_BASE.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        HttpUrl parsed = HttpUrl.parse(base + "/api/public/check-update");
        if (parsed == null) {
            throw new IOException("无效的更新服务地址");
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("platform", "android")
                .addQueryParameter("channel", BuildConfig.UPDATE_CHANNEL)
                .addQueryParameter("versionCode", String.valueOf(BuildConfig.VERSION_CODE))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", BuildConfig.UPDATE_API_KEY)
                .get()
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            int code = response.code();
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            if (!response.isSuccessful()) {
                return CheckUpdateResult.httpFail(code, raw.isEmpty() ? ("HTTP " + code) : raw);
            }
            return parseCheckBody(code, raw);
        }
    }

    @NonNull
    private static CheckUpdateResult parseCheckBody(int httpCode, String raw) {
        try {
            JSONObject o = new JSONObject(raw);
            boolean hasUpdate = o.optBoolean("hasUpdate", false);
            int remoteVc = o.optInt("versionCode", 0);
            String versionName = o.optString("versionName", "");
            boolean force = o.optBoolean("forceUpdate", false);
            int minSup = o.optInt("minSupportedVersion", 0);
            String apkUrl = o.optString("apkUrl", "");
            String sha256 = o.optString("sha256", "");
            long size = o.optLong("size", 0L);
            String publishedAt = null;
            if (o.has("publishedAt") && !o.isNull("publishedAt")) {
                publishedAt = o.optString("publishedAt");
                if (publishedAt.isEmpty()) {
                    publishedAt = null;
                }
            }

            List<String> notes = new ArrayList<>();
            JSONArray arr = o.optJSONArray("releaseNotes");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String line = arr.optString(i, "").trim();
                    if (!line.isEmpty()) {
                        notes.add(line);
                    }
                }
            }

            return new CheckUpdateResult(true, httpCode, null,
                    hasUpdate, remoteVc, versionName, force, minSup, apkUrl, sha256, size, notes, publishedAt);
        } catch (Exception e) {
            Log.w(TAG, "parse check-update", e);
            return CheckUpdateResult.httpFail(httpCode, "解析响应失败: " + e.getMessage());
        }
    }

    /**
     * 下载 APK 到 {@code destination}（覆盖写入）。
     */
    public static void downloadApkSync(@NonNull String apkUrl, @NonNull File destination) throws IOException {
        Request request = new Request.Builder()
                .url(apkUrl)
                .get()
                .build();
        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("下载失败 HTTP " + response.code());
            }
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("无法创建目录: " + parent);
            }
            try (InputStream in = response.body().byteStream();
                 FileOutputStream out = new FileOutputStream(destination, false)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                out.flush();
            }
        }
    }

    public static boolean verifySha256(@NonNull File file, @NonNull String expectedHex) throws IOException {
        String expect = expectedHex.trim().toLowerCase(Locale.US);
        if (expect.isEmpty()) {
            return true;
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException(e);
        }
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString().equalsIgnoreCase(expect);
    }

    /**
     * 发起安装 APK。失败时返回 false（调用方应提示用户手动安装）。
     */
    public static boolean tryStartInstall(@NonNull Activity activity, @NonNull File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                try {
                    Uri uri = Uri.parse("package:" + activity.getPackageName());
                    Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri);
                    activity.startActivity(s);
                } catch (Exception e) {
                    Log.w(TAG, "open unknown sources settings", e);
                }
                return false;
            }
        }
        try {
            String authority = BuildConfig.APPLICATION_ID + ".fileprovider";
            Uri apkUri = FileProvider.getUriForFile(activity, authority, apkFile);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(install);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "start install", e);
            return false;
        }
    }

    @NonNull
    public static String formatSubtitle(@NonNull CheckUpdateResult r) {
        StringBuilder sb = new StringBuilder();
        if (!r.versionName.isEmpty()) {
            sb.append("新版本 ").append(r.versionName);
            if (r.remoteVersionCode > 0) {
                sb.append(" (").append(r.remoteVersionCode).append(")");
            }
        } else if (r.remoteVersionCode > 0) {
            sb.append("新版本 build ").append(r.remoteVersionCode);
        }
        if (!r.releaseNotes.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            for (int i = 0; i < r.releaseNotes.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append("• ").append(r.releaseNotes.get(i));
            }
        }
        if (sb.length() == 0) {
            sb.append("优化产品体验，修复若干问题");
        }
        return sb.toString();
    }
}
