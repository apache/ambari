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

import { useContext, useState } from "react";
import { AppContext } from "../store/context";
import { cloneDeep, flatten, forEach, get, map, sortBy, startCase } from "lodash";
import { groupPropertyValues } from "../Utils/dataUtils";
import { blueprintUtils } from "../screens/ClusterWizard/utils";
import {
  buildConfigsJSON,
  getConfigPropertyByName,
} from "../screens/CommonConfigs/ConfigUtils";
import ConfigsApi from "../api/configsApi";
import toast from "react-hot-toast";
import { ConfigPropertiesType } from "../screens/CommonConfigs/types";
import WizardApi from "../api/wizardApi";

function useServerValidation(
  hostComponents: any,
  configProperties: any,
  serviceName: string,
  validationCallback: any
) {
  const [validationErrors, setValidationErrors] = useState<any>([]);
  const { allHostNames, services, cluster } = useContext(AppContext);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const stackName = get(cluster, "stack");
  const stackVersion = get(cluster, "versionNum");


  const processStackLevelConfigurations = (
      updatedConfigProperties: ConfigPropertiesType,
      stackLevelConfigs : any
    ) => {
  
      const result = cloneDeep(updatedConfigProperties);
  
      if (stackLevelConfigs) {
        const serviceName = "CLUSTER-ENV";
        result[serviceName] = {};
  
        stackLevelConfigs?.configurations?.forEach((config: any) => {
          const fileName = config.StackLevelConfigurations.type as string;
          const configType = fileName.slice(0, -4);
          const propertyName = config.StackLevelConfigurations
            .property_name as string;
          const propertyType = config.StackLevelConfigurations.property_type;
  
          if (!result[serviceName][configType]) {
            result[serviceName][configType] = {
              errors: 0,
              properties: {},
            };
          }
  
          result[serviceName][configType].properties[propertyName] = {
            propertyName: propertyName,
            ...(config.StackLevelConfigurations.property_display_name && {
              propertyDisplayname:
                config.StackLevelConfigurations.property_display_name,
            }),
            propertyDescription:
              config.StackLevelConfigurations.property_description,
            propertyValue: config.StackLevelConfigurations.property_value,
            propertyAttributes:
              config.StackLevelConfigurations.property_value_attributes,
            previousValue: config.StackLevelConfigurations.property_value,
            value: config.StackLevelConfigurations.property_value,
            final: config.StackLevelConfigurations.final
              ? config.StackLevelConfigurations.final
              : "",
            propertyType: propertyType ? propertyType : [],
            fileName: fileName,
            type: configType,
            serviceName: serviceName,
            isEditable: true,
            isHidden : true,
            isVisible: false,
          };
      })}
  
      return result;
    };

  async function validateConfigProperties() {
    try {
      const hosts = allHostNames;
      const allHostComponents = flatten(map(hostComponents, "host_components"));
      const components = flatten(map(allHostComponents, "HostRoles"));
      console.log("Here Components", components);
      forEach(components, (comp: any) => {
        comp.hostName = comp.host_name;
        comp.componentName = comp.component_name;
      });

      const mappedComponents = groupPropertyValues(components, "hostName");
      const hostMapping = [];
      for (const host in mappedComponents) {
        hostMapping.push({
          hostname: host,
          components: mappedComponents[host].map((comp: any) => {
            return { name: comp.component_name };
          }),
        });
      }


      const updatedConfigProperties = processStackLevelConfigurations(configProperties, await WizardApi.getStackLevelConfigurations(
      stackName,
      stackVersion,
      "configurations/*,Versions/config_types/*"
    ));

      const blueprint: any = blueprintUtils.getBlueprint(hosts, hostMapping);
      blueprint.blueprint.configurations = buildConfigsJSON(updatedConfigProperties);
      const payload: any = {
        hosts,
        recommendations: blueprint,
        services: selectedServices,
        validate: "configurations",
      };
      const validationsResponse = await ConfigsApi.validateConfigProperties(
        stackName,
        stackVersion,
        payload
      );
      const { resources } = validationsResponse;
      const [validationResult] = resources || [];
      const { items: validationItems } = validationResult;
      let updatedItems = [];
      const processedItems = new Map(); // Track processed items with their details to avoid duplicates
      
      for (const item of validationItems) {
        const property = getConfigPropertyByName(
          item?.["config-name"],
          updatedConfigProperties
        );
        
        // Apply Ember.js filtering logic: only show validation errors for properties that exist and are visible
        if (property && property.isVisible !== false && !property.isHidden) {
          // Create unique key for deduplication using property ID (same as Ember.js)
          const propertyId = `${property.propertyName}_${property.fileName || property.type + '.xml'}`;
          
          // Skip if we've already processed this property (deduplication like Ember.js)
          if (processedItems.has(propertyId)) {
            continue;
          }
          
          // Map server level to display type (same as Ember.js errorTypes mapping)
          let displayType = startCase(item?.level);
          let className = "bg-warning-subtle";
          
          if (item.level === "NOT_APPLICABLE") {
            displayType = "Critical";
            className = "bg-danger-super-subtle";
          } else if (item.level === "ERROR") {
            displayType = "Error";
            className = "bg-danger-super-subtle";
          } else if (item.level === "WARN") {
            displayType = "Warning";
            className = "bg-warning-subtle";
          }
          
          const processedItem = {
            ...item,
            ...{
              type: displayType,
              service: property?.serviceName || serviceName,
              property: property?.propertyName || item["config-name"],
              currentValue: property?.value || "",
              message: item.message,
              className: className,
            },
          };
          
          processedItems.set(propertyId, processedItem);
          updatedItems.push(processedItem);
        }
        // If property doesn't exist in current configuration, skip it (same as Ember.js behavior)
      }
      if (updatedItems.length) {
        validationCallback(sortBy(updatedItems, ["type"]));
        setValidationErrors(sortBy(updatedItems, ["type"]));
      } else {
        setValidationErrors([]);
        validationCallback([]);
      }
    } catch (error: any) {
      toast.error(error.message);
      validationCallback([]);
    }
  }
  return { validationErrors, validateConfigProperties };
}

export default useServerValidation;
