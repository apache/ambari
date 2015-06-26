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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.host.HostImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Manages all cluster provisioning actions on the cluster topology.
 */
//todo: cluster isolation
@Singleton
public class TopologyManager {

  public static final String INITIAL_CONFIG_TAG = "INITIAL";
  public static final String TOPOLOGY_RESOLVED_TAG = "TOPOLOGY_RESOLVED";

  private PersistedState persistedState;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private Collection<String> hostsToIgnore = new HashSet<String>();
  private final List<HostImpl> availableHosts = new LinkedList<HostImpl>();
  private final Map<String, LogicalRequest> reservedHosts = new HashMap<String, LogicalRequest>();
  private final Map<Long, LogicalRequest> allRequests = new HashMap<Long, LogicalRequest>();
  // priority is given to oldest outstanding requests
  private final Collection<LogicalRequest> outstandingRequests = new ArrayList<LogicalRequest>();
  //todo: currently only support a single cluster
  private Map<String, ClusterTopology> clusterTopologyMap = new HashMap<String, ClusterTopology>();

  //todo: inject
  private static LogicalRequestFactory logicalRequestFactory = new LogicalRequestFactory();
  private static AmbariContext ambariContext = new AmbariContext();

  private final Object initializationLock = new Object();

  /**
   * A boolean not cached thread-local (volatile) to prevent double-checked
   * locking on the synchronized keyword.
   */
  private volatile boolean isInitialized;

  private final static Logger LOG = LoggerFactory.getLogger(TopologyManager.class);

  public TopologyManager() {
    persistedState = ambariContext.getPersistedTopologyState();
  }

  //todo: can't call in constructor.
  //todo: Very important that this occurs prior to any usage
  private void ensureInitialized() {
    if (!isInitialized) {
      synchronized (initializationLock) {
        if (!isInitialized) {
          replayRequests(persistedState.getAllRequests());
          isInitialized = true;
        }
      }
    }
  }

  public RequestStatusResponse provisionCluster(TopologyRequest request) throws InvalidTopologyException, AmbariException {
    ensureInitialized();
    ClusterTopology topology = new ClusterTopologyImpl(ambariContext, request);
    // persist request after it has successfully validated
    PersistedTopologyRequest persistedRequest = persistedState.persistTopologyRequest(request);

    // get the id prior to creating ambari resources which increments the counter
    Long provisionId = ambariContext.getNextRequestId();
    ambariContext.createAmbariResources(topology);

    String clusterName = topology.getClusterName();
    clusterTopologyMap.put(clusterName, topology);

    addClusterConfigRequest(topology, new ClusterConfigurationRequest(ambariContext, topology, true));
    LogicalRequest logicalRequest = processRequest(persistedRequest, topology, provisionId);

    //todo: this should be invoked as part of a generic lifecycle event which could possibly
    //todo: be tied to cluster state
    Stack stack = topology.getBlueprint().getStack();
    ambariContext.persistInstallStateForUI(clusterName, stack.getName(), stack.getVersion());
    return getRequestStatus(logicalRequest.getRequestId());
  }

  public RequestStatusResponse scaleHosts(TopologyRequest request)
      throws InvalidTopologyException, AmbariException {

    ensureInitialized();
    String clusterName = request.getClusterName();
    ClusterTopology topology = clusterTopologyMap.get(clusterName);
    if (topology == null) {
      throw new InvalidTopologyException("Unable to retrieve cluster topology for cluster. This is most likely a " +
                                         "result of trying to scale a cluster via the API which was created using " +
                                         "the Ambari UI. At this time only clusters created via the API using a " +
                                         "blueprint can be scaled with this API.  If the cluster was originally created " +
                                         "via the API as described above, please file a Jira for this matter.");
    }

    PersistedTopologyRequest persistedRequest = persistedState.persistTopologyRequest(request);
    // this registers/updates all request host groups
    topology.update(request);
    return getRequestStatus(processRequest(persistedRequest, topology,
        ambariContext.getNextRequestId()).getRequestId());
  }

  public void onHostRegistered(HostImpl host, boolean associatedWithCluster) {
    ensureInitialized();
    if (associatedWithCluster || isHostIgnored(host.getHostName())) {
      return;
    }

    boolean matchedToRequest = false;
    String hostName = host.getHostName();
    synchronized(reservedHosts) {
      if (reservedHosts.containsKey(hostName)) {
        LogicalRequest request = reservedHosts.remove(hostName);
        HostOfferResponse response = request.offer(host);
        if (response.getAnswer() != HostOfferResponse.Answer.ACCEPTED) {
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

  public Request getRequest(long requestId) {
    ensureInitialized();
    return allRequests.get(requestId);
  }

  public Collection<LogicalRequest> getRequests(Collection<Long> requestIds) {
    ensureInitialized();
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

  // currently we are just returning all stages for all requests
  //and relying on the StageResourceProvider to convert each to a resource and do a predicate eval on each
  public Collection<StageEntity> getStages() {
    ensureInitialized();
    Collection<StageEntity> stages = new ArrayList<StageEntity>();
    for (LogicalRequest logicalRequest : allRequests.values()) {
      stages.addAll(logicalRequest.getStageEntities());
    }
    return stages;
  }

  public Collection<HostRoleCommand> getTasks(long requestId) {
    ensureInitialized();
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? Collections.<HostRoleCommand>emptyList() : request.getCommands();
  }

  public Collection<HostRoleCommand> getTasks(Collection<Long> requestIds) {
    ensureInitialized();
    Collection<HostRoleCommand> tasks = new ArrayList<HostRoleCommand>();
    for (long id : requestIds) {
      tasks.addAll(getTasks(id));
    }

    return tasks;
  }

  public Map<Long, HostRoleCommandStatusSummaryDTO> getStageSummaries(Long requestId) {
    ensureInitialized();
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? Collections.<Long, HostRoleCommandStatusSummaryDTO>emptyMap() :
        request.getStageSummaries();
  }

  public RequestStatusResponse getRequestStatus(long requestId) {
    ensureInitialized();
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? null : request.getRequestStatus();
  }

  public Collection<RequestStatusResponse> getRequestStatus(Collection<Long> ids) {
    ensureInitialized();
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
    ensureInitialized();
    return clusterTopologyMap.get(clusterName);
  }

  public Map<String, Collection<String>> getProjectedTopology() {
    ensureInitialized();
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

  private LogicalRequest processRequest(PersistedTopologyRequest request, ClusterTopology topology, Long requestId)
      throws AmbariException {

    finalizeTopology(request.getRequest(), topology);
    LogicalRequest logicalRequest = createLogicalRequest(request, topology, requestId);

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
        // not all required hosts have been matched (see earlier comment regarding outstanding logical requests)
        outstandingRequests.add(logicalRequest);
      }
    }
    return logicalRequest;
  }

  private LogicalRequest createLogicalRequest(PersistedTopologyRequest request, ClusterTopology topology, Long requestId)
      throws AmbariException {

    LogicalRequest logicalRequest = logicalRequestFactory.createRequest(
        requestId, request.getRequest(), topology);

    persistedState.persistLogicalRequest(logicalRequest, request.getId());

    allRequests.put(logicalRequest.getRequestId(), logicalRequest);
    synchronized (reservedHosts) {
      for (String host : logicalRequest.getReservedHosts()) {
        reservedHosts.put(host, logicalRequest);
      }
    }
    return logicalRequest;
  }

  private void processAcceptedHostOffer(ClusterTopology topology, HostOfferResponse response, HostImpl host) {
    String hostName = host.getHostName();
    try {
      topology.addHostToTopology(response.getHostGroupName(), hostName);
    } catch (InvalidTopologyException e) {
      // host already registered
      throw new RuntimeException("An internal error occurred while performing request host registration: " + e, e);
    } catch (NoSuchHostGroupException e) {
      // invalid host group
      throw new RuntimeException("An internal error occurred while performing request host registration: " + e, e);
    }

    // persist the host request -> hostName association
    persistedState.registerHostName(response.getHostRequestId(), hostName);

    for (TopologyTask task : response.getTasks()) {
      task.init(topology, ambariContext);
      executor.execute(task);
    }
  }

  private void replayRequests(Map<ClusterTopology, List<LogicalRequest>> persistedRequests) {
    boolean configChecked = false;
    for (Map.Entry<ClusterTopology, List<LogicalRequest>> requestEntry : persistedRequests.entrySet()) {
      ClusterTopology topology = requestEntry.getKey();
      clusterTopologyMap.put(topology.getClusterName(), topology);

      for (LogicalRequest logicalRequest : requestEntry.getValue()) {
        allRequests.put(logicalRequest.getRequestId(), logicalRequest);
        if (! logicalRequest.hasCompleted()) {
          outstandingRequests.add(logicalRequest);
          for (String reservedHost : logicalRequest.getReservedHosts()) {
            reservedHosts.put(reservedHost, logicalRequest);
          }
          // completed host requests are host requests which have been mapped to a host
          // and the host has ben added to the cluster
          for (HostRequest hostRequest : logicalRequest.getCompletedHostRequests()) {
            try {
              String hostName = hostRequest.getHostName();
              topology.addHostToTopology(hostRequest.getHostgroupName(), hostName);
              hostsToIgnore.add(hostName);
            } catch (InvalidTopologyException e) {
              LOG.warn("Attempted to add host to multiple host groups while replaying requests: " + e, e);
            } catch (NoSuchHostGroupException e) {
              LOG.warn("Failed to add host to topology while replaying requests: " + e, e);
            }
          }
        }
      }

      if (! configChecked) {
        configChecked = true;
        if (! ambariContext.doesConfigurationWithTagExist(topology.getClusterName(), TOPOLOGY_RESOLVED_TAG)) {
          addClusterConfigRequest(topology, new ClusterConfigurationRequest(ambariContext, topology, false));
        }
      }
    }
  }

  //todo: this should invoke a callback on each 'service' in the topology
  private void finalizeTopology(TopologyRequest request, ClusterTopology topology) {
    addKerberosClientIfNecessary(topology);
  }

  private boolean isHostIgnored(String host) {
    return hostsToIgnore.remove(host);
  }

  /**
   * Add the kerberos client to groups if kerberos is enabled for the cluster.
   *
   * @param topology  cluster topology
   */
  private void addKerberosClientIfNecessary(ClusterTopology topology) {
    if (topology.isClusterKerberosEnabled()) {
      for (HostGroup group : topology.getBlueprint().getHostGroups().values()) {
        group.addComponent("KERBEROS_CLIENT");
      }
    }
  }

  /**
   * Register the configuration task which is responsible for configuration topology resolution
   * and setting the updated configuration on the cluster.  This task needs to be submitted to the
   * executor before any host requests to ensure that no install or start tasks are executed prior
   * to configuration being set on the cluster.
   *
   * @param topology              cluster topology
   * @param configurationRequest  configuration request to be executed
   */
  private void addClusterConfigRequest(ClusterTopology topology, ClusterConfigurationRequest configurationRequest) {
    executor.execute(new ConfigureClusterTask(topology, configurationRequest));
  }

  private class ConfigureClusterTask implements Runnable {
    private ClusterConfigurationRequest configRequest;
    private ClusterTopology topology;


    public ConfigureClusterTask(ClusterTopology topology, ClusterConfigurationRequest configRequest) {
      this.configRequest = configRequest;
      this.topology = topology;
    }

    @Override
    public void run() {
      LOG.info("TopologyManager.ConfigureClusterTask: Entering");

      boolean completed = false;
      boolean interrupted = false;

      Collection<String> requiredHostGroups = getTopologyRequiredHostGroups();
      while (! completed && ! interrupted) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          interrupted = true;
          // reset interrupted flag on thread
          Thread.interrupted();
        }
        completed = areRequiredHostGroupsResolved(requiredHostGroups);
      }

      if (! interrupted) {
        try {
          LOG.info("TopologyManager.ConfigureClusterTask: Setting Configuration on cluster");
          // sets updated configuration on topology and cluster
          configRequest.process();
        } catch (Exception e) {
          // just logging and allowing config flag to be reset
          LOG.error("TopologyManager.ConfigureClusterTask: " +
              "An exception occurred while attempting to process cluster configs and set on cluster: " + e);
          e.printStackTrace();
        }
      }
      LOG.info("TopologyManager.ConfigureClusterTask: Exiting");
    }

    /**
     * Return the set of host group names which are required for configuration topology resolution.
     *
     * @return set of required host group names
     */
    private Collection<String> getTopologyRequiredHostGroups() {
      Collection<String> requiredHostGroups;
      try {
        requiredHostGroups = configRequest.getRequiredHostGroups();
      } catch (RuntimeException e) {
        // just log error and allow config topology update
        LOG.error("An exception occurred while attempting to determine required host groups for config update " + e);
        e.printStackTrace();
        requiredHostGroups = Collections.emptyList();
      }
      return requiredHostGroups;
    }

    /**
     * Determine if all hosts for the given set of required host groups are known.
     *
     * @param requiredHostGroups set of required host groups
     * @return true if all required host groups are resolved
     */
    private boolean areRequiredHostGroupsResolved(Collection<String> requiredHostGroups) {
      boolean configTopologyResolved = true;
      Map<String, HostGroupInfo> hostGroupInfo = topology.getHostGroupInfo();
      for (String hostGroup : requiredHostGroups) {
        HostGroupInfo groupInfo = hostGroupInfo.get(hostGroup);
        if (groupInfo == null || groupInfo.getHostNames().size() < groupInfo.getRequestedHostCount()) {
          configTopologyResolved = false;
          break;
        }
      }
      return configTopologyResolved;
    }
  }
}
