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

package org.apache.ambari.infra.solr;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Uploads a file to S3, meant to be used by solrDataManager.py
 */
public class S3Uploader {
  public static void main(String[] args) {
    try {
      String keyFilePath = args[0];
      String bucketName = args[1];
      String keyPrefix = args[2];
      String filePath = args[3];

      String keyFileContent = FileUtils.readFileToString(new File(keyFilePath)).trim();
      String[] keys = keyFileContent.split(",");
      String accessKey = keys[0];
      String secretKey = keys[1];

      BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
      AmazonS3Client client = new AmazonS3Client(credentials);

      File file = new File(filePath);
      String key = keyPrefix + file.getName();
      
      if (client.doesObjectExist(bucketName, key)) {
        System.out.println("Object '" + key + "' already exists");
        System.exit(0);
      }

      client.putObject(bucketName, key, file);
    } catch (Exception e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
    
    System.exit(0);
  }
}
