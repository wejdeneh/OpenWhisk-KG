package com.qse.actions;

import com.qse.utils.StorageUtils;

public class WriteBlobGivenName {
    
    public static void writeBlobGivenName(String name, byte[] data) {
        StorageUtils.uploadBlob(name, data);
    }

    public static void writeBlobGivenName(String name, String data) {
        StorageUtils.uploadBlob(name, data.getBytes());
    }
}