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
import { useContext, useEffect, useState } from "react";
import ViewsInformationApi from "../../api/viewsApiInfo";
import { Link, useHistory, useParams } from "react-router-dom";
import { Alert, Button, Col, Form, Row } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck, faPencil } from "@fortawesome/free-solid-svg-icons";
import { cloneDeep, get, set, startCase } from "lodash";
import DefaultButton from "../../components/DefaultButton";
import Select from "react-select";
import {
  FieldType,
  Group,
  Options,
  PrivilegesType,
  setDetailsType,
  setPermissionsType,
  User,
  ViewSectionsType,
} from "./types";

import { ClusterType, ControlType, EntityType } from "./enums";

import ConfirmationModal from "../../components/ConfirmationModal";
import UserGroupApi from "../../api/userGroupApi";
import AppContent from "../../context/AppContext";
import Spinner from "../../components/Spinner";
import toast from "react-hot-toast";
import {
  latestShortViewUrl,
  latestViewInstanceUrl,
} from "../../utils/navigation";

export default function EditInstance() {
  const [loading, setLoading] = useState(true);
  const { viewName, version, instanceName } = useParams<{
    viewName: string;
    instanceName: string;
    version: string;
  }>();
  const [instanceData, setInstanceData] = useState<any>();
  const [ViewData, setViewData] = useState<any>();
  const [clusterApiPermissions, setClusterApiPermissions] = useState<any>([]);
  const [permissionsEditLoading, setPermissionsEditLoading] = useState(false);
  const [isInstanceDataTransformed, setIsInstanceDataTransformed] =
    useState(false);

  const [showDeleteShortUrlModal, setShowDeleteShortUrlModal] = useState(false);

  const deleteShortUrlModal = () => {
    setShowDeleteShortUrlModal(true);
  };

  const deleteShortUrl = async (urlName: string) => {
    try {
      await ViewsInformationApi.deleteShortUrl(urlName);
      toast.success("Short Url deleted");
      viewSections.details.isEditing = false;
      getInstanceDetails();
    } catch (error) {
      toast.error("Failed to delete URL");
    }
  };

  const {
    cluster: { cluster_name: clusterName },
    setSelectedOption,
  } = useContext(AppContent);

  const viewSectionsObj: ViewSectionsType = {
    details: {
      isEditable: true,
      isEditing: false,
      apiResponsible: ViewData,
      fields: [
        {
          label: "Instance Name",
          type: ControlType.INPUT,
          hasError: false,
          isEditable: false,
          id: "instanceName",
          value: "",
          originalValue: "",
          apiResponseKey: ["instance_name"],
          required: true,
        },
        {
          label: "Display Name",
          type: ControlType.INPUT,
          hasError: false,
          isEditable: true,
          id: "displayName",
          value: "",
          originalValue: "",
          apiResponseKey: ["label"],
          required: true,
          validationRegEx: /^[^\s][a-zA-Z0-9_. ]*$/,
          errorMessage: "Must not contain any special characters",
        },
        {
          label: "Description",
          type: ControlType.INPUT,
          hasError: false,
          isEditable: true,
          id: "description",
          value: "",
          originalValue: "",
          apiResponseKey: ["description"],
          required: true,
        },
        {
          label: "Short URL",
          type: ControlType.LINK,
          hasError: false,
          isEditable: false,
          id: "shortUrl",
          value: "Create New URL",
          href: `/urls/link/${viewName}/${version}/${instanceName}`,
          prefixUrl: `/main/view/${viewName}/`,
          valuePlaceholder: "Create New URL",
          defaultUrl: `/urls/link/${viewName}/${version}/${instanceName}`,
          originalValue: "",
          isDeletable: false,
          deleteCallBack: deleteShortUrlModal,
          apiResponseKey: ["short_url"],
        },
        {
          label: "Visible",
          type: ControlType.CHECKBOX,
          value: false,
          hasError: false,
          isEditable: true,
          id: "visible",
          originalValue: "",
          apiResponseKey: ["visible"],
        },
      ],
    },
  };

  const [viewSections, setViewSections] =
    useState<ViewSectionsType>(viewSectionsObj);

  async function getInstanceDetails() {
    const data: unknown = await ViewsInformationApi.getInstanceLabel(
      viewName,
      version,
      instanceName
    );
    setInstanceData(data);
  }

  async function getViewDetails() {
    const data: any = await ViewsInformationApi.getViewDetails(
      viewName,
      version
    );
    setViewData(data);
  }

  const mapApiDataToFields = (viewSectionsToBeUpdated = viewSections) => {
    setLoading(true);
    const updatedSections = cloneDeep(viewSectionsToBeUpdated);
    Object.keys(updatedSections).map((section) => {
      updatedSections[section]?.fields.map((field: any) => {
        if (field.apiResponseKey) {
          //Pass the values to get in an array because response keys contain key in it
          field.value = get(
            instanceData.ViewInstanceInfo,
            field.apiResponseKey,
            field.type === ControlType.LINK ? field.valuePlaceholder : ""
          );

          field.originalValue = get(
            instanceData.ViewInstanceInfo,
            field.apiResponseKey,
            field.type === ControlType.LINK ? field.valuePlaceholder : ""
          );

          field.placeholderValue = get(
            instanceData?.ViewInstanceInfo,
            field.placeholder,
            ""
          );

          if (field.prefixUrl) {
            if (get(instanceData.ViewInstanceInfo, field.apiResponseKey)) {
              const shortUrl = String(field.value);
              field.value = field.prefixUrl + shortUrl;
              field.href = latestShortViewUrl(viewName, shortUrl);
              field.isExternal = true;
              field.isDeletable = true;
              field.originalValue = field.value;
            } else {
              field.value = field.valuePlaceholder || "";
              field.href = field.defaultUrl || "";
              field.isExternal = false;
              field.isDeletable = false;
            }
          }
        }
      });
    });
    setViewSections(updatedSections);
    setLoading(false);
  };

  useEffect(() => {
    setSelectedOption("Views");
    getViewDetails();
    getInstanceDetails();
  }, []);

  useEffect(() => {
    if (instanceData?.ViewInstanceInfo && isInstanceDataTransformed) {
      modifySections();
    }
  }, [isInstanceDataTransformed]);

  useEffect(() => {
    const transformInstanceData = () => {
      const instanceDataCopy = cloneDeep(instanceData);
      const viewInstanceInfo = instanceDataCopy.ViewInstanceInfo;
      const viewVersionInfo = ViewData.ViewVersionInfo;
      Object.keys(viewInstanceInfo.properties).map((propertyKey) => {
        const matchingKeyFromViewVersionInfo = viewVersionInfo.parameters.find(
          (parameter: any) => {
            return parameter.name === propertyKey;
          }
        );
        const matchingKeyFromSettings = null;

        if (matchingKeyFromViewVersionInfo) {
          set(viewInstanceInfo, ["properties", propertyKey, "viewInfo"], {
            ...matchingKeyFromViewVersionInfo,
            value: viewInstanceInfo.properties[propertyKey],
            isSetting: matchingKeyFromSettings ? true : false,
          });
        }
      });
      setInstanceData(instanceDataCopy);
      setIsInstanceDataTransformed(true);
    };
    if (
      instanceData?.ViewInstanceInfo &&
      ViewData?.ViewVersionInfo &&
      !isInstanceDataTransformed
    )
      transformInstanceData();
  }, [ViewData, instanceData]);

  const modifySections = () => {
    const transformedInstanceData = cloneDeep(instanceData);
    const updatedSections = cloneDeep(viewSections);
    set(updatedSections, "settings", {
      isEditable: true,
      isEditing: false,
      fields: Object.keys(instanceData.ViewInstanceInfo?.properties)
        ?.filter((property) => {
          const currentProperty = get(
            transformedInstanceData?.ViewInstanceInfo?.properties,
            [property],
            undefined
          );
          return !currentProperty?.viewInfo?.clusterConfig;
        })
        .map((property) => {
          const currentProperty = get(
            transformedInstanceData?.ViewInstanceInfo?.properties,
            [property],
            undefined
          );
          return {
            label: currentProperty.viewInfo.label,
            type: ControlType.INPUT,
            hasError: false,
            isEditable: true,
            id: property,
            originalValue: "",
            value: "",
            placeholder: ["properties", property, "viewInfo", "placeholder"],
            apiResponseKey: ["properties", property, "viewInfo", "value"],
            required: currentProperty.viewInfo.required,
          };
        }),
    });
    set(updatedSections, "clusterConfiguration", {
      isEditable: transformedInstanceData.clustertype === ClusterType.CUSTOM,
      isEditing: false,
      fields: Object.keys(transformedInstanceData?.ViewInstanceInfo?.properties)
        ?.filter((property) => {
          const currentProperty = get(
            transformedInstanceData?.ViewInstanceInfo?.properties,
            [property],
            undefined
          );
          return get(currentProperty, "viewInfo.clusterConfig");
        })
        .map((property) => {
          const currentProperty = get(
            transformedInstanceData?.ViewInstanceInfo?.properties,
            [property],
            undefined
          );

          return {
            label: currentProperty.viewInfo.label,
            type: ControlType.INPUT,
            hasError: false,
            isEditable: true,
            id: property,
            originalValue: "",
            value: "",
            placeholder: ["properties", property, "viewInfo", "placeholder"],
            apiResponseKey: ["properties", property, "viewInfo", "value"],
            required: currentProperty.viewInfo.required,
          };
        }),
    });

    setViewSections(updatedSections);
    mapApiDataToFields(updatedSections);
  };

  const [users, setUsers] = useState<User[]>();
  const [groups, setGroups] = useState<Group[]>();
  const [selectedUsers, setSelectedUsers] = useState<Options[]>([]);
  const [selectedGroups, setSelectedGroups] = useState<Options[]>([]);
  const [privileges, setPrivileges] = useState<PrivilegesType[]>();
  const history = useHistory();

  async function getPrivilegePermission() {
    const data: any = await ViewsInformationApi.getPrivileges(
      viewName,
      version,
      instanceName
    );
    setPrivileges(data.privileges);
    setClusterApiPermissions(data.privileges);
  }

  useEffect(() => {
    async function getUserDetails() {
      const data: any = await UserGroupApi.usersList("Users");
      setUsers(data.items);
    }
    getUserDetails();
    async function getGroupsDetails() {
      const data: any = await UserGroupApi.groupsList("Groups");
      setGroups(data.items);
    }
    getGroupsDetails();

    getPrivilegePermission();
  }, []);
  useEffect(() => {
    if (privileges) {
      const usersTemp: Options[] = [];
      const groupsTemp: Options[] = [];
      privileges.map((privilege: PrivilegesType) => {
        const option: Options = {
          value: privilege.PrivilegeInfo.principal_name,
          label: privilege.PrivilegeInfo.principal_name,
        };
        if (privilege.PrivilegeInfo.principal_type === EntityType.USER) {
          usersTemp.push(option);
        } else if (
          privilege.PrivilegeInfo.principal_type === EntityType.GROUP
        ) {
          groupsTemp.push(option);
        }
        return privilege;
      });
      setSelectedUsers(usersTemp);
      setSelectedGroups(groupsTemp);
    }
  }, [privileges]);

  useEffect(() => {
    if (privileges) {
      const permissionsCopy = cloneDeep(localClusterPermissions);

      privileges?.map((privilege: PrivilegesType) => {
        if (privilege?.PrivilegeInfo?.principal_type === EntityType.ROLE) {
          permissionsCopy[
            privilege?.PrivilegeInfo?.principal_name
          ].permissionGranted = true;
        }
      });

      setLocalClusterPermissions(permissionsCopy);
    }
  }, [privileges]);

  type Roles = {
    [key: string]: {
      permissionGranted: boolean;
    };
  };

  const userOptions = users?.map((user) => ({
    value: user.Users.user_name,
    label: user.Users.user_name,
  }));
  const groupOptions = groups?.map((group) => ({
    value: group.Groups.group_name,
    label: group.Groups.group_name,
  }));

  const [localClusterPermissions, setLocalClusterPermissions] = useState<Roles>(
    {
      "CLUSTER.ADMINISTRATOR": {
        permissionGranted: false,
      },
      "CLUSTER.OPERATOR": {
        permissionGranted: false,
      },
      "SERVICE.OPERATOR": {
        permissionGranted: false,
      },
      "SERVICE.ADMINISTRATOR": {
        permissionGranted: false,
      },
      "CLUSTER.USER": {
        permissionGranted: false,
      },
    }
  );

  const setPermissions = async (permissionsObj = localClusterPermissions) => {
    const data: setPermissionsType[] = [];

    if (selectedUsers.length !== 0) {
      selectedUsers.forEach((user: Options) => {
        const addPrivilege: setPermissionsType = {
          PrivilegeInfo: {
            permission_name: "VIEW.USER",
            principal_name: user.label,
            principal_type: "USER",
          },
        };
        data.push(addPrivilege);
      });
    }

    if (selectedGroups.length !== 0) {
      selectedGroups.forEach((group: Options) => {
        const addPrivilege: setPermissionsType = {
          PrivilegeInfo: {
            permission_name: "VIEW.USER",
            principal_name: group.label,
            principal_type: "GROUP",
          },
        };
        data.push(addPrivilege);
      });
    }

    for (const clusterPermission in permissionsObj) {
      if (permissionsObj[clusterPermission].permissionGranted === true) {
        const addPrivilege: setPermissionsType = {
          PrivilegeInfo: {
            permission_name: "VIEW.USER",
            principal_name: clusterPermission,
            principal_type: "ROLE",
          },
        };
        data.push(addPrivilege);
      }
    }
    if (clusterApiPermissions.length !== data.length) {
      setPermissionsEditLoading(true);
      try {
        await ViewsInformationApi.updatePrivileges(
          viewName,
          version,
          instanceName,
          data
        );
        toast.success("Updated permissions");
        console.log("Updated permissions");
      } catch (error) {
        toast.error("Could not update permissions");
        console.log("Could not update permissions", error);
      }

      getPrivilegePermission();
      setPermissionsEditLoading(false);

    }
  };

  const updateRolePrivileges = (principal_name: string) => {
    const permissionsCopy = cloneDeep(localClusterPermissions);

    if (permissionsCopy[principal_name]) {
      permissionsCopy[principal_name].permissionGranted =
        !permissionsCopy[principal_name].permissionGranted;
    }

    setLocalClusterPermissions(permissionsCopy);
    setPermissions(permissionsCopy);
  };

  const [showDeleteModal, setShowDeleteModal] = useState(Boolean);
  const deleteInstance = async () => {
    try {
      await ViewsInformationApi.deleteInstance(viewName, version, instanceName);
      console.log("Instance deleted successfully");
    } catch (error) {
      console.error("Failed to delete instance", error);
    }
  };

  const handleEditSection = (section: string, key: string, value: any) => {
    const updatedSections = cloneDeep(viewSections);
    if (updatedSections[section])
      updatedSections[section][key as keyof typeof viewSections.details] =
        value;
    updatedSections[section]?.fields.map((field: any) => {
      if (
        (field.validationRegEx && !field.validationRegEx.test(field.value)) ||
        (field.required && !field.value)
      ) {
        field.hasError = true;
      }
    });
    setViewSections(updatedSections);
  };

  const handleEditField = (
    section: string,
    fieldId: string,
    key: string,
    value: any
  ) => {
    const updatedSections = cloneDeep(viewSections);
    updatedSections[section]?.fields.map((field: any) => {
      if (field.id === fieldId) {
        field[key] = value;
      }
      if (
        (field.validationRegEx &&
          !field.validationRegEx.test(
            field.value ? field.value.toString() : ""
          )) ||
        (field.required && !field.value)
      ) {
        field.hasError = true;
      } else {
        field.hasError = false;
      }
    });
    setViewSections(updatedSections);
  };

  const renderField = (
    field: FieldType,
    isSectionEditing: boolean,
    sectionName: string
  ) => {
    switch (field.type) {
      case ControlType.INPUT:
        return (
          <>
            <Col md={4} className="nowrap">
              {field.label}
            </Col>
            <Col md={4}>
              <div className="d-flex flex-column">
                <Form.Control
                  placeholder={field.placeholderValue as string}
                  type="text"
                  value={field.value ? field.value.toString() : ""}
                  className={field.hasError ? "border-danger" : ""}
                  data-testid={field.id}
                  onChange={(e) => {
                    handleEditField(
                      sectionName,
                      field.id,
                      "value",
                      e.target.value
                    );
                  }}
                  isInvalid={
                    (isSectionEditing && field.required && !field.value) ||
                    (field.validationRegEx &&
                      !field.validationRegEx.test(String(field.value)))
                  }
                  disabled={!isSectionEditing || !field.isEditable}
                ></Form.Control>
                {isSectionEditing &&
                  field.required &&
                  (!field.value ||
                    (typeof field.value === "string" &&
                      (!field.value.trim() ||
                        field.value.startsWith(" ")))) && (
                    <Form.Control.Feedback className="mt-2" type="invalid">
                      Field is required
                    </Form.Control.Feedback>
                  )}
                {isSectionEditing &&
                field.validationRegEx &&
                !field.validationRegEx.test(String(field.value)) ? (
                  <Form.Control.Feedback type="invalid">
                    {field.errorMessage}
                  </Form.Control.Feedback>
                ) : null}
              </div>
            </Col>
          </>
        );
      case ControlType.LINK:
        return (
          <>
            <Col md={4}>{field.label}</Col>
            <Col md={4} className="align-items-center">
              {field.href && (
                field.isExternal ? (
                  <a
                    href={field.href}
                    className="custom-link"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {field.value}
                  </a>
                ) : (
                  <Link to={field.href} className="custom-link">
                    {field.value}
                  </Link>
                )
              )}
              {field.isDeletable && isSectionEditing ? (
                <a
                  className="mx-2 text-danger"
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    field.deleteCallBack && field.deleteCallBack();
                  }}
                >
                  Delete
                </a>
              ) : null}
            </Col>
          </>
        );
      case ControlType.CHECKBOX:
        return (
          <>
            <Col md={3} className="invisible"></Col>
            <Col md={3} className="ms-n2">
              <Form.Check
                checked={field.value === true}
                label={field.label}
                id={field.id}
                disabled={!isSectionEditing || !field.isEditable}
                onChange={() => {
                  handleCheckboxChange(sectionName, field.id, "value");
                }}
              ></Form.Check>
            </Col>
          </>
        );
    }
  };

  const handleCheckboxChange = (section: string,
    fieldId: string,
    key: string) => {
      const updatedSections = cloneDeep(viewSections);
    updatedSections[section]?.fields.map((field: any) => {
      if (field.id === fieldId) {
        field[key] = !field[key];
      }
    });
    setViewSections(updatedSections);
    }



  const setDetails = async () => {
    const data: setDetailsType = {
      ViewInstanceInfo: {
        visible: String(viewSections.details.fields[4].value),
        label: String(viewSections.details.fields[1].value),
        description: String(viewSections.details.fields[2].value),
      },
    };
    try {
      await ViewsInformationApi.updateDetails(
        viewName,
        version,
        instanceName,
        data
      );
      toast.success("Updated instance details successfully");
    } catch {
      toast.error("Failed to update instance details");
    }
  };

  const createSettingsPayload = (section: string) => {
    let properties: any = {};

    viewSections[section]?.fields.map((field) => {
      properties[field.id] = field.value;
    });
    return {
      ViewInstanceInfo: {
        properties: properties,
      },
    };
  };

  const setSettings = async () => {
    const data = createSettingsPayload("settings");
    try {
      await ViewsInformationApi.updateSettings(
        viewName,
        version,
        instanceName,
        data
      );
      toast.success("Updated instance settings successfully");
    } catch {
      toast.error("Failed to update instance settings");
    }
  };

  const createClusterConfiguartionPayload = (section: string) => {
    let properties: any = {};

    viewSections[section]?.fields.map((field) => {
      properties[field.id] = field.value;
    });
    return {
      ViewInstanceInfo: {
        properties: properties,
      },
    };
  };

  const setclusterconfiguration = async () => {
    const data = createClusterConfiguartionPayload("clusterConfiguration");
    try {
      await ViewsInformationApi.updateSettings(
        viewName,
        version,
        instanceName,
        data
      );
      toast.success("Updated instance configuration successfully");
    } catch {
      toast.error("Failed to update instance configuration");
    }
  };

  const saveSection = (section: string) => {
    const updatedSections = cloneDeep(viewSections);
    let hasError = false;
    updatedSections[section]?.fields.map((field: any) => {
      if (field.hasError) {
        hasError = true;
      }
    });
    if (hasError) {
      return;
    } else {
      if (updatedSections[section]) updatedSections[section].isEditing = false;
      updatedSections[section]?.fields.map((field: any) => {
        field.originalValue = field.value;
      });
      setViewSections(updatedSections);
      switch (section) {
        case "details":
          setDetails();
          break;

        case "settings":
          setSettings();
          break;

        case "clusterConfiguration":
          setclusterconfiguration();
          break;

        default:
          console.log("Api call failed");
      }
    }
  };

  const handleCancelEditing = (section: string) => {
    const updatedSections = cloneDeep(viewSections);
    if (updatedSections[section])
      set(updatedSections, `[${section}].isEditing`, false);
    if (updatedSections[section])
      updatedSections[section].fields.map((field) => {
        field.hasError = false;
        field.value = field.originalValue;
      });
    setViewSections(updatedSections);
  };

  const renderSections = () => {
    return Object.keys(viewSections).map((section) => {
      const currentSection = viewSections[section];
      return get(viewSections[section], "fields").length !== 0 ? (
        <div className="mt-4 border">
          <div className="d-flex justify-content-between bg-light p-2 px-3 align-items-center">
            <h5>{startCase(section)}</h5>
            {currentSection.isEditable && !currentSection.isEditing ? (
              <div
                className="custom-link d-flex cursor-pointer"
                onClick={() => {
                  handleEditSection(section, "isEditing", true);
                }}
              >
                <FontAwesomeIcon icon={faPencil} />
                <div className="ms-1" data-testid={section}>
                  Edit
                </div>
              </div>
            ) : null}
          </div>
          {currentSection?.fields.map((currentSectionField) => {
            return (
              <div className="justify-content-center">
                <Row className="my-3">
                  <Col md={2}></Col>
                  {renderField(
                    currentSectionField,
                    currentSection.isEditing,
                    section
                  )}
                </Row>
              </div>
            );
          })}
          {currentSection?.isEditing ? (
            <div className="d-flex justify-content-end pe-2 pb-3">
              <DefaultButton
                onClick={() => {
                  handleCancelEditing(section);
                }}
              >
                CANCEL
              </DefaultButton>
              <Button
                data-testid={section}
                variant="success"
                size="sm"
                className="ms-2"
                onClick={() => {
                  saveSection(section);
                }}
              >
                SAVE
              </Button>
            </div>
          ) : null}
        </div>
      ) : null;
    });
  };

  const selectAllPermissions = async () => {
    const permissionsCopy = cloneDeep(localClusterPermissions);
    for (const permissions in permissionsCopy) {
      permissionsCopy[permissions].permissionGranted = true;
    }
    setLocalClusterPermissions(permissionsCopy);
    setPermissions(permissionsCopy);
  };

  const deselectAllPermissions = async () => {
    const permissionsCopy = cloneDeep(localClusterPermissions);
    for (const permissions in permissionsCopy) {
      permissionsCopy[permissions].permissionGranted = false;
    }
    setLocalClusterPermissions(permissionsCopy);
    setPermissions(permissionsCopy);
  };

  return (
    <div data-testid="edit-instance">
      {loading ? (
        <Spinner />
      ) : (
        <div>
          <div>
            <div
              className="breadcrumb d-flex border-bottom pb-2"
              style={{ flexWrap: "nowrap" }}
            >
              <Link to="/views" className="custom-link">
                {""}
                <h3>Views</h3>
              </Link>
              <div className="mx-2">
                <h3 className="text-muted">/</h3>
              </div>
              <div className="d-flex justify-content-between w-100">
                <div className="text-muted d-flex align-items-center">
                  {/* <h4>{instanceData?.ViewInstanceInfo?.label}</h4> */}
                  <h4>{viewSections.details.fields[1].originalValue}</h4>
                  <a
                    href={latestViewInstanceUrl(viewName, version, instanceName)}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <h6 className="ms-2 custom-link">
                      Go to instance
                    </h6>
                  </a>
                </div>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => {
                    setShowDeleteModal(true);
                  }}
                >
                  DELETE INSTANCE
                </Button>
                <ConfirmationModal
                  isOpen={showDeleteModal}
                  onClose={() => setShowDeleteModal(false)}
                  modalTitle={"Delete View Instance"}
                  modalBody={`Are you sure you want to delete view instance ${instanceData?.ViewInstanceInfo.label}
            ?`}
                  successCallback={async () => {
                    try {
                      await deleteInstance();
                      setShowDeleteModal(false);
                      toast.success("Instance deleted successfully");
                      history.push("/views");
                    } catch (error) {
                      toast.error("Failed to delete instance");
                      console.error("Failed to delete instance:", error);
                    }
                  }}
                />
              </div>
            </div>
            <Row className="align-items-center">
              <Col md={4}>View</Col>
              <Col md={4}>{viewName}</Col>
            </Row>
            <Row className="align-items-center mt-3">
              <Col md={4}>Version</Col>
              <Col md={4}>
                <Form.Control
                  type="text"
                  disabled
                  value={version}
                ></Form.Control>
              </Col>
            </Row>
            {renderSections()}
          </div>
          <div className="border mx-0.5 mt-4">
            <div className="border-bottom bg-light">
              <div className="d-flex justify-content-between px-3 py-1 fs-6">
                <h5 className="me-1 p-2">Permissions</h5>
              </div>
            </div>
            <div className="px-3 py-3 w-100">
              <Row className="border-bottom align-items-center w-100 nowrap p-2 mcx-1">
                <Col md={4}>Permission</Col>
                <Col md={4}>Grant permission to these users</Col>
                <Col md={4}>Grant permission to these groups</Col>
              </Row>
              <Row className="p-2 mcx-1 align-items-center nowrap">
                <Col md={4}>Use</Col>
                <Col md={4} className="px-0 d-flex align-items-center">
                  <Select
                    isMulti
                    name="users"
                    options={userOptions}
                    placeholder="Select Users"
                    className="w-75"
                    value={selectedUsers}
                    onChange={(e: any) => {
                      setSelectedUsers(e);
                    }}
                    aria-label="select-users"
                  />

                  <DefaultButton
                    data-testid="set-users"
                    className="ms-2"
                    onClick={() => setPermissions()}
                  >
                    <FontAwesomeIcon icon={faCheck} />
                  </DefaultButton>
                </Col>
                <Col md={4} className="px-0 d-flex align-items-center">
                  <Select
                    isMulti
                    name="groups"
                    options={groupOptions}
                    className="basic-multi-select w-75"
                    placeholder="Select Groups"
                    value={selectedGroups}
                    onChange={(e: any) => setSelectedGroups(e)}
                    aria-label="select-groups"
                  />
                  <DefaultButton
                    data-testid="set-groups"
                    className="ms-2"
                    onClick={() => setPermissions()}
                  >
                    <FontAwesomeIcon icon={faCheck} />
                  </DefaultButton>
                </Col>
              </Row>
            </div>

            {instanceData?.ViewInstanceInfo.cluster_type ===
            ClusterType.LOCAL ? (
              <div className="px-3 py-3 w-100">
                <Row className="border-bottom align-items-center w-100 nowrap p-2 mcx-1">
                  <Col>Local Cluster Permissons </Col>
                </Row>
                <Row className="px-3 py-3">
                  <Col>
                    Grant Use permission for the following {clusterName} Roles:
                  </Col>
                </Row>
                <Row>
                  <Form.Check
                    type="checkbox"
                    label="Cluster Administrator"
                    id="clusterAdministrator"
                    checked={
                      localClusterPermissions["CLUSTER.ADMINISTRATOR"]
                        .permissionGranted
                    }
                    onChange={() =>
                      updateRolePrivileges("CLUSTER.ADMINISTRATOR")
                    }
                  />
                  <Form.Check
                    type="checkbox"
                    label="Cluster Operator"
                    id="clusterOperator"
                    checked={
                      localClusterPermissions["CLUSTER.OPERATOR"]
                        .permissionGranted
                    }
                    onChange={() => updateRolePrivileges("CLUSTER.OPERATOR")}
                  />

                  <Form.Check
                    type="checkbox"
                    label="Service Operator"
                    id="serviceOperator"
                    checked={
                      localClusterPermissions["SERVICE.OPERATOR"]
                        .permissionGranted
                    }
                    onChange={() => updateRolePrivileges("SERVICE.OPERATOR")}
                  />

                  <Form.Check
                    type="checkbox"
                    label="Service Administrator"
                    id="serviceAdministrator"
                    checked={
                      localClusterPermissions["SERVICE.ADMINISTRATOR"]
                        .permissionGranted
                    }
                    onChange={() =>
                      updateRolePrivileges("SERVICE.ADMINISTRATOR")
                    }
                  />

                  <Form.Check
                    type="checkbox"
                    label="Cluster User"
                    id="clusterUser"
                    checked={
                      localClusterPermissions["CLUSTER.USER"].permissionGranted
                    }
                    onChange={() => updateRolePrivileges("CLUSTER.USER")}
                  />
                </Row>
                <Row className="mx-3">
                  <Col>
                    <a
                      data-testid="check-all"
                      href="#"
                      onClick={(e) => {
                        e.preventDefault();
                        if (!permissionsEditLoading) selectAllPermissions();
                      }}
                      className="text-info"
                    >
                      Check all
                    </a>{" "}
                    |{" "}
                    <a
                      data-testid="clear-all"
                      href="#"
                      onClick={(e) => {
                        e.preventDefault();
                        if (!permissionsEditLoading) deselectAllPermissions();
                      }}
                      className="text-info"
                    >
                      Clear all
                    </a>
                  </Col>
                </Row>
              </div>
            ) : (
              <div className="px-3 py-3 w-100">
                <Row className="border-bottom align-items-center w-100 nowrap p-2 mcx-1">
                  <Col>Local Cluster Permissons </Col>
                </Row>
                <Alert className="my-3 mx-0">
                  The ability to inherit view Use permission based on Cluster
                  Roles is only available when using a Local Cluster
                  configuration.
                </Alert>
              </div>
            )}
          </div>
          <ConfirmationModal
            isOpen={showDeleteShortUrlModal}
            onClose={() => setShowDeleteShortUrlModal(false)}
            modalTitle={"Delete URL"}
            modalBody={`Are you sure you want to delete url "${instanceData?.ViewInstanceInfo.short_url_name}" 
            ?`}
            successCallback={async () => {
              setShowDeleteShortUrlModal(false);
              deleteShortUrl(instanceData?.ViewInstanceInfo.short_url_name);
            }}
          />
        </div>
      )}
    </div>
  );
}
