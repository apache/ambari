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
import {  useRef, useEffect } from "react";
import { find, get, isEmpty, isObject, merge } from "lodash";
import {  filenameExceptions } from "../constants";
import secureMappingObj from "../data/secure_mappings";
import siteProperties from "../data/BIGTOP/site_properties";
import propertyNameMapping from "../data/configs/propertyNameMapping"


export const useConfigs = (configs?: any, stackServices = []) => {
  const configTagFromFileNameMap = useRef<{ [key: string]: string }>({});
  function getServiceByConfigTypeMap() {
    let ret: any = {};
    stackServices?.forEach(function (s) {
      Object.keys(get(s, "StackServices.config_types", {})).forEach(function (
        ct
      ) {
        ret[ct] = s;
      });
    });
    return ret;
  }

  const serviceByConfigTypeMap = useRef<any>(getServiceByConfigTypeMap());
  const configTypesInfoMap = useRef<any>({});
  function secureConfigsMapping() {
    let ret = {};
    secureMappingObj.forEach(function (sc) {
      //@ts-ignore
      ret[sc.name] = true;
    });
    return ret;
  }
  const secureConfigsMap = useRef<any>(secureConfigsMapping());
  const configsCollectionMap = useRef<any>({});
  const getOriginalFileName = (fileName: string) => {
    if (/\.xml$/.test(fileName)) return fileName;
    return filenameExceptions.includes(fileName) ? fileName : fileName + ".xml";
  };

  useEffect(() => {
    if (stackServices.length) {
      serviceByConfigTypeMap.current = getServiceByConfigTypeMap();
    }
  }, [stackServices]);

  const getConfigTagFromFileName = (fileName: string) => {
    if (configTagFromFileNameMap.current[fileName]) {
      return configTagFromFileNameMap.current[fileName];
    }
    const ret = fileName.endsWith(".xml") ? fileName.slice(0, -4) : fileName;
    configTagFromFileNameMap.current[fileName] = ret;
    return ret;
  };

  const configId = (name: string, fileName: string) => {
    return `${name}__${getConfigTagFromFileName(fileName)}`;
  };

  function getConfig(id: string) {
    return configsCollectionMap.current[id];
  }

  function getConfigByName(name: string, fileName: string) {
    return getConfig(configId(name, fileName));
  }

  function getConfigTypesInfoFromService(serviceName: string) {
    const service = find(stackServices, [
      "StackServices.service_name",
      serviceName,
    ]);
    let configTypes:any = get(service, "StackServices.config_types", {});
    if (configTypesInfoMap.current[serviceName]) {
      // don't recalculate
      return configTypesInfoMap.current[serviceName];
    }
    let configTypesInfo = {
      items: [],
      supportsFinal: [],
      supportsAddingForbidden: [],
    };
    if (configTypes) {
      for (let key in configTypes) {
        if (configTypes.hasOwnProperty(key)) {
          configTypesInfo.items.push(key as never);
          if (
            configTypes[key].supports &&
            configTypes[key].supports.final === "true"
          ) {
            configTypesInfo.supportsFinal.push(key as never);
          }
          if (
            configTypes[key].supports &&
            configTypes[key].supports.adding_forbidden === "true"
          ) {
            configTypesInfo.supportsAddingForbidden.push(key as never);
          }
        }
      }
    }
    configTypesInfoMap.current[serviceName] = configTypesInfo;
    return configTypesInfo;
  }

  function shouldSupportFinal(serviceName: string, filename: string) {
    var unsupportedServiceNames = ["MISC", "Cluster"];
    if (
      !serviceName ||
      unsupportedServiceNames.includes(serviceName) ||
      !filename
    ) {
      return false;
    } else {
      let stackService = find(stackServices, [
        "StackServices.service_name",
        serviceName,
      ]);
      if (!stackService) {
        return false;
      }
      // console.log("Debug",getConfigTypesInfoFromService(stackService))
      return !!find(
        getConfigTypesInfoFromService(serviceName).supportsFinal,
        function (configType) {
          return filename.startsWith(configType);
        }
      );
    }
  }
  function shouldSupportAddingForbidden(serviceName: string, filename: string) {
    var unsupportedServiceNames = ["MISC", "Cluster"];
    if (
      !serviceName ||
      unsupportedServiceNames.includes(serviceName) ||
      !filename
    ) {
      return false;
    } else {
      return !!find(
        getConfigTypesInfoFromService(serviceName).supportsAddingForbidden,
        function (configType) {
          return filename.startsWith(configType);
        }
      );
    }
  }

  function getDefaultCategory(stackConfigProperty: boolean, fileName: string) {
    return (
      (stackConfigProperty ? "Advanced " : "Custom ") +
      getConfigTagFromFileName(fileName)
    );
  }

  function getIsSecure(propertyName: string) {
    return secureConfigsMap.current[propertyName];
  }

  function getDefaultDisplayType(value: any) {
    return value && value.trim().indexOf("\n") !== -1 ? "multiLine" : "string";
  }

  function createDefaultConfig(
    name: string,
    fileName: string,
    definedInStack: boolean,
    coreObject: any
  ) {
    let service =
      serviceByConfigTypeMap.current[getConfigTagFromFileName(fileName)];
    let serviceName = service
      ? get(service, "StackServices.service_name")
      : "MISC";
    const currentDisplayType =
      propertyNameMapping[name]?.displayType ||
      find(siteProperties.configProperties, ["name", name])?.displayType;
    const currentCategory =
      propertyNameMapping[name]?.category ||
      find(siteProperties.configProperties, ["name", name])?.category;
    var tpl = {
      /** core properties **/
      id: configId(name, fileName),
      name: name,
      filename: getOriginalFileName(fileName),
      value: "",
      savedValue: null,
      isFinal: false,
      savedIsFinal: null,
      /** UI and Stack properties **/
      recommendedValue: null,
      recommendedIsFinal: null,
      supportsFinal: shouldSupportFinal(serviceName, fileName),
      supportsAddingForbidden: shouldSupportAddingForbidden(
        serviceName,
        fileName
      ),
      serviceName: serviceName,
      displayName: propertyNameMapping[name]?.displayName || name,
      displayType:
        coreObject &&
        coreObject.propertyType &&
        coreObject.propertyType.contains("PASSWORD")
          ? "password"
          : currentDisplayType ||
            getDefaultDisplayType(!isEmpty(coreObject) ? coreObject.value : ""),
      description: "",
      category: currentCategory || getDefaultCategory(definedInStack, fileName),
      isSecureConfig: getIsSecure(name),
      showLabel: true,
      isVisible: true,
      isUserProperty: !definedInStack,
      isRequired: definedInStack,
      group: null,
      isRequiredByAgent: true,
      isReconfigurable: true,
      unit: null,
      hasInitialValue: false,
      isOverridable: true,
      index: Infinity,
      dependentConfigPattern: null,
      options: null,
      radioName: null,
      widgetType: null,
      errorMessage: "",
      warnMessage: "",
    };
    return Object.keys(coreObject || {}).length ? merge(tpl, coreObject) : tpl;
  }

  function getDefaultConfig(name: string, fileName: string, coreObject: any) {
    name = JSON.parse('"' + name + '"');
    let cfg =
      getConfigByName(name, fileName) ||
      createDefaultConfig(name, fileName, false, coreObject);
    if (isObject(coreObject) && !isEmpty(coreObject)) {
      cfg = coreObject;
    }
    return cfg;
  }

  function trimProperty(property: any) {
    var displayType = get(property, "displayType");
    var value = get(property, "value");
    var name = get(property, "name");
    var rez;
    switch (displayType) {
      case "directories":
      case "directory":
        rez = value.replace(/,/g, " ").trim().split(/\s+/g).join(",");
        break;
      case "host":
        rez = value.trim();
        break;
      case "password":
        break;
      default:
        if (
          name == "javax.jdo.option.ConnectionURL" ||
          name == "oozie.service.JPAService.jdbc.url"
        ) {
          rez = value.trim();
        }
        rez = typeof value == "string" ? value.replace(/(\s+$)/g, "") : value;
    }
    return rez == "" || rez == undefined ? value : rez;
  }

  function formatPropertyValue(serviceConfigProperty: any, originalValue: any) {
    // console.log("Property is",serviceConfigProperty.name);
    let value = !originalValue
      ? get(serviceConfigProperty, "value")
      : originalValue;
    let displayType =
      get(serviceConfigProperty, "displayType") ||
      get(serviceConfigProperty, "valueAttributes.type");
    console.log("Display Type for", serviceConfigProperty.name, displayType);

    // if (get(serviceConfigProperty, 'name') === 'kdc_type') {
    //   return App.router.get('mainAdminKerberosController.kdcTypesValues')[value];
    // }
    if (/^\s+$/.test("" + value)) {
      return " ";
    }
    switch (displayType) {
      case "int":
        if (/\d+m$/.test(value)) {
          return value.slice(0, value.length - 1);
        } else {
          var int = parseInt(value);
          return isNaN(int) ? "" : int.toString();
        }
      case "float":
        var float = parseFloat(value);
        return isNaN(float) ? "" : float.toString();
      case "componentHosts":
        if (typeof value == "string") {
          return value.replace(/\[|]|'|&apos;/g, "").split(",");
        }
        return value;
      case "content":
      case "string":
      case "multiLine":
      case "directories":
      case "directory":
        return trimProperty({ displayType: displayType, value: value });
      default:
        return value;
    }
  }

  function getConfigsFromJSON(configJSON: any) {
    let configs = [],
      filename = getOriginalFileName(configJSON.type),
      properties = configJSON.properties,
      attributes: any = [];
    [
      "FINAL",
      "PASSWORD",
      "USER",
      "GROUP",
      "TEXT",
      "ADDITIONAL_USER_PROPERTY",
      "NOT_MANAGED_HDFS_PATH",
      "VALUE_FROM_PROPERTY_FILE",
    ].forEach(function (attribute) {
      var json: any = {};
      json[attribute] =
        get(configJSON, "properties_attributes." + attribute.toLowerCase()) ||
        {};
      attributes.push(json);
    });

    for (let index in properties) {
      let serviceConfigObj = getDefaultConfig(index, filename, {});
      if (serviceConfigObj.isRequiredByAgent !== false) {
        serviceConfigObj.value = serviceConfigObj.savedValue =
          formatPropertyValue(serviceConfigObj, properties[index]);
        serviceConfigObj.isFinal = serviceConfigObj.savedIsFinal =
          attributes[0]["FINAL"][index] === "true";

        var propertyType = [];
        // iterate through all the attributes, except for FINAL
        for (var i = 1; i < attributes.length; i++) {
          for (var type in attributes[i]) {
            if (attributes[i][type][index] === "true") {
              propertyType.push(type);

              if (type === "PASSWORD") {
                serviceConfigObj.displayType = "password";
              }
            }
          }
        }
        serviceConfigObj.propertyType = propertyType;
        serviceConfigObj.isEditable = serviceConfigObj.isReconfigurable;
      }

      configs.push(serviceConfigObj);
    }
    return configs;
  }

  return {
    configs,
    getOriginalFileName,
    getConfigTagFromFileName,
    configId,
    getConfigByName,
    getDefaultConfig,
    getConfigsFromJSON,
    createDefaultConfig
  };
};
