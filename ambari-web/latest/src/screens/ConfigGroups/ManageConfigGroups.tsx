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

import {
  Alert,
  Button,
  Card,
  Dropdown,
  Form,
  Modal as ReactModal,
} from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useContext, useEffect, useRef, useState } from "react";
import { ClusterCreationContext } from "../ClusterWizard/clusterStore/context";
import { cloneDeep, get, isEmpty, set, sortBy } from "lodash";
import Spinner from "../../components/Spinner";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCog, faMinus, faPlus } from "@fortawesome/free-solid-svg-icons";
import CreateNewConfigGroup from "./CreateNewConfigGroup";
import Modal from "../../components/Modal";
import SelectConfigGroupHosts from "./SelectConfigGroupHosts";
import {
  ConfigGroupItemType,
  ConfigGroupType,
  DesiredConfigsItemType,
  DesiredConfigsType,
  HostDataType,
} from "./types";
import ConfigGroupApi from "../../api/configGroupApi";
import { ActionTypes } from "../ClusterWizard/clusterStore/types";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import classNames from "classnames";
import {
  buildConfigGroupUpdatePlan,
  moveHostsToConfigGroup,
  removeConfigGroupAndReturnHosts,
} from "../../Utils/configGroupSavePlan";

type ManageConfigGroupsProps = {
  isOpen: boolean;
  onClose: () => void;
  serviceName: string;
  successCallback: () => void;
  clusterName?: string;
  hostNames?: string;
};

// =============================================================
// Note: For using this component in installation wizard we have to pass the hostNames in props.
// Other than that if the cluster is installed then just pass the clusterName in props.
// The component automatically takes care of the corresponding APIs
// =============================================================

export default function ManageConfigGroups({
  isOpen,
  onClose,
  serviceName,
  successCallback,
  clusterName = "",
  hostNames = "",
}: ManageConfigGroupsProps) {
  const [hostData, setHostData] = useState<HostDataType>({ items: [] });
  const [configGroupData, setConfigGroupData] = useState<ConfigGroupType>({
    items: [],
  });
  const [desiredConfigsData, setDesiredConfigsData] =
    useState<DesiredConfigsType>({ items: [] });
  const [loading] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<string>("");
  const [selectedHosts, setSelectedHosts] = useState<string[]>([]);
  const [showCreateNewConfigGroupModal, setShowCreateNewConfigGroupModal] =
    useState(false);
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const [showPropertiesModal, setShowPropertiesModal] = useState(false);
  const [showSelectConfigHostsModal, setShowSelectConfigHostsModal] =
    useState(false);
  const [enableSave, setEnableSave] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  const { state, dispatch } = useContext(ClusterCreationContext);

  // Authorization hooks - implementing Ember.js config groups authorization patterns
  const { isAuthorized } = useAuthorizationPolicy();

  const canManageConfigGroups = isAuthorized("SERVICE.MANAGE_CONFIG_GROUPS");

  const isRenameConfigGroup = useRef(false);
  const isDuplicateConfigGroup = useRef(false);
  const config = useRef({
    name: "",
    description: "",
  });
  const previousConfigGroupData = useRef<ConfigGroupType>({ items: [] });

  useEffect(() => {
    if (isOpen) {
      if (clusterName) {
        getConfigGroupData();
        getHostsDataUsingClusterName();
      } else {
        getHostsDataUsingHostName();
      }
    }
  }, [isOpen, clusterName, hostNames]);

  useEffect(() => {
    let data = get(
      state,
      "clusterCreationSteps.step7.data.configGroupData",
      {}
    );
    if (!isEmpty(data)) {
      setConfigGroupData(data);
    }
  }, [state]);

  useEffect(() => {
    if (
      hostData?.items.length && 
      !getMatchingConfig(configGroupData, "group_name", "Default")
    ) {
      addDefaultConfigGroup();
    }
  }, [hostData, configGroupData]);

  useEffect(() => {
    if (!isEmpty(desiredConfigsData)) {
      let newConfigGroupData = { ...configGroupData };
      get(newConfigGroupData, "items", []).forEach((configGroup) => {
        let desiredConfigs = get(
          configGroup,
          "ConfigGroup.desired_configs",
          []
        );
        desiredConfigs?.forEach((config: DesiredConfigsItemType) => {
          let desiredConfig = get(desiredConfigsData, "items", []).find(
            (dc: DesiredConfigsItemType) =>
              dc?.type === config?.type && dc?.tag === config?.tag
          );
          set(config, "data", desiredConfig);
        });
      });
      setConfigGroupData(newConfigGroupData);
    }
  }, [desiredConfigsData]);

  useEffect(() => {
    setSelectedHosts([]);
  }, [selectedGroup]);

  useEffect(() => {
    if (clusterName) {
      // For existing clusters, compare with previous data to detect changes
      const configGroupsToBeDeleted = get(
        previousConfigGroupData.current,
        "items",
        []
      ).filter(
        (configGroup) =>
          !getMatchingConfig(
            configGroupData,
            "id",
            get(configGroup, "ConfigGroup.id")
          )
      );

      const configGroupsToBeAdded = get(configGroupData, "items", []).filter(
        (configGroup) =>
          get(configGroup, "ConfigGroup.group_name") !== "Default" &&
          !getMatchingConfig(
            previousConfigGroupData.current,
            "id",
            get(configGroup, "ConfigGroup.id")
          )
      );

      const configGroupsToBeUpdated = get(configGroupData, "items", []).filter(
        (configGroup) =>
          get(previousConfigGroupData.current, "items", []).some(
            (oldConfigGroup) =>
              get(oldConfigGroup, "ConfigGroup.id") ===
                get(configGroup, "ConfigGroup.id") &&
              !doesConfigsMatch(oldConfigGroup, configGroup, [
                "group_name",
                "description",
                "hosts",
              ])
          )
      );

      if (
        configGroupsToBeDeleted.length ||
        configGroupsToBeAdded.length ||
        configGroupsToBeUpdated.length
      ) {
        setEnableSave(true);
      } else {
        setEnableSave(false);
      }
    } else {
      const hasNonDefaultGroups = get(configGroupData, "items", []).some(
        (configGroup) => get(configGroup, "ConfigGroup.group_name") !== "Default"
      );
      
      setEnableSave(hasNonDefaultGroups);
    }
  }, [JSON.stringify(configGroupData), clusterName]);

  const getHostsDataUsingHostName = async () => {
    const response = await ConfigGroupApi.getHostsInfoUsingHostNames(
      hostNames,
      "Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name&minimal_response=true"
    );
    //TODO: from the context take out the host-component mapping and add it in response
    setHostData(response);
  };

  const getHostsDataUsingClusterName = async () => {
    const response = await ConfigGroupApi.getHostsInfoUsingClusterName(
      clusterName,
      "Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name,host_components&minimal_response=true"
    );
    setHostData(response);
  };

  const getConfigGroupData = async () => {
    const response = await ConfigGroupApi.getConfigGroupInfo(
      clusterName,
      serviceName,
      "*"
    );
    previousConfigGroupData.current = cloneDeep(response);
    setConfigGroupData(response);
    let configString = "";
    get(response, "items", []).forEach((configGroup: ConfigGroupItemType) => {
      get(configGroup, "ConfigGroup.desired_configs", []).forEach((config) => {
        configString += `(type=${get(config, "type")}&tag=${get(
          config,
          "tag"
        )})|`;
      });
    });
    if (configString) {
      configString = configString.slice(0, -1);
      getDesiredConfigsData(configString);
    }
  };

  const getDesiredConfigsData = async (configString: string) => {
    const response = await ConfigGroupApi.getDesiredConfigsInfo(
      clusterName,
      configString
    );
    setDesiredConfigsData(response);
  };

  const sortConfigs = (configGroupItemList: ConfigGroupItemType[]) => {
    return configGroupItemList.sort((a, b) => {
      const nameA = get(a, "ConfigGroup.group_name").toLowerCase();
      const nameB = get(b, "ConfigGroup.group_name").toLowerCase();

      if (nameA === "default") return -1;
      if (nameB === "default") return 1;

      if (nameA < nameB) return -1;
      if (nameA > nameB) return 1;
      return 0;
    });
  };

  const getMatchingConfig = (
    configGroups: ConfigGroupType,
    propertyName: string,
    propertyValue: any
  ) => {
    return configGroups?.items?.some(
      (item) => get(item, "ConfigGroup." + propertyName) === propertyValue
    );
  };

  const addConfigGroup = (configGroupItem: ConfigGroupItemType) => {
    if (
      !getMatchingConfig(
        configGroupData,
        "group_name",
        get(configGroupItem, "ConfigGroup.group_name")
      )
    ) {
      let newConfigGroupData = cloneDeep(configGroupData);
      let clonedConfigGroupItem = cloneDeep(configGroupItem);
      newConfigGroupData?.items?.push(clonedConfigGroupItem);
      set(newConfigGroupData, "items", sortConfigs(newConfigGroupData?.items));
      setConfigGroupData(newConfigGroupData);
    } else {
      console.error("Config group already exists");
    }
  };

  const addDefaultConfigGroup = () => {
    if (!getMatchingConfig(configGroupData, "group_name", "Default")) {
      let hostsInOtherConfigGroups: string[] = [];

      const configGroupDataClone = cloneDeep(configGroupData);
      
      get(configGroupDataClone, "items", []).forEach((configGroup) => {
        if (get(configGroup, "ConfigGroup.group_name") !== "Default") {
          hostsInOtherConfigGroups = hostsInOtherConfigGroups.concat(
            get(configGroup, "ConfigGroup.hosts", []).map((host) =>
              get(host, "host_name", "")
            )
          );
        }
      });

      const availableHostsForDefault = get(hostData, "items", [])
        .filter(
          (host) =>
            !hostsInOtherConfigGroups.includes(get(host, "Hosts.host_name"))
        )
        .map((host) => {
          return {
            host_name: get(host, "Hosts.host_name"),
          };
        });

      let defaultConfigGroup: ConfigGroupItemType = {
        ConfigGroup: {
          description: `Default cluster level ${serviceName} configuration`,
          group_name: "Default",
          hosts: availableHostsForDefault,
          tag: `${serviceName}`,
          desired_configs: [],
        },
      };

      addConfigGroup(defaultConfigGroup);
      setSelectedGroup("Default");
    }
  };

  const getItemFromConfigGroups = (
    configGroups: ConfigGroupType,
    configGroupName: string
  ) => {
    return get(configGroups, "items", []).filter(
      (item) => get(item, "ConfigGroup.group_name") === configGroupName
    );
  };

  const updateConfigGroup = (
    configGroupName: string,
    configGroupItem: ConfigGroupItemType
  ) => {
    let newConfigGroupData = { ...configGroupData };
    set(
      newConfigGroupData,
      "items",
      get(newConfigGroupData, "items", []).map((item) => {
        if (get(item, "ConfigGroup.group_name") === configGroupName) {
          return {
            ConfigGroup: {
              ...get(item, "ConfigGroup"),
              ...get(configGroupItem, "ConfigGroup"),
            },
          };
        }
        return item;
      })
    );
    setConfigGroupData(newConfigGroupData);
  };

  const getPropertyFromSelectedConfig = (propertyName: string) => {
    return get(
      getItemFromConfigGroups(configGroupData, selectedGroup),
      "[0].ConfigGroup." + propertyName
    );
  };

  const unsetConfigGroupModal = () => {
    setShowCreateNewConfigGroupModal(false);
    isRenameConfigGroup.current = false;
    isDuplicateConfigGroup.current = false;
    config.current = {
      name: "",
      description: "",
    };
  };

  const getHostsAvailableForSelectedGroup = () => {
    const selectedGroupHosts = new Set(
      (getPropertyFromSelectedConfig("hosts") || []).map(
        (host: { host_name?: string }) => host.host_name
      )
    );
    return get(hostData, "items", []).filter(
      (host) => !selectedGroupHosts.has(get(host, "Hosts.host_name"))
    );
  };

  const printDesiredConfigs = () => {
    let desiredConfigs = getPropertyFromSelectedConfig("desired_configs");
    let desiredConfigsString = "";
    if (!isEmpty(desiredConfigs)) {
      desiredConfigs.forEach((config: DesiredConfigsItemType) => {
        Object.keys(get(config, "data.properties", {})).forEach((key) => {
          desiredConfigsString += `${key} : ${get(
            config,
            "data.properties." + key,
            ""
          )}\n`;
        });
      });
    }
    return desiredConfigsString;
  };

  const getDesiredConfigsSize = () => {
    let desiredConfigs = getPropertyFromSelectedConfig("desired_configs");
    let size = 0;
    if (!isEmpty(desiredConfigs)) {
      desiredConfigs.forEach((config: DesiredConfigsItemType) => {
        size += Object.keys(get(config, "data.properties", {})).length;
      });
    }
    return size;
  };

  const doesConfigsMatch = (
    configGroup1: ConfigGroupItemType,
    configGroup2: ConfigGroupItemType,
    propertiesToMatch: string[]
  ) => {
    for (const property of propertiesToMatch) {
      let value1 = get(configGroup1, "ConfigGroup." + property);
      let value2 = get(configGroup2, "ConfigGroup." + property);

      if (typeof value1 === "string") {
        if (value1 !== value2) {
          return false;
        }
      } else {
        if (Array.isArray(value1)) {
          value1 = sortBy(value1);
          value2 = sortBy(value2);
        }
        if (JSON.stringify(value1) !== JSON.stringify(value2)) {
          return false;
        }
      }
    }
    return true;
  };

  const getDataForApiFormat = (configGroup: ConfigGroupItemType) => {
    return {
      ConfigGroup: {
        description: get(configGroup, "ConfigGroup.description", ""),
        desired_configs: get(
          configGroup,
          "ConfigGroup.desired_configs",
          []
        ).map((config: DesiredConfigsItemType) => ({
          type: get(config, "type"),
          tag: get(config, "tag"),
        })),
        group_name: get(configGroup, "ConfigGroup.group_name", ""),
        hosts: get(configGroup, "ConfigGroup.hosts", []).map((host) => {
          return {
            host_name: get(host, "host_name"),
          };
        }),
        service_name: serviceName,
        tag: serviceName,
      },
    };
  };

  const handleHostNameClick = (event: any, clickedHostName: string) => {
    if (event.metaKey) {
      if (selectedHosts.includes(clickedHostName)) {
        setSelectedHosts(selectedHosts.filter((h) => h !== clickedHostName));
      } else {
        setSelectedHosts([...selectedHosts, clickedHostName]);
      }
    } else {
      setSelectedHosts([clickedHostName]);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setSaveError("");
    try {
      if (clusterName) {
      const configGroupsToBeDeleted = get(
        previousConfigGroupData.current,
        "items",
        []
      ).filter(
        (configGroup) =>
          !getMatchingConfig(
            configGroupData,
            "id",
            get(configGroup, "ConfigGroup.id")
          )
      );

      const configGroupsToBeAdded = get(configGroupData, "items", []).filter(
        (configGroup) =>
          get(configGroup, "ConfigGroup.group_name") !== "Default" &&
          !getMatchingConfig(
            previousConfigGroupData.current,
            "id",
            get(configGroup, "ConfigGroup.id")
          )
      );

      const configGroupsToBeUpdated = get(configGroupData, "items", []).filter(
        (configGroup) =>
          get(previousConfigGroupData.current, "items", []).some(
            (oldConfigGroup) =>
              get(oldConfigGroup, "ConfigGroup.id") ===
                get(configGroup, "ConfigGroup.id") &&
              !doesConfigsMatch(oldConfigGroup, configGroup, [
                "group_name",
                "description",
                "hosts",
              ])
          )
      );

      const { toClear, toSet } = buildConfigGroupUpdatePlan(
        get(previousConfigGroupData.current, "items", []),
        configGroupsToBeUpdated
      );

      // Ambari enforces one non-default group per service. Clear source hosts
      // before assigning them to a target group or deleting their old group.
      await Promise.all(
        [...configGroupsToBeDeleted, ...toClear].map((configGroup) => {
          const data = getDataForApiFormat(configGroup);
          return ConfigGroupApi.updateConfigGroup(
            clusterName,
            get(configGroup, "ConfigGroup.id", "").toString(),
            {
              ...data,
              ConfigGroup: { ...data.ConfigGroup, hosts: [] },
            }
          );
        })
      );
      await Promise.all(
        configGroupsToBeDeleted.map((configGroup) =>
          ConfigGroupApi.removeConfigGroup(
            clusterName,
            get(configGroup, "ConfigGroup.id", "").toString()
          )
        )
      );
      await Promise.all(
        toSet.map((configGroup) =>
          ConfigGroupApi.updateConfigGroup(
            clusterName,
            get(configGroup, "ConfigGroup.id", "").toString(),
            getDataForApiFormat(configGroup)
          )
        )
      );
      await Promise.all(
        configGroupsToBeAdded.map((configGroup) =>
          ConfigGroupApi.addConfigGroup(clusterName, [
            getDataForApiFormat(configGroup),
          ])
        )
      );
      } else {
      const prevState = get(
        state,
        "clusterCreationSteps.step7.data.configGroupData",
        {}
      );
      dispatch({
        type: ActionTypes.STORE_INFORMATION,
        payload: {
          step: "step7",
          data: { ...prevState, configGroupData: configGroupData },
        },
      });
      }

      successCallback();
      onClose();
    } catch (error) {
      setSaveError(
        String(
          get(
            error,
            "response.data.message",
            get(error, "message", "Unable to save configuration groups.")
          )
        )
      );
    } finally {
      setSaving(false);
    }
  };

  const isRemoveHostDisabled = () => {
    return selectedGroup === "Default" || !selectedHosts.length;
  };

  const isAddHostDisabled = () => {
    return (
      selectedGroup === "Default" ||
      !getHostsAvailableForSelectedGroup().length
    );
  };

  if (loading) {
    return <Spinner />;
  }

  return (
    <div>
      {showCreateNewConfigGroupModal ? (
        <CreateNewConfigGroup
          isOpen={showCreateNewConfigGroupModal}
          onClose={unsetConfigGroupModal}
          successCallback={(formData) => {
            let formConfig = {
              ConfigGroup: {
                description: get(formData, "description", ""),
                group_name: get(formData, "name", ""),
              },
            };
            if (isRenameConfigGroup.current) {
              updateConfigGroup(selectedGroup, formConfig);
              setSelectedGroup(get(formData, "name", ""));
            } else {
              set(formConfig, "ConfigGroup.hosts", []);
              set(formConfig, "ConfigGroup.tag", serviceName);
              set(
                formConfig,
                "ConfigGroup.desired_configs",
                isDuplicateConfigGroup.current
                  ? cloneDeep(getPropertyFromSelectedConfig("desired_configs"))
                  : []
              );
              addConfigGroup(formConfig);
            }
          }}
          existingConfigGroups={get(configGroupData, "items", []).map(
            (configGroup) => get(configGroup, "ConfigGroup.group_name")
          )}
          config={config.current}
          isRename={isRenameConfigGroup.current}
        />
      ) : null}
      {showConfirmationModal ? (
        <Modal
          isOpen={showConfirmationModal}
          onClose={() => setShowConfirmationModal(false)}
          modalTitle="Confirmation"
          modalBody="Are you sure?"
          successCallback={() => {
            setConfigGroupData((current) => ({
              ...current,
              items: removeConfigGroupAndReturnHosts(
                current.items,
                selectedGroup
              ),
            }));
            setSelectedGroup("Default");
            setShowConfirmationModal(false);
          }}
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
          }}
        />
      ) : null}
      {showPropertiesModal ? (
        <Modal
          isOpen={showPropertiesModal}
          onClose={() => setShowPropertiesModal(false)}
          modalTitle="Properties"
          modalBody={printDesiredConfigs()}
          successCallback={() => setShowPropertiesModal(false)}
          options={{
            cancelableViaBtn: false,
            cancelableViaIcon: true,
          }}
        />
      ) : null}
      {showSelectConfigHostsModal ? (
        <SelectConfigGroupHosts
          isOpen={showSelectConfigHostsModal}
          onClose={() => setShowSelectConfigHostsModal(false)}
          successCallback={(hostsToBeAdded: any) => {
            setShowSelectConfigHostsModal(false);
            setConfigGroupData((current) => ({
              ...current,
              items: moveHostsToConfigGroup(
                current.items,
                selectedGroup,
                hostsToBeAdded
              ),
            }));
          }}
          configGroupName={selectedGroup}
          hostsList={getHostsAvailableForSelectedGroup()}
        />
      ) : null}
      <ReactModal
        show={isOpen}
        onHide={onClose}
        size="lg"
        className="custom-modal-container modal-lg make-scrollable custom-scrollbar"
      >
        <ReactModal.Header closeButton className="text-muted">
          <h2>Manage {serviceName} Configuration Groups</h2>
        </ReactModal.Header>
        <Form onSubmit={(e) => {
          e.preventDefault();
          handleSave();
        }}>
          <ReactModal.Body>
            {saveError ? <Alert variant="danger">{saveError}</Alert> : null}
            <Alert variant="info" className="text-muted fs-12 mb-5">
              You can apply different sets of {serviceName} configurations to
              groups of hosts by managing {serviceName} Configuration Groups and
              their host membership. Hosts belonging to a {serviceName}{" "}
              Configuration Group have the same set of configurations for{" "}
              {serviceName}. Each host belongs to one {serviceName}{" "}
              Configuration Group.
            </Alert>
            <div className="d-flex w-100 mb-4">
              <div className="w-30 me-4">
                <Card className="p-2 rounded-1 w-100 h-250px mb-3 scrollable custom-scrollbar">
                  {get(configGroupData, "items", []).map((configGroup) => {
                    return (
                      <div
                        key={get(configGroup, "ConfigGroup.group_name")}
                        onClick={() =>
                          setSelectedGroup(
                            get(configGroup, "ConfigGroup.group_name")
                          )
                        }
                        className={`p-1 option ${
                          get(configGroup, "ConfigGroup.group_name") ===
                          selectedGroup
                            ? "bg-info"
                            : ""
                        }`}
                      >
                        {get(configGroup, "ConfigGroup.group_name", "") +
                          " (" +
                          get(configGroup, "ConfigGroup.hosts", []).length +
                          ")"}
                      </div>
                    );
                  })}
                </Card>
                <div className="d-flex justify-content-end">
                  {canManageConfigGroups && (
                    <DefaultButton
                      className="me-2"
                      onClick={() => setShowCreateNewConfigGroupModal(true)}
                    >
                      <FontAwesomeIcon icon={faPlus} />
                    </DefaultButton>
                  )}
                  {canManageConfigGroups && (
                    <DefaultButton
                      className={
                        selectedGroup === "Default"
                          ? "disabled-btn me-2"
                          : "me-2"
                      }
                      onClick={() => setShowConfirmationModal(true)}
                      disabled={selectedGroup === "Default"}
                    >
                      <FontAwesomeIcon icon={faMinus} />
                    </DefaultButton>
                  )}
                  {canManageConfigGroups && (
                    <Dropdown>
                      <Dropdown.Toggle
                        variant="transparent"
                        className="btn-default"
                      >
                        <FontAwesomeIcon icon={faCog} className="me-2" />
                      </Dropdown.Toggle>
                      <Dropdown.Menu className="rounded-0">
                        <Dropdown.Item
                          onClick={() => {
                            if (selectedGroup !== "Default") {
                              isRenameConfigGroup.current = true;
                              config.current = {
                                name: selectedGroup,
                                description:
                                  getPropertyFromSelectedConfig("description"),
                              };
                              setShowCreateNewConfigGroupModal(true);
                            }
                          }}
                          className={
                            selectedGroup === "Default" ? "disabled-btn" : ""
                          }
                          disabled={selectedGroup === "Default"}
                        >
                          Rename
                        </Dropdown.Item>
                        <Dropdown.Item
                          onClick={() => {
                            isRenameConfigGroup.current = false;
                            isDuplicateConfigGroup.current = true;
                            config.current = {
                              name: selectedGroup + " Copy",
                              description: `${getPropertyFromSelectedConfig(
                                "description"
                              )} (Copy)`,
                            };
                            setShowCreateNewConfigGroupModal(true);
                          }}
                        >
                          Duplicate
                        </Dropdown.Item>
                      </Dropdown.Menu>
                    </Dropdown>
                  )}
                </div>
              </div>
              <div className="w-70">
                <Card className="p-2 w-75 rounded-1 w-100 h-250px mb-3 scrollable custom-scrollbar">
                  {getPropertyFromSelectedConfig("hosts")?.map(
                    (host: { [key: string]: string }) => {
                      return (
                        <div
                          onClick={(e) =>
                            handleHostNameClick(e, get(host, "host_name"))
                          }
                          className={`${
                            selectedHosts.includes(get(host, "host_name"))
                              ? "bg-info"
                              : ""
                          } p-1`}
                          key={get(host, "host_name")}
                        >
                          {get(host, "host_name")}
                        </div>
                      );
                    }
                  )}
                </Card>
                <div className="d-flex justify-content-end">
                  {canManageConfigGroups && (
                    <DefaultButton
                      className={
                        isAddHostDisabled() ? "disabled-btn me-2" : "me-2"
                      }
                      disabled={isAddHostDisabled()}
                      onClick={() => setShowSelectConfigHostsModal(true)}
                    >
                      <FontAwesomeIcon icon={faPlus} />
                    </DefaultButton>
                  )}
                  {canManageConfigGroups && (
                    <DefaultButton
                      className={isRemoveHostDisabled() ? "disabled-btn" : ""}
                      disabled={isRemoveHostDisabled()}
                      onClick={() => {
                        setConfigGroupData((current) => ({
                          ...current,
                          items: moveHostsToConfigGroup(
                            current.items,
                            "Default",
                            selectedHosts
                          ),
                        }));
                        setSelectedHosts([]);
                      }}
                    >
                      <FontAwesomeIcon icon={faMinus} />
                    </DefaultButton>
                  )}
                </div>
                <div className="d-flex">
                  <div className="ps-5 mb-2 d-flex-column">
                    <div className="d-flex justify-content-end mb-2 fs-12">
                      Overrides
                    </div>
                    <div className="d-flex justify-content-end fs-12">
                      Description
                    </div>
                  </div>
                  <div className="ps-5">
                    <div
                      className="custom-link mb-2 fs-12"
                      onClick={() => setShowPropertiesModal(true)}
                    >
                      {getDesiredConfigsSize()} properties
                    </div>
                    <div className="fs-12">
                      {getPropertyFromSelectedConfig("description")}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </ReactModal.Body>
          <ReactModal.Footer>
            <DefaultButton onClick={onClose}>CANCEL</DefaultButton>
            {canManageConfigGroups && (
              <Button
                type="submit"
                className={classNames("custom-btn text-white", {
                  "disabled-btn": !enableSave,
                })}
                disabled={!enableSave || saving}
              >
                {saving ? "SAVING" : "SAVE"}
              </Button>
            )}
            {/* Show unauthorized message if user lacks permissions */}
            {!canManageConfigGroups && (
              <div className="text-muted small">
                You do not have permission to modify configuration groups.
                Required permission: SERVICE.MANAGE_CONFIG_GROUPS
              </div>
            )}
          </ReactModal.Footer>
        </Form>
      </ReactModal>
    </div>
  );
}
