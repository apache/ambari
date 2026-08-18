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

import { useContext, useState } from "react";
import { AppContext } from "../../../../store/context";
import { find, flatten, get, map } from "lodash";
import { ActionTypes } from "./store/types";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import AssignMastersAddable from "../../../../components/AssignMastersAddable";
import { Card } from "react-bootstrap";
import { ManageJournalNodesContext } from "./store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getJournalNodeChangeSet } from "../haWorkflowUtils";

function Step1() {
  const { services, allHostNames } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const {
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { handleNextImperitive, currentStep },
  } = useContext(ManageJournalNodesContext);
  const journalNodeComponent = find(
    flatten([
      allServiceModels?.hdfs?.masterComponents || [],
      allServiceModels?.hdfs?.slaveComponents || [],
    ]),
    ["componentName", "JOURNALNODE"],
  );
  const originalJournalNodeHosts = map(
    journalNodeComponent?.hostComponents ||
      allServiceModels?.hdfs?.journalNodes ||
      [],
    (hostComponent: any) =>
      get(hostComponent, "HostRoles.host_name", hostComponent.hostName),
  ).filter(Boolean);
  const existingCount = Math.max(
    journalNodeComponent?.totalCount || 0,
    originalJournalNodeHosts.length,
  );
  const maximumJournalNodes = Math.min(
    allHostNames.length,
    Math.max(3, existingCount * 2 - 1),
  );
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  return (
    <>
      <div className="step-title">Assign JournalNodes</div>
      <div className="step-description">
        Add or remove JournalNodes
      </div>
      <Card className="mt-2">
        <Card.Body>
          <AssignMastersAddable
            showJournalNode
            mastersToShow={["JOURNALNODE"]}
            mastersToAdd={[]}
            mastersToCreate={[]}
            showCurrentPrefix={["JOURNALNODE"]}
            showAdditionalPrefix={["JOURNALNODE"]}
            services={map(services, "ServiceInfo.service_name")}
            mastersAddableInHA={["JOURNALNODE"]}
            maximumMasterCount={maximumJournalNodes}
            dispatch={(payload:any) => {
              const changeSet = getJournalNodeChangeSet(
                payload.masterComponentHosts,
                originalJournalNodeHosts,
              );
              setIsNextEnabled(
                changeSet.finalHosts.length >= 3 &&
                  changeSet.finalHosts.length <= maximumJournalNodes &&
                  !changeSet.isNoOp,
              );
              dispatch({
                type: ActionTypes.STORE_INFORMATION,
                payload: {
                  step: currentStep.name,
                  data: payload,
                },
              });
              void flushStateToDb();
            }}
          />
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        onBack={() => {}}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
      />
    </>
  );
}
export default Step1;
