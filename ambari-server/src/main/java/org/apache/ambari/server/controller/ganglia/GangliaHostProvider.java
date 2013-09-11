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
package org.apache.ambari.server.controller.ganglia;

import org.apache.ambari.server.controller.spi.SystemException;

/**
 *  Provider of Ganglia host information.
 */
public interface GangliaHostProvider {

  /**
   * Get the Ganglia server host name for the given cluster name.
   *
   * @param clusterName  the cluster name
   *
   * @return the Ganglia server
   *
   * @throws SystemException if unable to get the Ganglia server host name
   */
  public String getGangliaCollectorHostName(String clusterName) throws SystemException;
  
  /**
   * Get the status of Ganglia server host for the given cluster name.
   *
   * @param clusterName the cluster name
   *
   * @return true if heartbeat with Ganglia server host wasn't lost
   *
   * @throws SystemException if unable to get the status of Ganglia server host
   */
  public boolean isGangliaCollectorHostLive(String clusterName) throws SystemException;
  
  /**
   * Get the status of Ganglia server component for the given cluster name.
   *
   * @param clusterName the cluster name
   *
   * @return true if Ganglia server component is started
   *
   * @throws SystemException if unable to get the status of Ganglia server component
   */
  public boolean isGangliaCollectorComponentLive(String clusterName) throws SystemException;
}
