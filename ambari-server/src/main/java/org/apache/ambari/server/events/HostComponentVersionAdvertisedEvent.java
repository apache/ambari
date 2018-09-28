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
package org.apache.ambari.server.events;

import org.apache.ambari.server.agent.HeartbeatProcessor.ComponentVersionStructuredOut;
import org.apache.ambari.server.agent.StructuredOutputType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;

import com.google.common.base.MoreObjects;

/**
 * The {@link HostComponentVersionAdvertisedEvent}
 * occurs when a Host Component advertises it's current version value.
 */
public class HostComponentVersionAdvertisedEvent extends ClusterEvent {

  private final Cluster cluster;
  private final ServiceComponentHost sch;
  private final ComponentVersionStructuredOut componentVersionStructuredOut;

  /**
   * Constructor.
   *
   * @param cluster: cluster.
   * @param sch: the service component host
   */
  public HostComponentVersionAdvertisedEvent(Cluster cluster, ServiceComponentHost sch, ComponentVersionStructuredOut componentVersionStructuredOut) {
    super(AmbariEventType.HOST_COMPONENT_VERSION_ADVERTISED, cluster.getClusterId());
    this.cluster = cluster;
    this.sch = sch;
    this.componentVersionStructuredOut = componentVersionStructuredOut;
  }

  /**
   * Gets the component/host combination associated with this event.
   *
   * @return
   */
  public ServiceComponentHost getServiceComponentHost() {
    return sch;
  }

  /**
   * Gets the cluster associated with this event.
   *
   * @return
   */
  public Cluster getCluster() {
    return cluster;
  }

  /**
   * Gets the structured output parsed from
   * {@link StructuredOutputType#VERSION_REPORTING}.
   *
   * @return
   */
  public ComponentVersionStructuredOut getStructuredOutput() {
    return componentVersionStructuredOut;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("hostName", sch.getHostName())
      .add("service", sch.getServiceName())
      .add("component", sch.getServiceComponentName())
      .add("mpackVersion", componentVersionStructuredOut.mpackVersion)
      .add("version", componentVersionStructuredOut.version).toString();
  }
}
