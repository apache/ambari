/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Provider implementation for LDAP configurations.
 * It needs to be registered in the related GUICE module as a provider.
 * It's responsible for managing LDAP configurations in the application.
 * Whenever requested, this provider returns an AmbariLdapConfiguration which is always in sync with the persisted LDAP
 * configuration resource.
 *
 * The provider receives notifications on CRUD operations related to the persisted resource and reloads the cached
 * configuration instance accordingly.
 */
@Singleton
public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapAuthenticationProvider.class);
  private AmbariLdapConfiguration instance;

  @Inject
  private AmbariEventPublisher publisher;

  @Inject
  private Provider<AmbariConfigurationDAO> ambariConfigurationDAOProvider;

  @Inject
  private AmbariLdapConfigurationFactory ldapConfigurationFactory;

  private Gson gson = new GsonBuilder().create();

  @Inject
  public AmbariLdapConfigurationProvider() {
  }

  @Inject
  void register() {
    publisher.register(this);
  }

  @Override
  public AmbariLdapConfiguration get() {
    return instance != null ? instance : loadInstance(null);
  }

  /**
   * Loads the AmbariLdapConfiguration from the database.
   *
   * @param configurationId the configuration id
   * @return the AmbariLdapConfiguration instance
   */
  private AmbariLdapConfiguration loadInstance(Long configurationId) {
    AmbariConfigurationEntity configEntity = null;

    LOGGER.info("Loading LDAP configuration ...");
    if (null == configurationId) {

      LOGGER.debug("Initial loading of the ldap configuration ...");
      configEntity = ambariConfigurationDAOProvider.get().getLdapConfiguration();

    } else {

      LOGGER.debug("Reloading configuration based on the provied id: {}", configurationId);
      configEntity = ambariConfigurationDAOProvider.get().findByPK(configurationId);

    }

    if (configEntity != null) {
      Set propertyMaps = gson.fromJson(configEntity.getConfigurationBaseEntity().getConfigurationData(), Set.class);
      instance = ldapConfigurationFactory.createLdapConfiguration((Map<String, Object>) propertyMaps.iterator().next());
    }

    LOGGER.info("Loaded LDAP configuration instance: [ {} ]", instance);

    return instance;
  }

  // On changing the configuration, the provider gets updated with the fresh value
  @Subscribe
  public void ambariLdapConfigChanged(AmbariLdapConfigChangedEvent event) {
    LOGGER.info("LDAP config changed event received: {}", event);
    loadInstance(event.getConfigurationId());
    LOGGER.info("Refreshed LDAP config instance.");
  }


}
