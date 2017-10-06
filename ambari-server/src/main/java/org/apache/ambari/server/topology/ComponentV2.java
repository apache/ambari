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

package org.apache.ambari.server.topology;


import org.apache.ambari.server.controller.internal.ProvisionAction;

public class ComponentV2 {

  private final String name;

  private final Service service;

  private final ProvisionAction provisionAction;

  private final Configuration configuration;


  public ComponentV2(String name, Service service) {
    this(name, service, null, null);
  }

  public ComponentV2(String name, Service service, ProvisionAction provisionAction, Configuration configuration) {
    this.name = name;
    this.service = service;
    this.provisionAction = provisionAction;
    this.configuration = configuration;
  }

  /**
   * Gets the name of this component
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the provision action associated with this component.
   *
   * @return the provision action for this component, which
   *         may be null if the default action is to be used
   */
  public ProvisionAction getProvisionAction() {
    return this.provisionAction;
  }

  public Service getService() {
    return service;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
