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
import { Button, Form, Modal, Nav, Tab } from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useEffect, useState } from "react";
import {
  ClusterInfoType,
  ClusterInfoItemType,
  ClusterType,
  ConfigType,
  MappingType,
  ParametersType,
  ViewDetailsItemType,
  ViewDetailsType,
  RemoteClusterInfoItemType,
  RemoteClusterInfoType,
  ValidationErrorType,
} from "./types";
import { clusterType } from "./viewConstants";
import { cloneDeep, get, set } from "lodash";
import toast from "react-hot-toast";
import WarningModal from "../Users/WarningModal";
import ClusterApi from "../../api/clusterApi.ts";
import ViewApi from "../../api/viewApi.ts";
import Spinner from "../../components/Spinner";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleXmark } from "@fortawesome/free-solid-svg-icons";
import { useHistory } from "react-router";

type CreateInstanceProps = {
  isOpen: boolean;
  onClose: (showAddUserModal: boolean) => void;
  viewDetails: ViewDetailsType;
  viewInstanceInfoToBeCloned?: any;
  successCallback: () => void;
};

export default function CreateInstance({
  isOpen,
  onClose,
  viewDetails,
  viewInstanceInfoToBeCloned = {},
  successCallback,
}: CreateInstanceProps) {
  const [showCancelWarning, setShowCancelWarning] = useState(false);
  const [clusterInfo, setClusterInfo] = useState<ClusterInfoType>(
    {} as ClusterInfoType
  );
  const [remoteClusterInfo, setRemoteClusterInfo] =
    useState<RemoteClusterInfoType>({} as RemoteClusterInfoType);
  const [loading, setLoading] = useState(false);
  const [viewOptions, setViewOptions] = useState({});
  const [view, setView] = useState<string>("");
  const [version, setVersion] = useState<string>("");
  const [instanceName, setInstanceName] = useState<string>("");
  const [displayName, setDisplayName] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [isVisible, setIsVisible] = useState<boolean>(true);
  const [clusterHandle, setClusterHandle] = useState<string>("");
  const [nonClusterConfigs, setNonClusterConfigs] = useState<MappingType[]>([]);
  const [clusterConfigs, setClusterConfigs] = useState<MappingType[]>([]);
  const [configs, setConfigs] = useState<ConfigType>({});
  const [validationError, setValidationError] = useState<ValidationErrorType>({
    instanceName: "",
    displayName: "",
    description: "",
  });
  const [isFormSubmitted, setIsFormSubmitted] = useState(false);
  const [existingInstanceList, setExistingInstanceList] = useState<string[]>([]);
  const history = useHistory();

  const [activeKey, setActiveKey] = useState<ClusterType>(
    Object.keys(clusterType)[0] as ClusterType
  );

  const isThisCloneInstance =
    Object.keys(viewInstanceInfoToBeCloned).length > 0;

  useEffect(() => {
    async function getClusterInfo() {
      setLoading(true);
      const response = await ClusterApi.clusterInfo("Clusters/cluster_id");
      setClusterInfo(response);
      setLoading(false);
      setClusterHandle(get(response, "items[0].Clusters.cluster_id", "").toString());
    }
    async function getRemoteClusterInfo() {
      setLoading(true);
      const response = await ClusterApi.remoteClusterInfo(
        "ClusterInfo/services,ClusterInfo/cluster_id"
      );
      setRemoteClusterInfo(response);
      setLoading(false);
    }
    if (isOpen) {
      getClusterInfo();
      getRemoteClusterInfo();
    }
    if (isOpen && isThisCloneInstance) {
      setClusterHandle(
        get(viewInstanceInfoToBeCloned, "cluster_handle") === null
          ? ""
          : get(viewInstanceInfoToBeCloned, "cluster_handle")
      );
      setView(get(viewInstanceInfoToBeCloned, "view_name"));
      setVersion(get(viewInstanceInfoToBeCloned, "version"));
      setInstanceName(
        get(viewInstanceInfoToBeCloned, "instance_name") + "_Copy"
      );
      setDisplayName(get(viewInstanceInfoToBeCloned, "label") + "_Copy");
      setDescription(get(viewInstanceInfoToBeCloned, "description"));
      setIsVisible(get(viewInstanceInfoToBeCloned, "visible"));
      setConfigs(get(viewInstanceInfoToBeCloned, "properties"));
      setActiveKey(
        Object.entries(clusterType).find(
          ([, value]) =>
            value === get(viewInstanceInfoToBeCloned, "cluster_type")
        )?.[0] as ClusterType
      );
    }
    if (!isOpen) {
      resetValues();
    }
  }, [isOpen]);

  useEffect(() => {
    setViewOptions(
      get(viewDetails, "items", []).reduce((acc: any, item: any) => {
        return {
          ...acc,
          [get(item, "ViewInfo.view_name")]: get(item, "versions").map(
            (version: any) => get(version, "ViewVersionInfo.version")
          ),
        };
      }, {})
    );

    const instanceNamesList: string[] = (viewDetails?.items ?? []).flatMap(
      (item: { versions: any }) =>
        (item?.versions ?? []).flatMap((version: { instances: any }) =>
          (version?.instances ?? [])
            .map((instance: any) => {
              return get(instance, "ViewInstanceInfo.instance_name");
            })
            .filter(Boolean)
        )
    );

    setExistingInstanceList(instanceNamesList);

  }, [viewDetails]);

  useEffect(() => {
    if (!isThisCloneInstance) {
      setView(get(Object.keys(viewOptions), "[0]", ""));
    }
  }, [viewOptions]);

  useEffect(() => {
    if (!isThisCloneInstance) {
      setVersion(get(viewOptions, `[${view}][0]`, ""));
      setActiveKey(Object.keys(clusterType)[0] as ClusterType);
    }
  }, [view]);

  useEffect(() => {
    let defaultConfigs = {};
    setNonClusterConfigs(getConfigs(true, defaultConfigs));
    setClusterConfigs(getConfigs(false, defaultConfigs));
    setConfigs(defaultConfigs);
  }, [version, view]);

  useEffect(() => {
    const updatedConfigs = clusterConfigs.map((config) => ({
      ...config,
      error: "",
    }));
    setClusterConfigs(updatedConfigs);
    if (activeKey === "local") {
      setClusterHandle(
        get(clusterInfo, "items[0].Clusters.cluster_id", "").toString()
      );
    } else if (activeKey === "remote") {
      setClusterHandle(
        get(remoteClusterInfo, "items[0].ClusterInfo.cluster_id", "").toString()
      );
    } else {
      setClusterHandle("");
    }
  }, [activeKey]);

  const validateInstanceName = (instanceNameValue: string) => {
    const regex = /^\s*\w*\s*$/;
    if (instanceNameValue === "") {
      setValidationError({
        ...validationError,
        instanceName: "Field required!",
      });
    } else if (!regex.test(instanceNameValue)) {
      setValidationError({
        ...validationError,
        instanceName: "Must not contain special characters!",
      });
    } else if (existingInstanceList.includes(instanceNameValue)) {
      setValidationError({
        ...validationError,
        instanceName: "Instance with this name already exists.",
      });
    } else {
      setValidationError({
        ...validationError,
        instanceName: "",
      });
    }
  };

  const getConfigs = (condition: boolean, defaultConfigs: any) => {
    let currentConfigs = get(viewDetails, "items", []).flatMap(
      (item: ViewDetailsItemType) => {
        if (get(item, "ViewInfo.view_name") === view) {
          return get(item, "versions").flatMap((viewVersion: any) => {
            if (get(viewVersion, "ViewVersionInfo.version") === version) {
              return get(viewVersion, "ViewVersionInfo.parameters")
                .map((parameter: ParametersType) => {
                  if (
                    condition
                      ? get(parameter, "clusterConfig") === null
                      : get(parameter, "clusterConfig") !== null
                  ) {
                    const defaultVal = get(parameter, "defaultValue", null);
                    if (defaultVal !== null) {
                      let key = get(parameter, "name");
                      defaultConfigs[key] = defaultVal;
                    }
                    return {
                      label: get(parameter, "label"),
                      value: get(parameter, "name"),
                      placeholder: get(parameter, "placeholder"),
                      defaultValue: get(parameter, "defaultValue"),
                      isRequired: get(parameter, "required"),
                      masked: get(parameter, "masked"),
                      error: "",
                    };
                  }
                })
                .filter(Boolean);
            } else {
              return [];
            }
          });
        } else {
          return [];
        }
      }
    );
    setConfigs(defaultConfigs);
    return currentConfigs;
  };

  const resetValues = () => {
    setView(get(Object.keys(viewOptions), "[0]", ""));
    setVersion(get(viewOptions, `[${view}][0]`, ""));
    setInstanceName("");
    setDisplayName("");
    setDescription("");
    setClusterHandle("");
    setClusterConfigs([]);
    setNonClusterConfigs([]);
    setConfigs({});
    setActiveKey(Object.keys(clusterType)[0] as ClusterType);
    setIsVisible(true);
    setValidationError({
      instanceName: "",
      displayName: "",
      description: "",
    });
    setIsFormSubmitted(false);
  };

  const isFormValid = () => {
    let currentValidationError = cloneDeep(validationError);
    let hasError = false;
    for (let key in currentValidationError) {
      if (
        ((key === "instanceName" && !instanceName) ||
          (key === "displayName" && !displayName) ||
          (key === "description" && !description)) &&
        currentValidationError[key] === ""
      ) {
        currentValidationError[key] = "Field required!";
      }

      if (currentValidationError[key] !== "") {
        hasError = true;
      }
    }
    setValidationError(currentValidationError);

    if (nonClusterConfigs.length) {
      nonClusterConfigs.forEach(
        (nonClusterConfig: MappingType, idx: number) => {
          if (nonClusterConfig.isRequired) {
            if (
              nonClusterConfig.value in configs &&
              configs[nonClusterConfig.value] !== ""
            ) {
              set(nonClusterConfigs, `[${idx}]["error"]`, "");
            } else {
              set(nonClusterConfigs, `[${idx}]["error"]`, "Field required!");
              hasError = true;
            }
          }
        }
      );
    }

    if (clusterConfigs.length && activeKey === "custom") {
      clusterConfigs.forEach((clusterConfig: MappingType, idx: number) => {
        if (clusterConfig.isRequired) {
          if (
            clusterConfig.value in configs &&
            configs[clusterConfig.value] !== ""
          ) {
            set(clusterConfigs, `[${idx}]["error"]`, "");
          } else {
            set(clusterConfigs, `[${idx}]["error"]`, "Field required!");
            hasError = true;
          }
        }
      });
    }

    if (!clusterHandle && activeKey !== "custom") {
      hasError = true;
    }

    return !hasError;
  };

  const handleSave = async (event: any) => {
    event.preventDefault();
    if (isFormValid()) {
      const properties = Object.entries(configs).reduce(
        (acc: { [key: string]: any }, [key, value]) => {
          acc[key] = value === "" ? null : value;
          return acc;
        },
        {}
      );
      const viewData = {
        ViewInstanceInfo: {
          cluster_handle: clusterHandle === "" ? null : Number(clusterHandle),
          cluster_type: clusterType[activeKey],
          description: description,
          icon64_path: "",
          icon_path: "",
          instance_name: instanceName,
          label: displayName,
          properties: properties,
          visible: isVisible,
        },
      };
      await ViewApi.addView(view, version, instanceName, viewData);
      toast.success(
        <div className="toast-message">Created instance {instanceName}</div>
      );
      resetValues();
      onClose(false);
      successCallback();
      history.push(
        `views/${view}/versions/${version}/instances/${instanceName}/edit`
      );
    }
  };

  const isFormUpdated = () => {
    if (isThisCloneInstance) {
      return (
        instanceName !==
          get(viewInstanceInfoToBeCloned, "instance_name") + "_Copy" ||
        displayName !== get(viewInstanceInfoToBeCloned, "label") + "_Copy" ||
        description !== get(viewInstanceInfoToBeCloned, "description") ||
        isVisible !== get(viewInstanceInfoToBeCloned, "visible") ||
        clusterHandle !== get(viewInstanceInfoToBeCloned, "cluster_handle") ||
        JSON.stringify(configs) !==
          JSON.stringify(get(viewInstanceInfoToBeCloned, "properties"))
      );
    }
    return (
      view !== Object.keys(viewOptions)[0] ||
      instanceName ||
      displayName ||
      description ||
      clusterHandle !== get(clusterInfo, "items[0].Clusters.cluster_id", "").toString()
    );
  };

  const handleCancel = () => {
    if (isFormUpdated()) {
      setShowCancelWarning(true);
    } else {
      onClose(false);
    }
  };

  const handleWarningSave = (event: any) => {
    setShowCancelWarning(false);
    handleSave(event);
  };

  const handleWarningDiscard = () => {
    setShowCancelWarning(false);
    onClose(false);
  };

  return (
    <Modal data-testid="Create-instance"
      show={isOpen}
      onHide={handleCancel}
      size="lg"
      className="custom-modal-container make-scrollable"
    >
      <Modal.Header closeButton>
        <Modal.Title>
          {isThisCloneInstance ? (
            <h3>Clone Instance</h3>
          ) : (
            <h3>Create Instance</h3>
          )}
        </Modal.Title>
      </Modal.Header>
      {loading ? (
        <Spinner />
      ) : (
        <Form onSubmit={handleSave}>
          <Modal.Body>
            <Form.Group className="mb-4 d-flex ">
              <Form.Group className="me-5 w-100">
                <Form.Label>Select View *</Form.Label>
                <Form.Select
                  aria-label="Select View"
                  value={view}
                  className="w-100 custom-form-control"
                  onChange={(e) => setView(e.target.value)}
                  disabled={isThisCloneInstance}
                >
                  {Object.keys(viewOptions).map((viewOption, idx) => (
                    <option value={viewOption} key={idx}>
                      {viewOption}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group className="w-100">
                <Form.Label>Select Version *</Form.Label>
                <Form.Select
                  aria-label="Select Version"
                  value={version}
                  className="w-100 custom-form-control"
                  onChange={(e) => setVersion(e.target.value)}
                  disabled={isThisCloneInstance}
                >
                  {get(viewOptions, view, []).map(
                    (viewVersion: string, idx: number) => (
                      <option value={viewVersion} key={idx}>
                        {viewVersion}
                      </option>
                    )
                  )}
                </Form.Select>
              </Form.Group>
            </Form.Group>
            <Form.Group className="mb-4">
              <h5>Details</h5>
              <Form.Group className="mb-3">
                <Form.Label>Instance Name *</Form.Label>
                <Form.Control
                  data-testid="instance-name"
                  type="text"
                  value={instanceName}
                  onChange={(e) => {
                    setInstanceName(e.target.value);
                    validateInstanceName(e.target.value);
                  }}
                  className={
                    get(validationError, "instanceName") && isFormSubmitted
                      ? "border-danger"
                      : ""
                  }
                />
                {get(validationError, "instanceName") && isFormSubmitted ? (
                  <div className="text-danger mt-1">
                    <FontAwesomeIcon icon={faCircleXmark} />{" "}
                    {get(validationError, "instanceName")}
                  </div>
                ) : null}
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Display Name *</Form.Label>
                <Form.Control
                  data-testid="display-name"
                  type="text"
                  value={displayName}
                  onChange={(e) => {
                    setDisplayName(e.target.value);
                    if (e.target.value === "") {
                      setValidationError({
                        ...validationError,
                        displayName: "Field required!",
                      });
                    } else {
                      setValidationError({
                        ...validationError,
                        displayName: "",
                      });
                    }
                  }}
                  className={
                    get(validationError, "displayName") && isFormSubmitted
                      ? "border-danger"
                      : ""
                  }
                />
                {get(validationError, "displayName") && isFormSubmitted ? (
                  <div className="text-danger mt-1">
                    <FontAwesomeIcon icon={faCircleXmark} />{" "}
                    {get(validationError, "displayName")}
                  </div>
                ) : null}
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Description *</Form.Label>
                <Form.Control
                  data-testid="description"
                  type="text"
                  value={description}
                  onChange={(e) => {
                    setDescription(e.target.value);
                    if (e.target.value === "") {
                      setValidationError({
                        ...validationError,
                        description: "Field required!",
                      });
                    } else {
                      setValidationError({
                        ...validationError,
                        description: "",
                      });
                    }
                  }}
                  className={
                    get(validationError, "description") && isFormSubmitted
                      ? "border-danger"
                      : ""
                  }
                />
                {get(validationError, "description") && isFormSubmitted ? (
                  <div className="text-danger mt-1">
                    <FontAwesomeIcon icon={faCircleXmark} />{" "}
                    {get(validationError, "description")}
                  </div>
                ) : null}
              </Form.Group>
              <Form.Group className="mb-3 d-flex">
                <Form.Check
                  id="viewVisible"
                  type="checkbox"
                  className="custom-checkbox p-0"
                  checked={isVisible}
                  label="Visible"
                  onChange={() => setIsVisible(!isVisible)}
                />
              </Form.Group>
            </Form.Group>
            {nonClusterConfigs.length ? (
              <Form.Group className="mb-4">
                <h5>Settings</h5>
                {nonClusterConfigs.map((setting: MappingType, idx: number) => (
                  <Form.Group className="mb-3" key={idx}>
                    <Form.Label>
                      {setting.label}
                      {setting.isRequired ? " *" : ""}
                    </Form.Label>
                    <Form.Control
                      type="text"
                      placeholder={setting.placeholder}
                      value={configs[setting.value]}
                      defaultValue={setting.defaultValue}
                      onChange={(e) => {
                        setConfigs({
                          ...configs,
                          [setting.value]: e.target.value,
                        });
                        if (setting.isRequired) {
                          const updatedNonClusterConfigs = [
                            ...nonClusterConfigs,
                          ];
                          if (e.target.value === "") {
                            set(
                              updatedNonClusterConfigs,
                              `[${idx}]["error"]`,
                              "Field required!"
                            );
                          } else {
                            set(
                              updatedNonClusterConfigs,
                              `[${idx}]["error"]`,
                              ""
                            );
                          }
                          setNonClusterConfigs(updatedNonClusterConfigs);
                        }
                      }}
                      className={
                        setting.isRequired &&
                        get(nonClusterConfigs, `[${idx}]["error"]`) &&
                        isFormSubmitted
                          ? "border-danger"
                          : ""
                      }
                    />
                    {get(nonClusterConfigs, `[${idx}]["error"]`) &&
                    isFormSubmitted ? (
                      <div className="text-danger mt-1">
                        <FontAwesomeIcon icon={faCircleXmark} />{" "}
                        {get(nonClusterConfigs, `[${idx}]["error"]`)}
                      </div>
                    ) : null}
                  </Form.Group>
                ))}
              </Form.Group>
            ) : null}
            <Form.Group className="mb-4">
              <h5>Cluster Configuration</h5>
              <Form.Group className="mb-3 d-flex flex-column">
                <Form.Label>Cluster Type?</Form.Label>
                <div>
                  <Tab.Container activeKey={activeKey}>
                    <Nav
                      variant="pills"
                      className="mb-2"
                      onSelect={(selectedKey) => {
                        if (selectedKey)
                          setActiveKey(selectedKey as ClusterType);
                      }}
                    >
                      <Nav.Item>
                        <Nav.Link eventKey="local" className="tab-button">
                          LOCAL
                        </Nav.Link>
                      </Nav.Item>
                      <Nav.Item>
                        <Nav.Link eventKey="remote" className="tab-button" data-testid="/remote-toggle-button">
                          REMOTE
                        </Nav.Link>
                      </Nav.Item>
                      <Nav.Item>
                        {clusterConfigs.length ? (
                          <Nav.Link eventKey="custom" className="tab-button">
                            CUSTOM
                          </Nav.Link>
                        ) : null}
                      </Nav.Item>
                    </Nav>
                    <Form.Group className="mb-3">
                      <Form.Label>Cluster Name *</Form.Label>
                      <Tab.Content>
                        <Tab.Pane eventKey="local">
                          <Form.Select
                            value={clusterHandle}
                            className="w-50 custom-form-control  mb-3"
                            onChange={(e) => setClusterHandle(e.target.value)}
                          >
                            {get(clusterInfo, "items", []).map(
                              (info: ClusterInfoItemType, idx: number) => (
                                <option
                                  value={get(info, "Clusters.cluster_id")}
                                  key={idx}
                                >
                                  {get(info, "Clusters.cluster_name")}
                                </option>
                              )
                            )}
                          </Form.Select>
                        </Tab.Pane>
                        <Tab.Pane eventKey="remote">
                          <Form.Select
                            value={clusterHandle}
                            className="w-50 custom-form-control mb-3"
                            onChange={(e) => setClusterHandle(e.target.value)}
                          >
                            {get(remoteClusterInfo, "items", []).map(
                              (
                                info: RemoteClusterInfoItemType,
                                idx: number
                              ) => (
                                <option
                                  value={get(info, "ClusterInfo.cluster_id")}
                                  key={idx}
                                >
                                  {get(info, "ClusterInfo.name")}
                                </option>
                              )
                            )}
                          </Form.Select>
                        </Tab.Pane>
                        <Tab.Pane eventKey="custom">
                          <Form.Select
                            value={clusterHandle}
                            className="w-50 custom-form-control"
                            onChange={() => setClusterHandle("")}
                            disabled
                          ></Form.Select>
                        </Tab.Pane>
                        {!clusterHandle &&
                        activeKey !== "custom" &&
                        isFormSubmitted ? (
                          <div className="text-danger mt-1 mb-3">
                            <FontAwesomeIcon icon={faCircleXmark} />
                            {" Field required!"}
                          </div>
                        ) : null}
                        {clusterConfigs?.length
                          ? clusterConfigs?.map(
                              (clusterConfig: MappingType, idx: number) => (
                                <Form.Group className="mb-3" key={idx}>
                                  <Form.Label>
                                    {clusterConfig.label}
                                    {clusterConfig.isRequired ? " *" : ""}
                                  </Form.Label>
                                  <Form.Control
                                    type="text"
                                    disabled={activeKey !== "custom"}
                                    placeholder={clusterConfig.placeholder}
                                    defaultValue={clusterConfig.defaultValue}
                                    value={configs[clusterConfig.value]}
                                    onChange={(e) => {
                                      setConfigs({
                                        ...configs,
                                        [clusterConfig.value]: e.target.value,
                                      });
                                      if (clusterConfig.isRequired) {
                                        if (e.target.value === "") {
                                          set(
                                            clusterConfigs,
                                            `[${idx}]["error"]`,
                                            "Field required!"
                                          );
                                        } else {
                                          set(
                                            clusterConfigs,
                                            `[${idx}]["error"]`,
                                            ""
                                          );
                                        }
                                      }
                                    }}
                                    className={
                                      clusterConfig.isRequired &&
                                      get(
                                        clusterConfigs,
                                        `[${idx}]["error"]`
                                      ) &&
                                      isFormSubmitted
                                        ? "border-danger"
                                        : ""
                                    }
                                  />
                                  {get(clusterConfigs, `[${idx}]["error"]`) &&
                                  isFormSubmitted ? (
                                    <div className="text-danger mt-1">
                                      <FontAwesomeIcon icon={faCircleXmark} />{" "}
                                      {get(clusterConfigs, `[${idx}]["error"]`)}
                                    </div>
                                  ) : null}
                                </Form.Group>
                              )
                            )
                          : null}
                      </Tab.Content>
                    </Form.Group>
                  </Tab.Container>
                </div>
              </Form.Group>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <DefaultButton onClick={handleCancel}>Cancel</DefaultButton>
            <WarningModal
              isOpen={showCancelWarning}
              onClose={() => setShowCancelWarning(false)}
              handleWarningDiscard={handleWarningDiscard}
              handleWarningSave={handleWarningSave}
            />
            <Button
              className="custom-btn"
              type="submit"
              variant="success"
              onClick={() => setIsFormSubmitted(true)}
            >
              Save
            </Button>
          </Modal.Footer>
        </Form>
      )}
    </Modal>
  );
}

