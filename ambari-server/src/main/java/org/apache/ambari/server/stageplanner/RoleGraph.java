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
package org.apache.ambari.server.stageplanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RoleGraph {

  private static Log LOG = LogFactory.getLog(RoleGraph.class);

  Map<String, RoleGraphNode> graph = null;
  private RoleCommandOrder roleDependencies;
  private Stage initialStage = null;
  private boolean sameHostOptimization = true;

  public RoleGraph() {
  }
  
  public RoleGraph(RoleCommandOrder rd) {
    this.roleDependencies = rd;
  }

  /**
   * Given a stage builds a DAG of all execution commands within the stage.
   */
  public void build(Stage stage) {
    if (stage == null) {
      throw new IllegalArgumentException("Null stage");
    }
    graph = new TreeMap<String, RoleGraphNode>();
    initialStage = stage;

    Map<String, Map<String, HostRoleCommand>> hostRoleCommands = stage.getHostRoleCommands();
    for (String host : hostRoleCommands.keySet()) {
      for (String role : hostRoleCommands.get(host).keySet()) {
        HostRoleCommand hostRoleCommand = hostRoleCommands.get(host).get(role);
        RoleGraphNode rgn;
        if (graph.get(role) == null) {
          rgn = new RoleGraphNode(hostRoleCommand.getRole(),
              hostRoleCommand.getRoleCommand());
          graph.put(role, rgn);
        }
        rgn = graph.get(role);
        rgn.addHost(host);
      }
    }

    if (null != roleDependencies) {
      //Add edges
      for (String roleI : graph.keySet()) {
        for (String roleJ : graph.keySet()) {
          if (!roleI.equals(roleJ)) {
            RoleGraphNode rgnI = graph.get(roleI);
            RoleGraphNode rgnJ = graph.get(roleJ);
            int order = roleDependencies.order(rgnI, rgnJ);
            if (order == -1) {
              rgnI.addEdge(rgnJ);
            } else if (order == 1) {
              rgnJ.addEdge(rgnI);
            }
          }
        }
      }
    }
  }

  /**
   * Returns a list of stages that need to be executed one after another
   * to execute the DAG generated in the last {@link #build(Stage)} call.
   */
  public List<Stage> getStages() {
    long initialStageId = initialStage.getStageId();
    List<Stage> stageList = new ArrayList<Stage>();
    List<RoleGraphNode> firstStageNodes = new ArrayList<RoleGraphNode>();
    while (!graph.isEmpty()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(this.stringifyGraph());
      }

      for (String role: graph.keySet()) {
        RoleGraphNode rgn = graph.get(role);
        if (rgn.getInDegree() == 0) {
          firstStageNodes.add(rgn);
        }
      }
      Stage aStage = getStageFromGraphNodes(initialStage, firstStageNodes);
      aStage.setStageId(++initialStageId);
      stageList.add(aStage);
      //Remove first stage nodes from the graph, we know that none of
      //these nodes have an incoming edges.
      for (RoleGraphNode rgn : firstStageNodes) {
        if (this.sameHostOptimization) {
          //Perform optimization
        }
        removeZeroInDegreeNode(rgn.getRole().toString());
      }
      firstStageNodes.clear();
    }
    return stageList;
  }

  /**
   * Assumes there are no incoming edges.
   */
  private synchronized void removeZeroInDegreeNode(String role) {
    RoleGraphNode nodeToRemove = graph.remove(role);
    for (RoleGraphNode edgeNode: nodeToRemove.getEdges()) {
      edgeNode.decrementInDegree();
    }
  }

  private Stage getStageFromGraphNodes(Stage origStage,
      List<RoleGraphNode> stageGraphNodes) {

    Stage newStage = new Stage(origStage.getRequestId(),
        origStage.getLogDir(), origStage.getClusterName(),
        origStage.getClusterId(),
        origStage.getRequestContext(), origStage.getClusterHostInfo(),
        origStage.getCommandParamsStage(), origStage.getHostParamsStage());
    newStage.setSuccessFactors(origStage.getSuccessFactors());
    for (RoleGraphNode rgn : stageGraphNodes) {
      for (String host : rgn.getHosts()) {
        newStage.addExecutionCommandWrapper(origStage, host, rgn.getRole());
      }
    }
    return newStage;
  }

  public String stringifyGraph() {
    StringBuilder builder = new StringBuilder();
    builder.append("Graph:\n");
    for (String role : graph.keySet()) {
      builder.append(graph.get(role));
      for (RoleGraphNode rgn : graph.get(role).getEdges()) {
        builder.append(" --> ");
        builder.append(rgn);
      }
      builder.append("\n");
    }
    return builder.toString();
  }
}
