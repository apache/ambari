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

package org.apache.ambari.server.controller.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.RootServiceComponentConfiguration;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * AmbariServerConfigurationHandler handles Ambari server specific configuration properties.
 */
@Singleton
public class AmbariServerConfigurationHandler extends RootServiceComponentConfigurationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerConfigurationHandler.class);

  private final AmbariConfigurationDAO ambariConfigurationDAO;
  private final AmbariEventPublisher publisher;
  private final Configuration ambariConfiguration;

  private CredentialProvider credentialProvider;

  @Inject
  AmbariServerConfigurationHandler(AmbariConfigurationDAO ambariConfigurationDAO, AmbariEventPublisher publisher, Configuration ambariConfiguration) {
    this.ambariConfigurationDAO = ambariConfigurationDAO;
    this.publisher = publisher;
    this.ambariConfiguration = ambariConfiguration;
  }

  @Override
  public Map<String, RootServiceComponentConfiguration> getComponentConfigurations(String categoryName) {
    Map<String, RootServiceComponentConfiguration> configurations = null;

    List<AmbariConfigurationEntity> entities = (categoryName == null)
        ? ambariConfigurationDAO.findAll()
        : ambariConfigurationDAO.findByCategory(categoryName);

    if (entities != null) {
      configurations = new HashMap<>();
      for (AmbariConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        RootServiceComponentConfiguration configuration = configurations.get(category);
        if (configuration == null) {
          configuration = new RootServiceComponentConfiguration();
          configurations.put(category, configuration);
        }

        configuration.addProperty(entity.getPropertyName(), entity.getPropertyValue());
        if (categoryName != null) {
          configuration.addPropertyType(entity.getPropertyName(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(categoryName, entity.getPropertyName()));
        }
      }
    }

    return configurations;
  }

  @Override
  public void removeComponentConfiguration(String categoryName) {
    if (null == categoryName) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
      if (ambariConfigurationDAO.removeByCategory(categoryName) > 0) {
        publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
      }
    }
  }

  @Override
  public void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws AmbariException {
    boolean toBePublished = false;
    final Iterator<Map.Entry<String, String>> propertiesIterator = properties.entrySet().iterator();
    while (propertiesIterator.hasNext()) {
      Map.Entry<String, String> property = propertiesIterator.next();

      // Ensure the incoming property is valid
      AmbariServerConfigurationKey key = AmbariServerConfigurationUtils.getConfigurationKey(categoryName, property.getKey());
      if(key == null) {
        throw new IllegalArgumentException(String.format("Invalid Ambari server configuration key: %s:%s", categoryName, property.getKey()));
      }

      if (AmbariServerConfigurationUtils.isPassword(key)) {
        final String passwordFileOrCredentialStoreAlias = fetchPasswordFileNameOrCredentialStoreAlias(categoryName, property.getKey());
        if (StringUtils.isNotBlank(passwordFileOrCredentialStoreAlias)) { //if blank -> this is the first time setup; we simply need to store the alias/file name
          if (updatePasswordIfNeeded(categoryName, property.getKey(), property.getValue())) {
            toBePublished = true;
          }
          propertiesIterator.remove(); //we do not need to change the any PASSWORD type configuration going forward
        }
      }
    }

    if (!properties.isEmpty()) {
      toBePublished = ambariConfigurationDAO.reconcileCategory(categoryName, properties, removePropertiesIfNotSpecified) || toBePublished;
    }

    if (toBePublished) {
      // notify subscribers about the configuration changes
      publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
    }
  }

  @Override
  public OperationResult performOperation(String categoryName, Map<String, String> properties,
                                          boolean mergeExistingProperties, String operation,
                                          Map<String, Object> operationParameters) throws SystemException {
    throw new SystemException(String.format("The requested operation is not supported for this category: %s", categoryName));
  }

  public Map<String, Map<String, String>> getConfigurations() {
    Map<String, Map<String, String>> configurations = new HashMap<>();
    List<AmbariConfigurationEntity> entities = ambariConfigurationDAO.findAll();

    if (entities != null) {
      for (AmbariConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        Map<String, String> configuration = configurations.computeIfAbsent(category, k -> new HashMap<>());
        configuration.put(entity.getPropertyName(), entity.getPropertyValue());
      }
    }

    return configurations;
  }

  /**
   * Get the properties associated with a configuration category
   *
   * @param categoryName the name of the requested category
   * @return a map of property names to values; or null if the data does not exist
   */
  public Map<String, String> getConfigurationProperties(String categoryName) {
    Map<String, String> properties = null;

    List<AmbariConfigurationEntity> entities = ambariConfigurationDAO.findByCategory(categoryName);
    if (entities != null) {
      properties = new HashMap<>();
      for (AmbariConfigurationEntity entity : entities) {
        properties.put(entity.getPropertyName(), entity.getPropertyValue());
      }
    }

    return properties;
  }

  private boolean updatePasswordIfNeeded(String categoryName, String propertyName, String newPassword) throws AmbariException {
    if (newPassword != null) {
      final String passwordFileOrCredentailStoreAlias = fetchPasswordFileNameOrCredentialStoreAlias(categoryName, propertyName);
      if (!newPassword.equals(passwordFileOrCredentailStoreAlias)) { //we only need to do anything if the user-supplied password is a 'real' password
        if (ambariConfiguration.isSecurityPasswordEncryptionEnabled()) {
          getCredentialProvider().addAliasToCredentialStore(passwordFileOrCredentailStoreAlias, newPassword);
        } else {
          savePasswordInFile(passwordFileOrCredentailStoreAlias, newPassword);
        }
        return true;
      }
    }
    return false;
  }

  /*
   * If the configuration element is actually a PASSWORD type element then we either have a password file name stored in the DB
   * or - in case security password encryption is enabled - a Credential Store alias.
   */
  private String fetchPasswordFileNameOrCredentialStoreAlias(String categoryName, String propertyName) {
    for (AmbariConfigurationEntity entity : ambariConfigurationDAO.findByCategory(categoryName)) {
      if (entity.getPropertyName().equals(propertyName)) {
        return entity.getPropertyValue();
      }
    }

    return null;
  }

  private CredentialProvider getCredentialProvider() throws AmbariException {
    if (credentialProvider == null) {
      credentialProvider = new CredentialProvider(null, ambariConfiguration.getMasterKeyLocation(),
          ambariConfiguration.isMasterKeyPersisted(), ambariConfiguration.getMasterKeyStoreLocation());
    }
    return credentialProvider;
  }

  private void savePasswordInFile(String passwordFileName, String newPassword) throws AmbariException {
    try {
      if (StringUtils.isNotBlank(passwordFileName)) {
        FileUtils.writeStringToFile(new File(passwordFileName), newPassword, Charset.defaultCharset());
      }
    } catch (IOException e) {
      throw new AmbariException("Error while updating password file [" + passwordFileName + "]", e);
    }
  }
}
