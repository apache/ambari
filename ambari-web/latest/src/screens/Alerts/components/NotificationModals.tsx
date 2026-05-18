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

import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faMinus, faGear } from '@fortawesome/free-solid-svg-icons';
import { AlertNotification, AlertTarget } from '../types';

interface ManageNotificationsModalProps {
  notifications: AlertNotification[];
  selectedNotification: AlertNotification | null;
  handleSelectNotification: (notification: AlertNotification) => void;
  handleDeleteModalShow: (notification: AlertNotification) => void;
  toggleGearDropdown: (e: React.MouseEvent) => void;
  isDropdownOpen: boolean;
  handleEdit: (notification: AlertNotification) => void;
  handleDuplicate: (notification: AlertNotification) => void;
  handleToggleEnabled: (notification: AlertNotification) => void;
  handleCreateNotification: () => void;
  errorMessage: string;
  successMessage: string;
  setErrorMessage: (message: string) => void;
  setSuccessMessage: (message: string) => void;
  onFormValidityChange?: (isValid: boolean) => void;
  alertGroups?: { id: number; name: string; default?: boolean }[];
}

export const ManageNotificationsModal: React.FC<ManageNotificationsModalProps> = ({
  notifications,
  selectedNotification,
  handleSelectNotification,
  handleDeleteModalShow,
  toggleGearDropdown,
  isDropdownOpen,
  handleEdit,
  handleDuplicate,
  handleToggleEnabled: parentHandleToggleEnabled,
  handleCreateNotification,
  errorMessage,
  successMessage,
  alertGroups = [],
}) => {
  // Helper function to safely display groups
  const displayGroups = (target: AlertTarget): string => {
    if (!target) return '';
    if (target.global) return 'All';

    // If groups is empty or undefined but global is false
    if (!target.groups || target.groups.length === 0) return 'None selected';

    // Map group IDs to group names
    const groupNames = target.groups
      .map(groupId => alertGroups.find(g => g.id === groupId))
      .filter((group): group is NonNullable<typeof group> => group !== undefined)
      .map(group => group.default ? `${group.name} Default` : group.name);

    // If we couldn't resolve any group names, fall back to the generic message
    if (groupNames.length === 0) {
      return 'Custom groups selected';
    }
    return groupNames.join(', ');
  };

  // Helper function to safely display severities
  const displaySeverities = (target: AlertTarget): string => {
    if (!target) return '';
    if (!target.alert_states || target.alert_states.length === 0) return 'All';
    return Array.isArray(target.alert_states) ? target.alert_states.join(', ') : String(target.alert_states);
  };

  // Helper function to safely display email recipients
  const displayRecipients = (target: AlertTarget): string => {
    if (!target) return '';
    const RECIPIENTS_KEY = 'ambari.dispatch.recipients' as const;
    if (!target.properties || !(RECIPIENTS_KEY in target.properties)) return '';

    const recipients = target.properties[RECIPIENTS_KEY];
    if (Array.isArray(recipients)) {
      return recipients.join(', ');
    }
    return String(recipients);
  };

  // Add new state for tracking modified notifications
  const [modifiedNotifications, setModifiedNotifications] = useState<Record<string, AlertNotification>>({});
  const [_isLoading, _setIsLoading] = useState(false);

  // Local handleToggleEnabled to handle immediate UI updates
  const handleToggleEnabled = (notification: AlertNotification) => {
    if (!notification || !notification.AlertTarget) return;

    // Call the parent's toggle function to update the main notifications state immediately
    parentHandleToggleEnabled(notification);

    const updatedNotification = {
      ...notification,
      AlertTarget: {
        ...notification.AlertTarget,
        is_enabled: !notification.AlertTarget.is_enabled,
        _isModified: true
      }
    };

    // Update the modified notifications map
    setModifiedNotifications({
      ...modifiedNotifications,
      [String(notification.AlertTarget.id)]: updatedNotification
    });

    // If this is the selected notification, update it too
    if (selectedNotification && selectedNotification.AlertTarget.id === notification.AlertTarget.id) {
      handleSelectNotification(updatedNotification);
    }
  };


  return (
    <div className="modal-body">
      <style>
        {`
          .dropdown-item.disabled {
            color: #6c757d;
            pointer-events: none;
            background-color: transparent;
            opacity: 0.65;
          }
        `}
      </style>
      <div className="alert alert-info margin-bottom-5">
        <span>You can manage notification methods and recipients.</span>
      </div>

      {errorMessage && (
        <div className="alert alert-danger">
          {errorMessage}
        </div>
      )}

      {successMessage && (
        <div className="alert alert-success">
          {successMessage}
        </div>
      )}

      <div className="row manage-configuration-group-content" id="manage-alert-notification-content">
        <div className="col-md-12">
          <div className="row">
            <div className="col-md-4 notification-list">
              <span>&nbsp;</span>
              <select
                className="form-control group-select select-group-box"
                size={10}
                style={{ height: '400px' }}
                onChange={(e) => {
                  const selectedIndex = e.target.selectedIndex;
                  if (selectedIndex >= 0 && notifications[selectedIndex]) {
                    handleSelectNotification(notifications[selectedIndex]);
                  }
                }}
              >
                {notifications && notifications.length > 0 ? notifications.map(notification => (
                  <option
                    key={notification.AlertTarget.id}
                    value={notification.AlertTarget.id}
                    selected={selectedNotification?.AlertTarget?.id === notification.AlertTarget.id}
                  >
                    {notification.AlertTarget.name} {!notification.AlertTarget.is_enabled && '(Disabled)'}
                  </option>
                )) : (
                  <option value="">No notifications available</option>
                )}
              </select>
              <div className="btn-toolbar pull-right">
                <button
                  className="btn btn-default add-notification-button"
                  title="Create new Alert Notification"
                  onClick={handleCreateNotification}
                >
                  <FontAwesomeIcon icon={faPlus} />
                </button>
                <button
                  className="btn btn-default remove-notification-button"
                  title="Delete Alert Notification"
                  onClick={() => selectedNotification && handleDeleteModalShow(selectedNotification)}
                  disabled={!selectedNotification}
                >
                  <FontAwesomeIcon icon={faMinus} />
                </button>
                <div className="btn-group notification-actions-button dropup">
                  <button
                    className={`btn btn-default dropdown-toggle ${isDropdownOpen ? 'show' : ''}`}
                    data-bs-toggle="dropdown"
                    aria-expanded={isDropdownOpen}
                    title="More actions"
                    onClick={toggleGearDropdown}
                    disabled={!selectedNotification}
                  >
                    <FontAwesomeIcon icon={faGear} />&nbsp;<span className="caret"></span>
                  </button>
                  <ul className={`dropdown-menu dropdown-menu-end ${isDropdownOpen ? 'show' : ''}`}>
                    <li>
                      <a
                        className={`dropdown-item ${selectedNotification && !selectedNotification.AlertTarget?.is_enabled ? 'disabled' : ''}`}
                        href="#"
                        onClick={(e) => {
                          e.preventDefault();
                          if (selectedNotification && selectedNotification.AlertTarget?.is_enabled) handleEdit(selectedNotification);
                        }}
                      >
                        Edit
                      </a>
                    </li>
                    <li>
                      <a
                        className={`dropdown-item ${selectedNotification && !selectedNotification.AlertTarget?.is_enabled ? 'disabled' : ''}`}
                        href="#"
                        onClick={(e) => {
                          e.preventDefault();
                          if (selectedNotification && selectedNotification.AlertTarget?.is_enabled) handleDuplicate(selectedNotification);
                        }}
                      >
                        Duplicate
                      </a>
                    </li>
                    <li>
                      <a className="dropdown-item" href="#" onClick={(e) => {
                        e.preventDefault();
                        if (selectedNotification) handleToggleEnabled(selectedNotification);
                      }}>
                        {selectedNotification && selectedNotification.AlertTarget && selectedNotification.AlertTarget.is_enabled ? 'Disable' : 'Enable'}
                      </a>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
            <div className="col-md-8 notification-info">
              <span>&nbsp;</span>
              {selectedNotification ? (
                <div className="notification-details">
                  <div className="notification-detail-section">
                    <div className="row mt-3">
                      <div className="col-md-3 input-label">Name</div>
                      <div className="col-md-9 input-value">{selectedNotification.AlertTarget?.name}</div>
                    </div>
                    <div className="row mt-3">
                      <div className="col-md-3 input-label">Groups</div>
                      <div className="col-md-9 input-value">
                        {selectedNotification.AlertTarget && displayGroups(selectedNotification.AlertTarget)}
                      </div>
                    </div>
                    <div className="row mt-3">
                      <div className="col-md-3 input-label">Severities</div>
                      <div className="col-md-9 input-value">{selectedNotification.AlertTarget && displaySeverities(selectedNotification.AlertTarget)}</div>
                    </div>
                    <div className="row mt-3">
                      <div className="col-md-3 input-label">Type</div>
                      <div className="col-md-9 input-value">{selectedNotification.AlertTarget?.notification_type}</div>
                    </div>
                    {selectedNotification.AlertTarget?.notification_type === 'EMAIL' && (
                      <div className="row mt-3">
                        <div className="col-md-3 input-label">Recipients</div>
                        <div className="col-md-9 input-value">
                          {selectedNotification.AlertTarget && displayRecipients(selectedNotification.AlertTarget)}
                        </div>
                      </div>
                    )}
                    <div className="row mt-3">
                      <div className="col-md-3 input-label">Description</div>
                      <div className="col-md-9 input-value">{selectedNotification.AlertTarget?.description || 'No Description'}</div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="alert alert-info">
                  <p>Select a notification to view details or click the + button to create a new one.</p>
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="clearfix"></div>
        <div className="col-md-12 row">
          <div className="col-md-12 text-danger" id="manage-alert-notifications-error">
            &nbsp;
          </div>
        </div>
      </div>
    </div>
  );
};

interface DeleteNotificationModalProps {
  notification: AlertNotification | null;
}

export const DeleteNotificationModal: React.FC<DeleteNotificationModalProps> = ({
  notification
}) => {
  if (!notification) {
    return <div>No notification selected</div>;
  }

  return (
    <div className="modal-body">
      <p>
        Are you sure you want to delete the notification{' '}
        <strong>{notification.AlertTarget.name}</strong>?
      </p>
      <p>This action cannot be undone.</p>
    </div>
  );
};

interface NotificationFormModalProps {
  name: string;
  setName: (name: string) => void;
  description: string;
  setDescription: (description: string) => void;
  notificationType: string;
  setNotificationType: (type: string) => void;
  recipients: string;
  setRecipients: (recipients: string) => void;
  smtpServer: string;
  setSmtpServer: (server: string) => void;
  smtpPort: string;
  setSmtpPort: (port: string) => void;
  emailFrom: string;
  setEmailFrom: (from: string) => void;
  useAuthentication: boolean;
  setUseAuthentication: (use: boolean) => void;
  username: string;
  setUsername: (username: string) => void;
  password: string;
  setPassword: (password: string) => void;
  passwordConfirmation: string;
  setPasswordConfirmation: (password: string) => void;
  startTls: boolean;
  setStartTls: (startTls: boolean) => void;
  snmpVersion: string;
  setSnmpVersion: (version: string) => void;
  snmpCommunity: string;
  setSnmpCommunity: (community: string) => void;
  snmpHosts: string;
  setSnmpHosts: (hosts: string) => void;
  snmpPort: string;
  setSnmpPort: (port: string) => void;
  snmpOid: string;
  setSnmpOid: (oid: string) => void;
  scriptFilename: string;
  setScriptFilename: (filename: string) => void;
  scriptDispatchProperty: string;
  setScriptDispatchProperty: (property: string) => void;
  groupsOption: 'all' | 'custom';
  setGroupsOption: (option: 'all' | 'custom') => void;
  selectedGroups: number[];
  setSelectedGroups: (groups: number[]) => void;
  availableGroups: { id: number; name: string }[];
  selectedSeverities: string[];
  setSelectedSeverities: (severities: string[]) => void;
  isEnabled: boolean;
  setIsEnabled: (enabled: boolean) => void;
  customProperties: {name: string; value: string}[];
  setCustomProperties: (properties: {name: string; value: string}[]) => void;
  errorMessage: string;
  successMessage: string;
  setErrorMessage: (message: string) => void;
  setSuccessMessage: (message: string) => void;
}

export const NotificationFormModal: React.FC<NotificationFormModalProps> = ({
  name,
  setName,
  description,
  setDescription,
  notificationType,
  setNotificationType,
  recipients,
  setRecipients,
  smtpServer,
  setSmtpServer,
  smtpPort,
  setSmtpPort,
  emailFrom,
  setEmailFrom,
  useAuthentication,
  setUseAuthentication,
  username,
  setUsername,
  password,
  setPassword,
  passwordConfirmation,
  setPasswordConfirmation,
  startTls,
  setStartTls,
  snmpVersion,
  setSnmpVersion,
  snmpCommunity,
  setSnmpCommunity,
  snmpHosts,
  setSnmpHosts,
  snmpPort,
  setSnmpPort,
  snmpOid,
  setSnmpOid,
  scriptFilename,
  setScriptFilename,
  scriptDispatchProperty,
  setScriptDispatchProperty,
  groupsOption,
  setGroupsOption,
  selectedGroups,
  setSelectedGroups,
  availableGroups,
  selectedSeverities,
  setSelectedSeverities,
  customProperties,
  setCustomProperties,
  errorMessage,
  successMessage,
  setErrorMessage,
}) => {
  const [validationErrors, setValidationErrors] = useState<{[key: string]: string}>({});
  const [showAddPropertyModal, setShowAddPropertyModal] = useState(false);
  const [newPropertyName, setNewPropertyName] = useState('');
  const [newPropertyValue, setNewPropertyValue] = useState('');

  // Ember validator functions - exact replicas
  const isValidEmail = (value: string): boolean => {
    const emailRegex = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))$/i;
    return emailRegex.test(value);
  };

  const isValidInt = (value: string): boolean => {
    const intRegex = /^-?\d+$/;
    return intRegex.test(value);
  };

  const isValidAlertNotificationName = (value: string): boolean => {
    const configKeyRegex = /^[\s0-9a-z_\-]+$/i;
    return configKeyRegex.test(value);
  };

  const isHostname = (value: string): boolean => {
    const regex = /(?=^.{3,254}$)(^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*(\.[a-zA-Z]{1,62})$)/;
    return value === 'localhost' || regex.test(value);
  };

  const isValidFileName = (value: string): boolean => {
    const filenameRegex = /^[0-9a-zA-Z_-]+\.[a-zA-Z]+$/;
    return filenameRegex.test(value);
  };

  const isValidConfigKey = (value: string): boolean => {
    const configKeyRegex = /^\s*[0-9a-z_\-\.\/\*]+\s*$/i;
    return configKeyRegex.test(value);
  };


  // Validation function matching Ember's validation exactly
  const validateField = (fieldName: string, value: string): string => {
    switch (fieldName) {
      case 'name':
        // Ember nameValidation logic
        if (!value.trim()) {
          return 'Name is required';
        }
        if (!isValidAlertNotificationName(value)) {
          return 'Name contains invalid characters';
        }
        return '';

      case 'recipients':
        // Ember emailToValidation logic
        if (notificationType === 'EMAIL') {
          const emails = value.trim().split(',');
          const hasInvalidEmail = emails.some(email => email && !isValidEmail(email.trim()));
          if (hasInvalidEmail) {
            return 'Invalid email address';
          }
        }
        return '';

      case 'emailFrom':
        // Ember emailFromValidation logic
        if (value && !isValidEmail(value)) {
          return 'Invalid email address';
        }
        return '';

      case 'smtpPort':
        // Ember smtpPortValidation logic
        if (value && (!isValidInt(value) || parseInt(value, 10) < 0)) {
          return 'Port should be a valid integer';
        }
        return '';

      case 'username':
        // Ember smtpUsernameValidation logic
        if (notificationType === 'EMAIL' && useAuthentication && !value.trim()) {
          return 'Username is required';
        }
        return '';

      case 'password':
        // Ember smtpPasswordValidation logic
        if (notificationType === 'EMAIL' && useAuthentication && !value.trim()) {
          return 'Password is required';
        }
        return '';

      case 'passwordConfirmation':
        // Ember retypePasswordValidation logic
        if (password !== passwordConfirmation) {
          return 'Passwords do not match';
        }
        return '';

      case 'snmpHosts':
        // Ember hostsValidation logic
        if (!notificationType.includes('EMAIL') && notificationType !== 'Alert Script') {
          const hosts = value.trim().split(',');
          const hasInvalidHost = hosts.some(hostname => hostname && !isHostname(hostname.trim()));
          const isEmpty = value.trim() === '';
          if (hasInvalidHost || isEmpty) {
            return 'Invalid hostname';
          }
        }
        return '';

      case 'snmpPort':
        // Ember portValidation logic
        if (value && (!isValidInt(value) || parseInt(value, 10) < 0)) {
          return 'Port should be a valid integer';
        }
        return '';

      case 'scriptFilename':
        // Ember scriptFileNameValidation logic
        const trimmedValue = value.trim();
        if (trimmedValue && !isValidFileName(trimmedValue)) {
          return 'Invalid script filename';
        }
        return '';

      default:
        return '';
    }
  };

  // Real-time validation
  const handleFieldChange = (fieldName: string, value: string, setter: (value: string) => void) => {
    setter(value);
    const error = validateField(fieldName, value);
    setValidationErrors(prev => ({
      ...prev,
      [fieldName]: error
    }));
  };

  // Check if form is valid (no errors and all required fields filled)
  const isFormValid = (): boolean => {
    // Check if there are any validation errors
    const hasErrors = Object.values(validationErrors).some(error => error !== '');
    if (hasErrors) return false;

    // Check required fields based on current notification type
    if (!name.trim()) return false;

    if (notificationType === 'EMAIL') {
      if (!recipients.trim()) return false;
      if (useAuthentication) {
        if (!username.trim()) return false;
        if (!password.trim()) return false;
        if (password !== passwordConfirmation) return false;
      }
    } else if (notificationType === 'SNMP' || notificationType === 'Custom SNMP') {
      if (!snmpHosts.trim()) return false;
      if (!snmpPort.trim()) return false;
      if (!snmpCommunity.trim()) return false;
    } else if (notificationType === 'Alert Script') {
      if (!scriptFilename.trim()) return false;
    }

    // Check groups selection
    if (groupsOption === 'custom' && selectedGroups.length === 0) return false;

    // Check severities selection
    if (selectedSeverities.length === 0) return false;

    return true;
  };

  // Initial validation on component mount and when dependencies change
  React.useEffect(() => {
    // Validate all required fields initially
    const errors: {[key: string]: string} = {};
    
    // Always validate name
    errors.name = validateField('name', name);
    
    // Validate based on notification type
    if (notificationType === 'EMAIL') {
      errors.recipients = validateField('recipients', recipients);
      errors.emailFrom = validateField('emailFrom', emailFrom);
      errors.smtpPort = validateField('smtpPort', smtpPort);
      
      if (useAuthentication) {
        errors.username = validateField('username', username);
        errors.password = validateField('password', password);
        errors.passwordConfirmation = validateField('passwordConfirmation', passwordConfirmation);
      }
    } else if (notificationType === 'SNMP' || notificationType === 'Custom SNMP') {
      errors.snmpHosts = validateField('snmpHosts', snmpHosts);
      errors.snmpPort = validateField('snmpPort', snmpPort);
      errors.snmpCommunity = validateField('snmpCommunity', snmpCommunity);
    } else if (notificationType === 'Alert Script') {
      errors.scriptFilename = validateField('scriptFilename', scriptFilename);
    }

    // Validate groups selection
    if (groupsOption === 'custom' && selectedGroups.length === 0) {
      errors.groups = 'Please select at least one group or choose "All" groups';
    }

    // Validate severities selection
    if (selectedSeverities.length === 0) {
      errors.severities = 'Please select at least one severity level';
    }

    // Filter out empty errors
    const filteredErrors = Object.fromEntries(
      Object.entries(errors).filter(([, value]) => value !== '')
    );

    setValidationErrors(filteredErrors);
    
    // Notify parent about form validity change
    const formIsValid = isFormValid();
    if (setErrorMessage) {
      // Use setErrorMessage to communicate form validity to parent
      // If form is invalid, set a generic error message, otherwise clear it
      if (!formIsValid && Object.keys(filteredErrors).length > 0) {
      } else if (formIsValid) {
        setErrorMessage('');
      }
    }
  }, [name, notificationType, recipients, emailFrom, smtpPort, username, password, passwordConfirmation, 
      useAuthentication, snmpHosts, snmpPort, snmpCommunity, scriptFilename, groupsOption, 
      selectedGroups, selectedSeverities]);

  // Add custom property with proper validation
  const handleAddCustomProperty = () => {
    if (!newPropertyName.trim() || !newPropertyValue.trim()) {
      setErrorMessage('Property name and value are required');
      return;
    }

    // Check if property already exists
    if (customProperties.some(prop => prop.name === newPropertyName)) {
      setErrorMessage('Property with this name already exists');
      return;
    }

    // Validate property name using Ember's validation
    if (!isValidConfigKey(newPropertyName)) {
      setErrorMessage('Invalid property name format. Only letters, numbers, hyphens, underscores, dots, slashes and asterisks are allowed.');
      return;
    }

    setCustomProperties([...customProperties, { name: newPropertyName, value: newPropertyValue }]);
    setNewPropertyName('');
    setNewPropertyValue('');
    setShowAddPropertyModal(false);
    setErrorMessage('');
  };

  // Remove custom property
  const handleRemoveCustomProperty = (index: number) => {
    const newProps = customProperties.filter((_, i) => i !== index);
    setCustomProperties(newProps);
  };

  return (
    <div id="create-edit-alert-notification row">
      <form autoComplete="off" className="form-horizontal">
        {errorMessage && (
          <div className="alert alert-danger">
            {errorMessage}
          </div>
        )}
        {successMessage && (
          <div className="alert alert-success">
            {successMessage}
          </div>
        )}

        {/* alert-notification name */}
        <div className="form-group mt-3">
          <div className="row">
            <label className="control-label col-md-2" htmlFor="inputName">Name</label>
            <div className="col-md-10">
              <input
                type="text"
                className={`form-control ${validationErrors.name ? 'is-invalid' : ''}`}
                id="inputName"
                value={name}
                onChange={(e) => handleFieldChange('name', e.target.value, setName)}
              />
              {validationErrors.name && (
                <div className="text-danger mt-1">
                  {validationErrors.name}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* alert-notification groups */}
        <div className="form-group mt-3">
          <div className="row">
            <label className="control-label col-md-2" htmlFor="inputGroups">Groups</label>
            <div className="col-md-10">
              <div className="radio">
                <label>
                  <input
                    type="radio"
                    name="allGroups"
                    value="all"
                    checked={groupsOption === 'all'}
                    onChange={() => setGroupsOption('all')}
                  />
                  All
                </label>
              </div>
              <div className="radio">
                <label>
                  <input
                    type="radio"
                    name="allGroups"
                    value="custom"
                    checked={groupsOption === 'custom'}
                    onChange={() => setGroupsOption('custom')}
                  />
                  Custom
                </label>
              </div>
              <div>
                <select
                  multiple
                  id="inputGroups"
                  className="form-control"
                  disabled={groupsOption === 'all'}
                  value={selectedGroups.map(String)}
                  onChange={(e) => {
                    const select = e.target as HTMLSelectElement;
                    const selectedOptions = Array.from(select.selectedOptions).map(option => Number(option.value));
                    setSelectedGroups(selectedOptions);
                  }}
                >
                  {availableGroups.map(group => (
                    <option key={group.id} value={group.id}>
                      {group.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <a
                  href="#"
                  className={groupsOption === 'custom' && availableGroups.length > 0 ? '' : 'disabled'}
                  onClick={(e) => {
                    e.preventDefault();
                    if (groupsOption === 'custom') {
                      setSelectedGroups(availableGroups.map(group => group.id));
                    }
                  }}
                >
                  Select All
                </a> |{' '}
                <a
                  href="#"
                  className={groupsOption === 'custom' && selectedGroups.length > 0 ? '' : 'disabled'}
                  onClick={(e) => {
                    e.preventDefault();
                    if (groupsOption === 'custom') {
                      setSelectedGroups([]);
                    }
                  }}
                >
                  Clear All
                </a>
              </div>
              {validationErrors.groups && (
                <div className="help-block validation-block error-msg">
                  {validationErrors.groups}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* alert-notification severity */}
        <div className="form-group mt-3">
          <div className="row">
            <label className="control-label col-md-2">Severity Filter</label>
            <div className="col-md-10">
              <div>
                <select
                  multiple
                  id="inputSeverity"
                  className="form-control"
                  value={selectedSeverities}
                  onChange={(e) => {
                    const select = e.target as HTMLSelectElement;
                    const selectedOptions = Array.from(select.selectedOptions).map(option => option.value);
                    setSelectedSeverities(selectedOptions);
                  }}
                >
                  <option value="OK">OK</option>
                  <option value="WARNING">WARNING</option>
                  <option value="CRITICAL">CRITICAL</option>
                  <option value="UNKNOWN">UNKNOWN</option>
                </select>
              </div>
              <div>
                <a
                  href="#"
                  className={selectedSeverities.length === 4 ? 'disabled' : ''}
                  onClick={(e) => {
                    e.preventDefault();
                    setSelectedSeverities(['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
                  }}
                >
                  Select All
                </a> |{' '}
                <a
                  href="#"
                  className={selectedSeverities.length > 0 ? '' : 'disabled'}
                  onClick={(e) => {
                    e.preventDefault();
                    setSelectedSeverities([]);
                  }}
                >
                  Clear All
                </a>
              </div>
              {validationErrors.severities && (
                <div className="help-block validation-block error-msg">
                  {validationErrors.severities}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* alert-notification description */}
        <div className="form-group mt-3">
          <div className="row">
            <label className="control-label col-md-2" htmlFor="inputDescription">Description</label>
            <div className="col-md-10">
              <textarea
                id="inputDescription"
                rows={4}
                className="form-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* alert-notification method */}
        <div className="form-group mt-3">
          <div className="row">
            <label className="control-label col-md-2" htmlFor="inputMethod">Method</label>
            <div className="col-md-10">
              <select
                id="inputMethod"
                className="form-control"
                value={notificationType}
                onChange={(e) => setNotificationType(e.target.value)}
              >
                <option value="EMAIL">EMAIL</option>
                <option value="SNMP">SNMP</option>
                <option value="Custom SNMP">Custom SNMP</option>
                <option value="Alert Script">Alert Script</option>
              </select>
            </div>
          </div>
        </div>

        {/* alert-notification email */}
        {notificationType === 'EMAIL' && (
          <>
            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2" htmlFor="inputEmail">Email To</label>
                <div className="col-md-10">
                  <input
                    id="inputEmail"
                    type="text"
                    className={`form-control ${validationErrors.recipients ? 'is-invalid' : ''}`}
                    value={recipients}
                    onChange={(e) => handleFieldChange('recipients', e.target.value, setRecipients)}
                  />
                  {validationErrors.recipients && (
                    <div className="text-danger mt-1">
                      {validationErrors.recipients}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">SMTP Server</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className="form-control"
                    value={smtpServer}
                    onChange={(e) => setSmtpServer(e.target.value)}
                  />
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">SMTP Port</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className={`form-control ${validationErrors.smtpPort ? 'is-invalid' : ''}`}
                    value={smtpPort}
                    onChange={(e) => handleFieldChange('smtpPort', e.target.value, setSmtpPort)}
                  />
                  {validationErrors.smtpPort && (
                    <div className="text-danger mt-1">
                      {validationErrors.smtpPort}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Email From</label>
                <div className="col-md-10">
                  <input
                    type="email"
                    className={`form-control ${validationErrors.emailFrom ? 'is-invalid' : ''}`}
                    value={emailFrom}
                    onChange={(e) => handleFieldChange('emailFrom', e.target.value, setEmailFrom)}
                  />
                  {validationErrors.emailFrom && (
                    <div className="text-danger mt-1">
                      {validationErrors.emailFrom}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <div className="col-md-2"></div>
                <div className="col-md-10">
                  <div className="checkbox">
                    <input
                      type="checkbox"
                      id="inputUseAuthentication"
                      checked={useAuthentication}
                      onChange={(e) => setUseAuthentication(e.target.checked)}
                    />
                    <label htmlFor="inputUseAuthentication" className="checkbox-label">Use authentication</label>
                  </div>
                </div>
              </div>
            </div>

            {useAuthentication && (
              <>
                <div className="form-group mt-3">
                  <div className="row">
                    <label className="control-label col-md-2">Username</label>
                    <div className="col-md-10">
                      <input
                        type="text"
                        className={`form-control ${validationErrors.username ? 'is-invalid' : ''}`}
                        disabled={!useAuthentication}
                        value={username}
                        onChange={(e) => handleFieldChange('username', e.target.value, setUsername)}
                      />
                      {validationErrors.username && (
                        <div className="text-danger mt-1">
                          {validationErrors.username}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                <div className="form-group mt-3">
                  <div className="row">
                    <label className="control-label col-md-2">Password</label>
                    <div className="col-md-10">
                      <input
                        type="password"
                        className={`form-control ${validationErrors.password ? 'is-invalid' : ''}`}
                        disabled={!useAuthentication}
                        value={password}
                        onChange={(e) => handleFieldChange('password', e.target.value, setPassword)}
                      />
                      {validationErrors.password && (
                        <div className="text-danger mt-1">
                          {validationErrors.password}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                <div className="form-group mt-3">
                  <div className="row">
                    <label className="control-label col-md-2">Retype Password</label>
                    <div className="col-md-10">
                      <input
                        type="password"
                        className={`form-control ${validationErrors.passwordConfirmation ? 'is-invalid' : ''}`}
                        disabled={!useAuthentication}
                        value={passwordConfirmation}
                        onChange={(e) => handleFieldChange('passwordConfirmation', e.target.value, setPasswordConfirmation)}
                      />
                      {validationErrors.passwordConfirmation && (
                        <div className="text-danger mt-1">
                          {validationErrors.passwordConfirmation}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </>
            )}

            <div className="form-group mt-3">
              <div className="row">
                <div className="col-md-2"></div>
                <div className="col-md-10">
                  <div className="checkbox">
                    <input
                      type="checkbox"
                      id="inputSMTPSTARTTLS"
                      checked={startTls}
                      onChange={(e) => setStartTls(e.target.checked)}
                      disabled={!useAuthentication}
                    />
                    <label htmlFor="inputSMTPSTARTTLS" className="checkbox-label">Start TLS</label>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

        {/* alert-notification SNMP */}
        {(notificationType === 'SNMP' || notificationType === 'Custom SNMP') && (
          <>
            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Version</label>
                <div className="col-md-10">
                  <select
                    className="form-control"
                    value={snmpVersion}
                    onChange={(e) => setSnmpVersion(e.target.value)}
                  >
                    <option value="SNMPv1">SNMPv1</option>
                    <option value="SNMPv2c">SNMPv2c</option>
                  </select>
                </div>
              </div>
            </div>

            {notificationType === 'Custom SNMP' && (
              <div className="form-group mt-3">
                <div className="row">
                  <label className="control-label col-md-2">OIDs</label>
                  <div className="col-md-10">
                    <input
                      type="text"
                      className="form-control"
                      value={snmpOid}
                      onChange={(e) => setSnmpOid(e.target.value)}
                    />
                  </div>
                </div>
              </div>
            )}

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Community</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className={`form-control ${validationErrors.snmpCommunity ? 'is-invalid' : ''}`}
                    value={snmpCommunity}
                    onChange={(e) => handleFieldChange('snmpCommunity', e.target.value, setSnmpCommunity)}
                  />
                  {validationErrors.snmpCommunity && (
                    <div className="text-danger mt-1">
                      {validationErrors.snmpCommunity}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Hosts</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className={`form-control ${validationErrors.snmpHosts ? 'is-invalid' : ''}`}
                    value={snmpHosts}
                    onChange={(e) => handleFieldChange('snmpHosts', e.target.value, setSnmpHosts)}
                  />
                  {validationErrors.snmpHosts && (
                    <div className="text-danger mt-1">
                      {validationErrors.snmpHosts}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Port</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className={`form-control ${validationErrors.snmpPort ? 'is-invalid' : ''}`}
                    value={snmpPort}
                    onChange={(e) => handleFieldChange('snmpPort', e.target.value, setSnmpPort)}
                  />
                  {validationErrors.snmpPort && (
                    <div className="text-danger mt-1">
                      {validationErrors.snmpPort}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </>
        )}

        {/* alert-notification Alert Script */}
        {notificationType === 'Alert Script' && (
          <>
            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Script Filename</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className={`form-control ${validationErrors.scriptFilename ? 'is-invalid' : ''}`}
                    value={scriptFilename}
                    onChange={(e) => handleFieldChange('scriptFilename', e.target.value, setScriptFilename)}
                  />
                  {validationErrors.scriptFilename && (
                    <div className="text-danger mt-1">
                      {validationErrors.scriptFilename}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="form-group mt-3">
              <div className="row">
                <label className="control-label col-md-2">Script Dispatch Property</label>
                <div className="col-md-10">
                  <input
                    type="text"
                    className="form-control"
                    value={scriptDispatchProperty}
                    onChange={(e) => setScriptDispatchProperty(e.target.value)}
                  />
                </div>
              </div>
            </div>
          </>
        )}

        {/* alert-notification custom properties */}
        {customProperties.length > 0 && customProperties.map((customProperty, index) => (
          <div key={index} className="form-group mt-3">
            <div className="row align-items-center">
              <label className="control-label col-md-2">{customProperty.name}</label>
              <div className="col-md-9">
                <input
                  type="text"
                  className="form-control"
                  value={customProperty.value}
                  onChange={(e) => {
                    const newProps = [...customProperties];
                    newProps[index].value = e.target.value;
                    setCustomProperties(newProps);
                  }}
                />
              </div>
              <div className="col-md-1">
                <a
                  href="#"
                  className="btn-sm"
                  onClick={(e) => {
                    e.preventDefault();
                    handleRemoveCustomProperty(index);
                  }}
                >
                  <FontAwesomeIcon icon={faMinus} className='text-danger' />
                </a>
              </div>
            </div>
          </div>
        ))}

        <a
          href="#"
          className="add-custom-property"
          onClick={(e) => {
            e.preventDefault();
            setShowAddPropertyModal(true);
          }}
        >
          Add Property ...
        </a>

        {/* Add Property Modal */}
        {showAddPropertyModal && (
          <div className="modal fade show" style={{ display: 'block', position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1050 }}>
            <div className="modal-dialog modal-sm" style={{ margin: '30px auto' }}>
              <div className="modal-content">
                <div className="modal-header">
                  <h2 className="modal-title">Add Custom Property</h2>
                  <button
                    type="button"
                    className="close"
                    onClick={() => setShowAddPropertyModal(false)}
                  >
                    <span>&times;</span>
                  </button>
                </div>
                <div className="modal-body">
                  <div className="alert alert-warning">
                    <strong>Note:</strong> Property name can only contain letters, numbers or . -_* characters
                  </div>
                  <div className="form-group mt-3">
                    <label>Property Name</label>
                    <input
                      type="text"
                      className="form-control mt-1"
                      value={newPropertyName}
                      onChange={(e) => setNewPropertyName(e.target.value)}
                    />
                  </div>
                  <div className="form-group mt-3">
                    <label>Property Value</label>
                    <input
                      type="text"
                      className="form-control mt-1"
                      value={newPropertyValue}
                      onChange={(e) => setNewPropertyValue(e.target.value)}
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-default"
                    onClick={() => setShowAddPropertyModal(false)}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={handleAddCustomProperty}
                    disabled={!newPropertyName.trim() || !newPropertyValue.trim()}
                  >
                    Add
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </form>
    </div>
  );
};

interface AddNotificationsToGroupModalProps {
  availableNotifications: AlertNotification[];
  selectedNotifications: number[];
  handleToggleNotification: (id: number) => void;
  errorMessage: string;
}

export const AddNotificationsToGroupModal: React.FC<AddNotificationsToGroupModalProps> = ({
  availableNotifications,
  selectedNotifications,
  handleToggleNotification,
  errorMessage
}) => {
  const handleToggle = (id: number) => {
    handleToggleNotification(id);
  };

  return (
    <div className="modal-body">
      <div className="alert alert-info">
        <span>Select notifications to add to this alert group.</span>
      </div>
      <div className="notification-selection">
        {availableNotifications.length > 0 ? (
          availableNotifications.map(notification => {
            const targetId = notification.AlertTarget?.id;
            if (!targetId) return null;

            return (
              <div key={targetId} className="form-check">
                <input
                  type="checkbox"
                  className="form-check-input"
                  id={`notification-${targetId}`}
                  checked={selectedNotifications.includes(targetId)}
                  onChange={() => handleToggle(targetId)}
                />
                <label className="form-check-label" htmlFor={`notification-${targetId}`}>
                  {notification.AlertTarget?.name} ({notification.AlertTarget?.notification_type})
                </label>
              </div>
            );
          })
        ) : (
          <p>No notifications available. Create notifications first.</p>
        )}
      </div>
      {errorMessage && <div className="text-danger mt-3">{errorMessage}</div>}
    </div>
  );
};
