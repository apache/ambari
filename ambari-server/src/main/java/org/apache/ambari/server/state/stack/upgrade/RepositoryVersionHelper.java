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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.OperatingSystemReadOnlyResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.OsSpecific;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.RepoTag;
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

  @Inject
  private Provider<OsFamily> os_family;

  @Inject Provider<Clusters> clusters;

  /**
   * Used to retrieve management packs.
   */
  @Inject
  private Provider<AmbariMetaInfo> ambariMetainfoProvider;

  /**
   * Used for retrieving mpacks by their ID.
   */
  @Inject
  private MpackDAO mpackDAO;

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
  public List<RepoOsEntity> parseOperatingSystems(String repositoriesJson) throws Exception {
    final List<RepoOsEntity> operatingSystems = new ArrayList<>();
    final JsonArray rootJson = new JsonParser().parse(repositoriesJson).getAsJsonArray();
    for (JsonElement operatingSystemJson: rootJson) {
      JsonObject osObj = operatingSystemJson.getAsJsonObject();

      final RepoOsEntity operatingSystemEntity = new RepoOsEntity();

      operatingSystemEntity.setFamily(osObj.get(OperatingSystemReadOnlyResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID).getAsString());

      if (osObj.has(OperatingSystemReadOnlyResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS)) {
        operatingSystemEntity.setAmbariManaged(osObj.get(
            OperatingSystemReadOnlyResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS).getAsBoolean());
      }

      for (JsonElement repositoryElement: osObj.get(RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID).getAsJsonArray()) {
        final RepoDefinitionEntity repositoryEntity = new RepoDefinitionEntity();
        final JsonObject repositoryJson = repositoryElement.getAsJsonObject();
        repositoryEntity.setBaseUrl(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID).getAsString());
        repositoryEntity.setRepoName(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID).getAsString());
        repositoryEntity.setRepoID(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID).getAsString());
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID) != null) {
          repositoryEntity.setDistribution(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID) != null) {
          repositoryEntity.setComponents(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID) != null) {
          repositoryEntity.setMirrors(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID) != null) {
          repositoryEntity.setUnique(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID).getAsBoolean());
        }

        if (null != repositoryJson.get(RepositoryResourceProvider.REPOSITORY_TAGS_PROPERTY_ID)) {
          Set<RepoTag> tags = new HashSet<>();

          JsonArray jsonArray = repositoryJson.get(RepositoryResourceProvider.REPOSITORY_TAGS_PROPERTY_ID).getAsJsonArray();
          for(JsonElement je : jsonArray) {
            tags.add(RepoTag.valueOf(je.getAsString()));
          }
          repositoryEntity.setTags(tags);
        }

        operatingSystemEntity.addRepoDefinition(repositoryEntity);
      }
      operatingSystems.add(operatingSystemEntity);
    }
    return operatingSystems;
  }

  public List<RepoOsEntity> createRepoOsEntities(List<RepositoryInfo> repositories) {

    List<RepoOsEntity> repoOsEntities = new ArrayList<>();
    final Multimap<String, RepositoryInfo> operatingSystems = ArrayListMultimap.create();
    for (RepositoryInfo repository : repositories) {
      operatingSystems.put(repository.getOsType(), repository);
    }
    for (Entry<String, Collection<RepositoryInfo>> operatingSystem : operatingSystems.asMap().entrySet()) {
      RepoOsEntity operatingSystemEntity = new RepoOsEntity();
      List<RepoDefinitionEntity> repositoriesList = new ArrayList<>();
      for (RepositoryInfo repository : operatingSystem.getValue()) {
        RepoDefinitionEntity repositoryDefinition = RepoDefinitionEntity.from(repository);
        repositoriesList.add(repositoryDefinition);
        operatingSystemEntity.setAmbariManaged(repository.isAmbariManagedRepositories());
      }
      operatingSystemEntity.addRepoDefinitionEntities(repositoriesList);
      operatingSystemEntity.setFamily(operatingSystem.getKey());
      repoOsEntities.add(operatingSystemEntity);
    }
    return repoOsEntities;
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

    List<OsSpecific.Package> packages = new ArrayList<>();

    for (String serviceName : servicesOnHost) {
      ServiceInfo serviceInfo;
      StackInfo stackInfo;

      try {
        if (ami.get().isServiceRemovedInStack(stackId.getStackName(), stackId.getStackVersion(), serviceName)) {
          LOG.info(String.format("%s has been removed from stack %s-%s. Skip calculating its installation packages", stackId.getStackName(), stackId.getStackVersion(), serviceName));
          continue; //No need to calculate install packages for removed services
        }

        stackInfo = ami.get().getStack(stackId);
        serviceInfo = ami.get().getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
      } catch (AmbariException e) {
        throw new SystemException(String.format("Cannot obtain stack information for %s-%s", stackId.getStackName(), stackId.getStackVersion()), e);
      }

      List<OsSpecific.Package> packagesForStackService = amc.getPackagesForStackServiceHost(stackInfo, serviceInfo,
        new HashMap<>(), osFamily);

      List<String> blacklistedPackagePrefixes = configuration.get().getRollingUpgradeSkipPackagesPrefixes();
      for (OsSpecific.Package aPackage : packagesForStackService) {
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
   * Return repositories available for target os version on host based on the
   * mpack and host family.
   *
   * @param mpackEntity
   *          the management pack to get the repo for.
   * @param host
   *          target {@link Host} for providing repositories list
   *
   * @return {@link RepoOsEntity} with available repositories for host
   * @throws SystemException
   *           if no repository available for target {@link Host}
   */
  public RepoOsEntity getOSEntityForHost(MpackEntity mpackEntity, Host host)
      throws SystemException {
    String osFamily = host.getOsFamily();
    RepoOsEntity osEntity = null;
    for (RepoOsEntity operatingSystem : mpackEntity.getRepositoryOperatingSystems()) {
      if (osFamily.equals(operatingSystem.getFamily())) {
        osEntity = operatingSystem;
        break;
      }
    }

    if (null == osEntity) {
      throw new SystemException(
          String.format("Operating System matching %s could not be found for mpack with ID %s",
              osFamily, mpackEntity.getId()));
    }

    return osEntity;
  }

  /**
   * Return repositories available for target os version on host based on the
   * host family.
   *
   * @param operatingSystems
   *          the list of repository operating systems to use when finding a
   *          match for the host's OS family.
   * @param host
   *          target {@link Host} for providing repositories list
   *
   * @return {@link RepoOsEntity} with available repositories for host
   * @throws SystemException
   *           if no repository available for target {@link Host}
   */
  public RepoOsEntity getOSEntityForHost(List<RepoOsEntity> operatingSystems, Host host)
      throws SystemException {
    String osFamily = host.getOsFamily();
    RepoOsEntity osEntity = null;
    for (RepoOsEntity operatingSystem : operatingSystems) {
      if (osFamily.equals(operatingSystem.getFamily())) {
        osEntity = operatingSystem;
        break;
      }
    }

    if (null == osEntity) {
      throw new SystemException(
          String.format("Operating System matching %s could not be found", osFamily));
    }

    return osEntity;
  }

  /**
   * Adds a command repository to the action context
   * @param osEntity      the OS family
   */
  public CommandRepository getCommandRepository(Mpack mpack, RepoOsEntity osEntity)
      throws AmbariException {
    final CommandRepository commandRepo = new CommandRepository();
    final boolean sysPreppedHost = configuration.get().areHostsSysPrepped().equalsIgnoreCase("true");

    commandRepo.setRepositories(osEntity.getFamily(), osEntity.getRepoDefinitionEntities());
    commandRepo.setMpackId(mpack.getResourceId());
    commandRepo.setMpackName(mpack.getName());
    commandRepo.setMpackVersion(mpack.getVersion());
    commandRepo.getFeature().setPreInstalled(configuration.get().areHostsSysPrepped());
    commandRepo.getFeature().setIsScoped(!sysPreppedHost);

    if (!osEntity.isAmbariManaged()) {
      commandRepo.setNonManaged();
    } else {
      commandRepo.setRepoFileName(mpack.getName(), mpack.getResourceId());
      commandRepo.setUniqueSuffix(String.format("-repo-%s", mpack.getMpackId()));
    }

    if (configuration.get().arePackagesLegacyOverridden()) {
      LOG.warn("Legacy override option is turned on, disabling CommandRepositoryFeature.scoped feature");
      commandRepo.getFeature().setIsScoped(false);
    }
    return commandRepo;
  }


  /**
   * Builds repository information for inclusion in a command.  This replaces escaping json on
   * a command.
   *
   * @param cluster the cluster
   * @param host    the host
   * @param component {@link ServiceComponent} object, could be null to return service-related repository
   * @return  the command repository
   * @throws SystemException
   */
  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public CommandRepository getCommandRepository(final Cluster cluster, ServiceComponent component, final Host host)
      throws AmbariException, SystemException {

    AmbariMetaInfo ambariMetaInfo = ambariMetainfoProvider.get();

    long serviceGroupId = component.getServiceGroupId();
    ServiceGroup serviceGroup = cluster.getServiceGroup(serviceGroupId);
    long mpackId = serviceGroup.getMpackId();
    MpackEntity mpackEntity = mpackDAO.findById(mpackId);
    Mpack mpack = ambariMetaInfo.getMpack(mpackId);

    RepoOsEntity osEntity = getOSEntityForHost(mpackEntity, host);
    return getCommandRepository(mpack, osEntity);
  }

  /**
   * Adds a command repository to the action context
   * @param context       the context
   * @param osEntity      the OS family
   */
  public void addCommandRepositoryToContext(ActionExecutionContext context,
      RepoOsEntity osEntity) throws SystemException {

    AmbariMetaInfo ambariMetaInfo = ambariMetainfoProvider.get();

    try {
      final Cluster cluster = clusters.get().getCluster(context.getClusterName());
      ServiceGroup serviceGroup = cluster.getServiceGroup(context.getExpectedServiceGroupName());
      long mpackId = serviceGroup.getMpackId();
      Mpack mpack = ambariMetaInfo.getMpack(mpackId);

      final CommandRepository commandRepo = getCommandRepository(mpack, osEntity);

      context.addVisitor(command -> {
        if (null == command.getRepositoryFile()) {
          command.setRepositoryFile(commandRepo);
        }
      });
    } catch (AmbariException ambariException) {
      throw new SystemException(ambariException.getMessage(), ambariException);
    }
  }

  /** Get repository info given a cluster and host.
   *
   * @param cluster  the cluster
   * @param host     the host
   *
   * @return the repo info
   *
   * @throws AmbariException if the repository information can not be obtained
   */
  public String getRepoInfoString(Cluster cluster, ServiceComponent component, Host host) throws AmbariException, SystemException {
    return gson.toJson(getCommandRepository(cluster, component, host));
  }
}
