/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ambari.server.agent.stomp.dto;

import java.util.Map;

import org.apache.ambari.server.agent.CommandRepository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostRepositories {

  @JsonProperty("commandRepos")
  private Map<Long, CommandRepository> repositories;

  @JsonProperty("componentRepos")
  private Map<String, Long> componentRepos;

  public HostRepositories(Map<Long, CommandRepository> repositories, Map<String, Long> componentRepos) {
    this.repositories = repositories;
    this.componentRepos = componentRepos;
  }

  public Map<Long, CommandRepository> getRepositories() {
    return repositories;
  }

  public void setRepositories(Map<Long, CommandRepository> repositories) {
    this.repositories = repositories;
  }

  public Map<String, Long> getComponentRepos() {
    return componentRepos;
  }

  public void setComponentRepos(Map<String, Long> componentRepos) {
    this.componentRepos = componentRepos;
  }
}
