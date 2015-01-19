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

package org.apache.ambari.server.serveraction.kerberos;

import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KerberosOperationHandler is an abstract class providing basic implementations of common Kerberos
 * operations (like generating secure passwords) and placeholders for KDC-specific operations
 * (such as creating principals).
 */
public abstract class KerberosOperationHandler {
  private final static Logger LOG = LoggerFactory.getLogger(KerberosOperationHandler.class);

  private final static SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * The number of characters to generate for a secure password
   */
  protected final static int SECURE_PASSWORD_LENGTH = 18;

  /**
   * The set of available characters to use when generating a secure password
   */
  private final static char[] SECURE_PASSWORD_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890?.!$%^*()-_+=~".toCharArray();


  /**
   * The default set of ciphers to use for creating keytab entries
   */
  private static final Set<EncryptionType> DEFAULT_CIPHERS = Collections.unmodifiableSet(
      new HashSet<EncryptionType>() {{
        add(EncryptionType.DES_CBC_MD5);
        add(EncryptionType.DES3_CBC_SHA1_KD);
        add(EncryptionType.RC4_HMAC);
        add(EncryptionType.AES128_CTS_HMAC_SHA1_96);
        add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
      }});

  private KerberosCredential administratorCredentials = null;
  private String defaultRealm = null;
  private boolean open = false;

  /**
   * Create a secure (random) password using a secure random number generator and a set of (reasonable)
   * characters.
   *
   * @return a String containing the new password
   */
  public String createSecurePassword() {
    return createSecurePassword(SECURE_PASSWORD_LENGTH);
  }

  /**
   * Create a secure (random) password using a secure random number generator and a set of (reasonable)
   * characters.
   *
   * @param length an integer value declaring the length of the password to create,
   *               if <1, a default will be used.
   * @return a String containing the new password
   */
  public String createSecurePassword(int length) {
    StringBuilder passwordBuilder;

    // If the supplied length is less than 1 use the default value.
    if (length < 1) {
      length = SECURE_PASSWORD_LENGTH;
    }

    // Create a new StringBuilder and ensure its capacity is set for the length of the password to
    // be generated
    passwordBuilder = new StringBuilder(length);

    // For each character to be added to the password, (securely) generate a random number to pull
    // a random character from the character array
    for (int i = 0; i < length; i++) {
      passwordBuilder.append(SECURE_PASSWORD_CHARS[SECURE_RANDOM.nextInt(SECURE_PASSWORD_CHARS.length)]);
    }

    return passwordBuilder.toString();
  }

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler.
   * Implementation in this class is ignoring parameters ldapUrl and principalContainerDn and delegate to
   * <code>open(KerberosCredential administratorCredentials, String defaultRealm)</code>
   * Subclasses that want to use these parameters need to override this method.
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   *
   * @param administratorCredentials a KerberosCredential containing the administrative credentials
   *                                 for the relevant KDC
   * @param defaultRealm             a String declaring the default Kerberos realm (or domain)
   * @param kerberosConfiguration    a Map of key/value pairs containing data from the kerberos-env configuration set
   */
  public abstract void open(KerberosCredential administratorCredentials, String defaultRealm, Map<String, String> kerberosConfiguration)
      throws KerberosOperationException;

  /**
   * Closes and cleans up any resources used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used after this call.
   */
  public abstract void close()
      throws KerberosOperationException;

  /**
   * Test to see if the specified principal exists in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to test
   * @return true if the principal exists; false otherwise
   * @throws KerberosOperationException
   */
  public abstract boolean principalExists(String principal)
      throws KerberosOperationException;

  /**
   * Creates a new principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to add
   * @param password  a String containing the password to use when creating the principal
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosOperationException
   */
  public abstract Integer createPrincipal(String principal, String password, boolean service)
      throws KerberosOperationException;

  /**
   * Updates the password for an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @return an Integer declaring the new key number
   * @throws KerberosOperationException
   */
  public abstract Integer setPrincipalPassword(String principal, String password)
      throws KerberosOperationException;

  /**
   * Removes an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to remove
   * @return true if the principal was successfully removed; otherwise false
   * @throws KerberosOperationException
   */
  public abstract boolean removePrincipal(String principal)
      throws KerberosOperationException;

  /**
   * Tests to ensure the connection information and credentials allow for administrative
   * connectivity to the KDC
   *
   * @return true of successful; otherwise false
   * @throws KerberosOperationException if a failure occurs while testing the
   *                                    administrator credentials
   */
  public boolean testAdministratorCredentials() throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    KerberosCredential credentials = getAdministratorCredentials();
    if (credentials == null) {
      throw new KerberosOperationException("Missing KDC administrator credentials");
    } else {
      return principalExists(credentials.getPrincipal());
    }
  }

  /**
   * Create or append to a keytab file using the specified principal and password.
   *
   * @param principal  a String containing the principal to test
   * @param password   a String containing the password to use when creating the principal
   * @param keytabFile a File containing the absolute path to the keytab file
   * @return true if the keytab file was successfully created; false otherwise
   * @throws KerberosOperationException
   */
  public boolean createKeytabFile(String principal, String password, Integer keyNumber, File keytabFile)
      throws KerberosOperationException {
    boolean success = false;

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to create keytab file, missing principal");
    } else if (password == null) {
      throw new KerberosOperationException(String.format("Failed to create keytab file for %s, missing password", principal));
    } else if (keytabFile == null) {
      throw new KerberosOperationException(String.format("Failed to create keytab file for %s, missing file path", principal));
    } else {
      Keytab keytab;
      Set<EncryptionType> ciphers = new HashSet<EncryptionType>(DEFAULT_CIPHERS);
      List<KeytabEntry> keytabEntries = new ArrayList<KeytabEntry>();

      if (keytabFile.exists() && keytabFile.canRead() && (keytabFile.length() > 0)) {
        // If the keytab file already exists, read it in and append the new keytabs to it so that
        // potentially important data is not lost
        try {
          keytab = Keytab.read(keytabFile);
        } catch (IOException e) {
          // There was an issue reading in the existing keytab file... we might loose some keytabs
          // but that is unlikely...
          keytab = new Keytab();
        }

        // In case there were any existing keytab entries, add them to the new entries list so
        // they are not lost.  While at it, remove ciphers that already exist for the given principal
        // so duplicate entries aren't added to the file.
        List<KeytabEntry> existingEntries = keytab.getEntries();
        if ((existingEntries != null) && !existingEntries.isEmpty()) {

          for (KeytabEntry entry : existingEntries) {
            // Remove ciphers that will cause duplicate entries
            if (principal.equals(entry.getPrincipalName())) {
              ciphers.remove(entry.getKey().getKeyType());
            }

            keytabEntries.add(entry);
          }
        }
      } else {
        keytab = new Keytab();
      }

      if (ciphers.isEmpty()) {
        // There are no new keys to create
        success = true;
      } else {
        // Create a set of keys and relevant keytab entries
        Map<EncryptionType, EncryptionKey> keys = KerberosKeyFactory.getKerberosKeys(principal, password, ciphers);

        if (keys != null) {
          byte keyVersion = (keyNumber == null) ? 0 : keyNumber.byteValue();
          KerberosTime timestamp = new KerberosTime();

          for (EncryptionKey encryptionKey : keys.values()) {
            keytabEntries.add(new KeytabEntry(principal, 1, timestamp, keyVersion, encryptionKey));
          }

          keytab.setEntries(keytabEntries);

          try {
            keytab.write(keytabFile);
            success = true;
          } catch (IOException e) {
            String message = String.format("Failed to export keytab file for %s", principal);
            LOG.error(message, e);

            if (!keytabFile.delete()) {
              keytabFile.deleteOnExit();
            }

            throw new KerberosOperationException(message, e);
          }
        }
      }
    }

    return success;
  }

  public KerberosCredential getAdministratorCredentials() {
    return administratorCredentials;
  }

  public void setAdministratorCredentials(KerberosCredential administratorCredentials) {
    this.administratorCredentials = administratorCredentials;
  }

  public String getDefaultRealm() {
    return defaultRealm;
  }

  public void setDefaultRealm(String defaultRealm) {
    this.defaultRealm = defaultRealm;
  }

  /**
   * Test this KerberosOperationHandler to see whether is was previously open or not
   *
   * @return a boolean value indicating whether this KerberosOperationHandler was open (true) or not (false)
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Sets whether this KerberosOperationHandler is open or not.
   *
   * @param open a boolean value indicating whether this KerberosOperationHandler was open (true) or not (false)
   */
  public void setOpen(boolean open) {
    this.open = open;
  }

  /**
   * Given base64-encoded keytab data, decode the String to binary data and write it to a (temporary)
   * file.
   * <p/>
   * Upon success, a new file is created.  The caller is expected to clean up this file when done
   * with it.
   *
   * @param keytabData a String containing base64-encoded keytab data
   * @return a File pointing to the decoded keytab file or null if not successful
   * @throws KerberosOperationException
   */
  protected File createKeytabFile(String keytabData)
      throws KerberosOperationException {
    boolean success = false;
    File tempFile = null;

    // Create a temporary file
    try {
      tempFile = File.createTempFile("temp", ".dat");
    } catch (IOException e) {
      LOG.error(String.format("Failed to create temporary keytab file: %s", e.getLocalizedMessage()), e);
    }

    if ((tempFile != null) && (keytabData != null)) {
      OutputStream fos = null;

      // Decoded the base64-encoded String and write it to the temporary file
      try {
        fos = new FileOutputStream(tempFile);
        fos.write(Base64.decodeBase64(keytabData));
        success = true;
      } catch (FileNotFoundException e) {
        String message = String.format("Failed to write to temporary keytab file %s: %s",
            tempFile.getAbsolutePath(), e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } catch (IOException e) {
        String message = String.format("Failed to write to temporary keytab file %s: %s",
            tempFile.getAbsolutePath(), e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } finally {
        if (fos != null) {
          try {
            fos.close();
          } catch (IOException e) {
            // Ignore this...
          }
        }

        // If there was an issue, clean up the file
        if (!success) {
          if (!tempFile.delete()) {
            tempFile.deleteOnExit();
          }

          tempFile = null;
        }
      }
    }

    return tempFile;
  }

  /**
   * Executes a shell command.
   * <p/>
   * See {@link org.apache.ambari.server.utils.ShellCommandUtil#runCommand(String[])}
   *
   * @param command an array of String value representing the command and its arguments
   * @return a ShellCommandUtil.Result declaring the result of the operation
   * @throws KerberosOperationException
   */
  protected ShellCommandUtil.Result executeCommand(String[] command)
      throws KerberosOperationException {

    if ((command == null) || (command.length == 0)) {
      return null;
    } else {
      try {
        return ShellCommandUtil.runCommand(command);
      } catch (IOException e) {
        String message = String.format("Failed to execute the command: %s", e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } catch (InterruptedException e) {
        String message = String.format("Failed to wait for the command to complete: %s", e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      }
    }
  }
}
