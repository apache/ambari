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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.OperatingSystemResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provides helper methods to manage repository versions.
 */
@Singleton
public class RepositoryVersionHelper {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryVersionHelper.class);

  @Inject
  private Gson gson;

  @Inject(optional = true)
  private AmbariMetaInfo ambariMetaInfo;

  /**
   * Parses operating systems json to a list of entities. Expects json like:
   * <pre>
   * [
   *    {
   *       "repositories":[
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP-UTILS",
   *             "Repositories/repo_id":"HDP-UTILS-1.1.0.20"
   *          },
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP",
   *             "Repositories/repo_id":"HDP-2.2"
   *          }
   *       ],
   *       "OperatingSystems/os_type":"redhat6"
   *    }
   * ]
   * </pre>
   * @param repositoriesJson operating systems json
   * @return list of operating system entities
   * @throws Exception if any kind of json parsing error happened
   */
  public List<OperatingSystemEntity> parseOperatingSystems(String repositoriesJson) throws Exception {
    final List<OperatingSystemEntity> operatingSystems = new ArrayList<OperatingSystemEntity>();
    final JsonArray rootJson = new JsonParser().parse(repositoriesJson).getAsJsonArray();
    for (JsonElement operatingSystemJson: rootJson) {
      final OperatingSystemEntity operatingSystemEntity = new OperatingSystemEntity();
      operatingSystemEntity.setOsType(operatingSystemJson.getAsJsonObject().get(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID).getAsString());
      for (JsonElement repositoryJson: operatingSystemJson.getAsJsonObject().get(RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID).getAsJsonArray()) {
        final RepositoryEntity repositoryEntity = new RepositoryEntity();
        repositoryEntity.setBaseUrl(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID).getAsString());
        repositoryEntity.setName(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID).getAsString());
        repositoryEntity.setRepositoryId(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID).getAsString());
        operatingSystemEntity.getRepositories().add(repositoryEntity);
      }
      operatingSystems.add(operatingSystemEntity);
    }
    return operatingSystems;
  }

  /**
   * Serializes repository info to json for storing to DB.
   * Produces json like:
   * <pre>
   * [
   *    {
   *       "repositories":[
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP-UTILS",
   *             "Repositories/repo_id":"HDP-UTILS-1.1.0.20"
   *          },
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP",
   *             "Repositories/repo_id":"HDP-2.2"
   *          }
   *       ],
   *       "OperatingSystems/os_type":"redhat6"
   *    }
   * ]
   * </pre>
   *
   * @param repositories list of repository infos
   * @return serialized list of operating systems
   */
  public String serializeOperatingSystems(List<RepositoryInfo> repositories) {
    final JsonArray rootJson = new JsonArray();
    final Multimap<String, RepositoryInfo> operatingSystems = ArrayListMultimap.create();
    for (RepositoryInfo repository: repositories) {
      operatingSystems.put(repository.getOsType(), repository);
    }
    for (Entry<String, Collection<RepositoryInfo>> operatingSystem : operatingSystems.asMap().entrySet()) {
      final JsonObject operatingSystemJson = new JsonObject();
      final JsonArray repositoriesJson = new JsonArray();
      for (RepositoryInfo repository : operatingSystem.getValue()) {
        final JsonObject repositoryJson = new JsonObject();
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, repository.getBaseUrl());
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID, repository.getRepoName());
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID, repository.getRepoId());
        repositoriesJson.add(repositoryJson);
      }
      operatingSystemJson.add(RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID, repositoriesJson);
      operatingSystemJson.addProperty(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, operatingSystem.getKey());
      rootJson.add(operatingSystemJson);
    }
    return gson.toJson(rootJson);
  }

  /**
   * Scans the given stack for upgrade packages which can be applied to update the cluster to given repository version.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @param repositoryVersion target repository version
   * @param upgradeType if not {@code null} null, will only return upgrade packs whose type matches.
   * @return upgrade pack name
   * @throws AmbariException if no upgrade packs suit the requirements
   */
  public String getUpgradePackageName(String stackName, String stackVersion, String repositoryVersion, UpgradeType upgradeType) throws AmbariException {
    final Map<String, UpgradePack> upgradePacks = ambariMetaInfo.getUpgradePacks(stackName, stackVersion);
    for (UpgradePack upgradePack : upgradePacks.values()) {
      final String upgradePackName = upgradePack.getName();

      if (null != upgradeType && upgradePack.getType() != upgradeType) {
        continue;
      }

      // check that upgrade pack has <target> node
      if (StringUtils.isBlank(upgradePack.getTarget())) {
        LOG.error("Upgrade pack " + upgradePackName + " is corrupted, it should contain <target> node");
        continue;
      }
      if (upgradePack.canBeApplied(repositoryVersion)) {
        return upgradePackName;
      }
    }
    throw new AmbariException("There were no suitable upgrade packs for stack " + stackName + " " + stackVersion +
        ((null != upgradeType) ? " and upgrade type " + upgradeType : ""));
  }
}
