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
import {
  Service,
  ServicesResponse,
  StackServiceComponent,
  SelectedService,
} from "../types/StackServiceComponent";
import { isShownOnInstallerSlaveClientPage } from "../utils";
import { difference, flatten, get, isEmpty } from "lodash";
import { ContextWrapper } from "..";
import { HostsApi } from "../../../api/hostsApi";
import { AppContext } from "../../../store/context";
import RecommendationsApi from "../../../api/recommendationsApi";
import { ChooseServicesApi } from "../../../api/chooseServicesApi";

enum ComponentCategory {
  CLIENT = "CLIENT",
  SLAVE = "SLAVE",
}

const useServiceComponents = (wizardName: string, initialData?: any) => {
  const { Context } = useContext(ContextWrapper);
  const [serviceComponents, setServiceComponents] = useState<any>([]);
  const [allServiceComponentsList, setAllServiceComponentsList] = useState<any>(
    []
  );
  const {
    clusterName = "",
    cluster: { stack, versionNum },
  } = useContext(AppContext);

  const { state, installedServices = [] }: any = useContext(Context);
  const versionStepData = get(state, `${wizardName}Steps.VERSION.data`, {});
  const VERSION =
    versionNum || get(versionStepData, "selectedVersion.stack_version", "");
  const STACK = stack || get(versionStepData, "selectedStack.stack_name", "");

  const stepData: any = get(state, `${wizardName}Steps.MASTERS.data`, {});
  const hostsStepData: any = get(
    state,
    `${wizardName}Steps.HOST_STATUS.data`,
    {}
  );
  const servicesStepData: any = get(
    state,
    `${wizardName}Steps.SERVICES.data`,
    {}
  );

  const masterMapping = stepData?.mastersData;

  const getHosts = () => {
    const hosts: { [key: string]: any } = {};
    const registeredHosts = hostsStepData?.hosts?.filter((host: any) => {
      return host.bootStatus === "REGISTERED";
    });
    let hostIndex = 1;
    if (isEmpty(registeredHosts)) {
      return {};
    }
    for (let registeredHost of registeredHosts) {
      const hostObj = {
        name: registeredHost.name,
        bootStatus: "REGISTERED",
        isInstalled: false,
        id: hostIndex,
      };
      hosts[registeredHost.name] = hostObj;
      // set(hosts, registeredHost.name, hostObj);
      hostIndex++;
    }
    return hosts;
  };

  const getSelectedServices = () => {
    const selectedServices = [];
    const allServices = servicesStepData.services;
    for (let service in allServices) {
      const currentService = allServices[service];
      if (currentService.selected) {
        selectedServices.push({
          service_name: currentService.serviceName,
          display_name: currentService.display_name,
          id: currentService.serviceName,
          service_type: currentService.serviceType,
          is_selected: currentService.selected,
        });
      }
    }
    return selectedServices;
  };
  const hosts = getHosts();

  const masterComponentHosts = Object.keys(hosts);
  const services = getSelectedServices()
    .filter((service) => service.is_selected)
    .map((selectedService) => selectedService.id);

  const getRecommendationsRequestBody = (
    collection = masterMapping,
    isValidation = false
  ) => {
    const recommendationsPayload: any = {
      hosts: Object.keys(hosts),
      recommendations: {
        blueprint: { host_groups: [] },
        blueprint_cluster_binding: { host_groups: [] },
      },
      services,
    };
    let hostGroupIndex = 1;
    for (let host of Object.keys(hosts)) {
      const correspondingHost = collection.find((master: any) => {
        return master.host_name === host;
      });
      let components: { name: string }[] = [];
      if (correspondingHost) {
        components = correspondingHost.masterServices.map((service: any) => {
          return {
            name: service.component || service.component_name,
          };
        });
      }
      //@ts-ignore
      recommendationsPayload.recommendations.blueprint.host_groups.push({
        name: `host-group-${hostGroupIndex}`,
        components,
      });
      //@ts-ignore
      recommendationsPayload.recommendations.blueprint_cluster_binding.host_groups.push(
        {
          name: `host-group-${hostGroupIndex}`,
          hosts: [{ fqdn: host }],
        }
      );
      hostGroupIndex++;
    }
    if (isValidation) {
      recommendationsPayload.validate = "host_groups";
    } else {
      recommendationsPayload.recommend = "host_groups";
    }
    return recommendationsPayload;
  };

  const getHostGroupRecommendations = async () => {
    let mapping = masterMapping;
    let installedHostComponents: any = {}; // Map of hostname -> installed component names
    
    // For Add Service wizard, fetch existing components on each host
    if (wizardName === "addService") {
      const existingComponentsForHost = await HostsApi.getHostComponentsDetails(
        clusterName,
        "fields=Hosts/host_name,host_components/HostRoles/component_name,host_components/HostRoles/stale_configs,host_components/HostRoles/maintenance_state"
      );
      
      // Build map of installed components per host
      existingComponentsForHost.items.forEach((item: any) => {
        installedHostComponents[item.Hosts.host_name] = item.host_components.map(
          (component: any) => component.HostRoles.component_name
        );
      });
      
      mapping = existingComponentsForHost.items.map((item: any) => {
        return {
          host_name: item.Hosts.host_name,
          masterServices: item.host_components.map((component: any) => {
            return {
              component_name: component.HostRoles.component_name,
              component: component.HostRoles.component_name,
              stale_configs: component.HostRoles.stale_configs,
              maintenance_state: component.HostRoles.maintenance_state,
            };
          }),
        };
      });
    }
    const recommendationsPayload = getRecommendationsRequestBody(mapping);
    const recommendationsData = await RecommendationsApi.loadRecommendations(
      STACK,
      VERSION,
      recommendationsPayload
    );
    const recommendationsDataObj = recommendationsData?.resources?.[0];
    const result: any = {};
    const hostGroupBindings =
      recommendationsDataObj.recommendations.blueprint_cluster_binding
        .host_groups;
    const hostGroups =
      recommendationsDataObj.recommendations.blueprint.host_groups;

    // Create a map of host group name to components
    const hostGroupMap = hostGroups.reduce((acc: any, group: any) => {
      acc[group.name] = group.components.map(
        (component: any) => component.name
      );
      return acc;
    }, {});

    // Map each host to its components
    hostGroupBindings.forEach((binding: any) => {
      binding.hosts.forEach((host: any) => {
        result[host.fqdn] = hostGroupMap[binding.name];
      });
    });

    return { recommendations: result, installedComponents: installedHostComponents };
  };
  //@ts-ignore
  const getValidationsRequestBody = () => {};

  useEffect(() => {
    const getServicesAndComponents = async () => {
      if (isEmpty(initialData)) {
        const servicesAndComponents: ServicesResponse =
          await ChooseServicesApi.getServices(STACK, VERSION);
        const allServiceComponents: StackServiceComponent[] = [];
        const allList = flatten(
          servicesAndComponents.items.map((item) => get(item, "components", []))
        );
        setAllServiceComponentsList(
          allList.map((listItem) => listItem.StackServiceComponents)
        );
        let hasClientComponents = false;

        servicesAndComponents.items.forEach((service: Service) => {
          if (isSelectedService(service)) {
            if (!hasClientComponents) {
              hasClientComponents = checkForClientComponents(
                service.components
              );
            }
            for (const serviceComponent of service.components) {
              if (
                isShownOnInstallerSlaveClientPage(
                  serviceComponent.StackServiceComponents
                )
              ) {
                const componentDetails =
                  serviceComponent.StackServiceComponents;
                allServiceComponents.push({
                  componentName: get(componentDetails, "component_name", ""),
                  isDisabled: installedServices.includes(
                    get(componentDetails, "service_name", "")
                  ),
                });
              }
            }
          }
        });

        const nonMasterHosts = difference(
          Object.keys(hosts),
          masterComponentHosts
        );

        const clientHost = nonMasterHosts[0] || Object.keys(hosts)[Object.keys(hosts).length - 1];

        if (hasClientComponents) {
          allServiceComponents.push({
            componentName: ComponentCategory.CLIENT,
            isDisabled: false,
          });
        }

        const sortedHosts = [
          ...new Set([...masterComponentHosts, ...Object.keys(hosts)]),
        ];
        const { recommendations: recommendedServices, installedComponents } = await getHostGroupRecommendations();
        const hostServiceComponents: any = sortedHosts.map((hostname) => {
          const installedOnThisHost = installedComponents[hostname] || [];
          
          return {
            hostname,
            checkboxes: allServiceComponents.map((serviceComponent) => {
              // Check if component is already installed on THIS specific host
              const isInstalledOnHost = installedOnThisHost.includes(
                serviceComponent.componentName
              );
              
              // Match Ember logic: Don't check disabled components based on recommendations
              // Only check if: 
              // 1. Component is installed on this host (takes precedence), OR
              // 2. Component is NOT disabled AND is in recommendations, OR
              // 3. Component is CLIENT and this is the designated client host
              const isInRecommendations = !!recommendedServices[hostname]?.includes(
                serviceComponent.componentName
              );
              const isClientOnDesignatedHost =
                serviceComponent.componentName === ComponentCategory.CLIENT &&
                hostname === clientHost;
              
              let checked = false;
              if (wizardName === "addHost") {
                // For addHost wizard: only CLIENT components should be checked
                checked = isClientOnDesignatedHost;
              } else {
                // For other wizards:
                // 1. If component is installed on this host, check it
                if (isInstalledOnHost) {
                  checked = true;
                }
                // 2. Otherwise, check recommendations only if component is NOT disabled
                else if (!serviceComponent.isDisabled) {
                  checked = isInRecommendations;
                }
                // 3. CLIENT component gets special handling (if not already checked)
                if (isClientOnDesignatedHost && !checked) {
                  checked = true;
                }
              }

              return {
                label: serviceComponent.componentName,
                isDisabled: serviceComponent.isDisabled,
                checked: checked,
                isInstalled: isInstalledOnHost,
              };
            }),
          };
        });

        setServiceComponents(hostServiceComponents);
      } else {
        setServiceComponents(initialData);
      }
    };

    getServicesAndComponents();
    // Don't call getHostGroupRecommendations() separately as it's already called within getServicesAndComponents()
    // This prevents duplicate API calls that could interfere with configuration recommendations
  }, []);

  const isSelectedService = (service: Service): boolean => {
    return !!getSelectedServices().find(
      (ser: SelectedService) =>
        ser.is_selected &&
        ser.service_name === service.StackServices.service_name
    );
  };

  const checkForClientComponents = (components: any): boolean => {
    return components.some(
      (component: any) =>
        component.StackServiceComponents.component_category ===
        ComponentCategory.CLIENT
    );
  };

  const getClientComponents = () => {
    const clientComponents = allServiceComponentsList.filter(
      (stackServiceComponent: any) => {
        return get(stackServiceComponent, "is_client", false);
      }
    );
    return clientComponents.map((clientComponent: any) =>
      get(clientComponent, "component_name", false)
    );
  };

  return {
    serviceComponents,
    services,
    hosts,
    masterComponentHosts,
    setServiceComponents,
    getRecommendationsRequestBody,
    STACK,
    VERSION,
    allServiceComponentsList,
    getClientComponents,
    ComponentCategory,
  };
};

export default useServiceComponents;
