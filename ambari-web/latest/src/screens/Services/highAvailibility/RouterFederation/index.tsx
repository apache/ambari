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

import { useContext } from "react";
import { Alert, Button } from "react-bootstrap";
import StepWizard from "../../../../components/StepWizard";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getHdfsNamespaces } from "../haWorkflowUtils";
import {
  PersistedWorkflowContext,
  PersistedWorkflowProvider,
} from "../Federation/PersistedWorkflowContext";
import useHdfsWorkflowCapabilities from "../useHdfsWorkflowCapabilities";
import wizardSteps from "./wizardSteps";

export default function RouterFederationWizard() {
  const {
    allModelsLoaded,
    allServiceModels,
    masterSlaveClientsData,
  }: any =
    useContext(ServiceContext);
  const {
    capabilities,
    error: capabilityError,
    isLoading: isCapabilityLoading,
    retry: retryCapabilities,
  } = useHdfsWorkflowCapabilities();
  const wizardUtilities = useStepWizard(wizardSteps, 0);
  const hdfsModel = allServiceModels.hdfs;
  const namespaces = getHdfsNamespaces(hdfsModel);
  const componentData = Object.values(masterSlaveClientsData || {}) as any[];
  const isFullyStarted = (componentName: string) => {
    const component = componentData.find(
      (item) =>
        item?.ServiceComponentInfo?.component_name === componentName,
    );
    const total = Number(component?.ServiceComponentInfo?.total_count || 0);
    const started = Number(component?.ServiceComponentInfo?.started_count || 0);
    return total > 0 && started === total;
  };
  if (isCapabilityLoading) {
    return <div className="d-flex justify-content-center p-5"><Spinner /></div>;
  }
  if (capabilityError) {
    return (
      <Alert variant="danger">
        {capabilityError}
        <Button size="sm" className="ms-3" onClick={retryCapabilities}>
          Retry
        </Button>
      </Alert>
    );
  }
  if (!componentData.length) {
    if (!allModelsLoaded) {
      return <div className="d-flex justify-content-center p-5"><Spinner /></div>;
    }
    return (
      <Alert variant="danger">
        Ambari loaded no host-component topology for Router-based Federation.
        <Button size="sm" className="ms-3" onClick={retryCapabilities}>
          Retry
        </Button>
      </Alert>
    );
  }
  if (
    !allModelsLoaded ||
    !hdfsModel?.isNamespaceLoaded ||
    namespaces.length < 2 ||
    !capabilities.routerFederation
  ) {
    return (
      <Alert variant="danger">
        Router-based Federation requires a stack with the ROUTER component and
        an HDFS cluster with at least two loaded nameservices.
      </Alert>
    );
  }
  if (!isFullyStarted("ZOOKEEPER_SERVER") || !isFullyStarted("JOURNALNODE")) {
    return (
      <Alert variant="danger">
        All ZooKeeper Servers and JournalNodes must be started before enabling
        Router-based Federation.
      </Alert>
    );
  }
  return (
    <PersistedWorkflowProvider
      storageKey="ROUTER_FEDERATION"
      controllerName="routerFederationWizardController"
      progressStatus="ENABLING_ROUTER_FEDERATION"
      progressStepIndex={3}
      summaryUrl="/#/main/services/HDFS/summary"
      stepWizardUtilities={wizardUtilities}
    >
      <StepWizard
        wizardUtilities={wizardUtilities}
        Context={PersistedWorkflowContext}
      />
    </PersistedWorkflowProvider>
  );
}
