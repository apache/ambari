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
package org.apache.ambari.server.agent.stomp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ComponentStatus;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.stomp.dto.CommandStatusReports;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReport;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReports;
import org.apache.ambari.server.agent.stomp.dto.HostStatusReport;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.google.inject.Injector;

@Controller
@SendToUser("/")
@MessageMapping("/reports")
public class AgentReportsController {
  private static final Logger LOG = LoggerFactory.getLogger(AgentReportsController.class);
  private final HeartBeatHandler hh;
  private final AgentSessionManager agentSessionManager;

  public AgentReportsController(Injector injector) {
    hh = injector.getInstance(HeartBeatHandler.class);
    agentSessionManager = injector.getInstance(AgentSessionManager.class);
  }

  @SubscribeMapping("/component_status")
  public void handleComponentReportStatus(@Header String simpSessionId, ComponentStatusReports message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {
    List<ComponentStatus> statuses = new ArrayList<>();
    for (Map.Entry<String, List<ComponentStatusReport>> clusterReport : message.getComponentStatusReports().entrySet()) {
      for (ComponentStatusReport report : clusterReport.getValue()) {
        ComponentStatus componentStatus = new ComponentStatus();
        componentStatus.setClusterId(report.getClusterId());
        componentStatus.setComponentName(report.getComponentName());
        componentStatus.setServiceName(report.getServiceName());
        if (report.getCommand().equals(ComponentStatusReport.CommandStatusCommand.STATUS)) {
          componentStatus.setStatus(report.getStatus());
        } else {
          componentStatus.setSecurityState(report.getStatus());
        }
        statuses.add(componentStatus);
      }
    }

    hh.handleComponentReportStatus(statuses,
        agentSessionManager.getHost(simpSessionId).getHostName());
  }

  @SubscribeMapping("/commands_status")
  public void handleCommandReportStatus(@Header String simpSessionId, CommandStatusReports message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {
    List<CommandReport> statuses = new ArrayList<>();
    for (Map.Entry<String, List<CommandReport>> clusterReport : message.getClustersComponentReports().entrySet()) {
      statuses.addAll(clusterReport.getValue());
    }

    hh.handleCommandReportStatus(statuses,
        agentSessionManager.getHost(simpSessionId).getHostName());
  }

  @SubscribeMapping("/host_status")
  public void handleHostReportStatus(@Header String simpSessionId, HostStatusReport message) throws AmbariException {
    hh.handleHostReportStatus(message, agentSessionManager.getHost(simpSessionId).getHostName());
  }

  @SubscribeMapping("/alerts_status")
  public void handleAlertsStatus(@Header String simpSessionId, Alert[] message) throws AmbariException {
    String hostName = agentSessionManager.getHost(simpSessionId).getHostName();
    List<Alert> alerts = Arrays.asList(message);
    LOG.info("Handling {} alerts status for host {}", alerts.size(), hostName);
    hh.getHeartbeatProcessor().processAlerts(hostName, alerts);
  }

}
