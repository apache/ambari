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

import { Alert, Form } from "react-bootstrap";
import Modal from "../../components/Modal";
import { useState, useEffect, useContext } from "react";
import ConfigsApi from "../../api/configsApi";
import { AppContext } from "../../store/context";

type AddToConfigGroupModalProps = {
  isOpen: boolean;
  onClose: () => void;
  serviceName: string;
  configGroupNames: string[];
  onConfigGroupSelect: (configGroup: string) => void;
  setShowManageConfigGroupModal?: (show: boolean) => void;
};

export default function AddToConfigGroupModal({
  isOpen,
  onClose,
  serviceName,
  configGroupNames,
  onConfigGroupSelect,
  setShowManageConfigGroupModal,
}: AddToConfigGroupModalProps) {
  const { clusterName } = useContext(AppContext);
  const [isExistingConfigGroup, setIsExistingConfigGroup] = useState(true);
  const [selectedConfigGroup, setSelectedConfigGroup] = useState<string>("");
  const [newConfigGroup, setNewCofigGroup] = useState<string>("");
  const [newGroupCreatedModal, setNewGroupCreatedModal] =
    useState<boolean>(false);

  useEffect(() => {
    if (configGroupNames && configGroupNames.length > 0) {
      setSelectedConfigGroup(configGroupNames[0]);
      setIsExistingConfigGroup(true);
    } else {
      // If no existing config groups, automatically select "Create new" option
      setIsExistingConfigGroup(false);
      setSelectedConfigGroup("");
    }
  }, [configGroupNames]);

  const createNewConfigGroup = async () => {
    const payload = {
      ConfigGroup: {
        group_name: newConfigGroup,
        tag: serviceName,
        description: "New configuration group created ",
        service_name: serviceName,
        desired_configs: [],
        hosts: [],
      },
    };

    try {
      await ConfigsApi.createNewConfigGroup(clusterName, payload);
      onConfigGroupSelect(newConfigGroup);
      onClose();
      setNewGroupCreatedModal(true);
      onConfigGroupSelect(newConfigGroup);
    } catch (error) {
      console.error("Error creating config group:", error);
    }
  };

  const handleSuccess = () => {
    if (isExistingConfigGroup) {
      onConfigGroupSelect(selectedConfigGroup);
      onClose();
    } else {
      createNewConfigGroup();
    }
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        modalTitle={serviceName + " Configuration Group"}
        modalBody={
          <div>
            <Alert variant="info">
              Select or create a {serviceName} Configuration Group where the
              configuration value will be overridden.
            </Alert>
            <Form.Check
              type="radio"
              checked={isExistingConfigGroup}
              onChange={() => setIsExistingConfigGroup(true)}
              name="configGroupChoice"
              label={
                configGroupNames.length > 0
                  ? `Select an existing ${serviceName} Configuration Group`
                  : `Select an existing ${serviceName} Configuration Group (No existing groups found)`
              }
              disabled={configGroupNames.length === 0}
            />
            <select
              className="form-select mb-3"
              disabled={!isExistingConfigGroup || configGroupNames.length === 0}
              value={selectedConfigGroup}
              onChange={(e) => setSelectedConfigGroup(e.target.value)}
            >
              {configGroupNames.length > 0 ? (
                configGroupNames.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))
              ) : (
                <option value="">No configuration groups available</option>
              )}
            </select>
            <p className="text-secondary small mb-3">
              Overridden property will be changed for hosts belonging to the
              selected group
            </p>
            <Form.Check
              type="radio"
              checked={!isExistingConfigGroup}
              onChange={() => setIsExistingConfigGroup(false)}
              name="configGroupChoice"
              label={`Create a new ${serviceName} Configuration Group`}
            />
            <Form.Control
              type="text"
              value={newConfigGroup}
              onChange={(e) => setNewCofigGroup(e.target.value)}
              disabled={isExistingConfigGroup}
              className="mb-3"
            />
            <p className="text-secondary small mb-3">
              A new {serviceName} Configuration Group will be created with the
              given name. Initially there will be no hosts in the group, with
              only the selected property overridden.
            </p>
          </div>
        }
        successCallback={handleSuccess}
        options={{
          okButtonDisabled: !isExistingConfigGroup && !newConfigGroup.trim(),
        }}
      />
      <Modal
        isOpen={newGroupCreatedModal}
        onClose={() => {
          setShowManageConfigGroupModal?.(true);
          setNewGroupCreatedModal(false);
        }}
        successCallback={() => setNewGroupCreatedModal(false)}
        modalTitle="Save Configuration Group"
        modalBody={
          <div>
            <Alert className="my-3" variant="success">
              Configuration Group {newConfigGroup} has been successfully saved
            </Alert>
            <p>
              Click Manage Hosts to manage host membership to the configuration
              group
            </p>
          </div>
        }
        options={{ cancelButtonText: "MANAGE HOSTS" }}
      />
    </>
  );
}
