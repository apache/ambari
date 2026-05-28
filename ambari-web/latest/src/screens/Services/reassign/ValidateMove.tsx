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


import {useState,useContext,useEffect } from "react";
import useStepWizard from "../../../hooks/useStepWizard";
import Spinner from "../../../components/Spinner";
import { ReassignProvider, ReassignContext } from "./store/context";
import StepWizard from "../../../components/StepWizard";
import Modal from "../../../components/Modal";
import ClusterApi from "../../../api/clusterApi";
import { useParams } from "react-router-dom";
import { componentsWithManualCommands, reassignSteps } from "./constants";
import { ServiceContext } from "../../../store/ServiceContext";
import { ActionTypes } from "./store/types";
import Step1 from "./Step1";
import Step2 from "./Step2";
import Step3 from "./Step3";
import Step4 from "./Step4";
import Step5 from "./Step5";
import Step6 from "./Step6";

// Component to initialize the component name in the store
const ComponentNameInitializer: React.FC<{ componentName: string | undefined }> = ({ componentName }) => {
  const { dispatch } = useContext(ReassignContext);
  
  useEffect(() => {
    if (componentName) {
      dispatch({
        type: ActionTypes.SET_COMPONENT_NAME,
        payload: componentName,
      });
    }
  }, [componentName, dispatch]);
  
  return null;
};

function ValidateMove({
  serviceName: serviceNameProp,
}: {
  serviceName: string;
}) {
  const { componentName } = useParams();
  const { allServiceModels } = useContext(ServiceContext);
  const [canStartMove] = useState(true);
  const [validationErrors] = useState<string[]>([]);
  const [checkingForMovement] = useState(false);
  //@ts-ignore
  const isHAEnabled = allServiceModels?.["hdfs"]?.isNameNodeHaEnabled || false;
  const stepWizardUtilities = useStepWizard(getSteps(), 1);
  const [showModal, setShowModal] = useState(true);
  const getModalBodyContent = () => {
    if (checkingForMovement) {
      return (
        <div className="d-flex justify-content-center align-items-center flex-column p-4">
          <Spinner />
        </div>
      );
    }
    if (validationErrors.length) {
      return (
        <>
          <strong className="text-danger">Errors:</strong>
          {validationErrors.map((error) => (
            <div key={error} className="mt-2">
              {error}
            </div>
          ))}
        </>
      );
    }
    return (
      <ReassignProvider stepWizardUtilities={stepWizardUtilities}>
        <ComponentNameInitializer componentName={componentName} />
        <StepWizard wizardUtilities={stepWizardUtilities} />
      </ReassignProvider>
    );
  };
  function getSteps(){
     if (componentName) {
      const hasManualCommands = componentsWithManualCommands.includes(componentName);
      
      const inferredSteps:any = {
        1: {
          label: "Get Started",
          completed: false,
          Component: <Step1 />,
          canGoBack: false,
          isNextEnabled: false,
          name: reassignSteps.GET_STARTED,
        },
        2: {
          label: "Assign Masters",
          completed: false,
          Component: <Step2/>,
          canGoBack: true,
          isNextEnabled: false,
          name: reassignSteps.ASSIGN_MASTER,
        },
        3: {
          label: "Review",
          completed: false,
          Component:<Step3/>,
          canGoBack: true,
          isNextEnabled: false,
          name: reassignSteps.REVIEW,
          nextLabel: "DEPLOY",
        },
        4: {
          label: "Configure Component",
          completed: false,
          Component: <Step4 />,
          canGoBack: false,
          isNextEnabled: true,
          name: reassignSteps.CONFIGURE_COMPONENT,
          // Set nextLabel to "Complete" if this is the final step
          nextLabel: hasManualCommands ? "Next" : "Complete",
        },
      };
      
      if(hasManualCommands){
        inferredSteps[5]={
          label: "Manual Commands",
          completed: false,
          Component: <Step5 />,
          canGoBack: true,
          isNextEnabled: false,
          name: reassignSteps.MANUAL_COMMANDS,
          nextLabel: "Next",
        }
        inferredSteps[6]={
          label: "Start and Test Services",
          completed: false,
          Component: <Step6 />,
          canGoBack: true,
          isNextEnabled: false,
          name: reassignSteps.START_AND_TEST_SERVICES,
          nextLabel: "Complete",
        }
        
        // Only add Step 7 for NAMENODE with HA enabled
        // if (componentName === 'NAMENODE' && isHAEnabled) {
        //   inferredSteps[7]={
        //     label: "Finalize Move",
        //     completed: false,
        //     Component: <Step7 />,
        //     canGoBack: true,
        //     isNextEnabled: false,
        //     name: reassignSteps.FINALIZE_MOVE,
        //     nextLabel: "Complete",
        //   }
        // }
      }
      
      return inferredSteps
    }
  }
  return (
    <>
      {showModal ? (
        <Modal
          isOpen={showModal}
          onClose={async () => {
            // Clear persisted data on cancel/close
            await ClusterApi.postPersistData(
              JSON.stringify({
                USER_REDIRECTION_URL: "",
                REASSIGN_COMPONENT: JSON.stringify({}),
                CLUSTER_STATE: JSON.stringify({}),
              })
            );
            setShowModal(false);
            window.location.href = `/#/main/services/${serviceNameProp}/summary`;
          }}
          modalTitle={`Move ${componentName}`}
          modalBody={getModalBodyContent()}
          successCallback={() => {
            setShowModal(false);
            window.location.href = `/#/main/services/${serviceNameProp}/summary`;
          }}
          options={{
            shouldShowFooter: canStartMove ? false : true,
            modalSize: "modal-wizard",
            cancelableViaIcon: true,
            cancelableViaBtn: false,
          }}
        />
      ) : null}
    </>
  );
}

export default ValidateMove;
