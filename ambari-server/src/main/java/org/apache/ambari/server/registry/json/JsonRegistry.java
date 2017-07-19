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
package org.apache.ambari.server.registry.json;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.exceptions.RegistryMpackNotFoundException;
import org.apache.ambari.server.exceptions.RegistryScenarioNotFoundException;
import org.apache.ambari.server.orm.entities.RegistryEntity;
import org.apache.ambari.server.registry.Registry;
import org.apache.ambari.server.registry.RegistryMpack;
import org.apache.ambari.server.registry.RegistryScenario;
import org.apache.ambari.server.registry.RegistryType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * JSON implementation of a software registry
 */
public class JsonRegistry implements Registry {
  private static final Logger LOG = LoggerFactory.getLogger(JsonRegistry.class);

  @Inject
  private AmbariEventPublisher eventPublisher;

  @Inject
  private Gson gson;

  /**
   * The software registry id
   */
  private final Long registryId;

  /**
   * The software registry name
   */
  private final String registryName;

  /**
   * The software registry type (See {@link RegistryType}
   */
  private final RegistryType registryType;

  /**
   * The software registry Uri
   */
  private final String registryUri;

  private final JsonRegistryDefinition registryDefinition;

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getRegistryId() {
    return registryId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRegistryName() {
    return registryName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RegistryType getRegistryType() {
    return registryType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRegistryUri() {
    return registryUri;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<? extends RegistryScenario> getRegistryScenarios() {
    return registryDefinition.getScenarios();
  }

  @AssistedInject
  public JsonRegistry(@Assisted RegistryEntity registryEntity, AmbariEventPublisher eventPublisher, Gson gson)
    throws AmbariException {
    this.eventPublisher = eventPublisher;
    this.gson = gson;
    this.registryId = registryEntity.getRegistryId();
    this.registryName = registryEntity.getRegistryName();
    this.registryType = registryEntity.getRegistryType();
    this.registryUri = registryEntity.getRegistryUri();
    try {
      URI uri = new URI(registryUri);
      URL url = uri.toURL();
      String jsonString = IOUtils.toString(url);
      this.registryDefinition = gson.fromJson(jsonString, JsonRegistryDefinition.class);
    } catch (MalformedURLException e) {
      throw new AmbariException(e.getMessage(), e);
    } catch (IOException e) {
      throw new AmbariException(e.getMessage(), e);
    } catch (URISyntaxException e) {
      throw new AmbariException(e.getMessage(), e);
    }
  }

  @Override
  public RegistryScenario getRegistryScenario(final String scenarioName)
    throws AmbariException {
      RegistryScenario registryScenario = null;
      for (RegistryScenario scenario : getRegistryScenarios()) {
        if (scenarioName.equals(scenario.getScenarioName())) {
          registryScenario = scenario;
          break;
        }
      }
      if(registryScenario == null) {
        throw new RegistryScenarioNotFoundException(this.registryName, scenarioName);
      }
      return registryScenario;
    }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<? extends RegistryMpack> getRegistryMpacks() {
    return registryDefinition.getMpacks();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RegistryMpack getRegistryMpack(final String mpackName)
    throws AmbariException {
    RegistryMpack registryMpack = null;
    if(mpackName == null || mpackName.isEmpty()) {
      throw new AmbariException(String.format("Registry mpack name cannot be null"));
    }
    for (RegistryMpack mpack : getRegistryMpacks()) {
      if (mpackName.equals(mpack.getMpackName())) {
        registryMpack = mpack;
        break;
      }
    }
    if(registryMpack == null) {
      throw new RegistryMpackNotFoundException(this.registryName, mpackName);
    }
    return registryMpack;
  }
}
