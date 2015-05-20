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
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.state.host.HostImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logical Request implementation.
 */
public class LogicalRequest extends Request {

  private final Collection<HostRequest> allHostRequests = new ArrayList<HostRequest>();
  // sorted set with master host requests given priority
  private final Collection<HostRequest> outstandingHostRequests = new TreeSet<HostRequest>();
  private final Map<String, HostRequest> requestsWithReservedHosts = new HashMap<String, HostRequest>();

  private final ClusterTopology topology;

  private static AmbariManagementController controller;

  private static final AtomicLong hostIdCounter = new AtomicLong(1);


  public LogicalRequest(Long id, TopologyRequest request, ClusterTopology topology)
      throws AmbariException {

    //todo: abstract usage of controller, etc ...
    super(id, getController().getClusters().getCluster(
        request.getClusterName()).getClusterId(), getController().getClusters());

    setRequestContext(String.format("Logical Request: %s", request.getDescription()));

    this.topology = topology;
    createHostRequests(request, topology);
  }

  public LogicalRequest(Long id, TopologyRequest request, ClusterTopology topology,
                        TopologyLogicalRequestEntity requestEntity) throws AmbariException {

    //todo: abstract usage of controller, etc ...
    super(id, getController().getClusters().getCluster(
        request.getClusterName()).getClusterId(), getController().getClusters());

    setRequestContext(String.format("Logical Request: %s", request.getDescription()));

    this.topology = topology;
    createHostRequests(topology, requestEntity);
  }

  public HostOfferResponse offer(HostImpl host) {
    // attempt to match to a host request with an explicit host reservation first
    synchronized (requestsWithReservedHosts) {
      HostRequest hostRequest = requestsWithReservedHosts.remove(host.getHostName());
      if (hostRequest != null) {
        HostOfferResponse response = hostRequest.offer(host);
        if (response.getAnswer() != HostOfferResponse.Answer.ACCEPTED) {
          // host request rejected host that it explicitly requested
          throw new RuntimeException("LogicalRequest declined host offer of explicitly requested host: " +
              host.getHostName());
        }
        return response;
      }
    }

    // not explicitly reserved, at least not in this request, so attempt to match to outstanding host requests
    boolean predicateRejected = false;
    synchronized (outstandingHostRequests) {
      //todo: prioritization of master host requests
      Iterator<HostRequest> hostRequestIterator = outstandingHostRequests.iterator();
      while (hostRequestIterator.hasNext()) {
        HostOfferResponse response = hostRequestIterator.next().offer(host);
        switch (response.getAnswer()) {
          case ACCEPTED:
            hostRequestIterator.remove();
            return response;
          case DECLINED_DONE:
            //todo: should have been done on ACCEPT
            hostRequestIterator.remove();
          case DECLINED_PREDICATE:
            predicateRejected = true;
        }
      }
    }
    // if at least one outstanding host request rejected for predicate or we have an outstanding request
    // with a reserved host decline due to predicate, otherwise decline due to all hosts being resolved
    return predicateRejected || ! requestsWithReservedHosts.isEmpty() ?
        new HostOfferResponse(HostOfferResponse.Answer.DECLINED_PREDICATE) :
        new HostOfferResponse(HostOfferResponse.Answer.DECLINED_DONE);
  }

  @Override
  public List<HostRoleCommand> getCommands() {
    List<HostRoleCommand> commands = new ArrayList<HostRoleCommand>();
    for (HostRequest hostRequest : allHostRequests) {
      commands.addAll(new ArrayList<HostRoleCommand>(hostRequest.getLogicalTasks()));
    }
    return commands;
  }

  public Collection<String> getReservedHosts() {
    return requestsWithReservedHosts.keySet();
  }

  public boolean hasCompleted() {
    return requestsWithReservedHosts.isEmpty() && outstandingHostRequests.isEmpty();
  }

  public Collection<HostRequest> getCompletedHostRequests() {
    Collection<HostRequest> completedHostRequests = new ArrayList<HostRequest>(allHostRequests);
    completedHostRequests.removeAll(outstandingHostRequests);
    completedHostRequests.removeAll(requestsWithReservedHosts.values());

    return completedHostRequests;
  }

  //todo: this is only here for toEntity() functionality
  public Collection<HostRequest> getHostRequests() {
    return new ArrayList<HostRequest>(allHostRequests);
  }

  public Map<String, Collection<String>> getProjectedTopology() {
    Map<String, Collection<String>> hostComponentMap = new HashMap<String, Collection<String>>();

    //todo: synchronization
    for (HostRequest hostRequest : allHostRequests) {
      HostGroup hostGroup = hostRequest.getHostGroup();
      for (String host : topology.getHostGroupInfo().get(hostGroup.getName()).getHostNames()) {
        Collection<String> hostComponents = hostComponentMap.get(host);
        if (hostComponents == null) {
          hostComponents = new HashSet<String>();
          hostComponentMap.put(host, hostComponents);
        }
        hostComponents.addAll(hostGroup.getComponents());
      }
    }
    return hostComponentMap;
  }

  // currently we are just returning all stages for all requests
  public Collection<StageEntity> getStageEntities() {
    Collection<StageEntity> stages = new ArrayList<StageEntity>();
    for (HostRequest hostRequest : allHostRequests) {
      StageEntity stage = new StageEntity();
      stage.setStageId(hostRequest.getStageId());
      stage.setRequestContext(getRequestContext());
      stage.setRequestId(getRequestId());
      //todo: not sure what this byte array is???
      //stage.setClusterHostInfo();
      stage.setClusterId(getClusterId());
      stage.setSkippable(false);
      // getTaskEntities() sync's state with physical tasks
      stage.setHostRoleCommands(hostRequest.getTaskEntities());

      stages.add(stage);
    }
    return stages;
  }

  public RequestStatusResponse getRequestStatus() {
    RequestStatusResponse requestStatus = new RequestStatusResponse(getRequestId());
    requestStatus.setRequestContext(getRequestContext());

    // convert HostRoleCommands to ShortTaskStatus
    List<ShortTaskStatus> shortTasks = new ArrayList<ShortTaskStatus>();
    for (HostRoleCommand task : getCommands()) {
      shortTasks.add(new ShortTaskStatus(task));
    }
    requestStatus.setTasks(shortTasks);

    return requestStatus;
  }

  public Map<Long, HostRoleCommandStatusSummaryDTO> getStageSummaries() {
    Map<Long, HostRoleCommandStatusSummaryDTO> summaryMap = new HashMap<Long, HostRoleCommandStatusSummaryDTO>();

    for (StageEntity stage : getStageEntities()) {
      //Number minStartTime = 0;
      //Number maxEndTime = 0;
      int aborted = 0;
      int completed = 0;
      int failed = 0;
      int holding = 0;
      int holdingFailed = 0;
      int holdingTimedout = 0;
      int inProgress = 0;
      int pending = 0;
      int queued = 0;
      int timedout = 0;

      //todo: where does this logic belong?
      for (HostRoleCommandEntity task : stage.getHostRoleCommands()) {
        HostRoleStatus taskStatus = task.getStatus();

        switch (taskStatus) {
          case ABORTED:
            aborted += 1;
            break;
          case COMPLETED:
            completed += 1;
            break;
          case FAILED:
            failed += 1;
            break;
          case HOLDING:
            holding += 1;
            break;
          case HOLDING_FAILED:
            holdingFailed += 1;
            break;
          case HOLDING_TIMEDOUT:
            holdingTimedout += 1;
            break;
          case IN_PROGRESS:
            inProgress += 1;
            break;
          case PENDING:
            pending += 1;
            break;
          case QUEUED:
            queued += 1;
            break;
          case TIMEDOUT:
            timedout += 1;
            break;
          default:
            System.out.println("Unexpected status when creating stage summaries: " + taskStatus);
        }
      }

      HostRoleCommandStatusSummaryDTO stageSummary = new HostRoleCommandStatusSummaryDTO(stage.isSkippable() ? 1 : 0, 0, 0,
          stage.getStageId(), aborted, completed, failed, holding, holdingFailed, holdingTimedout, inProgress, pending, queued, timedout);
      summaryMap.put(stage.getStageId(), stageSummary);
    }
    return summaryMap;
  }

  private void createHostRequests(TopologyRequest request, ClusterTopology topology) {
    Map<String, HostGroupInfo> hostGroupInfoMap = request.getHostGroupInfo();
    Blueprint blueprint = topology.getBlueprint();
    for (HostGroupInfo hostGroupInfo : hostGroupInfoMap.values()) {
      String groupName = hostGroupInfo.getHostGroupName();
      int hostCardinality = hostGroupInfo.getRequestedHostCount();
      List<String> hostnames = new ArrayList<String>(hostGroupInfo.getHostNames());

      for (int i = 0; i < hostCardinality; ++i) {
        if (! hostnames.isEmpty()) {
          // host names are specified
          String hostname = hostnames.get(i);
          HostRequest hostRequest = new HostRequest(getRequestId(), hostIdCounter.getAndIncrement(), getClusterName(),
              hostname, blueprint.getName(), blueprint.getHostGroup(groupName), null, topology);
          synchronized (requestsWithReservedHosts) {
            requestsWithReservedHosts.put(hostname, hostRequest);
          }
        } else {
          // host count is specified
          HostRequest hostRequest = new HostRequest(getRequestId(), hostIdCounter.getAndIncrement(), getClusterName(),
              null, blueprint.getName(), blueprint.getHostGroup(groupName), hostGroupInfo.getPredicate(), topology);
          outstandingHostRequests.add(hostRequest);
        }
      }
    }
    allHostRequests.addAll(outstandingHostRequests);
    allHostRequests.addAll(requestsWithReservedHosts.values());
  }

  private void createHostRequests(ClusterTopology topology,
                                  TopologyLogicalRequestEntity requestEntity) {

    for (TopologyHostRequestEntity hostRequestEntity : requestEntity.getTopologyHostRequestEntities()) {
      Long hostRequestId = hostRequestEntity.getId();
      synchronized (hostIdCounter) {
        if (hostIdCounter.get() <= hostRequestId) {
          hostIdCounter.set(hostRequestId + 1);
        }
      }
      TopologyHostGroupEntity hostGroupEntity = hostRequestEntity.getTopologyHostGroupEntity();

      String reservedHostName = hostGroupEntity.
          getTopologyHostInfoEntities().iterator().next().getFqdn();

      //todo: move predicate processing to host request
      HostRequest hostRequest = new HostRequest(getRequestId(), hostRequestId,
          reservedHostName, topology, hostRequestEntity);

      allHostRequests.add(hostRequest);
      if (! hostRequest.isCompleted()) {
        if (reservedHostName != null) {
          requestsWithReservedHosts.put(reservedHostName, hostRequest);
        } else {
          outstandingHostRequests.add(hostRequest);
        }
      }
    }
  }

  private synchronized static AmbariManagementController getController() {
    if (controller == null) {
      controller = AmbariServer.getController();
    }
    return controller;
  }
}
