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
import { useParams } from "react-router-dom";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import { ambari_metrics_properties } from "../../data/configs/services/ambari_metrics_properties";
import { hbase_properties } from "../../data/configs/services/hbase_properties";
import { hdfs_properties } from "../../data/configs/services/hdfs_properties";
import { hive_properties } from "../../data/configs/services/hive_properties";
import { mapreduce2_properties } from "../../data/configs/services/mapreduce2_properties";
import { ranger_properties } from "../../data/configs/services/ranger_properties";
import { tez_properties } from "../../data/configs/services/tez_properties";
import { yarn_properties } from "../../data/configs/services/yarn_properties";
import { zookeeper_properties } from "../../data/configs/services/zookeeper_properties";
import { kerberos_properties } from "../../data/configs/services/kerberos_properties";
import { get, isEmpty, map, find } from "lodash";
import { Card, Dropdown, Badge } from "react-bootstrap";
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
import { translate } from "../../Utils/Utility";
import Modal from "../../components/Modal";

export default function Hostconfigs() {
  const params = useParams();
  const hostName = params.hostname;

  const [loading, setLoading] = useState<boolean>(true);
  const [themes, setThemes] = useState<any>({});
  const [configs, setConfigs] = useState<any>({});
  const [configProperties, setConfigProperties] = useState({});
  const [propertyValues, setPropertyValues] = useState<any>({});
  const [defaultVersionNumber, setDefaultVersionNumber] = useState<string>();
  const [selectedVersion, setSelectedVersion] = useState<string>();
  const [showChangeConfigGroupModal, setShowChangeConfigGroupModal] = useState<boolean>(false);
  const [selectedConfigGroup, setSelectedConfigGroup] = useState<string>("Default");
  const [configGroup, setConfigGroup] = useState<string>("Default");
  const [hostData, setHostData] = useState<any>(null);
  const [hostConfigGroups, setHostConfigGroups] = useState<any[]>([]);
  const [availableConfigGroups, setAvailableConfigGroups] = useState<string[]>(["Default"]);
  const [serviceConfigGroups, setServiceConfigGroups] = useState<{ [key: string]: string[] }>({});
  const [currentService, setCurrentService] = useState<string>("");
  const [hostServices, setHostServices] = useState<string[]>([]);

  const { clusterName, services, cluster } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const stackName = get(cluster, "stack");
  const stackVersion = get(cluster, "versionNum");

  const serviceInCluster = map(services, "ServiceInfo.service_name");

  const propertiesFileMap: { [key: string]: any } = {
    HDFS: hdfs_properties,
    YARN: yarn_properties,
    HIVE: hive_properties,
    HBASE: hbase_properties,
    RANGER: ranger_properties,
    MAPREDUCE2: mapreduce2_properties,
    TEZ: tez_properties,
    ZOOKEEPER: zookeeper_properties,
    AMBARI_METRICS: ambari_metrics_properties,
    KERBEROS: kerberos_properties,
  };

  useEffect(() => {
    if (serviceInCluster.length > 0 && hostName) {
      getHostData();
    }
  }, [services, hostName]);

  useEffect(() => {
    if (hostData && hostServices.length > 0) {
      getConfigurations();
      getThemes();
      getHostConfigGroups();
      getPropertiesValues();
    }
  }, [hostData, hostServices]);

  useEffect(() => {
    if (!isEmpty(configs)) {
      getConfigProperties();
    }
  }, [configs, propertyValues, JSON.stringify(allServiceModels)]);

  // Custom callback to handle service changes from Config component
  const onServiceChange = (selectedService: string) => {
    if (selectedService !== currentService && hostServices.includes(selectedService)) {
      handleServiceChange(selectedService);
    }
  };

  const getThemes = async () => {
    if (hostServices.length === 0) return;

    setLoading(true);
    try {
      const response = await WizardApi.getStackThemes(
        stackName,
        stackVersion,
        hostServices.join(","),
        "themes/*"
      );
      setThemes(response);
    } catch (error) {
      console.error("Error fetching themes:", error);
    } finally {
      setLoading(false);
    }
  };

  const getConfigurations = async () => {
    if (hostServices.length === 0) return;

    setLoading(true);
    try {
      const response = await WizardApi.getStackConfigurations(
        stackName,
        stackVersion,
        hostServices.join(","),
        "configurations/*,configurations/dependencies/*,StackServices/config_types/*"
      );
      setConfigs(response);
    } catch (error) {
      console.error("Error fetching configurations:", error);
    } finally {
      setLoading(false);
    }
  };

  const getHostData = async () => {
    if (!hostName) return;

    setLoading(true);
    try {
      const response = await HostsApi.getHostData(
        clusterName,
        hostName,
        "host_components/HostRoles/service_name,host_components/HostRoles/component_name,host_components/HostRoles/display_name"
      );
      setHostData(response);

      // Extract services running on this host
      const hostComponents = get(response, "host_components", []);
      const servicesOnHost = [...new Set(hostComponents.map((comp: any) =>
        get(comp, "HostRoles.service_name")
      ))].filter(Boolean) as string[];

      setHostServices(servicesOnHost);
    } catch (error) {
      console.error("Error fetching host data:", error);
    } finally {
      setLoading(false);
    }
  };

  const getHostConfigGroups = async () => {
    if (!hostName || hostServices.length === 0) return;

    try {
      const configGroups = [];
      const serviceSpecificGroups: { [key: string]: string[] } = {};

      // Initialize each service with Default group
      hostServices.forEach(service => {
        serviceSpecificGroups[service] = ["Default"];
      });

      // Get config groups for each service running on this host
      for (const serviceName of hostServices) {
        const response = await ConfigGroupApi.getConfigGroupInfo(
          clusterName,
          serviceName,
          "ConfigGroup/group_name,ConfigGroup/hosts,ConfigGroup/tag"
        );

        if (response.items) {
          // Filter config groups that include this host
          const relevantGroups = response.items.filter((group: any) => {
            const hosts = get(group, "ConfigGroup.hosts", []);
            return hosts.some((host: any) => host.host_name === hostName);
          });

          // Store service-specific config groups
          const serviceGroups = ["Default"];
          relevantGroups.forEach((group: any) => {
            const groupName = get(group, "ConfigGroup.group_name");
            if (groupName && !serviceGroups.includes(groupName)) {
              serviceGroups.push(groupName);
            }
          });
          serviceSpecificGroups[serviceName] = serviceGroups;

          configGroups.push(...relevantGroups.map((group: any) => ({
            ...group,
            serviceName: serviceName
          })));
        }
      }

      setHostConfigGroups(configGroups);
      setServiceConfigGroups(serviceSpecificGroups);

      // Set initial service and config groups
      if (hostServices.length > 0) {
        const firstService = hostServices[0];
        setCurrentService(firstService);
        setAvailableConfigGroups(serviceSpecificGroups[firstService] || ["Default"]);

        // Set the config group for the first service
        const serviceGroups = serviceSpecificGroups[firstService] || ["Default"];
        const primaryGroup = serviceGroups.find(group => group !== "Default") || "Default";
        setConfigGroup(primaryGroup);
        setSelectedConfigGroup(primaryGroup);
      }
    } catch (error) {
      console.error("Error fetching host config groups:", error);
      // Fallback to Default if there's an error
      setAvailableConfigGroups(["Default"]);
      setConfigGroup("Default");
      setSelectedConfigGroup("Default");
    }
  };

  const getPropertiesValues = async () => {
    if (hostServices.length === 0) return;

    setLoading(true);
    try {
      // Fetch configs for services running on this host only
      const response = await ConfigsApi.getConfigValues(
        clusterName,
        hostServices.join(",")
      );
      setPropertyValues(response);

      // Handle config group specific values
      response.items.map((item: any) => {
        const groupName = get(item, "group_name", "Default");
        if (groupName === "Default") {
          const latestDefaultVersion = get(item, "service_config_version", "");
          setDefaultVersionNumber(latestDefaultVersion);
          setSelectedVersion(latestDefaultVersion);
        }

        // If this host belongs to a specific config group, prioritize that group's configs
        if (hostConfigGroups.length > 0) {
          const hostGroup = find(hostConfigGroups, (group) => {
            const hosts = get(group, "ConfigGroup.hosts", []);
            return hosts.some((host: any) => host.host_name === hostName);
          });

          if (hostGroup && groupName === get(hostGroup, "ConfigGroup.group_name")) {
            setSelectedVersion(get(item, "service_config_version", ""));
          }
        }
      });
    } catch (error) {
      console.error("Error fetching property values:", error);
    } finally {
      setLoading(false);
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
                if (
                  !configPropertiesCopy[serviceName]["Custom " + type]
                    .properties[propertyName]
                ) {
                  if (configPropertiesCopy[serviceName]["Custom " + type]) {
                    configPropertiesCopy[serviceName][
                      "Custom " + type
                    ].properties[propertyName] = {
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
                      isEditable:
                        configGroup === "Default" &&
                        selectedVersion === defaultVersionNumber,
                    };
                  }
                }

                if (
                  !configPropertiesCopy[serviceName]["Custom " + type]
                    .properties[propertyName].overrideValues
                ) {
                  configPropertiesCopy[serviceName][
                    "Custom " + type
                  ].properties[propertyName].overrideValues = [];
                }
                configPropertiesCopy[serviceName]["Custom " + type].properties[
                  propertyName
                ].overrideValues?.push({
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

      const serviceConfigCategories = getConfigCategories(serviceName);
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

    // Update available config groups for the selected service
    const serviceGroups = serviceConfigGroups[selectedService] || ["Default"];
    setAvailableConfigGroups(serviceGroups);

    // Reset to Default or first available group for the service
    const defaultGroup = serviceGroups.includes("Default") ? "Default" : serviceGroups[0];
    setConfigGroup(defaultGroup);
    setSelectedConfigGroup(defaultGroup);

    // Refresh config properties for the new service and group
    if (!isEmpty(configs)) {
      getConfigProperties();
    }
  };

  const setVisibilityForKerberosProperties = (
    configProps: ConfigPropertiesType
  ) => {
    const updatedConfigs = { ...configProps };

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

    switch (kdcType.value.toLowerCase()) {
      case translate("admin.kerberos.wizard.step1.option.manual"):
        updatePropertyVisibility("kdc_hosts", false);
        updatePropertyVisibility("admin_server_host", false);
        updatePropertyVisibility("domains", false);
        break;

      case translate("admin.kerberos.wizard.step1.option.ad"):
        updatePropertyVisibility("container_dn", true);
        updatePropertyVisibility("ldap_url", true);
        break;

      case translate("admin.kerberos.wizard.step1.option.ipa"):
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
      isEditable: true,
    };

    return updatedConfigs;
  };

  const getChangeConfigGroupModalBody = () => {
    return <div className="d-flex h-25">
      <div className="mt-2 me-2">Groups: </div>
      <Dropdown>
        <Dropdown.Toggle variant="outline-secondary" size="sm">
          {selectedConfigGroup}
        </Dropdown.Toggle>
        <Dropdown.Menu className="bring-to-front">
          {availableConfigGroups.map((groupName) => (
            <Dropdown.Item
              key={groupName}
              active={groupName === configGroup}
              onClick={() => {
                setSelectedConfigGroup(groupName);
              }}
            >
              {groupName}
            </Dropdown.Item>
          ))}
        </Dropdown.Menu>
      </Dropdown>
    </div>;
  }

  if (loading) {
    return <Spinner />;
  }

  return (
    <>
      {showChangeConfigGroupModal && (
        <Modal
          isOpen={showChangeConfigGroupModal}
          onClose={() => setShowChangeConfigGroupModal(false)}
          modalTitle="Change Group"
          modalBody={getChangeConfigGroupModalBody()}
          successCallback={() => {
            setConfigGroup(selectedConfigGroup);
            if (!isEmpty(configs)) {
              getConfigProperties();
            }
            setShowChangeConfigGroupModal(false);
          }}
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            buttonSize: "sm",
            okButtonVariant: "primary",
            okButtonDisabled: configGroup === selectedConfigGroup,
            modalBodyClassName: "h-150px"
          }}
        />
      )}
      <div>
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
                  {availableConfigGroups.length > 1 && (
                    <div className="custom-link" onClick={() => setShowChangeConfigGroupModal(true)}>Change</div>
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
