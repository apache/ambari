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

import { useContext, useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import { hbase_properties } from "../../data/configs/services/hbase_properties";
import { hdfs_properties } from "../../data/configs/services/hdfs_properties";
import { hive_properties } from "../../data/configs/services/hive_properties";
import { mapreduce2_properties } from "../../data/configs/services/mapreduce2_properties";
import { ranger_properties } from "../../data/configs/services/ranger_properties";
import { tez_properties } from "../../data/configs/services/tez_properties";
import { yarn_properties } from "../../data/configs/services/yarn_properties";
import { zookeeper_properties } from "../../data/configs/services/zookeeper_properties";
import { kerberos_properties } from "../../data/configs/services/kerberos_properties";
import { cloneDeep, get, isEmpty, map } from "lodash";
import { Alert, Badge, Button, Card, Form } from "react-bootstrap";
import Config from "../CommonConfigs/Config";
import {
  fetchComponentHostNamesByComponent,
  formatPropertyValue,
  getConfigCategories,
} from "../CommonConfigs/ConfigUtils";
import { AppContext } from "../../store/context";
import WizardApi from "../../api/wizardApi";
import ConfigsApi from "../../api/configsApi";
import ConfigGroupApi from "../../api/configGroupApi";
import { HostsApi } from "../../api/hostsApi";
import Spinner from "../../components/Spinner";
import { ServiceContext } from "../../store/ServiceContext";
import { serviceNameModelMapping } from "../../constants";
import Modal from "../../components/Modal";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import { messages } from "../messages";
import {
  buildConfigGroupMembershipUpdates,
  buildHostConfigGroupState,
} from "../../Utils/hostConfigs";
import {
  classifyDefaultThemeResponse,
  describeThemeRequestError,
  ThemeLoadNotice,
} from "../CommonConfigs/themeLoadUtils";

export default function Hostconfigs() {
  const params = useParams();
  const hostName = params.hostname;

  const [loading, setLoading] = useState<boolean>(true);
  const [themes, setThemes] = useState<any>({});
  const [configs, setConfigs] = useState<any>({});
  const [configProperties, setConfigProperties] = useState({});
  const [propertyValues, setPropertyValues] = useState<any>({});
  const [showChangeConfigGroupModal, setShowChangeConfigGroupModal] = useState<boolean>(false);
  const [selectedConfigGroup, setSelectedConfigGroup] = useState<string>("Default");
  const [configGroup, setConfigGroup] = useState<string>("Default");
  const [groupsByService, setGroupsByService] = useState<Record<string, any[]>>({});
  const [assignedGroupByService, setAssignedGroupByService] = useState<Record<string, string>>({});
  const [currentService, setCurrentService] = useState<string>("");
  const [hostServices, setHostServices] = useState<string[]>([]);
  const [hostComponentsByService, setHostComponentsByService] = useState<
    Record<string, string[]>
  >({});
  const [loadError, setLoadError] = useState("");
  const [themeLoadNotice, setThemeLoadNotice] =
    useState<ThemeLoadNotice | null>(null);
  const [themeLoading, setThemeLoading] = useState(false);
  const [groupChangeError, setGroupChangeError] = useState("");
  const [isChangingGroup, setIsChangingGroup] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const themeRequestId = useRef(0);

  const { clusterName, services, cluster } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const { isAuthorized } = useAuthorizationPolicy();
  const stackName = get(cluster, "stack");
  const stackVersion = get(cluster, "versionNum");
  const canManageConfigGroups = isAuthorized("SERVICE.MANAGE_CONFIG_GROUPS");

  const serviceInCluster = map(services, "ServiceInfo.service_name");
  const serviceOrderKey = serviceInCluster.join(",");

  const propertiesFileMap: { [key: string]: any } = {
    HDFS: hdfs_properties,
    YARN: yarn_properties,
    HIVE: hive_properties,
    HBASE: hbase_properties,
    RANGER: ranger_properties,
    MAPREDUCE2: mapreduce2_properties,
    TEZ: tez_properties,
    ZOOKEEPER: zookeeper_properties,
    KERBEROS: kerberos_properties,
  };

  useEffect(() => {
    let active = true;
    if (!clusterName || !hostName || !stackName || !stackVersion) {
      return () => {
        active = false;
      };
    }

    const loadHostConfigurations = async () => {
      setLoading(true);
      setLoadError("");
      setThemeLoadNotice(null);
      try {
        const hostResponse = await HostsApi.getHostData(
          clusterName,
          hostName,
          "host_components/HostRoles/service_name,host_components/HostRoles/component_name,host_components/HostRoles/display_name",
        );
        const rawHostComponents = get(hostResponse, "host_components", []);
        const hostComponents = Array.isArray(rawHostComponents)
          ? rawHostComponents
          : [];
        const componentsByService = hostComponents.reduce(
          (result: Record<string, string[]>, component: unknown) => {
            const serviceName = get(component, "HostRoles.service_name");
            const componentName = get(component, "HostRoles.component_name");
            if (!serviceName || !componentName) return result;
            if (!result[serviceName]) result[serviceName] = [];
            if (!result[serviceName].includes(componentName)) {
              result[serviceName].push(componentName);
            }
            return result;
          },
          {},
        );
        const componentServices = Object.keys(componentsByService);
        const servicesOnHost = componentServices.sort((left, right) => {
          const leftIndex = serviceInCluster.indexOf(left);
          const rightIndex = serviceInCluster.indexOf(right);
          return (leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex)
            - (rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex);
        });

        if (!active) return;
        setHostServices(servicesOnHost);
        setHostComponentsByService(componentsByService);
        if (!servicesOnHost.length) {
          setCurrentService("");
          setConfigs({});
          setThemes({});
          setPropertyValues({});
          setGroupsByService({});
          setAssignedGroupByService({});
          return;
        }

        const serviceNames = servicesOnHost.join(",");
        const [configResponse, themeResult, groupResponse, valueResponse] = await Promise.all([
          WizardApi.getStackConfigurations(
            stackName,
            stackVersion,
            serviceNames,
            "configurations/*,configurations/dependencies/*,StackServices/config_types/*",
          ),
          WizardApi.getStackThemes(
            stackName,
            stackVersion,
            serviceNames,
            "themes/*",
          )
            .then((response) => ({
              response,
              notice: classifyDefaultThemeResponse(response, servicesOnHost),
            }))
            .catch((error: unknown) => ({
              response: { items: [] },
              notice: {
                kind: "request" as const,
                message: describeThemeRequestError(
                  error,
                  "The host configuration Theme request failed.",
                ),
              },
            })),
          ConfigGroupApi.getConfigGroupsForServices(clusterName, servicesOnHost),
          ConfigsApi.getConfigValues(clusterName, serviceNames),
        ]);

        if (!active) return;
        const groupState = buildHostConfigGroupState(
          groupResponse.items || [],
          servicesOnHost,
          hostName,
        );
        const firstService = servicesOnHost[0];
        const firstGroup = groupState.assignedGroupByService[firstService] || "Default";
        setThemes(themeResult.response);
        setThemeLoadNotice(themeResult.notice);
        setPropertyValues(valueResponse);
        setGroupsByService(groupState.groupsByService);
        setAssignedGroupByService(groupState.assignedGroupByService);
        setCurrentService(firstService);
        setConfigGroup(firstGroup);
        setSelectedConfigGroup(firstGroup);
        setConfigs(configResponse);
      } catch (error: any) {
        if (active) {
          setLoadError(
            error?.response?.data?.message || "Ambari could not load host configurations.",
          );
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void loadHostConfigurations();
    return () => {
      active = false;
      themeRequestId.current += 1;
    };
  }, [clusterName, hostName, serviceOrderKey, stackName, stackVersion, retryCount]);

  useEffect(() => {
    if (!isEmpty(configs)) {
      getConfigProperties();
    }
  }, [
    configs,
    propertyValues,
    JSON.stringify(allServiceModels),
    JSON.stringify(hostComponentsByService),
    themes,
  ]);

  const retryThemes = async () => {
    if (!hostServices.length || !stackName || !stackVersion) return;
    const requestId = ++themeRequestId.current;
    setThemeLoading(true);
    try {
      const response = await WizardApi.getStackThemes(
        stackName,
        stackVersion,
        hostServices.join(","),
        "themes/*",
      );
      if (requestId !== themeRequestId.current) return;
      setThemes(response);
      setThemeLoadNotice(
        classifyDefaultThemeResponse(response, hostServices),
      );
    } catch (error: unknown) {
      if (requestId !== themeRequestId.current) return;
      setThemeLoadNotice({
        kind: "request",
        message: describeThemeRequestError(
          error,
          "The host configuration Theme request failed.",
        ),
      });
    } finally {
      if (requestId === themeRequestId.current) setThemeLoading(false);
    }
  };

  const onServiceChange = (selectedService: string) => {
    if (selectedService !== currentService && hostServices.includes(selectedService)) {
      handleServiceChange(selectedService);
    }
  };

  const getConfigProperties = () => {
    let configPropertiesCopy: ConfigPropertiesType = {};
    let updatedConfigProperties: ConfigPropertiesType = {};

    configs?.items?.forEach((service: any) => {
      service.configurations?.forEach((config: any) => {
        const fileName = config.StackConfigurations.type as string;
        const configType = fileName.slice(0, -4);
        const propertyName = config.StackConfigurations.property_name as string;
        const serviceName = config.StackConfigurations.service_name;
        const propertyType = config.StackConfigurations.property_type;

        if (!configPropertiesCopy[serviceName]) {
          configPropertiesCopy[serviceName] = {};
        }
        if (!configPropertiesCopy[serviceName][configType]) {
          configPropertiesCopy[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        configPropertiesCopy[serviceName][configType].properties[propertyName] =
        {
          propertyName: propertyName,
          ...(config.StackConfigurations.property_display_name && {
            propertyDisplayname:
              config.StackConfigurations.property_display_name,
          }),
          propertyValue: config.StackConfigurations.property_value,
          propertyAttributes:
            config.StackConfigurations.property_value_attributes,
          previousValue: config.StackConfigurations.property_value,
          value: config.StackConfigurations.property_value,
          final: config.StackConfigurations.final
            ? config.StackConfigurations.final
            : "",
          fileName: fileName,
          propertyType: propertyType ? propertyType : [],
          type: configType,
          isEditable: false,
          isVisible: true,
        };

        if (
          configPropertiesCopy[serviceName][configType].properties[propertyName]
            .propertyAttributes.type == "password"
        ) {
          configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ] = {
            ...configPropertiesCopy[serviceName][configType].properties[
            propertyName
            ],
            confirmPassword: config.StackConfigurations.property_value,
          };
        }

        if (
          propertyType &&
          (propertyType.includes("USER") || propertyType.includes("GROUP"))
        ) {
          delete configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ];
        }
      });
    });

    Object.keys(configPropertiesCopy).map((serviceName: string) => {
      Object.keys(configPropertiesCopy[serviceName]).map(
        (configType: string) => {
          if (!!!configType.endsWith("env")) {
            configPropertiesCopy[serviceName]["Custom " + configType] = {
              errors: 0,
              properties: {},
              displayName: "Custom " + configType,
            };
          }
        }
      );
    });

    if (!isEmpty(propertyValues)) {
      const defaultItems = propertyValues?.items?.filter(
        (item: any) => item.group_name === "Default"
      );
      const otherItems = propertyValues?.items?.filter(
        (item: any) => item.group_name !== "Default"
      );

      defaultItems?.forEach((item: any) => {
        item?.configurations?.forEach((config: any) => {
          const type = config.type;
          const properties = config.properties;
          const serviceName = get(item, "service_name", "");

          Object.keys(properties).forEach((propertyName: string) => {
            if (configPropertiesCopy[serviceName]?.[type]) {
              if (
                configPropertiesCopy[serviceName][type]?.properties[
                propertyName
                ]
              ) {
                configPropertiesCopy[serviceName][type].properties[
                  propertyName
                ].value = formatPropertyValue(
                  configPropertiesCopy[serviceName][type]?.properties[
                  propertyName
                  ],
                  properties[propertyName]
                );
                configPropertiesCopy[serviceName][type].properties[
                  propertyName
                ].previousValue = formatPropertyValue(
                  configPropertiesCopy[serviceName][type]?.properties[
                  propertyName
                  ],
                  properties[propertyName]
                );

                if (
                  configPropertiesCopy[serviceName][type].properties[
                    propertyName
                  ].propertyAttributes.type === "password"
                ) {
                  configPropertiesCopy[serviceName][type].properties[
                    propertyName
                  ].confirmPassword =
                    configPropertiesCopy[serviceName][type].properties[
                      propertyName
                    ].value;
                }
              } else {
                if (configPropertiesCopy[serviceName]["Custom " + type]) {
                  configPropertiesCopy[serviceName][
                    "Custom " + type
                  ].properties[propertyName] = {
                    propertyName: propertyName,
                    propertyDisplayname: propertyName,
                    propertyValue: properties[propertyName],
                    propertyAttributes: {},
                    previousValue: properties[propertyName],
                    value: properties[propertyName],
                    final: "",
                    fileName: type + ".xml",
                    propertyType: [],
                    type: type,
                    isEditable: false,
                  };
                }
              }
            }
          });
        });
      });
      otherItems?.forEach((item: any) => {
        item?.configurations?.forEach((config: any) => {
          const type = config.type;
          const properties = config.properties;
          const serviceName = get(item, "service_name", "");
          const groupName = get(item, "group_name", "");

          Object.keys(properties).forEach((propertyName: string) => {
            if (configPropertiesCopy[serviceName]?.[type]) {
              if (
                configPropertiesCopy[serviceName][type]?.properties[
                propertyName
                ]
              ) {
                if (
                  !configPropertiesCopy[serviceName][type].properties[
                    propertyName
                  ].overrideValues
                ) {
                  configPropertiesCopy[serviceName][type].properties[
                    propertyName
                  ].overrideValues = [];
                }
                configPropertiesCopy[serviceName][type].properties[
                  propertyName
                ].overrideValues.push({
                  value: formatPropertyValue(
                    configPropertiesCopy[serviceName][type]?.properties[
                    propertyName
                    ],
                    properties[propertyName]
                  ),
                  groupName: groupName,
                  previousValue: formatPropertyValue(
                    configPropertiesCopy[serviceName][type]?.properties[
                    propertyName
                    ],
                    properties[propertyName]
                  ),
                });
              } else {
                const customSection =
                  configPropertiesCopy[serviceName]["Custom " + type];
                if (!customSection) return;
                if (!customSection.properties[propertyName]) {
                  customSection.properties[propertyName] = {
                    propertyName: propertyName,
                    propertyDisplayname: propertyName,
                    propertyValue: "Undefined",
                    propertyAttributes: { type: "undefined" },
                    previousValue: "Undefined",
                    value: "Undefined",
                    final: "",
                    fileName: type + ".xml",
                    propertyType: [],
                    type: type,
                    isEditable: false,
                  };
                }

                const customProperty = customSection.properties[propertyName];
                if (!customProperty.overrideValues) {
                  customProperty.overrideValues = [];
                }
                customProperty.overrideValues?.push({
                  value: properties[propertyName],
                  groupName: groupName,
                  previousValue: properties[propertyName],
                });
              }
            }
          });
        });
      });
    }

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!updatedConfigProperties[serviceName]) {
        updatedConfigProperties[serviceName] = {};
      }

      const componentsOnHost = new Set(hostComponentsByService[serviceName] || []);
      const serviceConfigCategories = getConfigCategories(serviceName).filter(
        (category) => !category.showHost || componentsOnHost.has(category.name),
      );
      serviceConfigCategories.forEach((category) => {
        if (!updatedConfigProperties[serviceName][category.name]) {
          updatedConfigProperties[serviceName][category.name] = {
            errors: 0,
            properties: {},
            displayName: category.displayName,
          };
        }

        const isMasterComponent = allServiceModels[
          serviceNameModelMapping[serviceName.toUpperCase()]
        ]?.masterComponents?.some(
          (component: any) => component.componentName === category.name
        );

        const isSlaveComponent = allServiceModels[
          serviceNameModelMapping[serviceName.toUpperCase()]
        ]?.slaveComponents?.some(
          (component: any) => component.componentName === category.name
        );

        if (isMasterComponent || isSlaveComponent) {
          updatedConfigProperties[serviceName][category.name].properties[
            category.name.toLowerCase() + "_hosts"
          ] = {
            propertyName: category.name.toLowerCase() + "_hosts",
            propertyDisplayname: category.displayName + " hosts",
            propertyValue: "",
            propertyAttributes: {
              type: "hosts",
            },
            previousValue: "",
            value: isMasterComponent
              ? fetchComponentHostNamesByComponent(
                allServiceModels[serviceNameModelMapping[serviceName.toUpperCase()]].masterComponents,
                category.name
              )
              : fetchComponentHostNamesByComponent(
                allServiceModels[serviceNameModelMapping[serviceName.toUpperCase()]].slaveComponents,
                category.name
              ),
            fileName: serviceName + "-site.xml",
            final: "",
            propertyType: [],
            type: category.name,
            isEditable: false,
          };
        }
      });
    });

    Object.keys(propertiesFileMap).map((service: string) => {
      if (configPropertiesCopy[service]) {
        propertiesFileMap[service].forEach((property: any) => {
          const { serviceName, filename, name, category } = property;
          if (!category) {
            return;
          }
          const configType = filename.slice(0, -4);
          const isComponentOnlyCategory = getConfigCategories(serviceName).some(
            (configCategory) =>
              configCategory.name === category && configCategory.showHost,
          );
          if (
            isComponentOnlyCategory &&
            !(hostComponentsByService[serviceName] || []).includes(category)
          ) {
            return;
          }

          if (configPropertiesCopy[serviceName][configType]?.properties[name]) {
            if (!category.includes("Advanced")) {
              if (!updatedConfigProperties[serviceName]) {
                updatedConfigProperties[serviceName] = {};
              }
              if (!updatedConfigProperties[serviceName][category]) {
                updatedConfigProperties[serviceName][category] = {
                  errors: 0,
                  properties: {},
                };
              }

              updatedConfigProperties[serviceName][category].properties[name] =
                configPropertiesCopy[serviceName][configType].properties[name];

              delete configPropertiesCopy[serviceName][configType].properties[
                name
              ];
            }
          }
        });
      }
    });

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!updatedConfigProperties[serviceName]) {
        updatedConfigProperties[serviceName] = {};
      }
      Object.keys(configPropertiesCopy[serviceName]).forEach((configType) => {
        if (!updatedConfigProperties[serviceName][configType]) {
          updatedConfigProperties[serviceName][configType] = {
            errors: 0,
            properties: {},
            displayName: !configType.includes("Custom")
              ? "Advanced " + configType
              : configType,
          };
        }
        Object.keys(
          configPropertiesCopy[serviceName][configType].properties
        ).forEach((propertyName) => {
          updatedConfigProperties[serviceName][configType].properties[
            propertyName
          ] =
            configPropertiesCopy[serviceName][configType].properties[
            propertyName
            ];
        });
      });
    });

    updatedConfigProperties = onLoadOverrides(updatedConfigProperties);
    updatedConfigProperties = setVisibilityForKerberosProperties(
      updatedConfigProperties
    );

    Object.values(updatedConfigProperties).forEach((serviceConfigs) => {
      Object.values(serviceConfigs).forEach((section) => {
        Object.values(section.properties).forEach((property) => {
          property.isEditable = false;
        });
      });
    });

    setConfigProperties(updatedConfigProperties);
  };

  const onLoadOverrides = (updatedConfigProperties: ConfigPropertiesType) => {
    const isRangerPresent = services.some(
      (service) => service.ServiceInfo.service_name === "RANGER"
    );

    let configs = updatedConfigProperties;

    if (!isRangerPresent) {
      configs = removeRangerConfigs(configs);
    }

    return configs;
  };

  const removeRangerConfigs = (configProps: ConfigPropertiesType) => {
    const updatedConfigs = { ...configProps };

    Object.keys(updatedConfigs).forEach((serviceName) => {
      Object.keys(updatedConfigs[serviceName]).forEach((sectionName) => {
        if (sectionName.toLowerCase().includes("ranger")) {
          delete updatedConfigs[serviceName][sectionName];
        }
      });
    });

    return updatedConfigs;
  };

  const handleServiceChange = (selectedService: string) => {
    setCurrentService(selectedService);
    const assignedGroup = assignedGroupByService[selectedService] || "Default";
    setConfigGroup(assignedGroup);
    setSelectedConfigGroup(assignedGroup);
  };

  const setVisibilityForKerberosProperties = (
    configProps: ConfigPropertiesType
  ) => {
    const updatedConfigs = cloneDeep(configProps);

    if (!serviceInCluster.includes("KERBEROS")) {
      return updatedConfigs;
    }

    const kdcType = Object.values(updatedConfigs["KERBEROS"])
      .flatMap((section) => Object.values(section.properties))
      .find((prop) => prop.propertyName === "kdc_type");

    if (!kdcType) {
      return updatedConfigs;
    }

    const updatePropertyVisibility = (
      propertyName: string,
      isVisible: boolean
    ) => {
      Object.values(updatedConfigs["KERBEROS"]).forEach((section) => {
        const property = section.properties[propertyName];
        if (property) {
          property.isVisible = isVisible;
        }
      });
    };

    switch (String(kdcType.value).toLowerCase()) {
      case messages["admin.kerberos.wizard.step1.option.manual"].toLowerCase():
        updatePropertyVisibility("kdc_hosts", false);
        updatePropertyVisibility("admin_server_host", false);
        updatePropertyVisibility("domains", false);
        break;

      case messages["admin.kerberos.wizard.step1.option.ad"].toLowerCase():
        updatePropertyVisibility("container_dn", true);
        updatePropertyVisibility("ldap_url", true);
        break;

      case messages["admin.kerberos.wizard.step1.option.ipa"].toLowerCase():
        updatePropertyVisibility("group", true);
        Object.values(updatedConfigs["KERBEROS"]).forEach((section) => {
          const manageKrb5Conf = section.properties["manage_krb5_conf"];
          const installPackages = section.properties["install_packages"];

          if (manageKrb5Conf) {
            manageKrb5Conf.value = "false";
          }
          if (installPackages) {
            installPackages.value = "false";
          }
        });
        updatePropertyVisibility("admin_server_host", false);
        updatePropertyVisibility("domains", false);
        break;
    }

    if (!updatedConfigs["KERBEROS"]?.["KDC"]?.properties) {
      return updatedConfigs;
    }
    updatedConfigs["KERBEROS"]["KDC"].properties["Test.KDC.Connection"] = {
      propertyName: "Test.KDC.Connection",
      propertyDisplayname: " ",
      propertyDescription: "Test KDC Connection",
      propertyValue: "TEST KDC CONNECTION",
      propertyAttributes: {
        type: "button",
        overridable: false,
      },
      previousValue: "TEST KDC CONNECTION",
      value: "TEST KDC CONNECTION",
      final: "false",
      isEditable: false,
    };

    return updatedConfigs;
  };

  const availableConfigGroups = [
    "Default",
    ...(groupsByService[currentService] || []).map(
      (group) => get(group, "ConfigGroup.group_name", ""),
    ).filter(Boolean),
  ];

  const changeConfigGroup = async () => {
    if (
      !hostName
      || !currentService
      || selectedConfigGroup === configGroup
      || isChangingGroup
      || !canManageConfigGroups
    ) {
      return;
    }

    const updates = buildConfigGroupMembershipUpdates(
      groupsByService[currentService] || [],
      currentService,
      configGroup,
      selectedConfigGroup,
      hostName,
    );
    setIsChangingGroup(true);
    setGroupChangeError("");
    try {
      for (const update of updates) {
        await ConfigGroupApi.updateConfigGroup(
          clusterName,
          update.groupId,
          update.payload,
        );
      }
      setGroupsByService((current) => ({
        ...current,
        [currentService]: current[currentService].map((group) => (
          updates.find((update) => update.groupId === String(get(group, "ConfigGroup.id")))?.group
          || group
        )),
      }));
      setAssignedGroupByService((current) => ({
        ...current,
        [currentService]: selectedConfigGroup,
      }));
      setConfigGroup(selectedConfigGroup);
      setShowChangeConfigGroupModal(false);
    } catch (error: any) {
      setGroupChangeError(
        error?.response?.data?.message || "Ambari could not change this host's config group.",
      );
    } finally {
      setIsChangingGroup(false);
    }
  };

  const getChangeConfigGroupModalBody = () => (
    <div>
      <Form.Group controlId="hostConfigGroup">
        <Form.Label>Group</Form.Label>
        <Form.Select
          value={selectedConfigGroup}
          disabled={isChangingGroup}
          onChange={(event) => setSelectedConfigGroup(event.target.value)}
        >
          {availableConfigGroups.map((groupName) => (
            <option key={groupName} value={groupName}>{groupName}</option>
          ))}
        </Form.Select>
      </Form.Group>
      {groupChangeError ? (
        <Alert className="mt-3 mb-0" variant="danger">{groupChangeError}</Alert>
      ) : null}
    </div>
  );

  if (loading) {
    return <Spinner />;
  }

  if (loadError) {
    return (
      <Alert variant="danger">
        {loadError}{" "}
        <Button
          size="sm"
          variant="outline-danger"
          onClick={() => setRetryCount((value) => value + 1)}
        >
          Retry
        </Button>
      </Alert>
    );
  }

  if (!hostServices.length) {
    return <Alert variant="info">There are no configurable services on this host.</Alert>;
  }

  return (
    <>
      {showChangeConfigGroupModal && (
        <Modal
          isOpen={showChangeConfigGroupModal}
          onClose={() => {
            if (!isChangingGroup) {
              setSelectedConfigGroup(configGroup);
              setGroupChangeError("");
              setShowChangeConfigGroupModal(false);
            }
          }}
          modalTitle="Change Group"
          modalBody={getChangeConfigGroupModalBody()}
          successCallback={() => void changeConfigGroup()}
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            buttonSize: "sm",
            okButtonVariant: "primary",
            okButtonDisabled: configGroup === selectedConfigGroup || isChangingGroup,
            modalBodyClassName: "h-150px"
          }}
        />
      )}
      <div>
        {themeLoadNotice && (
          <Alert
            className="mx-5 mb-3"
            variant={themeLoadNotice.kind === "empty" ? "info" : "warning"}
          >
            {themeLoadNotice.kind === "empty"
              ? "No host configuration Theme layout is defined. "
              : "Host configuration layout could not be loaded. "}
            Advanced configurations remain available. {themeLoadNotice.message}{" "}
            {themeLoadNotice.kind !== "empty" && (
              <Button
                size="sm"
                variant="outline-warning"
                disabled={themeLoading}
                onClick={() => void retryThemes()}
              >
                {themeLoading ? "Retrying Theme" : "Retry Theme"}
              </Button>
            )}
          </Alert>
        )}
        <Card className="rounded-0 mx-5">
          <div className="p-3 d-flex justify-content-between align-items-center">
            <div>
              <h4 className="mb-1">Host Configurations - {hostName}</h4>
              <div className="d-flex align-items-center flex-wrap">
                {currentService && (
                  <div className="me-3 mb-1">
                    <span className="text-muted me-2">Current Service:</span>
                    <Badge bg="info" className="me-2">
                      {currentService}
                    </Badge>
                  </div>
                )}
                <div className="mb-1 d-flex">
                  <span className="text-muted me-2">Config Group:</span>
                  <Badge
                    bg={configGroup === "Default" ? "secondary" : "primary"}
                    className="me-2"
                  >
                    {configGroup}
                  </Badge>
                  {canManageConfigGroups && availableConfigGroups.length > 1 && (
                    <Button
                      className="p-0 align-baseline"
                      variant="link"
                      onClick={() => {
                        setSelectedConfigGroup(configGroup);
                        setGroupChangeError("");
                        setShowChangeConfigGroupModal(true);
                      }}
                    >
                      Change
                    </Button>
                  )}
                </div>
              </div>
            </div>
            <div className="text-muted">
              <small>
                All Services: {hostServices.join(", ")}
              </small>
            </div>
          </div>
          <Config
            configProperties={configProperties}
            setConfigProperties={setConfigProperties}
            configPropertiesData={configs}
            configSection="default"
            allThemes
            themeData={themes}
            servicesList={hostServices}
            installedServices={serviceInCluster}
            configGroup={configGroup}
            setConfigGroup={setConfigGroup}
            hostConfigs={true}
            displayUndoRedo={false}
            onServiceChange={onServiceChange}
          />
        </Card>
      </div>
    </>
  );
}
