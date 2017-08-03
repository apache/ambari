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
package org.apache.ambari.server.utils;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceFilesKeeperHelper {

  private static final String HASH_SUM_FILE=".hash";
  private static final String ARCHIVE_NAME="archive.zip";
  private static final String PYC_EXT=".pyc";

  private static int BUFFER_SIZE = 1024 * 32;

  private static final Logger LOG = LoggerFactory.getLogger
    (ResourceFilesKeeperHelper.class);

  /**
   * Refresh directory hash and archive if the contents of the directory have changed
   * @param directory   the directory whose archive needs to be updated
   * @param noZip       If set, updates only the hash file and skips creating the archive file
   */
  public static void updateDirectoryArchive(String directory, boolean noZip) {
    boolean skipEmptyDirectory = true;
    File dir = new File(directory);

    String newHash = calcHashSum(directory);
    String oldHash = readHashSum(directory);

    LOG.info("Directory {} :: oldHash = {}, newHash = {}", directory, oldHash, newHash);

    if (!newHash.equals(oldHash)) {
      if (!noZip) {
        LOG.info("Creating archive for directory " + directory);
        zipDirectory(directory, skipEmptyDirectory);
      }
      if (skipEmptyDirectory && (!dir.exists() && dir.listFiles().length == 0)) {
        LOG.info("Empty directory. Skipping generation of hash file for " + directory);
      } else {
        writeHashSum(directory, newHash);
      }
    } else if (!dir.isFile()) {
      zipDirectory(directory, skipEmptyDirectory);
    }
  }

  /**
   * Calculate hash on the specified directory
   * @param directory the directory for which hash needs to be calculated
   * @return  calculated hash sum
   */
  private static String calcHashSum(String directory) {

    File folder = new File(directory);
    if (!folder.isDirectory()) {
      return "";
    }
    Collection<File> listOfFilesInFolder = FileUtils.listFiles(folder, null, true);
    List<String> sortedListOfFilePaths = new ArrayList<>();
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
    }

    for (File file : listOfFilesInFolder) {
      if (!isFileIgnored(file)) {
        sortedListOfFilePaths.add(file.getPath());
      }
    }
    Collections.sort(sortedListOfFilePaths);
    for (String filePath : sortedListOfFilePaths) {
      InputStream fis = null;
      try {
        fis = new FileInputStream(filePath);
        int n = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        while (n != -1) {
          n = fis.read(buffer);
          if (n > 0) {
            digest.update(buffer, 0, n);
          }
        }
      } catch (FileNotFoundException e) {
      } catch (IOException e) {
      } finally {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }

    }
    return new String(Hex.encodeHex(digest.digest()));
  }

  /**
   * Read hash value from the hash file in the specified directory
   * @param directory the directory from where to read the hash value
   * @return  the hash value read from the hash file
   */
  private static String readHashSum(String directory) {
    String pathToHash = directory + File.separator + HASH_SUM_FILE;
    File hashFile = new File(pathToHash);
    String hash = "";

    if (hashFile.exists() && !hashFile.isDirectory()) {
      InputStream fis = null;
      try {
        fis = new FileInputStream(pathToHash);
        DataInputStream in = new DataInputStream(fis);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        hash = br.readLine();
      } catch (FileNotFoundException e) {
      } catch (IOException e) {
      } finally {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
    return hash == null ? "" : hash.trim();
  }

  /**
   * Write hash value to the hash file in the specified directory
   * @param directory directory with corresponding hash
   * @param hash  hash value to be written
   */
  private static void writeHashSum(String directory, String hash) {

    String hashFilePath = directory + File.separator + HASH_SUM_FILE;

    PrintWriter out = null;
    try {
      out = new PrintWriter(hashFilePath);
      out.print(hash);

      File hashFile = new File(hashFilePath);
      hashFile.setExecutable(true, false);
      hashFile.setReadable(true, false);
      hashFile.setWritable(true, true);
    } catch (FileNotFoundException e) {
    } finally {
      out.close();
    }
  }

  /**
   * Create archive zip file for specified directory
   * @param directory   the directory that needs to be archived
   * @param skipEmptyDirectory  If set skips creating archives for empty directories
   */
  private static void zipDirectory(String directory, boolean skipEmptyDirectory) {
    File dirFile = new File(directory);
    if (!dirFile.exists() || (skipEmptyDirectory && dirFile.listFiles().length == 0)) {
      LOG.info("Empty or non existing directory. Skipping archive creation for " + directory);
      return;
    }

    String zipFilePath = directory + File.separator + ARCHIVE_NAME;
    Collection<File> filesInDirectory = FileUtils.listFiles(dirFile, null, true);

    ZipOutputStream zos = null;
    FileOutputStream fos = null;
    try {
      byte[] buffer = new byte[1024];

      fos = new FileOutputStream(zipFilePath);
      zos = new ZipOutputStream(fos);

      for (File file : filesInDirectory) {
        if (!isFileIgnored(file)) {
          FileInputStream fis = null;
          try{
            fis = new FileInputStream(file);

            // begin writing a new ZIP entry, positions the stream to the start of the entry data
            String filePathInZip = file.getPath().replaceAll(dirFile.getPath(), "");
            filePathInZip = filePathInZip.startsWith(File.separator) ?
                    filePathInZip.replaceFirst(File.separator, "") : filePathInZip;
            zos.putNextEntry(new ZipEntry(filePathInZip));

            int length;
            while ((length = fis.read(buffer)) > 0) {
              zos.write(buffer, 0, length);
            }

            zos.closeEntry();
          } finally {
            if (fis != null) {
              // close the InputStream
              fis.close();
            }
          }

        }
      }

    }
    catch (IOException e) {
    }
    finally {
      try {
        if (zos != null) {
          zos.close();
        }
        if (fos != null) {
          fos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    File archiveFile = new File(zipFilePath);
    archiveFile.setExecutable(true, false);
    archiveFile.setReadable(true, false);
    archiveFile.setWritable(true, true);
  }

  /**
   * Check if a file should be ignored when computed the hash for a directory
   * @param file  the file to be checked
   * @return  True if file should ignore, otherwise False
   */
  private static boolean isFileIgnored(File file) {
    String fileName = file.getName();
    return (fileName.equals(HASH_SUM_FILE) || fileName.equals(ARCHIVE_NAME) || fileName.endsWith(PYC_EXT) || file.isDirectory());
  }
}
