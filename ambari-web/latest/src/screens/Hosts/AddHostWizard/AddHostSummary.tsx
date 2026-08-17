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

import { useContext, useRef, useState } from "react";
import { Alert, Card, ListGroup } from "react-bootstrap";
import { get } from "lodash";
import { ContextWrapper } from "../../ClusterWizard";
import WizardFooter from "../../../components/StepWizard/WizardFooter";

const failedTaskStatuses = new Set(["ABORTED", "FAILED", "TIMEDOUT"]);

export default function AddHostSummary() {
  const { Context } = useContext(ContextWrapper);
  const {
    flushStateToDb,
    state,
    stepWizardUtilities: { currentStep },
  }: any = useContext(Context);
  const hosts = get(
    state,
    "addHostSteps.INSTALL_START_TEST.data.hostInfo",
    [],
  );
  const clusterStatus = get(
    state,
    "addHostSteps.INSTALL_START_TEST.data.clusterStatus",
    {},
  );
  const [finishError, setFinishError] = useState("");
  const finishing = useRef(false);

  const successfulHosts = hosts.filter((host: any) => host.status === "success");
  const warningHosts = hosts.filter((host: any) => host.status === "warning");
  const failedHosts = hosts.filter((host: any) => host.status === "failed");
  const failedTasks = hosts.flatMap((host: any) => (host.logTasks || [])
    .filter((task: any) => failedTaskStatuses.has(task.Tasks?.status))
    .map((task: any) => ({
      hostName: host.name,
      role: task.Tasks?.role || "Unknown task",
      status: task.Tasks?.status,
    })));

  const finish = async () => {
    if (finishing.current) return;
    finishing.current = true;
    setFinishError("");
    try {
      await Promise.resolve(flushStateToDb("cancel"));
    } catch (error: any) {
      setFinishError(
        error?.response?.data?.message
          || error?.message
          || "Ambari could not clear the Add Host wizard state.",
      );
      finishing.current = false;
    }
  };

  return (
    <>
      <div className="step-title">Summary</div>
      <p className="step-description mt-2">
        Here is the result of adding the selected hosts.
      </p>

      {finishError ? <Alert variant="danger">{finishError}</Alert> : null}
      <Alert variant={clusterStatus.status === "STARTED" ? "success" : "warning"}>
        Deployment status: <b>{clusterStatus.status || "Unknown"}</b>
      </Alert>

      <Card className="mt-3">
        <Card.Header>Host Results</Card.Header>
        <ListGroup variant="flush">
          <ListGroup.Item className="text-success">
            {successfulHosts.length} host(s) completed successfully
          </ListGroup.Item>
          <ListGroup.Item className="text-warning">
            {warningHosts.length} host(s) completed with warnings
          </ListGroup.Item>
          <ListGroup.Item className="text-danger">
            {failedHosts.length} host(s) failed
          </ListGroup.Item>
        </ListGroup>
      </Card>

      {failedTasks.length ? (
        <Card className="mt-3 mb-5">
          <Card.Header>Failed Tasks</Card.Header>
          <ListGroup variant="flush">
            {failedTasks.map((task: any, index: number) => (
              <ListGroup.Item key={`${task.hostName}-${task.role}-${index}`}>
                <b>{task.hostName}</b>: {task.role} ({task.status})
              </ListGroup.Item>
            ))}
          </ListGroup>
        </Card>
      ) : null}

      <WizardFooter
        isNextEnabled={!finishing.current}
        isBackEnabled={false}
        lifted
        step={{ ...currentStep, nextLabel: "COMPLETE" }}
        onNext={() => void finish()}
        onCancel={() => void finish()}
        onBack={() => {}}
      />
    </>
  );
}
