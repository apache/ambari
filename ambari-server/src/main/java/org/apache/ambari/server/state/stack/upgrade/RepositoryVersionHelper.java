/*
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.ActionExecutionContext.ExecutionCommandVisitor;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.OperatingSystemResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
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
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Provides helper methods to manage repository versions.
 */
@Singleton
public class RepositoryVersionHelper {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryVersionHelper.class);

  @Inject
  private Gson gson;

  @Inject
  private Provider<AmbariMetaInfo> ami;

  @Inject
  private Provider<Configuration> configuration;

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
    final List<OperatingSystemEntity> operatingSystems = new ArrayList<>();
    final JsonArray rootJson = new JsonParser().parse(repositoriesJson).getAsJsonArray();
    for (JsonElement operatingSystemJson: rootJson) {
      JsonObject osObj = operatingSystemJson.getAsJsonObject();

      final OperatingSystemEntity operatingSystemEntity = new OperatingSystemEntity();

      operatingSystemEntity.setOsType(osObj.get(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID).getAsString());

      if (osObj.has(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS)) {
        operatingSystemEntity.setAmbariManagedRepos(osObj.get(
            OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS).getAsBoolean());
      }

      for (JsonElement repositoryElement: osObj.get(RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID).getAsJsonArray()) {
        final RepositoryEntity repositoryEntity = new RepositoryEntity();
        final JsonObject repositoryJson = repositoryElement.getAsJsonObject();
        repositoryEntity.setBaseUrl(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID).getAsString());
        repositoryEntity.setName(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID).getAsString());
        repositoryEntity.setRepositoryId(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID).getAsString());
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID) != null) {
          repositoryEntity.setDistribution(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID) != null) {
          repositoryEntity.setComponents(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID) != null) {
          repositoryEntity.setMirrorsList(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID) != null) {
          repositoryEntity.setUnique(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID).getAsBoolean());
        }
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
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID, repository.getDistribution());
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID, repository.getComponents());
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID, repository.getMirrorsList());
        repositoryJson.addProperty(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID, repository.isUnique());
        repositoriesJson.add(repositoryJson);
        operatingSystemJson.addProperty(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS, repository.isAmbariManagedRepositories());
      }
      operatingSystemJson.add(RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID, repositoriesJson);
      operatingSystemJson.addProperty(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, operatingSystem.getKey());
      rootJson.add(operatingSystemJson);
    }
    return gson.toJson(rootJson);
  }

  public String serializeOperatingSystemEntities(List<OperatingSystemEntity> operatingSystems) {
    List<RepositoryInfo> repositoryInfos = new ArrayList<>();
    for (OperatingSystemEntity os: operatingSystems) {
      for (RepositoryEntity repositoryEntity: os.getRepositories()) {
        RepositoryInfo repositoryInfo = new RepositoryInfo();
        repositoryInfo.setRepoId(repositoryEntity.getRepositoryId());
        repositoryInfo.setRepoName(repositoryEntity.getName());
        repositoryInfo.setDistribution(repositoryEntity.getDistribution());
        repositoryInfo.setComponents(repositoryEntity.getComponents());
        repositoryInfo.setBaseUrl(repositoryEntity.getBaseUrl());
        repositoryInfo.setOsType(os.getOsType());
        repositoryInfo.setAmbariManagedRepositories(os.isAmbariManagedRepos());
        repositoryInfos.add(repositoryInfo);
      }
    }
    return serializeOperatingSystems(repositoryInfos);
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
    final Map<String, UpgradePack> upgradePacks = ami.get().getUpgradePacks(stackName, stackVersion);
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

  /**
   * Build the role parameters for an install command.
   *
   * @param amc           the management controller.  Tests don't use the same instance that gets injected.
   * @param repoVersion   the repository version
   * @param osFamily      the os family
   * @param servicesOnHost the set of services to check for packages
   * @return a Map<String, String> to use in
   */
  public Map<String, String> buildRoleParams(AmbariManagementController amc, RepositoryVersionEntity repoVersion, String osFamily, Set<String> servicesOnHost)
    throws SystemException {

    StackId stackId = repoVersion.getStackId();

    List<ServiceOsSpecific.Package> packages = new ArrayList<>();

    for (String serviceName : servicesOnHost) {
      ServiceInfo info;

      try {
        if (ami.get().isServiceRemovedInStack(stackId.getStackName(), stackId.getStackVersion(), serviceName)) {
          LOG.info(String.format("%s has been removed from stack %s-%s. Skip calculating its installation packages", stackId.getStackName(), stackId.getStackVersion(), serviceName));
          continue; //No need to calculate install packages for removed services
        }

        info = ami.get().getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
      } catch (AmbariException e) {
        throw new SystemException(String.format("Cannot obtain stack information for %s-%s", stackId.getStackName(), stackId.getStackVersion()), e);
      }

      List<ServiceOsSpecific.Package> packagesForService = amc.getPackagesForServiceHost(info,
        new HashMap<String, String>(), osFamily);

      List<String> blacklistedPackagePrefixes = configuration.get().getRollingUpgradeSkipPackagesPrefixes();

      for (ServiceOsSpecific.Package aPackage : packagesForService) {
        if (!aPackage.getSkipUpgrade()) {
          boolean blacklisted = false;
          for (String prefix : blacklistedPackagePrefixes) {
            if (aPackage.getName().startsWith(prefix)) {
              blacklisted = true;
              break;
            }
          }
          if (! blacklisted) {
            packages.add(aPackage);
          }
        }
      }
    }

    Map<String, String> roleParams = new HashMap<>();
    roleParams.put("stack_id", stackId.getStackId());
    // !!! TODO make roleParams <String, Object> so we don't have to do this awfulness.
    roleParams.put(KeyNames.PACKAGE_LIST, gson.toJson(packages));

    return roleParams;
  }

  /**
   * Adds a command repository to the action context
   * @param context       the context
   * @param osEntity      the OS family
   * @param repoVersion   the repository version entity
   */
  public void addCommandRepository(ActionExecutionContext context,
      RepositoryVersionEntity repoVersion, OperatingSystemEntity osEntity) {

    final CommandRepository commandRepo = new CommandRepository();
    boolean sysPreppedHost = configuration.get().areHostsSysPrepped().equalsIgnoreCase("true");

    commandRepo.setRepositories(osEntity.getOsType(), osEntity.getRepositories());
    commandRepo.setRepositoryVersion(repoVersion.getVersion());
    commandRepo.setRepositoryVersionId(repoVersion.getId());
    commandRepo.setStackName(repoVersion.getStackId().getStackName());
    commandRepo.getFeature().setPreInstalled(configuration.get().areHostsSysPrepped());
    commandRepo.getFeature().setIsScoped(!sysPreppedHost);

    if (!osEntity.isAmbariManagedRepos()) {
      commandRepo.setNonManaged();
    } else {
      if (repoVersion.isLegacy()){
        commandRepo.setLegacyRepoFileName(repoVersion.getStackName(), repoVersion.getVersion());
        commandRepo.setLegacyRepoId(repoVersion.getVersion());
        commandRepo.getFeature().setIsScoped(false);
      } else {
        commandRepo.setRepoFileName(repoVersion.getStackName(), repoVersion.getId());
        commandRepo.setUniqueSuffix(String.format("-repo-%s", repoVersion.getId()));
      }
    }

    if (configuration.get().arePackagesLegacyOverridden()) {
      LOG.warn("Legacy override option is turned on, disabling CommandRepositoryFeature.scoped feature");
      commandRepo.getFeature().setIsScoped(false);
    }

    context.addVisitor(new ExecutionCommandVisitor() {
      @Override
      public void visit(ExecutionCommand command) {
        if (null == command.getRepositoryFile()) {
          command.setRepositoryFile(commandRepo);
        }
      }
    });
  }


}