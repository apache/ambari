/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class S3UploaderTest {

  public static final String TEST_BUCKET = "test_bucket";
  public static final String TEST_PATH = "test_path";
  public static final String GZ = "gz";
  public static final String LOG_TYPE = "hdfs_namenode";
  public static final String ACCESS_KEY_VALUE = "accessKeyValue";
  public static final String SECRET_KEY_VALUE = "secretKeyValue";

  @Test
  public void shouldUploadToS3ToRightBucket() {
    File fileToUpload = mock(File.class);
    String fileName = "hdfs_namenode.log.123343493473948";
    expect(fileToUpload.getName()).andReturn(fileName);
    final File compressedFile = mock(File.class);
    Map<String, Object> configs = setupS3Configs();

    S3OutputConfiguration s3OutputConfiguration = new S3OutputConfiguration(configs);
    expect(compressedFile.delete()).andReturn(true);
    expect(fileToUpload.delete()).andReturn(true);
    replay(fileToUpload, compressedFile);

    S3Uploader s3Uploader = new S3Uploader(s3OutputConfiguration, true, LOG_TYPE) {
      @Override
      protected File createCompressedFileForUpload(File fileToUpload, String compressionAlgo) {
        return compressedFile;
      }
      protected void uploadFileToS3(String bucketName, String s3Key, File localFile, String accessKey, String secretKey) {
      }
    };
    String resolvedPath = s3Uploader.uploadFile(fileToUpload, LOG_TYPE);

    assertEquals("test_path/hdfs_namenode/hdfs_namenode.log.123343493473948.gz", resolvedPath);
  }

  @Test
  public void shouldCleanupLocalFilesOnSuccessfulUpload() {
    File fileToUpload = mock(File.class);
    String fileName = "hdfs_namenode.log.123343493473948";
    expect(fileToUpload.getName()).andReturn(fileName);
    final File compressedFile = mock(File.class);
    Map<String, Object> configs = setupS3Configs();

    S3OutputConfiguration s3OutputConfiguration = new S3OutputConfiguration(configs);
    expect(compressedFile.delete()).andReturn(true);
    expect(fileToUpload.delete()).andReturn(true);
    replay(fileToUpload, compressedFile);

    S3Uploader s3Uploader = new S3Uploader(s3OutputConfiguration, true, LOG_TYPE) {
      @Override
      protected File createCompressedFileForUpload(File fileToUpload, String compressionAlgo) {
        return compressedFile;
      }
      protected void uploadFileToS3(String bucketName, String s3Key, File localFile, String accessKey, String secretKey) {
      }
    };
    s3Uploader.uploadFile(fileToUpload, LOG_TYPE);

    verify(fileToUpload);
    verify(compressedFile);
  }

  @Test
  public void shouldNotCleanupUncompressedFileIfNotRequired() {
    File fileToUpload = mock(File.class);
    String fileName = "hdfs_namenode.log.123343493473948";
    expect(fileToUpload.getName()).andReturn(fileName);
    final File compressedFile = mock(File.class);
    Map<String, Object> configs = setupS3Configs();

    S3OutputConfiguration s3OutputConfiguration = new S3OutputConfiguration(configs);
    expect(compressedFile.delete()).andReturn(true);
    replay(fileToUpload, compressedFile);

    S3Uploader s3Uploader = new S3Uploader(s3OutputConfiguration, false, LOG_TYPE) {
      @Override
      protected File createCompressedFileForUpload(File fileToUpload, String compressionAlgo) {
        return compressedFile;
      }
      protected void uploadFileToS3(String bucketName, String s3Key, File localFile, String accessKey, String secretKey) {
      }
    };
    s3Uploader.uploadFile(fileToUpload, LOG_TYPE);

    verify(fileToUpload);
    verify(compressedFile);
  }

  @Test
  public void shouldExpandVariablesInPath() {
    File fileToUpload = mock(File.class);
    String fileName = "hdfs_namenode.log.123343493473948";
    expect(fileToUpload.getName()).andReturn(fileName);
    final File compressedFile = mock(File.class);
    Map<String, Object> configs = setupS3Configs();
    configs.put(S3OutputConfiguration.S3_LOG_DIR_KEY, "$cluster/"+TEST_PATH);


    S3OutputConfiguration s3OutputConfiguration = new S3OutputConfiguration(configs);
    expect(compressedFile.delete()).andReturn(true);
    expect(fileToUpload.delete()).andReturn(true);
    replay(fileToUpload, compressedFile);

    S3Uploader s3Uploader = new S3Uploader(s3OutputConfiguration, true, LOG_TYPE) {
      @Override
      protected File createCompressedFileForUpload(File fileToUpload, String compressionAlgo) {
        return compressedFile;
      }
      protected void uploadFileToS3(String bucketName, String s3Key, File localFile, String accessKey, String secretKey) {
      }
    };
    s3Uploader.uploadFile(fileToUpload, LOG_TYPE);
  }

  private Map<String, Object> setupS3Configs() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(S3OutputConfiguration.S3_BUCKET_NAME_KEY, TEST_BUCKET);
    configs.put(S3OutputConfiguration.S3_LOG_DIR_KEY, TEST_PATH);
    configs.put(S3OutputConfiguration.S3_ACCESS_KEY, ACCESS_KEY_VALUE);
    configs.put(S3OutputConfiguration.S3_SECRET_KEY, SECRET_KEY_VALUE);
    configs.put(S3OutputConfiguration.COMPRESSION_ALGO_KEY, GZ);
    Map<String, String> nameValueMap = new HashMap<>();
    nameValueMap.put(S3OutputConfiguration.CLUSTER_KEY, "cl1");
    configs.put(S3OutputConfiguration.ADDITIONAL_FIELDS_KEY, nameValueMap);
    return configs;
  }
}
