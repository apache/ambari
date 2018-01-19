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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * AmbariServerConfigurationHandler handles Ambari server specific configuration properties.
 */
@StaticallyInject
class AmbariServerConfigurationHandler extends RootServiceComponentConfigurationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerConfigurationHandler.class);

  @Inject
  private static AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private static AmbariEventPublisher publisher;


  @Override
  public Map<String, Map<String, String>> getConfigurations(String categoryName)
      throws NoSuchResourceException {
    Map<String, Map<String, String>> configurations = null;

    List<AmbariConfigurationEntity> entities = (categoryName == null)
        ? ambariConfigurationDAO.findAll()
        : ambariConfigurationDAO.findByCategory(categoryName);

    if (entities != null) {
      configurations = new HashMap<>();

      for (AmbariConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        Map<String, String> properties = configurations.get(category);

        if (properties == null) {
          properties = new TreeMap<>();
          configurations.put(category, properties);
        }

        properties.put(entity.getPropertyName(), entity.getPropertyValue());
      }
    }

    return configurations;
  }

  @Override
  public void removeConfiguration(String categoryName) throws NoSuchResourceException {
    if (null == categoryName) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
      try {
        if (ambariConfigurationDAO.removeByCategory(categoryName) > 0) {
          publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
        }
      } catch (IllegalStateException e) {
        throw new NoSuchResourceException(e.getMessage());
      }
    }
  }

  @Override
  public void updateCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) {
    if (ambariConfigurationDAO.reconcileCategory(categoryName, properties, removePropertiesIfNotSpecified)) {
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
}
