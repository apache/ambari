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
import { DropdownButton, DropdownItem, Form } from "react-bootstrap";
import Table from "./Table";
import Modal from "./Modal";
import { translate, translateWithVariables } from "../Utils/Utility";

type RecommendationModalProps = {
  isOpen: boolean;
  onClose: () => void;
  componentDisplayName?: string;
  add: boolean;
  recommendedPropertiesToChange?: any;
  callback: (properties?: any) => void;
  fromService?: boolean;
  serviceName?: string;
  componentName?: string;
  validDropDownHosts?: string[];
  handleHostChange?: (host: string) => void;
  commonMessage?: string;
  selectedHostForDropDown?: string | null;
   selectRecommendedProperties?: (newProperties:any)=>void
};
export default function RecommendationModal({
  isOpen,
  onClose,
  componentDisplayName,
  add,
  recommendedPropertiesToChange,
  //@ts-ignore
  callback = (properties?: any) => {},
  fromService,
  serviceName,
  componentName,
  validDropDownHosts,
  handleHostChange,
  commonMessage = "",
  selectedHostForDropDown,
   selectRecommendedProperties
}: RecommendationModalProps) {
  const [isAllChecked, setIsAllChecked] = useState(false);
  const [propertiesToChange, setPropertiesToChange] = useState<any[]>(
    recommendedPropertiesToChange || []
  );
  const [selectedHost, setSelectedHost] = useState<string | null>(selectedHostForDropDown || null);
  useEffect(() => {
    setIsAllChecked(
      propertiesToChange.every((property: any) => property.saveRecommended)
    );
  }, [propertiesToChange]);

  const handleHostSelection = (host: any) => {
    setSelectedHost(host); // Update internal state immediately to show selection in UI
    if (handleHostChange) {
      handleHostChange(host); // Trigger callback directly
    }
  };

  const handleHeaderCheckboxChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const isChecked = event.target.checked;

    const updatedProperties = propertiesToChange.map((property: any) => ({
      ...property,
      saveRecommended: isChecked,
    }));
    
    setPropertiesToChange(updatedProperties);
    if(selectRecommendedProperties){
      selectRecommendedProperties(updatedProperties);
    }
  };

  const handleRowCheckboxChange = (index: number, event: any): any => {
    const isChecked = event.target.checked;

    const newProperties = cloneDeep(propertiesToChange);
    newProperties[index].saveRecommended = isChecked;
    setPropertiesToChange(newProperties);
    if(selectRecommendedProperties){
      selectRecommendedProperties(newProperties);
    }
  };

  function getModalBody() {
    // Use serviceName if available, otherwise use componentName
    const displayComponentName = serviceName || componentName || componentDisplayName;
    const addHostFromServiceMessage = `Select the host to add ${displayComponentName} component: `;

    const dropDownButtonToAddValidHosts = (
      <DropdownButton
        title={
          selectedHost
            ? selectedHost
            : validDropDownHosts && validDropDownHosts.length > 0
            ? validDropDownHosts[0]
            : ""
        }
      >
        {validDropDownHosts?.map((host) => {
          return (
            <DropdownItem
              key={host}
              onClick={() => {
                handleHostSelection(host);
              }}
            >
              {host}
            </DropdownItem>
          );
        })}
      </DropdownButton>
    );

    const message = !add
      ? commonMessage
        ? commonMessage
        : translateWithVariables("hosts.host.deleteComponent.popup.msg1", {
            "0": componentDisplayName || "",
          })
      : translateWithVariables("hosts.host.addComponent.msg", {
          "0": componentDisplayName || "",
        });

    if (propertiesToChange && propertiesToChange.length > 0) {
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
          header: "Property Name",
          width: "15%",
        },
        {
          accessorKey: "serviceDisplayName",
          header: "Service",
          width: "5%",
        },
        {
          accessorKey: "configGroup",
          header: "Config Group",
          cell: ({ getValue }: { getValue: () => any }) => getValue() || "",
          width: "10%",
        },
        {
          accessorKey: "propertyFileName",
          header: "File Name",
          width: "10%",
        },
        {
          accessorKey: "initialValue",
          header: "Original Value",
          width: "30%",
        },
        {
          accessorKey: "recommendedValue",
          header: "Recommended Value",
          width: "30%",
        },
      ];
      return (
        <div>
          {fromService ? (
            <>
              <div className="d-flex justfiy-content-between">
                <div className="fs-6">{addHostFromServiceMessage}</div>
                <div className="mx-2">{dropDownButtonToAddValidHosts}</div>
                <div></div>
              </div>
              <div className="m-2 fs-12">{message}</div>
            </>
          ) : (
            <>
              <div className="fs-12">{message}</div>
            </>
          )}

          <h3 className="mt-2">
            {translate("popup.dependent.configs.table.recommended")}
          </h3>
          <div className="mb-3 alert alert-warning fs-12">
            {translate("popup.dependent.configs.title.values")}
          </div>

          <Table
            columns={columns}
            data={propertiesToChange}
            entityName="property"
            hover
          />
        </div>
      );
    }
    return (
      <div>
        {fromService ? (
          <>
            <div className="d-flex justfiy-content-between">
              <div className="fs-6">{addHostFromServiceMessage}</div>
              <div className="mx-2">{dropDownButtonToAddValidHosts}</div>
              <div></div>
            </div>
            <div className="m-2">{message}</div>
          </>
        ) : (
          <div>{message}</div>
        )}
      </div>
    );
  }

  function getOkButtonText() {
    return add
      ? translate("hosts.host.addComponent.popup.confirm")
      : translate("hosts.host.deleteComponent.popup.confirm");
  }

  return (
    <Modal
      isOpen={isOpen}
      className={propertiesToChange.length > 0?"bg-operations-modal":""}
      onClose={onClose}
      modalTitle={translate("popup.confirmation.commonHeader")}
      modalBody={getModalBody()}
      successCallback={() => {
        onClose();
        if (recommendedPropertiesToChange) {
          callback(propertiesToChange);
        } else {
          callback();
        }
      }}
      options={{
        modalSize: "modal-lg",
        buttonSize: "sm",
        okButtonText: getOkButtonText(),
        cancelButtonText: translate("popup.confirmation.cancel"),
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
      }}
    />
  );
}
