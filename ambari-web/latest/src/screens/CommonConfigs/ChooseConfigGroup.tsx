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
import { useContext, useEffect, useState, useMemo } from "react";
import Select from "react-select";
import { AppContext } from "../../store/context";
import ConfigGroupApi from "../../api/configGroupApi";
import { get } from "lodash";
import { useAuth } from "../../hooks/useAuth";
import { ClusterCreationContext } from "../ClusterWizard/clusterStore/context";
import { getLocalConfigGroups } from "../../Utils/configGroupUtils";

type ChooseConfigGroupProps = {
  serviceName: string;
  selectedConfigGroup: string;
  onConfigGroupChange: (configGroup: string) => void;
  setShowManageConfigGroupModal: (show: boolean) => void;
  configGroupsData: any[];
  setConfigGroupsData: (data: any[]) => void;
  refetchTrigger?: number;
  hostsList?: string[]; // Add hostsList prop for cluster installation
};

export default function ChooseConfigGroup({
  serviceName,
  selectedConfigGroup,
  onConfigGroupChange,
  setShowManageConfigGroupModal,
  setConfigGroupsData,
  refetchTrigger,
  hostsList
}: ChooseConfigGroupProps) {
  const { clusterName, allHostNames } = useContext(AppContext);
  const clusterCreationContext = useContext(ClusterCreationContext);
  
  // Use hostsList prop if provided (during installation), otherwise use allHostNames (existing cluster)
  const availableHosts = hostsList || allHostNames || [];
  const hostsCount = availableHosts.length;
  const [loading, setLoading] = useState(true);
  const [localConfigGroupsData, setLocalConfigGroupsData] = useState<any[]>([]);
  
  const defaultHostCount = useMemo(() => {
    const defaultGroup = localConfigGroupsData.find((group: any) => 
      get(group, "ConfigGroup.group_name", "") === "Default"
    );
    
    if (defaultGroup) {
      return get(defaultGroup, "ConfigGroup.hosts", []).length;
    }
    
    const assignedHostsCount = localConfigGroupsData
      .filter((group: any) => get(group, "ConfigGroup.group_name", "") !== "Default")
      .reduce((total, group) => {
        return total + get(group, "ConfigGroup.hosts", []).length;
      }, 0);
    return hostsCount - assignedHostsCount;
  }, [hostsCount, localConfigGroupsData]);

  const defaultOption = useMemo(() => ({
    value: "Default",
    label: `Default (${defaultHostCount})`
  }), [defaultHostCount]);
  
  const [selectedGroup, setSelectedGroup] = useState<any>(defaultOption);

  // Authorization hooks - implementing Ember.js config groups authorization patterns
  const { hasAuthorization } = useAuth();
  
  // Check specific authorizations for config groups operations
  const canModifyConfigs = hasAuthorization('SERVICE.MODIFY_CONFIGS');

  const manageConfigGroup = useMemo(
    () => ({
      value: "manageConfigGroup",
      label: "Manage Config Group",
    }),
    []
  );

  const options = useMemo(
    () =>
      localConfigGroupsData
        .filter((group: any) => get(group, "ConfigGroup.group_name", "") !== "Default")
        .map((group: any) => ({
          value: get(group, "ConfigGroup.group_name", ""),
          label:
            get(group, "ConfigGroup.group_name", "") +
            " (" +
            get(group, "ConfigGroup.hosts", []).length +
            ")",
        })),
    [localConfigGroupsData]
  );

  const allOptions = useMemo(
    () => {
      // Only include "Manage Config Group" option if user has SERVICE.MODIFY_CONFIGS permission
      const baseOptions = [defaultOption, ...options];
      return canModifyConfigs ? [manageConfigGroup, ...baseOptions] : baseOptions;
    },
    [manageConfigGroup, defaultOption, options, canModifyConfigs]
  );

  const fetchConfigGroups = async () => {
    try {
      setLoading(true);
      
      if (!clusterName) {
        const serviceConfigGroups = getLocalConfigGroups(
          serviceName, 
          clusterCreationContext?.state, 
          availableHosts
        );
        
        setLocalConfigGroupsData(serviceConfigGroups);
        setConfigGroupsData(serviceConfigGroups);
        setLoading(false);
        return;
      }
      
      const response = await ConfigGroupApi.getConfigGroups(
        clusterName,
        "*"
      );
      
      // Filter config groups for the specific service
      const serviceConfigGroups = response.items.filter((group: any) => 
        get(group, "ConfigGroup.service_name", "") === serviceName ||
        get(group, "ConfigGroup.tag", "") === serviceName
      );
      
      // Use local state to prevent infinite re-renders
      setLocalConfigGroupsData(serviceConfigGroups);
      
      // Only update parent state if needed for ManageConfigGroups modal
      setConfigGroupsData(serviceConfigGroups);
    } catch (error) {
      console.error("Error fetching config groups:", error);
      
      const fallbackConfigGroups = getLocalConfigGroups(
        serviceName, 
        clusterCreationContext?.state, 
        availableHosts
      );
      
      setLocalConfigGroupsData(fallbackConfigGroups);
      setConfigGroupsData(fallbackConfigGroups);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigGroups();
  }, [serviceName, clusterName, refetchTrigger, availableHosts]);

  useEffect(() => {
    if (!clusterName && clusterCreationContext?.state) {
      const serviceConfigGroups = getLocalConfigGroups(
        serviceName, 
        clusterCreationContext.state, 
        availableHosts
      );
      
      setLocalConfigGroupsData(serviceConfigGroups);
      setConfigGroupsData(serviceConfigGroups);
    }
  }, [clusterCreationContext?.state, serviceName, clusterName, availableHosts]);

  useEffect(() => {
    if (!loading && selectedConfigGroup !== selectedGroup?.value) {
      const option = allOptions.find(
        (opt) => opt.value === selectedConfigGroup
      );
      if (option) {
        setSelectedGroup(option);
      }
    }
  }, [selectedConfigGroup, allOptions, loading]);


  useEffect(() => {
    if (selectedGroup?.value === "Default") {
      setSelectedGroup(defaultOption);
    }
  }, [defaultOption]);

  return (
    <>
      <div className="d-flex align-items-center">
        <label className="mx-2">Config Group</label>
        <div>
          <Select
            options={allOptions}
            value={selectedGroup}
            onChange={(selectedOption) => {
              if (selectedOption?.value === "manageConfigGroup") {
                setShowManageConfigGroupModal(true);
              } else {
                if (selectedOption?.value) {
                  setSelectedGroup(selectedOption);
                  onConfigGroupChange(selectedOption.value);
                }
              }
            }}
            className="config-group-select"
            isLoading={loading}
            loadingMessage={() => "Loading config groups..."}
            placeholder={loading ? "Loading..." : "Select config group"}
            menuPortalTarget={document.body}
            styles={{
              menuPortal: (provided) => ({
                ...provided,
                zIndex: 9999,
              }),
            }}
          />
        </div>
      </div>
    </>
  );
}
