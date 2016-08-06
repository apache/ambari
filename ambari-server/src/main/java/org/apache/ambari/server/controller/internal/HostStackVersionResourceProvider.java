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
package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Resource provider for host stack versions resources.
 */
@StaticallyInject
public class HostStackVersionResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String HOST_STACK_VERSION_ID_PROPERTY_ID              = PropertyHelper.getPropertyId("HostStackVersions", "id");
  protected static final String HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("HostStackVersions", "cluster_name");
  protected static final String HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("HostStackVersions", "host_name");
  protected static final String HOST_STACK_VERSION_STACK_PROPERTY_ID           = PropertyHelper.getPropertyId("HostStackVersions", "stack");
  protected static final String HOST_STACK_VERSION_VERSION_PROPERTY_ID         = PropertyHelper.getPropertyId("HostStackVersions", "version");
  protected static final String HOST_STACK_VERSION_STATE_PROPERTY_ID           = PropertyHelper.getPropertyId("HostStackVersions", "state");
  protected static final String HOST_STACK_VERSION_REPOSITORIES_PROPERTY_ID    = PropertyHelper.getPropertyId("HostStackVersions", "repositories");
  protected static final String HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID    = PropertyHelper.getPropertyId("HostStackVersions", "repository_version");

  protected static final String INSTALL_PACKAGES_ACTION = "install_packages";
  protected static final String INSTALL_PACKAGES_FULL_NAME = "Install version";


  private static Set<String> pkPropertyIds = Sets.newHashSet(
          HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_ID_PROPERTY_ID,
          HOST_STACK_VERSION_STACK_PROPERTY_ID,
          HOST_STACK_VERSION_VERSION_PROPERTY_ID,
          HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);

  private static Set<String> propertyIds = Sets.newHashSet(
          HOST_STACK_VERSION_ID_PROPERTY_ID,
          HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_STACK_PROPERTY_ID,
          HOST_STACK_VERSION_VERSION_PROPERTY_ID,
          HOST_STACK_VERSION_STATE_PROPERTY_ID,
          HOST_STACK_VERSION_REPOSITORIES_PROPERTY_ID,
          HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);

  private static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.Cluster, HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      put(Type.Host, HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID);
      put(Type.HostStackVersion, HOST_STACK_VERSION_ID_PROPERTY_ID);
      put(Type.Stack, HOST_STACK_VERSION_STACK_PROPERTY_ID);
      put(Type.StackVersion, HOST_STACK_VERSION_VERSION_PROPERTY_ID);
      put(Type.RepositoryVersion, HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);
    }
  };

  @Inject
  private static HostVersionDAO hostVersionDAO;

  @Inject
  private static RepositoryVersionDAO repositoryVersionDAO;

  private static Gson gson = StageUtils.getGson();

  @Inject
  private static StageFactory stageFactory;

  @Inject
  private static RequestFactory requestFactory;

  @Inject
  private static Provider<AmbariActionExecutionHelper> actionExecutionHelper;

  @Inject
  private static Configuration configuration;


  /**
   * Constructor.
   */
  public HostStackVersionResourceProvider(
          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (Map<String, Object> propertyMap: propertyMaps) {
      final String hostName = propertyMap.get(HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID).toString();
      String clusterName = null;
      if (propertyMap.containsKey(HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID)) {
        clusterName = propertyMap.get(HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID).toString();
      }
      final Long id;
      List<HostVersionEntity> requestedEntities;
      if (propertyMap.get(HOST_STACK_VERSION_ID_PROPERTY_ID) == null) {
        if (clusterName == null) {
          requestedEntities = hostVersionDAO.findByHost(hostName);
        } else {
          requestedEntities = hostVersionDAO.findByClusterAndHost(clusterName, hostName);
        }
      } else {
        try {
          id = Long.parseLong(propertyMap.get(HOST_STACK_VERSION_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("Stack version should have numerical id");
        }
        final HostVersionEntity entity = hostVersionDAO.findByPK(id);
        if (entity == null) {
          throw new NoSuchResourceException("There is no stack version with id " + id);
        } else {
          requestedEntities = Collections.singletonList(entity);
        }
      }
      if (requestedEntities != null) {
        addRequestedEntities(resources, requestedEntities, requestedIds, clusterName);
      }
    }

    return resources;
  }


  /**
   * Adds requested entities to resources
   * @param resources a list of resources to add to
   * @param requestedEntities requested entities
   * @param requestedIds
   * @param clusterName name of cluster or null if no any
   */
  public void addRequestedEntities(Set<Resource> resources,
                                   List<HostVersionEntity> requestedEntities,
                                   Set<String> requestedIds,
                                   String clusterName) {
    for (HostVersionEntity entity: requestedEntities) {
      StackId stackId = new StackId(entity.getRepositoryVersion().getStack());

      RepositoryVersionEntity repoVerEntity = repositoryVersionDAO.findByStackAndVersion(
          stackId, entity.getRepositoryVersion().getVersion());

      final Resource resource = new ResourceImpl(Resource.Type.HostStackVersion);

      setResourceProperty(resource, HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, entity.getHostName(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_STACK_PROPERTY_ID, stackId.getStackName(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_VERSION_PROPERTY_ID, stackId.getStackVersion(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_STATE_PROPERTY_ID, entity.getState().name(), requestedIds);

      if (clusterName != null) {
        setResourceProperty(resource, HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
      }

      if (repoVerEntity != null) {
        Long repoVersionId = repoVerEntity.getId();
        setResourceProperty(resource, HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, repoVersionId, requestedIds);
      }

      resources.add(resource);
    }
  }


  @Override
  public RequestStatus createResources(Request request) throws SystemException,
          UnsupportedPropertyException, ResourceAlreadyExistsException,
          NoSuchParentResourceException {
    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    String hostName;
    final String desiredRepoVersion;
    String stackName;
    String stackVersion;
    if (request.getProperties().size() > 1) {
      throw new UnsupportedOperationException("Multiple requests cannot be executed at the same time.");
    }

    Map<String, Object> propertyMap  = iterator.next();

    Set<String> requiredProperties = Sets.newHashSet(
            HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID,
            HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID,
            HOST_STACK_VERSION_STACK_PROPERTY_ID,
            HOST_STACK_VERSION_VERSION_PROPERTY_ID);

    for (String requiredProperty : requiredProperties) {
      Validate.isTrue(propertyMap.containsKey(requiredProperty),
              String.format("The required property %s is not defined", requiredProperty));
    }

    String clName = (String) propertyMap.get(HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
    hostName = (String) propertyMap.get(HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID);
    desiredRepoVersion = (String) propertyMap.get(HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);

    Host host;
    try {
      host = getManagementController().getClusters().getHost(hostName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(
              String.format("Can not find host %s", hostName), e);
    }
    AmbariManagementController managementController = getManagementController();
    AmbariMetaInfo ami = managementController.getAmbariMetaInfo();

    stackName = (String) propertyMap.get(HOST_STACK_VERSION_STACK_PROPERTY_ID);
    stackVersion = (String) propertyMap.get(HOST_STACK_VERSION_VERSION_PROPERTY_ID);
    final StackId stackId = new StackId(stackName, stackVersion);
    if (!ami.isSupportedStack(stackName, stackVersion)) {
      throw new NoSuchParentResourceException(String.format("Stack %s is not supported",
              stackId));
    }

    Set<Cluster> clusterSet;
    if (clName == null) {
      try {
        clusterSet = getManagementController().getClusters().getClustersForHost(hostName);
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format((
                "Host %s does not belong to any cluster"
        ), hostName), e);
      }
    } else {
      Cluster cluster;
      try {
        cluster = getManagementController().getClusters().getCluster(clName);
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format((
                "Cluster %s does not exist"
        ), clName), e);
      }
      clusterSet = Collections.singleton(cluster);
    }

    Cluster cluster;
    if (clusterSet.isEmpty()) {
      throw new UnsupportedOperationException(String.format("Host %s belongs " +
        "to 0 clusters with stack id %s. Performing %s action failed.",
        hostName, stackId, INSTALL_PACKAGES_FULL_NAME));
    } else if (clusterSet.size() > 1) {
      throw new UnsupportedOperationException(String.format("Host %s belongs " +
        "to %d clusters with stack id %s. Performing %s action on multiple " +
        "clusters is not supported", hostName, clusterSet.size(), stackId,
        INSTALL_PACKAGES_FULL_NAME));
    } else {
      cluster = clusterSet.iterator().next();
    }

    RepositoryVersionEntity repoVersionEnt = repositoryVersionDAO.findByStackAndVersion(stackId, desiredRepoVersion);
    if (repoVersionEnt==null) {
      throw new IllegalArgumentException(String.format(
              "Repo version %s is not available for stack %s",
              desiredRepoVersion, stackId));
    }

    HostVersionEntity hostVersEntity = hostVersionDAO.findByClusterStackVersionAndHost(clName, stackId,
            desiredRepoVersion, hostName);
    if (hostVersEntity == null) {
      throw new IllegalArgumentException(String.format(
        "Repo version %s for stack %s is not available for host %s",
        desiredRepoVersion, stackId, hostName));
    }
    if (hostVersEntity.getState() != RepositoryVersionState.INSTALLED &&
            hostVersEntity.getState() != RepositoryVersionState.INSTALL_FAILED &&
            hostVersEntity.getState() != RepositoryVersionState.OUT_OF_SYNC) {
      throw new UnsupportedOperationException(String.format("Repo version %s for stack %s " +
        "for host %s is in %s state. Can not transition to INSTALLING state",
              desiredRepoVersion, stackId, hostName, hostVersEntity.getState().toString()));
    }

    List<OperatingSystemEntity> operatingSystems = repoVersionEnt.getOperatingSystems();
    Map<String, List<RepositoryEntity>> perOsRepos = new HashMap<>();
    for (OperatingSystemEntity operatingSystem : operatingSystems) {
      perOsRepos.put(operatingSystem.getOsType(), operatingSystem.getRepositories());
    }

    // Determine repositories for host
    String osFamily = host.getOsFamily();
    final List<RepositoryEntity> repoInfo = perOsRepos.get(osFamily);
    if (repoInfo == null) {
      throw new SystemException(String.format("Repositories for os type %s are " +
                      "not defined. Repo version=%s, stackId=%s",
        osFamily, desiredRepoVersion, stackId));
    }
    // For every host at cluster, determine packages for all installed services
    List<ServiceOsSpecific.Package> packages = new ArrayList<>();
    Set<String> servicesOnHost = new HashSet<>();
    List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
    for (ServiceComponentHost component : components) {
      servicesOnHost.add(component.getServiceName());
    }
    List<String> blacklistedPackagePrefixes = configuration.getRollingUpgradeSkipPackagesPrefixes();
    for (String serviceName : servicesOnHost) {
      ServiceInfo info;
      try {
        info = ami.getService(stackName, stackVersion, serviceName);
      } catch (AmbariException e) {
        throw new SystemException("Can not enumerate services", e);
      }
      List<ServiceOsSpecific.Package> packagesForService = managementController.getPackagesForServiceHost(info,
              new HashMap<String, String>(), // Contents are ignored
        osFamily);
      for (ServiceOsSpecific.Package aPackage : packagesForService) {
        if (! aPackage.getSkipUpgrade()) {
          boolean blacklisted = false;
          for(String prefix : blacklistedPackagePrefixes) {
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
    final String packageList = gson.toJson(packages);
    final String repoList = gson.toJson(repoInfo);

    Map<String, String> params = new HashMap<String, String>(){{
      put("stack_id", stackId.getStackId());
      put("repository_version", desiredRepoVersion);
      put("base_urls", repoList);
      put(KeyNames.PACKAGE_LIST, packageList);
    }};

    VersionDefinitionXml xml = null;
    try {
      xml = repoVersionEnt.getRepositoryXml();
    } catch (Exception e) {
      throw new SystemException(String.format("Could not load xml from repo version %s",
          repoVersionEnt.getVersion()));
    }

    if (null != xml && StringUtils.isNotBlank(xml.getPackageVersion(osFamily))) {
      params.put(KeyNames.PACKAGE_VERSION, xml.getPackageVersion(osFamily));
    }


    // Create custom action
    RequestResourceFilter filter = new RequestResourceFilter(null, null,
            Collections.singletonList(hostName));

    ActionExecutionContext actionContext = new ActionExecutionContext(
            cluster.getClusterName(), INSTALL_PACKAGES_ACTION,
            Collections.singletonList(filter),
            params);
    actionContext.setTimeout(Short.valueOf(configuration.getDefaultAgentTaskTimeout(true)));

    String caption = String.format(INSTALL_PACKAGES_FULL_NAME + " on host %s", hostName);
    RequestStageContainer req = createRequest(caption);

    Map<String, String> hostLevelParams = new HashMap<>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());

    // Generate cluster host info
    String clusterHostInfoJson;
    try {
      clusterHostInfoJson = StageUtils.getGson().toJson(
        StageUtils.getClusterHostInfo(cluster));
    } catch (AmbariException e) {
      throw new SystemException("Could not build cluster topology", e);
    }

    Stage stage = stageFactory.createNew(req.getId(),
            "/tmp/ambari",
            cluster.getClusterName(),
            cluster.getClusterId(),
            caption,
            clusterHostInfoJson,
            "{}",
            StageUtils.getGson().toJson(hostLevelParams));

    long stageId = req.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    req.addStages(Collections.singletonList(stage));

    try {
      actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null);
    } catch (AmbariException e) {
      throw new SystemException("Can not modify stage", e);
    }

    try {
      hostVersEntity.setState(RepositoryVersionState.INSTALLING);
      hostVersionDAO.merge(hostVersEntity);

      cluster.recalculateClusterVersionState(repoVersionEnt);
      req.persist();
    } catch (AmbariException e) {
      throw new SystemException("Can not persist request", e);
    }
    return getRequestStatus(req.getRequestStatusResponse());
  }


  private RequestStageContainer createRequest(String caption) {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
            actionManager.getNextRequestId(), null, requestFactory, actionManager);
    requestStages.setRequestContext(caption);

    return requestStages;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Method not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Method not supported");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }
}
