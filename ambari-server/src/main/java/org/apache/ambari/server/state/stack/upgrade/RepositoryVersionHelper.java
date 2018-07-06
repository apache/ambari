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
import java.util.List;
import java.util.Map.Entry;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
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
  private Provider<Configuration> configuration;

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
    Long mpackId = serviceGroup.getMpackId();
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

      addCommandRepositoryToContext(context, mpack, osEntity);

    } catch (AmbariException ambariException) {
      throw new SystemException(ambariException.getMessage(), ambariException);
    }
  }

  /**
   * Adds a command repository to the action context for the supplied mpack.  This is
   * primarily called when distributing packages of a different mpack for a compatible
   * service group
   * @param context
   *          the context
   * @param mpack
   *          the target mpack
   * @param osEntity
   *          the OS family
   * @throws SystemException
   */
  public void addCommandRepositoryToContext(ActionExecutionContext context,
      Mpack mpack, RepoOsEntity osEntity) throws SystemException {
    try {
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
