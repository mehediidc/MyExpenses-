package com.mhdigital.myexpenses.nativeplugin;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MyExpensesNative extends CordovaPlugin {
    private static final int REQ_STORAGE = 7211;
    private CallbackContext pendingBackup;
    private String pendingFilename;
    private String pendingJson;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("printHtml".equals(action)) {
            final String title = args.optString(0, "MyExpenses");
            final String html = args.optString(1, "");
            cordova.getActivity().runOnUiThread(() -> printHtml(title, html, callbackContext));
            return true;
        }
        if ("saveBackup".equals(action)) {
            pendingFilename = args.optString(0, "MyExpenses_Backup.json");
            pendingJson = args.optString(1, "");
            pendingBackup = callbackContext;
            cordova.getActivity().runOnUiThread(this::saveBackupInternal);
            return true;
        }
        return false;
    }

    private void printHtml(String title, String html, CallbackContext cb) {
        Activity activity = cordova.getActivity();
        final WebView printView = new WebView(activity);
        printView.setBackgroundColor(Color.WHITE);
        printView.getSettings().setJavaScriptEnabled(false);
        printView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(title);
                pm.print(title, adapter, new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build());
                cb.success("Print dialog opened");
            }
        });
        String doc = "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><style>"
            + "body{font-family:Arial,sans-serif;padding:18px;color:#111;background:#fff}h2{text-align:center;margin:0 0 16px}"
            + ".summary-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-bottom:14px}.summary{border:1px solid #aaa;padding:10px}"
            + ".summary span{display:block;font-size:11px}.summary strong{display:block;font-size:16px;margin-top:4px}.report-line{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #ddd}"
            + ".report-name{font-weight:700}.report-values{text-align:right}.ledger-table{width:100%;border-collapse:collapse;font-size:12px}.ledger-table th,.ledger-table td{border:1px solid #999;padding:7px 6px}.ledger-table th{background:#eee}.ledger-table td.num{text-align:right}.ledger-opening{font-weight:700;background:#f5f5f5}.print-meta{font-size:11px;color:#555;margin-bottom:10px}@page{size:A4;margin:10mm}"
            + "</style></head><body><h2>" + escapeHtml(title) + "</h2>" + html + "</body></html>";
        printView.loadDataWithBaseURL(null, doc, "text/html", "UTF-8", null);
    }

    private void saveBackupInternal() {
        if (pendingBackup == null) return;
        try {
            if (Build.VERSION.SDK_INT <= 28 && cordova.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 23) {
                    cordova.getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
                    return;
                }
            }
            writeBackup();
        } catch (Exception e) {
            pendingBackup.error("Backup failed: " + e.getMessage());
            pendingBackup = null;
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == REQ_STORAGE && pendingBackup != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) writeBackup();
            else { pendingBackup.error("Storage permission denied"); pendingBackup = null; }
        }
    }

    private void writeBackup() {
        try {
            String safe = pendingFilename.replaceAll("[^A-Za-z0-9._-]", "_");
            OutputStream out;
            String location;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyExpenses");
                android.net.Uri uri = cordova.getActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Could not create Downloads file");
                out = cordova.getActivity().getContentResolver().openOutputStream(uri);
                location = "Downloads/MyExpenses/" + safe;
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyExpenses");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("Could not create Downloads folder");
                File file = new File(dir, safe);
                out = new FileOutputStream(file);
                location = file.getAbsolutePath();
            }
            if (out == null) throw new Exception("Could not open backup file");
            out.write(pendingJson.getBytes("UTF-8"));
            out.close();
            final String msg = "Backup saved: " + location;
            cordova.getActivity().runOnUiThread(() -> Toast.makeText(cordova.getActivity(), msg, Toast.LENGTH_LONG).show());
            pendingBackup.success(location);
            pendingBackup = null;
        } catch (Exception e) {
            if (pendingBackup != null) pendingBackup.error("Backup failed: " + e.getMessage());
            pendingBackup = null;
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
