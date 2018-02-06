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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

public class HadoopCredentialStore implements PasswordStore {
  private static final Logger LOG = LoggerFactory.getLogger(InfraManagerSecurityConfig.class);
  public static final String CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY = "hadoop.security.credential.provider.path";

  private final String credentialStoreProviderPath;

  public HadoopCredentialStore(String credentialStoreProviderPath) {
    this.credentialStoreProviderPath = credentialStoreProviderPath;
  }

  @Override
  public Optional<String> getPassword(String propertyName) {
    try {
      if (isBlank(credentialStoreProviderPath)) {
        return Optional.empty();
      }

      org.apache.hadoop.conf.Configuration config = new org.apache.hadoop.conf.Configuration();
      config.set(CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY, credentialStoreProviderPath);
      char[] passwordChars = config.getPassword(propertyName);
      return (isNotEmpty(passwordChars)) ? Optional.of(new String(passwordChars)) : Optional.empty();
    } catch (Exception e) {
      LOG.warn("Could not load password {} from credential store.", propertyName);
      return Optional.empty();
    }
  }
}
