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

package org.apache.ambari.server.security.encryption;

import org.apache.ambari.server.AmbariException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.KeyStore;

/**
 * FileBasedCredentialStoreService is a CredentialStoreService implementation that creates and manages
 * a JCEKS (Java Cryptography Extension KeyStore) file on disk.  The key store and its contents are
 * encrypted using the key from the supplied {@link MasterKeyService}.
 * <p/>
 * Most of the work for this implementation is handled by the {@link CredentialStoreServiceImpl}.
 * This class handles the details of the storage location and associated input and output streams.
 */
public class FileBasedCredentialStoreService extends CredentialStoreServiceImpl {
  private static final String KEYSTORE_FILENAME = "credentials.jceks";
  private static final Logger LOG = LoggerFactory.getLogger(FileBasedCredentialStoreService.class);

  /**
   * The directory to use for storing the key store file
   */
  private File keyStoreDir;

  /**
   * Constructs a new FileBasedCredentialStoreService using the specified key store directory
   *
   * @param keyStoreDir a String containing the absolute path to the directory in which to store the key store file
   */
  public FileBasedCredentialStoreService(String keyStoreDir) {
    this(new File(keyStoreDir));
  }

  /**
   * Constructs a new FileBasedCredentialStoreService using the specified key store directory
   *
   * @param keyStoreDir a File pointing to the directory in which to store the key store file
   */
  public FileBasedCredentialStoreService(File keyStoreDir) {
    if (keyStoreDir == null) {
      LOG.warn("Writing key store to the current working directory of the running process");
    } else if (!keyStoreDir.exists()) {
      LOG.warn("The destination directory does not exist. Failures may occur when writing the key store to disk: {}", keyStoreDir.getAbsolutePath());
    } else if (!keyStoreDir.isDirectory()) {
      LOG.warn("The destination does not point to directory. Failures may occur when writing the key store to disk: {}", keyStoreDir.getAbsolutePath());
    }

    this.keyStoreDir = keyStoreDir;
  }

  @Override
  protected void persistCredentialStore(KeyStore keyStore) throws AmbariException {
    putKeyStore(keyStore, getKeyStoreFile());
  }


  @Override
  protected KeyStore loadCredentialStore() throws AmbariException {
    return getKeyStore(getKeyStoreFile(), DEFAULT_STORE_TYPE);
  }

  /**
   * Reads the key store data from the specified file. If the file does not exist, a new KeyStore
   * will be created.
   *
   * @param keyStoreFile a File pointing to the key store file
   * @param keyStoreType the type of key store data to read (or create)
   * @return the loaded KeyStore
   * @throws AmbariException           if the Master Key Service is not set
   * @see CredentialStoreServiceImpl#loadCredentialStore()
   */
  private KeyStore getKeyStore(final File keyStoreFile, String keyStoreType) throws AmbariException{
    KeyStore keyStore;
    FileInputStream inputStream;

    if (keyStoreFile.exists()) {
      LOG.debug("Reading key store from {}", keyStoreFile.getAbsolutePath());
      try {
        inputStream = new FileInputStream(keyStoreFile);
      } catch (FileNotFoundException e) {
        throw new AmbariException(String.format("Failed to open the key store file: %s", e.getLocalizedMessage()), e);
      }
    } else {
      LOG.debug("Key store file not found in {}. Returning new (non-persisted) KeyStore", keyStoreFile.getAbsolutePath());
      inputStream = null;
    }

    try {
      keyStore = loadKeyStore(inputStream, keyStoreType);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return keyStore;
  }

  /**
   * Writes the specified KeyStore to a file.
   *
   * @param keyStore     the KeyStore to write to a file
   * @param keyStoreFile the File in which to store the KeyStore data
   * @throws AmbariException if an error occurs while writing the KeyStore data
   */
  private void putKeyStore(KeyStore keyStore, File keyStoreFile) throws AmbariException {
    LOG.debug("Writing key store to {}", keyStoreFile.getAbsolutePath());

    FileOutputStream outputStream = null;

    try {
      outputStream = new FileOutputStream(new File(keyStoreDir, KEYSTORE_FILENAME));
      writeKeyStore(keyStore, outputStream);
    } catch (FileNotFoundException e) {
      throw new AmbariException(String.format("Failed to open the key store file: %s", e.getLocalizedMessage()), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  /**
   * Calculates the absolute path to the key store file
   *
   * @return a File pointing to the absolute path of the key store file
   */
  private File getKeyStoreFile() {
    return new File(keyStoreDir, KEYSTORE_FILENAME);
  }
}
