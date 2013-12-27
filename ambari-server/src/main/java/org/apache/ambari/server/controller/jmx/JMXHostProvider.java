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
package org.apache.ambari.server.controller.jmx;

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;

/**
 * Provider of JMX host information.
 */
public interface JMXHostProvider {

  /**
   * Get the JMX host name for the given cluster name and component name.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   *
   * @return the JMX host name
   *
   * @throws SystemException if unable to get the JMX host name
   */
  public String getHostName(String clusterName, String componentName)
      throws SystemException;

  /**
   * Get the port for the specified cluster name and component.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   *
   * @return the port for the specified cluster name and component
   *
   * @throws SystemException if unable to get the JMX port
   */
  public String getPort(String clusterName, String componentName)
      throws SystemException;
  
  /**
   * Get the protocol for the specified cluster name and component.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   *
   * @return the JMX protocol for the specified cluster name and component, one of http or https
   *
   */
  public String getJMXProtocol(String clusterName, String componentName) ;
  
  
}
