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

package org.apache.ambari.server.topology;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigGroupRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.controller.internal.ConfigGroupResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.host.HostImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.server.controller.AmbariServer.getController;

/**
 * Represents a set of requests to a single host such as install, start, etc.
 */
public class HostRequest implements Comparable<HostRequest> {

  private long requestId;
  private String blueprint;
  private HostGroup hostGroup;
  private String hostgroupName;
  private Predicate predicate;
  private int cardinality = -1;
  private String hostname = null;
  private String cluster;
  private boolean containsMaster;
  private long stageId = -1;
  //todo: should be able to use the presence of hostName for this
  private boolean outstanding = true;

  //todo: remove
  private Map<String, Long> logicalInstallTaskIds = new HashMap<String, Long>();
  //todo: remove
  private Map<String, Long> logicalStartTaskIds = new HashMap<String, Long>();

  Collection<HostRoleCommand> logicalTasks = new ArrayList<HostRoleCommand>();

  // logical task id -> physical tasks
  private Map<Long, Collection<Long>> physicalTasks = new HashMap<Long, Collection<Long>>();

  private static HostResourceProvider hostResourceProvider;

  private HostComponentResourceProvider hostComponentResourceProvider;

  private AmbariManagementController controller = getController();
  private ActionManager actionManager = controller.getActionManager();
  private ConfigHelper configHelper = controller.getConfigHelper();
  private AmbariMetaInfo metaInfoManager = controller.getAmbariMetaInfo();

  //todo: temporary refactoring step
  private TopologyManager.ClusterTopologyContext topologyContext;

  private static HostRoleCommandFactory hostRoleCommandFactory;

  public static void init(HostRoleCommandFactory factory) {
    hostRoleCommandFactory = factory;
  }

  public HostRequest(long requestId, long stageId, String cluster, String blueprintName, HostGroup hostGroup,
                     int cardinality, Predicate predicate, TopologyManager.ClusterTopologyContext topologyContext) {
    this.requestId = requestId;
    this.stageId = stageId;
    this.cluster = cluster;
    this.blueprint = blueprintName;
    this.hostGroup = hostGroup;
    this.hostgroupName = hostGroup.getName();
    this.cardinality = cardinality;
    this.predicate = predicate;
    this.containsMaster = hostGroup.containsMasterComponent();
    this.topologyContext = topologyContext;

    createTasks();
    System.out.println("HostRequest: Created request: Host Association Pending");
  }

  public HostRequest(long requestId, long stageId, String cluster, String blueprintName, HostGroup hostGroup,
                     String hostname, Predicate predicate, TopologyManager.ClusterTopologyContext topologyContext) {
    this.requestId = requestId;
    this.stageId = stageId;
    this.cluster = cluster;
    this.blueprint = blueprintName;
    this.hostGroup = hostGroup;
    this.hostgroupName = hostGroup.getName();
    this.hostname = hostname;
    this.predicate = predicate;
    this.containsMaster = hostGroup.containsMasterComponent();
    this.topologyContext = topologyContext;

    createTasks();
    System.out.println("HostRequest: Created request for host: " + hostname);
  }

  //todo: synchronization
  public synchronized HostOfferResponse offer(HostImpl host) {
    if (! outstanding) {
      return new HostOfferResponse(HostOfferResponse.Answer.DECLINED_DONE);
    }
    if (matchesHost(host)) {
      outstanding = false;
      hostname = host.getHostName();
      List<TopologyTask> tasks = provision(host);

      return new HostOfferResponse(HostOfferResponse.Answer.ACCEPTED, hostGroup.getName(), tasks);
    } else {
      return new HostOfferResponse(HostOfferResponse.Answer.DECLINED_PREDICATE);
    }
  }

  public void setHostName(String hostName) {
    this.hostname = hostName;
  }

  public long getRequestId() {
    return requestId;
  }

  public String getClusterName() {
    return cluster;
  }
  public String getBlueprint() {
    return blueprint;
  }

  public HostGroup getHostGroup() {
    return hostGroup;
  }

  public String getHostgroupName() {
    return hostgroupName;
  }

  public int getCardinality() {
    return cardinality;
  }

  public Predicate getPredicate() {
    return predicate;
  }


  private List<TopologyTask> provision(HostImpl host) {
    List<TopologyTask> tasks = new ArrayList<TopologyTask>();

    tasks.add(new CreateHostResourcesTask(topologyContext.getClusterTopology(), host, getHostgroupName()));
    setHostOnTasks(host);

    HostGroup hostGroup = getHostGroup();
    tasks.add(new ConfigureConfigGroup(getConfigurationGroupName(hostGroup.getBlueprintName(),
        hostGroup.getName()), getClusterName(), hostname));

    tasks.add(getInstallTask());
    tasks.add(getStartTask());

    return tasks;
  }

  private void createTasks() {
    HostGroup hostGroup = getHostGroup();
    for (String component : hostGroup.getComponents()) {
      if (component == null || component.equals("AMBARI_SERVER")) {
        System.out.printf("Skipping component %s when creating request\n", component);
        continue;
      }

      String hostName = getHostName() != null ?
          getHostName() :
          "PENDING HOST ASSIGNMENT : HOSTGROUP=" + getHostgroupName();

      HostRoleCommand installTask = hostRoleCommandFactory.create(hostName, Role.valueOf(component), null, RoleCommand.INSTALL);
      installTask.setStatus(HostRoleStatus.PENDING);
      installTask.setTaskId(topologyContext.getNextTaskId());
      installTask.setRequestId(getRequestId());
      installTask.setStageId(stageId);

      //todo: had to add requestId to ShortTaskStatus
      //todo: revert addition of requestId when we are using LogicalTask
      installTask.setRequestId(getRequestId());

      logicalTasks.add(installTask);
      registerLogicalInstallTaskId(component, installTask.getTaskId());

      Stack stack = hostGroup.getStack();
      try {
        // if component isn't a client, add a start task
        if (! metaInfoManager.getComponent(stack.getName(), stack.getVersion(), stack.getServiceForComponent(component), component).isClient()) {
          HostRoleCommand startTask = hostRoleCommandFactory.create(hostName, Role.valueOf(component), null, RoleCommand.START);
          startTask.setStatus(HostRoleStatus.PENDING);
          startTask.setRequestId(getRequestId());
          startTask.setTaskId(topologyContext.getNextTaskId());
          startTask.setRequestId(getRequestId());
          startTask.setStageId(stageId);
          logicalTasks.add(startTask);
          registerLogicalStartTaskId(component, startTask.getTaskId());
        }
      } catch (AmbariException e) {
        e.printStackTrace();
        //todo: how to handle
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Get a config group name based on a bp and host group.
   *
   * @param bpName         blueprint name
   * @param hostGroupName  host group name
   * @return  config group name
   */
  protected String getConfigurationGroupName(String bpName, String hostGroupName) {
    return String.format("%s:%s", bpName, hostGroupName);
  }

  private void setHostOnTasks(HostImpl host) {
    for (HostRoleCommand task : getTasks()) {
      task.setHostEntity(host.getHostEntity());
    }
  }

  //todo: analyze all all configuration needs for dealing with deprecated properties
  /**
   * Since global configs are deprecated since 1.7.0, but still supported.
   * We should automatically map any globals used, to *-env dictionaries.
   *
   * @param blueprintConfigurations  map of blueprint configurations keyed by type
   */
  private void handleGlobalsBackwardsCompability(Stack stack,
                                                 Map<String, Map<String, String>> blueprintConfigurations) {

    StackId stackId = new StackId(stack.getName(), stack.getVersion());
    configHelper.moveDeprecatedGlobals(stackId, blueprintConfigurations, getClusterName());
  }

  public Collection<HostRoleCommand> getTasks() {
    // sync logical task state with physical tasks
    for (HostRoleCommand logicalTask : logicalTasks) {
      Collection<Long> physicalTaskIds = physicalTasks.get(logicalTask.getTaskId());
      if (physicalTaskIds != null) {
        //todo: for now only one physical task per logical task
        long physicalTaskId = physicalTaskIds.iterator().next();
        HostRoleCommand physicalTask = actionManager.getTaskById(physicalTaskId);
        if (physicalTask != null) {
          logicalTask.setStatus(physicalTask.getStatus());
          logicalTask.setCommandDetail(physicalTask.getCommandDetail());
          logicalTask.setCustomCommandName(physicalTask.getCustomCommandName());
          //todo: once we retry on failures, start/end times could span multiple physical tasks
          logicalTask.setStartTime(physicalTask.getStartTime());
          logicalTask.setEndTime(physicalTask.getEndTime());
          logicalTask.setErrorLog(physicalTask.getErrorLog());
          logicalTask.setExitCode(physicalTask.getExitCode());
          logicalTask.setExecutionCommandWrapper(physicalTask.getExecutionCommandWrapper());
          //todo: may be handled at a higher level than physical task
          logicalTask.setLastAttemptTime(physicalTask.getLastAttemptTime());
          logicalTask.setOutputLog(physicalTask.getOutputLog());
          logicalTask.setStderr(physicalTask.getStderr());
          logicalTask.setStdout(physicalTask.getStdout());
          logicalTask.setStructuredOut(physicalTask.getStructuredOut());
        }
      }
    }
    return logicalTasks;
  }

  public Collection<HostRoleCommandEntity> getTaskEntities() {
    Collection<HostRoleCommandEntity> taskEntities = new ArrayList<HostRoleCommandEntity>();
    for (HostRoleCommand task : logicalTasks) {
      HostRoleCommandEntity entity = task.constructNewPersistenceEntity();
      // the above method doesn't set all of the fields for some unknown reason
      entity.setRequestId(task.getRequestId());
      entity.setStageId(task.getStageId());
      entity.setTaskId(task.getTaskId());
      entity.setOutputLog(task.getOutputLog());
      entity.setErrorLog(task.errorLog);

      // set state from physical task
      Collection<Long> physicalTaskIds = physicalTasks.get(task.getTaskId());
      if (physicalTaskIds != null) {
        //todo: for now only one physical task per logical task
        long physicalTaskId = physicalTaskIds.iterator().next();
        HostRoleCommand physicalTask = actionManager.getTaskById(physicalTaskId);
        if (physicalTask != null) {
          entity.setStatus(physicalTask.getStatus());
          entity.setCommandDetail(physicalTask.getCommandDetail());
          entity.setCustomCommandName(physicalTask.getCustomCommandName());
          //todo: once we retry on failures, start/end times could span multiple physical tasks
          entity.setStartTime(physicalTask.getStartTime());
          entity.setEndTime(physicalTask.getEndTime());
          entity.setErrorLog(physicalTask.getErrorLog());
          entity.setExitcode(physicalTask.getExitCode());
          //todo: may be handled at a higher level than physical task
          entity.setLastAttemptTime(physicalTask.getLastAttemptTime());
          entity.setOutputLog(physicalTask.getOutputLog());
          entity.setStdError(physicalTask.getStderr().getBytes());
          entity.setStdOut(physicalTask.getStdout().getBytes());
          entity.setStructuredOut(physicalTask.getStructuredOut().getBytes());
        }
      }

      taskEntities.add(entity);
    }
    return taskEntities;
  }

  public boolean containsMaster() {
    return containsMaster;
  }

  public boolean matchesHost(HostImpl host) {
    if (hostname != null) {
      return host.getHostName().equals(hostname);
    } else if (predicate != null) {
      return predicate.evaluate(new HostResourceAdapter(host));
    } else {
      return true;
    }
  }

  public String getHostName() {
    return hostname;
  }

  public long getStageId() {
    return stageId;
  }

  //todo: remove
  private void registerLogicalInstallTaskId(String component, long taskId) {
    logicalInstallTaskIds.put(component, taskId);
  }

  //todo: remove
  private void registerLogicalStartTaskId(String component, long taskId) {
    logicalStartTaskIds.put(component, taskId);
  }

  //todo: remove
  private long getLogicalInstallTaskId(String component) {
    return logicalInstallTaskIds.get(component);
  }

  //todo: remove
  private long getLogicalStartTaskId(String component) {
    return logicalStartTaskIds.get(component);
  }

  //todo: since this is used to determine equality, using hashCode() isn't safe as it can return the same
  //todo: value for 2 unequal requests
  @Override
  public int compareTo(HostRequest other) {
    if (containsMaster()) {
      return other.containsMaster() ? hashCode() - other.hashCode() : -1;
    } else if (other.containsMaster()) {
      return 1;
    } else return hashCode() - other.hashCode();
  }

  //todo: once we have logical tasks, move tracking of physical tasks there
  public void registerPhysicalTaskId(long logicalTaskId, long physicalTaskId) {
    Collection<Long> physicalTasksForId = physicalTasks.get(logicalTaskId);
    if (physicalTasksForId == null) {
      physicalTasksForId = new HashSet<Long>();
      physicalTasks.put(logicalTaskId, physicalTasksForId);
    }
    physicalTasksForId.add(physicalTaskId);
  }

  //todo: temporary step
  public TopologyTask getInstallTask() {
    return new InstallHostTask();
  }

  //todo: temporary step
  public TopologyTask getStartTask() {
    return new StartHostTask();
  }

  //todo: temporary refactoring step
  public HostGroupInfo createHostGroupInfo(HostGroup group) {
    HostGroupInfo info = new HostGroupInfo(group.getName());
    info.setConfiguration(group.getConfiguration());

    return info;
  }

  private synchronized HostResourceProvider getHostResourceProvider() {
    if (hostResourceProvider == null) {
      hostResourceProvider = (HostResourceProvider)
          ClusterControllerHelper.getClusterController().ensureResourceProvider(Resource.Type.Host);

    }
    return hostResourceProvider;
  }

  private synchronized HostComponentResourceProvider getHostComponentResourceProvider() {
    if (hostComponentResourceProvider == null) {
      hostComponentResourceProvider = (HostComponentResourceProvider)
          ClusterControllerHelper.getClusterController().ensureResourceProvider(Resource.Type.HostComponent);
    }
    return hostComponentResourceProvider;
  }

  //todo: extract
  private class InstallHostTask implements TopologyTask {
    //todo: use future to obtain returned Response which contains the request id
    //todo: error handling
    //todo: monitor status of requests

    @Override
    public Type getType() {
      return Type.INSTALL;
    }

    @Override
    public void run() {
      try {
        System.out.println("HostRequest.InstallHostTask: Executing INSTALL task for host: " + hostname);
        RequestStatusResponse response = getHostResourceProvider().install(getHostName(), cluster);
        // map logical install tasks to physical install tasks
        List<ShortTaskStatus> underlyingTasks = response.getTasks();
        for (ShortTaskStatus task : underlyingTasks) {
          Long logicalInstallTaskId = getLogicalInstallTaskId(task.getRole());
          //todo: for now only one physical task per component
          long taskId = task.getTaskId();
          //physicalTasks.put(logicalInstallTaskId, Collections.singleton(taskId));
          registerPhysicalTaskId(logicalInstallTaskId, taskId);

          //todo: move this to provision
          //todo: shouldn't have to iterate over all tasks to find install task
          //todo: we are doing the same thing in the above registerPhysicalTaskId() call
          // set attempt count on task
          for (HostRoleCommand logicalTask : logicalTasks) {
            if (logicalTask.getTaskId() == logicalInstallTaskId) {
              logicalTask.incrementAttemptCount();
            }
          }
        }
      } catch (ResourceAlreadyExistsException e) {
        e.printStackTrace();
      } catch (SystemException e) {
        e.printStackTrace();
      } catch (NoSuchParentResourceException e) {
        e.printStackTrace();
      } catch (UnsupportedPropertyException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  //todo: extract
  private class StartHostTask implements TopologyTask {
    //todo: use future to obtain returned Response which contains the request id
    //todo: error handling
    //todo: monitor status of requests

    @Override
    public Type getType() {
      return Type.START;
    }

    @Override
    public void run() {
      try {
        System.out.println("HostRequest.StartHostTask: Executing START task for host: " + hostname);
        RequestStatusResponse response = getHostComponentResourceProvider().start(cluster, hostname);
        // map logical install tasks to physical install tasks
        List<ShortTaskStatus> underlyingTasks = response.getTasks();
        for (ShortTaskStatus task : underlyingTasks) {
          String component = task.getRole();
          Long logicalStartTaskId = getLogicalStartTaskId(component);
          // for now just set on outer map
          registerPhysicalTaskId(logicalStartTaskId, task.getTaskId());

          //todo: move this to provision
          // set attempt count on task
          for (HostRoleCommand logicalTask : logicalTasks) {
            if (logicalTask.getTaskId() == logicalStartTaskId) {
              logicalTask.incrementAttemptCount();
            }
          }
        }
      } catch (SystemException e) {
        e.printStackTrace();
      } catch (UnsupportedPropertyException e) {
        e.printStackTrace();
      } catch (NoSuchParentResourceException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private class CreateHostResourcesTask implements TopologyTask {
    private ClusterTopology topology;
    private HostImpl host;
    private String groupName;

    public CreateHostResourcesTask(ClusterTopology topology, HostImpl host, String groupName) {
      this.topology = topology;
      this.host = host;
      this.groupName = groupName;
    }

    @Override
    public Type getType() {
      return Type.RESOURCE_CREATION;
    }

    @Override
    public void run() {
      try {
        createHostResources();
      } catch (AmbariException e) {
        //todo: report error to caller
        e.printStackTrace();
        System.out.println("An error occurred when creating host resources: " + e.toString());
      }
    }

    private void createHostResources() throws AmbariException {
      Map<String, Object> properties = new HashMap<String, Object>();
      properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, getClusterName());
      properties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, host.getHostName());
      properties.put(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID, host.getRackInfo());

      getHostResourceProvider().createHosts(new RequestImpl(null, Collections.singleton(properties), null, null));
      createHostComponentResources();
    }

    private void createHostComponentResources() throws AmbariException {
      Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
      Stack stack = topology.getBlueprint().getStack();
      for (String component : topology.getBlueprint().getHostGroup(groupName).getComponents()) {
        //todo: handle this in a generic manner.  These checks are all over the code
        if (! component.equals("AMBARI_SERVER")) {
          requests.add(new ServiceComponentHostRequest(topology.getClusterName(),
              stack.getServiceForComponent(component), component, host.getHostName(), null));
        }
      }

      controller.createHostComponents(requests);
    }
  }

  //todo: extract
  private class ConfigureConfigGroup implements TopologyTask {
    private String groupName;
    private String clusterName;
    private String hostName;

    public ConfigureConfigGroup(String groupName, String clusterName, String hostName) {
      this.groupName = groupName;
      this.clusterName = clusterName;
      this.hostName = hostName;
    }

    @Override
    public Type getType() {
      return Type.CONFIGURE;
    }

    @Override
    public void run() {
      try {
        //todo: add task to offer response
        if (! addHostToExistingConfigGroups()) {
          createConfigGroupsAndRegisterHost();
        }
      } catch (Exception e) {
        //todo: handle exceptions
        e.printStackTrace();
        throw new RuntimeException("Unable to register config group for host: " + hostname);
      }
    }

    /**
     * Add the new host to an existing config group.
     *
     * @throws SystemException                an unknown exception occurred
     * @throws UnsupportedPropertyException   an unsupported property was specified in the request
     * @throws NoSuchParentResourceException  a parent resource doesn't exist
     */
    private boolean addHostToExistingConfigGroups()
        throws SystemException,
        UnsupportedPropertyException,
        NoSuchParentResourceException {

      boolean addedHost = false;

      Clusters clusters;
      Cluster cluster;
      try {
        clusters = controller.getClusters();
        cluster = clusters.getCluster(clusterName);
      } catch (AmbariException e) {
        throw new IllegalArgumentException(
            String.format("Attempt to add hosts to a non-existent cluster: '%s'", clusterName));
      }
      // I don't know of a method to get config group by name
      //todo: add a method to get config group by name
      Map<Long, ConfigGroup> configGroups = cluster.getConfigGroups();
      for (ConfigGroup group : configGroups.values()) {
        if (group.getName().equals(groupName)) {
          try {
            group.addHost(clusters.getHost(hostName));
            group.persist();
            addedHost = true;
          } catch (AmbariException e) {
            // shouldn't occur, this host was just added to the cluster
            throw new SystemException(String.format(
                "Unable to obtain newly created host '%s' from cluster '%s'", hostName, clusterName));
          }
        }
      }
      return addedHost;
    }

    /**
     * Register config groups for host group scoped configuration.
     * For each host group with configuration specified in the blueprint, a config group is created
     * and the hosts associated with the host group are assigned to the config group.
     *
     * @throws ResourceAlreadyExistsException attempt to create a config group that already exists
     * @throws SystemException                an unexpected exception occurs
     * @throws UnsupportedPropertyException   an invalid property is provided when creating a config group
     * @throws NoSuchParentResourceException  attempt to create a config group for a non-existing cluster
     */
    private void createConfigGroupsAndRegisterHost() throws
        ResourceAlreadyExistsException, SystemException,
        UnsupportedPropertyException, NoSuchParentResourceException {

      //HostGroupEntity entity = hostGroup.getEntity();
      HostGroup hostGroup = getHostGroup();
      Map<String, Map<String, Config>> groupConfigs = new HashMap<String, Map<String, Config>>();

      Stack stack = hostGroup.getStack();

      // get the host-group config with cluster creation template overrides
      Configuration topologyHostGroupConfig = topologyContext.getClusterTopology().
          getHostGroupInfo().get(hostGroup.getName()).getConfiguration();

      //handling backwards compatibility for group configs
      //todo: doesn't belong here
      handleGlobalsBackwardsCompability(stack, topologyHostGroupConfig.getProperties());

      // iterate over topo host group configs which were defined in CCT/HG and BP/HG only, no parent configs
      for (Map.Entry<String, Map<String, String>> entry: topologyHostGroupConfig.getProperties().entrySet()) {
        String type = entry.getKey();
        String service = stack.getServiceForConfigType(type);
        Config config = new ConfigImpl(type);
        config.setTag(hostGroup.getName());
        config.setProperties(entry.getValue());
        //todo: attributes
        Map<String, Config> serviceConfigs = groupConfigs.get(service);
        if (serviceConfigs == null) {
          serviceConfigs = new HashMap<String, Config>();
          groupConfigs.put(service, serviceConfigs);
        }
        serviceConfigs.put(type, config);
      }

      String bpName = topologyContext.getClusterTopology().getBlueprint().getName();
      for (Map.Entry<String, Map<String, Config>> entry : groupConfigs.entrySet()) {
        String service = entry.getKey();
        Map<String, Config> serviceConfigs = entry.getValue();
        String absoluteGroupName = getConfigurationGroupName(bpName, hostGroup.getName());
        Collection<String> groupHosts;

        groupHosts = topologyContext.getClusterTopology().getHostGroupInfo().
            get(hostgroupName).getHostNames();

        ConfigGroupRequest request = new ConfigGroupRequest(
            null, getClusterName(), absoluteGroupName, service, "Host Group Configuration",
            new HashSet<String>(groupHosts), serviceConfigs);

        // get the config group provider and create config group resource
        ConfigGroupResourceProvider configGroupProvider = (ConfigGroupResourceProvider)
            ClusterControllerHelper.getClusterController().ensureResourceProvider(Resource.Type.ConfigGroup);
        configGroupProvider.createResources(Collections.singleton(request));
      }
    }


  }

  private class HostResourceAdapter implements Resource {
    Resource hostResource;

    public HostResourceAdapter(HostImpl host) {
      buildPropertyMap(host);
    }

    @Override
    public Object getPropertyValue(String id) {
      return hostResource.getPropertyValue(id);
    }

    @Override
    public Map<String, Map<String, Object>> getPropertiesMap() {
      return hostResource.getPropertiesMap();
    }

    @Override
    public Type getType() {
      return Type.Host;
    }

    @Override
    public void addCategory(String id) {
      // read only, nothing to do
    }

    @Override
    public void setProperty(String id, Object value) {
      // read only, nothing to do
    }

    private void buildPropertyMap(HostImpl host) {
      hostResource = new ResourceImpl(Resource.Type.Host);

      hostResource.setProperty(HostResourceProvider.HOST_NAME_PROPERTY_ID,
          host.getHostName());
      hostResource.setProperty(HostResourceProvider.HOST_PUBLIC_NAME_PROPERTY_ID,
          host.getPublicHostName());
      hostResource.setProperty(HostResourceProvider.HOST_IP_PROPERTY_ID,
          host.getIPv4());
      hostResource.setProperty(HostResourceProvider.HOST_TOTAL_MEM_PROPERTY_ID,
          host.getTotalMemBytes());
      hostResource.setProperty(HostResourceProvider.HOST_CPU_COUNT_PROPERTY_ID,
          (long) host.getCpuCount());
      hostResource.setProperty(HostResourceProvider.HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID,
          (long) host.getPhCpuCount());
      hostResource.setProperty(HostResourceProvider.HOST_OS_ARCH_PROPERTY_ID,
          host.getOsArch());
      hostResource.setProperty(HostResourceProvider.HOST_OS_TYPE_PROPERTY_ID,
          host.getOsType());
      hostResource.setProperty(HostResourceProvider.HOST_OS_FAMILY_PROPERTY_ID,
          host.getOsFamily());
      hostResource.setProperty(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID,
          host.getRackInfo());
      hostResource.setProperty(HostResourceProvider.HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID,
          host.getLastHeartbeatTime());
      hostResource.setProperty(HostResourceProvider.HOST_LAST_AGENT_ENV_PROPERTY_ID,
          host.getLastAgentEnv());
      hostResource.setProperty(HostResourceProvider.HOST_LAST_REGISTRATION_TIME_PROPERTY_ID,
          host.getLastRegistrationTime());
      hostResource.setProperty(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID,
          host.getStatus());
      hostResource.setProperty(HostResourceProvider.HOST_HOST_HEALTH_REPORT_PROPERTY_ID,
          host.getHealthStatus().getHealthReport());
      hostResource.setProperty(HostResourceProvider.HOST_DISK_INFO_PROPERTY_ID,
          host.getDisksInfo());
      hostResource.setProperty(HostResourceProvider.HOST_STATE_PROPERTY_ID,
          host.getState());
    }      
  }
}
