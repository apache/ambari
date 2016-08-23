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
package org.apache.ambari.logfeeder.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * Utility to connect to s3
 */
public class S3Util {
  public static final S3Util INSTANCE = new S3Util();

  private static final Logger LOG = Logger.getLogger(S3Util.class);

  public static final String S3_PATH_START_WITH = "s3://";
  public static final String S3_PATH_SEPARATOR = "/";

  public AmazonS3 getS3Client(String accessKey, String secretKey) {
    AWSCredentials awsCredentials = AWSUtil.INSTANCE.createAWSCredentials(
        accessKey, secretKey);
    AmazonS3 s3client;
    if (awsCredentials != null) {
      s3client = new AmazonS3Client(awsCredentials);
    } else {
      s3client = new AmazonS3Client();
    }
    return s3client;
  }

  public TransferManager getTransferManager(String accessKey, String secretKey) {
    AWSCredentials awsCredentials = AWSUtil.INSTANCE.createAWSCredentials(
        accessKey, secretKey);
    TransferManager transferManager;
    if (awsCredentials != null) {
      transferManager = new TransferManager(awsCredentials);
    } else {
      transferManager = new TransferManager();
    }
    return transferManager;
  }

  public void shutdownTransferManager(TransferManager transferManager) {
    if (transferManager != null) {
      transferManager.shutdownNow();
    }
  }

  public String getBucketName(String s3Path) {
    String bucketName = null;
    // s3path
    if (s3Path != null) {
      String[] s3PathParts = s3Path.replace(S3_PATH_START_WITH, "").split(
          S3_PATH_SEPARATOR);
      bucketName = s3PathParts[0];
    }
    return bucketName;
  }

  public String getS3Key(String s3Path) {
    StringBuilder s3Key = new StringBuilder();
    // s3path
    if (s3Path != null) {
      String[] s3PathParts = s3Path.replace(S3_PATH_START_WITH, "").split(
          S3_PATH_SEPARATOR);
      ArrayList<String> s3PathList = new ArrayList<String>(
          Arrays.asList(s3PathParts));
      s3PathList.remove(0);// remove bucketName
      for (int index = 0; index < s3PathList.size(); index++) {
        if (index > 0) {
          s3Key.append(S3_PATH_SEPARATOR);
        }
        s3Key.append(s3PathList.get(index));
      }
    }
    return s3Key.toString();
  }

  public void uploadFileTos3(String bucketName, String s3Key, File localFile,
      String accessKey, String secretKey) {
    TransferManager transferManager = getTransferManager(accessKey, secretKey);
    try {
      Upload upload = transferManager.upload(bucketName, s3Key, localFile);
      upload.waitForUploadResult();
    } catch (AmazonClientException | InterruptedException e) {
      LOG.error("s3 uploading failed for file :" + localFile.getAbsolutePath(),
          e);
    } finally {
      shutdownTransferManager(transferManager);
    }
  }

  /**
   * Get the buffer reader to read s3 file as a stream
   */
  public BufferedReader getReader(String s3Path, String accessKey,
      String secretKey) throws IOException {
    // TODO error handling
    // Compression support
    // read header and decide the compression(auto detection)
    // For now hard-code GZIP compression
    String s3Bucket = getBucketName(s3Path);
    String s3Key = getS3Key(s3Path);
    S3Object fileObj = getS3Client(accessKey, secretKey).getObject(
        new GetObjectRequest(s3Bucket, s3Key));
    GZIPInputStream objectInputStream;
    try {
      objectInputStream = new GZIPInputStream(fileObj.getObjectContent());
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
          objectInputStream));
      return bufferedReader;
    } catch (IOException e) {
      LOG.error("Error in creating stream reader for s3 file :" + s3Path,
          e.getCause());
      throw e;
    }
  }

  public void writeIntoS3File(String data, String bucketName, String s3Key,
      String accessKey, String secretKey) {
    InputStream in = null;
    try {
      in = IOUtils.toInputStream(data, "UTF-8");
    } catch (IOException e) {
      LOG.error(e);
    }
    if (in != null) {
      TransferManager transferManager = getTransferManager(accessKey, secretKey);
      try {
        if (transferManager != null) {
          transferManager.upload(
                  new PutObjectRequest(bucketName, s3Key, in,
                  new ObjectMetadata())).waitForUploadResult();
          LOG.debug("Data Uploaded to s3 file :" + s3Key + " in bucket :"
              + bucketName);
        }
      } catch (AmazonClientException | InterruptedException e) {
        LOG.error(e);
      } finally {
        try {
          shutdownTransferManager(transferManager);
          in.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

}
