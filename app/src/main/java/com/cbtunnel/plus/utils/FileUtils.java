package com.cbtunnel.plus.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.utils.de.De;
import com.cbtunnel.plus.utils.de.De2;
import com.cbtunnel.plus.utils.en.En;
import com.cbtunnel.plus.utils.en.En2;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Scanner;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class FileUtils {

    @SuppressLint("NewApi")
    public static boolean copyToClipboard(Context context, String text){
        try{
            int sdk = android.os.Build.VERSION.SDK_INT;
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                clipboard.setText(text);
            }
            else {
                android.content.ClipData clip = android.content.ClipData.newPlainText("Message", text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        } catch (Exception e) {
            hLogStatus.logDebug(e.getMessage());
            return false;
        }
    }

    public static String getClipboard(Context context){
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);
            return clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String readFromAsset(final AppCompatActivity c,String name){
        try {
            File file = new File(c.getFilesDir(),name);
            StringBuilder b = new StringBuilder();
            Reader reader = null;
            char[] buff = new char[1024];
            if (file.exists()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            } else {
                reader = new BufferedReader(new InputStreamReader(c.getAssets().open(name)));
            }
            while (true) {
                int read = reader.read(buff,0,buff.length);
                if (read <= 0) {
                    break;
                }
                b.append(buff,0,read);
            }
            return showJson(b.toString());
        } catch (Exception e) {
            Toast.makeText(c,"readFromAsset error! "+e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public static String readFromRaw(Context context, int resId) {
        InputStream in = context.getResources().openRawResource(resId);
        Scanner scanner = new Scanner(in,"UTF-8").useDelimiter("\\A");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.next());
        }
        scanner.close();
        return sb.toString();
    }

    public static String readTextUri(Context c, Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(c.getContentResolver().openInputStream(uri)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

    public static File zipFile(Context cont) {
        try {
            File file = new File(cont.getFilesDir(), "mtkConfig.zip");
            InputStream in = cont.getAssets().open("mtkConfig.zip");
            OutputStream out = new FileOutputStream(file);
            byte[] bits = new byte[1024];
            while (true) {
                int read = in.read(bits, 0, bits.length);
                if (read <= 0) {
                    break;
                }
                out.write(bits, 0, read);
            }
            out.flush();
            out.close();
            in.close();
            return file;
        } catch (Exception e) {
        }
        return null;
    }

    private static String getPass() {
//        String str = "4861726R696573446576582I37506R75732I616R6R2I6I65742I636E6L2I7068";

        //new get pass
//        String str = "7074696669782E7370696465726D616E2E6669786275696C647A6F6E652E6D61782E7068";
        //New Aug
        String str = "4669786275696R6465762I6669782I636E72652I74756I6I656R2I62642I7677";
        return str.substring(0, 4);
    }

    public static String showJson(String msg){
        try {
            String _de = XxTea.decryptBase64StringToString(msg, getPass());
            if (_de.isEmpty()||_de==null){
                return "";
            }
            return _De(_de);
        }catch (Exception e){
            return "";
        }
    }

    public static String hideJson(String msg){
        try {
            String _en = _En(msg);
            if (_en.isEmpty()||_en==null){
                return "";
            }
            return XxTea.encryptToBase64String(_en, getPass());
        }catch (Exception e){
            return "";
        }
    }

    public static String encodeID(String realID) {
        StringBuilder fakeID = new StringBuilder();
        for (char c : realID.toCharArray()) {
            fakeID.append(encodeChar(c));
        }
        return fakeID.toString();
    }

    // Function to decode the fake V2Ray ID back to the real ID
    public static String decodeID(String fakeID) {
        StringBuilder realID = new StringBuilder();
        for (char c : fakeID.toCharArray()) {
            realID.append(decodeChar(c));
        }
        return realID.toString();
    }

    // Helper function to encode a character
    private static char encodeChar(char c) {
        // Example transformation: shift alphanumeric characters by 3 positions
        if (c >= '0' && c <= '9') {
            return (char) ('0' + (c - '0' + 3) % 10);
        } else if (c >= 'a' && c <= 'z') {
            return (char) ('a' + (c - 'a' + 3) % 26);
        } else if (c >= 'A' && c <= 'Z') {
            return (char) ('A' + (c - 'A' + 3) % 26);
        }
        return c; // Non-alphanumeric characters are unchanged
    }

    // Helper function to decode a character
    private static char decodeChar(char c) {
        // Example transformation: reverse the shift by 3 positions
        if (c >= '0' && c <= '9') {
            return (char) ('0' + (c - '0' + 7) % 10);
        } else if (c >= 'a' && c <= 'z') {
            return (char) ('a' + (c - 'a' + 23) % 26);
        } else if (c >= 'A' && c <= 'Z') {
            return (char) ('A' + (c - 'A' + 23) % 26);
        }
        return c; // Non-alphanumeric characters are unchanged
    }

    private static String _De(String msg)throws Exception{
        De de = new De();
        de.setMethod(new De2());
        int p = Integer.parseInt(getPass());
        String de1 = de.decryptString(msg, p);
        return de.decryptString(de1, p);
    }
    private static String _En(String msg)throws Exception{
        En encrypter = new En();
        encrypter.setMethod(new En2());
        int p = Integer.parseInt(getPass());
        String e1 = encrypter.encryptString(msg,p);
        return encrypter.encryptString(e1,p);
    }

    public static String hideSTR(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    public static class FileTooLarge extends IOException {
        public FileTooLarge(String fn, long max_size) {
            super(String.format(harliesApplication.resString(R.string.file_too_large), new Object[]{fn, Long.valueOf(max_size)}));
        }
    }

    public static String readFile(String path, long max_len) throws IOException {
        return readStream(new FileInputStream(path), max_len, path);
    }

    public static String readUri(Context context, Uri uri, long max_len) throws IOException {
        return readStream(context.getContentResolver().openInputStream(uri), max_len, uriBasename(uri));
    }

    public static String readAsset(Context context, String filename) throws IOException {
        return readStream(context.getResources().getAssets().open(filename), 0, filename);
    }

    public static String readFileAppPrivate(Context context, String filename) throws IOException {
        return readStream(context.openFileInput(filename), 0, filename);
    }

    public static void writeFileAppPrivate(Context context, String filename, String content) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, 0);
        try {
            fos.write(content.getBytes());
        } finally {
            fos.close();
        }
    }

    public static String readStream(InputStream stream, long max_len, String fn) throws IOException {
        try {
            Reader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            while (true) {
                int read = reader.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    break;
                }
                builder.append(buffer, 0, read);
                if (max_len > 0 && ((long) builder.length()) > max_len) {
                    throw new FileTooLarge(fn, max_len);
                }
            }
            String stringBuilder = builder.toString();
            return stringBuilder;
        } finally {
            stream.close();
        }
    }

    public static byte[] readFileByteArray(String path, long max_len) throws IOException {
        File file = new File(path);
        InputStream is = new FileInputStream(file);
        try {
            long length = file.length();
            if ((max_len <= 0 || length <= max_len) && length <= 2147483647L) {
                byte[] bytes = new byte[((int) length)];
                int offset = 0;
                while (offset < bytes.length) {
                    int numRead = is.read(bytes, offset, bytes.length - offset);
                    if (numRead < 0) {
                        break;
                    }
                    offset += numRead;
                }
                if (offset >= bytes.length) {
                    return bytes;
                }
                throw new IOException("Could not completely read file: " + path);
            }
            throw new FileTooLarge(path, max_len);
        } finally {
            is.close();
        }
    }

    public static boolean deleteFile(String path) {
        if (path != null) {
            return new File(path).delete();
        }
        return false;
    }

    public static boolean renameFile(String from_path, String to_path) {
        if (from_path == null || to_path == null) {
            return false;
        }
        return new File(from_path).renameTo(new File(to_path));
    }

    public static String basename(String path) {
        if (path != null) {
            return new File(path).getName();
        }
        return null;
    }

    public static String dirname(String path) {
        if (path != null) {
            return new File(path).getParent();
        }
        return null;
    }

    public static String uriBasename(Uri uri) {
        if (uri != null) {
            return uri.getLastPathSegment();
        }
        return null;
    }

}
