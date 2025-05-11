package com.qse.utils;

import io.minio.*;
import io.minio.errors.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class StorageUtils {
    
    private static MinioClient minioClient;
    private static final String BUCKET_NAME = "qse-storage";
    
    static {
        try {
            minioClient = MinioClient.builder()
                .endpoint(System.getenv("MINIO_ENDPOINT"))
                .credentials(System.getenv("MINIO_ACCESS_KEY"), System.getenv("MINIO_SECRET_KEY"))
                .build();
            
            // Create bucket if it doesn't exist
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void uploadBlob(String objectName, byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectName)
                    .stream(bais, data.length, -1)
                    .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static byte[] downloadBlob(String objectName) {
        try {
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectName)
                    .build()
            );
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static BufferedReader getBufferedReader(String objectName) {
        try {
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectName)
                    .build()
            );
            return new BufferedReader(new InputStreamReader(stream));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
