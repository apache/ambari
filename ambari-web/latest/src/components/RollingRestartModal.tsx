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

import { get } from "lodash";
import Modal from "./Modal";
import { useEffect, useState } from "react";
import { pluralize, validateInteger } from "../screens/Hosts/utils";
import { Alert, Form } from "react-bootstrap";
import modalManager from "../store/ModalManager";
import { translate, translateWithVariables } from "../Utils/Utility";

interface RollingRestartModalProps {
  hostComponentName: string;
  serviceName: string;
  isServiceInMM: boolean;
  staleConfigsOnly: boolean;
  skipMaintenance: boolean;
  allHostComponents: any[];
  turnOnMm: boolean;
  isOpen: boolean;
  onClose: () => void;
  successCallback: (
    restartComponents: any,
    batchSize: number,
    waitTime: number,
    tolerateSize: number,
    turnOnMm: boolean
  ) => void;
}

export default function RollingRestartModal({
  hostComponentName,
  serviceName,
  isServiceInMM,
  staleConfigsOnly,
  skipMaintenance,
  allHostComponents,
  turnOnMm,
  isOpen,
  onClose,
  successCallback,
}: RollingRestartModalProps) {
  const [formData, setFormData] = useState({
    batchSize: "-1",
    interBatchWaitTimeSeconds: "-1",
    tolerateSize: "-1",
    staleConfigsOnly: false,
    turnOnMm: false,
  });

  const [errorsList, setErrorsList] = useState<string[]>([]);
  const [warningsList, setWarningsList] = useState<string[]>([]);

  useEffect(() => {
    initialize();
  }, []);

  useEffect(() => {
    validate();
  }, [JSON.stringify(formData)]);

  const initialize = () => {
    if (
      get(formData, "batchSize") === "-1" &&
      get(formData, "interBatchWaitTimeSeconds") === "-1" &&
      get(formData, "tolerateSize") === "-1"
    ) {
      const restartCount = restartHostComponents().length;
      let calculatedBatchSize = "1";

      if (restartCount > 10 && hostComponentName !== "DATANODE") {
        calculatedBatchSize = Math.ceil(restartCount / 10).toString();
      }

      setFormData({
        batchSize: calculatedBatchSize,
        tolerateSize: calculatedBatchSize,
        interBatchWaitTimeSeconds: "120",
        staleConfigsOnly: staleConfigsOnly,
        turnOnMm: turnOnMm,
      });
    }
  };

  const validate = () => {
    const displayName = pluralize(hostComponentName);
    const componentName = get(allHostComponents, "[0].componentName", "");
    const totalCount = restartHostComponents().length;
    const bs = get(formData, "batchSize");
    const ts = get(formData, "tolerateSize");
    const wait = get(formData, "interBatchWaitTimeSeconds");
    let errors = [];
    let warnings = [];
    let bsError = "";
    let tsError = "";
    let waitError = "";
    if (totalCount < 1) {
      errors.push(
        translateWithVariables("rollingrestart.dialog.msg.noRestartHosts", {
          "0": displayName,
        }) as string
      );
    } else {
      if (componentName == "DATANODE") {
        if (parseInt(bs) > 1) {
          warnings.push(
            translate(
              "rollingrestart.dialog.warn.datanode.batch.size"
            ) as string
          );
        }
        bsError = validateInteger(bs, 1, undefined);
      } else {
        bsError = validateInteger(bs, 1, totalCount);
      }
      tsError = validateInteger(ts, 0, totalCount);
      if (bsError) {
        errors.push(
          translateWithVariables(
            "rollingrestart.dialog.err.invalid.batchsize",
            {
              "0": bsError,
            }
          ) as string
        );
      }
      if (tsError) {
        errors.push(
          translateWithVariables(
            "rollingrestart.dialog.err.invalid.toleratesize",
            {
              "0": tsError,
            }
          ) as string
        );
      }
    }
    waitError = validateInteger(wait, 0, undefined);
    if (waitError) {
      errors.push(
        translateWithVariables("rollingrestart.dialog.err.invalid.waitTime", {
          "0": waitError,
        }) as string
      );
    }
    setErrorsList(errors);
    setWarningsList(warnings);
  };

  const restartHostComponents = () => {
    let hostComponents = skipMaintenance
      ? allHostComponents
      : nonMaintainanceHostComponents();
    if (get(formData, "staleConfigsOnly")) {
      hostComponents = hostComponents.filter(
        (hostComponent) => get(hostComponent, "staleConfigs") === true
      );
    }
    return hostComponents;
  };

  const nonMaintainanceHostComponents = () => {
    return allHostComponents.filter(
      (hostComponent) => get(hostComponent, "passiveState") !== "ON"
    );
  };

  const restartMessage = () => {
    return translateWithVariables("rollingrestart.dialog.msg.restart", {
      "0": pluralize(hostComponentName),
    });
  };

  const componentsWithMaintenanceHost = () => {
    return allHostComponents.filter(
      (hostComponent) => get(hostComponent, "passiveState") === "ON"
    );
  };

  const maintainanceMessage = () => {
    const count = componentsWithMaintenanceHost().length;
    if (count > 0) {
      return translateWithVariables("rollingrestart.dialog.msg.maintainance", {
        "0": count.toString(),
      });
    }
    return null;
  };

  const suggestTurnOnMaintenanceMsg = () => {
    if (!isServiceInMM) {
      return translateWithVariables(
        "rollingrestart.dialog.msg.serviceNotInMM",
        {
          "0": serviceName,
        }
      );
    }
    return null;
  };

  const batchSizeMessage = () => {
    return translateWithVariables(
      "rollingrestart.dialog.msg.componentsAtATime",
      {
        "0": pluralize(hostComponentName),
      }
    );
  };

  const staleConfigsOnlyMessage = () => {
    return translateWithVariables(
      "rollingrestart.dialog.msg.staleConfigsOnly",
      {
        "0": pluralize(hostComponentName),
      }
    );
  };

  const turnOnMmMsg = () => {
    return translateWithVariables("passiveState.turnOnFor", {
      "0": serviceName,
    });
  };

  const getModalBody = () => {
    return (
      <div>
        <Alert variant="info">
          <div>{restartMessage()}</div>
          {maintainanceMessage() && (
            <div className="mt-1">{maintainanceMessage()}</div>
          )}
          {suggestTurnOnMaintenanceMsg() && (
            <div className="mt-1">{suggestTurnOnMaintenanceMsg()}</div>
          )}
        </Alert>
        <Form>
          <Form.Group className="mb-1 d-flex">
            <Form.Label className="mt-2 me-3 w-25 d-flex justify-content-end">
              {translate("common.restart")}
            </Form.Label>
            <Form.Control
              type="text"
              value={formData.batchSize}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  batchSize: e.target.value,
                })
              }
              className="custom-form-control w-20"
            />
            <Form.Label className="mt-2 ms-3">{batchSizeMessage()}</Form.Label>
          </Form.Group>
          <Form.Group className="mb-1 d-flex">
            <Form.Label className="mt-2 me-3 w-25 d-flex justify-content-end">
              {translate("rollingrestart.dialog.msg.timegap.prefix")}
            </Form.Label>
            <Form.Control
              type="text"
              value={formData.interBatchWaitTimeSeconds}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  interBatchWaitTimeSeconds: e.target.value,
                })
              }
              className="custom-form-control w-20"
            />
            <Form.Label className="mt-2 ms-3">
              {translate("rollingrestart.dialog.msg.timegap.suffix")}
            </Form.Label>
          </Form.Group>
          <Form.Group className="mb-2 d-flex">
            <Form.Label className="mt-2 me-3 w-25 d-flex justify-content-end">
              {translate("rollingrestart.dialog.msg.toleration.prefix")}
            </Form.Label>
            <Form.Control
              type="text"
              value={formData.tolerateSize}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  tolerateSize: e.target.value,
                })
              }
              className="custom-form-control w-20"
            />
            <Form.Label className="mt-2 ms-3">
              {translate("rollingrestart.dialog.msg.toleration.suffix")}
            </Form.Label>
          </Form.Group>
          <Form.Group className="mb-1 d-flex">
            <Form.Check
              checked={formData.staleConfigsOnly}
              onChange={() =>
                setFormData({
                  ...formData,
                  staleConfigsOnly: !get(formData, "staleConfigsOnly"),
                })
              }
              className="custom-checkbox w-25 d-flex justify-content-end"
            />
            <Form.Label className="mt-1 ms-2">
              {staleConfigsOnlyMessage()}
            </Form.Label>
          </Form.Group>
          {suggestTurnOnMaintenanceMsg() && (
            <Form.Group className="mb-3 d-flex">
              <Form.Check
                checked={formData.turnOnMm}
                onChange={() =>
                  setFormData({
                    ...formData,
                    turnOnMm: !get(formData, "turnOnMm"),
                  })
                }
                className="custom-checkbox w-25 d-flex justify-content-end"
              />
              <Form.Label className="mt-1 ms-2">{turnOnMmMsg()}</Form.Label>
            </Form.Group>
          )}
        </Form>
        {errorsList.length > 0 && (
          <Alert variant="danger">
            <ul className="m-0">
              {errorsList.map((error: string) => {
                return (
                  <li key={error} className="text-danger">
                    {error}
                  </li>
                );
              })}
            </ul>
          </Alert>
        )}
        {warningsList.length > 0 && (
          <Alert variant="warning" className="mt-2">
            <ul className="m-0">
              {warningsList.map((warning: string) => {
                return <li key={warning}>{warning}</li>;
              })}
            </ul>
          </Alert>
        )}
      </div>
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      modalTitle={
        translateWithVariables("rollingrestart.dialog.title", {
          "0": hostComponentName,
        }) as string
      }
      modalBody={getModalBody()}
      successCallback={() => {
        successCallback(
          restartHostComponents(),
          parseInt(formData.batchSize),
          parseInt(formData.interBatchWaitTimeSeconds),
          parseInt(formData.tolerateSize),
          formData.turnOnMm
        );
        modalManager.hide();
      }}
      options={{
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
        okButtonDisabled: errorsList.length > 0,
        okButtonText: translate("rollingrestart.dialog.primary") as string,
      }}
    />
  );
}
