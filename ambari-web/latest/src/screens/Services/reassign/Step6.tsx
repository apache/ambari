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

import { useContext, useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { find } from "lodash";
import { Card, Alert } from "react-bootstrap";

import { AppContext } from "../../../store/context";
import { ReassignContext } from "./store/context";
import { ServiceContext } from "../../../store/ServiceContext";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import OperationsProgress from "../../../components/OperationsProgress";
import { getStepData, translateWithVariables } from "../../../Utils/Utility";
import { reassignSteps } from "./constants";
import { ActionTypes } from "./store/types";
import { startServices } from "../../../Utils/taskUtils";
import { HostsApi } from "../../../api/hostsApi";
import { ServiceApi } from "../../../api/serviceApi";


function Step6() {
  const { clusterName, services } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      jumpToStep,
      wizardSteps,
    },
  } = useContext(ReassignContext);
  const { allServiceModels } = useContext(ServiceContext);
  const { componentName } = useParams();

  const [stepOperations, setStepOperations] = useState<any>([]);
  const [isCompleted, setIsCompleted] = useState(false);
  const [hasFailed, setHasFailed] = useState(false);
  const [lastStepFailed, setLastStepFailed] = useState(false);

  const isHAEnabled = allServiceModels?.["hdfs"]?.isNameNodeHaEnabled || false;

  // Get reassign data from state
  const reassignData = getStepData(
    state,
    reassignSteps.ASSIGN_MASTER,
    "masterComponentHosts",
    "reassignSteps"
  );

  const sourceHost = find(
    reassignData,
    (master: any) => master.component_name === componentName && master.isMoving && master.movedHost
  )?.hostName;

  const targetHost = find(
    reassignData,
    (master: any) => master.component_name === componentName && master.isMoving && master.movedHost
  )?.movedHost;

  const serviceName = find(
    services,
    (service: any) =>
      service.ServiceInfo.service_name ===
      getServiceNameForComponent(componentName!)
  )?.ServiceInfo.service_name;

  // Helper functions
  function getServiceNameForComponent(componentName: string): string {
    const componentToServiceMap: { [key: string]: string } = {
      NAMENODE: "HDFS",
      SECONDARY_NAMENODE: "HDFS",
      RESOURCEMANAGER: "YARN",
      HIVE_SERVER: "HIVE",
      HIVE_METASTORE: "HIVE",
      OOZIE_SERVER: "OOZIE",
      MYSQL_SERVER: "HIVE",
      WEBHCAT_SERVER: "HIVE",
      APP_TIMELINE_SERVER: "YARN",
      HISTORYSERVER: "MAPREDUCE2",
    };
    return componentToServiceMap[componentName] || "";
  }

  function getHostComponents(): string[] {
    if (componentName === "NAMENODE" && isHAEnabled) {
      return ["NAMENODE", "ZKFC"];
    }
    return [componentName!];
  }

  function getHostComponentsNames(): string {
    const hostComponents = getHostComponents();
    let hostComponentsNames = "";
    hostComponents.forEach((comp, index) => {
      hostComponentsNames += index ? "+" : "";
      hostComponentsNames += comp;
    });
    return hostComponentsNames;
  }
  //@ts-ignore
  const checkComponentsInstallationStatus = useCallback(async () => {
    // Get the list of components being moved
    const hostComponents = getHostComponents();

    try {
      // Get the current status of all components on the target host
      const response = await HostsApi.getHostComponentsDetails(
        clusterName,
        `fields=host_components/HostRoles/state&host_components/HostRoles/host_name=${targetHost}`
      );

      // Extract all host components
      const hostComponentsData = response.items.flatMap(
        (item: any) => item.host_components || []
      );

      // Check each component we're interested in
      for (const componentName of hostComponents) {
        const component = hostComponentsData.find(
          (comp: any) =>
            comp.HostRoles?.component_name === componentName &&
            comp.HostRoles?.host_name === targetHost
        );

        if (!component) {
          console.warn(
            `Component ${componentName} not found on host ${targetHost}`
          );
          throw new Error(
            `Component ${componentName} not found on host ${targetHost}`
          );
        }

        const state = component.HostRoles?.state;
        console.log(`Component ${componentName} installation state: ${state}`);

        // If component is not in INSTALLED state, throw an error
        if (state !== "INSTALLED") {
          throw new Error(
            `Component ${componentName} is not yet fully installed (current state: ${state}). Waiting for installation to complete...`
          );
        }
      }

      // All components are installed
      return { status: 200 };
    } catch (error) {
      console.error("Error checking component installation status:", error);
      throw error;
    }
  }, [clusterName, targetHost, getHostComponents]);

  function formatComponentName(componentName: string): string {
    return componentName
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  // Operation implementations
  const putHostComponentsInMaintenanceMode = useCallback(async () => {
    const hostComponents = getHostComponents();

    for (const component of hostComponents) {
      await HostsApi.updateHostComponentPassiveState(
        clusterName,
        sourceHost!,
        component,
        {
          context: `Put ${component} in Maintenance Mode`,
          passive_state: "ON",
        }
      );
    }

    return { status: 200 };
  }, [clusterName, sourceHost]);

  const stopHostComponentsInMaintenanceMode = useCallback(async () => {
    const hostComponents = getHostComponents();

    for (const component of hostComponents) {
      await HostsApi.updateHostComponentForHost(
        clusterName,
        sourceHost!,
        component,
        {
          RequestInfo: {
            context: `Stop ${component} in Maintenance Mode`,
          },
          Body: {
            HostRoles: {
              state: "INSTALLED",
            },
          },
        }
      );
    }

    return { status: 200 };
  }, [clusterName, sourceHost]);

  const deleteHostComponents = useCallback(async () => {
    const hostComponents = getHostComponents();

    for (const component of hostComponents) {
      try {
        await HostsApi.deleteHostComponent(clusterName, sourceHost!, component);
      } catch (error: any) {
        // If component doesn't exist, consider it a success
        if (
          error.response?.data
            ?.toString()
            .includes(
              "org.apache.ambari.server.controller.spi.NoSuchResourceException"
            )
        ) {
          console.log(
            `Component ${component} already deleted or doesn't exist`
          );
        } else {
          throw error;
        }
      }
    }

    return { status: 200 };
  }, [clusterName, sourceHost]);

  const startDatanodes = useCallback(async () => {
  return await HostsApi.updateHostComponents(clusterName, "", {
    HostRoles: {
      state: "STARTED",
    },
    context: "Start DataNodes",
    query: "HostRoles/component_name=DATANODE&HostRoles/maintenance_state=OFF",
    taskNum: 1,
    componentName: "DATANODE",
    serviceName: "HDFS",
  });
}, [clusterName]);


  // const startDatanodes = useCallback(async () => {
  //   return await updateComponent(
  //     clusterName,
  //     "DATANODE",
  //     "",
  //     "HDFS",
  //     "Start",
  //     1
  //   );
  // }, [clusterName]);

  const stopMysqlService = useCallback(async () => {
    return await HostsApi.updateHostComponentForHost(
      clusterName,
      sourceHost!,
      "MYSQL_SERVER",
      {
        RequestInfo: {
          context: "Stop MySQL Server",
        },
        Body: {
          HostRoles: {
            state: "INSTALLED",
          },
        },
      }
    );
  }, [clusterName, sourceHost]);

  const installPxf = useCallback(async () => {
    // Check if PXF service exists
    const pxfService = services.find(
      (s: any) => s.ServiceInfo.service_name === "PXF"
    );
    if (!pxfService) return { status: 200 };

    return await ServiceApi.createComponent(clusterName, "PXF", "PXF");
  }, [clusterName, services]);

  const startAllServices = useCallback(async () => {
    return await startServices(clusterName, true, [], false);
  }, [clusterName]);

  // Initialize operations based on component type and configuration
  const initializeOperations = useCallback(() => {
    const ops: any[] = [];
    let opId = 0;

    // Check if PXF needs to be installed
    const hasPxfService = services.some(
      (s: any) => s.ServiceInfo.service_name === "PXF"
    );
    const isNameNodeMove = componentName === "NAMENODE";
    const needsPxfInstall = hasPxfService && isNameNodeMove;

    // Get host components with PXF if needed
    const pxfHosts = hasPxfService
      ? services
          .find((s: any) => s.ServiceInfo.service_name === "PXF")
          ?.ServiceInfo?.components?.find(
            (c: any) => c.ServiceComponentInfo.component_name === "PXF"
          )
          ?.host_components?.map((hc: any) => hc.HostRoles?.host_name) || []
      : [];

    const dataNodeHosts =
      services
        .find((s: any) => s.ServiceInfo.service_name === "HDFS")
        ?.ServiceInfo?.components?.find(
          (c: any) => c.ServiceComponentInfo.component_name === "DATANODE"
        )
        ?.host_components?.map((hc: any) => hc.HostRoles?.host_name) || [];
    //@ts-ignore
    const needsPxfRemoval =
      isNameNodeMove &&
      pxfHosts.includes(sourceHost!) &&
      !dataNodeHosts.includes(sourceHost!);

    // Add operations based on component type
    if (componentName === "MYSQL_SERVER") {
      ops.push({
        id: opId++,
        label: "Stop MySQL Server",
        callback: stopMysqlService,
        skippable: false,
      });
    }

    const hostComponentsNames = getHostComponentsNames();

    ops.push({
      id: opId++,
      label: `Put source ${hostComponentsNames} in Maintenance Mode`,
      callback: putHostComponentsInMaintenanceMode,
      skippable: false,
    });

    ops.push({
      id: opId++,
      label: `Stop ${hostComponentsNames} in Maintenance Mode`,
      callback: stopHostComponentsInMaintenanceMode,
      skippable: false,
    });

    ops.push({
      id: opId++,
      label: `Delete disabled ${hostComponentsNames}`,
      callback: deleteHostComponents,
      skippable: false,
    });

    if (isNameNodeMove && isHAEnabled) {
      ops.push({
        id: opId++,
        label: "Start DataNodes",
        callback: startDatanodes,
        skippable: false,
      });
    }

    if (needsPxfInstall && !pxfHosts.includes(targetHost!)) {
      ops.push({
        id: opId++,
        label: "Install PXF on NameNode",
        callback: installPxf,
        skippable: false,
      });
    }

    // In the initializeOperations function
    // Add this operation after installHostComponents and before startAllServices
    // ops.push({
    //   id: opId++,
    //   label: `Verify ${formatComponentName(componentName!)} Installation`,
    //   callback: checkComponentsInstallationStatus,
    //   skippable: false,
    // });

    ops.push({
      id: opId++,
      label: "Start All Services",
      callback: startAllServices,
      skippable: false,
    });

    return ops;
  }, [
    componentName,
    serviceName,
    sourceHost,
    targetHost,
    isHAEnabled,
    services,
    putHostComponentsInMaintenanceMode,
    stopHostComponentsInMaintenanceMode,
    deleteHostComponents,
    startDatanodes,
    stopMysqlService,
    installPxf,
    startAllServices,
  ]);

  // Effects
  useEffect(() => {
    initializeOperations();
  }, [initializeOperations]);

  const handleCompletionStatus = (completed: boolean) => {
    setIsCompleted(completed);
  };

  const getMessageForAlert = () => {
    if (hasFailed) {
      return translateWithVariables("services.reassign.step6.status.failed", {
        "0": formatComponentName(componentName || ""),
        "1": sourceHost || "",
        "2": targetHost || "",
      });
    } else if (isCompleted) {
      return translateWithVariables("services.reassign.step6.status.success", {
        "0": formatComponentName(componentName || ""),
        "1": sourceHost || "",
        "2": targetHost || "",
      });
    } else {
      return translateWithVariables("services.reassign.step6.status.info", {
        "0": formatComponentName(componentName || ""),
      });
    }
  };


  const savedOperationsState = getStepData(
    state,
    reassignSteps.START_AND_TEST_SERVICES,
    "operationsState",
    "reassignSteps"
  );
  useEffect(() => {
    const operations = (() => {
      const initialOperations = initializeOperations();
      if (savedOperationsState && Array.isArray(savedOperationsState)) {
        return initialOperations.map((originalOp) => {
          const savedOp = savedOperationsState.find(
            (saved: any) => saved.id === originalOp.id
          );
          return savedOp
            ? { ...originalOp, ...savedOp, callback: originalOp.callback }
            : originalOp;
        });
      }

      return initialOperations;
    })();
    setStepOperations(operations);
  }, [JSON.stringify(savedOperationsState)])

  // Monitor stepOperations to detect if only the last step failed
  useEffect(() => {
    if (stepOperations.length > 0) {
      const lastOperationIndex = stepOperations.length - 1;
      const lastOperation = stepOperations[lastOperationIndex];
      
      // Check if last operation failed
      const isLastStepFailed = lastOperation?.status === 'FAILED';
      
      // Check if all previous operations completed successfully
      const allPreviousStepsCompleted = stepOperations
        .slice(0, lastOperationIndex)
        .every((op: any) => op.status === 'COMPLETED');
      
      // Check if any previous step failed
      const anyPreviousStepFailed = stepOperations
        .slice(0, lastOperationIndex)
        .some((op: any) => op.status === 'FAILED');
      
      // Set states based on conditions
      setHasFailed(anyPreviousStepFailed);
      setLastStepFailed(isLastStepFailed && allPreviousStepsCompleted && !anyPreviousStepFailed);
    }
  }, [stepOperations]);


  if (!stepOperations.length) {
    return (
      <div
        className="d-flex justify-content-center align-items-center"
        style={{ height: "200px" }}
      >
        <div>Loading operations...</div>
      </div>
    );
  }

  return (
    <>
      <div className="step-title">Start and Test Services</div>
      <div className="step-description mt-1">
        Monitor the progress as we finalize the{" "}
        {formatComponentName(componentName!)} move.
      </div>

      <Alert variant={hasFailed ? "danger" : isCompleted ? "success" : "info"} className="mb-4">
        {getMessageForAlert()}
      </Alert>

      <Card className="mt-3">
        <Card.Header>
          <h5 className="mb-0">Progress</h5>
        </Card.Header>
        <Card.Body>
          <OperationsProgress
            title={`Start and Test Services`}
            description={`Finalizing ${formatComponentName(
              componentName!
            )} move from ${sourceHost} to ${targetHost}`}
            setCompletionStatus={handleCompletionStatus}
            operations={stepOperations as any}
            dispatch={(operationsState: any) => {
              dispatch({
                type: ActionTypes.STORE_INFORMATION,
                payload: {
                  step: currentStep.name,
                  data: {
                    operationsState,
                  },
                },
              });
            }}
          />
        </Card.Body>
      </Card>


      <WizardFooter
        step={{
          ...currentStep,
          nextLabel: "Complete",
        }}
        isNextEnabled={isCompleted || lastStepFailed}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(5);
        }}
        onNext={() => {
          // Find current step number
          const currentStepNumber = Object.keys(wizardSteps).find(
            (stepNum) => wizardSteps[stepNum].name === currentStep.name
          );
          
          // Check if there's a next step
          const nextStepNumber = currentStepNumber ? parseInt(currentStepNumber) + 1 : null;
          const hasNextStep = nextStepNumber && wizardSteps[nextStepNumber];
          
          if (hasNextStep) {
            // If there's a next step, proceed to it
            flushStateToDb("next");
            handleNextImperitive();
          } else {
            // If this is the last step, complete the wizard and redirect
            flushStateToDb("complete");
            window.location.href = `/#/main/services/${serviceName}/summary`;
          }
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step6;
