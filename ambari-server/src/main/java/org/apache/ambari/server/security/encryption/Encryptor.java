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

/**
 * Defines a generic contract on encrypting/decrypting sensitive data
 */
public interface Encryptor<T> {

  /**
   * Encrypts the given encryptible object
   * 
   * @param encryptible
   *          to be encrypted
   * @param additionalInfo
   *          any additional information that is needed during the encryption
   *          process (may be null)
   * @return the encrypted value
   */
  T encryptSensitiveData(T encryptible, Object... additionalInfo);

  /**
   * Decrypts the given decryptible object
   * 
   * @param decryptible
   *          to be decrypted
   * @param additionalInfo
   *          any additional information that is needed during the decryption
   *          process (may be null)
   * @return the decrypted value
   */
  T decryptSensitiveData(T decryptible, Object... additionalInfo);

}
