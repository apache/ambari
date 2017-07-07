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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;

/**
 *
 */
public class RegistryImpl implements Registry {
  private static final Logger LOG = LoggerFactory.getLogger(RegistryImpl.class);

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

  @Inject
  public RegistryImpl(@Assisted RegistryEntity registryEntity, Injector injector, AmbariEventPublisher eventPublisher)
    throws AmbariException {
    this.registryId = registryEntity.getRegistryId();
    this.registryName = registryEntity.getRegistryName();
    this.registryType = registryEntity.getRegistryType();
    this.registryUri = registryEntity.getRegistryUri();
  }
}
