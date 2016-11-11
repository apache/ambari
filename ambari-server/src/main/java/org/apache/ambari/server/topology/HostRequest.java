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

package org.apache.ambari.server.topology;

import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;
import static org.apache.ambari.server.controller.internal.ProvisionAction.START_ONLY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyHostTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.ambari.server.state.host.HostImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a set of requests to a single host such as install, start, etc.
 */
public class HostRequest implements Comparable<HostRequest> {

  private final static Logger LOG = LoggerFactory.getLogger(HostRequest.class);

  private long requestId;
  private String blueprint;
  private HostGroup hostGroup;
  private String hostgroupName;
  private Predicate predicate;
  private String hostname = null;
  private long clusterId;
  private boolean containsMaster;
  private final long id;
  private boolean isOutstanding = true;
  private final boolean skipFailure;

  private Map<TopologyTask, Map<String, Long>> logicalTaskMap = new HashMap<TopologyTask, Map<String, Long>>();

  Map<Long, HostRoleCommand> logicalTasks = new HashMap<Long, HostRoleCommand>();

  // logical task id -> physical tasks
  private Map<Long, Long> physicalTasks = new HashMap<Long, Long>();

  private List<TopologyTask> topologyTasks = new ArrayList<TopologyTask>();

  private ClusterTopology topology;

  private static PredicateCompiler predicateCompiler = new PredicateCompiler();

  public HostRequest(long requestId, long id, long clusterId, String hostname, String blueprintName,
                     HostGroup hostGroup, Predicate predicate, ClusterTopology topology, boolean skipFailure) {
    this.requestId = requestId;
    this.id = id;
    this.clusterId = clusterId;
    blueprint = blueprintName;
    this.hostGroup = hostGroup;
    hostgroupName = hostGroup.getName();
    this.predicate = predicate;
    containsMaster = hostGroup.containsMasterComponent();
    this.topology = topology;
    this.skipFailure = skipFailure;
    createTasks(this.skipFailure);
    LOG.info("HostRequest: Created request for host: " +
        (hostname == null ? "Host Assignment Pending" : hostname));
  }

  /**
   * Only to be used when replaying persisted requests upon server startup.
   *
   * @param requestId    logical request id
   * @param id           host request id
   * @param predicate    host predicate
   * @param topology     cluster topology
   * @param entity       host request entity
   */
  public HostRequest(long requestId, long id, String predicate,
                     ClusterTopology topology, TopologyHostRequestEntity entity, boolean skipFailure) {

    this.requestId = requestId;
    this.id = id;
    clusterId = topology.getClusterId();
    blueprint = topology.getBlueprint().getName();
    hostgroupName = entity.getTopologyHostGroupEntity().getName();
    hostGroup = topology.getBlueprint().getHostGroup(hostgroupName);
    hostname = entity.getHostName();
    this.predicate = toPredicate(predicate);
    containsMaster = hostGroup.containsMasterComponent();
    this.topology = topology;
    this.skipFailure = skipFailure;

    createTasksForReplay(entity);

    //todo: we may be able to simplify by just checking hostname
    isOutstanding = hostname == null || !topology.getAmbariContext().
        isHostRegisteredWithCluster(clusterId, hostname);

    LOG.info("HostRequest: Successfully recovered host request for host: " +
        (hostname == null ? "Host Assignment Pending" : hostname));
  }

  //todo: synchronization
  public synchronized HostOfferResponse offer(HostImpl host) {
    if (!isOutstanding) {
      return HostOfferResponse.DECLINED_DUE_TO_DONE;
    }
    if (matchesHost(host)) {
      isOutstanding = false;
      hostname = host.getHostName();
      setHostOnTasks(host);
      return HostOfferResponse.createAcceptedResponse(id, hostGroup.getName(), topologyTasks);
    } else {
      return HostOfferResponse.DECLINED_DUE_TO_PREDICATE;
    }
  }

  public void setHostName(String hostName) {
    hostname = hostName;
  }

  public long getRequestId() {
    return requestId;
  }

  public long getClusterId() {
    return clusterId;
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

  public Predicate getPredicate() {
    return predicate;
  }

  public boolean isCompleted() {
    return ! isOutstanding;
  }

  public boolean shouldSkipFailure() {
    return skipFailure;
  }

  private void createTasks(boolean skipFailure) {
    // high level topology tasks such as INSTALL, START, ...
    topologyTasks.add(new PersistHostResourcesTask());
    topologyTasks.add(new RegisterWithConfigGroupTask());

    InstallHostTask installTask = new InstallHostTask(skipFailure);
    topologyTasks.add(installTask);
    logicalTaskMap.put(installTask, new HashMap<String, Long>());

    boolean skipStartTaskCreate = topology.getProvisionAction().equals(INSTALL_ONLY);
    boolean skipInstallTaskCreate = topology.getProvisionAction().equals(START_ONLY);

    StartHostTask startTask = null;
    if (!skipStartTaskCreate) {
      startTask = new StartHostTask(skipFailure);
      topologyTasks.add(startTask);
      logicalTaskMap.put(startTask, new HashMap<String, Long>());
    } else {
      LOG.info("Skipping Start task creation since provision action = " + topology.getProvisionAction());
    }

    // lower level logical component level tasks which get mapped to physical tasks
    HostGroup hostGroup = getHostGroup();
    Collection<String> startOnlyComponents = hostGroup.getComponentNames(START_ONLY);
    Collection<String> installOnlyComponents = hostGroup.getComponentNames(INSTALL_ONLY);
    Collection<String> installAndStartComponents = hostGroup.getComponentNames(INSTALL_AND_START);

    for (String component : hostGroup.getComponentNames()) {
      if (component == null || component.equals("AMBARI_SERVER")) {
        LOG.info("Skipping component {} when creating request\n", component);
        continue;
      }

      String hostName = getHostName() != null ?
          getHostName() :
          "PENDING HOST ASSIGNMENT : HOSTGROUP=" + getHostgroupName();

      AmbariContext context = topology.getAmbariContext();
      Stack stack = hostGroup.getStack();

      // Skip INSTALL task in case server component is marked as START_ONLY, or the cluster provision_action is
      // START_ONLY, unless component is marked with INSTALL_ONLY or INSTALL_AND_START.
      if (startOnlyComponents.contains(component) || (skipInstallTaskCreate &&
        !installOnlyComponents.contains(component) && !installAndStartComponents.contains(component))
          && stack != null && !stack.getComponentInfo(component).isClient()) {
        LOG.info("Skipping create of INSTALL task for {} on {}.", component, hostName);
      } else {
        HostRoleCommand logicalInstallTask = context.createAmbariTask(
          getRequestId(), id, component, hostName, AmbariContext.TaskType.INSTALL, skipFailure);
        logicalTasks.put(logicalInstallTask.getTaskId(), logicalInstallTask);
        logicalTaskMap.get(installTask).put(component, logicalInstallTask.getTaskId());
      }

      // Skip START task if component is a client, or ir marked as INSTALL_ONLY or cluster provision_action is
      // INSTALL_ONLY
      if (installOnlyComponents.contains(component) || skipStartTaskCreate ||
        (stack != null && stack.getComponentInfo(component).isClient())) {
        LOG.info("Skipping create of START task for {} on {}.", component, hostName);
      } else {
        HostRoleCommand logicalStartTask = context.createAmbariTask(
            getRequestId(), id, component, hostName, AmbariContext.TaskType.START, skipFailure);
        logicalTasks.put(logicalStartTask.getTaskId(), logicalStartTask);
        logicalTaskMap.get(startTask).put(component, logicalStartTask.getTaskId());
      }
    }
  }

  private void createTasksForReplay(TopologyHostRequestEntity entity) {
    topologyTasks.add(new PersistHostResourcesTask());
    topologyTasks.add(new RegisterWithConfigGroupTask());
    InstallHostTask installTask = new InstallHostTask(skipFailure);
    topologyTasks.add(installTask);
    logicalTaskMap.put(installTask, new HashMap<String, Long>());

    boolean skipStartTaskCreate = topology.getProvisionAction().equals(INSTALL_ONLY);

    if (!skipStartTaskCreate) {
      StartHostTask startTask = new StartHostTask(skipFailure);
      topologyTasks.add(startTask);
      logicalTaskMap.put(startTask, new HashMap<String, Long>());
    }

    AmbariContext ambariContext = topology.getAmbariContext();
    // lower level logical component level tasks which get mapped to physical tasks
    for (TopologyHostTaskEntity topologyTaskEntity : entity.getTopologyHostTaskEntities()) {
      TopologyTask.Type taskType = TopologyTask.Type.valueOf(topologyTaskEntity.getType());
      for (TopologyLogicalTaskEntity logicalTaskEntity : topologyTaskEntity.getTopologyLogicalTaskEntities()) {
        Long logicalTaskId = logicalTaskEntity.getId();
        String component = logicalTaskEntity.getComponentName();

        AmbariContext.TaskType logicalTaskType = getLogicalTaskType(taskType);
        HostRoleCommand task = ambariContext.createAmbariTask(logicalTaskId, getRequestId(), id,
            component, entity.getHostName(), logicalTaskType, skipFailure);

        logicalTasks.put(logicalTaskId, task);
        Long physicalTaskId = logicalTaskEntity.getPhysicalTaskId();
        if (physicalTaskId != null) {
          registerPhysicalTaskId(logicalTaskId, physicalTaskId);
        }

        //assumes only one task per type
        for (TopologyTask topologyTask : topologyTasks) {
          if (taskType == topologyTask.getType()) {
            logicalTaskMap.get(topologyTask).put(component, logicalTaskId);
          }
        }
      }
    }
  }

  private static AmbariContext.TaskType getLogicalTaskType(TopologyTask.Type topologyTaskType) {
    return topologyTaskType ==
        TopologyTask.Type.INSTALL ?
        AmbariContext.TaskType.INSTALL :
        AmbariContext.TaskType.START;
  }

  private void setHostOnTasks(HostImpl host) {
    for (HostRoleCommand task : getLogicalTasks()) {
      task.setHost(host.getHostId(), host.getHostName());
    }
  }

  public List<TopologyTask> getTopologyTasks() {
    return topologyTasks;
  }

  public Collection<HostRoleCommand> getLogicalTasks() {
    // sync logical task state with physical tasks
    for (HostRoleCommand logicalTask : logicalTasks.values()) {
      // set host on command detail if it is set to null
      String commandDetail = logicalTask.getCommandDetail();
      if (commandDetail != null && commandDetail.contains("null")) {
        logicalTask.setCommandDetail(commandDetail.replace("null", hostname));
      }
      Long physicalTaskId = physicalTasks.get(logicalTask.getTaskId());
      if (physicalTaskId != null) {
        HostRoleCommand physicalTask = topology.getAmbariContext().getPhysicalTask(physicalTaskId);
        if (physicalTask != null) {
          logicalTask.setStatus(physicalTask.getStatus());
          logicalTask.setCommandDetail(physicalTask.getCommandDetail());
          logicalTask.setCustomCommandName(physicalTask.getCustomCommandName());
          //todo: once we retry on failures, start/end times could span multiple physical tasks
          logicalTask.setStartTime(physicalTask.getStartTime());
          logicalTask.setOriginalStartTime(physicalTask.getOriginalStartTime());
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
    return logicalTasks.values();
  }

  public Map<String, Long> getLogicalTasksForTopologyTask(TopologyTask topologyTask) {
    return new HashMap<String, Long>(logicalTaskMap.get(topologyTask));
  }

  public HostRoleCommand getLogicalTask(long logicalTaskId) {
    return logicalTasks.get(logicalTaskId);
  }

  public Collection<HostRoleCommandEntity> getTaskEntities() {
    Collection<HostRoleCommandEntity> taskEntities = new ArrayList<HostRoleCommandEntity>();
    for (HostRoleCommand task : logicalTasks.values()) {
      HostRoleCommandEntity entity = task.constructNewPersistenceEntity();
      // the above method doesn't set all of the fields for some unknown reason
      entity.setOutputLog(task.getOutputLog());
      entity.setErrorLog(task.errorLog);

      // set state from physical task
      Long physicalTaskId = physicalTasks.get(task.getTaskId());
      if (physicalTaskId != null) {
        HostRoleCommand physicalTask = topology.getAmbariContext().getPhysicalTask(physicalTaskId);
        if (physicalTask != null) {
          entity.setStatus(physicalTask.getStatus());
          entity.setCommandDetail(physicalTask.getCommandDetail());
          entity.setCustomCommandName(physicalTask.getCustomCommandName());
          //todo: once we retry on failures, start/end times could span multiple physical tasks
          entity.setStartTime(physicalTask.getStartTime());
          entity.setOriginalStartTime(physicalTask.getOriginalStartTime());
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
    return (hostname != null) ?
        host.getHostName().equals(hostname) :
        predicate == null || predicate.evaluate(new HostResourceAdapter(host));
  }

  public String getHostName() {
    return hostname;
  }

  public long getId() {
    return id;
  }

  public long getStageId() {
    // stage id is same as host request id
    return getId();
  }

  public Long getPhysicalTaskId(long logicalTaskId) {
    return physicalTasks.get(logicalTaskId);
  }

  public Map<Long, Long> getPhysicalTaskMapping() {
    return new HashMap<>(physicalTasks);
  }

  //todo: since this is used to determine equality, using hashCode() isn't safe as it can return the same
  //todo: value for 2 unequal requests
  @Override
  public int compareTo(HostRequest other) {
    if (containsMaster()) {
      return other.containsMaster() ? hashCode() - other.hashCode() : -1;
    } else if (other.containsMaster()) {
      return 1;
    } else {
      return hashCode() - other.hashCode();
    }
  }

  //todo: once we have logical tasks, move tracking of physical tasks there
  public void registerPhysicalTaskId(long logicalTaskId, long physicalTaskId) {
    physicalTasks.put(logicalTaskId, physicalTaskId);

    topology.getAmbariContext().getPersistedTopologyState().
        registerPhysicalTask(logicalTaskId, physicalTaskId);
  }

  private Predicate toPredicate(String predicate) {
    Predicate compiledPredicate = null;
    try {
      if (predicate != null && ! predicate.isEmpty()) {
        compiledPredicate = predicateCompiler.compile(predicate);
      }
    } catch (InvalidQueryException e) {
      // log error and proceed without predicate
      LOG.error("Unable to compile predicate for host request: " + e, e);
    }
    return compiledPredicate;
  }

  private class PersistHostResourcesTask implements TopologyTask {
    private AmbariContext ambariContext;

    @Override
    public Type getType() {
      return Type.RESOURCE_CREATION;
    }

    @Override
    public void init(ClusterTopology topology, AmbariContext ambariContext) {
      this.ambariContext = ambariContext;
    }

    @Override
    public void run() {
      LOG.info("HostRequest: Executing RESOURCE_CREATION task for host: {}", hostname);
      HostGroup group = topology.getBlueprint().getHostGroup(getHostgroupName());
      Map<String, Collection<String>> serviceComponents = new HashMap<String, Collection<String>>();
      for (String service : group.getServices()) {
        serviceComponents.put(service, new HashSet<String> (group.getComponents(service)));
      }
      ambariContext.createAmbariHostResources(getClusterId(), getHostName(), serviceComponents);
    }
  }

  private class RegisterWithConfigGroupTask implements TopologyTask {
    private ClusterTopology clusterTopology;
    private AmbariContext ambariContext;

    @Override
    public Type getType() {
      return Type.CONFIGURE;
    }

    @Override
    public void init(ClusterTopology topology, AmbariContext ambariContext) {
      clusterTopology = topology;
      this.ambariContext = ambariContext;
    }

    @Override
    public void run() {
      LOG.info("HostRequest: Executing CONFIGURE task for host: {}", hostname);
      ambariContext.registerHostWithConfigGroup(getHostName(), clusterTopology, getHostgroupName());
    }
  }

  //todo: extract
  private class InstallHostTask implements TopologyTask {
    private ClusterTopology clusterTopology;
    private final boolean skipFailure;

    public InstallHostTask(boolean skipFailure) {
      this.skipFailure = skipFailure;
    }

    @Override
    public Type getType() {
      return Type.INSTALL;
    }

    @Override
    public void init(ClusterTopology topology, AmbariContext ambariContext) {
      clusterTopology = topology;
    }

    @Override
    public void run() {
      LOG.info("HostRequest: Executing INSTALL task for host: {}", hostname);
      boolean skipInstallTaskCreate = topology.getProvisionAction().equals(ProvisionAction.START_ONLY);
      RequestStatusResponse response = clusterTopology.installHost(hostname, skipInstallTaskCreate, skipFailure);
      // map logical install tasks to physical install tasks
      List<ShortTaskStatus> underlyingTasks = response.getTasks();
      for (ShortTaskStatus task : underlyingTasks) {
        Long logicalInstallTaskId = logicalTaskMap.get(this).get(task.getRole());
        if(logicalInstallTaskId == null) {
          LOG.info("Skipping physical install task registering, because component {} cannot be found", task.getRole());
          continue;
        }
        //todo: for now only one physical task per component
        long taskId = task.getTaskId();
        registerPhysicalTaskId(logicalInstallTaskId, taskId);

        //todo: move this to provision
        //todo: shouldn't have to iterate over all tasks to find install task
        //todo: we are doing the same thing in the above registerPhysicalTaskId() call
        // set attempt count on task
        for (HostRoleCommand logicalTask : logicalTasks.values()) {
          if (logicalTask.getTaskId() == logicalInstallTaskId) {
            logicalTask.incrementAttemptCount();
          }
        }
      }

      LOG.info("HostRequest: Exiting INSTALL task for host: {}", hostname);
    }
  }

  //todo: extract
  private class StartHostTask implements TopologyTask {
    private ClusterTopology clusterTopology;
    private final boolean skipFailure;

    public StartHostTask(boolean skipFailure) {
      this.skipFailure = skipFailure;
    }

    @Override
    public Type getType() {
      return Type.START;
    }

    @Override
    public void init(ClusterTopology topology, AmbariContext ambariContext) {
      clusterTopology = topology;
    }

    @Override
    public void run() {
      LOG.info("HostRequest: Executing START task for host: {}", hostname);
      RequestStatusResponse response = clusterTopology.startHost(hostname, skipFailure);
      // map logical install tasks to physical install tasks
      List<ShortTaskStatus> underlyingTasks = response.getTasks();
      for (ShortTaskStatus task : underlyingTasks) {
        String component = task.getRole();
        Long logicalStartTaskId = logicalTaskMap.get(this).get(component);
        if(logicalStartTaskId == null) {
          LOG.info("Skipping physical start task registering, because component {} cannot be found", task.getRole());
          continue;
        }
        // for now just set on outer map
        registerPhysicalTaskId(logicalStartTaskId, task.getTaskId());

        //todo: move this to provision
        // set attempt count on task
        for (HostRoleCommand logicalTask : logicalTasks.values()) {
          if (logicalTask.getTaskId() == logicalStartTaskId) {
            logicalTask.incrementAttemptCount();
          }
        }
      }

      LOG.info("HostRequest: Exiting START task for host: {}", hostname);
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
