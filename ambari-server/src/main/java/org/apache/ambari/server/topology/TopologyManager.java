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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.host.HostImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages all cluster provisioning actions on the cluster topology.
 */
//todo: cluster isolation
@Singleton
public class TopologyManager {

  private final List<HostImpl> availableHosts = new LinkedList<HostImpl>();
  private final Map<String, LogicalRequest> reservedHosts = new HashMap<String, LogicalRequest>();
  private final Map<Long, LogicalRequest> allRequests = new HashMap<Long, LogicalRequest>();
  // priority is given to oldest outstanding requests
  private final Collection<LogicalRequest> outstandingRequests = new ArrayList<LogicalRequest>();
  private Map<String, ClusterTopology> clusterTopologyMap = new HashMap<String, ClusterTopology>();
  private final Map<TopologyTask.Type, Set<TopologyTask>> pendingTasks = new HashMap<TopologyTask.Type, Set<TopologyTask>>();

  //todo: proper wait/notify mechanism
  private final Object configurationFlagLock = new Object();
  private boolean configureComplete = false;

  private AmbariManagementController controller;
  ExecutorService executor;
  //todo: task id's.  Use existing mechanism for getting next task id sequence
  private final static AtomicLong nextTaskId = new AtomicLong(10000);
  private final Object serviceResourceLock = new Object();

  protected final static Logger LOG = LoggerFactory.getLogger(TopologyManager.class);


  public TopologyManager() {
    pendingTasks.put(TopologyTask.Type.CONFIGURE, new HashSet<TopologyTask>());
    pendingTasks.put(TopologyTask.Type.INSTALL, new HashSet<TopologyTask>());
    pendingTasks.put(TopologyTask.Type.START, new HashSet<TopologyTask>());

    executor = getExecutorService();
  }

  public RequestStatusResponse provisionCluster(TopologyRequest request) throws InvalidTopologyException, AmbariException {
    ClusterTopology topology = new ClusterTopologyImpl(request);

    String clusterName = topology.getClusterName();
    clusterTopologyMap.put(clusterName, topology);

    createClusterResource(clusterName);
    createServiceAndComponentResources(topology);

    LogicalRequest logicalRequest = processRequest(request, topology);
    try {
      addClusterConfigRequest(new ClusterConfigurationRequest(topology));
    } catch (AmbariException e) {
      //todo
      throw e;
    }

    //todo: this should be invoked as part of a generic lifecycle event which could possibly
    //todo: be tied to cluster state
    persistInstallStateForUI(clusterName);
    return getRequestStatus(logicalRequest.getRequestId());
  }

  public RequestStatusResponse scaleHosts(TopologyRequest request)
      throws InvalidTopologyException, AmbariException {

    String clusterName = request.getClusterName();
    ClusterTopology topology = clusterTopologyMap.get(clusterName);
    if (topology == null) {
      throw new AmbariException("TopologyManager: Unable to retrieve cluster topology for cluster: " + clusterName);
    }

    // this registers/updates all request host groups
    topology.update(request);
    return getRequestStatus(processRequest(request, topology).getRequestId());
  }

  //todo: should be synchronized on same lock as onHostRegistered()
  //todo: HostImpl is what is registered with the HearbeatHandler and contains more host info than HostInfo so
  //todo: we should probably change to use HostImpl
  public void onHostRegistered(HostImpl host, boolean associatedWithCluster) {
    if (associatedWithCluster) {
      return;
    }

    boolean matchedToRequest = false;
    String hostName = host.getHostName();
    synchronized(reservedHosts) {
      if (reservedHosts.containsKey(hostName)) {
        LogicalRequest request = reservedHosts.remove(hostName);
        HostOfferResponse response = request.offer(host);
        if (response.getAnswer() != HostOfferResponse.Answer.ACCEPTED) {
          //todo: this is handled explicitly in LogicalRequest so this shouldn't happen here
          throw new RuntimeException("LogicalRequest declined host offer of explicitly requested host: " + hostName);
        }
        processAcceptedHostOffer(getClusterTopology(request.getClusterName()), response, host);
        matchedToRequest = true;
      }
    }

    // can be true if host was reserved
    if (! matchedToRequest) {
      synchronized (outstandingRequests) {
        Iterator<LogicalRequest> outstandingRequestIterator = outstandingRequests.iterator();
        while (! matchedToRequest && outstandingRequestIterator.hasNext()) {
          LogicalRequest request = outstandingRequestIterator.next();
          HostOfferResponse hostOfferResponse = request.offer(host);
          switch (hostOfferResponse.getAnswer()) {
            case ACCEPTED:
              matchedToRequest = true;
              processAcceptedHostOffer(getClusterTopology(request.getClusterName()), hostOfferResponse, host);
              break;
            case DECLINED_DONE:
              outstandingRequestIterator.remove();
              break;
            case DECLINED_PREDICATE:
              break;
          }
        }
      }
    }

    if (! matchedToRequest) {
      synchronized (availableHosts) {
        LOG.info("TopologyManager: Queueing available host {}", hostName);
        availableHosts.add(host);
      }
    }
  }

  public void onHostLeft(String hostname) {
    //todo:
  }

  public Request getRequest(long requestId) {
    return allRequests.get(requestId);
  }

  public Collection<LogicalRequest> getRequests(Collection<Long> requestIds) {
    if (requestIds.isEmpty()) {
      return allRequests.values();
    } else {
      Collection<LogicalRequest> matchingRequests = new ArrayList<LogicalRequest>();
      for (long id : requestIds) {
        LogicalRequest request = allRequests.get(id);
        if (request != null) {
          matchingRequests.add(request);
        }
      }
      return matchingRequests;
    }
  }

  //todo: currently we are just returning all stages for all requests
  //todo: and relying on the StageResourceProvider to convert each to a resource and do a predicate eval on each
  public Collection<StageEntity> getStages() {
    Collection<StageEntity> stages = new ArrayList<StageEntity>();
    for (LogicalRequest logicalRequest : allRequests.values()) {
      stages.addAll(logicalRequest.getStageEntities());
    }
    return stages;
  }

  public Collection<HostRoleCommand> getTasks(long requestId) {
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? Collections.<HostRoleCommand>emptyList() : request.getCommands();
  }

  public Collection<HostRoleCommand> getTasks(Collection<Long> requestIds) {
    Collection<HostRoleCommand> tasks = new ArrayList<HostRoleCommand>();
    for (long id : requestIds) {
      tasks.addAll(getTasks(id));
    }

    return tasks;
  }

  public Map<Long, HostRoleCommandStatusSummaryDTO> getStageSummaries(Long requestId) {
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? Collections.<Long, HostRoleCommandStatusSummaryDTO>emptyMap() :
        request.getStageSummaries();
  }

  public RequestStatusResponse getRequestStatus(long requestId) {
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? null : request.getRequestStatus();
  }

  public Collection<RequestStatusResponse> getRequestStatus(Collection<Long> ids) {
    List<RequestStatusResponse> requestStatusResponses = new ArrayList<RequestStatusResponse>();
    for (long id : ids) {
      RequestStatusResponse response = getRequestStatus(id);
      if (response != null) {
        requestStatusResponses.add(response);
      }
    }

    return requestStatusResponses;
  }

  public ClusterTopology getClusterTopology(String clusterName) {
    return clusterTopologyMap.get(clusterName);
  }

  public Map<String, Collection<String>> getProjectedTopology() {
    Map<String, Collection<String>> hostComponentMap = new HashMap<String, Collection<String>>();

    for (LogicalRequest logicalRequest : allRequests.values()) {
      Map<String, Collection<String>> requestTopology = logicalRequest.getProjectedTopology();
      for (Map.Entry<String, Collection<String>> entry : requestTopology.entrySet()) {
        String host = entry.getKey();
        Collection<String> hostComponents = hostComponentMap.get(host);
        if (hostComponents == null) {
          hostComponents = new HashSet<String>();
          hostComponentMap.put(host, hostComponents);
        }
        hostComponents.addAll(entry.getValue());
      }
    }
    return hostComponentMap;
  }

  private LogicalRequest processRequest(TopologyRequest request, ClusterTopology topology) throws AmbariException {

    finalizeTopology(request, topology);
    LogicalRequest logicalRequest = createLogicalRequest(request, topology);

    boolean requestHostComplete = false;
    //todo: overall synchronization. Currently we have nested synchronization here
    synchronized(availableHosts) {
      Iterator<HostImpl> hostIterator = availableHosts.iterator();
      while (! requestHostComplete && hostIterator.hasNext()) {
        HostImpl host = hostIterator.next();
        synchronized (reservedHosts) {
          String hostname = host.getHostName();
          if (reservedHosts.containsKey(hostname))  {
            if (logicalRequest.equals(reservedHosts.get(hostname))) {
              // host is registered to this request, remove it from reserved map
              reservedHosts.remove(hostname);
            } else {
              // host is registered with another request, don't offer
              //todo: clean up logic
              continue;
            }
          }
        }
        HostOfferResponse response = logicalRequest.offer(host);
        switch (response.getAnswer()) {
          case ACCEPTED:
            //todo: when host matches last host it returns ACCEPTED so we don't know that logical request is no
            //todo: longer outstanding until we call offer again.  This is really only an issue if we need to
            //todo: deal specifically with outstanding hosts other than calling offer.  Also, failure handling
            //todo: may affect this behavior??
            hostIterator.remove();
            processAcceptedHostOffer(getClusterTopology(logicalRequest.getClusterName()), response, host);
            break;
          case DECLINED_DONE:
            requestHostComplete = true;
            break;
          case DECLINED_PREDICATE:
            break;
        }
      }

      if (! requestHostComplete) {
        // not all required hosts have been matched (see earlier comment regarding outstanding logical requests
        outstandingRequests.add(logicalRequest);
      }
    }
    return logicalRequest;
  }

  private LogicalRequest createLogicalRequest(TopologyRequest request, ClusterTopology topology) throws AmbariException {
    LogicalRequest logicalRequest = new LogicalRequest(request, new ClusterTopologyContext(topology));
    allRequests.put(logicalRequest.getRequestId(), logicalRequest);
    synchronized (reservedHosts) {
      for (String host : logicalRequest.getReservedHosts()) {
        reservedHosts.put(host, logicalRequest);
      }
    }

    return logicalRequest;
  }

  private void processAcceptedHostOffer(ClusterTopology topology, HostOfferResponse response, HostImpl host) {
    try {
      topology.addHostToTopology(response.getHostGroupName(), host.getHostName());
    } catch (InvalidTopologyException e) {
      //todo
      throw new RuntimeException(e);
    } catch (NoSuchHostGroupException e) {
      throw new RuntimeException(e);
    }

    List<TopologyTask> tasks = response.getTasks();
    synchronized (configurationFlagLock) {
      if (configureComplete) {
        for (TopologyTask task : tasks) {
          task.run();
        }
      }else {
        for (TopologyTask task : tasks) {
          //todo: proper state dependencies
          TopologyTask.Type taskType = task.getType();
          if (taskType == TopologyTask.Type.RESOURCE_CREATION || taskType == TopologyTask.Type.CONFIGURE) {
            task.run();
          } else {
            // all type collections are added at init time
            pendingTasks.get(taskType).add(task);
          }
        }
      }
    }
  }

  //todo: this should invoke a callback on each 'service' in the topology
  private void finalizeTopology(TopologyRequest request, ClusterTopology topology) {
    addKerberosClientIfNecessary(topology);
  }

  /**
   * Add the kerberos client to groups if kerberos is enabled for the cluster.
   *
   * @param topology  cluster topology
   */
  //for now, hard coded here
  private void addKerberosClientIfNecessary(ClusterTopology topology) {

    String clusterName = topology.getClusterName();
    //todo: logic would ideally be contained in the stack
    Cluster cluster;
    try {
      cluster = getController().getClusters().getCluster(clusterName);
    } catch (AmbariException e) {
      //todo: this shouldn't happen at this point but still need to handle in a generic manner for topo finalization
      throw new RuntimeException("Parent Cluster resource doesn't exist.  clusterName= " + clusterName);
    }
    if (cluster.getSecurityType() == SecurityType.KERBEROS) {
      for (HostGroup group : topology.getBlueprint().getHostGroups().values()) {
        group.addComponent("KERBEROS_CLIENT");
      }
    }
  }

  // create a thread pool which is used for task execution
  private synchronized ExecutorService getExecutorService() {
    if (executor == null) {
      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

      int THREAD_POOL_CORE_SIZE = 2;
      int THREAD_POOL_MAX_SIZE = 100;
      int THREAD_POOL_TIMEOUT = Integer.MAX_VALUE;
      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
          THREAD_POOL_CORE_SIZE,
          THREAD_POOL_MAX_SIZE,
          THREAD_POOL_TIMEOUT,
          TimeUnit.SECONDS,
          queue);

      //threadPoolExecutor.allowCoreThreadTimeOut(true);
      executor = threadPoolExecutor;
    }
    return executor;
  }

  private void addClusterConfigRequest(ClusterConfigurationRequest configurationRequest) {
    //pendingTasks.get(Action.CONFIGURE).add(new ConfigureClusterTask(configurationRequest));
    synchronized (configurationFlagLock) {
      configureComplete = false;
    }
    executor.submit(new ConfigureClusterTask(configurationRequest));
  }

  private void createClusterResource(String clusterName) throws AmbariException {
    Stack stack = clusterTopologyMap.get(clusterName).getBlueprint().getStack();
    String stackInfo = String.format("%s-%s", stack.getName(), stack.getVersion());
    ClusterRequest clusterRequest = new ClusterRequest(null, clusterName, stackInfo, null);
    getController().createCluster(clusterRequest);
  }

  private void createServiceAndComponentResources(ClusterTopology topology) {
    String clusterName = topology.getClusterName();
    Collection<String> services = topology.getBlueprint().getServices();

    synchronized(serviceResourceLock) {
      try {
        Cluster cluster = getController().getClusters().getCluster(clusterName);
        services.removeAll(cluster.getServices().keySet());
      } catch (AmbariException e) {
        //todo
        throw new RuntimeException(e);
      }
      Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
      Set<ServiceComponentRequest> componentRequests = new HashSet<ServiceComponentRequest>();
      for (String service : services) {
        serviceRequests.add(new ServiceRequest(clusterName, service, null));
        for (String component : topology.getBlueprint().getComponents(service)) {
          componentRequests.add(new ServiceComponentRequest(clusterName, service, component, null));
        }
      }
      try {
        ServiceResourceProvider serviceResourceProvider = (ServiceResourceProvider) ClusterControllerHelper.
            getClusterController().ensureResourceProvider(Resource.Type.Service);

        serviceResourceProvider.createServices(serviceRequests);

        ComponentResourceProvider componentResourceProvider = (ComponentResourceProvider) ClusterControllerHelper.
            getClusterController().ensureResourceProvider(Resource.Type.Component);

        componentResourceProvider.createComponents(componentRequests);
      } catch (AmbariException e) {
        //todo
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Persist cluster state for the ambari UI.  Setting this state informs that UI that a cluster has been
   * installed and started and that the monitoring screen for the cluster should be displayed to the user.
   *
   * @param clusterName  name of cluster
   */
  //todo: invoke as part of a generic callback possible associated with cluster state
  private void persistInstallStateForUI(String clusterName) throws AmbariException {
    Stack stack = clusterTopologyMap.get(clusterName).getBlueprint().getStack();
    String stackInfo = String.format("%s-%s", stack.getName(), stack.getVersion());
    ClusterRequest clusterRequest = new ClusterRequest(null, clusterName, "INSTALLED", null, stackInfo, null);

    getController().updateClusters(Collections.singleton(clusterRequest), null);
  }

  private synchronized AmbariManagementController getController() {
    if (controller == null) {
      controller = AmbariServer.getController();
    }
    return controller;
  }

  private class ConfigureClusterTask implements Runnable {
    private ClusterConfigurationRequest configRequest;


    public ConfigureClusterTask(ClusterConfigurationRequest configRequest) {
      this.configRequest = configRequest;
    }


    @Override
    public void run() {
      LOG.info("TopologyManager.ConfigureClusterTask: Entering");

      boolean completed = false;
      boolean interrupted = false;

      while (! completed && ! interrupted) {
        completed = areConfigsResolved();

        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          interrupted = true;
          // reset interrupted flag on thread
          Thread.interrupted();

        }
      }

      if (! interrupted) {
        try {
          LOG.info("TopologyManager.ConfigureClusterTask: Setting Configuration on cluster");
          // sets updated configuration on topology and cluster
          configRequest.process();
        } catch (Exception e) {
          //todo: how to handle this?  If this fails, we shouldn't start any hosts.
          LOG.error("TopologyManager.ConfigureClusterTask: " +
              "An exception occurred while attempting to process cluster configs and set on cluster: " + e);
          e.printStackTrace();
        }

        synchronized (configurationFlagLock) {
          LOG.info("TopologyManager.ConfigureClusterTask: Setting configure complete flag to true");
          configureComplete = true;
        }

        // execute all queued install/start tasks
        executor.submit(new ExecuteQueuedHostTasks());
      }
      LOG.info("TopologyManager.ConfigureClusterTask: Exiting");
    }

    // get set of required host groups from config processor and confirm that all requests
    // have fully resolved the host names for the required host groups
    private boolean areConfigsResolved() {
      boolean configTopologyResolved = true;
      Collection<String> requiredHostGroups;
      try {
        requiredHostGroups = configRequest.getRequiredHostGroups();
      } catch (RuntimeException e) {
        //todo: for now if an exception occurs, log error and return true which will result in topology update
        LOG.error("An exception occurred while attempting to determine required host groups for config update " + e);
        e.printStackTrace();
        requiredHostGroups = Collections.emptyList();
      }

      synchronized (outstandingRequests) {
        for (LogicalRequest outstandingRequest : outstandingRequests) {
          if (! outstandingRequest.areGroupsResolved(requiredHostGroups)) {
            configTopologyResolved = false;
            break;
          }
        }
      }
      return configTopologyResolved;
    }
  }

  private class ExecuteQueuedHostTasks implements Runnable {
    @Override
    public void run() {
      //todo: lock is too coarse grained, should only be on start tasks
      synchronized(pendingTasks) {
        // execute queued install tasks
        //todo: once agent configuration is removed from agent install, we will be able to
        //todo: install without regard to configuration resolution
        Iterator<TopologyTask> iter = pendingTasks.get(TopologyTask.Type.INSTALL).iterator();
        while (iter.hasNext()) {
          iter.next().run();
          iter.remove();
        }

        iter = pendingTasks.get(TopologyTask.Type.START).iterator();
        while (iter.hasNext()) {
          iter.next().run();
          iter.remove();
        }
      }
    }
  }

  //todo: this is a temporary step, remove after refactoring makes it no longer needed
  public class ClusterTopologyContext {
    private ClusterTopology clusterTopology;

    public ClusterTopologyContext(ClusterTopology clusterTopology) {
      this.clusterTopology = clusterTopology;
    }

    public ClusterTopology getClusterTopology() {
      return clusterTopology;
    }

    public long getNextTaskId() {
      synchronized (nextTaskId) {
        return nextTaskId.getAndIncrement();
      }
    }
  }
}
