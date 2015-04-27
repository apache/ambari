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
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.host.HostImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.apache.ambari.server.controller.AmbariServer.getController;

/**
 * Logical Request implementation.
 */
public class LogicalRequest extends Request {

  private Collection<HostRequest> allHostRequests = new ArrayList<HostRequest>();
  // sorted set with master host requests given priority
  private Collection<HostRequest> outstandingHostRequests = new TreeSet<HostRequest>();
  private Map<String, HostRequest> requestsWithReservedHosts = new HashMap<String, HostRequest>();

  private final ClusterTopology topology;


  //todo: topologyContext is a temporary refactoring step
  public LogicalRequest(TopologyRequest requestRequest, TopologyManager.ClusterTopologyContext topologyContext) throws AmbariException {
    //todo: abstract usage of controller, etc ...
    super(getController().getActionManager().getNextRequestId(), getController().getClusters().getCluster(
        requestRequest.getClusterName()).getClusterId(), getController().getClusters());

    this.topology = topologyContext.getClusterTopology();
    createHostRequests(requestRequest, topologyContext);
  }

  public HostOfferResponse offer(HostImpl host) {
    // attempt to match to a host request with an explicit host reservation first
    synchronized (requestsWithReservedHosts) {
      HostRequest hostRequest = requestsWithReservedHosts.remove(host.getHostName());
      if (hostRequest != null) {
        HostOfferResponse response = hostRequest.offer(host);
        if (response.getAnswer() != HostOfferResponse.Answer.ACCEPTED) {
          //todo: error handling.  This is really a system exception and shouldn't happen
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
    //todo: could also check if outstandingHostRequests is empty
    return predicateRejected || ! requestsWithReservedHosts.isEmpty() ?
        new HostOfferResponse(HostOfferResponse.Answer.DECLINED_PREDICATE) :
        new HostOfferResponse(HostOfferResponse.Answer.DECLINED_DONE);
  }

  //todo
  @Override
  public Collection<Stage> getStages() {
    return super.getStages();
  }

  @Override
  public List<HostRoleCommand> getCommands() {
    List<HostRoleCommand> commands = new ArrayList<HostRoleCommand>();
    for (HostRequest hostRequest : allHostRequests) {
      commands.addAll(new ArrayList<HostRoleCommand>(hostRequest.getTasks()));
    }
    return commands;
  }

  public Collection<String> getReservedHosts() {
    return requestsWithReservedHosts.keySet();
  }

  //todo: account for blueprint name?
  //todo: this should probably be done implicitly at a lower level
  public boolean areGroupsResolved(Collection<String> hostGroupNames) {
    synchronized (outstandingHostRequests) {
      // iterate over outstanding host requests
      for (HostRequest request : outstandingHostRequests) {
        if (hostGroupNames.contains(request.getHostgroupName()) && request.getHostName() == null) {
          return false;
        }
      }
    }
    return true;
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

  //todo: currently we are just returning all stages for all requests
  //todo: and relying on the StageResourceProvider to convert each to a resource and do a predicate eval on each
  //todo: needed to change the name to avoid a name collision.
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
    //todo: other request status fields
    //todo: ordering of tasks?

    // convert HostRoleCommands to ShortTaskStatus
    List<ShortTaskStatus> shortTasks = new ArrayList<ShortTaskStatus>();
    for (HostRoleCommand task : getCommands()) {
      shortTasks.add(new ShortTaskStatus(task));
    }
    requestStatus.setTasks(shortTasks);
    //todo: null tasks?

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
            //todo: proper log msg
            System.out.println("Unexpected status when creating stage summaries: " + taskStatus);
        }
      }

      //todo: skippable.  I only see a skippable field on the stage, not the tasks
      //todo: time related fields
      HostRoleCommandStatusSummaryDTO stageSummary = new HostRoleCommandStatusSummaryDTO(stage.isSkippable() ? 1 : 0, 0, 0,
          stage.getStageId(), aborted, completed, failed, holding, holdingFailed, holdingTimedout, inProgress, pending, queued, timedout);
      summaryMap.put(stage.getStageId(), stageSummary);
    }
    return summaryMap;
  }

  //todo: context is a temporary refactoring step
  private void createHostRequests(TopologyRequest requestRequest, TopologyManager.ClusterTopologyContext topologyContext) {
    //todo: consistent stage ordering
    //todo: confirm that stages don't need to be unique across requests
    long stageIdCounter = 0;
    Map<String, HostGroupInfo> hostGroupInfoMap = requestRequest.getHostGroupInfo();
    for (HostGroupInfo hostGroupInfo : hostGroupInfoMap.values()) {
      String groupName = hostGroupInfo.getHostGroupName();
      Blueprint blueprint = topology.getBlueprint();
      int hostCardinality;
      List<String> hostnames;

      hostCardinality = hostGroupInfo.getRequestedHostCount();
      hostnames = new ArrayList<String>(hostGroupInfo.getHostNames());


      for (int i = 0; i < hostCardinality; ++i) {
        if (! hostnames.isEmpty()) {
          // host names are specified
          String hostname = hostnames.get(i);
          //todo: pass in HostGroupInfo
          HostRequest hostRequest = new HostRequest(getRequestId(), stageIdCounter++, getClusterName(),
              blueprint.getName(), blueprint.getHostGroup(groupName), hostname, hostGroupInfo.getPredicate(),
              topologyContext);
          synchronized (requestsWithReservedHosts) {
            requestsWithReservedHosts.put(hostname, hostRequest);
          }
        } else {
          // host count is specified
          //todo: pass in HostGroupInfo
          HostRequest hostRequest = new HostRequest(getRequestId(), stageIdCounter++, getClusterName(),
              blueprint.getName(), blueprint.getHostGroup(groupName), hostCardinality, hostGroupInfo.getPredicate(),
              topologyContext);
          outstandingHostRequests.add(hostRequest);
        }
      }
    }

    allHostRequests.addAll(outstandingHostRequests);
    allHostRequests.addAll(requestsWithReservedHosts.values());
  }
}
