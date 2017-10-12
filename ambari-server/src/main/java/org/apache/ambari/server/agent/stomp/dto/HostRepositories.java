package org.apache.ambari.server.agent.stomp.dto;

import java.util.Map;

import org.apache.ambari.server.agent.CommandRepository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
