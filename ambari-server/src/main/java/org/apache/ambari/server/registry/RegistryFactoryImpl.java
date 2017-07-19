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

package org.apache.ambari.server.registry;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.entities.RegistryEntity;
import org.apache.ambari.server.registry.json.JsonRegistry;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Registry Factory implementation
 */
@Singleton
public class RegistryFactoryImpl implements RegistryFactory {

  private Injector injector;

  /**
   * Constructor
   * @param injector
   */
  @Inject
  public RegistryFactoryImpl(Injector injector) {
    this.injector = injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Registry create(final RegistryEntity registryEntity) throws AmbariException {
    RegistryType type = registryEntity.getRegistryType();
    switch (type) {
      case JSON:
        return new JsonRegistry(
          registryEntity, injector.getInstance(AmbariEventPublisher.class), injector.getInstance(Gson.class));
      default:
        throw new AmbariException("Unknown registry type");
    }
  }
}
