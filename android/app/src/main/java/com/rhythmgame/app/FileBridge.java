package com.rhythmgame.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import androidx.core.content.FileProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileBridge {
    private static final String TAG = "RGameBridge";
    private final Context ctx;
    private final Activity activity;
    private final File storageRoot;
    private final Handler mainHandler;
    private final List<String[]> importResults = new ArrayList<>(); // [name, "mp3"|"json", base64data]

    public FileBridge(Activity activity) {
        this.ctx = activity;
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
        File f = activity.getExternalFilesDir(null);
        if (f == null) f = activity.getFilesDir();
        this.storageRoot = f;
        if (!storageRoot.exists()) storageRoot.mkdirs();
    }

    @JavascriptInterface public String getStoragePath() { return storageRoot != null ? storageRoot.getAbsolutePath() : ""; }

    /* ── Native file picker ── */

    @JavascriptInterface
    public void pickFiles() {
        mainHandler.post(() -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "application/json"});
            activity.startActivityForResult(intent, 100);
        });
    }

    // Called from MainActivity.onActivityResult
    void processImportUris(List<Uri> uris) {
        importResults.clear();
        for (Uri uri : uris) {
            try {
                String name = getFileName(uri);
                if (name == null) name = "import_" + System.currentTimeMillis();
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
                String type = ext.equals(".mp3") ? "mp3" : "json";

                // Read file content
                InputStream in = ctx.getContentResolver().openInputStream(uri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                in.close();

                String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                importResults.add(new String[]{name, type, b64});
            } catch (Exception e) {
                Log.e(TAG, "processImportUri error", e);
            }
        }
    }

    @JavascriptInterface
    public String importGetCount() { return String.valueOf(importResults.size()); }

    @JavascriptInterface
    public String importGetName(int index) {
        if (index < 0 || index >= importResults.size()) return null;
        return importResults.get(index)[0];
    }

    @JavascriptInterface
    public String importGetType(int index) {
        if (index < 0 || index >= importResults.size()) return null;
        return importResults.get(index)[1];
    }

    @JavascriptInterface
    public String importGetData(int index) {
        if (index < 0 || index >= importResults.size()) return null;
        return importResults.get(index)[2];
    }

    private String getFileName(Uri uri) {
        String name = null;
        try (Cursor c = ctx.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception e) {}
        if (name == null) {
            String path = uri.getLastPathSegment();
            if (path != null && path.contains("/")) path = path.substring(path.lastIndexOf('/') + 1);
            name = path != null ? path : "import";
        }
        // Remove any URI-encoding
        try { name = java.net.URLDecoder.decode(name, "UTF-8"); } catch (Exception e) {}
        return name;
    }

    /* ── File listing ── */

    @JavascriptInterface public String listMp3s() { return listFiles(".mp3"); }
    @JavascriptInterface public String listJsons() { return listFiles(".json"); }

    private String listFiles(String ext) {
        try {
            File[] files = storageRoot.listFiles((d, n) -> n.toLowerCase().endsWith(ext));
            JSONArray arr = new JSONArray();
            if (files != null) for (File f : files) arr.put(f.getName());
            return arr.toString();
        } catch (Exception e) { return "[]"; }
    }

    @JavascriptInterface
    public String getJsonNoteCount(String filename) {
        try {
            String text = readFileText(filename);
            if (text == null) return "0";
            JSONObject obj = new JSONObject(text);
            JSONArray notes = obj.optJSONArray("notes");
            return String.valueOf(notes != null ? notes.length() : 0);
        } catch (Exception e) { return "0"; }
    }

    /* ── Delete ── */

    @JavascriptInterface
    public boolean deleteFile(String name) {
        try {
            File f = new File(storageRoot, name);
            return f.exists() && f.delete();
        } catch (Exception e) { return false; }
    }

    /* ── Read ── */

    @JavascriptInterface
    public String readFileText(String name) {
        try {
            File f = new File(storageRoot, name);
            if (!f.exists()) return null;
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    @JavascriptInterface
    public String readFileBase64(String name) {
        try {
            File f = new File(storageRoot, name);
            if (!f.exists()) return null;
            int len = (int) f.length();
            byte[] data = new byte[len];
            FileInputStream fis = new FileInputStream(f);
            int total = 0;
            while (total < len) total += fis.read(data, total, len - total);
            fis.close();
            return Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    /* ── Write ── */

    @JavascriptInterface
    public boolean writeFileBase64(String name, String base64Data) {
        try {
            File f = new File(storageRoot, name);
            byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data);
            fos.close();
            return true;
        } catch (Exception e) { return false; }
    }

    @JavascriptInterface
    public boolean writeFileText(String name, String text) {
        try {
            File f = new File(storageRoot, name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(text.getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (Exception e) { return false; }
    }

    /* ── URI ── */

    @JavascriptInterface
    public String getFileUri(String name) {
        try {
            File f = new File(storageRoot, name);
            if (!f.exists()) return null;
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", f);
            return uri.toString();
        } catch (Exception e) { return null; }
    }

    @JavascriptInterface
    public String getAssetUri(String assetPath) {
        return "file:///android_asset/" + assetPath;
    }

    /* ── Demo ── */

    @JavascriptInterface
    public boolean setupDemo() {
        try {
            copyIfMissing("闪亮.mp3");
            copyIfMissing("闪亮.json");
            return true;
        } catch (Exception e) { return false; }
    }

    private void copyIfMissing(String name) throws IOException {
        File dest = new File(storageRoot, name);
        if (dest.exists()) return;
        InputStream in = ctx.getAssets().open(name);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
    }
}
