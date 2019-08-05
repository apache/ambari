/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.conf.security;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public class HadoopCredentialStore {
  public static final String CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY = "hadoop.security.credential.provider.path";

  private final String credentialStoreProviderPath;

  public HadoopCredentialStore(String credentialStoreProviderPath) {
    this.credentialStoreProviderPath = credentialStoreProviderPath;
  }

  public Optional<char[]> get(String key) {
    try {
      if (isBlank(credentialStoreProviderPath)) {
        return Optional.empty();
      }

      org.apache.hadoop.conf.Configuration config = new org.apache.hadoop.conf.Configuration();
      config.set(CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY, credentialStoreProviderPath);
      char[] passwordChars = config.getPassword(key);
      return (isNotEmpty(passwordChars)) ? Optional.of(passwordChars) : Optional.empty();
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Could not load password %s from credential store.", key), e);
    }
  }

  public Secret getSecret(String key) {
    return new HadoopCredential(this, key);
  }
}
