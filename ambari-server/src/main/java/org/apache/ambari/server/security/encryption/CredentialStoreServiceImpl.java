/**
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class CredentialStoreServiceImpl implements CredentialStoreService {
  private static final String CREDENTIALS_SUFFIX = "credentials.jceks";
  private static final String DEFAULT_STORE_TYPE = "JCEKS";
  private MasterKeyService masterService;
  private String keyStoreDir;
  private static volatile boolean isCredentialStoreCreated = false;
  static final Logger LOG = LoggerFactory.getLogger
    (CredentialStoreServiceImpl.class);

  public CredentialStoreServiceImpl(String keyStoreDir) {
    this.keyStoreDir = keyStoreDir;
    final File keyStoreFile = new File(keyStoreDir + File.separator +
      CREDENTIALS_SUFFIX);
    if (keyStoreFile.exists()) {
      isCredentialStoreCreated = true;
    }
  }

  protected KeyStore loadCredentialStore() throws AmbariException {
    if (masterService == null)
      throw new AmbariException("Master Key Service is not set for this " +
        "Credential store.");

    final File keyStoreFile = new File(keyStoreDir + File.separator +
      CREDENTIALS_SUFFIX);
    LOG.debug("keystoreFile => " + keyStoreFile.getAbsolutePath());
    if (!isCredentialStoreCreated) {
      createCredentialStore();
    }
    return getKeystore(keyStoreFile, DEFAULT_STORE_TYPE);
  }

  @Override
  public void addCredential(String alias, String value) throws
    AmbariException {
    KeyStore ks = loadCredentialStore();
    if (ks != null) {
      try {
        final Key key = new SecretKeySpec(value.getBytes("UTF8"), "AES");
        ks.setKeyEntry( alias, key, masterService.getMasterSecret(), null);
        final File  keyStoreFile = new File(keyStoreDir + File.separator +
          CREDENTIALS_SUFFIX);
        writeKeystoreToFile(ks, keyStoreFile);
      } catch (KeyStoreException e) {
        e.printStackTrace();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      } catch (CertificateException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public char[] getCredential(String alias) throws AmbariException {
    char[] credential = null;
    KeyStore ks = loadCredentialStore();
    if (ks != null && alias != null && !alias.isEmpty()) {
      try {
        LOG.debug("keystore = " + ks.aliases());
        Key key = ks.getKey(alias, masterService.getMasterSecret());
        if (key == null) {
          throw new AmbariException("Credential not found for alias: " +
            alias);
        }
        credential = new String(key.getEncoded()).toCharArray();
      } catch (UnrecoverableKeyException e) {
        e.printStackTrace();
      } catch (KeyStoreException e) {
        e.printStackTrace();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
    }
    return credential;
  }

  public void writeKeystoreToFile(final KeyStore keyStore, final File file)
    throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    final FileOutputStream  out = new FileOutputStream(file);
    try {
      keyStore.store(out, masterService.getMasterSecret());
    }
    finally {
      out.close();
    }
  }

  private synchronized void createCredentialStore() {
    String filename = keyStoreDir + File.separator + CREDENTIALS_SUFFIX;
    createKeystore(filename, DEFAULT_STORE_TYPE);
    isCredentialStoreCreated = true;
  }

  private void createKeystore(String filename, String keystoreType) {
    try {
      FileOutputStream out = new FileOutputStream(filename);
      KeyStore ks = KeyStore.getInstance(keystoreType);
      ks.load(null, null);
      ks.store(out, masterService.getMasterSecret());
    } catch (KeyStoreException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (CertificateException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private KeyStore getKeystore(final File keyStoreFile, String storeType) {
    KeyStore credStore = null;
    try {
      credStore = loadKeyStore(keyStoreFile, masterService.getMasterSecret(), storeType);
    } catch (CertificateException e) {
      e.printStackTrace();
    } catch (KeyStoreException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return credStore;
  }

  private static KeyStore loadKeyStore(final File keyStoreFile,
          final char[] masterPassword, String storeType)
    throws CertificateException, IOException,
    KeyStoreException, NoSuchAlgorithmException {
    final KeyStore  keyStore = KeyStore.getInstance(storeType);
    if (keyStoreFile.exists()) {
      final FileInputStream input   = new FileInputStream(keyStoreFile);
      try {
        keyStore.load(input, masterPassword);
      }
      finally {
        input.close();
      }
    }
    else {
      keyStore.load(null, masterPassword);
    }

    return keyStore;
  }

  @Override
  public void setMasterKeyService(MasterKeyService masterService) {
    this.masterService = masterService;
  }
}