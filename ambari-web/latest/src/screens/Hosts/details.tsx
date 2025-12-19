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

import { get, set } from "lodash";
import {
  addDeleteComponentsMap,
  getComponentDisplayName,
  getComponentName,
  getHostComponentsInfo,
  serviceNonClientActiveComponents,
  setRackInfo,
  showConfirmationPopup,
} from "./utils";
import modalManager from "../../store/ModalManager";
import { HostsApi } from "../../api/hostsApi";
import {
  showAlertModal,
  showErrorModal,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";
import {
  defaultSuccessCallback,
  infoPassiveState,
  restartHostComponents,
} from "./batchUtils";
import { IHostStackVersion } from "../../models/hostStackVersion";
import { Alert } from "react-bootstrap";
import { IHostComponent } from "../../models/hostComponent";
import { ComponentStatus } from "./enums";
import { checkNnLastCheckpointTime } from "./actions";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faWarning } from "@fortawesome/free-solid-svg-icons";
import { IHost } from "../../models/host";
import {
  downloadClientConfigsCall,
  ResourceTypeEnum,
} from "./supportClientConfigsDownload";
//TODO: uncomment below lines and their usages after these components are available
// import BackgroundOperations from "../BackgroundOperations";
// import RecommendationModal from "../../components/RecommendationModal";

export const doAction = (option: any) => {
  switch (option.action) {
    case "deleteHost":
      validateAndDeleteHost(option);
      break;
    case "startAllComponents":
      doStartAllComponents(option);
      break;
    case "stopAllComponents":
      doStopAllComponents(option);
      break;
    case "restartAllComponents":
      doRestartAllComponents(option);
      break;
    case "onOffPassiveModeForHost":
      onOffPassiveModeForHost(option);
      break;
    case "setRackId":
      setRackIdForHost(option);
      break;
    case "downloadClientConfigs":
      downloadClientConfigs(option);
      break;
    case "downloadAllClientConfigs":
      downloadAllClientConfigs(option);
      break;
    case "regenerateKeytabFileOperations":
      regenerateKeytabFileOperations(option);
      break;
    default:
      break;
  }
};

const setRackIdForHost = (option: any) => {
  const operationData = {
    message: translate("hosts.host.details.setRackId"),
  };
  setRackInfo(
    operationData,
    [
      {
        hostName: option.hostName,
      },
    ],
    option.clusterName,
    option.callback,
    option.rack
  );
};

const onOffPassiveModeForHost = (context: any) => {
  const state = context.active ? "ON" : "OFF";
  const message = translateWithVariables("hosts.host.details.for.postfix", {
    "0": context.label,
  }) as string;
  let popupInfo = translateWithVariables("hosts.passiveMode.popup", {
    "0": context.active ? "On" : "Off",
    "1": context.hostName,
  });

  let popupAlert = "";

  if (state === "OFF") {
    const currentHostVersion = get(context, "host.stackVersions", []).find(
      (version: IHostStackVersion) => version.isCurrent()
    )?.repoVersion;
    const currentClusterVersion = get(context, "clusterStackVersion", []).find(
      (version: any) => version.state === "CURRENT"
    )?.repository_version;
    if (currentHostVersion !== currentClusterVersion) {
      popupAlert = translateWithVariables(
        "hosts.passiveMode.popup.version.mismatch",
        {
          "0": context.hostName,
          "1": currentClusterVersion,
        }
      ) as string;
    }
  }
  let modalProps = {
    isOpen: true,
    onClose: () => {},
    modalTitle: translate("popup.confirmation.commonHeader"),
    modalBody: (
      <div>
        <div>{popupInfo}</div>
        {popupAlert ? (
          <Alert variant="warning" className="mt-2">
            {popupAlert}
          </Alert>
        ) : null}
      </div>
    ),
    successCallback: () => {
      hostPassiveModeRequest(
        state,
        message,
        context.hostName,
        context.clusterName
      );
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

const hostPassiveModeRequest = async (
  state: string,
  message: string,
  hostNames: string,
  clusterName: string
) => {
  const data = {
    RequestInfo: {
      context: message,
      query: "Hosts/host_name.in(" + hostNames + ")",
    },
    Body: {
      Hosts: {
        maintenance_state: state,
      },
    },
  };

  try {
    await HostsApi.updateHost(clusterName, data);
    updateHost(state);
  } catch (error) {
    showErrorModal(message + get(error, "message", ""));
  }
};

const updateHost = (state: string) => {
  infoPassiveState(state);
  // setTimeout(() => {
  //   window.location.reload();
  // }, 2000);
};

const doStartAllComponents = (context: any) => {
  const hostComponents: IHostComponent[] = get(
    context,
    "host.hostComponents",
    []
  );
  const components = serviceNonClientActiveComponents(hostComponents);
  if (components.length) {
    showConfirmationPopup(translate("question.sure.startAll") as string, () =>
      sendComponentsCommand(
        components,
        translate(
          "hosts.host.maintainance.startAllComponents.context"
        ) as string,
        ComponentStatus.STARTED
      )
    );
  }
};

export const sendComponentsCommand = async (
  component: IHostComponent[],
  context: string,
  state: string
) => {
  const clusterName = get(component, "[0].clusterName", "");
  const hostName = get(component, "[0].hostName", "");
  const query =
    "HostRoles/component_name.in(" +
    component.map(getComponentName).join(",") +
    ")";
  const data = {
    RequestInfo: {
      context: context,
      operation_level: {
        level: "HOST",
        cluster_name: clusterName,
        host_name: hostName,
      },
      query: query,
    },
    Body: {
      HostRoles: {
        state: state,
      },
    },
  };

  const response = await HostsApi.updateHostComponentsForHost(
    clusterName,
    hostName,
    query,
    data
  );
  const requestId = get(response, "Requests.id", -1);
  if (requestId !== -1) {
    // modalManager.show(
    //   <BackgroundOperations
    //     isOpen={true}
    //     onClose={() => {
    //       modalManager.hide();
    //     }}
    //     requestId={requestId}
    //   />
    // );
  }
};

const doStopAllComponents = (context: any) => {
  const hostComponents: IHostComponent[] = get(
    context,
    "host.hostComponents",
    []
  );
  const hostName = context.hostName;
  const clusterName = get(context, "clusterName", "");
  const components: IHostComponent[] =
    serviceNonClientActiveComponents(hostComponents);
  if (components.length) {
    if (
      components.find(
        (component: IHostComponent) =>
          getComponentName(component) === "NAMENODE"
      )?.workStatus === ComponentStatus.STARTED
    ) {
      checkNnLastCheckpointTime(
        () =>
          showConfirmationPopup(
            translate("question.sure.stopAll") as string,
            () =>
              sendComponentsCommand(
                components,
                translate(
                  "hosts.host.maintainance.stopAllComponents.context"
                ) as string,
                ComponentStatus.STOPPED
              )
          ),
        hostName,
        clusterName
      );
    } else {
      showConfirmationPopup(translate("question.sure.stopAll") as string, () =>
        sendComponentsCommand(
          components,
          translate(
            "hosts.host.maintainance.stopAllComponents.context"
          ) as string,
          ComponentStatus.STOPPED
        )
      );
    }
  }
};

const doRestartAllComponents = (context: any) => {
  const hostComponents: IHostComponent[] = get(
    context,
    "host.hostComponents",
    []
  );
  const hostName = context.hostName;
  const clusterName = get(context, "clusterName", "");
  const components: IHostComponent[] =
    serviceNonClientActiveComponents(hostComponents);
  if (components.length) {
    if (
      components.find(
        (component: IHostComponent) =>
          getComponentName(component) === "NAMENODE"
      )?.workStatus === ComponentStatus.STARTED
    ) {
      checkNnLastCheckpointTime(
        () =>
          showConfirmationPopup(
            translate("question.sure.restartAll") as string,
            () =>
              restartHostComponents(
                components,
                translateWithVariables(
                  "rollingrestart.context.allOnSelectedHost",
                  {
                    "0": hostName,
                  }
                ) as string,
                "HOST"
              )
          ),
        hostName,
        clusterName
      );
    } else {
      showConfirmationPopup(
        translate("question.sure.restartAll") as string,
        () =>
          restartHostComponents(
            components,
            translateWithVariables("rollingrestart.context.allOnSelectedHost", {
              "0": hostName,
            }) as string,
            "HOST"
          )
      );
    }
  }
};

const downloadClientConfigs = (context: any) => {
  const data = {
    clusterName: context.clusterName,
    hostName: context.hostName,
    componentName: context.componentName,
    resourceType: ResourceTypeEnum.HOST_COMPONENT,
  };
  downloadClientConfigsCall(data);
};

const downloadAllClientConfigs = (context: any) => {
  const data = {
    clusterName: context.clusterName,
    hostName: context.hostName,
    resourceType: ResourceTypeEnum.HOST,
  };
  downloadClientConfigsCall(data);
};

export const confirmRecoverHost = (context: any) => {
  const host: IHost = context.host;
  const hostComponents: IHostComponent[] = get(host, "hostComponents", []);
  const allowedStates = [
    ComponentStatus.STOPPED,
    ComponentStatus.INSTALL_FAILED,
    ComponentStatus.INIT,
  ];
  let componentsNotStopped: IHostComponent[] = [];
  hostComponents.forEach((component: IHostComponent) => {
    if (!allowedStates.includes(component.workStatus as ComponentStatus)) {
      componentsNotStopped.push(component);
    }
  });
  if (componentsNotStopped.length) {
    let body = translate("hosts.recover.error.popup.body") as string;
    if (body.includes("{0}")) {
      body = body.replace(
        "{0}",
        componentsNotStopped
          .map((component: IHostComponent) => getComponentName(component))
          .join(", ")
      );
    }
    const modalProps = {
      isOpen: true,
      onClose: () => {},
      modalTitle: translate("hosts.recover.error.popup.title"),
      modalBody: (
        <Alert variant="warning">
          <FontAwesomeIcon icon={faWarning} className="text-warning" />
          {body}
        </Alert>
      ),
      successCallback: () => {
        modalManager.hide();
      },
      options: {
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: false,
        okButtonVariant: "primary",
      },
    };
    modalManager.show(modalProps);
  } else {
    const modalProps = {
      isOpen: true,
      onClose: () => {},
      modalTitle: translate("hosts.recover.popup.title"),
      modalBody: translate("hosts.recover.popup.body"),
      successCallback: () => {
        recoverHost(context);
        modalManager.hide();
      },
      options: {
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
        okButtonText: "YES",
        cancelButtonText: "NO",
      },
    };
    modalManager.show(modalProps);
  }
};

const recoverHost = async (context: any) => {
  const host: IHost = context.host;
  const components = host.hostComponents;
  const hostName = host.hostName;
  const clusterName = get(context, "clusterName", "");
  const isKerberosEnabled = get(context, "isKerberosEnabled", false);
  const batches: any[] = [
    {
      order_id: 1,
      type: "PUT",
      uri: `/clusters/${clusterName}/hosts/${hostName}/host_components`,
      RequestBodyInfo: {
        RequestInfo: {
          context: translate("hosts.host.recover.initAllComponents.context"),
          operation_level: {
            level: "HOST",
            cluster_name: clusterName,
            host_name: hostName,
          },
          query: `HostRoles/component_name.in(${components
            .map((c) => c.componentName)
            .join(",")})`,
        },
        Body: {
          HostRoles: {
            state: "INIT",
          },
        },
      },
    },
  ];

  batches.push({
    order_id: 2,
    type: "PUT",
    uri: `/clusters/${clusterName}/hosts/${hostName}/host_components`,
    RequestBodyInfo: {
      RequestInfo: {
        context: translate("hosts.host.recover.installAllComponents.context"),
        operation_level: {
          level: "HOST",
          cluster_name: clusterName,
          host_name: hostName,
        },
        query: `HostRoles/component_name.in(${components
          .map((c) => c.componentName)
          .join(",")})`,
      },
      Body: {
        HostRoles: {
          state: "INSTALLED",
        },
      },
    },
  });

  if (isKerberosEnabled) {
    batches.push({
      order_id: 3,
      type: "PUT",
      uri: `/clusters/${clusterName}`,
      RequestBodyInfo: {
        RequestInfo: {
          context: translate("hosts.host.recover.regenerateKeytabs.context"),
          query: `regenerate_keytabs=all&regenerate_hosts=${hostName}&config_update_policy=none`,
        },
        Body: {
          Clusters: {
            security_type: "KERBEROS",
          },
        },
      },
    });
  }

  await context.getKDCSessionState(() => {
    doRecoverHost(batches, clusterName);
  });
};

const doRecoverHost = async (batches: any[], clusterName: string) => {
  const data = {
    intervalTimeSeconds: 1,
    tolerateSize: 0,
    batches: batches,
  };
  try {
    const respone = await HostsApi.batchRequest(clusterName, data);
    recoverHostSuccessCallback(respone);
  } catch (error) {
    showErrorModal(get(error, "message", ""));
  }
};

const recoverHostSuccessCallback = (response: any) => {
  const requestId = get(response, "data.resources.[0].RequestSchedule.id", -1);
  if (requestId !== -1) {
    // modalManager.show(
    //   <BackgroundOperations
    //     isOpen={true}
    //     onClose={() => {
    //       modalManager.hide();
    //     }}
    //     requestId={requestId}
    //   />
    // );
  }
};

const validateAndDeleteHost = (context: any) => {
  const clusterComponents = get(context, "clusterComponents", []);
  const serviceModels = get(context, "serviceModels", {});
  const container = getHostComponentsInfo(
    get(context, "host.hostComponents", []),
    clusterComponents,
    serviceModels
  );
  const properties = {};
  const hostData = get(context, "host", {} as IHost);
  if (container.nonDeletableComponents.length > 0) {
    raiseDeleteComponentsError(container, "nonDeletableList", hostData);
    return;
  } else if (container.nonAddableMasterComponents.length > 0) {
    raiseDeleteComponentsError(container, "masterList", hostData);
    return;
  } else if (container.runningComponents.length > 0) {
    raiseDeleteComponentsError(container, "runningList", hostData);
    return;
  } else if (container.lastMasterComponents.length > 0) {
    raiseDeleteComponentsError(container, "lastMasterList", hostData);
    return;
  }

  set(properties, "fromDeleteHost", true);

  if (container.isReconfigureRequired) {
    reconfigureAndDeleteHost(container, context, properties);
  } else {
    confirmDeleteHost(container, context);
  }
};

const raiseDeleteComponentsError = (
  container: any,
  type: string,
  hostData: IHost
) => {
  let components = [];
  if (type === "nonDeletableList") {
    components = container.nonDeletableComponents;
  } else if (type === "masterList") {
    components = container.nonAddableMasterComponents;
  } else if (type === "runningList") {
    components = container.runningComponents;
  } else if (type === "lastMasterList") {
    components = container.lastMasterComponents;
  }

  let componentsBody = translate(`hosts.cant.do.popup.${type}.body`) as string;
  if (componentsBody.includes("{0}")) {
    componentsBody = componentsBody.replace("{0}", components.length);
  }

  const hostComponents = hostData.hostComponents.filter(
    (component: IHostComponent) =>
      components.includes(getComponentDisplayName(component))
  );
  const decommissionableComponents = hostComponents.filter(
    (component: IHostComponent) => component.decommissionAllowed
  );

  const showBodyEnd = ["runningList", "masterList", "lastMasterList"].includes(
    type
  );
  let componentsBodyEnd = "";
  if (showBodyEnd) {
    componentsBodyEnd = translate(
      `hosts.cant.do.popup.${type}.body.end`
    ) as string;
    if (componentsBodyEnd.includes("{0}")) {
      componentsBodyEnd = componentsBodyEnd.replace(
        "{0}",
        decommissionableComponents.map(getComponentDisplayName).join(", ")
      );
    }
  }

  const modalProps = {
    isOpen: true,
    onClose: () => {},
    modalTitle: translate("hosts.cant.do.popup.title"),
    modalBody: (
      <div>
        <Alert variant="warning" className="mt-2">
          <div className="d-flex">
            <div className="me-4">
              <FontAwesomeIcon icon={faWarning} className="text-warn" />
            </div>
            <div>
              <div className="text-dark fw-bold mb-2">{componentsBody}</div>
              <div>{components.join(", ")}</div>
              {showBodyEnd ? (
                <div className="mt-4">{componentsBodyEnd}</div>
              ) : null}
            </div>
          </div>
        </Alert>
      </div>
    ),
    successCallback: () => {
      modalManager.hide();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: false,
      okButtonVariant: "primary",
    },
  };

  modalManager.show(modalProps);
};

const reconfigureAndDeleteHost = (
  _container: any,
  context: any,
  properties: any
) => {
  const hostName = get(context, "hostName");
  let reconfiguredComponents: any = [];
  const loadComponentRelatedConfigs = get(
    context,
    "loadComponentRelatedConfigs"
  );

  get(context, "host.hostComponents", []).forEach(
    (component: IHostComponent) => {
      const componentsMapItem = addDeleteComponentsMap[component.componentName];
      if (componentsMapItem) {
        reconfiguredComponents.push(component.displayName);
        if (componentsMapItem.hostPropertyName) {
          set(properties, componentsMapItem.hostPropertyName, hostName);
        }
        if (componentsMapItem.addPropertyName) {
          set(properties, componentsMapItem.addPropertyName, true);
        }
        loadComponentRelatedConfigs(
          componentsMapItem.configTagsCallbackName,
          componentsMapItem.configsCallbackName,
          properties
        );
      }
    }
  );

  // modalManager.show(
  //   <RecommendationModal
  //     isOpen={true}
  //     onClose={() => {
  //       modalManager.hide();
  //     }}
  //     componentDisplayName={reconfiguredComponents.join(", ")}
  //     add={false}
  //     callback={() => confirmDeleteHost(container, context)}
  //     commonMessage={
  //       translateWithVariables(
  //         "hosts.host.delete.componentsRequireReconfigure",
  //         {
  //           "0": reconfiguredComponents.join(", "),
  //         }
  //       ) as string
  //     }
  //   />
  // );
};

const confirmDeleteHost = (container: any, context: any) => {
  const publicHostName = get(context, "host.publicHostName", "");

  const header = translate("hosts.delete.popup.title");
  const deletePopupBody = translateWithVariables("hosts.delete.popup.body", {
    "0": publicHostName,
  });

  let lastComponentError = "";
  if (container.lastComponents.length > 0) {
    lastComponentError = translateWithVariables(
      "hosts.delete.popup.body.msg4",
      {
        "0": container.lastComponents.join(", "),
      }
    ) as string;
  }

  let unknownComponents = "";
  if (container.unknownComponents.length > 0) {
    unknownComponents = container.unknownComponents.join(", ");
  }

  let decommissionWarning = "";
  if (container.toDecommissionComponents.length > 0) {
    decommissionWarning = translateWithVariables(
      "hosts.delete.popup.body.msg7",
      {
        "0": container.toDecommissionComponents.join(", "),
      }
    ) as string;
  }

  const modalProps = {
    isOpen: true,
    onClose: () => {},
    modalTitle: header,
    modalBody: (
      <div>
        {unknownComponents ? (
          <div>
            <div>{translate("hosts.delete.popup.unknownComponents")}</div>
            <div className="mt-1">{unknownComponents}</div>
          </div>
        ) : null}
        <div>
          <FontAwesomeIcon icon={faWarning} className="text-warning me-2" />
          {deletePopupBody}
        </div>
        {lastComponentError ? (
          <div className="mt-2">
            <Alert variant="warning" className="mt-2">
              {lastComponentError}
            </Alert>
          </div>
        ) : null}
        {decommissionWarning ? (
          <div className="mt-2">
            <Alert variant="warning" className="mt-2">
              {decommissionWarning}
            </Alert>
          </div>
        ) : null}
        <Alert variant="warning" className="mt-2">
          <div>{translate("common.important.strong")}</div>
          {unknownComponents ? (
            <div className="mt-2">
              {translate("hosts.delete.popup.body.msg.unknownComponents")}
            </div>
          ) : null}
          <div>{translate("hosts.delete.popup.body.msg1")}</div>
        </Alert>
        {!unknownComponents ? (
          <Alert variant="warning" className="mt-2">
            <span>{translate("hosts.delete.popup.body.msg5")}</span>
            <span className="text-danger">
              {translate("hosts.delete.popup.body.msg6")}
            </span>
          </Alert>
        ) : null}
        <Alert variant="warning" className="mt-2">
          <div>{translate("common.important.strong")}</div>
          <div>{translate("hosts.delete.popup.body.msg3")}</div>
        </Alert>
      </div>
    ),
    successCallback: () => {
      doDeleteHost(context, container, {}, {});
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

const doDeleteHost = (
  context: any,
  container: any,
  groupedPropertiesToChange: any,
  deletedHostComponentError: any
) => {
  const allComponents = get(context, "host.hostComponents", []);
  const doDeleteHostComponent = get(context, "doDeleteHostComponent");
  const applyConfigsCustomization = get(
    context,
    "applyConfigsCustomization",
    () => {}
  );
  const putConfigsToServer = get(context, "putConfigsToServer", () => {});
  const clearConfigsChanges = get(context, "clearConfigsChanges", () => {});
  let deleteRequests = [];

  const deleteHost = async () => {
    if (allComponents.length > 0) {
      for (const component of allComponents) {
        deleteRequests.push(doDeleteHostComponent(component));
      }
      try {
        await Promise.all(deleteRequests);
        if (container.isReconfigureRequired) {
          const reconfiguredComponents = allComponents
            .filter(
              (component: IHostComponent) =>
                addDeleteComponentsMap[component.componentName]
            )
            .map((component: any) => component.displayName)
            .join(", ");
          applyConfigsCustomization();
          putConfigsToServer(groupedPropertiesToChange, reconfiguredComponents);
          clearConfigsChanges();
        }
        await deleteHostCall(context);
      } catch (e) {
        set(
          deletedHostComponentError,
          "xhr.responseText",
          `{"message": "${get(
            deletedHostComponentError,
            "xhr.statusText",
            ""
          )}"}`
        );
        showErrorModal(get(deletedHostComponentError, "xhr.responseText", ""));
      }
    }
  };

  return deleteHost();
};

const deleteHostCall = async (context: any) => {
  const clusterName = get(context, "host.cluster", "");
  const hostName = get(context, "host.hostName", "");
  try {
    await HostsApi.deleteHost(clusterName, hostName);
    deleteHostCallSuccessCallback();
  } catch (error) {
    deleteHostCallErrorCallback(error);
  }
};

const deleteHostCallSuccessCallback = () => {
  window.location.href = "#/main/hosts";
};

const deleteHostCallErrorCallback = (error: any) => {
  const errorMessage = get(error, "message", "");
  showErrorModal(errorMessage);
};

const regenerateKeytabFileOperations = (context: any) => {
  const clusterName = get(context, "clusterName", "");
  const hostName = get(context, "hostName", "");
  showConfirmationPopup(
    translateWithVariables("question.sure.regenerateKeytab.host", {
      "0": hostName,
    }) as string,
    async () => {
      try {
        const response = await HostsApi.regenerateKeytabsForHost(
          clusterName,
          hostName
        );
        const requestId = get(response, "Requests.id", -1);
        defaultSuccessCallback(requestId);
      } catch (error) {
        showAlertModal(
          translate("common.error"),
          translateWithVariables(
            "alerts.notifications.regenerateKeytab.host.error",
            {
              "0": hostName,
            }
          )
        );
      }
    }
  );
};
