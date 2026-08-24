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

import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Dropdown, Spinner, Table } from "react-bootstrap";
import { get } from "lodash";
import {
  canStartFlumeAgent,
  canStopFlumeAgent,
  extractFlumeAgents,
  FlumeAgent,
} from "../../Utils/flumeAgents";
import { isOperationTerminal } from "../../Utils/backgroundOperations";
import { ServiceApi } from "../../api/serviceApi";
import { cachedServiceApi } from "../../api/cachedServiceApi";
import BackgroundOperations from "../BackgroundOperations";
import modalManager from "../../store/ModalManager";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";

type FlumeAction = "start" | "stop";

type PendingAgent = {
  action: FlumeAction;
  agent: FlumeAgent;
  requestId?: number;
  targetStatus: FlumeAgent["status"];
  refreshing?: boolean;
};

const displayStatus = (status: FlumeAgent["status"]) => ({
  RUNNING: "Running",
  NOT_RUNNING: "Stopped",
  UNKNOWN: "Unknown",
})[status];

function FlumeSummary() {
  const {
    backgroundOperations,
    clusterName,
  } = useContext(AppContext);
  const { masterSlaveClientsData } = useContext(ServiceContext);
  const { isAuthorized } = useAuthorizationPolicy();
  const [pendingAgents, setPendingAgents] = useState<Record<string, PendingAgent>>({});
  const submitCommandRef = useRef<
    (_agent: FlumeAgent, _action: FlumeAction) => Promise<void>
  >(async () => undefined);
  const agents = useMemo(
    () => extractFlumeAgents(masterSlaveClientsData),
    [masterSlaveClientsData]
  );
  const canStartStop = isAuthorized("SERVICE.START_STOP");

  const removePendingAgent = useCallback((agentId: string) => {
    setPendingAgents((current) => {
      const next = { ...current };
      delete next[agentId];
      return next;
    });
  }, []);

  const showFailure = useCallback((pending: PendingAgent, error: unknown) => {
    const label = pending.action === "start" ? "Start" : "Stop";
    const message = get(
      error,
      "response.data.message",
      get(error, "message", `${label} Flume agent could not be submitted.`)
    );
    modalManager.show({
      modalTitle: `${label} Flume Agent Failed`,
      modalBody: message,
      successCallback: () => {
        modalManager.hide();
        void submitCommandRef.current(pending.agent, pending.action);
      },
      onClose: () => modalManager.hide(),
      options: { okButtonText: "RETRY" },
    });
  }, []);

  const submitCommand = useCallback(async (
    agent: FlumeAgent,
    action: FlumeAction
  ) => {
    const targetState = action === "start" ? "STARTED" : "INSTALLED";
    const targetStatus = action === "start" ? "RUNNING" : "NOT_RUNNING";
    const label = action === "start" ? "Start" : "Stop";
    const pending = { action, agent, targetStatus } as PendingAgent;
    setPendingAgents((current) => ({ ...current, [agent.id]: pending }));

    try {
      const response = await ServiceApi.updateFlumeAgent(
        clusterName,
        agent.hostName,
        agent.name,
        targetState,
        `${label} Flume Agent ${agent.name}`
      );
      const requestId = Number(get(response, "data.Requests.id"));
      if (Number.isFinite(requestId)) {
        setPendingAgents((current) => ({
          ...current,
          [agent.id]: { ...pending, requestId },
        }));
        modalManager.show(
          <BackgroundOperations
            isOpen
            onClose={() => modalManager.hide()}
            requestId={requestId}
          />
        );
      } else {
        await cachedServiceApi.fetchAllServiceComponents(clusterName, true);
        removePendingAgent(agent.id);
      }
    } catch (error) {
      removePendingAgent(agent.id);
      showFailure(pending, error);
    }
  }, [clusterName, removePendingAgent, showFailure]);
  submitCommandRef.current = submitCommand;

  const confirmCommand = (agent: FlumeAgent, action: FlumeAction) => {
    const label = action === "start" ? "Start" : "Stop";
    modalManager.show({
      modalTitle: "Confirmation",
      modalBody: `${label} Flume agent ${agent.name} on ${agent.hostName}?`,
      successCallback: () => {
        modalManager.hide();
        void submitCommand(agent, action);
      },
      onClose: () => modalManager.hide(),
      options: { okButtonText: label.toUpperCase() },
    });
  };

  useEffect(() => {
    agents.forEach((agent) => {
      const pending = pendingAgents[agent.id];
      if (pending && agent.status === pending.targetStatus) {
        removePendingAgent(agent.id);
      }
    });
  }, [agents, pendingAgents, removePendingAgent]);

  useEffect(() => {
    Object.values(pendingAgents).forEach((pending) => {
      if (!pending.requestId || pending.refreshing) return;
      const request = backgroundOperations.find(
        (operation) => Number(operation?.Requests?.id) === pending.requestId
      );
      const status = request?.Requests?.request_status;
      if (!isOperationTerminal(status)) return;

      if (status === "COMPLETED") {
        setPendingAgents((current) => ({
          ...current,
          [pending.agent.id]: { ...pending, refreshing: true },
        }));
        void cachedServiceApi
          .fetchAllServiceComponents(clusterName, true)
          .finally(() => removePendingAgent(pending.agent.id));
      } else {
        removePendingAgent(pending.agent.id);
        showFailure(pending, new Error(`The background request ended with status ${status}.`));
      }
    });
  }, [
    backgroundOperations,
    clusterName,
    pendingAgents,
    removePendingAgent,
    showFailure,
  ]);

  if (!masterSlaveClientsData) {
    return <Spinner animation="border" size="sm" />;
  }

  if (agents.length === 0) {
    return <div className="text-muted">No Flume agents are available.</div>;
  }

  return (
    <Table responsive hover size="sm" className="mb-0">
      <thead>
        <tr>
          <th>Host</th>
          <th>Agent</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {agents.map((agent) => {
          const isPending = Boolean(pendingAgents[agent.id]);
          const status = isPending ? "Pending" : displayStatus(agent.status);
          return (
            <tr key={agent.id}>
              <td>{agent.hostName}</td>
              <td>{agent.name}</td>
              <td>
                {canStartStop ? (
                  <Dropdown>
                    <Dropdown.Toggle
                      size="sm"
                      variant="light"
                      disabled={isPending}
                      aria-label={`Actions for Flume agent ${agent.name} on ${agent.hostName}`}
                    >
                      {isPending && <Spinner animation="border" size="sm" className="me-1" />}
                      {status}
                    </Dropdown.Toggle>
                    <Dropdown.Menu>
                      <Dropdown.Item
                        disabled={!canStartFlumeAgent(agent.status)}
                        onClick={() => confirmCommand(agent, "start")}
                      >
                        Start Agent
                      </Dropdown.Item>
                      <Dropdown.Item
                        disabled={!canStopFlumeAgent(agent.status)}
                        onClick={() => confirmCommand(agent, "stop")}
                      >
                        Stop Agent
                      </Dropdown.Item>
                    </Dropdown.Menu>
                  </Dropdown>
                ) : status}
              </td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
}

export default FlumeSummary;
