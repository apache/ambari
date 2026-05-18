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
import { cloneDeep, get, isEmpty, set } from "lodash";
import { useEffect, useRef, useState } from "react";
import { Card, Col, Container, Row } from "react-bootstrap";
import { getDependentConfigChanges, isValidUserName } from "../utils";
import Table from "../../../components/Table";
import Modal from "../../../components/Modal";
import TooltipInput from "../../../components/TooltipInput";
import Spinner from "../../../components/Spinner";

interface AccountsTabProps {
  configProperties:any,
  setConfigProperties:any,
  services: string[];
}

enum UserGroupsProperties {
  USER = "USER",
  GROUP = "GROUP",
  ADDITIONAL_USER_PROPERTY = "ADDITIONAL_USER_PROPERTY",
  ADDITIONAL_GROUP_PROPERTY = "ADDITIONAL_GROUP_PROPERTY",
}

export default function AccountsTab({
  configProperties,
  setConfigProperties,
  services,
}: AccountsTabProps) {
  const [accountConfigs, setAccountConfigs] = useState(configProperties);
  const [showWarning, setShowWarning] = useState<boolean>(false);
  const affectedProperties = useRef<any[]>([]);
  const allConfigs = useRef<any[]>([]);

  const serviceName = "MISC";
  const configType = "Users and Groups";

  useEffect(() => {
    setConfigProperties(accountConfigs);
  }, [accountConfigs]);
  
  
  useEffect(() => {
    allConfigs.current = getAllConfigs();
  }, [accountConfigs]);

  const getAllConfigs = () => {
    let allConfigs: any[] = [];
    Object.keys(accountConfigs).forEach((serviceName) => {
      Object.keys(accountConfigs[serviceName]).forEach((configType) => {
        Object.keys(accountConfigs[serviceName][configType]?.properties).forEach((property) => {
          const configuration = accountConfigs[serviceName][configType]?.properties[property];
          allConfigs.push(configuration);
        });
      });
    });

    return allConfigs;
  };

  const columnInWarningTable = [
    {
      header: "Service",
      accessorKey: "serviceName",
    },
    {
      header: "Property",
      accessorKey: "propertyName",
    },
    {
      header: "Current Value",
      accessorKey: "propertyValue",
    },
    {
      header: "Adjusted Value",
      accessorKey: "new_value",
    },
  ];

  const updateAllConfigs = (affectedProperties: any[]) => {
    const configPropertiesCopy = cloneDeep(accountConfigs);

    affectedProperties.forEach((affectedProperty) => {
 
      configPropertiesCopy[affectedProperty.serviceName][affectedProperty.type].properties[affectedProperty.propertyName].value = affectedProperty.new_value;
    });
    setAccountConfigs(configPropertiesCopy);
  };

  const applyConfigChanges = () => {
    setShowWarning(false);
    let affectedPropertiesCopy = cloneDeep(affectedProperties.current);
    affectedPropertiesCopy.forEach((affectedProperty) => {
      set(
        affectedProperty,
        "value",
        get(affectedProperty, "new_value")
      );
    });
    affectedProperties.current = affectedPropertiesCopy;
    updateAllConfigs(affectedProperties.current);
    //Get the configs for config tab from context then update it using affectedProperties and push it back
  };

  if (isEmpty(accountConfigs)) {
    return <Spinner/>;
  }

  

  return (
    <div>
      {showWarning ? (
        <Modal
          isOpen={showWarning}
          onClose={() => setShowWarning(false)}
          modalTitle="Warning: you must also change these Service properties"
          modalBody={
            <Table
              data={affectedProperties.current}
              columns={columnInWarningTable}
            />
          }
          successCallback={applyConfigChanges}
          options={{
            modalSize: "modal-lg",
            buttonSize: "sm",
            okButtonText: "APPLY",
            cancelButtonText: "CANCEL",
            cancelableViaIcon: false,
            cancelableViaBtn: true,
            okButtonVariant: "success",
            extraButtons: [
              {
                text: "IGNORE",
                onClick: () => setShowWarning(false),
                variant: "warning",
                order: 1,
              },
            ],
          }}
        />
      ) : null}
      <Card>
        <Card.Body>
          <div className="mb-4 text-muted ps-3">
            Please review these settings for Service Accounts
          </div>
          <div className="p-2">
          {accountConfigs[serviceName][configType] && Object.keys(accountConfigs[serviceName][configType].properties).map((propertyName:any) => {
                const config = accountConfigs[serviceName][configType]?.properties[propertyName];
                return ( 
                  config.propertyType.includes(UserGroupsProperties.ADDITIONAL_USER_PROPERTY) || config.propertyType.includes(UserGroupsProperties.ADDITIONAL_GROUP_PROPERTY) ? (
                    <div key={config.propertyName} className="ps-3">
                      <TooltipInput
                        tooltipProps={{
                          heading: config.propertyDisplayname + " - " + get(config,"propertyName"),
                          message: config.propertyDescription,
                          placement: "right",
                        }}
                        formControlProps={{
                          type: "checkbox",
                          label: config.propertyDisplayname,
                          checked: config.value === "true",
                          onChange: (e) => {
                            let tempConfig = config;
                            tempConfig.value = e.target.checked ? "true" : "false";
                            setAccountConfigs({
                              ...accountConfigs,
                              [serviceName]: {
                                ...accountConfigs[serviceName],
                                [configType]: {
                                  ...accountConfigs[serviceName][configType],
                                  properties: {
                                    ...accountConfigs[serviceName][configType].properties,
                                    [propertyName]: tempConfig,
                                  },
                                },
                              },
                            });
                          },
                          className: "custom-checkbox",
                        }}
                      />
                    </div>
                ):null);
                
              })}
          </div>
          <div className="p-2 w-50">
            <Container>
              <Row className="p-2">
                <Col className="fw-bolder">Users/Groups</Col>
                <Col className="fw-bolder">Username</Col>
              </Row>
              {accountConfigs[serviceName][configType] && Object.keys(accountConfigs[serviceName][configType].properties).map((propertyName:any) => {
                const config = accountConfigs[serviceName][configType].properties[propertyName];
                    if(propertyName==="dfs.permissions.superusergroup") return;
                    return ( 
                      config.propertyType.includes(UserGroupsProperties.USER) || config.propertyType.includes(UserGroupsProperties.GROUP) ? (
                      <Row key={propertyName} className="p-2 text-muted">
                        <Col className="pt-2">{config.propertyDisplayname?config.propertyDisplayname : config.propertyName}</Col>
                        <Col>
                          <TooltipInput
                            tooltipProps={{
                              heading: config.propertyDisplayname + " - " + get(config,"propertyName"),
                              message: config.propertyDescription,
                              placement: "right",
                            }}
                            formControlProps={{
                              type: "text",
                              value: config.value,
                              onChange: (e) => {
                                let tempConfig = config;
                                tempConfig.value = e.target.value;
                                setAccountConfigs({
                                  ...accountConfigs,
                                  [serviceName]: {
                                    ...accountConfigs[serviceName],
                                    [configType]: {
                                      ...accountConfigs[serviceName][configType],
                                      properties: {
                                        ...accountConfigs[serviceName][configType].properties,
                                        [propertyName]: tempConfig,
                                      },
                                    },
                                  },
                                });
                              },
                              onBlur: () => {
                                const allAffectedProperties =
                                  getDependentConfigChanges(
                                    config,
                                    services,
                                    allConfigs.current
                                  ) || [];
                                if (allAffectedProperties.length > 0) {
                                  affectedProperties.current =
                                    allAffectedProperties;
                                  setShowWarning(true);
                                }
                              },
                              className: isValidUserName(config.value)
                                ? "rounded-0"
                                : "rounded-0 border-danger",
                            }}
                          />
                        </Col>
                      </Row>
                    ):null)

                    
                  })}
            </Container>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
}
