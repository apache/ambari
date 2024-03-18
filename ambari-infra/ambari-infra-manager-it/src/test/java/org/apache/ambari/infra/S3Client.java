/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;

public class S3Client {
  private final MinioClient s3client;
  private final String bucket;

  public S3Client(String host, int port, String bucket) {
    try {
      s3client = new MinioClient(String.format("http://%s:%d", host, port), "remote-identity", "remote-credential");
      this.bucket = bucket;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void createBucket() {
    try {
      if (!s3client.bucketExists(bucket))
        s3client.makeBucket(bucket);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void putObject(String key, InputStream inputStream, long length) {
    try {
      s3client.putObject(bucket, key, inputStream, length, "application/octet-stream");
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void putObject(String key, byte[] bytes) {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream("anything".getBytes())) {
      putObject(key, inputStream, bytes.length);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public List<String> listObjectKeys() {
    try {
      List<String> keys = new ArrayList<>();
      for (Result<Item> item : s3client.listObjects(bucket)) {
        keys.add(item.get().objectName());
      }
      return keys;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<String> listObjectKeys(String text) {
    try {
      List<String> keys = new ArrayList<>();
      for (Result<Item> item : s3client.listObjects(bucket)) {
        String objectName = item.get().objectName();
        if (objectName.contains(text))
          keys.add(objectName);
      }
      return keys;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteObject(String key) {
    try {
      s3client.removeObject(bucket, key);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public InputStream getObject(String key) {
    try {
      return s3client.getObject(bucket, key);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
