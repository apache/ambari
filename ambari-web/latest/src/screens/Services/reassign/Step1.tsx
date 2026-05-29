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
import { useParams } from "react-router-dom";
import { AppContext } from "../../../store/context";
import { map } from "lodash";
import { componentsToStopAllServices, relatedServicesMap } from "./constants";
import { Alert } from "react-bootstrap";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import { ReassignContext } from "./store/context";
import { ActionTypes } from "./store/types";

function Step1() {
  const { services } = useContext(AppContext);
  const { componentName } = useParams<{ componentName: string }>();
  const [isNavigating, setIsNavigating] = useState(false);
  const {
      dispatch,
      flushStateToDb,
      stepWizardUtilities: { currentStep, handleNextImperitive },
    } = useContext(ReassignContext);

  function getRestartingServices() {
    let listOfServices = [];
    const installedServices = map(services, "ServiceInfo.service_name");
    if (componentsToStopAllServices.includes(componentName!)) {
      listOfServices = installedServices;
    } else {
      listOfServices = relatedServicesMap[componentName!] || [];
      if (!listOfServices.length) {
        listOfServices = installedServices.filter(function (service) {
          return service != "HDFS";
        });
      } else {
        const installedServicesToStop = listOfServices.filter(function (
          service: any
        ) {
          return installedServices.includes(service);
        });
        listOfServices = installedServicesToStop;
      }
    }
    return listOfServices.join(", ");
  }
  return (
    <>
      <h3 className="step-title">Get Started</h3>
      <p className="step-description light-text">
        This wizard will walk you through moving {componentName}.
        <br />
        The process to reassign {componentName} involves a combination of{" "}
        automated steps (that will be handled by the wizard) and
        manual steps (that you must perform in sequence as instructed by
        the wizard).
        <br />
        <br />
        <br />
        <Alert className="fs-12" variant="danger">
          Following services will be restarted as part of the wizard:{" "}
          <span className="fw-bolder fs-12">{getRestartingServices()}</span>.You
          should plan a cluster maintenance window and prepare for cluster
          downtime when moving {componentName}.
        </Alert>
      </p>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!isNavigating}
        onBack={() => {}}
        onCancel={async () => {
         flushStateToDb("cancel");
        }}
        onNext={async () => {
          // Prevent multiple rapid clicks
          if (isNavigating) return;
          
          setIsNavigating(true);
          try {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: {
                step: currentStep.name,
              //   data: {
              //     loadBalancerUrl,
              //   },
              },
            });
            flushStateToDb("next");
            handleNextImperitive();
          } catch (error) {
            setIsNavigating(false);
          }
        }}
      />
    </>
  );
}

export default Step1;
