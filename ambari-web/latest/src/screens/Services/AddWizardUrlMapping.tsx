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

import { useLocation } from "react-router-dom";
import Modal from "../../components/Modal";
import ClusterCreationWizard from "../ClusterWizard";
import {
  AddServiceContext,
  AddServiceProvider,
} from "./AddServiceWizard/wizardDataStore/context";
import addServiceWizardSteps from "./AddServiceWizard/addServiceWizardSteps";
import modalManager from "../../store/ModalManager";
import {
  AddHostContext,
  AddHostProvider,
} from "../Hosts/AddHostWizard/wizardDataStore/context";
import addHostWizardSteps from "../Hosts/AddHostWizard/addHostWizardSteps";
import { ComponentProps, useContext } from "react";
import { CANCEL_ADD_SERVICE_WIZARD_EVENT } from "../../Utils/addServicePersistence";

function AddWizardUrlMapping() {
  const location = useLocation();
  function mapUrlToComponent() {
    if (location.pathname.includes("service/add")) {
      return (
        <Modal
          isOpen={true}
          onClose={() => {
            modalManager.show(
              <Modal
                options={{}}
                modalTitle="Confirmation"
                isOpen={true}
                modalBody={
                  <div>
                    Are you sure you want to cancel the service creation? All
                    your progress will be lost.
                  </div>
                }
                onClose={() => {
                  modalManager.hide();
                }}
                successCallback={() => {
                  window.dispatchEvent(
                    new Event(CANCEL_ADD_SERVICE_WIZARD_EVENT)
                  );
                }}
              />
            );
          }}
          modalTitle="Add Service"
          options={{
            modalSize: "modal-wizard",
            shouldShowFooter: false,
          }}
          successCallback={() => {}}
          modalBody={
            <ClusterCreationWizard
              Context={AddServiceContext}
              Provider={AddServiceProvider}
              wizardSteps={addServiceWizardSteps as any}
              initialActiveStep={1}
            />
          }
        ></Modal>
      );
    } else if (location.pathname.includes("host/add")) {
      return <AddHostWizardModal />;
    }
  }
  return mapUrlToComponent();
}

function AddHostWizardModal() {
  const { flushStateToDb } = useContext(AddHostContext);

  return (
    <Modal
      isOpen={true}
      onClose={() => {
        modalManager.show(
          <Modal
            options={{}}
            modalTitle="Confirmation"
            isOpen={true}
            modalBody={
              <div>
                Are you sure you want to cancel the host addition? All your
                progress will be lost.
              </div>
            }
            onClose={() => {
              modalManager.hide();
            }}
            successCallback={async () => {
              await flushStateToDb("cancel");
              modalManager.hide();
            }}
          />
        );
      }}
      modalTitle="Add Hosts"
      options={{
        modalSize: "modal-wizard",
        shouldShowFooter: false,
      }}
      successCallback={() => {}}
      modalBody={
        <ClusterCreationWizard
          Context={AddHostContext}
          Provider={AddHostProvider}
          wizardSteps={
            addHostWizardSteps as unknown as ComponentProps<
              typeof ClusterCreationWizard
            >["wizardSteps"]
          }
          initialActiveStep={1}
        />
      }
    />
  );
}

export default AddWizardUrlMapping;
