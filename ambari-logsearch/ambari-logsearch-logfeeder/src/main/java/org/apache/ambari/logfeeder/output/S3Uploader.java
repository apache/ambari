/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.common.annotations.VisibleForTesting;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.util.CompressionUtil;
import org.apache.ambari.logfeeder.util.S3Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that handles the uploading of files to S3.
 *
 * This class can be used to upload a file one time, or start a daemon thread that can
 * be used to upload files added to a queue one after the other. When used to upload
 * files via a queue, one instance of this class is created for each file handled in
 * {@link org.apache.ambari.logfeeder.input.InputFile}.
 */
public class S3Uploader implements Runnable {
  private static final Logger LOG = Logger.getLogger(S3Uploader.class);
  
  public static final String POISON_PILL = "POISON-PILL";

  private final S3OutputConfiguration s3OutputConfiguration;
  private final boolean deleteOnEnd;
  private final String logType;
  private final BlockingQueue<String> fileContextsToUpload;
  private final AtomicBoolean stopRunningThread = new AtomicBoolean(false);

  public S3Uploader(S3OutputConfiguration s3OutputConfiguration, boolean deleteOnEnd, String logType) {
    this.s3OutputConfiguration = s3OutputConfiguration;
    this.deleteOnEnd = deleteOnEnd;
    this.logType = logType;
    this.fileContextsToUpload = new LinkedBlockingQueue<>();
  }

  /**
   * Starts a thread that can be used to upload files from a queue.
   *
   * Add files to be uploaded using the method {@link #addFileForUpload(String)}.
   * If this thread is started, it must be stopped using the method {@link #stopUploaderThread()}.
   */
  void startUploaderThread() {
    Thread s3UploaderThread = new Thread(this, "s3-uploader-thread-"+logType);
    s3UploaderThread.setDaemon(true);
    s3UploaderThread.start();
  }

  /**
   * Stops the thread used to upload files from a queue.
   *
   * This method must be called to cleanly free up resources, typically on shutdown of the process.
   * Note that this method does not drain any remaining files, and instead stops the thread
   * as soon as any file being currently uploaded is complete.
   */
  void stopUploaderThread() {
    stopRunningThread.set(true);
    boolean offerStatus = fileContextsToUpload.offer(POISON_PILL);
    if (!offerStatus) {
      LOG.warn("Could not add poison pill to interrupt uploader thread.");
    }
  }

  /**
   * Add a file to a queue to upload asynchronously.
   * @param fileToUpload Full path to the local file which must be uploaded.
   */
  void addFileForUpload(String fileToUpload) {
    boolean offerStatus = fileContextsToUpload.offer(fileToUpload);
    if (!offerStatus) {
      LOG.error("Could not add file " + fileToUpload + " for upload.");
    }
  }

  @Override
  public void run() {
    while (!stopRunningThread.get()) {
      try {
        String fileNameToUpload = fileContextsToUpload.take();
        if (POISON_PILL.equals(fileNameToUpload)) {
          LOG.warn("Found poison pill while waiting for files to upload, exiting");
          return;
        }
        uploadFile(new File(fileNameToUpload), logType);
      } catch (InterruptedException e) {
        LOG.error("Interrupted while waiting for elements from fileContextsToUpload", e);
        return;
      }
    }
  }

  /**
   * Upload the given file to S3.
   *
   * The file which should be available locally, is first compressed using the compression
   * method specified by {@link S3OutputConfiguration#getCompressionAlgo()}. This compressed
   * file is what is uploaded to S3.
   * @param fileToUpload the file to upload
   * @param logType the name of the log which is used in the S3 path constructed.
   * @return
   */
  String uploadFile(File fileToUpload, String logType) {
    String bucketName = s3OutputConfiguration.getS3BucketName();
    String s3AccessKey = s3OutputConfiguration.getS3AccessKey();
    String s3SecretKey = s3OutputConfiguration.getS3SecretKey();
    String compressionAlgo = s3OutputConfiguration.getCompressionAlgo();

    String keySuffix = fileToUpload.getName() + "." + compressionAlgo;
    String s3Path = new S3LogPathResolver().getResolvedPath(
        s3OutputConfiguration.getS3Path() + LogFeederConstants.S3_PATH_SEPARATOR + logType, keySuffix,
        s3OutputConfiguration.getCluster());
    LOG.info(String.format("keyPrefix=%s, keySuffix=%s, s3Path=%s", s3OutputConfiguration.getS3Path(), keySuffix, s3Path));
    File sourceFile = createCompressedFileForUpload(fileToUpload, compressionAlgo);

    LOG.info("Starting S3 upload " + sourceFile + " -> " + bucketName + ", " + s3Path);
    uploadFileToS3(bucketName, s3Path, sourceFile, s3AccessKey, s3SecretKey);

    // delete local compressed file
    sourceFile.delete();
    if (deleteOnEnd) {
      LOG.info("Deleting input file as required");
      if (!fileToUpload.delete()) {
        LOG.error("Could not delete file " + fileToUpload.getAbsolutePath() + " after upload to S3");
      }
    }
    return s3Path;
  }

  @VisibleForTesting
  protected void uploadFileToS3(String bucketName, String s3Key, File localFile, String accessKey, String secretKey) {
    TransferManager transferManager = S3Util.getTransferManager(accessKey, secretKey);
    try {
      Upload upload = transferManager.upload(bucketName, s3Key, localFile);
      upload.waitForUploadResult();
    } catch (AmazonClientException | InterruptedException e) {
      LOG.error("s3 uploading failed for file :" + localFile.getAbsolutePath(), e);
    } finally {
      S3Util.shutdownTransferManager(transferManager);
    }
  }

  @VisibleForTesting
  protected File createCompressedFileForUpload(File fileToUpload, String compressionAlgo) {
    File outputFile = new File(fileToUpload.getParent(), fileToUpload.getName() + "_" + new Date().getTime() +
        "." + compressionAlgo);
    outputFile = CompressionUtil.compressFile(fileToUpload, outputFile, compressionAlgo);
    return outputFile;
  }
}
