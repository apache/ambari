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

package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.Map;

/**
 * Host Group definition.
 */
public interface HostGroup {

  /**
   * Get the host group name.
   *
   * @return host group name
   */
  public String getName();

  /**
   * Get associated host information.
   *
   * @return collection of hosts associated with the host group
   */
  public Collection<String> getHostInfo();

  /**
   * Get the components associated with the host group.
   *
   * @return  collection of component names for the host group
   */
  public Collection<String> getComponents();

  /**
   * Get the configurations associated with the host group.
   *
   * @return map of configuration type to a map of properties
   */
  public Map<String, Map<String, String>> getConfigurationProperties();
}
