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
import KerberosApi from "../../api/kerberosApi";
import { ServicesStackDescriptorConfigs } from "./addSecurityConfigs";
import useKerberosConfigs from "./useKerberosConfigs";
import { get, isEmpty, cloneDeep } from "lodash";
import { Alert, Button, Tab, Tabs } from "react-bootstrap";
import AdvancedConfigs from "../CommonConfigs/AdvancedConfigs";
import Spinner from "../../components/Spinner";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "../KerberosWizard/KerberosStore/context";
import { ActionTypes } from "../KerberosWizard/KerberosStore/types";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import { AppContext } from "../../store/context";
import { getTotalErros } from "../CommonConfigs/ConfigUtils";
import { translate } from "../../Utils/Utility";
import { RequestApi } from "../../api/requestApi";
import {
  applyKerberosRecommendations,
  buildDesiredConfigTagQuery,
  buildKerberosRecommendationPayload,
  collectDescriptorFormValues,
  isManualKdcPlan,
  removeDescriptorIdentityReferences,
  updateKerberosDescriptor as applyDescriptorValues,
} from "../../Utils/kerberosWizard";
import { responseErrorMessage } from "../../Utils/httpError";
import ConfigsApi from "../../api/configsApi";
import { generateHostGroups } from "../../Utils/Utility";

interface ConfigProperties {
    propertyName: string;
    propertyDisplayname: string;
    propertyDescription: string;
    propertyValue: string;
    propertyAttributes: {
        type: string;
        overridable: boolean;
    };
    previousValue: string;
    value: string;
    final: string;
    propertyDisplayValue?: string;
    errorMessages?: string[];
    confirmPassword?: boolean;
}

interface CategoryProperties {
    errors: number;
    properties: { [key: string]: ConfigProperties };
}

interface KerberosConfigProperties {
    [key: string]: { [key: string]: CategoryProperties };
}

type ConfigCategory = {
  name: string;
  displayName: string;
};

type StepConfig = {
  displayName: string;
  name: string;
  serviceName: string;
  configCategories: ConfigCategory[];
  configs: any[];
  configGroups: never[];
  showConfig: boolean;
};

function ConfigureIdentities() {

  const {
    state,
    dispatch,
    flushStateToDb,
    onExitPopUp,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      wizardSteps,
      handleBackImperitive,
      jumpToStep,
    },
  } = useContext(EnableKerberosContext);

  const [ loading, setLoading ] = useState(false);
  const { configId, kerberosIdentitiesMap } = useKerberosConfigs();
  const [ result, setResult ] = useState<Record<string, any>>({})
  const [ tabErrors, setTabErrors ] = useState({});
  const [ nextEnabled, setNextEnabled ] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const getStepConfigsRef = useRef<(configs: unknown) => StepConfig[]>(() => []);
  const transformDataRef = useRef<(data: unknown[]) => KerberosConfigProperties>(
    () => ({}),
  );
  const {
    allHostNames,
    cluster,
    clusterName,
    services,
    stackConfigurations,
    supports,
  } = useContext(AppContext);
  const configsStep2: ConfigPropertiesType = get(state, `kerberosWizardSteps.${wizardSteps[2].name}.data.configProperties`, {})
  const selectedKdcPlan = get(
    state,
    `kerberosWizardSteps.${wizardSteps[1].name}.data.selectedKdcPlan`,
    "",
  );
  const storedResult = get(
    state,
    `kerberosWizardSteps.${wizardSteps[4].name}.data.result`,
    null,
  );

  useEffect(() => {
    let active = true;
    async function getKerberosIdentities() {
      if (!isEmpty(storedResult)) {
        setResult(storedResult);
        setLoadError("");
        return;
      }
      setLoading(true);
      setLoadError("");
      try {
        const kerberosDescriptor =
          await KerberosApi.getKerberosDescriptorProperties("true", clusterName);

        const descriptorConfigs = ServicesStackDescriptorConfigs(
          kerberosDescriptor,
          kerberosIdentitiesMap,
          configId
        );
        const kerberosConfigs = getStepConfigsRef.current(descriptorConfigs);
        let nextResult = transformDataRef.current(kerberosConfigs);

        if (supports.kerberosStackAdvisor) {
          const stackName = get(cluster, "version", "").split("-")[0];
          const stackVersion = get(cluster, "version", "").split("-")[1];
          if (!stackName || !stackVersion) {
            throw new Error("The cluster stack version is unavailable.");
          }
          const desiredConfigResponse = await ConfigsApi.loadConfigTags(clusterName);
          const configQuery = buildDesiredConfigTagQuery(
            desiredConfigResponse?.Clusters?.desired_configs,
          );
          const configurations = configQuery
            ? (await ConfigsApi.getConfigsByTags(clusterName, configQuery))?.items ?? []
            : [];
          const hostGroups = await generateHostGroups(clusterName, allHostNames);
          const recommendationPayload = buildKerberosRecommendationPayload({
            hostNames: allHostNames,
            serviceNames: [
              ...services.map((service: any) =>
                service.ServiceInfo?.service_name,
              ).filter(Boolean),
              "KERBEROS",
            ],
            hostGroups,
            configurations,
            descriptorConfigs,
          });
          const recommendations = await ConfigsApi.getRecommendations(
            stackName,
            stackVersion,
            recommendationPayload,
          );
          nextResult = applyKerberosRecommendations(nextResult, recommendations);
        }

        if (active) {
          setResult(nextResult);
        }
      } catch (error) {
        if (active) {
          setLoadError(responseErrorMessage(
            error,
            "Ambari could not load the Kerberos descriptor and recommendations.",
          ));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }
    void getKerberosIdentities();
    return () => {
      active = false;
    };
  }, [
    allHostNames,
    cluster,
    clusterName,
    configId,
    kerberosIdentitiesMap,
    loadAttempt,
    services,
    storedResult,
    supports.kerberosStackAdvisor,
  ]);

  useEffect(() => {
   const noErros = getTotalErros(tabErrors);
   setNextEnabled(noErros);
  
  }, [tabErrors]);

  const transformData = (data: any[]): KerberosConfigProperties => {
    const kerberosConfigProperties: KerberosConfigProperties = {};

    data.forEach(section => {
      const serviceName = get(section, 'serviceName', '');
      if (!kerberosConfigProperties[serviceName]) {
        kerberosConfigProperties[serviceName] = {};
      }
  
      const configCategories = get(section, 'configCategories', []);
      configCategories.forEach((category: any) => {
        const categoryName = get(category, 'name', '');
        if(categoryName === 'KERBEROS') return;

        if (!kerberosConfigProperties[serviceName][categoryName]) {
          kerberosConfigProperties[serviceName][categoryName] = {
            errors: 0,
            properties: {}
          };
        }
  
        const configs = get(section, 'configs', []);
        configs.forEach((config: { propertyDisplayValue: any; errorMessages: any; confirmPassword: any; }) => {
          if (get(config, 'category', '') === categoryName && get(config, 'isVisible', false)) {
            const name = get(config, 'name', '');
            kerberosConfigProperties[serviceName][categoryName].properties[name] = {
              propertyName: name,
              propertyDisplayname: get(config, 'displayName', ''),
              propertyDescription: get(config, 'description', ''),
              propertyValue: get(config, 'value', ''),
              propertyAttributes: {
                type: "string",
                overridable: get(config, 'isOverridable', false),
              },
              previousValue: get(config, 'previousValue', ''),
              value: get(config, 'value', ''),
              final: get(config, 'final', 'false'),
              filename: get(config, 'filename', ''),
              ...(config.propertyDisplayValue && { propertyDisplayValue: config.propertyDisplayValue }),
              ...(config.errorMessages && { errorMessages: config.errorMessages }),
              ...(config.confirmPassword && { confirmPassword: config.confirmPassword }),
              isEditable:true
            };
          }
        });
      });
    });

    return kerberosConfigProperties;
  };
  

  function getStepConfigs(configs: any) {
    let configProperties = prepareConfigProperties(configs);

    configProperties = sortConfigs(configProperties);
    const newStepConfigs = createServiceConfig(configProperties);
    return newStepConfigs;
  }

  function prepareConfigProperties(configs: any) {
    const installedServiceNames = ["Cluster", "AMBARI"].concat(
      services.map((service: any) => service.ServiceInfo?.service_name)
    );
    let configPropertiesCopy = cloneDeep(configs);
    const siteProperties = stackConfigurations;

    configPropertiesCopy = configPropertiesCopy.filter(
      (item: { serviceName: string }) =>
        installedServiceNames.includes(item.serviceName)
    );

    const realm = configsStep2?.KERBEROS?.KDC?.properties?.realm;
    const realmValue = realm ? realm.value : null;

    if (realmValue !== null) {
      configPropertiesCopy = configPropertiesCopy.map((config: any) => {
        if (config.name === "realm") {
          return {
            ...config,
            value: realmValue,
            savedValue: realmValue,
            recommendedValue: realmValue,
          };
        }
        return config;
      });
    }

    configPropertiesCopy.forEach(
      (property: {
        isSecureConfig: boolean;
        name: string;
        observesValueFrom: any;
        value: any;
        recommendedValue: any;
        isVisible: boolean;
        category: string;
        serviceName: string;
        identityType: string;
        displayName: string | undefined;
        index: number;
        displayType: string;
      }) => {
        property.isSecureConfig = false;
        if (["spnego_keytab", "spnego_principal"].includes(property.name)) {
          // Add observer logic if needed, alternative ways in React might be different
        }
        if (property.observesValueFrom) {
          const observedConfig = configPropertiesCopy.find(
            (config: { name: any }) =>
              config.name === property.observesValueFrom
          );
          if (observedConfig) {
            property.value = observedConfig.value;
            property.recommendedValue = observedConfig.value;
            property.isVisible = true;
          }
        }
        property.category =
          property.serviceName === "Cluster" ? "Global" : property.serviceName;

        if (property.identityType === "user") {
          property.category = "Ambari Principals";
        }

        const siteProperty = siteProperties.find(
          (prop: any) => prop.name === property.name
        );
        if (siteProperty) {
          if (siteProperty.category === property.category) {
            property.displayName = siteProperty.displayName;
            if (siteProperty.index) {
              property.index = siteProperty.index;
            }
          }
          if (siteProperty.displayType) {
            property.displayType = siteProperty.displayType;
          }
        }
      }
    );

    return configPropertiesCopy;
  }

  function createServiceConfig(configs: any): StepConfig[] {
    const clusterConfigs = configs.filter(
      (config: { serviceName: string }) => config.serviceName === "Cluster"
    );

    // storm user principal is not required for ambari operation
    const userConfigs = configs.filter(
      (config: { identityType: string }) => config.identityType === "user"
    );

    const combinedConfigs = [...clusterConfigs, ...userConfigs];
    const configMap = new Map(
      combinedConfigs.map((config) => [config.name, config])
    );
    const generalConfigs = Array.from(configMap.values());

    const advancedConfigs = configs.filter(
      (element: { name: any }) => !generalConfigs.includes(element.name)
    );

    const categoryForGeneralConfigs = [
      { name: "Global", displayName: "Global" },
      { name: "Ambari Principals", displayName: "Ambari Principals" },
    ];

    const categoryForAdvancedConfigs = createCategoryForServices();

    return [
      {
        displayName: "General",
        name: "GENERAL",
        serviceName: "KERBEROS_GENERAL",
        configCategories: categoryForGeneralConfigs,
        configs: generalConfigs,
        configGroups: [],
        showConfig: true,
      },
      {
        displayName: "Advanced",
        name: "ADVANCED",
        serviceName: "KERBEROS_ADVANCED",
        configCategories: categoryForAdvancedConfigs,
        configs: advancedConfigs,
        configGroups: [],
        showConfig: true,
      },
    ];
  }

  function createCategoryForServices() {
    return services.map((item) => ({
      name: item.ServiceInfo?.service_name,
      displayName: item.ServiceInfo?.service_name,
      collapsedByDefault: true,
    }));
  }

  function sortConfigs(configs: any[]) {
    return configs.sort(
      (
        a: { index: number; name: number },
        b: { index: number; name: number }
      ) => {
        if (a.index > b.index) return 1;
        if (a.index < b.index) return -1;
        if (a.name > b.name) return 1;
        if (a.name < b.name) return -1;
        return 0;
      }
    );
  }

  getStepConfigsRef.current = getStepConfigs;
  transformDataRef.current = transformData;

  async function saveConfigurations() {
    const response = await KerberosApi.getKerberosDescriptorProperties("true", clusterName);
    const kerberosDescriptor = get(response, "KerberosDescriptor.kerberos_descriptor", {});
    const configs = collectDescriptorFormValues(result);
    const updatedKerberosDescriptor = removeDescriptorIdentityReferences(
      applyDescriptorValues(kerberosDescriptor, configs),
    );
    
    const payload = {
      "artifact_data":
      updatedKerberosDescriptor
    }
    await KerberosApi.createKerberosDescriptor(clusterName, payload);

    try {
      await RequestApi.preparingOperations(clusterName, {
        Clusters: { security_type: "NONE" },
      });
    } catch {
      // Classic advances after this cleanup request settles in either state.
    }
  }

  if(loading) {
    return <Spinner />
  }

  if (loadError || isEmpty(result)) {
    return (
      <Alert variant="danger">
        <div>
          {loadError || "The Kerberos descriptor did not contain editable identities."}
        </div>
        <Button className="mt-3" onClick={() => setLoadAttempt((value) => value + 1)}>
          Retry
        </Button>
      </Alert>
    );
  }

  return (
    <div>
        <div>
            <h5> Configure Identities</h5>
            <p>Configure principal name and keytab location for service users and hadoop service components.</p>
            {submitError && <Alert variant="danger">{submitError}</Alert>}
        </div>
        <div>
            <Tabs defaultActiveKey="general">
                <Tab eventKey="general" title="General">
                    <AdvancedConfigs
                        configPropertiesData={result}
                        setConfigProperties={setResult}
                        chosenService={"KERBEROS_GENERAL"}
                        setTabErrors={setTabErrors}
                        displayUndoRedo={false}
                        canEdit
                    />
                </Tab>
                <Tab eventKey="advanced" title="Advanced">
                    <AdvancedConfigs
                        configPropertiesData={result}
                        setConfigProperties={setResult}
                        chosenService={"KERBEROS_ADVANCED"}
                        setTabErrors  ={setTabErrors}
                        displayUndoRedo={false}
                        canEdit
                    />
                </Tab>
            </Tabs>
        </div>
        <Alert variant="success" className="mt-3">
          {translate("installer.step7.noIssues")}
        </Alert>
        <WizardFooter
          isNextEnabled={nextEnabled && !isSubmitting}
          step={currentStep}
          onNext={async () => {
            if(result) {
              dispatch({
                type: ActionTypes.STORE_INFORMATION,
                payload: { step: currentStep.name, data: {result}}
              })
            }
            setIsSubmitting(true);
            setSubmitError("");
            try {
              await saveConfigurations();
              flushStateToDb("next");
              handleNextImperitive();
            } catch (error) {
              setSubmitError(responseErrorMessage(
                error,
                "Ambari could not save the Kerberos descriptor. Correct the problem and retry.",
              ));
            } finally {
              setIsSubmitting(false);
            }
          }}
          onCancel={() => {
            onExitPopUp(false, false);
          }}
          onBack={() => {
            if (isManualKdcPlan(selectedKdcPlan)) {
              flushStateToDb("jump", 2);
              jumpToStep(2, true);
            } else {
              flushStateToDb("back")
              handleBackImperitive();
            }
          }}
        />
    </div>
  );
}

export default ConfigureIdentities;
