package de.xikolo.util;

import java.io.File;
import java.text.DecimalFormat;

public class FileUtil {

    public static String getFormattedFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                length += file.length();
            } else {
                length += folderSize(file);
            }
        }
        return length;
    }

    public static long folderFileNumber(File directory) {
        long files = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                files++;
            } else {
                files += folderFileNumber(file);
            }
        }
        return files;
    }

    public static void delete(File directory) {
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                file.delete();
            } else {
                delete(file);
            }
        }
    }

}
