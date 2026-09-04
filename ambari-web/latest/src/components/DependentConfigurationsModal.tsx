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
  /** editable recommendations (Ember.js recommendedChanges) */
  recommendations?: any[];
  /** non-editable required changes (Ember.js requiredChanges) */
  requiredChanges?: any[];
  /** legacy single-list prop, treated as editable recommendations */
  dependentConfigs?: any[];
  onSave: (updatedConfigs: any[]) => void;
};

const columnsBase = [
  {
    accessorKey: "propertyName",
    header: "Property",
    width: "20%",
  },
  {
    accessorKey: "serviceDisplayName",
    header: "Service",
    cell: ({ row }: { row: any }) =>
      row.original.serviceDisplayName || row.original.serviceName,
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
    cell: ({ row }: { row: any }) =>
      row.original.propertyFileName || row.original.fileName,
    width: "15%",
  },
  {
    accessorKey: "initialValue",
    header: "Original Value",
    cell: ({ getValue }: { getValue: () => any }) => {
      const value = getValue();
      return (
        <div
          style={{ maxWidth: "200px", wordBreak: "break-word", fontSize: "12px" }}
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
          style={{ maxWidth: "200px", wordBreak: "break-word", fontSize: "12px" }}
          title={value === null ? "Property removed" : String(value)}
        >
          {value === null ? "Property removed" : String(value)}
        </div>
      );
    },
    width: "20%",
  },
];

export default function DependentConfigurationsModal({
  isOpen,
  onClose,
  recommendations,
  requiredChanges,
  dependentConfigs,
  onSave,
}: DependentConfigurationsModalProps) {
  const initialRecommended = recommendations || dependentConfigs || [];

  const [isAllChecked, setIsAllChecked] = useState(true);
  const [editableConfigs, setEditableConfigs] = useState<any[]>(
    initialRecommended
  );

  useEffect(() => {
    const next = recommendations || dependentConfigs || [];
    setEditableConfigs(next);
    setIsAllChecked(next.every((config: any) => config.saveRecommended));
  }, [recommendations, dependentConfigs]);

  useEffect(() => {
    setIsAllChecked(
      editableConfigs.length > 0 &&
        editableConfigs.every((config: any) => config.saveRecommended)
    );
  }, [editableConfigs]);

  const applyValueRestoration = (config: any, isChecked: boolean) => {
    const updatedConfig = { ...config, saveRecommended: isChecked };
    updatedConfig.currentValue = isChecked
      ? config.recommendedValue
      : config.initialValue;
    return updatedConfig;
  };

  const handleHeaderCheckboxChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const isChecked = event.target.checked;
    setEditableConfigs(
      editableConfigs.map((config: any) =>
        applyValueRestoration(config, isChecked)
      )
    );
  };

  const handleRowCheckboxChange = (index: number, event: any) => {
    const isChecked = event.target.checked;
    const newConfigs = cloneDeep(editableConfigs);
    newConfigs[index] = applyValueRestoration(newConfigs[index], isChecked);
    setEditableConfigs(newConfigs);
  };

  const handleSave = () => {
    const merged = editableConfigs.map((c: any) => ({
      ...c,
      saveRecommendedDefault: c.saveRecommended,
    }));
    onSave([...merged, ...(requiredChanges || [])]);
    onClose();
  };

  const editableColumns = [
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
          onChange={(e) => handleRowCheckboxChange(row.index, e)}
        />
      ),
      width: "5%",
    },
    ...columnsBase,
  ];

  const getModalBody = () => {
    const hasEditable = editableConfigs.length > 0;
    const hasRequired = (requiredChanges || []).length > 0;

    if (!hasEditable && !hasRequired) {
      return <div>No dependent configuration changes found.</div>;
    }

    return (
      <div>
        {hasEditable && (
          <>
            <h4>Dependent Configurations</h4>
            <div className="alert alert-warning mb-3" style={{ fontSize: "14px" }}>
              Based on the services you are adding, Ambari is recommending the following dependent configuration changes.<br />
              Ambari will update all checked configuration changes to the <strong>Recommended Value</strong>.
              Uncheck any configuration to retain the <strong>Current Value</strong>.
            </div>
            <Table
              columns={editableColumns}
              data={editableConfigs}
              entityName="configuration"
              hover
              className="dependent-configs-table"
            />
          </>
        )}
        {hasRequired && (
          <>
            <h4 className={hasEditable ? "mt-4" : ""}>Required Changes</h4>
            <div className="alert alert-info mb-3" style={{ fontSize: "14px" }}>
              The following configuration changes are required and will always be applied.
            </div>
            <Table
              columns={columnsBase}
              data={requiredChanges || []}
              entityName="required configuration"
              hover
              className="required-configs-table"
            />
          </>
        )}
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
