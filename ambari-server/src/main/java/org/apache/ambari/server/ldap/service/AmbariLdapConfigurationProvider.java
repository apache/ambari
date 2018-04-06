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

package org.apache.ambari.server.ldap.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.JpaInitializedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Provider implementation for LDAP configurations.
 * It needs to be registered in the related GUICE module as a provider.
 * It's responsible for managing LDAP configurations in the application.
 * Whenever requested, this provider returns an AmbariLdapConfiguration which is always in sync with the persisted LDAP
 * configuration resource.
 * <p>
 * The provider receives notifications on CRUD operations related to the persisted resource and reloads the cached
 * configuration instance accordingly.
 */
@Singleton
public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapConfigurationProvider.class);
  private AmbariLdapConfiguration instance;

  @Inject
  private AmbariEventPublisher publisher;

  @Inject
  private Provider<AmbariConfigurationDAO> ambariConfigurationDAOProvider;

  @Inject
  public AmbariLdapConfigurationProvider() {
  }
  
  private AtomicBoolean jpaInitialized = new AtomicBoolean(false);

  @Inject
  void register() {
    publisher.register(this);
    LOGGER.info("Registered AmbariLdapConfigurationProvider in event publisher");
  }

  @Override
  public AmbariLdapConfiguration get() {
    LOGGER.debug("Getting LDAP configuration...");
    if (instance == null || instance.toMap().isEmpty()) {
      loadInstance();
    }
    return instance;
  }

  /**
   * Loads the AmbariLdapConfiguration from the database.
   */
  private void loadInstance() {
    List<AmbariConfigurationEntity> configEntities = null;

    LOGGER.info("Loading LDAP configuration ...");
    if (jpaInitialized.get()) {
      LOGGER.info("Actually loading LDAP configuration from DB ...");
      configEntities = ambariConfigurationDAOProvider.get().findByCategory(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName());
    }

    if (configEntities != null) {
      Map<String, String> properties = toProperties(configEntities);
      instance = new AmbariLdapConfiguration(properties);
    }else {
      instance = new AmbariLdapConfiguration();
    }

    LOGGER.info("Loaded LDAP configuration instance: [ {} ]", instance.toMap());
  }

  private Map<String, String> toProperties(List<AmbariConfigurationEntity> configEntities) {
    Map<String, String> map = new HashMap<>();

    for (AmbariConfigurationEntity entity : configEntities) {
      map.put(entity.getPropertyName(), entity.getPropertyValue());
    }

    return map;
  }

  // On changing the configuration, the provider gets updated with the fresh value
  @Subscribe
  public void ambariLdapConfigChanged(AmbariConfigurationChangedEvent event) {
    LOGGER.info("LDAP config changed event received: {}", event);
    loadInstance();
    LOGGER.info("Refreshed LDAP config instance.");
  }
  
  @Subscribe
  public void jpaInitialized(JpaInitializedEvent event) {
    LOGGER.info("JPA initialized event received: {}", event);
    jpaInitialized.getAndSet(true);
    loadInstance();
    LOGGER.info("Refreshed LDAP config instance.");
  }
}
