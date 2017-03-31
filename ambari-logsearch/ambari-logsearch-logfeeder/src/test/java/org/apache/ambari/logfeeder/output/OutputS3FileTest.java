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

import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.output.spool.LogSpooler;
import org.apache.ambari.logfeeder.output.spool.LogSpoolerContext;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class OutputS3FileTest {

  private Map<String, Object> configMap;

  @Before
  public void setupConfiguration() {
    configMap = new HashMap<>();
    String[] configKeys = new String[] {
        S3OutputConfiguration.SPOOL_DIR_KEY,
        S3OutputConfiguration.S3_BUCKET_NAME_KEY,
        S3OutputConfiguration.S3_LOG_DIR_KEY,
        S3OutputConfiguration.S3_ACCESS_KEY,
        S3OutputConfiguration.S3_SECRET_KEY,
        S3OutputConfiguration.COMPRESSION_ALGO_KEY,
        S3OutputConfiguration.ADDITIONAL_FIELDS_KEY
    };
    Map<String, String> additionalKeys = new HashMap<>();
    additionalKeys.put(S3OutputConfiguration.CLUSTER_KEY, "cl1");
    Object[] configValues = new Object[] {
        "/var/ambari-logsearch/logfeeder",
        "s3_bucket_name",
        "logs",
        "ABCDEFGHIJ1234",
        "amdfbldkfdlf",
        "gz",
        additionalKeys
    };
    for (int i = 0; i < configKeys.length; i++) {
      configMap.put(configKeys[i], configValues[i]);
    }
  }

  @Test
  public void shouldSpoolLogEventToNewSpooler() throws Exception {

    Input input = mock(Input.class);
    InputMarker inputMarker = new InputMarker(input, null, 0);
    expect(input.getFilePath()).andReturn("/var/log/hdfs-namenode.log");
    expect(input.getStringValue(OutputS3File.INPUT_ATTRIBUTE_TYPE)).andReturn("hdfs-namenode");
    final LogSpooler spooler = mock(LogSpooler.class);
    spooler.add("log event block");
    final S3Uploader s3Uploader = mock(S3Uploader.class);
    replay(input, spooler, s3Uploader);

    OutputS3File outputS3File = new OutputS3File() {
      @Override
      protected LogSpooler createSpooler(String filePath) {
        return spooler;
      }

      @Override
      protected S3Uploader createUploader(String logType) {
        return s3Uploader;
      }
    };
    outputS3File.loadConfig(configMap);
    outputS3File.init();
    outputS3File.write("log event block", inputMarker);
    verify(spooler);
  }

  @Test
  public void shouldReuseSpoolerForSamePath() throws Exception {
    Input input = mock(Input.class);
    InputMarker inputMarker = new InputMarker(input, null, 0);
    expect(input.getFilePath()).andReturn("/var/log/hdfs-namenode.log");
    expect(input.getStringValue(OutputS3File.INPUT_ATTRIBUTE_TYPE)).andReturn("hdfs-namenode");
    final LogSpooler spooler = mock(LogSpooler.class);
    spooler.add("log event block1");
    spooler.add("log event block2");
    final S3Uploader s3Uploader = mock(S3Uploader.class);
    replay(input, spooler, s3Uploader);

    OutputS3File outputS3File = new OutputS3File() {
      private boolean firstCallComplete;
      @Override
      protected LogSpooler createSpooler(String filePath) {
        if (!firstCallComplete) {
          firstCallComplete = true;
          return spooler;
        }
        throw new IllegalStateException("Shouldn't call createSpooler for same path.");
      }

      @Override
      protected S3Uploader createUploader(String logType) {
        return s3Uploader;
      }
    };
    outputS3File.loadConfig(configMap);
    outputS3File.init();
    outputS3File.write("log event block1", inputMarker);
    outputS3File.write("log event block2", inputMarker);
    verify(spooler);
  }

  @Test
  public void shouldRolloverWhenSufficientSizeIsReached() throws Exception {

    String thresholdSize = Long.toString(15 * 1024 * 1024L);
    LogSpoolerContext logSpoolerContext = mock(LogSpoolerContext.class);
    File activeSpoolFile = mock(File.class);
    expect(activeSpoolFile.length()).andReturn(20*1024*1024L);
    expect(logSpoolerContext.getActiveSpoolFile()).andReturn(activeSpoolFile);
    replay(logSpoolerContext, activeSpoolFile);

    OutputS3File outputS3File = new OutputS3File();
    configMap.put(S3OutputConfiguration.ROLLOVER_SIZE_THRESHOLD_BYTES_KEY, thresholdSize);
    outputS3File.loadConfig(configMap);
    outputS3File.init();

    assertTrue(outputS3File.shouldRollover(logSpoolerContext));
  }

  @Test
  public void shouldNotRolloverBeforeSufficientSizeIsReached() throws Exception {
    String thresholdSize = Long.toString(15 * 1024 * 1024L);
    LogSpoolerContext logSpoolerContext = mock(LogSpoolerContext.class);
    File activeSpoolFile = mock(File.class);
    expect(activeSpoolFile.length()).andReturn(10*1024*1024L);
    expect(logSpoolerContext.getActiveSpoolFile()).andReturn(activeSpoolFile);
    replay(logSpoolerContext, activeSpoolFile);

    OutputS3File outputS3File = new OutputS3File();
    configMap.put(S3OutputConfiguration.ROLLOVER_SIZE_THRESHOLD_BYTES_KEY, thresholdSize);
    outputS3File.loadConfig(configMap);
    outputS3File.init();

    assertFalse(outputS3File.shouldRollover(logSpoolerContext));
  }

  @Test
  public void shouldUploadFileOnRollover() throws Exception {
    Input input = mock(Input.class);
    InputMarker inputMarker = new InputMarker(input, null, 0);
    expect(input.getFilePath()).andReturn("/var/log/hdfs-namenode.log");
    expect(input.getStringValue(OutputS3File.INPUT_ATTRIBUTE_TYPE)).andReturn("hdfs-namenode");
    final LogSpooler spooler = mock(LogSpooler.class);
    spooler.add("log event block1");
    final S3Uploader s3Uploader = mock(S3Uploader.class);
    s3Uploader.addFileForUpload("/var/ambari-logsearch/logfeeder/hdfs-namenode.log.gz");
    replay(input, spooler, s3Uploader);

    OutputS3File outputS3File = new OutputS3File() {
      @Override
      protected LogSpooler createSpooler(String filePath) {
        return spooler;
      }
      @Override
      protected S3Uploader createUploader(String logType) {
        return s3Uploader;
      }
    };
    outputS3File.write("log event block1", inputMarker);
    outputS3File.handleRollover(new File("/var/ambari-logsearch/logfeeder/hdfs-namenode.log.gz"));

    verify(s3Uploader);
  }
}
