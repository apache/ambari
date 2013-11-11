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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.TimeStamp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MasterKeyServiceImpl implements MasterKeyService {
  private static Log LOG = LogFactory.getLog(MasterKeyServiceImpl.class);
  private static final String MASTER_PERSISTENCE_TAG = "#1.0# " +
    TimeStamp.getCurrentTime().toDateString();
  private char[] master = null;
  private String MASTER_PASSPHRASE = "masterpassphrase";
  private AESEncryptor aes = new AESEncryptor(MASTER_PASSPHRASE);

  // TODO: Create static factory methods vs constructors

  /**
   * Test/console friendly construction
   * @param masterKey
   * @param masterFileLocation
   * @param persistMaster
   */
  public MasterKeyServiceImpl(String masterKey, String masterFileLocation,
                              boolean persistMaster) {
    this.master = masterKey.toCharArray();
    if (masterFileLocation != null) {
      if (persistMaster) {
        LOG.debug("Persisting master key file.");
        File masterFile = new File(masterFileLocation);
        if (masterFile.exists()) {
          LOG.info("Resetting master key before persist.");
          try {
            PrintWriter pw = new PrintWriter(masterFile);
            pw.print("");
            pw.close();
          } catch (FileNotFoundException e) {
            LOG.error("Cannot reset master key file located at: " +
              masterFileLocation);
            e.printStackTrace();
          }
        }
        persistMaster(masterFile);
      }
    } else {
      if (persistMaster) {
        LOG.error("Cannot persist master key without specifying master key " +
          "location.");
      }
    }
  }

  /**
   * Construction - post creation of the key
   * @param masterFileLocation
   * @param isPersisted
   */
  public MasterKeyServiceImpl(String masterFileLocation, boolean isPersisted) {
    if (masterFileLocation == null || masterFileLocation.isEmpty())
      throw new IllegalArgumentException("Master Key location not provided.");
    if (isPersisted) {
      File masterFile = new File(masterFileLocation);
      if (masterFile.exists()) {
        try {
          initializeFromFile(masterFile);
        } catch (Exception ex) {
          LOG.error("Cannot intitialize master key from file: " +
            masterFileLocation + "\n" + ex);
        }
      } else {
        LOG.error("Cannot find master key at specified location " +
          masterFileLocation);
      }
    } else {
      // Master key is not persisted, read from environment.
      String key = readMasterKey();
      if (key != null) {
        this.master = key.toCharArray();
      } else {
        LOG.debug("Master key is not provided as a System property or an " +
          "environment varialble.");
      }
    }
  }

  /**
   * Construction for Non-persisted master key
   * @param masterKey
   */
  public MasterKeyServiceImpl(String masterKey) {
    if (masterKey != null) {
      this.master = masterKey.toCharArray();
    } else {
      throw new IllegalArgumentException("Master key cannot be null");
    }
  }

  /**
   * Construction for Non-persisted master key from environment
   *
   */
  public MasterKeyServiceImpl() {
    String key = readMasterKey();
    if (key == null) {
      throw new IllegalStateException("Cannot read master key from " +
        "environment.");
    } else
      this.master = key.toCharArray();
  }

  private String readMasterKey() {
    String key = null;
    Map<String, String> envVariables = System.getenv();
    if (envVariables != null && !envVariables.isEmpty()) {
      key = envVariables.get(Configuration.MASTER_KEY_ENV_PROP);
      if (key == null || key.isEmpty()) {
        String keyPath = envVariables.get(Configuration.MASTER_KEY_LOCATION);
        if (keyPath != null && !keyPath.isEmpty()) {
          File keyFile = new File(keyPath);
          if (keyFile.exists()) {
            try {
              initializeFromFile(keyFile);
              if (this.master != null)
                key = new String(this.master);
              FileUtils.deleteQuietly(keyFile);
            } catch (IOException e) {
              LOG.error("Cannot read master key from file: " + keyPath);
              e.printStackTrace();
            } catch (Exception e) {
              LOG.error("Cannot read master key from file: " + keyPath);
              e.printStackTrace();
            }
          }
        }
      }
    }
    return key;
  }

  public boolean isMasterKeyInitialized() {
    return this.master != null;
  }

  private EncryptionResult encryptMaster(char[] master) {
    try {
      return aes.encrypt(new String(master));
    } catch (Exception e) {
      // TODO log failed encryption attempt
      // need to ensure that we don't persist now
      e.printStackTrace();
    }
    return null;
  }

  private void persistMaster(File masterFile) {
    EncryptionResult atom = encryptMaster(master);
    try {
      ArrayList<String> lines = new ArrayList<String>();
      lines.add(MASTER_PERSISTENCE_TAG);

      String line = Base64.encodeBase64String((
        Base64.encodeBase64String(atom.salt) + "::" +
          Base64.encodeBase64String(atom.iv) + "::" +
          Base64.encodeBase64String(atom.cipher)).getBytes("UTF8"));
      lines.add(line);
      FileUtils.writeLines(masterFile, "UTF8", lines);

      // restrict os permissions to only the user running this process
      chmod("600", masterFile);
    } catch (IOException e) {
      LOG.error("Failed to persist master. " + e.getLocalizedMessage());
      e.printStackTrace();
    }
  }

  private void initializeFromFile(File masterFile) throws Exception {
    try {
      List<String> lines = FileUtils.readLines(masterFile, "UTF8");
      String tag = lines.get(0);
      LOG.info("Loading from persistent master: " + tag);
      String line = new String(Base64.decodeBase64(lines.get(1)));
      String[] parts = line.split("::");
      this.master = new String(aes.decrypt(Base64.decodeBase64(parts[0]),
        Base64.decodeBase64(parts[1]), Base64.decodeBase64(parts[2])),
        "UTF8").toCharArray();
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public char[] getMasterSecret() {
    return this.master;
  }

  private void chmod(String args, File file) throws IOException {
    if (isUnixEnv()) {
      //args and file should never be null.
      if (args == null || file == null)
        throw new IOException("nullArg");
      if (!file.exists())
        throw new IOException("fileNotFound");

      // " +" regular expression for 1 or more spaces
      final String[] argsString = args.split(" +");
      List<String> cmdList = new ArrayList<String>();
      cmdList.add("/bin/chmod");
      cmdList.addAll(Arrays.asList(argsString));
      cmdList.add(file.getAbsolutePath());
      new ProcessBuilder(cmdList).start();
    }
  }

  private boolean isUnixEnv() {
    return (File.separatorChar == '/');
  }

  private String generateMasterKey() {
    char[] chars = { 'a', 'b', 'c', 'd', 'e', 'f', 'g',
      'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
      'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
      'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      '2', '3', '4', '5', '6', '7', '8', '9'};

    StringBuffer sb = new StringBuffer();
    Random r = new Random();
    for (int i = 0; i < chars.length; i++) {
      sb.append(chars[r.nextInt(chars.length)]);
    }
    return sb.toString();
  }

  public static void main(String args[]) {
    String masterKey = "ThisissomeSecretPassPhrasse";
    String masterKeyLocation = "/var/lib/ambari-server/keys/master";
    boolean persistMasterKey = false;
    if (args != null && args.length > 0) {
      masterKey = args[0];
      if (args.length > 1)
        masterKeyLocation = args[1];
      if (args.length > 2 && !args[2].isEmpty())
        persistMasterKey = args[2].toLowerCase().equals("true");
    }
    MasterKeyService masterKeyService = new MasterKeyServiceImpl
      (masterKey, masterKeyLocation, persistMasterKey);
    if (!masterKeyService.isMasterKeyInitialized()) {
      System.exit(1);
    }
    System.exit(0);
  }
}