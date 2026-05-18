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

import { cloneDeep } from "lodash";
import { useEffect, useState } from "react";
import { Form } from "react-bootstrap";
import Table from "./Table";
import Modal from "./Modal";

type DependentConfigurationsModalProps = {
  isOpen: boolean;
  onClose: () => void;
  dependentConfigs: any[];
  onSave: (updatedConfigs: any[]) => void;
};

export default function DependentConfigurationsModal({
  isOpen,
  onClose,
  dependentConfigs,
  onSave,
}: DependentConfigurationsModalProps) {
  const [isAllChecked, setIsAllChecked] = useState(true);
  const [configsToChange, setConfigsToChange] = useState<any[]>(
    dependentConfigs || []
  );

  useEffect(() => {
    setConfigsToChange(dependentConfigs || []);
    setIsAllChecked(
      (dependentConfigs || []).every((config: any) => config.saveRecommended)
    );
  }, [dependentConfigs]);

  useEffect(() => {
    setIsAllChecked(
      configsToChange.every((config: any) => config.saveRecommended)
    );
  }, [configsToChange]);

  const applyValueRestoration = (config: any, isChecked: boolean) => {
    const updatedConfig = { ...config, saveRecommended: isChecked };
    
    if (isChecked) {
      updatedConfig.currentValue = config.recommendedValue;
    } else {
      updatedConfig.currentValue = config.initialValue;
    }
    
    return updatedConfig;
  };

  const handleHeaderCheckboxChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const isChecked = event.target.checked;
    
    // Apply value restoration logic to all configs
    const updatedConfigs = configsToChange.map((config: any) => 
      applyValueRestoration(config, isChecked)
    );
    
    setConfigsToChange(updatedConfigs);
  };

  const handleRowCheckboxChange = (index: number, event: any) => {
    const isChecked = event.target.checked;
    const newConfigs = cloneDeep(configsToChange);
    
    // Apply value restoration logic for individual row
    newConfigs[index] = applyValueRestoration(newConfigs[index], isChecked);
    
    setConfigsToChange(newConfigs);
  };

  const handleSave = () => {
    onSave(configsToChange);
    onClose();
  };

  const getModalBody = () => {
    if (!configsToChange || configsToChange.length === 0) {
      return <div>No dependent configuration changes found.</div>;
    }

    const columns = [
      {
        accessorKey: "saveRecommended",
        header: () => (
          <Form.Check
            id="select-all-checkbox"
            type="checkbox"
            checked={isAllChecked}
            onChange={handleHeaderCheckboxChange}
          />
        ),
        cell: ({ row }: { row: any }) => (
          <Form.Check
            type="checkbox"
            checked={row.original.saveRecommended}
            onChange={(e) => {
              handleRowCheckboxChange(row.index, e);
            }}
          />
        ),
        width: "5%",
      },
      {
        accessorKey: "propertyName",
        header: "Property",
        width: "20%",
      },
      {
        accessorKey: "serviceDisplayName",
        header: "Service",
        width: "10%",
      },
      {
        accessorKey: "configGroup",
        header: "Config Group",
        cell: ({ getValue }: { getValue: () => any }) => getValue() || "Default",
        width: "10%",
      },
      {
        accessorKey: "propertyFileName",
        header: "File Name",
        width: "15%",
      },
      {
        accessorKey: "initialValue",
        header: "Original Value",
        cell: ({ getValue }: { getValue: () => any }) => {
          const value = getValue();
          return (
            <div 
              style={{ 
                maxWidth: '200px', 
                wordBreak: 'break-word',
                fontSize: '12px'
              }}
              title={value === null ? "Property undefined" : String(value)}
            >
              {value === null ? "Property undefined" : String(value)}
            </div>
          );
        },
        width: "20%",
      },
      {
        accessorKey: "recommendedValue",
        header: "Recommended Value",
        cell: ({ getValue }: { getValue: () => any }) => {
          const value = getValue();
          return (
            <div 
              style={{ 
                maxWidth: '200px', 
                wordBreak: 'break-word',
                fontSize: '12px'
              }}
              title={value === null ? "Property removed" : String(value)}
            >
              {value === null ? "Property removed" : String(value)}
            </div>
          );
        },
        width: "20%",
      },
    ];

    return (
      <div>
        <h4>Dependent Configurations</h4>
        <div className="alert alert-warning mb-3" style={{ fontSize: '14px' }}>
          Based on the services you are adding, Ambari is recommending the following dependent configuration changes.<br/>
          Ambari will update all checked configuration changes to the <strong>Recommended Value</strong>. 
          Uncheck any configuration to retain the <strong>Current Value</strong>.
        </div>

        <Table
          columns={columns}
          data={configsToChange}
          entityName="configuration"
          hover
          className="dependent-configs-table"
        />
      </div>
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      className="bg-operations-modal"
      onClose={onClose}
      modalTitle="Dependent Configurations"
      modalBody={getModalBody()}
      successCallback={handleSave}
      options={{
        modalSize: "modal-xl",
        buttonSize: "sm",
        okButtonText: "OK",
        cancelButtonText: "Cancel",
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
      }}
    />
  );
}