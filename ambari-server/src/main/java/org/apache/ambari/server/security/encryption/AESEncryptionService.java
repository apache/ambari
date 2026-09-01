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
package org.apache.ambari.server.security.encryption;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.inject.Inject;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.TextEncoding;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.google.inject.Singleton;

@Singleton
public class AESEncryptionService implements EncryptionService {

  private static final String ENCODED_TEXT_FIELD_DELIMITER = "::";
  private static final String UTF_8_CHARSET = StandardCharsets.UTF_8.name();
  private static final byte[] GCM_AAD = "ambari-agent-config-v2".getBytes(StandardCharsets.UTF_8);
  private static final int GCM_ITERATIONS = 210000;
  private static final int GCM_KEY_BITS = 256;
  private static final int GCM_TAG_BITS = 128;
  private static final int GCM_SALT_BYTES = 16;
  private static final int GCM_NONCE_BYTES = 12;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private MasterKeyService environmentMasterKeyService;

  @Inject
  private Configuration configuration;

  @Override
  public String encrypt(String toBeEncrypted) {
    return encrypt(toBeEncrypted, TextEncoding.BASE_64);
  }

  @Override
  public String encrypt(String toBeEncrypted, TextEncoding textEncoding) {
    return encrypt(toBeEncrypted, getAmbariMasterKey(), textEncoding);
  }

  @Override
  public String encrypt(String toBeEncrypted, String key) {
    return encrypt(toBeEncrypted, key, TextEncoding.BASE_64);
  }

  @Override
  public String encrypt(String toBeEncrypted, String key, TextEncoding textEncoding) {
    try {
      final EncryptionResult encryptionResult = getAesEncryptor(key).encrypt(toBeEncrypted);
      return TextEncoding.BASE_64 == textEncoding ? encodeEncryptionResultBase64(encryptionResult) : encodeEncryptionResultBinHex(encryptionResult);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String encryptGcm(String toBeEncrypted, String key, TextEncoding textEncoding) {
    byte[] salt = new byte[GCM_SALT_BYTES];
    byte[] nonce = new byte[GCM_NONCE_BYTES];
    SECURE_RANDOM.nextBytes(salt);
    SECURE_RANDOM.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, deriveGcmKey(key, salt), new GCMParameterSpec(GCM_TAG_BITS, nonce));
      cipher.updateAAD(GCM_AAD);
      byte[] ciphertext = cipher.doFinal(toBeEncrypted.getBytes(StandardCharsets.UTF_8));
      EncryptionResult result = new EncryptionResult(salt, nonce, ciphertext);
      return TextEncoding.BASE_64 == textEncoding ? encodeEncryptionResultBase64(result) : encodeEncryptionResultBinHex(result);
    } catch (GeneralSecurityException | IOException e) {
      throw new SecurityException("Unable to encrypt Agent configuration with AES-GCM", e);
    }
  }

  private SecretKeySpec deriveGcmKey(String key, byte[] salt) throws GeneralSecurityException {
    PBEKeySpec keySpec = new PBEKeySpec(key.toCharArray(), salt, GCM_ITERATIONS, GCM_KEY_BITS);
    try {
      byte[] derivedKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).getEncoded();
      return new SecretKeySpec(derivedKey, "AES");
    } finally {
      keySpec.clearPassword();
    }
  }

  private AESEncryptor getAesEncryptor(String key) {
    // AESEncryptor owns mutable Cipher state and a random salt/IV. Reusing it
    // across calls would be both thread-unsafe and cryptographically unsafe.
    return new AESEncryptor(key);
  }

  @Override
  public final String getAmbariMasterKey() {
    initEnvironmentMasterKeyService();
    return String.valueOf(environmentMasterKeyService.getMasterSecret());
  }

  private void initEnvironmentMasterKeyService() {
    if (environmentMasterKeyService == null) {
      environmentMasterKeyService = new MasterKeyServiceImpl(configuration);
      if (!environmentMasterKeyService.isMasterKeyInitialized()) {
        throw new SecurityException("You are trying to use a persisted master key but its initialization has been failed!");
      }
    }
  }

  private String encodeEncryptionResultBase64(EncryptionResult encryptionResult) throws UnsupportedEncodingException {
    return Base64.encodeBase64String((Base64.encodeBase64String(encryptionResult.salt) + ENCODED_TEXT_FIELD_DELIMITER + Base64.encodeBase64String(encryptionResult.iv)
        + ENCODED_TEXT_FIELD_DELIMITER + Base64.encodeBase64String(encryptionResult.cipher)).getBytes(UTF_8_CHARSET));
  }

  private String encodeEncryptionResultBinHex(EncryptionResult encryptionResult) throws UnsupportedEncodingException {
    return Hex.encodeHexString((Hex.encodeHexString(encryptionResult.salt) + ENCODED_TEXT_FIELD_DELIMITER + Hex.encodeHexString(encryptionResult.iv)
        + ENCODED_TEXT_FIELD_DELIMITER + Hex.encodeHexString(encryptionResult.cipher)).getBytes(UTF_8_CHARSET));
  }

  @Override
  public String decrypt(String toBeDecrypted) {
    return decrypt(toBeDecrypted, TextEncoding.BASE_64);
  }

  @Override
  public String decrypt(String toBeDecrypted, TextEncoding textEncoding) {
    return decrypt(toBeDecrypted, getAmbariMasterKey(), textEncoding);
  }

  @Override
  public String decrypt(String toBeDecrypted, String key) {
    return decrypt(toBeDecrypted, key, TextEncoding.BASE_64);
  }

  @Override
  public String decrypt(String toBeDecrypted, String key, TextEncoding textEncoding) {
    try {
      final byte[] decodedValue = TextEncoding.BASE_64 == textEncoding ? Base64.decodeBase64(toBeDecrypted) : Hex.decodeHex(toBeDecrypted.toCharArray());
      final String decodedText = new String(decodedValue, UTF_8_CHARSET);
      final String[] decodedParts = decodedText.split(ENCODED_TEXT_FIELD_DELIMITER);
      final AESEncryptor aes = getAesEncryptor(key);
      if (TextEncoding.BASE_64 == textEncoding) {
        return new String(aes.decrypt(Base64.decodeBase64(decodedParts[0]), Base64.decodeBase64(decodedParts[1]), Base64.decodeBase64(decodedParts[2])), UTF_8_CHARSET);
      } else {
        return new String(
          aes.decrypt(Hex.decodeHex(decodedParts[0].toCharArray()), Hex.decodeHex(decodedParts[1].toCharArray()), Hex.decodeHex(decodedParts[2].toCharArray())),
          UTF_8_CHARSET);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (DecoderException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String decryptGcm(String toBeDecrypted, String key, TextEncoding textEncoding) {
    try {
      byte[] decodedValue = TextEncoding.BASE_64 == textEncoding
          ? Base64.decodeBase64(toBeDecrypted)
          : Hex.decodeHex(toBeDecrypted.toCharArray());
      String[] decodedParts = new String(decodedValue, StandardCharsets.UTF_8)
          .split(ENCODED_TEXT_FIELD_DELIMITER, -1);
      if (decodedParts.length != 3) {
        throw new SecurityException("Agent AES-GCM envelope must contain exactly three fields");
      }
      byte[] salt = TextEncoding.BASE_64 == textEncoding ? Base64.decodeBase64(decodedParts[0]) : Hex.decodeHex(decodedParts[0].toCharArray());
      byte[] nonce = TextEncoding.BASE_64 == textEncoding ? Base64.decodeBase64(decodedParts[1]) : Hex.decodeHex(decodedParts[1].toCharArray());
      byte[] ciphertext = TextEncoding.BASE_64 == textEncoding ? Base64.decodeBase64(decodedParts[2]) : Hex.decodeHex(decodedParts[2].toCharArray());
      if (salt.length != GCM_SALT_BYTES || nonce.length != GCM_NONCE_BYTES
          || ciphertext.length < GCM_TAG_BITS / Byte.SIZE) {
        throw new SecurityException("Agent AES-GCM envelope has invalid field lengths");
      }
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, deriveGcmKey(key, salt), new GCMParameterSpec(GCM_TAG_BITS, nonce));
      cipher.updateAAD(GCM_AAD);
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | DecoderException | RuntimeException e) {
      throw new SecurityException("Unable to authenticate or decrypt Agent AES-GCM configuration", e);
    }
  }
}
