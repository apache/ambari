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

import { RequestApi } from "../../../api/requestApi";

export interface ComponentValidationResult {
  canAdd: boolean;
  reason?: 'NO_HOSTS_AVAILABLE' | 'OPERATION_IN_PROGRESS' | 'COMPONENT_ALREADY_EXISTS';
  message?: string;
}

/**
 * Validates if a component can be added to the cluster
 * @param componentName - Name of the component to validate
 * @param validDropDownHosts - Array of available hosts for the component
 * @param clusterName - Name of the cluster
 * @param serviceModels - Service models containing component information
 * @param serviceModelName - Name of the service model to check
 * @returns Promise<ComponentValidationResult>
 */
export const validateComponentAddition = async (
  componentName: string,
  validDropDownHosts: string[],
  clusterName: string,
  serviceModels: any,
  serviceModelName: string
): Promise<ComponentValidationResult> => {
  
  // Check if no hosts are available for the component
  if (!validDropDownHosts || validDropDownHosts.length === 0) {
    const componentDisplayName = getComponentDisplayName(componentName);
    return {
      canAdd: false,
      reason: 'NO_HOSTS_AVAILABLE',
      message: `${componentDisplayName} is already installed on all available hosts in the cluster. No additional instances can be added.`
    };
  }

  // Check for ongoing component installation/addition operations
  const hasOngoingOperations = await checkOngoingComponentOperations(componentName, clusterName);
  if (hasOngoingOperations) {
    const componentDisplayName = getComponentDisplayName(componentName);
    return {
      canAdd: false,
      reason: 'OPERATION_IN_PROGRESS',
      message: `A ${componentDisplayName} installation or addition operation is already in progress. Please wait for the current operation to complete before adding another instance.`
    };
  }

  // Additional validation: Check if component already exists on the selected host
  // This prevents duplicate additions to the same host
  const selectedHost = validDropDownHosts[0];
  if (selectedHost && serviceModels && serviceModels[serviceModelName]) {
    const masterComponents = serviceModels[serviceModelName].masterComponents || [];
    const componentExists = masterComponents.some((component: any) => {
      return component.componentName === componentName &&
             component.hostComponents?.some((hc: any) => hc.HostRoles?.host_name === selectedHost);
    });

    if (componentExists) {
      const componentDisplayName = getComponentDisplayName(componentName);
      return {
        canAdd: false,
        reason: 'COMPONENT_ALREADY_EXISTS',
        message: `${componentDisplayName} is already installed on host ${selectedHost}. Please select a different host or refresh the page if this is incorrect.`
      };
    }
  }

  return { canAdd: true };
};

/**
 * Checks if there are ongoing operations for the specified component
 * @param componentName - Name of the component to check
 * @param clusterName - Name of the cluster
 * @returns Promise<boolean>
 */
export const checkOngoingComponentOperations = async (
  componentName: string,
  clusterName: string
): Promise<boolean> => {
  try {
    const runningRequests = await RequestApi.getRunningRequests(clusterName);
    
    if (!runningRequests.items || runningRequests.items.length === 0) {
      return false;
    }

    return runningRequests.items.some((request: any) => {
      const context = request.Requests?.request_context || '';
      const contextLower = context.toLowerCase();
      const componentLower = componentName.toLowerCase();
      
      // Check for various patterns that indicate component addition/installation
      const isComponentOperation = contextLower.includes(componentLower) || 
                                 contextLower.includes(componentLower.replace('_', ' ')) ||
                                 contextLower.includes(componentLower.replace('_', ''));
      
      const isAdditionOperation = contextLower.includes('install') || 
                                contextLower.includes('add') ||
                                contextLower.includes('create') ||
                                contextLower.includes('deploy');
      
      return isComponentOperation && isAdditionOperation;
    });
  } catch (error) {
    console.error('Error checking ongoing component operations:', error);
    // In case of error, err on the side of caution and allow the operation
    // The backend will handle any conflicts
    return false;
  }
};

/**
 * Gets a user-friendly display name for a component
 * @param componentName - Technical component name
 * @returns string - User-friendly display name
 */
const getComponentDisplayName = (componentName: string): string => {
  const displayNameMap: { [key: string]: string } = {
    'ZOOKEEPER_SERVER': 'ZooKeeper Server',
    'HIVE_METASTORE': 'Hive Metastore',
    'HIVE_SERVER': 'HiveServer2',
    'HBASE_MASTER': 'HBase Master',
    'RANGER_KMS_SERVER': 'Ranger KMS Server'
  };

  return displayNameMap[componentName] || componentName.replace(/_/g, ' ');
};

/**
 * Validates multiple components at once
 * @param componentValidations - Array of component validation requests
 * @returns Promise<ComponentValidationResult[]>
 */
export const validateMultipleComponents = async (
  componentValidations: Array<{
    componentName: string;
    validDropDownHosts: string[];
    clusterName: string;
    serviceModels: any;
    serviceModelName: string;
  }>
): Promise<ComponentValidationResult[]> => {
  const results = await Promise.all(
    componentValidations.map(validation => 
      validateComponentAddition(
        validation.componentName,
        validation.validDropDownHosts,
        validation.clusterName,
        validation.serviceModels,
        validation.serviceModelName
      )
    )
  );

  return results;
};
