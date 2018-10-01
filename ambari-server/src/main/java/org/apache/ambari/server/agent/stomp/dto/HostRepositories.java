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

import java.util.SortedMap;

import org.apache.ambari.server.agent.CommandRepository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostRepositories {

  @JsonProperty("commandRepos")
  private SortedMap<Long, CommandRepository> repositories;

  @JsonProperty("componentRepos")
  private SortedMap<String, Long> componentRepos;

  public HostRepositories(SortedMap<Long, CommandRepository> repositories, SortedMap<String, Long> componentRepos) {
    this.repositories = repositories;
    this.componentRepos = componentRepos;
  }

  public SortedMap<Long, CommandRepository> getRepositories() {
    return repositories;
  }

  public SortedMap<String, Long> getComponentRepos() {
    return componentRepos;
  }
}
