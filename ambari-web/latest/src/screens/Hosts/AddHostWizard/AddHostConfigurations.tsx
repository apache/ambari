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

import { useContext, useEffect, useMemo, useRef, useState } from "react";
import { AppContext } from "../../../store/context";
import ConfigGroupApi from "../../../api/configGroupApi";
import { cloneDeep, get, isEmpty, set } from "lodash";
import Table from "../../../components/Table";
import { Alert, Button, Form } from "react-bootstrap";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import { ContextWrapper } from "../../ClusterWizard";
import { ActionTypes } from "./wizardDataStore/types";
import { translate } from "../../../Utils/Utility";
import Spinner from "../../../components/Spinner";
import {
  buildAddHostConfigGroups,
  selectedAddHostServices,
} from "../../../Utils/hostWizard";

export default function AddHostConfigurations() {
  const { clusterName } = useContext(AppContext);
  const { Context } = useContext(ContextWrapper);
  const {
    dispatch,
    state,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      jumpToStep,
      prevStepNumber,
    },
  }: any = useContext(Context);

  const initialConfigs = get(
    state,
    `addHostSteps.CONFIGURATIONS.data.configurations`,
    []
  );

  const [configGroups, setConfigGroups] = useState({});
  const [formData, setFormData] = useState(initialConfigs);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const skipped = useRef(false);
  const selectedServices = useMemo(() => selectedAddHostServices(
    get(state, "addHostSteps.SLAVES_AND_CLIENTS.data.serviceComponents", []),
    get(state, "addHostSteps.SLAVES_AND_CLIENTS.data.allServiceComponentsList", []),
  ), [state]);

  useEffect(() => {
    if (!clusterName) {
      return;
    }
    if (selectedServices.length === 0) {
      if (!skipped.current) {
        skipped.current = true;
        dispatch({
          type: ActionTypes.STORE_INFORMATION,
          payload: { step: currentStep.name, data: { configurations: [] } },
        });
        void Promise.resolve(flushStateToDb("next")).then(handleNextImperitive);
      }
      setLoading(false);
      return;
    }
    void getConfigGroups();
  }, [clusterName, retryCount, selectedServices.join(",")]);

  useEffect(() => {
    if (!isEmpty(configGroups)) {
      setFormData(buildAddHostConfigGroups(
        selectedServices,
        configGroups,
        clusterName,
        initialConfigs,
      ));
    }
  }, [configGroups, clusterName, selectedServices.join(",")]);

  const getConfigGroups = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await ConfigGroupApi.getConfigGroupsForServices(
        clusterName,
        selectedServices,
      );
      setConfigGroups(response);
    } catch (requestError: any) {
      setError(
        requestError?.response?.data?.message || "Ambari could not load configuration groups.",
      );
    } finally {
      setLoading(false);
    }
  };

  const getSelectedConfigGroupName = (configGroups: any[]) => {
    return configGroups.filter((cg: any) => cg.isSelected)?.[0]?.group_name;
  };

  const setSelectedConfigGroup = (
    configGroupName: string,
    serviceName: string
  ) => {
    const formDataCopy = cloneDeep(formData);
    formDataCopy.forEach((service: any) => {
      if (get(service, "serviceName") === serviceName) {
        get(service, "configGroups", []).forEach((cg: any) => {
          if (get(cg, "group_name") === configGroupName) {
            set(cg, "isSelected", true);
          } else {
            set(cg, "isSelected", false);
          }
        });
      }
    });
    setFormData(formDataCopy);
  };

  const columnInTable = [
    {
      header: translate("common.service"),
      id: "service",
      width: "40%",
      cell: (info: any) => {
        return get(info, "row.original.serviceName");
      },
    },
    {
      header: translate("common.conf.group"),
      id: "configGroup",
      width: "60%",
      cell: (info: any) => {
        const serviceName = get(info, "row.original.serviceName");
        const configGroups = get(info, "row.original.configGroups");
        return (
          <Form.Select
            className="custom-form-control fs-12 w-50"
            value={getSelectedConfigGroupName(configGroups)}
            onChange={(e) => {
              setSelectedConfigGroup(e.target.value, serviceName);
            }}
          >
            {configGroups.map((configGroup: any) => {
              return (
                <option
                  key={get(configGroup, "group_name")}
                  value={get(configGroup, "group_name")}
                >
                  {get(configGroup, "group_name")}
                </option>
              );
            })}
          </Form.Select>
        );
      },
    },
  ];

  const moveToNextStep = () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: { configurations: formData },
      },
    });
    flushStateToDb("next");
    handleNextImperitive();
  };

  return (
    <div>
      <h2 className="step-title">{translate("addHost.step4.header")}</h2>
      <p className="make-all-grey step-description">
        {translate("addHost.step4.title")}
      </p>
      {error && (
        <Alert variant="danger">
          {error}{" "}
          <Button size="sm" variant="outline-danger" onClick={() => setRetryCount((value) => value + 1)}>
            Retry
          </Button>
        </Alert>
      )}
      {loading ? <Spinner /> : <Table data={formData} columns={columnInTable} />}
      <WizardFooter
        isNextEnabled={!loading && !error}
        step={currentStep}
        onNext={() => {
          moveToNextStep();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(prevStepNumber);
        }}
      />
    </div>
  );
}
