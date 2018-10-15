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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.AmbariRuntimeException;
import org.apache.ambari.server.events.StackUpgradeFinishEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.utils.TextEncoding;
import org.apache.commons.collections.CollectionUtils;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * {@link Encryptor} implementation for encrypting/decrypting PASSWORD type
 * properties
 */

@Singleton
public class PasswordPropertiesEncryptor implements Encryptor<Map<String, String>> {

  private static final String ENCRYPTED_PROPERTY_PREFIX = "${enc=aes256_hex, value=";
  private static final String ENCRYPTED_PROPERTY_SCHEME = ENCRYPTED_PROPERTY_PREFIX + "%s}";

  private final EncryptionService encryptionService;
  private final Map<Long, Map<String, Set<String>>> clusterPasswordProperties = new ConcurrentHashMap<>();

  @Inject
  public PasswordPropertiesEncryptor(EncryptionService encryptionService, AmbariEventPublisher ambariEventPublisher) {
    this.encryptionService = encryptionService;
    ambariEventPublisher.register(this);
  }

  /**
   * It is a must that any caller of this method adds the following additional
   * information on the following indexes in <code>additionalInfo</code> with the
   * following types:
   * <ul>
   * <li>[0] - <code>org.apache.ambari.server.state.Cluster</code> (the cluster
   * whose properties should be scanned for PASSWORD properties)</li>
   * <li>[1] - <code>java.lang.String</code> (to keep properties only that match
   * this configuration type )</li>
   * </ul>
   */
  @Override
  public Map<String, String> encryptSensitiveData(Map<String, String> properties, Object... additionaInfo) {
    try {
      final Map<String, String> encryptedProperties = new HashMap<>(properties.size());
      if (properties != null) {
        final Cluster cluster = (Cluster) additionaInfo[0];
        final String configType = (String) additionaInfo[1];
        final Set<String> passwordProperties = getPasswordProperties(cluster, configType);
        if (CollectionUtils.isNotEmpty(passwordProperties)) {
          String encryptedValue;
          for (Map.Entry<String, String> property : properties.entrySet()) {
            encryptedValue = passwordProperties.contains(property.getKey()) && !isEncryptedPassword(property.getValue())
                ? encryptAndDecoratePropertyValue(property.getValue())
                : property.getValue();
            encryptedProperties.put(property.getKey(), encryptedValue);
          }
        }
      }
      return encryptedProperties;
    } catch (Exception e) {
      throw new AmbariRuntimeException("Error while encrypting sensitive data", e);
    }
  }

  private boolean isEncryptedPassword(String password) {
    return password != null && password.startsWith(ENCRYPTED_PROPERTY_PREFIX); // assuming previous encryption by this class
  }

  private Set<String> getPasswordProperties(Cluster cluster, String configType) throws AmbariException {
    final long clusterId = cluster.getClusterId();
    if (!clusterPasswordProperties.containsKey(clusterId)) {
      clusterPasswordProperties.put(clusterId, new ConcurrentHashMap<>());
    }
    if (!clusterPasswordProperties.get(clusterId).containsKey(configType)) {
      clusterPasswordProperties.get(clusterId).put(configType, cluster.getConfigPropertiesTypes(configType).get(PropertyType.PASSWORD));
    }

    return clusterPasswordProperties.get(clusterId).get(configType);
  }

  private String encryptAndDecoratePropertyValue(String propertyValue) throws Exception {
    final String encrypted = encryptionService.encrypt(propertyValue, TextEncoding.BIN_HEX);
    return String.format(ENCRYPTED_PROPERTY_SCHEME, encrypted);
  }

  @Override
  public Map<String, String> decryptSensitiveData(Map<String, String> properties, Object... additionalInfo) {
    final Map<String, String> decryptedProperties = new HashMap<>(properties.size());
    if (properties != null) {
      String decryptedValue;
      for (Map.Entry<String, String> property : properties.entrySet()) {
        decryptedValue = isEncryptedPassword(property.getValue()) ? decryptProperty(property.getValue()) : property.getValue();
        decryptedProperties.put(property.getKey(), decryptedValue);
      }
    }
    return decryptedProperties;
  }

  private String decryptProperty(String property) {
    try {
      // sample value: ${enc=aes256_hex, value=5248...303d}
      final String encrypted = property.substring(ENCRYPTED_PROPERTY_PREFIX.length(), property.indexOf('}'));
      return encryptionService.decrypt(encrypted, TextEncoding.BIN_HEX);
    } catch (Exception e) {
      throw new AmbariRuntimeException("Error while decrypting property", e);
    }
  }

  @Subscribe
  public void onStackUpgradeFinished(StackUpgradeFinishEvent event) {
    // during stack upgrade it is very unlikely that one edits cluster configuration
    // therefore it's safe to assume that other threads will not encrypting
    // sensitive data at the same time we clear the cached password properties. Thus
    // additional Java locking is not needed (using concurrent map to store this
    // information is enough)
    final long clusterId = event.getClusterId();
    if (clusterPasswordProperties.containsKey(clusterId)) {
      clusterPasswordProperties.get(clusterId).clear();
    }
  }

}
