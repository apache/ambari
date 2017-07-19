/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.registry;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.exceptions.RegistryNotFoundException;
import org.apache.ambari.server.orm.dao.RegistryDAO;
import org.apache.ambari.server.orm.entities.RegistryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Provides high-level access to software registries
 */
@Singleton
public class RegistryManagerImpl implements RegistryManager {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryManagerImpl.class);

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  private RegistryDAO registryDAO;
  @Inject
  private RegistryFactory registryFactory;

  /**
   * Used to publish events relating to software registry CRUD operations.
   */
  @Inject
  private AmbariEventPublisher eventPublisher;


  private Map<Long, Registry> registriesById = new ConcurrentHashMap<>();
  private Map<String, Registry> registriesByName = new ConcurrentHashMap<>();

  @Inject
  public RegistryManagerImpl(RegistryDAO registryDAO, RegistryFactory registryFactory) {

    this.registryDAO = registryDAO;
    this.registryFactory = registryFactory;
  }

  /**
   * Inititalizes all of the in-memory state collections that this class
   * unfortunately uses. It's annotated with {@link com.google.inject.Inject} as a way to define a
   * very simple lifecycle with Guice where the constructor is instantiated
   * (allowing injected members) followed by this method which initiailizes the
   * state of the instance.
   * <p/>
   * Because some of these stateful initializations may actually reference this
   * {@link RegistryManager} instance, we must do this after the object has been
   * instantiated and injected.
   */
  @Inject
  void loadRegistries() throws AmbariException {
    for (RegistryEntity registryEntity : registryDAO.findAll()) {
      Registry registry = registryFactory.create(registryEntity);
      registriesById.put(registryEntity.getRegistryId(), registry);
      registriesByName.put(registryEntity.getRegistryName(), registry);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Registry addRegistry(String registryName, RegistryType registryType, String registryUri)
    throws AmbariException {

    RegistryEntity registryEntity = new RegistryEntity();
    registryEntity.setRegistryName(registryName);
    registryEntity.setRegistryUri(registryUri);
    registryEntity.setRegistryType(registryType);
    Long registryId = registryDAO.create(registryEntity);
    registryEntity.setRegistryId(registryId);
    Registry registry = registryFactory.create(registryEntity);
    registriesById.put(registry.getRegistryId(), registry);
    registriesByName.put(registry.getRegistryName(), registry);
    return registry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Registry getRegistry(final Long registryId) throws AmbariException {
    Registry registry = null;
    if(registryId != null) {
      registry = registriesById.get(registryId);
    }
    if(registry == null) {
      throw new RegistryNotFoundException(registryId);
    }
    return registry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Registry getRegistry(final String registryName) throws AmbariException {
    Registry registry = null;
    if(registryName != null) {
      registry = registriesByName.get(registryName);
    }
    if(registry == null) {
      throw new RegistryNotFoundException(registryName);
    }
    return registry;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Map<Long, Registry> getRegistries() {
    return Collections.unmodifiableMap(registriesById);
  }
}
