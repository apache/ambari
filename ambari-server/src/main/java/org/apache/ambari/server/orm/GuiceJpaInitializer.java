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

package org.apache.ambari.server.orm;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ambari.server.events.JpaInitializedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;

/**
 * This class needs to be instantiated with Guice to initialize Guice-persist
 */
@Singleton
public class GuiceJpaInitializer {

  private final AtomicBoolean jpaInitialized = new AtomicBoolean(false);

  /**
   * GuiceJpaInitializer constructor.
   */
  @Inject
  public GuiceJpaInitializer(PersistService persistService) {
    if (persistService != null) {
      persistService.start();
    }
  }

  /**
   * Called to indicate that the JPA service is initialized and ready for use.
   * <p>
   * This means that the schema for the underlying database matches the JPA entity objects expectations
   * and the PersistService has been started.
   * <p>
   * If an {@link AmbariEventPublisher} is supplied, a {@link JpaInitializedEvent} is published so
   * that subscribers can perform database-related tasks when the infrastructure is ready.
   *
   * @param publisher an {@link AmbariEventPublisher} to use for publishing the event, optional
   */
  public void setInitialized(AmbariEventPublisher publisher) {
    jpaInitialized.set(true);

    if (publisher != null) {
      publisher.publish(new JpaInitializedEvent());
    }
  }

  /**
   * Returns whether JPA has been initialized or not
   *
   * @return <code>true</code> if JPA has been initialized; <code>false</code> if JPA has not been initialized
   * @see #setInitialized(AmbariEventPublisher)
   */
  public boolean isInitialized() {
    return jpaInitialized.get();
  }
}
