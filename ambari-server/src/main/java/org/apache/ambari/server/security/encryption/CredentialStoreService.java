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

public interface CredentialStoreService {
  /**
   * Adds a new credential to this CredentialStoreService
   * <p/>
   * The supplied key will be converted into UTF-8 bytes before being stored.
   *
   * @param alias a string declaring the alias (or name) of the credential
   * @param key   an array of chars containing the credential
   * @throws AmbariException if an error occurs while storing the new credential
   */
  void addCredential(String alias, char[] key) throws AmbariException;

  /**
   * Retrieves the specified credential from this CredentialStoreService
   *
   * @param alias a string declaring the alias (or name) of the credential
   * @return an array of chars containing the credential
   * @throws AmbariException if an error occurs while retrieving the new credential
   */
  char[] getCredential(String alias) throws AmbariException;

  /**
   * Removes the specified credential from this CredentialStoreService
   *
   * @param alias a string declaring the alias (or name) of the credential
   * @throws AmbariException if an error occurs while removing the new credential
   */
  void removeCredential(String alias) throws AmbariException;

  /**
   * Sets the MasterKeyService for this CredentialStoreService
   *
   * @param masterKeyService the MasterKeyService
   */
  void setMasterKeyService(MasterKeyService masterKeyService);
}
