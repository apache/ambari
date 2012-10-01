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

package org.apache.ambari.api.controller.jdbc;

import org.apache.ambari.api.controller.spi.ManagementController;
import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.api.controller.spi.Request;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.Set;

/**
 * Generic JDBC implementation of a management controller.
 */
public class JDBCManagementController implements ManagementController {

  private final ConnectionFactory connectionFactory;

  public JDBCManagementController(ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  @Override
  public void createClusters(Request request) {
    JDBCHelper.createResources(connectionFactory, request);
  }

  @Override
  public void createServices(Request request) {
    JDBCHelper.createResources(connectionFactory, request);
  }

  @Override
  public void createComponents(Request request) {
    JDBCHelper.createResources(connectionFactory, request);
  }

  @Override
  public void createHosts(Request request) {
    JDBCHelper.createResources(connectionFactory, request);
  }

  @Override
  public void createHostComponents(Request request) {
    JDBCHelper.createResources(connectionFactory, request);
  }

  @Override
  public Set<Resource> getClusters(Request request, Predicate predicate) {
    return JDBCHelper.getResources(connectionFactory, Resource.Type.Cluster, request, predicate);
  }

  @Override
  public Set<Resource> getServices(Request request, Predicate predicate) {
    return JDBCHelper.getResources(connectionFactory, Resource.Type.Service, request, predicate);
  }

  @Override
  public Set<Resource> getComponents(Request request, Predicate predicate) {
    return JDBCHelper.getResources(connectionFactory, Resource.Type.Component, request, predicate);
  }

  @Override
  public Set<Resource> getHosts(Request request, Predicate predicate) {
    return JDBCHelper.getResources(connectionFactory, Resource.Type.Host, request, predicate);
  }

  @Override
  public Set<Resource> getHostComponents(Request request, Predicate predicate) {
    return JDBCHelper.getResources(connectionFactory, Resource.Type.HostComponent, request, predicate);
  }

  @Override
  public void updateClusters(Request request, Predicate predicate) {
    JDBCHelper.updateResources(connectionFactory, request, predicate);
  }

  @Override
  public void updateServices(Request request, Predicate predicate) {
    JDBCHelper.updateResources(connectionFactory, request, predicate);
  }

  @Override
  public void updateComponents(Request request, Predicate predicate) {
    JDBCHelper.updateResources(connectionFactory, request, predicate);
  }

  @Override
  public void updateHosts(Request request, Predicate predicate) {
    JDBCHelper.updateResources(connectionFactory, request, predicate);
  }

  @Override
  public void updateHostComponents(Request request, Predicate predicate) {
    JDBCHelper.updateResources(connectionFactory, request, predicate);
  }

  @Override
  public void deleteClusters(Predicate predicate) {
    JDBCHelper.deleteResources(connectionFactory, predicate);
  }

  @Override
  public void deleteServices(Predicate predicate) {
    JDBCHelper.deleteResources(connectionFactory, predicate);
  }

  @Override
  public void deleteComponents(Predicate predicate) {
    JDBCHelper.deleteResources(connectionFactory, predicate);
  }

  @Override
  public void deleteHosts(Predicate predicate) {
    JDBCHelper.deleteResources(connectionFactory, predicate);
  }

  @Override
  public void deleteHostComponents(Predicate predicate) {
    JDBCHelper.deleteResources(connectionFactory, predicate);
  }
}
