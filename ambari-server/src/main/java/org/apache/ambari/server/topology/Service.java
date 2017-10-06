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


import org.apache.ambari.server.controller.internal.Stack;

import java.util.Set;

public class Service {

  private final String type;

  private final String name;

  private final Stack stack;

  private final Configuration configuration;

  private final Set<Service> dependentServices;

  public Service(String type, Stack stack) {
    this(type, null, stack, null, null);
  }

  /**
   * In case there's no name specified name will be set to type.
   * @param type
   * @param name
   * @param stack
   * @param configuration
   */
  public Service(String type, String name, Stack stack, Configuration configuration, Set<Service> dependentServices) {
    this.type = type;
    if (name == null) {
      this.name = type;
    } else {
      this.name = name;
    }
    this.stack = stack;
    this.configuration = configuration;
    this.dependentServices = dependentServices;
  }

  /**
   * Gets the name of this service
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }

  public String getType() {
    return type;
  }

  public Stack getStack() {
    return stack;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
