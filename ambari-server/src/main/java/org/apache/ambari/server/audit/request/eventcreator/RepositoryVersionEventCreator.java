/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ChangeRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles privilege requests
 * For resource type {@link Resource.Type#Repository}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class RepositoryVersionEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.RepositoryVersion).build();

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return resourceTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {
    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

    switch (request.getRequestType()) {
      case POST:
        return AddRepositoryVersionRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withStackName(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "stack_name")))
          .withStackVersion(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "stack_version")))
          .withDisplayName(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "display_name")))
          .withRepoVersion(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "repository_version")))
          .withRepos(getRepos(request))
          .build();
      case PUT:
        return ChangeRepositoryVersionRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withStackName(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "stack_name")))
          .withStackVersion(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "stack_version")))
          .withDisplayName(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "display_name")))
          .withRepoVersion(getProperty(request, PropertyHelper.getPropertyId("RepositoryVersions", "repository_version")))
          .withRepos(getRepos(request))
          .build();
      case DELETE:
        return DeleteRepositoryVersionRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withStackName(request.getResource().getKeyValueMap().get(Resource.Type.Stack))
          .withStackVersion(request.getResource().getKeyValueMap().get(Resource.Type.StackVersion))
          .withRepoVersion(request.getResource().getKeyValueMap().get(Resource.Type.RepositoryVersion))
          .build();
      default:
        return null;
    }
  }

  /**
   * Assembles repositories from the request
   * operating system -> list of repositories where the repository is a map of properties (repo_id, repo_name, base_url)
   * @param request
   * @return a map of repositories
   */
  private Map<String, List<Map<String, String>>> getRepos(Request request) {

    Map<String, List<Map<String, String>>> result = new HashMap<String, List<Map<String, String>>>();

    if (!request.getBody().getPropertySets().isEmpty()) {
      if (request.getBody().getPropertySets().iterator().next().get("operating_systems") instanceof Set) {
        Set<Object> set = (Set<Object>) request.getBody().getPropertySets().iterator().next().get("operating_systems");

        result = createResultForOperationSystems(set);
      }
    }
    return result;
  }

  /**
   * Returns repos for the set of operating systems
   * @param set
   * @return
   */
  private Map<String, List<Map<String, String>>> createResultForOperationSystems(Set<Object> set) {
    Map<String, List<Map<String, String>>> result = new HashMap<String, List<Map<String, String>>>();
    for (Object entry : set) {
      if (entry instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) entry;
        String osType = (String) map.get(PropertyHelper.getPropertyId("OperatingSystems", "os_type"));
        if (!result.containsKey(osType)) {
          result.put(osType, new LinkedList<Map<String, String>>());
        }
        if (map.get("repositories") instanceof Set) {
          Set<Object> repos = (Set<Object>) map.get("repositories");
          for (Object repo : repos) {
            if (repo instanceof Map) {
              Map<String, String> resultMap = buildResultRepo((Map<String, String>) repo);
              result.get(osType).add(resultMap);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns a map with the repository properties (repo_id, repo_name, base_url)
   * @param repo
   * @return
   */
  private Map<String, String> buildResultRepo(Map<String, String> repo) {
    Map<String, String> m = repo;
    String repoId = m.get(PropertyHelper.getPropertyId("Repositories", "repo_id"));
    String repo_name = m.get(PropertyHelper.getPropertyId("Repositories", "repo_name"));
    String baseUrl = m.get(PropertyHelper.getPropertyId("Repositories", "base_url"));
    Map<String, String> resultMap = new HashMap<>();
    resultMap.put("repo_id", repoId);
    resultMap.put("repo_name", repo_name);
    resultMap.put("base_url", baseUrl);
    return resultMap;
  }

  /**
   * Returns property from the request based on the propertyId parameter
   * @param request
   * @param properyId
   * @return
   */
  private String getProperty(Request request, String properyId) {
    if (!request.getBody().getPropertySets().isEmpty()) {
      return String.valueOf(request.getBody().getPropertySets().iterator().next().get(properyId));
    }
    return null;
  }


}
