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

import { forEach, get, set } from "lodash";
import { HostsApi } from "../../api/hostsApi";
import { IHostComponent } from "../../models/hostComponent";
import modalManager from "../../store/ModalManager";
import BackgroundOperations from "../BackgroundOperations";
import RollingRestartModal from "../../components/RollingRestartModal";
import {
  showErrorModal,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";

const getOperationLevelObject = (
  level: string,
  serviceName: string,
  componentName: string,
  clusterName: string
) => {
  const operationLevel = {
    level: level,
    cluster_name: clusterName,
  };
  if (level === "SERVICE") {
    set(operationLevel, "service_name", serviceName);
  } else if (level !== "HOST") {
    set(operationLevel, "service_name", serviceName);
    set(operationLevel, "hostcomponent_name", componentName);
  }

  return operationLevel;
};

export const restartHostComponents = async (
  hostComponentsList: IHostComponent[],
  context: string,
  level: string
) => {
  context = context || "Restart components";

  const clusterName = get(hostComponentsList, "[0].clusterName", "");

  let componentToHostsMap: any = {};
  forEach(hostComponentsList, (hc) => {
    const hostName = get(hc, "hostName", "");
    const componentName = get(hc, "componentName", "");
    if (get(componentToHostsMap, componentName, "") === "") {
      set(componentToHostsMap, componentName, {
        serviceName: get(hc, "serviceName", ""),
        hostNames: [],
      });
    }
    get(componentToHostsMap, componentName + ".hostNames").push(hostName);
  });

  const resource_filters = Object.keys(componentToHostsMap).map(
    (componentName) => {
      return {
        service_name: get(
          componentToHostsMap,
          componentName + ".serviceName",
          ""
        ),
        component_name: componentName,
        hosts: get(componentToHostsMap, componentName + ".hostNames", []).join(
          ","
        ),
      };
    }
  );

  let operation_level = {};
  if (hostComponentsList.length > 0) {
    const serviceComponentName = get(
      hostComponentsList,
      "[0].componentName",
      ""
    );
    const serviceName = get(hostComponentsList, "[0].serviceName", "");
    operation_level = getOperationLevelObject(
      level,
      serviceName,
      serviceComponentName,
      clusterName
    );
  }

  if (resource_filters.length) {
    const data = {
      RequestInfo: {
        command: "RESTART",
        context: context,
        operation_level: operation_level,
      },
      "Requests/resource_filters": resource_filters,
    };

    const response = await HostsApi.clusterRequests(clusterName, data);
    const requestId = get(response, "Requests.id", -1);
    if (requestId !== -1) {
      modalManager.show(
        <BackgroundOperations
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          requestId={requestId}
        />
      );
    }
  }
};

export const getComponentsFromServer = async (
  options: any,
  clusterName: string,
  callBack: (responseData: any) => void
) => {
  const requestParameters = constructComponentsCallUrl(options);
  const data = {
    parameters: requestParameters.params,
    fields: requestParameters.fields,
  };
  const responseData: any = await HostsApi.getHostComponents(
    clusterName,
    requestParameters.fields,
    data
  );
  callBack(responseData);
};

const constructComponentsCallUrl = (options: any) => {
  const multipleValueParams: any = {
    services: "host_components/HostRoles/service_name.in(<entity-names>)",
    hosts: "Hosts/host_name.in(<entity-names>)",
    components: "host_components/HostRoles/component_name.in(<entity-names>)",
  };
  const singleValueParams: any = {
    staleConfigs: "host_components/HostRoles/stale_configs=",
    passiveState: "Hosts/maintenance_state=",
    workStatus: "host_components/HostRoles/state=",
  };
  const displayParams = options.displayParams || [];
  let urlParams = "";
  let addAmpersand = false;

  Object.keys(multipleValueParams).forEach((key) => {
    const arrayParams = options[key];
    if (Array.isArray(arrayParams) && arrayParams.length > 0) {
      if (addAmpersand) {
        urlParams += "&";
        addAmpersand = false;
      }
      urlParams += multipleValueParams[key].replace(
        "<entity-names>",
        arrayParams.join(",")
      );
      addAmpersand = true;
    }
  });

  Object.keys(singleValueParams).forEach((key) => {
    const param = options[key];
    if (param !== undefined && param !== null) {
      urlParams += addAmpersand ? "&" : "";
      urlParams += singleValueParams[key] + param.toString();
      addAmpersand = true;
    }
  });

  let params = urlParams;
  let fields = "";
  displayParams.forEach((displayParam: string, index: number, array: any) => {
    if (index === 0) {
      fields += addAmpersand ? "&" : "";
      fields += "fields=";
    }
    fields += displayParam;
    fields += array.length === index + 1 ? "" : ",";
  });
  fields += "&minimal_response=true";

  return { fields: fields.substring(1), params: params };
};

export const infoPassiveState = (passiveState: string) => {
  const enabled = passiveState === "OFF" ? "enabled" : "suppressed";
  const modalProps = {
    modalTitle: translate("common.information"),
    modalBody: translateWithVariables("hostPopup.warning.alertsTimeOut", {
      "0": passiveState.toLowerCase(),
      "1": enabled,
    }),
    onClose: () => {},
    successCallback: () => {
      modalManager.hide();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: true,
      okButtonVariant: "primary",
    },
  };
  modalManager.show(modalProps);
};

export const showRollingRestartPopup = (
  hostComponentName: string,
  serviceName: string,
  isMaintenanceModeOn: boolean,
  staleConfigsOnly: boolean,
  hostComponents: any[],
  clusterName: string,
  skipMaintenance = false
) => {
  modalManager.show(
    <RollingRestartModal
      isOpen={true}
      onClose={() => {
        modalManager.hide();
      }}
      successCallback={async (
        restartComponents,
        batchSize,
        waitTime,
        tolerateSize,
        turnOnMm
      ) => {
        if (turnOnMm) {
          try {
            const data = {
              requestInfo: translateWithVariables("passiveState.turnOnFor", {
                "0": serviceName,
              }),
              serviceName: serviceName.toUpperCase(),
              passive_state: "ON",
            };
            const response = await HostsApi.updateServicePassiveState(
              clusterName,
              serviceName.toUpperCase(),
              data
            );
            const requestId = get(response, "Requests.id", -1);
            defaultSuccessCallbackWithoutReload(requestId);
          } catch (error) {
            defaultErrorCallback(error);
          }
        }
        doPostBatchRollingRestartRequest(
          restartComponents,
          batchSize,
          waitTime,
          tolerateSize,
          clusterName
        );
      }}
      hostComponentName={hostComponentName}
      serviceName={serviceName}
      isServiceInMM={isMaintenanceModeOn}
      staleConfigsOnly={staleConfigsOnly}
      allHostComponents={hostComponents}
      skipMaintenance={skipMaintenance}
      turnOnMm={false}
    />
  );
};

const doPostBatchRollingRestartRequest = async (
  restartHostComponents: any[],
  batchSize: number,
  intervalTimeSeconds: number,
  tolerateSize: number,
  clusterName: string
) => {
  if (!restartHostComponents.length) {
    return;
  }
  const batches = getBatchesForRollingRestartRequest(
    restartHostComponents,
    batchSize,
    clusterName
  );
  const data = {
    intervalTimeSeconds: intervalTimeSeconds,
    tolerateSize: tolerateSize,
    batches: batches,
  };
  try {
    const response = await HostsApi.batchRequest(clusterName, data);
    const requestID = get(
      response,
      "data.resources.[0].RequestSchedule.id",
      -1
    );
    defaultSuccessCallbackWithoutReload(requestID);
  } catch (error) {
    defaultErrorCallback(error);
  }
};

const getBatchesForRollingRestartRequest = (
  restartHostComponents: any[],
  batchSize: number,
  clusterName: string
) => {
  let hostIndex = 0;
  const batches = [];
  const batchCount = Math.ceil(restartHostComponents.length / batchSize);
  const sampleHostComponent = restartHostComponents[0];
  const componentName = get(sampleHostComponent, "componentName");
  const serviceName = get(sampleHostComponent, "serviceName");

  for (let count = 0; count < batchCount; count++) {
    const hostNames = [];
    for (
      let hc = 0;
      hc < batchSize && hostIndex < restartHostComponents.length;
      hc++
    ) {
      hostNames.push(get(restartHostComponents[hostIndex++], "hostName"));
    }
    if (hostNames.length) {
      batches.push({
        order_id: count + 1,
        type: "POST",
        uri: `/clusters/${clusterName}/requests`,
        RequestBodyInfo: {
          RequestInfo: {
            context: `_PARSE_.ROLLING-RESTART.${componentName}.${
              count + 1
            }.${batchCount}`,
            command: "RESTART",
          },
          "Requests/resource_filters": [
            {
              service_name: serviceName,
              component_name: componentName,
              hosts: hostNames.join(","),
            },
          ],
        },
      });
    }
  }
  return batches;
};

export const defaultSuccessCallback = (requestId: number) => {
  if (requestId !== -1) {
    modalManager.show(
      <BackgroundOperations
        isOpen={true}
        onClose={() => {
          modalManager.hide();
          if (
            window.location.hash.match(
              /^#\/main\/services\/[A-Za-z]+\/summary$/
            )
          ) {
            window.location.reload();
          }
        }}
        requestId={requestId}
      />
    );
  }
};

export const defaultSuccessCallbackWithoutReload = (requestId: number) => {
  if (requestId !== -1) {
    modalManager.show(
      <BackgroundOperations
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        requestId={requestId}
      />
    );
  }
};

export const defaultErrorCallback = (error: any) => {
  showErrorModal(get(error, "message", ""));
};
