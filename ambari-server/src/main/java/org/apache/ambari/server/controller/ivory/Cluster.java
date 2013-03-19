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
package org.apache.ambari.server.controller.ivory;

import java.util.Map;
import java.util.Set;

/**
 * Ivory cluster.
 */
public class Cluster {

  private final String name;
  private final String colo;
  private final Set<String> interfaces;
  private final Set<String> locations;
  private final Map<String, String> properties;

  /**
   * Construct a cluster.
   *
   * @param name        the cluster name
   * @param colo        the colo
   * @param interfaces  the interfaces
   * @param locations   the locations
   * @param properties  the properties
   */
  public Cluster(String name, String colo, Set<String> interfaces, Set<String> locations, Map<String, String> properties) {
    this.name = name;
    this.colo = colo;
    this.interfaces = interfaces;
    this.locations = locations;
    this.properties = properties;
  }

  /**
   * Get the cluster name.
   *
   * @return the cluster name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the colo.
   *
   * @return the colo
   */
  public String getColo() {
    return colo;
  }

  /**
   * Get the interfaces.
   *
   * @return the interfaces
   */
  public Set<String> getInterfaces() {
    return interfaces;
  }

  /**
   * Get the locations.
   *
   * @return the locations
   */
  public Set<String> getLocations() {
    return locations;
  }

  /**
   * Get the properties.
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Cluster cluster = (Cluster) o;

    return !(colo != null       ? !colo.equals(cluster.colo)             : cluster.colo != null) &&
           !(interfaces != null ? !interfaces.equals(cluster.interfaces) : cluster.interfaces != null) &&
           !(locations != null  ? !locations.equals(cluster.locations)   : cluster.locations != null) &&
           !(name != null       ? !name.equals(cluster.name)             : cluster.name != null) &&
           !(properties != null ? !properties.equals(cluster.properties) : cluster.properties != null);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (colo != null ? colo.hashCode() : 0);
    result = 31 * result + (interfaces != null ? interfaces.hashCode() : 0);
    result = 31 * result + (locations != null ? locations.hashCode() : 0);
    result = 31 * result + (properties != null ? properties.hashCode() : 0);
    return result;
  }
}
