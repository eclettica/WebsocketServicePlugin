package it.linup.cordova.plugin.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileUtils {

    private static String tag = "FileUtils ";


    public static void chmod(String path, String permissions,
                             boolean isRecursive) throws IOException {
        ProcessBuilder pb;
        if (isRecursive) {
            pb = new ProcessBuilder("/system/bin/chmod", "-R", permissions, path);
        } else {
            pb = new ProcessBuilder("/system/bin/chmod", permissions, path);
        }
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                LogUtils.printLog(tag,line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts all files in the directory specified relative to the assets
     * folder
     *
     * @param path
     *            The directory to extract
     * @throws IOException
     */
    public static void extractAssetDirectory(Context c, String path)
            throws IOException {
        String list[] = c.getAssets().list(path);
        String internalDirPath = c.getFilesDir().getCanonicalPath();
        if (list.length > 0) { // Path is a folder
            new File(internalDirPath + '/' + path).mkdir();
            for (String file : list) {
                extractAssetDirectory(c, path + '/' + file);
            }
        } else { // Path is a file
            extractAssetFile(c, path);
        }
    }

    /**
     * Extracts a single file specified by path relative to the assets folder
     *
     * @throws IOException
     */
    public static void extractAssetFile(Context c, String path)
            throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            File internalDir = c.getFilesDir();
            String outPath = internalDir.getCanonicalPath() + "/" + path;

            in = c.getAssets().open(path);
            out = new FileOutputStream(outPath);
            copyFile(in, out);

            out.flush();
        } catch (IOException e) {
            Log.e("FileUtils", "Error extracting " + path);
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    public static void copyFile(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void writeToFile(String name, String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(name, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String readFromFile(String name, Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(name);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

}
