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

import React, { useState, useContext, useEffect } from 'react';
import Modal from '../../components/Modal';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEnvelope, faGear, faPlus, faTh } from '@fortawesome/free-solid-svg-icons';
import { AlertGroupItem, AlertDefinition, AlertNotification, AlertGroupState } from './types';
import { useNavigate, useParams } from 'react-router-dom';
import { AppContext } from '../../store/context';
import { 
  ManageAlertGroupsModal, 
  ManageAlertSettingsModal, 
  NewGroupModal, 
  DeleteConfirmationModal, 
  RenameGroupModal, 
  DuplicateGroupModal,
  AddDefinitionsModal,
  ResultsModal
} from './components/AlertGroupModals';
import {
  ManageNotificationsModal,
  DeleteNotificationModal,
  NotificationFormModal
} from './components/NotificationModals';
import { AlertsApi } from '../../api/alertsApi';
import useAuthorizationPolicy from '../../hooks/useAuthorizationPolicy';
import ClusterApi from '../../api/clusterApi';
import ConfigsApi from '../../api/configsApi';
import { ambariApi } from '../../api/config/axiosConfig';
import { getAlertActionPolicy } from '../../Utils/alertPolicy';
import {
  executeAlertGroupSave,
  planAlertGroupSave,
  reconcileAlertGroupSave,
  reconcileAlertGroupSaveWithServer,
  validateAlertGroupName,
} from '../../Utils/alertGroups';
import {
  buildAlertNotificationPayload,
  notificationBuiltInKeys,
  notificationTypeToUi,
  validateAlertNotificationForm,
  type AlertNotificationForm,
} from '../../Utils/alertNotifications';
import { validateRepeatTolerance } from '../../Utils/alertDefinitions';

interface ActionsButtonProps {
  alertGroups: AlertGroupItem[];
  allAlertDefinitions?: AlertDefinition[];
  onModalStateChange?: (isOpen: boolean) => void;
}

export const ActionsButton: React.FC<ActionsButtonProps> = ({ 
  alertGroups: initialAlertGroups, 
  allAlertDefinitions,
  onModalStateChange}) => {
  // Context and URL params
  const { clusterName: urlClusterName } = useParams<{ clusterName: string }>();
  const { clusterName: contextClusterName, supports } = useContext(AppContext);
  const navigate = useNavigate();
  
  // Authorization hooks - implementing Ember.js alert authorization patterns
  const { isAuthorized } = useAuthorizationPolicy();
  
  // Check specific authorizations for alert operations
  const canManageAlerts = isAuthorized(
    'SERVICE.TOGGLE_ALERTS, CLUSTER.TOGGLE_ALERTS',
  );
  const canManageNotifications = isAuthorized('CLUSTER.MANAGE_ALERT_NOTIFICATIONS');
  const actionPolicy = getAlertActionPolicy(
    Boolean(supports.createAlerts),
    canManageAlerts,
    canManageNotifications,
  );
  
  const [clusterName] = useState<string>(urlClusterName || contextClusterName || '');
  
  // State for alerts
  const [alertGroups, setAlertGroups] = useState<AlertGroupItem[]>(initialAlertGroups);
  const [selectedGroup, setSelectedGroup] = useState<AlertGroupState | AlertGroupItem | null>(null);
  const [selectedDefinition, setSelectedDefinition] = useState<AlertDefinition | null>(null);
  const [availableDefinitions, setAvailableDefinitions] = useState<AlertDefinition[]>([]);
  const [selectedDefinitions, setSelectedDefinitions] = useState<number[]>([]);
  const [filteredComponent, setFilteredComponent] = useState<string>('');
  const [filteredService, setFilteredService] = useState<string>('');
  const [, setLoading] = useState<boolean>(false);
  const [, setErrorMessage] = useState<string>('');
  
  const [isSaveDisabled, setIsSaveDisabled] = useState<boolean>(true);
  const [modalError, setModalError] = useState<string>('');
  const [modalSuccess, setModalSuccess] = useState<string>('');
  const [groupLoadError, setGroupLoadError] = useState<string>('');
  const [notificationLoadError, setNotificationLoadError] = useState<string>('');

  // State for notifications
  const [notifications, setNotifications] = useState<AlertNotification[]>([]);
  const [selectedNotification, setSelectedNotification] = useState<AlertNotification | null>(null);
  const [showDeleteNotificationModal, setShowDeleteNotificationModal] = useState<boolean>(false);
  const [showNotificationFormModal, setShowNotificationFormModal] = useState<boolean>(false);
  const [notificationFormMode, setNotificationFormMode] = useState<'create' | 'edit' | 'duplicate'>('create');
  
  // Notification form state
  const [notificationName, setNotificationName] = useState<string>('');
  const [notificationDescription, setNotificationDescription] = useState<string>('');
  const [notificationType, setNotificationType] = useState<string>('EMAIL');
  const [notificationEnabled, setNotificationEnabled] = useState<boolean>(true);
  const [isNotificationNameValid, _setIsNotificationNameValid] = useState<boolean>(true);
  const [groupsOption, setGroupsOption] = useState<'all' | 'custom'>('all');
  const [selectedGroups, setSelectedGroups] = useState<number[]>([]);
  const [selectedSeverities, setSelectedSeverities] = useState<string[]>([]);
  const [customProperties, setCustomProperties] = useState<{name: string, value: string}[]>([]);
  
  // Email specific state
  const [notificationRecipients, setNotificationRecipients] = useState<string>('');
  const [smtpServer, setSmtpServer] = useState<string>('');
  const [smtpPort, setSmtpPort] = useState<string>('');
  const [emailFrom, setEmailFrom] = useState<string>('');
  const [useAuthentication, setUseAuthentication] = useState<boolean>(false);
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [passwordConfirmation, setPasswordConfirmation] = useState<string>('');
  const [startTls, setStartTls] = useState<boolean>(false);
  
  // SNMP specific state
  const [snmpVersion, setSnmpVersion] = useState<string>('SNMPv1');
  const [snmpCommunity, setSnmpCommunity] = useState<string>('');
  const [snmpHosts, setSnmpHosts] = useState<string>('');
  const [snmpPort, setSnmpPort] = useState<string>('');
  const [snmpOid, setSnmpOid] = useState<string>('');
  
  // Alert Script specific state
  const [scriptFilename, setScriptFilename] = useState<string>('');
  const [scriptDispatchProperty, setScriptDispatchProperty] = useState<string>('');
  
  // State for alert settings
  const [alertCheckCount, setAlertCheckCount] = useState('1');
  
  // State for modals
  const [modalContent, setModalContent] = useState<string>('');
  const [showNewModal, setShowNewModal] = useState(false);
  const [showAddDefinitionModal, setShowAddDefinitionModal] = useState(false);
  const [showRenameModal, setShowRenameModal] = useState(false);
  const [showDuplicateModal, setShowDuplicateModal] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);
  const [newGroupName, setNewGroupName] = useState('');
  const [renameGroupName, setRenameGroupName] = useState('');
  const [duplicateGroupName, setDuplicateGroupName] = useState('');
  const [groupToRename, setGroupToRename] = useState<AlertGroupItem | AlertGroupState | null>(null);
  const [groupToDuplicate, setGroupToDuplicate] = useState<AlertGroupItem | AlertGroupState | null>(null);
  const [groupToDelete, setGroupToDelete] = useState<AlertGroupItem | AlertGroupState | null>(null);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isActionsDropdownOpen, setIsActionsDropdownOpen] = useState(false);

  // State for results modal
  const [showResultsModal, setShowResultsModal] = useState(false);
  const [resultsNewCount, setResultsNewCount] = useState(0);
  const [resultsUpdatedCount, setResultsUpdatedCount] = useState(0);
  const [resultsRemovedCount, setResultsRemovedCount] = useState(0);
  const [pendingChanges, setPendingChanges] = useState<{
    create: number;
    update: number;
    delete: number;
  }>({ create: 0, update: 0, delete: 0 });

  // State for name validation
  const [isNewNameValid, setIsNewNameValid] = useState(true);
  const [isRenameValid, setIsRenameValid] = useState(true);
  const [isDuplicateNameValid, setIsDuplicateNameValid] = useState(true);

  // State for showing the main modal
  const [showMainModal, setShowMainModal] = useState<boolean>(false);

  useEffect(() => {
    onModalStateChange?.(
      showMainModal || showNewModal || showAddDefinitionModal || showRenameModal ||
      showDuplicateModal || showDeleteConfirmation || showDeleteNotificationModal ||
      showNotificationFormModal || showResultsModal,
    );
  }, [
    onModalStateChange,
    showAddDefinitionModal,
    showDeleteConfirmation,
    showDeleteNotificationModal,
    showDuplicateModal,
    showMainModal,
    showNewModal,
    showNotificationFormModal,
    showRenameModal,
    showResultsModal,
  ]);

  // Helper function to get group name
  const getGroupName = (group: AlertGroupItem | AlertGroupState | null): string => {
    if (!group) return '';
    
    try {
      if ('AlertGroup' in group && group.AlertGroup) {
        return group.AlertGroup.name || '';
      } else if ('name' in group) {
        return group.name || '';
      }
      return '';
    } catch (error) {
      console.error('Error getting group name:', error);
      return '';
    }
  };

  // Check if a group name already exists
  const groupNameExists = (name: string): boolean => {
    const normalized = name.trim().toLowerCase();
    return alertGroups.some(group => 
      group.AlertGroup.name.trim().toLowerCase() === normalized &&
      !group.AlertGroup._deleted
    );
  };

  // Handle selecting a group
  const handleSelectGroup = (group: AlertGroupItem): void => {
    const { AlertGroup } = group;
    
    setSelectedGroup({
      id: AlertGroup.id,
      name: AlertGroup.name,
      default: AlertGroup.default,
      definitions: AlertGroup.definitions || [],
      targets: AlertGroup.targets || [],
      _isModified: AlertGroup._isModified,
      _isNew: AlertGroup._isNew
    });
    
    setSelectedDefinition(null);
    setIsSaveDisabled(true);
  };

  // Handle selecting a definition
  const handleSelectDefinition = (definition: AlertDefinition): void => {
    setSelectedDefinition(definition);
  };

  // Load available definitions for a group
  const loadAvailableDefinitions = async (group: AlertGroupItem | AlertGroupState): Promise<void> => {
    try {
      let definitions: AlertDefinition[] = allAlertDefinitions || [];

      if (!definitions.length && clusterName) {
        // If no definitions were passed as props, fetch them
        const response = await AlertsApi.getAlertDefinition(clusterName, '*', Date.now());
        if (response && response.items) {
          definitions = response.items.map((item: any) => item.AlertDefinition);
        }
      }

      // Extract existing definition IDs from the group
      let existingDefinitionIds: number[] = [];
      
      // Check if the group is an AlertGroupItem
      if ('AlertGroup' in group) {
        // Use destructuring to get the values we need
        const { definitions: groupDefinitions } = group.AlertGroup;
        
        if (Array.isArray(groupDefinitions)) {
          existingDefinitionIds = groupDefinitions.map(def => {
            if (typeof def === 'number') {
              return def;
            } else if (def && typeof def === 'object' && 'id' in def) {
              return def.id;
            }
            return null;
          }).filter((id): id is number => id !== null);
        }
      } else {
        // Use destructuring to get the values we need
        const { definitions: groupDefinitions } = group;
        
        if (Array.isArray(groupDefinitions)) {
          existingDefinitionIds = groupDefinitions.map(def => {
            if (typeof def === 'number') {
              return def;
            } else if (def && typeof def === 'object' && 'id' in def) {
              return def.id;
            }
            return null;
          }).filter((id): id is number => id !== null);
        }
      }
      
      // Filter out definitions that are already in the group
      const availableDefs = definitions.filter(def => 
        !existingDefinitionIds.includes(def.id)
      );
      
      setAvailableDefinitions(availableDefs);
      setSelectedDefinitions([]);
    } catch (error) {
      console.error('Error loading available definitions:', error);
    }
  };

  // Handle removing a definition from a group
  const handleRemoveDefinition = (definitionId: number): void => {
    try {
      if (!selectedGroup) {
        console.error('No group selected');
        return;
      }


      // Get the current definitions from the selected group
      let currentDefinitions: any[] = [];
      
      if ('AlertGroup' in selectedGroup) {
        currentDefinitions = [...(selectedGroup.AlertGroup.definitions || [])];
      } else {
        currentDefinitions = [...(selectedGroup.definitions || [])];
      }


      // Remove the definition
      const newDefinitions = currentDefinitions.filter(def => {
        if (typeof def === 'number') {
          return def !== definitionId;
        } else if (typeof def === 'object' && 'id' in def) {
          return def.id !== definitionId;
        }
        return true;
      });

      
      // Check if there are actual changes
      const hasChanges = currentDefinitions.length !== newDefinitions.length;

      if (!hasChanges) {
        return;
      }

      // Update the selected group with the new definitions
      if ('AlertGroup' in selectedGroup) {
        // Update AlertGroupItem
        const updatedGroup = {
          ...selectedGroup,
          AlertGroup: {
            ...selectedGroup.AlertGroup,
            definitions: newDefinitions,
            _isModified: true // Always mark as modified when removing definitions
          }
        };


        // Update the group in the alertGroups array
        const updatedGroups = alertGroups.map(g => 
          g.AlertGroup.id === selectedGroup.AlertGroup.id ? updatedGroup : g
        );
        
        setAlertGroups(updatedGroups);
        setSelectedGroup(updatedGroup);
        
        // Enable save button
        setIsSaveDisabled(false);
      } else {
        // Update AlertGroupState
        const updatedGroup: AlertGroupState = {
          ...selectedGroup,
          definitions: newDefinitions,
          _isModified: true // Always mark as modified when removing definitions
        };
        
        
        setSelectedGroup(updatedGroup);
        
        // Also update the corresponding group in alertGroups
        if (selectedGroup && 'id' in selectedGroup) {
          const updatedGroups = alertGroups.map(g => {
            if (g.AlertGroup.id === selectedGroup.id) {
              return {
                ...g,
                AlertGroup: {
                  ...g.AlertGroup,
                  definitions: newDefinitions,
                  _isModified: true // Always mark as modified when removing definitions
                }
              };
            }
            return g;
          });
          
          
          setAlertGroups(updatedGroups);
          
          // Enable save button
          setIsSaveDisabled(false);
        }
      }
      
      // Show success message
      setModalSuccess('Definition removed. Click Save to apply changes.');
      
      // Clear success message after a delay
      setTimeout(() => {
        setModalSuccess('');
      }, 5000);
    } catch (error) {
      console.error('Error removing definition:', error);
      setModalError('Failed to remove definition');
      
      // Clear error message after a delay
      setTimeout(() => {
        setModalError('');
      }, 5000);
    }
  };

  // Handle add definitions
  const handleAddDefinitions = async (): Promise<void> => {
    try {
      setLoading(true);
      setModalError('');
      setErrorMessage('');
      
      if (!selectedGroup) {
        setLoading(false);
        setModalError('No group selected');
        return;
      }
      
      let existingDefinitionIds: number[] = [];

      // Extract existing definition IDs and group info based on the structure
      if ('AlertGroup' in selectedGroup) {
        const { definitions: groupDefinitions } = selectedGroup.AlertGroup;
        
        if (Array.isArray(groupDefinitions)) {
          existingDefinitionIds = groupDefinitions.map(def => {
            if (typeof def === 'number') {
              return def;
            } else if (def && typeof def === 'object' && 'id' in def) {
              return def.id;
            }
            return null;
          }).filter((id): id is number => id !== null);
        }
      } else {
        const { definitions: groupDefinitions } = selectedGroup;
        
        if (Array.isArray(groupDefinitions)) {
          existingDefinitionIds = groupDefinitions.map(def => {
            if (typeof def === 'number') {
              return def;
            } else if (def && typeof def === 'object' && 'id' in def) {
              return def.id;
            }
            return null;
          }).filter((id): id is number => id !== null);
        }
      }
      
      // Combine existing and new definitions, removing duplicates
      const allDefinitionIds = [...new Set([...existingDefinitionIds, ...selectedDefinitions])];
      
      
      // Only mark as modified if there are actual changes
      const hasChanges = selectedDefinitions.length > 0;
      
      // Update the state instead of making an API call
      if ('AlertGroup' in selectedGroup) {
        const { id: groupId } = selectedGroup.AlertGroup;
        
        // Update the selected group
        const updatedSelectedGroup = {
          ...selectedGroup,
          AlertGroup: {
            ...selectedGroup.AlertGroup,
            definitions: allDefinitionIds,
            _isModified: hasChanges
          }
        };
        
        setSelectedGroup(updatedSelectedGroup);
        
        // Update the alertGroups state
        const updatedGroups = alertGroups.map(g => {
          if (g.AlertGroup.id === groupId) {
            return {
              ...g,
              AlertGroup: {
                ...g.AlertGroup,
                definitions: allDefinitionIds,
                _isModified: hasChanges
              }
            };
          }
          return g;
        });
        
        setAlertGroups(updatedGroups);
        
        // Enable save button if there are changes
        if (hasChanges) {
          setIsSaveDisabled(false);
        }
      } else {
        const { id: groupId } = selectedGroup;
        
        // Update the selected group
        const updatedSelectedGroup = {
          ...selectedGroup,
          definitions: allDefinitionIds,
          _isModified: hasChanges
        };
        
        setSelectedGroup(updatedSelectedGroup);
        
        // Update the alertGroups state
        const updatedGroups = alertGroups.map(g => {
          if (g.AlertGroup.id === groupId) {
            return {
              ...g,
              AlertGroup: {
                ...g.AlertGroup,
                definitions: allDefinitionIds,
                _isModified: hasChanges
              }
            };
          }
          return g;
        });
        
        setAlertGroups(updatedGroups);
        
        // Enable save button if there are changes
        if (hasChanges) {
          setIsSaveDisabled(false);
        }
      }
      
      // Reset state and close modal
      setSelectedDefinitions([]);
      setShowAddDefinitionModal(false);
    } catch (error: any) {
      console.error('Error adding definitions:', error);
      
      // Show error message
      const errorMsg = error.message || 'Failed to add definitions';
      setErrorMessage(errorMsg);
      setModalError(errorMsg);
      
      // Clear error message after a delay
      setTimeout(() => {
        setErrorMessage('');
        setModalError('');
      }, 5000);
    } finally {
      setLoading(false);
    }
  };

  // Handle create group
  const handleCreateGroup = async (): Promise<void> => {
    try {
      setLoading(true);
      setErrorMessage('');
      setModalError('');
      
      const nameError = validateAlertGroupName(newGroupName);
      if (nameError) throw new Error(nameError);
      
      // Check if a group with this name already exists
      if (groupNameExists(newGroupName)) {
        throw new Error('Alert Group with given name already exists');
      }
      
      
      // Generate a temporary negative ID for the new group
      const tempId = -Math.floor(Math.random() * 2000000000);
      
      // Create a new group in the state instead of making an API call
      // The actual creation will happen when the user clicks Save
      const newGroup: AlertGroupItem = {
        AlertGroup: {
          id: tempId,
          name: newGroupName.trim(),
          default: false,
          definitions: [],
          targets: [],
          _isNew: true,
          _isModified: false,
          _deleted: false,
          cluster_name: clusterName
        }
      };
      
      // Add the new group to the state
      setAlertGroups([...alertGroups, newGroup]);
      
      // Select the new group
      handleSelectGroup(newGroup);
      
      // Enable save button
      setIsSaveDisabled(false);

      // Reset state and close new group modal
      setNewGroupName('');
      handleCloseModal();
    } catch (error: any) {
      console.error('Error creating group:', error);
      
      // Show error message
      const errorMsg = error.message || 'Failed to create group';
      setErrorMessage(errorMsg);
      setModalError(errorMsg);
      
      // Don't clear error message for duplicate name errors
      if (!error.message.includes('already exists')) {
        // Clear error message after a delay
        setTimeout(() => {
          setErrorMessage('');
          setModalError('');
        }, 5000);
      }
    } finally {
      setLoading(false);
    }
  };

  // Handle duplicate confirmation
  const handleDuplicateConfirm = (): void => {
    if (!groupToDuplicate || !clusterName) {
      handleCloseModal();
      return;
    }
    
    try {
      const nameError = validateAlertGroupName(duplicateGroupName);
      if (nameError) throw new Error(nameError);
      
      // Check if a group with this name already exists
      if (groupNameExists(duplicateGroupName)) {
        throw new Error('Alert Group with given name already exists');
      }
      
      
      // Generate a temporary negative ID for the new group
      const tempId = -Math.floor(Math.random() * 2000000000);
      
      // Get the definitions from the group to duplicate
      const definitionsToDuplicate = 'AlertGroup' in groupToDuplicate 
        ? [...(groupToDuplicate.AlertGroup.definitions || [])]
        : [...(groupToDuplicate.definitions || [])];
      const targetsToDuplicate = 'AlertGroup' in groupToDuplicate
        ? [...(groupToDuplicate.AlertGroup.targets || [])]
        : [...(groupToDuplicate.targets || [])];
      
      
      // Create a new group with the duplicated data
      const newGroup: AlertGroupItem = {
        AlertGroup: {
          id: tempId,
          name: duplicateGroupName.trim(),
          default: false,
          definitions: definitionsToDuplicate,
          targets: targetsToDuplicate,
          _isNew: true,
          _isModified: false,
          _deleted: false,
          cluster_name: clusterName
        }
      };
      
      
      // Add the new group to the state
      const updatedGroups = [...alertGroups, newGroup];
      setAlertGroups(updatedGroups);
      
      // Select the new group
      handleSelectGroup(newGroup);
      
      // Enable save button
      setIsSaveDisabled(false);
      
      handleCloseModal();
    } catch (error: any) {
      console.error('Error duplicating group:', error);
      
      // Show error message
      const errorMsg = error.message || 'Failed to duplicate group';
      setErrorMessage(errorMsg);
      setModalError(errorMsg);
      
      // Don't clear error message for duplicate name errors
      if (!error.message.includes('already exists')) {
        // Clear error message after a delay
        setTimeout(() => {
          setErrorMessage('');
          setModalError('');
        }, 5000);
      }
    }
  };

  // Handle rename confirmation
  const handleRenameConfirm = (): void => {
    if (!groupToRename || !clusterName) {
      handleCloseModal();
      return;
    }
    
    try {
      const currentName = getGroupName(groupToRename);
      const normalizedName = renameGroupName.trim();
      const nameError = validateAlertGroupName(renameGroupName);
      if (nameError) throw new Error(nameError);
      
      if (normalizedName === currentName.trim()) {
        handleCloseModal();
        return; // User didn't change the name
      }
      
      // Check if a group with this name already exists (excluding the current group)
      const groupId = 'AlertGroup' in groupToRename ? groupToRename.AlertGroup.id : groupToRename.id;
      const nameExists = alertGroups.some(group => 
        group.AlertGroup.name.trim().toLowerCase() === normalizedName.toLowerCase() &&
        group.AlertGroup.id !== groupId &&
        !group.AlertGroup._deleted
      );
      
      if (nameExists) {
        throw new Error('Alert Group with given name already exists');
      }
      
      
      if ('AlertGroup' in groupToRename) {
        // Handle AlertGroupItem structure
        const updatedGroup = {
          ...groupToRename,
          AlertGroup: {
            ...groupToRename.AlertGroup,
            name: normalizedName,
            _isModified: true
          }
        };
        
        // Find the group in the alertGroups array and update it
        const groupIndex = alertGroups.findIndex(g => g.AlertGroup.id === groupToRename.AlertGroup.id);
        
        if (groupIndex !== -1) {
          // Create a new array with the updated group
          const updatedGroups = [...alertGroups];
          updatedGroups[groupIndex] = updatedGroup;
          
          
          // Update both states
          setAlertGroups(updatedGroups);
          setSelectedGroup(updatedGroup);
          
          // Enable save button
          setIsSaveDisabled(false);
          
          handleCloseModal();
        } else {
          throw new Error('Group not found in the list');
        }
      } else {
        // Handle AlertGroupState structure
        const updatedGroup = {
          ...groupToRename,
          name: normalizedName,
          _isModified: true
        };
        
        setSelectedGroup(updatedGroup);
        
        // Find the corresponding group in alertGroups and update it
        if (groupToRename.id) {
          const updatedGroups = alertGroups.map(g => {
            if (g.AlertGroup.id === groupToRename.id) {
              const updatedGroupItem = {
                ...g,
                AlertGroup: {
                  ...g.AlertGroup,
                  name: normalizedName,
                  _isModified: true
                }
              };
              
              
              return updatedGroupItem;
            }
            return g;
          });
          
          setAlertGroups(updatedGroups);
          
          // Enable save button
          setIsSaveDisabled(false);
        }
        
        handleCloseModal();
      }
    } catch (error: any) {
      console.error('Error renaming group:', error);
      
      // Show error message
      const errorMsg = error.message || 'Failed to rename group';
      setErrorMessage(errorMsg);
      setModalError(errorMsg);
      
      // Don't clear error message for duplicate name errors
      if (!error.message.includes('already exists')) {
        // Clear error message after a delay
        setTimeout(() => {
          setErrorMessage('');
          setModalError('');
        }, 5000);
      }
    }
  };

  // Handle delete group
  const handleDeleteGroup = (group: AlertGroupItem | AlertGroupState): void => {
    
    // Make sure we have a clean state
    setModalError('');
    setModalSuccess('');
    
    // Set the group to delete
    setGroupToDelete(group);
    
    // Show the confirmation modal
    setShowDeleteConfirmation(true);
  };

  // Handle add definitions to group
  const handleAddDefinitionsToGroup = (group: AlertGroupItem | AlertGroupState): void => {
    
    setSelectedGroup(group);
    setSelectedDefinitions([]);
    setFilteredComponent('');
    setFilteredService('');
    setShowAddDefinitionModal(true);
    setModalError('');
    setModalSuccess('');
  };

  // Handle save global alert settings
  const handleSaveGlobalAlertSettings = async (): Promise<void> => {
    const validationError = validateRepeatTolerance(alertCheckCount);
    if (validationError) {
      setModalError(validationError);
      return;
    }

    try {
      setLoading(true);
      setModalError('');
      setModalSuccess('');
      
      // Update the cluster-env configuration with the new alerts_repeat_tolerance value
      const response = await ClusterApi.getCluster(clusterName);
      const desiredConfigs = response?.Clusters?.desired_configs;

      if (desiredConfigs && desiredConfigs["cluster-env"]) {
        const clusterEnvTag = desiredConfigs["cluster-env"].tag;
        
        // Get current cluster-env configuration
        const configUrl = `clusters/${clusterName}/configurations?type=cluster-env&tag=${clusterEnvTag}&fields=*`;
        const configResponse = await ambariApi.request({
          url: configUrl,
          method: "GET",
        });

        if (configResponse.data && configResponse.data.items && configResponse.data.items.length > 0) {
          const clusterEnvConfig = configResponse.data.items[0];
          
          // Get ALL existing properties from the current configuration
          const existingProperties = clusterEnvConfig.properties || {};
          
          // Create updated properties by merging existing properties with the new value
          // This prevents data loss by preserving all existing configuration
          const updatedProperties = {
            ...existingProperties,
            alerts_repeat_tolerance: alertCheckCount
          };

          // Save the updated configuration using the proper service config API structure
          const configData = {
            Clusters: {
              desired_config: {
                type: "cluster-env",
                tag: `version${Date.now()}`,
                properties: updatedProperties, // Send ALL properties, not just the changed one
                service_config_version_note: "Updated global alert check count"
              }
            }
          };

          await ConfigsApi.saveConfigs(clusterName, configData);

          // ConfigsApi.saveConfigs returns the response data, not the full response object
          // If we get here without an exception, the save was successful
          // Reload the global settings to confirm the change
          await loadGlobalAlertSettings();
        } else {
          throw new Error('Failed to get current cluster-env configuration');
        }
      } else {
        throw new Error('cluster-env configuration not found');
      }
      
      setShowMainModal(false);
      setIsSaveDisabled(true);
    } catch (error) {
      console.error('Error saving global alert settings:', error);
      setModalError('Failed to save global alert settings. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Handle save changes
  const handleSaveChanges = async (): Promise<void> => {
    try {
      setLoading(true);
      setErrorMessage('');
      setModalError('');
      const plan = planAlertGroupSave(alertGroups);
      const newCount = plan.create.length;
      const updatedCount = plan.update.length;
      const removedCount = plan.delete.length;

      setPendingChanges({
        create: newCount,
        update: updatedCount,
        delete: removedCount
      });
      const result = await executeAlertGroupSave(plan, {
        create: (_group, payload) => AlertsApi.createAlertGroup(clusterName, payload),
        update: (group, payload) => AlertsApi.updateAlertGroup(clusterName, group.AlertGroup.id, payload),
        delete: (group) => AlertsApi.deleteAlertGroup(clusterName, group.AlertGroup.id),
      });

      const locallyReconciledGroups = reconcileAlertGroupSave(alertGroups, result.failures);
      let serverGroups: AlertGroupItem[] | null = null;
      try {
        const response = await AlertsApi.getAlertGroups(clusterName);
        if (!Array.isArray(response?.items)) throw new Error('Invalid Alert Groups response');
        serverGroups = response.items.map((item: any) => ({ AlertGroup: item.AlertGroup }));
      } catch (error) {
        console.error('Error refreshing alert groups after save:', error);
      }

      let reconciledGroups = locallyReconciledGroups;
      let remainingFailures = result.failures;
      if (serverGroups && result.failures.length > 0) {
        const reconciliation = reconcileAlertGroupSaveWithServer(
          serverGroups,
          alertGroups,
          result.failures,
        );
        reconciledGroups = reconciliation.groups;
        remainingFailures = reconciliation.failures;
      }

      if (remainingFailures.length > 0) {
        const failedCounts = remainingFailures.reduce((counts, failure) => {
          counts[failure.kind] += 1;
          return counts;
        }, { create: 0, update: 0, delete: 0 });
        const errorMsg = `Some operations failed: ${failedCounts.create} creations, ${failedCounts.update} updates, ${failedCounts.delete} deletions failed. Review the pending changes and try again.${serverGroups ? '' : ' The server refresh also failed; only failed operations remain pending locally.'}`;
        setAlertGroups(reconciledGroups);
        setSelectedGroup(null);
        setErrorMessage(errorMsg);
        setModalError(errorMsg);
        setIsSaveDisabled(false);
        return;
      }

      if (!serverGroups) {
        setAlertGroups(locallyReconciledGroups);
        setSelectedGroup(null);
        setGroupLoadError('The changes were saved, but Alert Groups could not be reloaded. Retry the load before closing.');
        setIsSaveDisabled(true);
        return;
      }

      setAlertGroups(serverGroups);
      setSelectedGroup(null);
      setShowMainModal(false);
      setIsSaveDisabled(true);

      if (newCount > 0 || updatedCount > 0 || removedCount > 0) {
        setResultsNewCount(newCount);
        setResultsUpdatedCount(updatedCount);
        setResultsRemovedCount(removedCount);
        setTimeout(() => setShowResultsModal(true), 500);
      }
    } catch (error: any) {
      console.error('Error saving alert groups:', error);
      
      // Show error message
      const errorMsg = 'Failed to save alert groups: ' + (error.message || 'Unknown error');
      setErrorMessage(errorMsg);
      setModalError(errorMsg);
      
    } finally {
      setLoading(false);
    }
  };

  // Handle close rename modal
  // const handleCloseRenameModal = handleCloseModal;

  // Handle close duplicate modal
  // const handleCloseDuplicateModal = handleCloseModal;

  // Initialize rename modal state when opened
  React.useEffect(() => {
    if (showRenameModal && groupToRename) {
      try {
        const name = getGroupName(groupToRename);
        setRenameGroupName(name);
      } catch (error) {
        console.error('Error setting rename group name:', error);
      }
    }
  }, [showRenameModal, groupToRename]);

  // Initialize duplicate modal state when opened
  React.useEffect(() => {
    if (showDuplicateModal && groupToDuplicate) {
      try {
        const name = getGroupName(groupToDuplicate);
        // Remove " (Default)" from the name if present
        const cleanName = name.replace(/\s*\(Default\)\s*/, '');
        setDuplicateGroupName(`Copy of ${cleanName}`);
      } catch (error) {
        console.error('Error setting duplicate group name:', error);
      }
    }
  }, [showDuplicateModal, groupToDuplicate]);

  // Close dropdown when clicking outside
  React.useEffect(() => {
    const handleClickOutside = () => {
      if (isDropdownOpen) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [isDropdownOpen]);

  // Check if there are any pending changes
  const checkForPendingChanges = (): boolean => {
    return alertGroups.some(group => 
      group.AlertGroup._isNew || 
      group.AlertGroup._isModified || 
      group.AlertGroup._deleted
    );
  };

  // Use effect to check for pending changes and update the save button state
  React.useEffect(() => {
    const hasChanges = checkForPendingChanges();
    setIsSaveDisabled(!hasChanges);
  }, [alertGroups]);

  // Handle confirm delete group
  const handleConfirmDeleteGroup = async (): Promise<void> => {
    if (!groupToDelete || !clusterName) return;

    try {
      setLoading(true);
      setErrorMessage('');
      setModalError('');
      
      
      // Instead of making an API call, just mark the group for deletion
      // The actual deletion will happen when the user clicks Save
      const updatedGroups = alertGroups.map(g => {
        if ('AlertGroup' in groupToDelete) {
          // Handle AlertGroupItem
          if (g.AlertGroup.id === groupToDelete.AlertGroup.id) {
            return {
              ...g,
              AlertGroup: {
                ...g.AlertGroup,
                _deleted: true
              }
            };
          }
        } else {
          // Handle AlertGroupState
          if (g.AlertGroup.id === groupToDelete.id) {
            return {
              ...g,
              AlertGroup: {
                ...g.AlertGroup,
                _deleted: true
              }
            };
          }
        }
        return g;
      });
      
      
      setAlertGroups(updatedGroups);

      // Reset states
      handleCloseModal();
      
      // Clear the selected group if it was the one that was marked for deletion
      if (selectedGroup) {
        if ('AlertGroup' in selectedGroup && 'AlertGroup' in groupToDelete) {
          if (selectedGroup.AlertGroup.id === groupToDelete.AlertGroup.id) {
            setSelectedGroup(null);
          }
        } else if (!('AlertGroup' in selectedGroup) && !('AlertGroup' in groupToDelete)) {
          if (selectedGroup.id === groupToDelete.id) {
            setSelectedGroup(null);
          }
        }
      }
      
      // Enable save button since we've made changes
      setIsSaveDisabled(false);
    } catch (error: any) {
      console.error('Error marking group for deletion:', error);
      
      // Show error message
      const errorMsg = error.message || 'Failed to mark group for deletion';
      setErrorMessage(errorMsg);
      setModalError(errorMsg);
      
      // Clear error message after a delay
      setTimeout(() => {
        setErrorMessage('');
        setModalError('');
      }, 5000);
    } finally {
      setLoading(false);
    }
  };

  // Handle setting new group name with validation
  const handleNewGroupNameChange = (name: string): void => {
    setNewGroupName(name);
    const nameError = validateAlertGroupName(name);
    if (nameError) {
      setIsNewNameValid(false);
      setModalError(nameError);
      return;
    }
    
    // Check if name already exists
    if (groupNameExists(name)) {
      setIsNewNameValid(false);
      setModalError('Alert Group with given name already exists');
    } else {
      setIsNewNameValid(true);
      setModalError('');
    }
  };
  
  // Handle setting rename group name with validation
  const handleRenameGroupNameChange = (name: string): void => {
    setRenameGroupName(name);
    const nameError = validateAlertGroupName(name);
    if (nameError) {
      setIsRenameValid(false);
      setModalError(nameError);
      return;
    }
    
    // Get current group ID to exclude from duplicate check
    const currentGroupId = groupToRename ? 
      ('AlertGroup' in groupToRename ? groupToRename.AlertGroup.id : groupToRename.id) : 
      undefined;
    
    // Check if name already exists (excluding the current group)
    const nameExists = alertGroups.some(group => 
      group.AlertGroup.name.trim().toLowerCase() === name.trim().toLowerCase() &&
      group.AlertGroup.id !== currentGroupId &&
      !group.AlertGroup._deleted
    );
    
    if (nameExists) {
      setIsRenameValid(false);
      setModalError('Alert Group with given name already exists');
    } else {
      setIsRenameValid(true);
      setModalError('');
    }
  };
  
  // Handle setting duplicate group name with validation
  const handleDuplicateGroupNameChange = (name: string): void => {
    setDuplicateGroupName(name);
    const nameError = validateAlertGroupName(name);
    if (nameError) {
      setIsDuplicateNameValid(false);
      setModalError(nameError);
      return;
    }
    
    // Check if name already exists
    if (groupNameExists(name)) {
      setIsDuplicateNameValid(false);
      setModalError('Alert Group with given name already exists');
    } else {
      setIsDuplicateNameValid(true);
      setModalError('');
    }
  };

  // Toggle modal content
  const toggleModalContent = (content: string): void => {
    setModalContent(content);
    setShowMainModal(true);
    setIsActionsDropdownOpen(false); // Close the dropdown menu
  };

  // Handle manage alert groups
  const handleManageAlertGroups = (): void => {
    setModalError('');
    setGroupLoadError('');
    setNotificationLoadError('');
    toggleModalContent('Manage Alert Groups');
    void Promise.all([loadAlertGroups(), loadNotifications()]);
  };

  // Handle manage notifications
  const handleManageNotifications = (): void => {
    setModalError('');
    setNotificationLoadError('');
    toggleModalContent('Manage Notifications');
    void loadNotifications();
  };

  // Handle manage alert settings
  const handleManageAlertSettings = (): void => {
    setIsSaveDisabled(true);
    toggleModalContent('Manage Alert Settings');
  };

  // Render modal body based on content
  const renderModalBody = (): React.ReactNode => {
    switch (modalContent) {
      case 'Manage Alert Groups':
        return (
          <ManageAlertGroupsModal
            alertGroups={alertGroups.filter(group => !group.AlertGroup._deleted)}
            selectedGroup={selectedGroup}
            handleSelectGroup={handleSelectGroup}
            toggleNewModal={toggleNewModal}
            handleAddDefinitionsToGroup={handleAddDefinitionsToGroup}
            handleDeleteGroup={handleDeleteGroup}
            toggleGearDropdown={toggleGearDropdown}
            isDropdownOpen={isDropdownOpen}
            handleRename={handleRenameGroup}
            handleDuplicate={handleDuplicateGroup}
            selectedDefinition={selectedDefinition}
            handleSelectDefinition={handleSelectDefinition}
            handleRemoveDefinition={handleRemoveDefinition}
            allAlertDefinitions={allAlertDefinitions}
            loadAvailableDefinitions={loadAvailableDefinitions}
            setShowAddDefinitionModal={setShowAddDefinitionModal}
            errorMessage={groupLoadError || notificationLoadError || modalError}
            successMessage={modalSuccess}
            notifications={notifications}
            onNotificationsChange={handleNotificationsChange}
            onManageNotifications={handleManageNotifications}
            onRetry={groupLoadError || notificationLoadError ? () => {
              setGroupLoadError('');
              setNotificationLoadError('');
              setModalError('');
              void Promise.all([loadAlertGroups(), loadNotifications()]);
            } : undefined}
          />
        );
      case 'Manage Notifications':
        return (
          <ManageNotificationsModal
            notifications={notifications.filter(n => !n.AlertTarget._deleted)}
            selectedNotification={selectedNotification}
            handleSelectNotification={handleSelectNotification}
            handleDeleteModalShow={handleDeleteNotificationModalShow}
            toggleGearDropdown={toggleGearDropdown}
            isDropdownOpen={isDropdownOpen}
            handleEdit={handleEditNotification}
            handleDuplicate={handleDuplicateNotification}
            handleToggleEnabled={handleToggleNotificationEnabled}
            handleCreateNotification={handleCreateNotification}
            errorMessage={notificationLoadError || modalError}
            successMessage={modalSuccess}
            setErrorMessage={setModalError}
            setSuccessMessage={setModalSuccess}
            onRetry={notificationLoadError ? () => {
              setNotificationLoadError('');
              setModalError('');
              void loadNotifications();
            } : undefined}
            alertGroups={alertGroups
              .filter(group => !group.AlertGroup._deleted)
              .map(group => ({ 
                id: group.AlertGroup.id || 0, 
                name: group.AlertGroup.name,
                default: group.AlertGroup.default
              }))
            }
          />
        );
      case 'Manage Alert Settings':
        return (
          <ManageAlertSettingsModal
            alertCheckCount={alertCheckCount}
            setAlertCheckCount={setAlertCheckCount}
            setIsSaveDisabled={setIsSaveDisabled}
            modalError={modalError}
            modalSuccess={modalSuccess}
          />
        );
      default:
        return null; // Return null instead of a div to prevent flash of content
    }
  };

  const loadAlertGroups = async (): Promise<void> => {
    if (!clusterName) return;

    try {
      setLoading(true);
      const response = await AlertsApi.getAlertGroups(clusterName);
      setAlertGroups((response?.items || []).map((item: any) => ({ AlertGroup: item.AlertGroup })));
      setGroupLoadError('');
    } catch (error) {
      console.error('Error loading alert groups:', error);
      setGroupLoadError('Failed to load alert groups.');
    } finally {
      setLoading(false);
    }
  };

  // Load notifications
  const loadNotifications = async (): Promise<void> => {
    if (!clusterName) return;
    
    try {
      setLoading(true);
      const response = await AlertsApi.getNotifications(clusterName);
      if (response && response.items) {
        
        const fetchedNotifications: AlertNotification[] = response.items.map((item: any) => {
          // Convert API's 'enabled' property to UI's 'is_enabled' property
          const alertTarget = item.AlertTarget;
          
          
          return {
            AlertTarget: {
              ...alertTarget,
              is_enabled: alertTarget.is_enabled ?? alertTarget.enabled,
              // If groups is an array of objects, keep it as is
              // The API returns groups as an array of objects, but our code expects it as an array of numbers
              // when sending data back to the API
              groups: Array.isArray(alertTarget.groups) 
                ? alertTarget.groups.map((group: any) => typeof group === 'number' ? group : group.id)
                : alertTarget.groups
            }
          };
        });
        
        setNotifications(fetchedNotifications);
        setNotificationLoadError('');
      }
    } catch (error) {
      console.error('Error loading notifications:', error);
      setNotificationLoadError('Failed to load notifications.');
    } finally {
      setLoading(false);
    }
  };
  
  // Load data on component mount
  useEffect(() => {
    loadNotifications();
    loadGlobalAlertSettings();
  }, [clusterName]);

  // Load global alert settings
  const loadGlobalAlertSettings = async (): Promise<void> => {
    if (!clusterName) return;
    
    try {
      const response = await ClusterApi.getCluster(clusterName);
      const desiredConfigs = response?.Clusters?.desired_configs;

      if (desiredConfigs && desiredConfigs["cluster-env"]) {
        const clusterEnvTag = desiredConfigs["cluster-env"].tag;
        const configUrl = `clusters/${clusterName}/configurations?type=cluster-env&tag=${clusterEnvTag}&fields=*`;

        const configResponse = await ambariApi.request({
          url: configUrl,
          method: "GET",
        });

        if (
          configResponse.data &&
          configResponse.data.items &&
          configResponse.data.items.length > 0
        ) {
          const clusterEnvConfig = configResponse.data.items[0];
          const globalCheckCount = clusterEnvConfig.properties?.alerts_repeat_tolerance || "1";
          setAlertCheckCount(String(globalCheckCount));
        }
      }
    } catch (error) {
      console.error('Error loading global alert settings:', error);
      setAlertCheckCount('1');
    } finally {
    }
  };

  // Handle notification selection
  const handleSelectNotification = (notification: AlertNotification): void => {
    setSelectedNotification(notification);
  };
  
  // Handle notification deletion modal
  const handleDeleteNotificationModalShow = (notification: AlertNotification): void => {
    setModalError('');
    setModalSuccess('');
    setSelectedNotification(notification);
    setShowDeleteNotificationModal(true);
  };
  
  // Handle notification deletion confirmation
  const handleDeleteNotification = async (notification: AlertNotification): Promise<void> => {
    try {
      setLoading(true);
      
      if (notification.AlertTarget.id) {
        // Directly delete the notification
        await AlertsApi.deleteNotification(clusterName, notification.AlertTarget.id);
        
        // Remove the notification from state
        const updatedNotifications = notifications.filter(n => 
          n.AlertTarget.id !== notification.AlertTarget.id
        );
        
        setNotifications(updatedNotifications);
        setSelectedNotification(null);
        setModalSuccess(`Notification "${notification.AlertTarget.name}" deleted successfully.`);
      }
      
      setShowDeleteNotificationModal(false);
      
      // Reload notifications to ensure the updated list is available immediately
      await loadNotifications();
      
      setTimeout(() => {
        setModalSuccess('');
      }, 5000);
    } catch (error) {
      console.error('Error deleting notification:', error);
      setModalError('Failed to delete notification. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Handle notification edit
  const handleEditNotification = (notification: AlertNotification): void => {
    try {
      // Set form mode and notification details
      setNotificationFormMode('edit');
      setSelectedNotification(notification);
      setNotificationName(notification.AlertTarget.name);
      setNotificationDescription(notification.AlertTarget.description || '');
      const uiType = notificationTypeToUi(notification.AlertTarget.notification_type);
      setNotificationType(uiType);
      
      // Reset all type-specific fields first
      setNotificationRecipients('');
      setSmtpServer('');
      setSmtpPort('');
      setEmailFrom('');
      setUseAuthentication(false);
      setUsername('');
      setPassword('');
      setPasswordConfirmation('');
      setStartTls(false);
      setSnmpVersion('SNMPv1');
      setSnmpCommunity('');
      setSnmpHosts('');
      setSnmpPort('');
      setSnmpOid('');
      setScriptFilename('');
      setScriptDispatchProperty('');
      
      // Set type-specific fields based on notification type
      if (uiType === 'EMAIL') {
        // Handle recipients which may be an array or string
        const recipients = notification.AlertTarget.properties?.['ambari.dispatch.recipients'];
        if (recipients) {
          if (Array.isArray(recipients)) {
            setNotificationRecipients(recipients.join(','));
          } else {
            setNotificationRecipients(String(recipients));
          }
        } else {
          setNotificationRecipients('');
        }
        
        setSmtpServer(notification.AlertTarget.properties?.['mail.smtp.host'] || '');
        setSmtpPort(notification.AlertTarget.properties?.['mail.smtp.port'] || '');
        setEmailFrom(notification.AlertTarget.properties?.['mail.smtp.from'] || '');
        
        const hasAuth = String(notification.AlertTarget.properties?.['mail.smtp.auth']) === 'true';
        setUseAuthentication(hasAuth);
        setUsername(notification.AlertTarget.properties?.['ambari.dispatch.credential.username'] || '');
        if (notification.AlertTarget.properties?.['ambari.dispatch.credential.password']) {
          setPassword('');
        } else {
          setPassword('');
        }
        setPasswordConfirmation('');
        setStartTls(String(notification.AlertTarget.properties?.['mail.smtp.starttls.enable']) === 'true');
      } else if (uiType === 'SNMP' || uiType === 'Custom SNMP') {
        setSnmpVersion(notification.AlertTarget.properties?.['ambari.dispatch.snmp.version'] || 'SNMPv1');
        setSnmpCommunity(notification.AlertTarget.properties?.['ambari.dispatch.snmp.community'] || '');
        const recipients = notification.AlertTarget.properties?.['ambari.dispatch.recipients'];
        setSnmpHosts(Array.isArray(recipients) ? recipients.join(',') : String(recipients || ''));
        setSnmpPort(notification.AlertTarget.properties?.['ambari.dispatch.snmp.port'] || '');
        
        if (uiType === 'Custom SNMP') {
          setSnmpOid(notification.AlertTarget.properties?.['ambari.dispatch.snmp.oids.trap'] || '');
        }
      } else if (uiType === 'Alert Script') {
        setScriptFilename(notification.AlertTarget.properties?.['ambari.dispatch-property.script.filename'] || '');
        setScriptDispatchProperty(notification.AlertTarget.properties?.['ambari.dispatch-property.script'] || '');
      }
      
      // Set notification enabled state
      setNotificationEnabled(notification.AlertTarget.is_enabled || false);
      
      // Set groups option and selected groups
      if (notification.AlertTarget.global) {
        setGroupsOption('all');
        setSelectedGroups([]);
      } else {
        setGroupsOption('custom');
        
        // Use the group IDs directly
        if (notification.AlertTarget.groups && notification.AlertTarget.groups.length > 0) {
          setSelectedGroups(notification.AlertTarget.groups);
        } else {
          setSelectedGroups([]);
        }
      }
      
      // Set selected severities
      const severities: string[] = [];
      if (notification.AlertTarget.alert_states) {
        if (notification.AlertTarget.alert_states.includes('OK')) severities.push('OK');
        if (notification.AlertTarget.alert_states.includes('WARNING')) severities.push('WARNING');
        if (notification.AlertTarget.alert_states.includes('CRITICAL')) severities.push('CRITICAL');
        if (notification.AlertTarget.alert_states.includes('UNKNOWN')) severities.push('UNKNOWN');
      }
      setSelectedSeverities(severities.length > 0 ? severities : ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
      
      // Set custom properties
      const customProps: {name: string, value: string}[] = [];
      if (notification.AlertTarget.properties) {
        const standardProps = notificationBuiltInKeys(uiType);
        
        Object.entries(notification.AlertTarget.properties).forEach(([key, value]) => {
          if (!standardProps.includes(key)) {
            customProps.push({ name: key, value: String(value) });
          }
        });
      }
      setCustomProperties(customProps);
      
      // Show the form modal
      setShowNotificationFormModal(true);
    } catch (error) {
      console.error('Error editing notification:', error);
      setModalError('Failed to edit notification');
    }
  };

  // Handle notification duplication
  const handleDuplicateNotification = (notification: AlertNotification): void => {
    try {
      // First, set all the same values as edit
      handleEditNotification(notification);
      
      // Then override the mode and name
      setNotificationFormMode('duplicate');
      setNotificationName(`Copy of ${notification.AlertTarget.name}`);
      
    } catch (error) {
      console.error('Error duplicating notification:', error);
      setModalError('Failed to duplicate notification');
    }
  };

  // Handle notification toggle enabled
  const handleToggleNotificationEnabled = async (notification: AlertNotification): Promise<void> => {
    try {
      setLoading(true);
      setModalError('');
      setModalSuccess('');
      
      // Make direct API call to toggle the enabled state
      const response = await AlertsApi.updateNotification(
        clusterName,
        notification.AlertTarget.id || 0,
        {
          AlertTarget: {
            enabled: !notification.AlertTarget.is_enabled
          }
        }
      );
      
      if (response && response.status === 200) {
        // Update the notification in state
        const updatedNotifications = notifications.map(n => {
          if (n.AlertTarget.id === notification.AlertTarget.id) {
            return { 
              AlertTarget: { 
                ...n.AlertTarget,
                is_enabled: !n.AlertTarget.is_enabled
              } 
            };
          }
          return n;
        });
        
        setNotifications(updatedNotifications);
        
        // If the toggled notification is the selected one, update it
        if (selectedNotification && selectedNotification.AlertTarget.id === notification.AlertTarget.id) {
          setSelectedNotification({
            AlertTarget: {
              ...selectedNotification.AlertTarget,
              is_enabled: !selectedNotification.AlertTarget.is_enabled
            }
          });
        }
        
        // Show success message
        setModalSuccess(`Notification "${notification.AlertTarget.name}" ${notification.AlertTarget.is_enabled ? 'disabled' : 'enabled'} successfully.`);
        
        // Reload notifications to ensure the updated list is available immediately
        await loadNotifications();
        
        setTimeout(() => {
          setModalSuccess('');
        }, 5000);
      } else {
        throw new Error('Failed to update notification enabled state');
      }
    } catch (error) {
      console.error('Error toggling notification enabled state:', error);
      setModalError('Failed to toggle notification enabled state');
    } finally {
      setLoading(false);
    }
  };

  // Handle notification form submission
  const handleNotificationFormSubmit = async (): Promise<void> => {
    try {
      setModalError('');
      const form: AlertNotificationForm = {
        name: notificationName,
        description: notificationDescription,
        type: notificationType as AlertNotificationForm['type'],
        global: groupsOption === 'all',
        groups: selectedGroups,
        alertStates: selectedSeverities,
        recipients: notificationRecipients,
        smtpHost: smtpServer,
        smtpPort,
        emailFrom,
        useAuthentication,
        username,
        password,
        passwordConfirmation,
        startTls,
        snmpVersion,
        snmpCommunity,
        snmpHosts,
        snmpPort,
        snmpOid,
        scriptFilename,
        scriptDispatchProperty,
        customProperties,
        existingProperties: selectedNotification?.AlertTarget.properties,
        preserveSensitivePassword: notificationFormMode === 'edit',
      };
      const errors = validateAlertNotificationForm(form);
      const duplicateName = notifications.some((notification) =>
        notification.AlertTarget.name.trim().toLowerCase() === notificationName.trim().toLowerCase() &&
        (notificationFormMode !== 'edit' || notification.AlertTarget.id !== selectedNotification?.AlertTarget.id),
      );
      if (duplicateName) errors.unshift('An Alert Notification with this name already exists.');
      if (errors.length > 0) {
        setModalError(errors.join(' '));
        return;
      }
      setLoading(true);
      const payload = buildAlertNotificationPayload(form);
      if (notificationFormMode === 'edit') {
        const notificationId = selectedNotification?.AlertTarget.id;
        if (!notificationId) throw new Error('The selected notification has no server ID.');
        await AlertsApi.updateNotification(clusterName, notificationId, payload);
      } else {
        await AlertsApi.createNotification(clusterName, payload);
      }

      await loadNotifications();
      setSelectedNotification(null);
      resetNotificationForm();
      setShowNotificationFormModal(false);
      setModalSuccess(notificationFormMode === 'edit'
        ? 'Notification updated successfully.'
        : notificationFormMode === 'duplicate'
          ? 'Notification duplicated successfully.'
          : 'Notification created successfully.');
    } catch (error) {
      console.error('Error submitting notification form:', error);
      setModalError('Failed to save notification. Please try again.');
    } finally {
      setLoading(false);
    }
  };
  
  // Reset notification form
  const resetNotificationForm = () => {
    setNotificationName('');
    setNotificationDescription('');
    setNotificationType('EMAIL');
    setNotificationEnabled(true);
    setNotificationRecipients('');
    setSmtpServer('');
    setSmtpPort('');
    setEmailFrom('');
    setUseAuthentication(false);
    setUsername('');
    setPassword('');
    setPasswordConfirmation('');
    setStartTls(false);
    setSnmpVersion('SNMPv1');
    setSnmpCommunity('');
    setSnmpHosts('');
    setSnmpPort('');
    setSnmpOid('');
    setScriptFilename('');
    setScriptDispatchProperty('');
    setGroupsOption('all');
    setSelectedGroups([]);
    setSelectedSeverities(['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
    setCustomProperties([]);
  };

  // Handle save notification changes
  const handleSaveNotification = async (): Promise<void> => {
    if (!clusterName) return;
    
    try {
      setLoading(true);
      setModalError('');
      setModalSuccess('');
      
      // Count operations
      let createCount = 0;
      let updateCount = 0;
      let deleteCount = 0;
      
      // Collect operations
      const createOperations: Promise<any>[] = [];
      const updateOperations: Promise<any>[] = [];
      const deleteOperations: Promise<any>[] = [];
      
      // Process notifications
      for (const notification of notifications) {
        // Handle deleted notifications
        if (notification.AlertTarget._deleted) {
          if (notification.AlertTarget.id) {
            deleteOperations.push(
              AlertsApi.deleteNotification(clusterName, notification.AlertTarget.id)
            );
            deleteCount++;
          }
          continue;
        }
        
        // Prepare notification data without is_enabled property
        const notificationData: any = {
          name: notification.AlertTarget.name,
          description: notification.AlertTarget.description || '',
          notification_type: notification.AlertTarget.notification_type,
          properties: notification.AlertTarget.properties || {},
          global: notification.AlertTarget.global,
          alert_states: notification.AlertTarget.alert_states || ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
        };
        
        // Add groups if not global
        if (!notificationData.global && notification.AlertTarget.groups && notification.AlertTarget.groups.length > 0) {
          notificationData.groups = notification.AlertTarget.groups;
        }
        
        // Wrap data in AlertTarget object
        const requestData = {
          AlertTarget: notificationData
        };
        
        // Handle new notifications
        if (notification.AlertTarget._isNew) {
          createOperations.push(
            AlertsApi.createNotification(clusterName, requestData)
          );
          createCount++;
          continue;
        }
        
        // Handle modified notifications
        if (notification.AlertTarget._isModified && notification.AlertTarget.id) {
          // Add ID for update
          notificationData.id = notification.AlertTarget.id;
          
          updateOperations.push(
            AlertsApi.updateNotification(clusterName, notification.AlertTarget.id, requestData)
          );
          updateCount++;
        }
      }
      
      // Execute all operations
      await Promise.all([
        ...createOperations,
        ...updateOperations,
        ...deleteOperations
      ]);
      
      // Reload notifications
      const response = await AlertsApi.getNotifications(clusterName);
      if (response && response.items) {
        const fetchedNotifications: AlertNotification[] = response.items.map((item: any) => ({
          AlertTarget: item.AlertTarget
        }));
        setNotifications(fetchedNotifications);
      }
      
      // Reset state
      setSelectedNotification(null);
      setIsSaveDisabled(true);
      setPendingChanges({
        create: 0,
        update: 0,
        delete: 0
      });
      
      // Show results modal
      setResultsNewCount(createCount);
      setResultsUpdatedCount(updateCount);
      setResultsRemovedCount(deleteCount);
      setShowResultsModal(true);
      
      setModalSuccess('Notification changes saved successfully');
    } catch (error) {
      console.error('Error saving notification changes:', error);
      setModalError('Failed to save notification changes');
    } finally {
      setLoading(false);
    }
  };

  // Handle create notification
  const handleCreateNotification = async (): Promise<void> => {
    try {
      setLoading(true);
      setModalError('');
      setModalSuccess('');
      
      // Creating a new notification
      setNotificationFormMode('create');
      setSelectedNotification(null);
      setNotificationName('');
      setNotificationDescription('');
      setNotificationType('EMAIL');
      setNotificationEnabled(true);
      
      // Reset all fields
      setNotificationRecipients('');
      setSmtpServer('');
      setSmtpPort('');
      setEmailFrom('');
      setUseAuthentication(false);
      setUsername('');
      setPassword('');
      setPasswordConfirmation('');
      setStartTls(true); // Set Start TLS to true by default for Email
      setSnmpVersion('SNMPv1');
      setSnmpCommunity('');
      setSnmpHosts('');
      setSnmpPort('');
      setSnmpOid('');
      setScriptFilename('');
      setScriptDispatchProperty('');
      
      // Reset groups and severities
      setGroupsOption('all');
      setSelectedGroups([]);
      setSelectedSeverities(['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
      
      // Reset custom properties
      setCustomProperties([]);
      
      // Show the form modal
      setShowNotificationFormModal(true);
    } catch (error) {
      console.error('Error preparing notification form:', error);
      setModalError('Failed to prepare notification form');
      // Still show the modal even if there's an error
      setShowNotificationFormModal(true);
    } finally {
      setLoading(false);
    }
  };

  // Toggle the gear dropdown
  const toggleGearDropdown = (e: React.MouseEvent): void => {
    e.stopPropagation();
    setIsDropdownOpen(!isDropdownOpen);
  };

  // Close dropdown when clicking outside
  React.useEffect(() => {
    const handleClickOutside = () => {
      if (isDropdownOpen) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [isDropdownOpen]);

  // Handle close modal
  const handleCloseModal = (): void => {
    // Don't clear modalContent as it would close the main Manage Alert Groups modal
    setModalError('');
    setModalSuccess('');
    
    // Reset states for modals
    setShowNewModal(false);
    setShowAddDefinitionModal(false);
    setShowRenameModal(false);
    setShowDuplicateModal(false);
    setShowDeleteConfirmation(false);
    setShowDeleteNotificationModal(false);
    setShowNotificationFormModal(false);
    
    // Reset form states
    setNewGroupName('');
    setRenameGroupName('');
    setDuplicateGroupName('');
    setGroupToRename(null);
    setGroupToDuplicate(null);
    setGroupToDelete(null);
    
    // Don't reset selected group or definition here
  };

  // Handle close main modal
  const handleCloseMainModal = (): void => {
    // Check if there are unsaved changes
    const hasChanges = checkForPendingChanges();
    
    if (hasChanges) {
      // Show confirmation dialog
      if (window.confirm('You have unsaved changes. Are you sure you want to close this modal?')) {
        // Close the modal first
        setShowMainModal(false);
        
        // Reset all states
        setSelectedGroup(null);
        setSelectedDefinition(null);
        
        // Reset all flags in alertGroups
        const resetGroups = alertGroups
          .filter(group => !group.AlertGroup._isNew) // Remove new groups
          .map(group => ({
            ...group,
            AlertGroup: {
              ...group.AlertGroup,
              _isModified: false,
              _deleted: false
            }
          }));
        
        setAlertGroups(resetGroups);
        
        // Disable save button
        setIsSaveDisabled(true);
      }
    } else {
      // No changes, just close the modal
      setShowMainModal(false);
      setSelectedGroup(null);
      setSelectedDefinition(null);
    }
  };

  // Handle close results modal
  const handleCloseResultsModal = (): void => {
    setShowResultsModal(false);
  };

  // Toggle new modal
  const toggleNewModal = (): void => {
    // Don't close the main modal
    setShowNewModal(true);
    setModalError('');
    setModalSuccess('');
    setNewGroupName('');
    setIsNewNameValid(true);
  };

  // Handle rename group
  const handleRenameGroup = (group: AlertGroupItem | AlertGroupState): void => {
    setGroupToRename(group);
    const currentName = getGroupName(group);
    setRenameGroupName(currentName);
    setIsRenameValid(true);
    setShowRenameModal(true);
    setModalError('');
    setModalSuccess('');
  };

  // Handle duplicate group
  const handleDuplicateGroup = (group: AlertGroupItem | AlertGroupState): void => {
    setGroupToDuplicate(group);
    
    // Set initial name for the duplicated group
    const originalName = getGroupName(group);
    const newName = originalName.includes('(Default)') 
      ? originalName.replace('(Default)', '').trim() + ' Copy'
      : originalName + ' Copy';
    
    setDuplicateGroupName(newName);
    
    // Validate the initial name
    if (groupNameExists(newName)) {
      setIsDuplicateNameValid(false);
      setModalError('Alert Group with given name already exists');
    } else {
      setIsDuplicateNameValid(true);
      setModalError('');
    }
    
    setShowDuplicateModal(true);
    setModalSuccess('');
  };

  // Handle notifications change for a specific group
  const handleNotificationsChange = (groupId: number, updatedNotifications: AlertNotification[]): void => {
    const targets = updatedNotifications.flatMap((notification) =>
      notification.AlertTarget.id ? [notification.AlertTarget] : [],
    );
    const updatedGroups = alertGroups.map((group) => group.AlertGroup.id === groupId
      ? {
          ...group,
          AlertGroup: {
            ...group.AlertGroup,
            targets,
            _isModified: !group.AlertGroup._isNew,
          },
        }
      : group,
    );
    setAlertGroups(updatedGroups);
    const updatedGroup = updatedGroups.find((group) => group.AlertGroup.id === groupId);
    if (updatedGroup) handleSelectGroup(updatedGroup);
    setIsSaveDisabled(false);
    setModalError('');
    setModalSuccess('Notification assignments changed. Click Save to apply them.');
  };

  return (
    <>
      <Modal
        isOpen={showDeleteConfirmation}
        onClose={handleCloseModal}
        modalTitle="Confirm Delete"
        modalBody={<DeleteConfirmationModal groupToDelete={groupToDelete} />}
        successCallback={handleConfirmDeleteGroup}
        options={{
          modalSize: 'modal-sm',
          shouldShowFooter: true,
          okButtonText: 'Delete',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'danger',
          okButtonDisabled: false,
        }}
      />
      <div className="btn-group position-relative">
        <button
          className={`btn btn-success dropdown-toggle ${isActionsDropdownOpen ? 'show' : ''}`}
          aria-expanded={isActionsDropdownOpen}
          onClick={(e) => {
            e.stopPropagation();
            setIsActionsDropdownOpen(!isActionsDropdownOpen);
          }}
        >
          ACTIONS&nbsp; <span className="caret"></span>
        </button>
        <ul
          className={`dropdown-menu dropdown-menu-end ${isActionsDropdownOpen ? 'show' : ''}`}
          style={{
            position: 'absolute',
            top: '100%',
            right: 0,
            left: 'auto',
            width: 'max-content',
            minWidth: '12rem',
            maxWidth: 'calc(100vw - 2rem)',
            marginTop: '2px',
          }}
        >
          {actionPolicy.create && (
            <li>
              <button
                className="dropdown-item"
                onClick={() => navigate('/main/alerts/add/1')}
              >
                <FontAwesomeIcon icon={faPlus} />&nbsp; Create Alert
              </button>
            </li>
          )}
          <li>
            <button 
              className="dropdown-item" 
              onClick={handleManageAlertGroups}
            >
              <FontAwesomeIcon icon={faTh} />&nbsp; Manage Alert Groups
            </button>
          </li>
          {/* Manage Notifications - Requires CLUSTER.MANAGE_ALERT_NOTIFICATIONS authorization */}
          {actionPolicy.notifications && (
            <li>
              <button 
                className="dropdown-item" 
                onClick={handleManageNotifications}
              >
                <FontAwesomeIcon icon={faEnvelope} />&nbsp; Manage Notifications
              </button>
            </li>
          )}
          <li>
            <button 
              className="dropdown-item" 
              onClick={handleManageAlertSettings}
            >
              <FontAwesomeIcon icon={faGear} />&nbsp; Manage Alert Settings
            </button>
          </li>
        </ul>
      </div>

      <Modal
        isOpen={showMainModal}
        onClose={handleCloseMainModal}
        modalTitle={modalContent || ''}
        modalBody={renderModalBody()}
        successCallback={
          modalContent === 'Manage Notifications' ? handleSaveNotification :
          modalContent === 'Manage Alert Settings' ? handleSaveGlobalAlertSettings :
          handleSaveChanges
        }
        options={{
          modalSize: modalContent === 'Manage Alert Settings' ? 'modal-sm' : 'modal-lg',
          shouldShowFooter: true,
          okButtonText: 'Save',
          cancelButtonText: modalContent === 'Manage Notifications'?'Close':'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'primary',
          okButtonDisabled: isSaveDisabled,
          cancelableViaSuccessBtn: modalContent === 'Manage Notifications'?false:true
        }}
      />

      <Modal
        isOpen={showNewModal}
        onClose={handleCloseModal}
        modalTitle="Create Alert Group"
        modalBody={
          <NewGroupModal
            newGroupName={newGroupName}
            setNewGroupName={handleNewGroupNameChange}
            modalError={modalError}
            modalSuccess={modalSuccess}
          />
        }
        successCallback={handleCreateGroup}
        options={{
          modalSize: 'modal-sm',
          shouldShowFooter: true,
          okButtonText: 'Create',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'primary',
          okButtonDisabled: !newGroupName.trim() || !isNewNameValid
        }}
      />

      <Modal
        isOpen={showAddDefinitionModal}
        onClose={handleCloseModal}
        modalTitle="Add Alert Definitions"
        modalBody={
          <AddDefinitionsModal
            selectedGroup={selectedGroup}
            availableDefinitions={availableDefinitions}
            selectedDefinitions={selectedDefinitions}
            setSelectedDefinitions={setSelectedDefinitions}
            filteredComponent={filteredComponent}
            setFilteredComponent={setFilteredComponent}
            filteredService={filteredService}
            setFilteredService={setFilteredService}
            modalError={modalError}
            modalSuccess={modalSuccess}
          />
        }
        successCallback={handleAddDefinitions}
        options={{
          modalSize: 'modal-lg',
          shouldShowFooter: true,
          okButtonText: 'Add',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'primary',
          okButtonDisabled: selectedDefinitions.length === 0 || modalError !== ''
        }}
      />

      <Modal
        isOpen={showRenameModal}
        onClose={handleCloseModal}
        modalTitle="Rename Alert Group"
        modalBody={
          <RenameGroupModal
            currentName={groupToRename ? getGroupName(groupToRename) : ''}
            newName={renameGroupName}
            setNewName={handleRenameGroupNameChange}
            modalError={modalError}
            modalSuccess={modalSuccess}
          />
        }
        successCallback={handleRenameConfirm}
        options={{
          modalSize: 'modal-sm',
          shouldShowFooter: true,
          okButtonText: 'Rename',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'primary',
          okButtonDisabled: !renameGroupName.trim() || !isRenameValid
        }}
      />

      <Modal
        isOpen={showDuplicateModal}
        onClose={handleCloseModal}
        modalTitle="Duplicate Alert Group"
        modalBody={
          <DuplicateGroupModal
            currentName={groupToDuplicate ? getGroupName(groupToDuplicate) : ''}
            newName={duplicateGroupName}
            setNewName={handleDuplicateGroupNameChange}
            modalError={modalError}
            modalSuccess={modalSuccess}
          />
        }
        successCallback={handleDuplicateConfirm}
        options={{
          modalSize: 'modal-sm',
          shouldShowFooter: true,
          okButtonText: 'Duplicate',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'primary',
          okButtonDisabled: !duplicateGroupName.trim() || !isDuplicateNameValid
        }}
      />

      <Modal
        isOpen={showDeleteNotificationModal}
        onClose={handleCloseModal}
        modalTitle="Confirm Delete"
        modalBody={<DeleteNotificationModal notification={selectedNotification} errorMessage={modalError} />}
        successCallback={() => selectedNotification && handleDeleteNotification(selectedNotification)}
        options={{
          modalSize: 'modal-sm',
          shouldShowFooter: true,
          okButtonText: 'Delete',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'danger',
        }}
      />

      <Modal
        isOpen={showNotificationFormModal}
        onClose={handleCloseModal}
        modalTitle={
          notificationFormMode === 'create' 
            ? 'Create Alert Notification' 
            : notificationFormMode === 'edit' 
              ? 'Edit Alert Notification' 
              : 'Duplicate Alert Notification'
        }
        modalBody={
          <NotificationFormModal
            name={notificationName}
            setName={setNotificationName}
            description={notificationDescription}
            setDescription={setNotificationDescription}
            notificationType={notificationType}
            setNotificationType={setNotificationType}
            recipients={notificationRecipients}
            setRecipients={setNotificationRecipients}
            smtpServer={smtpServer}
            setSmtpServer={setSmtpServer}
            smtpPort={smtpPort}
            setSmtpPort={setSmtpPort}
            emailFrom={emailFrom}
            setEmailFrom={setEmailFrom}
            useAuthentication={useAuthentication}
            setUseAuthentication={setUseAuthentication}
            username={username}
            setUsername={setUsername}
            password={password}
            setPassword={setPassword}
            passwordConfirmation={passwordConfirmation}
            setPasswordConfirmation={setPasswordConfirmation}
            startTls={startTls}
            setStartTls={setStartTls}
            snmpVersion={snmpVersion}
            setSnmpVersion={setSnmpVersion}
            snmpCommunity={snmpCommunity}
            setSnmpCommunity={setSnmpCommunity}
            snmpHosts={snmpHosts}
            setSnmpHosts={setSnmpHosts}
            snmpPort={snmpPort}
            setSnmpPort={setSnmpPort}
            snmpOid={snmpOid}
            setSnmpOid={setSnmpOid}
            scriptFilename={scriptFilename}
            setScriptFilename={setScriptFilename}
            scriptDispatchProperty={scriptDispatchProperty}
            setScriptDispatchProperty={setScriptDispatchProperty}
            groupsOption={groupsOption}
            setGroupsOption={setGroupsOption}
            selectedGroups={selectedGroups}
            setSelectedGroups={(ids: number[]) => setSelectedGroups(ids)}
            availableGroups={alertGroups
              .filter(group => !group.AlertGroup._deleted)
              .map(group => ({ 
                id: group.AlertGroup.id || 0, 
                name: group.AlertGroup.name 
              }))
            }
            selectedSeverities={selectedSeverities}
            setSelectedSeverities={setSelectedSeverities}
            isEnabled={notificationEnabled}
            setIsEnabled={setNotificationEnabled}
            customProperties={customProperties}
            setCustomProperties={setCustomProperties}
            errorMessage={modalError}
            successMessage={modalSuccess}
            setErrorMessage={setModalError}
            setSuccessMessage={setModalSuccess}
            preserveSensitivePassword={notificationFormMode === 'edit'}
          />
        }
        successCallback={handleNotificationFormSubmit}
        options={{
          modalSize: 'modal-lg',
          shouldShowFooter: true,
          okButtonText: notificationFormMode === 'create' ? 'Create' : 'Save',
          cancelButtonText: 'Cancel',
          cancelableViaIcon: true,
          cancelableViaBtn: true,
          okButtonVariant: 'primary',
          okButtonDisabled: !notificationName.trim() || !isNotificationNameValid,
        }}
      />

      <Modal
        isOpen={showResultsModal}
        onClose={handleCloseResultsModal}
        modalTitle="Alert Groups processing results"
        modalBody={
          <ResultsModal
            newCount={resultsNewCount || pendingChanges.create}
            updatedCount={resultsUpdatedCount || pendingChanges.update}
            removedCount={resultsRemovedCount || pendingChanges.delete}
          />
        }
        successCallback={handleCloseResultsModal}
        options={{
          modalSize: 'modal-md',
          shouldShowFooter: true,
          okButtonText: 'Close',
          cancelableViaBtn: false,
          cancelableViaIcon: true,
          okButtonVariant: 'primary'
        }}
      />
    </>
  );
};
