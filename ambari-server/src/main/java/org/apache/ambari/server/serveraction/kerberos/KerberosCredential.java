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
package org.apache.ambari.server.serveraction.kerberos;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.ambari.server.AmbariException;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

/**
 * KerberosCredential encapsulates data needed to authenticate an identity to a KDC.
 * <p/>
 * This class has the ability to encrypt and decrypt itself using the AES encryption algorithm.
 */
public class KerberosCredential {

  /**
   * A property name used to hold the KDC administrator's principal value.
   */
  public static final String KEY_NAME_PRINCIPAL = "principal";
  /**
   * A property name used to hold the KDC administrator's password value.
   */
  public static final String KEY_NAME_PASSWORD = "password";
  /**
   * A property name used to hold the KDC administrator's (base64-encoded) keytab
   * value.
   */
  public static final String KEY_NAME_KEYTAB = "keytab";

  /**
   * This principal value
   */
  private String principal = null;

  /**
   * The plaintext password value
   */
  private String password = null;

  /**
   * A base64-encoded keytab
   */
  private String keytab = null;

  /**
   * Given a Map of attributes, attempts to safely retrieve the data needed to create a
   * KerberosCredential representing a KDC administrator.
   * <p/>
   * It is expected that the following properties exist in the Map:
   * <ul>
   * <li>principal</li>
   * <li>password (optional)</li>
   * <li>keytab (optional)</li>
   * </ul>
   * <p/>
   * Each of these properties may be prefixed with some prefix value to generate a relevant key value.
   * If prefix was "kerberos_admin/", then the key representing the principal would be computed
   * to be "kerberos_admin/principal".
   *
   * @param map    a Map of attributes containing the values needed to create a new KerberosCredential
   * @param prefix a String containing the prefix to used along with the base key name (principal, etc...)
   *               to create the relevant key name ([prefix]base_key. etc...)
   * @return a KerberosCredential or null if commandParameters is null
   */
  public static KerberosCredential fromMap(Map<String, Object> map, String prefix) {
    KerberosCredential credential = null;

    if (map != null) {
      Object attribute;
      String principal;
      String password;
      String keytab;

      if (prefix == null) {
        prefix = "";
      }

      attribute = map.get(prefix + KEY_NAME_PRINCIPAL);
      principal = (attribute == null) ? null : attribute.toString();

      attribute = map.get(prefix + KEY_NAME_PASSWORD);
      password = (attribute == null) ? null : attribute.toString();

      attribute = map.get(prefix + KEY_NAME_KEYTAB);
      keytab = (attribute == null) ? null : attribute.toString();

      if (((principal != null) && !principal.isEmpty()) ||
          ((password != null) && !password.isEmpty()) ||
          ((keytab != null) && !keytab.isEmpty())) {
        credential = new KerberosCredential(principal, password, keytab);
      }
    }

    return credential;
  }

  /**
   * Decrypts a String containing base64-encoded encrypted data into a new KerberosCredential.
   * <p/>
   * Given a key and a base64-encoded set of bytes containing encrypted data (ideally obtained from
   * {@link #encrypt(KerberosCredential, byte[])} or {@link #encrypt(byte[])}, decodes and decrypts
   * into a new KerberosCredential.
   *
   * @param cipherText a String containing base64-encoded encrypted data
   * @param key        an array of bytes used to decrypt the encrypted data
   * @return a new KerberosCredential
   * @throws AmbariException if an error occurs while decrypting the data
   */
  public static KerberosCredential decrypt(String cipherText, byte[] key) throws AmbariException {
    if (cipherText == null) {
      return null;
    } else {
      try {
        SecretKeySpec secretKey = new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] plaintext = cipher.doFinal(Base64.decodeBase64(cipherText));
        return new Gson().fromJson(new String(plaintext), KerberosCredential.class);
      } catch (NoSuchAlgorithmException e) {
        throw new AmbariException("Failed to decrypt cipher text due to invalid encryption algorithm", e);
      } catch (NoSuchPaddingException e) {
        throw new AmbariException("Failed to decrypt cipher text due to invalid padding scheme algorithm", e);
      } catch (IllegalBlockSizeException e) {
        throw new AmbariException("Failed to decrypt cipher text due to invalid block size", e);
      } catch (BadPaddingException e) {
        throw new AmbariException("Failed to decrypt cipher text due to invalid padding", e);
      } catch (InvalidKeyException e) {
        throw new AmbariException("Failed to decrypt cipher text due to invalid key", e);
      } catch (JsonSyntaxException e) {
        throw new AmbariException("Failed to decrypt cipher, cannot parse data into a KerberosCredential", e);
      }
    }
  }

  /**
   * Encrypts a KerberosCredential into a base64-encoded set of bytes.
   * <p/>
   * Given a KerberosCredential and a key, serializes the data into a JSON-formatted string and
   * encrypts it.
   *
   * @param kerberosCredential the KerberosCredential to encrypt
   * @param key                an array of bytes used to decrypt the encrypted data
   * @return a String containing base64-encoded encrypted data
   * @throws AmbariException if an error occurs while encrypting the KerberosCredential
   */
  public static String encrypt(KerberosCredential kerberosCredential, byte[] key) throws AmbariException {
    if (kerberosCredential == null) {
      return null;
    } else {
      try {
        SecretKeySpec secretKey = new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        String plaintext = new Gson().toJson(kerberosCredential);
        return Base64.encodeBase64String(cipher.doFinal(plaintext.getBytes()));
      } catch (NoSuchAlgorithmException e) {
        throw new AmbariException("Failed to encrypt plaintext due to invalid encryption algorithm", e);
      } catch (NoSuchPaddingException e) {
        throw new AmbariException("Failed to encrypt plaintext due to invalid padding scheme algorithm", e);
      } catch (IllegalBlockSizeException e) {
        throw new AmbariException("Failed to encrypt plaintext due to invalid key", e);
      } catch (BadPaddingException e) {
        throw new AmbariException("Failed to encrypt plaintext due to unexpected reasons", e);
      } catch (InvalidKeyException e) {
        throw new AmbariException("Failed to encrypt plaintext due to invalid key", e);
      }
    }
  }

  /**
   * Creates an empty KerberosCredential
   */
  public KerberosCredential() {
    principal = null;
    password = null;
    keytab = null;
  }

  /**
   * Creates a new KerberosCredential
   *
   * @param principal a String containing the principal name for this Kerberos credential
   * @param password  a String containing the password for this Kerberos credential
   * @param keytab    a String containing the base64 encoded keytab for this Kerberos credential
   */
  public KerberosCredential(String principal, String password, String keytab) {
    this.principal = principal;
    this.password = password;
    this.keytab = keytab;
  }

  /**
   * @return a String containing the principal name for this Kerberos credential
   */
  public String getPrincipal() {
    return principal;
  }

  /**
   * @param principal a String containing the principal name for this Kerberos credential
   */
  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  /**
   * @return a String containing the password for this Kerberos credential
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password a String containing the password for this Kerberos credential
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return a String containing the base64 encoded keytab for this Kerberos credential
   */
  public String getKeytab() {
    return keytab;
  }

  /**
   * @param keytab a String containing the base64 encoded keytab for this Kerberos credential
   */
  public void setKeytab(String keytab) {
    this.keytab = keytab;
  }

  /**
   * Encrypts this KerberosCredential into a base64-encoded set of bytes.
   * <p/>
   * Serializes this KerberosCredential into a JSON-formatted string and
   * encrypts it using the supplied key.
   *
   * @param key an array of bytes used to decrypt the encrypted data
   * @return a String containing base64-encoded encrypted data
   * @throws AmbariException if an error occurs while encrypting the KerberosCredential
   */
  public String encrypt(byte[] key) throws AmbariException {
    return encrypt(this, key);
  }
}
