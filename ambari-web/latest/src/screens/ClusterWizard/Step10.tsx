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

import { useContext, useEffect, useRef, useState } from "react";
import { cloneDeep, filter, find, flatten, get, map, some, uniq } from "lodash";
import { pluralize, role } from "../../Utils/Utility";
import { Alert, Card, CardBody } from "react-bootstrap";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import ClusterApi from "../../api/clusterApi";
import { ContextWrapper } from ".";

type Step10Props = {
  wizardName?: string;
};

function Step10({ wizardName = "clusterCreation" }: Step10Props) {
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  }: any = useContext(Context);
  const [clusterInfoState, setClusterInfoState] = useState([]);
  const [finishError, setFinishError] = useState("");
  const [isFinishing, setIsFinishing] = useState(false);
  const clusterInfo = useRef<any>([]);
  const installFlag = useRef<boolean>(true);
  const startFlag = useRef<boolean>(true);

  const getStepData = (stepName: string, dataKey: string) => {
    const stepData = get(state, `${wizardName}Steps.${stepName}.data`, {});
    return get(stepData, dataKey, "");
  };
  function loadInstalledHosts() {
    //Return from Step 9
    const hostsInfo: any = getStepData("INSTALL_START_TEST", "hostInfo");
    const clusterStatus: any = getStepData(
      "INSTALL_START_TEST",
      "clusterStatus"
    );
    const succeededHosts = filter(hostsInfo, ["status", "success"]);
    const warnedHosts = hostsInfo.filter(function (host: any) {
      return ["warning", "failed"].includes(host.status);
    });
    const clusterInfoCopy = cloneDeep(clusterInfo.current);
    if (succeededHosts.length) {
      const successStatement =
        clusterStatus.status === "START_SKIPPED"
          ? `Installed services successfully on ${
              succeededHosts.length
            } new ${pluralize(succeededHosts.length, "host")}`
          : `Installed and started services successfully on ${
              succeededHosts.length
            } new ${pluralize(succeededHosts.length, "host")}`;
      get(find(clusterInfoCopy, ["id", 1]), "status").push({
        id: 1,
        color: "text-success",
        displayStatement: successStatement,
      });
    }
    if (warnedHosts.length) {
      const warnStatement = warnedHosts.length + "warnings";
      get(find(clusterInfoCopy, ["id", 1]), "status").push({
        id: 2,
        color: "text-warning",
        displayStatement: warnStatement,
        statements: [],
      });
      warnedHosts.forEach(function (_host: any) {
        let clusterState: any = "";
        if (clusterStatus.status === "INSTALL FAILED") {
          clusterState = "Installing";
        } else {
          if (clusterStatus.status === "START FAILED") {
            clusterState = "Starting";
          }
        }

        [
          { Tst: "FAILED", st: "failed" },
          { Tst: "ABORTED", st: "aborted" },
          { Tst: "TIMEDOUT", st: "timedout" },
        ].forEach(function (s: any) {
          filter(_host.logTasks, ["Tasks.status", s.Tst]).forEach(function (
            _task: any
          ) {
            var statement =
              clusterState +
              role(_task.Tasks.role, false) +
              "failed on" +
              _host.name;
            get(
              find(get(find(clusterInfoCopy, ["id", 1]), "status"), ["id", 2]),
              "statements"
            ).push({
              status: s.st,
              color: "text-info",
              displayStatement: statement,
            });
          });
        });
      });
    }
    clusterInfo.current = clusterInfoCopy;
  }
  function loadRegisteredHosts() {
    const mastersData: any = getStepData("MASTERS", "mastersData");
    const allMasterServices = flatten(map(mastersData.masterServices));
    let masterHosts = uniq(
      map(filter(allMasterServices, ["isInstalled", false]), "hostName")
    );
    let slaveData: any = getStepData("SLAVES_AND_CLIENTS", "serviceComponents");
    const slaveHosts = uniq(
      map(
        filter(slaveData, function (hostSlave) {
          return !!some(hostSlave.checkboxes, ["checked", true]);
        }),
        "hostname"
      )
    );
    const allRegisteredHosts = map(
      getStepData("INSTALL_START_TEST", "hostInfo"),
      "name"
    );
    let installedHosts = getStepData("HOSTS", "installedHosts");
    if (!installedHosts) {
      installedHosts = [];
    }
    let registeredHosts = uniq([
      ...allRegisteredHosts,
      ...masterHosts,
      ...slaveHosts,
      ...installedHosts,
    ]);
    const registerHostsStatement = `The cluster consists of ${pluralize(
      registeredHosts.length,
      "host"
    )}`;
    const registerHostsObj: any = {
      id: 1,
      color: "text-info",
      displayStatement: registerHostsStatement,
      status: [],
    };
    clusterInfo.current.push(registerHostsObj);
    return registerHostsObj;
  }
  function loadMasterComponent(component: any, clusterInfoCopy: any) {
    if (component.hostName) {
      var statement = `${component.display_name} installed on ${component.hostName}`;
      get(find(clusterInfoCopy, ["id", 2]), "status").push({
        id: 1,
        color: "text-info",
        displayStatement: statement,
      });
    }
  }
  function loadMasterComponents() {
    const clusterStatus: any = getStepData(
      "INSTALL_START_TEST",
      "clusterStatus"
    );
    const mastersData: any = getStepData("MASTERS", "mastersData");
    const clusterInfoCopy = cloneDeep(clusterInfo.current);
    var components = flatten(map(mastersData, "masterServices"));
    if (clusterStatus.status === "INSTALL FAILED") {
      clusterInfoCopy.push({
        id: 2,
        displayStatement: "Installing master services failed",
        color: "text-danger",
        status: [],
      });
      return false;
    } else {
      clusterInfoCopy.push({
        id: 2,
        displayStatement: "Master services installed",
        color: "text-success",
        status: [],
      });
    }

    components.forEach(function (component: any) {
      if (
        [
          "NAMENODE",
          "SECONDARY_NAMENODE",
          "JOBTRACKER",
          "HISTORYSERVER",
          "RESOURCEMANAGER",
          "HBASE_MASTER",
          "HIVE_SERVER",
          "OOZIE_SERVER",
          "GANGLIA_SERVER",
        ].includes(component.component)
      ) {
        loadMasterComponent(component, clusterInfoCopy);
      }
    });
    clusterInfo.current = clusterInfoCopy;
    return true;
  }
  function loadStartedServices() {
    const clusterStatus: any = getStepData(
      "INSTALL_START_TEST",
      "clusterStatus"
    );
    const clusterInfoCopy = cloneDeep(clusterInfo.current);
    let startedServices = false;
    if (clusterStatus.status === "STARTED") {
      clusterInfoCopy.push({
        id: 3,
        color: "text-success",
        displayStatement: "All services started",
        status: [],
      });
      clusterInfoCopy.push({
        id: 4,
        color: "text-success",
        displayStatement: "All tests passed",
        status: [],
      });
      startedServices = true;
    } else if (clusterStatus.status === "START_SKIPPED") {
      clusterInfoCopy.push({
        id: 3,
        color: "text-warning",
        displayStatement: "Starting services skipped",
        status: [],
      });
      startedServices = false;
    } else {
      clusterInfoCopy.push({
        id: 3,
        color: "text-danger",
        displayStatement: "Starting services failed",
        status: [],
      });
      startedServices = false;
    }
    clusterInfo.current = clusterInfoCopy;
    return startedServices;
  }
  useEffect(() => {
    loadRegisteredHosts();
    loadInstalledHosts();
    installFlag.current = loadMasterComponents();
    startFlag.current = loadStartedServices();
  }, []);
  useEffect(() => {
    const uniqIds = uniq(map(clusterInfo.current, "id"));
    const uniqueRecords: any = [];
    uniqIds.forEach((id) => {
      const record = find(clusterInfo.current, ["id", id]);
      if (record) {
        const recordStatus = map(record.status, "displayStatement");
        const uniqueDisplayStatements = uniq(recordStatus);
        record.status = uniqueDisplayStatements.map((displayStatement: any) => {
          return {
            displayStatement,
            color: find(record.status, ["displayStatement", displayStatement])
              ?.color,
          };
        });
        uniqueRecords.push(record);
      }
    });
    setClusterInfoState(uniqueRecords);
  }, [clusterInfo.current]);

  const finish = async () => {
    if (isFinishing) return;
    setIsFinishing(true);
    setFinishError("");
    try {
      if (wizardName === "clusterCreation") {
        const clusterName = getStepData("NAME", "clusterName");
        await ClusterApi.updateCluster(clusterName, {
          Clusters: { provisioning_state: "INSTALLED" },
        });
      }
      await Promise.resolve(flushStateToDb("complete"));
      if (wizardName === "clusterCreation") {
        window.location.href = "/#/main/dashboard/metrics";
        window.location.reload();
      }
    } catch (error: any) {
      setFinishError(
        error?.response?.data?.message
          || error?.message
          || "Ambari could not complete the installation workflow.",
      );
      setIsFinishing(false);
    }
  };

  return (
    <>
      <div className="step-title">Summary</div>
      <p className="step-description mt-2">
        Here is the summary of the install process.
      </p>
      {finishError ? <Alert variant="danger">{finishError}</Alert> : null}
      <Card className="mt-2">
        <CardBody>
          {clusterInfoState.map((info: any) => {
            return (
              <div key={info.id} className={`${info.color} mt-2`}>
                {info?.displayStatement}
                <div className="ms-2">
                  {info.status.map((status: any, index: number) => {
                    return (
                      <div
                        key={`${info.id}-${status.displayStatement}-${index}`}
                        className={` mt-2 ${status.color}`}
                      >
                        {status.displayStatement}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </CardBody>
      </Card>
      <WizardFooter
        isNextEnabled={!isFinishing}
        isCancelEnabled={!isFinishing}
        step={{ ...currentStep, nextLabel: "COMPLETE" }}
        onNext={() => void finish()}
        onCancel={() => void finish()}
        onBack={() => {}}
      />
    </>
  );
}

export default Step10;
