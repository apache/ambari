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

import React from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faPlus,
  faMinus,
  faGear,
  faEnvelope,
} from "@fortawesome/free-solid-svg-icons";
import {
  AlertGroupItem,
  AlertDefinition,
  AlertGroupState,
  AlertDefinitionReference,
  AlertNotification,
} from "../types";
import EditableList from "../../../components/EditableList";
import Select from "react-select";
import { validateRepeatTolerance } from '../../../Utils/alertDefinitions';

// Helper function to get definitions from a group
const getDefinitions = (
  group: AlertGroupItem | AlertGroupState | null
): AlertDefinitionReference[] => {
  if (!group) return [];
  if ("AlertGroup" in group) {
    return group.AlertGroup.definitions || [];
  }
  return group.definitions || [];
};

// Helper function to get group ID
const getGroupId = (
  group: AlertGroupItem | AlertGroupState | null
): number | undefined => {
  if (!group) return undefined;
  return "AlertGroup" in group ? group.AlertGroup.id : group.id;
};

// Helper function to check if group is default
const isDefaultGroup = (
  group: AlertGroupItem | AlertGroupState | null
): boolean => {
  if (!group) return false;
  return "AlertGroup" in group ? group.AlertGroup.default : group.default;
};

// Helper function to get notifications for a group
const getGroupNotifications = (
  group: AlertGroupItem | AlertGroupState | null,
  notifications: AlertNotification[] = []
): AlertNotification[] => {
  if (!group || !notifications.length) return [];

  const targets = "AlertGroup" in group
    ? group.AlertGroup.targets || []
    : group.targets || [];
  const targetIds = new Set(targets.flatMap((target) => {
    const id = Number(typeof target === "number" ? target : target.id);
    return Number.isFinite(id) && id > 0 ? [id] : [];
  }));

  return notifications.filter((notification) => {
    const id = Number(notification.AlertTarget.id);
    return Number.isFinite(id) && targetIds.has(id);
  });
};

interface NewGroupModalProps {
  newGroupName: string;
  setNewGroupName: (name: string) => void;
  modalError: string;
  modalSuccess: string;
}

export const NewGroupModal: React.FC<NewGroupModalProps> = ({
  newGroupName,
  setNewGroupName,
  modalError,
  modalSuccess,
}) => {
  return (
    <div className="modal-body">
      <div className="form-group">
        <label htmlFor="groupName">Group Name</label>
        <input
          type="text"
          className="form-control"
          id="groupName"
          value={newGroupName}
          onChange={(e) => {
            setNewGroupName(e.target.value);
          }}
          placeholder="Enter group name"
        />
      </div>
      {modalError && (
        <div className="alert alert-danger mt-3" role="alert">
          {modalError}
        </div>
      )}
      {modalSuccess && (
        <div className="alert alert-success mt-3" role="alert">
          {modalSuccess}
        </div>
      )}
    </div>
  );
};

interface ManageAlertGroupsModalProps {
  alertGroups: AlertGroupItem[];
  selectedGroup: AlertGroupItem | AlertGroupState | null;
  handleSelectGroup: (group: AlertGroupItem) => void;
  toggleNewModal: () => void;
  handleAddDefinitionsToGroup: (
    group: AlertGroupItem | AlertGroupState
  ) => void;
  handleDeleteGroup: (group: any) => void;
  toggleGearDropdown: (e: React.MouseEvent) => void;
  isDropdownOpen: boolean;
  handleRename: (group: any) => void;
  handleDuplicate: (group: any) => void;
  selectedDefinition: AlertDefinition | null;
  handleSelectDefinition: (definition: AlertDefinition) => void;
  handleRemoveDefinition: (definitionId: number) => void;
  allAlertDefinitions?: AlertDefinition[];
  loadAvailableDefinitions: (group: AlertGroupItem | AlertGroupState) => void;
  setShowAddDefinitionModal: (show: boolean) => void;
  errorMessage: string;
  successMessage: string;
  notifications?: AlertNotification[];
  onNotificationsChange?: (
    groupId: number,
    notifications: AlertNotification[]
  ) => void;
  onManageNotifications?: () => void;
  onRetry?: () => void;
}

export const ManageAlertGroupsModal: React.FC<ManageAlertGroupsModalProps> = ({
  alertGroups,
  selectedGroup,
  handleSelectGroup,
  toggleNewModal,
  // handleAddDefinitionsToGroup,
  handleDeleteGroup,
  toggleGearDropdown,
  isDropdownOpen,
  handleRename,
  handleDuplicate,
  selectedDefinition,
  handleSelectDefinition,
  handleRemoveDefinition,
  allAlertDefinitions,
  loadAvailableDefinitions,
  setShowAddDefinitionModal,
  errorMessage,
  successMessage,
  notifications,
  onNotificationsChange,
  onRetry,
  // onManageNotifications
}) => {
  // Filter out deleted groups
  const filteredAlertGroups = alertGroups.filter(
    (group) => !group.AlertGroup._deleted
  );

  return (
    <div
      className="modal-body"
      data-qa="modal-body"
      style={{ maxHeight: "702.5px" }}
    >
      <div className="row">
        <div className="col-md-12 col-lg-12">
          <p className="alert alert-info">
            <span>
              You can manage alert groups for each service in this dialog. View
              the list of alert groups and the alert definitions configured in
              them. You can also add/remove alert definitions, and pick
              notification for that alert group.
            </span>
          </p>
        </div>
      </div>

      {/* Display success and error messages prominently at the top */}
      {errorMessage && (
        <div className="row">
          <div className="col-md-12 col-lg-12">
            <div
              className="alert alert-danger"
              style={{ marginBottom: "15px", fontWeight: "bold" }}
            >
              <span>{errorMessage}</span>
              {onRetry && <button className="btn btn-outline-danger ms-3" onClick={onRetry}>Retry</button>}
            </div>
          </div>
        </div>
      )}

      {successMessage && (
        <div className="row">
          <div className="col-md-12 col-lg-12">
            <div
              className="alert alert-success"
              style={{ marginBottom: "15px", fontWeight: "bold" }}
            >
              {successMessage}
            </div>
          </div>
        </div>
      )}

      <div className="row manage-configuration-group-content">
        <div className="col-md-4 col-lg-4">
          <span>&nbsp;</span>
          <select
            className="form-control group-select select-group-box"
            size={10}
            style={{ height: "300px" }}
            onChange={(e) => {
              const select = e.target as HTMLSelectElement;
              const selectedIndex = select.selectedIndex;
              if (selectedIndex >= 0 && filteredAlertGroups[selectedIndex]) {
                handleSelectGroup(filteredAlertGroups[selectedIndex]);
              }
            }}
          >
            {filteredAlertGroups && filteredAlertGroups.length > 0 ? (
              filteredAlertGroups.map((group) => {
                const definitionCount = group.AlertGroup.definitions
                  ? group.AlertGroup.definitions.length
                  : 0;
                const currentGroupId = getGroupId(selectedGroup);
                return (
                  <option
                    key={group.AlertGroup.id}
                    value={group.AlertGroup.id}
                    selected={currentGroupId === group.AlertGroup.id}
                  >
                    {group.AlertGroup.name}{" "}
                    {group.AlertGroup.default ? "Default" : ""} (
                    {definitionCount})
                  </option>
                );
              })
            ) : (
              <option value="">No alert groups available</option>
            )}
          </select>
          <div className="btn-toolbar pull-right">
            <button
              rel="button-info"
              className="btn btn-default add-group-button"
              title="Create Alert Group"
              onClick={toggleNewModal}
            >
              <FontAwesomeIcon icon={faPlus} />
            </button>
            <button
              rel="button-info"
              className="btn btn-default remove-group-button"
              title={
                isDefaultGroup(selectedGroup)
                  ? "Cannot delete default alert group"
                  : "Delete Alert Group"
              }
              disabled={!selectedGroup || isDefaultGroup(selectedGroup)}
              onClick={() => {
                if (selectedGroup) {
                  const groupToDelete = filteredAlertGroups.find(
                    (g) => getGroupId(selectedGroup) === g.AlertGroup.id
                  );
                  if (groupToDelete) {
                    handleDeleteGroup(groupToDelete);
                  }
                }
              }}
            >
              <FontAwesomeIcon icon={faMinus} />
            </button>
            <div className="btn-group">
              <button
                className={`btn btn-default dropdown-toggle actions-group-button ${
                  isDropdownOpen ? "show" : ""
                }`}
                data-bs-toggle="dropdown"
                aria-expanded={isDropdownOpen}
                onClick={(e) => {
                  e.stopPropagation();
                  toggleGearDropdown(e);
                }}
              >
                <FontAwesomeIcon icon={faGear} />
                &nbsp;<span className="caret"></span>
              </button>
              {isDropdownOpen && (
                <ul
                  className="dropdown-menu dropdown-menu-end"
                  style={{
                    display: "block",
                    position: "absolute",
                    right: 0,
                    top: "100%",
                    marginTop: "2px",
                    zIndex: 9999,
                  }}
                >
                  <li
                    className={isDefaultGroup(selectedGroup) ? "disabled" : ""}
                  >
                    <a
                      className="dropdown-item"
                      href="#"
                      rel="button-info-dropdown"
                      title="Rename Alert Group"
                      onClick={(e) => {
                        e.preventDefault();
                        if (selectedGroup && !isDefaultGroup(selectedGroup)) {
                          handleRename(selectedGroup);
                          toggleGearDropdown(e);
                        }
                      }}
                    >
                      <span>Rename</span>
                    </a>
                  </li>
                  <li className={!selectedGroup ? "disabled" : ""}>
                    <a
                      className="dropdown-item"
                      href="#"
                      rel="button-info-dropdown"
                      title="Duplicate Alert Group"
                      onClick={(e) => {
                        e.preventDefault();
                        if (selectedGroup) {
                          handleDuplicate(selectedGroup);
                          toggleGearDropdown(e);
                        }
                      }}
                    >
                      <span>Duplicate</span>
                    </a>
                  </li>
                  {/* <li className={!selectedGroup ? "disabled" : ""}>
                    <a 
                      className="dropdown-item"
                      href="#" 
                      rel="button-info-dropdown" 
                      title="Add Definitions to Group"
                      onClick={(e) => {
                        e.preventDefault();
                        if (selectedGroup) {
                          handleAddDefinitionsToGroup(selectedGroup);
                          toggleGearDropdown(e);
                        }
                      }}
                    >
                      <span>Add Definitions to Group</span>
                    </a>
                  </li> */}
                </ul>
              )}
            </div>
          </div>
        </div>

        <div className="col-md-8 col-lg-8">
          <span>&nbsp;</span>
          <select
            className="form-control group-select pull-right select-definiton-box"
            multiple
            style={{ height: "300px" }}
          >
            {selectedGroup &&
              getDefinitions(selectedGroup).map((defRef, index) => {
                const defId = typeof defRef === "number" ? defRef : defRef.id;
                const definition = allAlertDefinitions?.find(
                  (d) => d.id === defId
                );
                if (!definition) return null;

                return (
                  <option
                    key={index}
                    value={definition.id}
                    selected={selectedDefinition?.id === definition.id}
                    onClick={() => handleSelectDefinition(definition)}
                  >
                    {definition.label || definition.name}
                  </option>
                );
              })}
          </select>
          <div className="button-toolbar">
            <div className="pull-right">
              <a
                rel="button-info"
                className={`btn btn-default ${
                  isDefaultGroup(selectedGroup) ? "disabled" : ""
                }`}
                title={
                  isDefaultGroup(selectedGroup)
                    ? "Cannot modify default alert group"
                    : "Add Alert Definition"
                }
                onClick={() => {
                  if (!isDefaultGroup(selectedGroup) && selectedGroup) {
                    loadAvailableDefinitions(selectedGroup);
                    setShowAddDefinitionModal(true);
                  }
                }}
              >
                <FontAwesomeIcon icon={faPlus} />
              </a>
              <a
                rel="button-info"
                className={`btn btn-default ${
                  !selectedDefinition || isDefaultGroup(selectedGroup)
                    ? "disabled"
                    : ""
                }`}
                title={
                  isDefaultGroup(selectedGroup)
                    ? "Cannot modify default alert group"
                    : "Remove Alert Definition"
                }
                onClick={() => {
                  if (selectedDefinition && !isDefaultGroup(selectedGroup)) {
                    handleRemoveDefinition(selectedDefinition.id);
                  }
                }}
              >
                <FontAwesomeIcon icon={faMinus} />
              </a>
            </div>
            <div className="clearfix"></div>
          </div>
          <div className="notification-editable-list">
            <span>
              <FontAwesomeIcon icon={faEnvelope} />
            </span>
            &nbsp;<span>Notifications</span>
            <EditableList
              items={getGroupNotifications(selectedGroup, notifications)}
              resources={notifications || []}
              name={
                selectedGroup
                  ? "AlertGroup" in selectedGroup
                    ? selectedGroup.AlertGroup.name
                    : selectedGroup.name
                  : ""
              }
              isCaseSensitive={false}
              onItemsChange={(updatedNotifications) => {
                if (selectedGroup && onNotificationsChange) {
                  const groupId = getGroupId(selectedGroup);
                  if (groupId) {
                    onNotificationsChange(groupId, updatedNotifications);
                  }
                }
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

interface AddDefinitionsModalProps {
  selectedGroup: AlertGroupItem | AlertGroupState | null;
  availableDefinitions: AlertDefinition[];
  selectedDefinitions: number[];
  setSelectedDefinitions: React.Dispatch<React.SetStateAction<number[]>>;
  filteredComponent: string;
  setFilteredComponent: (component: string) => void;
  filteredService: string;
  setFilteredService: (service: string) => void;
  modalError: string;
  modalSuccess: string;
}

export const AddDefinitionsModal: React.FC<AddDefinitionsModalProps> = ({
  selectedGroup,
  availableDefinitions,
  selectedDefinitions,
  setSelectedDefinitions,
  filteredComponent,
  setFilteredComponent,
  filteredService,
  setFilteredService,
  modalError,
  modalSuccess,
}) => {
  // Get unique service and component names for filters
  const serviceNames = [
    ...new Set(availableDefinitions.map((def) => def.service_name)),
  ].sort();
  const componentNames = [
    ...new Set(availableDefinitions.map((def) => def.component_name || "N/A")),
  ]
    .filter((c) => c)
    .sort();

  // Filter definitions based on selected filters
  const filteredDefinitions = availableDefinitions.filter((def) => {
    const serviceMatch =
      !filteredService || def.service_name === filteredService;
    const componentMatch =
      !filteredComponent || (def.component_name || "N/A") === filteredComponent;
    return serviceMatch && componentMatch;
  });

  const groupName = selectedGroup
    ? "AlertGroup" in selectedGroup
      ? selectedGroup.AlertGroup.name
      : selectedGroup.name
    : "";

  return (
    <div
      className="modal-body"
      data-qa="modal-body"
      style={{ maxHeight: "702.5px" }}
    >
      <form className="form-horizontal mbm" autoComplete="off">
        <div className="override-controls">
          <div className="alert alert-info">
            Select alert definitions to be added to this "{groupName}" Alert
            Group.
          </div>

          {/* Display success and error messages prominently */}
          {modalError && (
            <div
              className="alert alert-danger"
              style={{ marginBottom: "15px", fontWeight: "bold" }}
            >
              {modalError}
            </div>
          )}

          {modalSuccess && (
            <div
              className="alert alert-success"
              style={{ marginBottom: "15px", fontWeight: "bold" }}
            >
              {modalSuccess}
            </div>
          )}

          <div className="row">
            <div className="col-md-8">
              <a href="#">
                {selectedDefinitions.length} out of{" "}
                {availableDefinitions.length} alert definitions selected
              </a>
            </div>
            <div className="col-md-2" id="component-dropdown-div">
              {/* Component dropdown */}
              <Select
                value={
                  filteredComponent
                    ? { value: filteredComponent, label: filteredComponent }
                    : null
                }
                onChange={(selectedOption) => {
                  setFilteredComponent(
                    selectedOption ? selectedOption.value : ""
                  );
                }}
                options={[
                  { value: "", label: "All Components" },
                  ...componentNames.map((component) => ({
                    value: component,
                    label: component,
                  })),
                ]}
                placeholder="Component"
                isClearable
                isSearchable
                className="react-select-container"
                classNamePrefix="react-select"
                styles={{
                  container: (provided) => ({
                    ...provided,
                    minWidth: "120px",
                    fontSize: "12px",
                  }),
                  control: (provided) => ({
                    ...provided,
                    minHeight: "30px",
                    fontSize: "12px",
                  }),
                }}
              />
            </div>
            <div className="col-md-2" id="filter-dropdown-div">
              {/* Service dropdown */}
              <Select
                value={
                  filteredService
                    ? { value: filteredService, label: filteredService }
                    : null
                }
                onChange={(selectedOption) => {
                  setFilteredService(
                    selectedOption ? selectedOption.value : ""
                  );
                }}
                options={[
                  { value: "", label: "All Services" },
                  ...serviceNames.map((service) => ({
                    value: service,
                    label: service,
                  })),
                ]}
                placeholder="Service"
                isClearable
                isSearchable
                className="react-select-container w-100"
                classNamePrefix="react-select"
                styles={{
                  container: (provided) => ({
                    ...provided,
                    minWidth: "120px",
                    fontSize: "12px",
                  }),
                  control: (provided) => ({
                    ...provided,
                    minHeight: "30px",
                    fontSize: "12px",
                  }),
                }}
              />
            </div>
          </div>

          <table className="table table-hover">
            <thead>
              <tr>
                <th style={{ width: "35%" }}>Alert Definition</th>
                <th style={{ width: "25%" }}>Service</th>
                <th style={{ width: "30%" }}>Component</th>
                <th style={{ width: "10%" }}>
                  <div className="checkbox">
                    <input
                      type="checkbox"
                      id="select-all-alert-definitions"
                      checked={
                        filteredDefinitions.length > 0 &&
                        selectedDefinitions.length ===
                          filteredDefinitions.length
                      }
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedDefinitions(
                            filteredDefinitions.map((def) => def.id)
                          );
                        } else {
                          setSelectedDefinitions([]);
                        }
                      }}
                    />
                    <label htmlFor="select-all-alert-definitions" className="checkbox-label"></label>
                  </div>
                </th>
              </tr>
            </thead>
          </table>

          <div
            className="hosts-table-container"
            style={{ maxHeight: "400px", overflowY: "auto" }}
          >
            <table className="table table-hover">
              <tbody>
                {filteredDefinitions.map((def) => (
                  <tr
                    key={def.id}
                    className=""
                    onClick={() => {
                      if (selectedDefinitions.includes(def.id)) {
                        setSelectedDefinitions(
                          selectedDefinitions.filter((id) => id !== def.id)
                        );
                      } else {
                        setSelectedDefinitions([
                          ...selectedDefinitions,
                          def.id,
                        ]);
                      }
                    }}
                    style={{ cursor: "pointer" }}
                  >
                    <td style={{ width: "35%" }}>{def.label || def.name}</td>
                    <td style={{ width: "25%" }}>{def.service_name}</td>
                    <td style={{ width: "30%" }}>{def.component_name || ""}</td>
                    <td style={{ width: "10%" }}>
                      <div className="checkbox">
                        <input
                          type="checkbox"
                          id={`alert-definition-${def.id}`}
                          checked={selectedDefinitions.includes(def.id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedDefinitions([
                                ...selectedDefinitions,
                                def.id,
                              ]);
                            } else {
                              setSelectedDefinitions(
                                selectedDefinitions.filter(
                                  (id) => id !== def.id
                                )
                              );
                            }
                          }}
                          onClick={(e) => e.stopPropagation()}
                        />
                        <label htmlFor={`alert-definition-${def.id}`} className="checkbox-label"></label>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </form>
    </div>
  );
};

interface ManageAlertSettingsModalProps {
  alertCheckCount: string;
  setAlertCheckCount: (count: string) => void;
  setIsSaveDisabled: (disabled: boolean) => void;
  modalError: string;
  modalSuccess: string;
}

export const ManageAlertSettingsModal: React.FC<
  ManageAlertSettingsModalProps
> = ({
  alertCheckCount,
  setAlertCheckCount,
  setIsSaveDisabled,
  modalError,
  modalSuccess,
}) => {
  const [validationError, setValidationError] = React.useState<string>("");

  const validateInput = (value: string): boolean => {
    const error = validateRepeatTolerance(value);
    setValidationError(error || "");
    return error === null;
  };

  const handleInputChange = (value: string) => {
    const isValid = validateInput(value);
    setAlertCheckCount(value);
    setIsSaveDisabled(!isValid);
  };

  return (
    <div className="modal-body">
      <div className="mt-1">
        <h3>Alert Check Counts</h3>
        <div className="fs-12">
          Set the number of alert checks to perform before dispatching a
          notification. If during an alert check a state change occurs, Ambari
          will attempt to check this number of times before dispatching a
          notification. Increase this number if your environment experiences
          transient issues resulting in false alerts.
        </div>
      </div>

      {modalError && <div className="alert alert-danger">{modalError}</div>}
      {modalSuccess && (
        <div className="alert alert-success">{modalSuccess}</div>
      )}

      <div className="form-group mt-3">
        <label htmlFor="alertCheckCount">Check Count:</label>
        <input
          type="text"
          className={`form-control ${validationError ? "is-invalid" : ""}`}
          id="alertCheckCount"
          value={alertCheckCount}
          onChange={(e) => handleInputChange(e.target.value)}
          placeholder="Enter check count (1-99 or DEBUG)"
          style={{ width: "200px" }}
        />
        {validationError && (
          <div className="text-danger mt-1">{validationError}</div>
        )}
        <small className="form-text text-muted mt-1">
          Enter a number between 1 and 99, or 'DEBUG' for debug mode.
        </small>
      </div>
    </div>
  );
};

interface DeleteConfirmationModalProps {
  groupToDelete: AlertGroupItem | AlertGroupState | null;
}

export const DeleteConfirmationModal: React.FC<
  DeleteConfirmationModalProps
> = ({ groupToDelete }) => {
  const groupName = groupToDelete
    ? "AlertGroup" in groupToDelete
      ? groupToDelete.AlertGroup.name
      : groupToDelete.name
    : "";

  return (
    <div className="modal-body">
      <p>Are you sure you want to delete the alert group "{groupName}"?</p>
      <p>This action cannot be undone.</p>
    </div>
  );
};

interface RenameGroupModalProps {
  currentName: string;
  newName: string;
  setNewName: (name: string) => void;
  modalError: string;
  modalSuccess: string;
}

export const RenameGroupModal: React.FC<RenameGroupModalProps> = ({
  currentName,
  newName,
  setNewName,
  modalError,
  modalSuccess,
}) => {
  return (
    <div className="modal-body">
      <div className="form-group">
        <label htmlFor="newName">New Name</label>
        <input
          type="text"
          className="form-control"
          id="newName"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder={currentName}
        />
      </div>
      {modalError && (
        <div className="alert alert-danger mt-3" role="alert">
          {modalError}
        </div>
      )}
      {modalSuccess && (
        <div className="alert alert-success mt-3" role="alert">
          {modalSuccess}
        </div>
      )}
    </div>
  );
};

interface DuplicateGroupModalProps {
  currentName: string;
  newName: string;
  setNewName: (name: string) => void;
  modalError: string;
  modalSuccess: string;
}

export const DuplicateGroupModal: React.FC<DuplicateGroupModalProps> = ({
  currentName,
  newName,
  setNewName,
  modalError,
  modalSuccess,
}) => {
  return (
    <div className="modal-body">
      <div className="form-group">
        <label htmlFor="newName">New Name</label>
        <input
          type="text"
          className="form-control"
          id="newName"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder={`Copy of ${currentName}`}
        />
      </div>
      {modalError && (
        <div className="alert alert-danger mt-3" role="alert">
          {modalError}
        </div>
      )}
      {modalSuccess && (
        <div className="alert alert-success mt-3" role="alert">
          {modalSuccess}
        </div>
      )}
    </div>
  );
};

interface ResultsModalProps {
  newCount: number;
  updatedCount: number;
  removedCount: number;
}

export const ResultsModal: React.FC<ResultsModalProps> = ({
  newCount,
  updatedCount,
  removedCount,
}) => {
  return (
    <div className="modal-body">
      <p>
        New Alert Groups - <strong>{newCount}</strong>
      </p>
      <p>
        Updated Alert Groups - <strong>{updatedCount}</strong>
      </p>
      <p>
        Removed Alert Groups - <strong>{removedCount}</strong>
      </p>
    </div>
  );
};
