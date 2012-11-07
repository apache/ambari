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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.List;

public class StackInfo {
  private String name;
  private String version;
  private List<RepositoryInfo> repositories;
  private List<ServiceInfo> services;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<RepositoryInfo> getRepositories() {
    if( repositories == null ) repositories = new ArrayList<RepositoryInfo>();
    return repositories;
  }

  public void setRepositories(List<RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public synchronized List<ServiceInfo> getServices() {
    if (services == null) services = new ArrayList<ServiceInfo>();
    return services;
  }

  public synchronized void setServices(List<ServiceInfo> services) {
    this.services = services;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Stack name:" + name + "\nversion:" + version );//TODO add repository
    if (services != null) {
      sb.append("\n\t\tService:");
      for (ServiceInfo service : services) {
        sb.append("\t\t" + service.toString());
      }
    }

    if (repositories != null) {
      sb.append("\n\t\tRepositories:");
      for (RepositoryInfo repository : repositories) {
        sb.append("\t\t" + repository.toString());
      }
    }

    return sb.toString();
  }
}
