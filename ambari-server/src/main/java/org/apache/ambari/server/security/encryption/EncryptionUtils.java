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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;

import com.google.inject.Singleton;

@Singleton
public class EncryptionUtils {

  private static final String BASE_64_FIELD_DELIMITER = "::";
  private static final String UTF_8_CHARSET = StandardCharsets.UTF_8.name();

  private MasterKeyService environmentMasterKeyService;

  /**
   * Encrypts the given text using the master key found in the environment
   * 
   * @param toBeEncrypted
   *          the text to be encrypted
   * @return the String representation of the encrypted text
   * @throws Exception
   *           in case any error happened during the encryption process
   */
  public String encrypt(String toBeEncrypted) throws Exception {
    return encrypt(toBeEncrypted, (String) null);
  }

  /**
   * Encrypts the given text using the given master key
   * 
   * @param toBeEncrypted
   *          the text to be encrypted
   * @param masterKey
   *          the mater key to be used for encryption
   * @return the String representation of the encrypted text
   * @throws Exception
   *           in case any error happened during the encryption process
   */
  public String encrypt(String toBeEncrypted, String masterKey) throws Exception {
    return encrypt(toBeEncrypted, getMasterKeyService(masterKey));
  }

  private MasterKeyService getMasterKeyService(String masterKey) {
    if (masterKey == null) {
      initEnvironmentMasterKeyService();
      if (!environmentMasterKeyService.isMasterKeyInitialized()) {
        throw new SecurityException("You are trying to use a persisted master key but its initialization has been failed!");
      }
      return environmentMasterKeyService;
    }
    return new MasterKeyServiceImpl(masterKey);
  }

  private void initEnvironmentMasterKeyService() {
    if (environmentMasterKeyService == null) {
      environmentMasterKeyService = new MasterKeyServiceImpl();
    }
  }

  String encrypt(String toBeEncrypted, MasterKeyService masterKeyService) throws Exception {
    final AESEncryptor aes = new AESEncryptor(new String(masterKeyService.getMasterSecret()));
    final EncryptionResult encryptionResult = aes.encrypt(toBeEncrypted);
    return encodeEncryptionResult(encryptionResult);
  }

  private String encodeEncryptionResult(EncryptionResult encryptionResult) throws UnsupportedEncodingException {
    return Base64.encodeBase64String((Base64.encodeBase64String(encryptionResult.salt) + BASE_64_FIELD_DELIMITER + Base64.encodeBase64String(encryptionResult.iv)
        + BASE_64_FIELD_DELIMITER + Base64.encodeBase64String(encryptionResult.cipher)).getBytes(UTF_8_CHARSET));
  }

  /**
   * Decrypts the given text using the master key found in the environment
   * 
   * @param toBeDecrypted
   *          the text to be decrypted
   * @return the String representation of the decrypted text
   * @throws Exception
   *           in case any error happened during the decryption process
   */
  public String decrypt(String toBeDecrypted) throws Exception {
    return decrypt(toBeDecrypted, (String) null);
  }

  /**
   * Decrypts the given text using the given master key
   * 
   * @param toBeDecrypted
   *          the text to be decrypted
   * @param masterKey
   *          the mater key to be used for decryption
   * @return the String representation of the decrypted text
   * @throws Exception
   *           in case any error happened during the decryption process
   */
  public String decrypt(String toBeDecrypted, String masterKey) throws Exception {
    return decrypt(toBeDecrypted, getMasterKeyService(masterKey));
  }

  String decrypt(String toBeDecrypted, MasterKeyService masterKeyService) throws Exception {
    final String base64DecodedValue = new String(Base64.decodeBase64(toBeDecrypted), UTF_8_CHARSET);
    final String[] base64DecodedParts = base64DecodedValue.split(BASE_64_FIELD_DELIMITER);
    final AESEncryptor aes = new AESEncryptor(new String(masterKeyService.getMasterSecret()));
    return new String(aes.decrypt(Base64.decodeBase64(base64DecodedParts[0]), Base64.decodeBase64(base64DecodedParts[1]), Base64.decodeBase64(base64DecodedParts[2])),
        UTF_8_CHARSET);
  }
}
