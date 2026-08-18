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
import { ConfigPropertiesType, InputType } from "../CommonConfigs/types";
import WizardApi from "../../api/wizardApi";
import { isEmpty } from "lodash";
import { Alert, Button, Card, Nav, Spinner } from "react-bootstrap";
import AdvancedConfigs from "../CommonConfigs/AdvancedConfigs";
import { kerberos_properties } from "../../data/configs/services/kerberos_properties";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "./KerberosStore/context";
import { get } from "lodash";
import { ActionTypes } from "./KerberosStore/types";
import { kerberos_ui_properties } from "../../data/configs/kerberos_ui_properties";
import KerberosApi from "../../api/kerberosApi";
import { AppContext } from "../../store/context";
import ClusterDeploymentApi from "../../api/clusterDeployment";
import {
  getConfigCategories,
  getTotalErros,
  validateAllProperties,
} from "../CommonConfigs/ConfigUtils";
import credentialsUtils from "../../Utils/credentialsUtils";
import {
  applyKdcPlanVisibility,
  buildKerberosConfigurationPayload,
  isManualKdcPlan,
  runKerberosConfiguration,
} from "../../Utils/kerberosWizard";
import { responseErrorMessage } from "../../Utils/httpError";

export default function ConfigureKerberos() {
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

  const services = ["KERBEROS"];
  const [configs, setConfigs] = useState<any>({});
  const [configProperties, setConfigProperties] =
    useState<ConfigPropertiesType>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [tabErrors, setTabErrors] = useState({});
  const [nextEnabled, setNextEnabled] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { clusterName, allHostNames, cluster } = useContext(AppContext);
  const stackName = get(cluster, "version", "").split("-")[0];
  const stackVersion = get(cluster, "version", "").split("-")[1];

  const serviceName = "KERBEROS";

  const kdcType = get(
    state,
    `kerberosWizardSteps.${wizardSteps[1].name}.data.selectedKdcPlan`,
    ""
  );

  useEffect(() => {
    const noErros = getTotalErros(tabErrors);
    setNextEnabled(noErros);
  }, [tabErrors]);

  useEffect(() => {
    getConfigurations();
    const configPropertiesFromState = get(
      state,
      `kerberosWizardSteps.${wizardSteps[2].name}.data.configProperties`,
      null
    );
    if (configPropertiesFromState) {
      setConfigProperties(configPropertiesFromState);
    }
  }, []);

  useEffect(() => {
    if (!isEmpty(configs) && isEmpty(configProperties)) {
      getConfigProperties();
    }
  }, [configs]);

  const trimKerberosPropertyValue = (
    value: any,
    displayType: string
  ) => {

    // return value;
    if (typeof value !== "string") {
      return value;
    }

    switch (displayType) {
      case InputType.CONTENT:
      case InputType.MULTILINE:
        // For multiline content, trim trailing spaces from each line
        return value
          .split("\n")
          .map((line) => line.replace(/\s+$/, ""))
          .join("\n");

      case InputType.DIRECTORIES:
      case InputType.DIRECTORY:
        return value.replace(/,/g, " ").trim().split(/\s+/g).join(",");

      case InputType.HOST:
        return value.trim();

      case InputType.PASSWORD:
        return value;

      default:
        // For all other types, just remove trailing spaces
        return value.replace(/\s+$/, "");
    }
  };

  const getConfigurations = async () => {
    setLoading(true);
    setLoadError("");
    try {
      const response = await WizardApi.getStackConfigurations(
        stackName,
        stackVersion,
        services.join(","),
        "configurations/*,configurations/dependencies/*,StackServices/config_types/*"
      );
      setConfigs(response);
    } catch (error) {
      setLoadError(responseErrorMessage(
        error,
        "Ambari could not load the Kerberos configuration definitions.",
      ));
    } finally {
      setLoading(false);
    }
  };

  const getConfigProperties = () => {
    let configPropertiesCopy: ConfigPropertiesType = {};
    configs?.items?.forEach((service: any) => {
      service.configurations?.forEach((config: any) => {
        const fileName = config.StackConfigurations.type as string;
        const configType = fileName.split(".").slice(0, -1).join(".");
        const propertyName = config.StackConfigurations.property_name as string;
        const serviceName = config.StackConfigurations.service_name;

        if (!configPropertiesCopy[serviceName]) {
          configPropertiesCopy[serviceName] = {};
        }
        if (!configPropertiesCopy[serviceName][configType]) {
          configPropertiesCopy[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        // Set default value for preconfigure_services if it's null or empty
        let propertyValue = config.StackConfigurations.property_value;
        if (
          propertyName === "preconfigure_services" &&
          (propertyValue === null ||
            propertyValue === undefined ||
            propertyValue === "")
        ) {
          propertyValue = "DEFAULT";
        }

        configPropertiesCopy[serviceName][configType].properties[propertyName] =
          {
            propertyName: propertyName,
            ...(config.StackConfigurations.property_display_name && {
              propertyDisplayname:
                config.StackConfigurations.property_display_name,
            }),
            propertyDescription:
              config.StackConfigurations.property_description,
            propertyValue: propertyValue,
            propertyAttributes:
              config.StackConfigurations.property_value_attributes,
            previousValue: propertyValue,
            // value: propertyValue,

            // Fixed (correct arguments):
            // value: formatPropertyValue(
            //   configPropertiesCopy[serviceName][configType].properties[
            //     propertyName
            //   ],
            //   propertyValue
            // ),
            // Instead of calling formatPropertyValue, use our safe function:
            value: trimKerberosPropertyValue(
              propertyValue,
              config.StackConfigurations.property_value_attributes?.type
            ),

            final: config.StackConfigurations.final
              ? config.StackConfigurations.final
              : "",
            isEditable: true,
            type: configType,
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
      });
    });

    let KerberosConfigProperties: ConfigPropertiesType = {};

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!KerberosConfigProperties[serviceName]) {
        KerberosConfigProperties[serviceName] = {};
      }

      const serviceConfigCategories = getConfigCategories(serviceName);
      serviceConfigCategories.forEach((category) => {
        if (!KerberosConfigProperties[serviceName][category.name]) {
          KerberosConfigProperties[serviceName][category.name] = {
            errors: 0,
            properties: {},
            displayName: category.displayName,
          };
        }
      });
    });

    kerberos_properties.forEach((property) => {
      const { serviceName, filename, name, category } = property;
      const configType = filename.split(".")[0];

      if (!category.includes("Advanced")) {
        if (!KerberosConfigProperties[serviceName]) {
          KerberosConfigProperties[serviceName] = {};
        }
        if (!KerberosConfigProperties[serviceName][category]) {
          KerberosConfigProperties[serviceName][category] = {
            errors: 0,
            properties: {},
          };
        }

        KerberosConfigProperties[serviceName][category].properties[name] =
          configPropertiesCopy[serviceName][configType].properties[name];

        delete configPropertiesCopy[serviceName][configType].properties[name];
      }
    });

    kerberos_ui_properties.forEach((property) => {
      const {
        serviceName,
        name,
        category,
        displayName,
        displayType,
        description,
        recommendedValue,
      } = property;

      if (!KerberosConfigProperties[serviceName]) {
        KerberosConfigProperties[serviceName] = {};
      }
      if (!KerberosConfigProperties[serviceName][category]) {
        KerberosConfigProperties[serviceName][category] = {
          errors: 0,
          properties: {},
        };
      }

      const defaultValue =
        recommendedValue !== undefined ? String(recommendedValue) : "";

      KerberosConfigProperties[serviceName][category].properties[name] = {
        propertyName: name,
        propertyDisplayname: displayName,
        propertyDescription: description || "",
        propertyValue: defaultValue,
        propertyAttributes: {
          type: displayType || "string",
          overridable: false,
        },
        previousValue: defaultValue,
        value: defaultValue,
        final: "false",
        isEditable: true,
      };
    });

    KerberosConfigProperties[serviceName]["KDC"].properties["kdc_type"].value =
      kdcType;

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!KerberosConfigProperties[serviceName]) {
        KerberosConfigProperties[serviceName] = {};
      }
      Object.keys(configPropertiesCopy[serviceName]).forEach((configType) => {
        if (!KerberosConfigProperties[serviceName]["Advanced " + configType]) {
          KerberosConfigProperties[serviceName]["Advanced " + configType] = {
            errors: 0,
            properties: {},
          };
        }
        Object.keys(
          configPropertiesCopy[serviceName][configType].properties
        ).forEach((propertyName) => {
          const property =
            configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ];

          // Special handling for preconfigure_services to set default value and ensure it's a text field
          if (propertyName === "preconfigure_services") {
            if (
              !property.value ||
              property.value === "" ||
              property.value === null
            ) {
              property.value = "DEFAULT";
              property.propertyValue = "DEFAULT";
              property.previousValue = "DEFAULT";
            }
            // Override server-side attributes to ensure it's a text field, not dropdown
            property.propertyAttributes = {
              ...property.propertyAttributes,
              type: "string",
            };
            // Remove any dropdown-specific attributes
            delete property.propertyAttributes.entries;
            delete property.propertyAttributes.selection_cardinality;
          }

          KerberosConfigProperties[serviceName][
            "Advanced " + configType
          ].properties[propertyName] = property;
        });
      });
    });

    KerberosConfigProperties[serviceName]["KDC"].properties[
      "Test.KDC.Connection"
    ] = {
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

    // Ensure preconfigure_services has the correct default value
    if (
      KerberosConfigProperties[serviceName]?.["Advanced kerberos-env"]
        ?.properties?.["preconfigure_services"]
    ) {
      const preconfigProperty =
        KerberosConfigProperties[serviceName]["Advanced kerberos-env"]
          .properties["preconfigure_services"];
      if (
        !preconfigProperty.value ||
        preconfigProperty.value === "" ||
        preconfigProperty.value === null
      ) {
        preconfigProperty.value = "DEFAULT";
        preconfigProperty.propertyValue = "DEFAULT";
        preconfigProperty.previousValue = "DEFAULT";
      }
    }

    KerberosConfigProperties = applyKdcPlanVisibility(
      KerberosConfigProperties,
      kdcType,
    );
    KerberosConfigProperties = validateAllProperties(KerberosConfigProperties);

    setConfigProperties(KerberosConfigProperties);
  };

  const createConfigurations = async () => {
    return await KerberosApi.createKerberosConfigurations(
      clusterName,
      buildKerberosConfigurationPayload(configProperties, kdcType),
    );
  };

  const getKdcCredentialsType = async () => {
    const storage = await credentialsUtils.storageInfo(clusterName);
    const persistCredentials =
      configProperties[serviceName]?.["Kadmin"]?.properties?.[
        "persist_credentials"
      ]?.value;
    return storage?.[credentialsUtils.STORE_TYPES.PERSISTENT_KEY]
      && (persistCredentials === "true" || persistCredentials === true)
      ? credentialsUtils.STORE_TYPES.PERSISTENT
      : credentialsUtils.STORE_TYPES.TEMPORARY;
  };

  const createKerberosAdminSession = async () => {
    const adminPrincipalValue =
      configProperties[serviceName]["Kadmin"].properties["admin_principal"]
        .value;
    const adminPasswordValue =
      configProperties[serviceName]["Kadmin"].properties["admin_password"]
        .value;

    if (isManualKdcPlan(kdcType)) {
      return await KerberosApi.createAdminSession(clusterName, [{
        session_attributes: {
          kerberos_admin: {
            principal: adminPrincipalValue,
            password: adminPasswordValue,
          },
        },
      }]);
    }
    const kdcCredentialsType = await getKdcCredentialsType();
    const resource = credentialsUtils.createCredentialResource(
      adminPrincipalValue,
      adminPasswordValue,
      kdcCredentialsType as string,
    );
    return await credentialsUtils.createOrUpdateCredentials(
      clusterName,
      credentialsUtils.ALIAS.KDC_CREDENTIALS,
      resource as any,
    );
  };

  const deleteKerberosService = async () => {
    try {
      return await KerberosApi.deleteKerberosService(
        clusterName,
        serviceName
      );
    } catch (error: any) {
      if ((error?.response?.status ?? error?.status) !== 404) {
        throw error;
      }
    }
  };

  const createKerberosService = async () => {
    const payloadData = { ServiceInfo: { service_name: "KERBEROS" } };
    return await ClusterDeploymentApi.createSelectedServices(
      clusterName,
      payloadData
    );
  };

  const createServiceComponent = async (componentName: string) => {
    const payloadData = {
      components: [
        {
          ServiceComponentInfo: {
            component_name: componentName,
          },
        },
      ],
    };
    return await ClusterDeploymentApi.addRequestToCreateComponent(
      clusterName,
      serviceName,
      payloadData
    );
  };

  const createKerberosHostComponents = async () => {
    let queryStr = "";
    allHostNames.forEach((hostName: string) => {
      queryStr += "Hosts/host_name=" + hostName + "|";
    });

    queryStr = queryStr.slice(0, -1);

    const payloadData = {
      RequestInfo: {
        query: queryStr,
      },
      Body: {
        host_components: [
          {
            HostRoles: {
              component_name: "KERBEROS_CLIENT",
            },
          },
        ],
      },
    };

    return await ClusterDeploymentApi.registerHostToCluster(
      clusterName,
      payloadData
    );
  };

  const createKerberosResources = async () => {
    await createKerberosService();
    await createServiceComponent("KERBEROS_CLIENT");
    await createKerberosHostComponents();
  };

  const onSubmitConfigureKerberos = async () => {
    await runKerberosConfiguration(kdcType, {
      deleteKerberosService,
      createKerberosResources,
      createConfigurations,
      createKerberosAdminSession,
    });
  };

  if (loading) {
    return <Spinner />;
  }

  if (loadError || isEmpty(configProperties)) {
    return (
      <Alert variant="danger">
        <div>{loadError || "No Kerberos configuration definitions were returned."}</div>
        <Button className="mt-3" variant="outline-danger" onClick={getConfigurations}>
          Retry
        </Button>
      </Alert>
    );
  }

  return (
    <>
      <div className="p-3">
        <div className="p-2">
          <h4>Configure Kerberos</h4>
          <p>Please configure kerberos related properties.</p>
          {submitError && <Alert variant="danger">{submitError}</Alert>}

          <Card>
            <Nav>
              <Nav.Link>{serviceName}</Nav.Link>
            </Nav>
            {/* TODO: to have validations of leading whitespace */}
            <AdvancedConfigs
              chosenService={serviceName}
              setConfigProperties={setConfigProperties}
              configPropertiesData={configProperties}
              displayUndoRedo={false}
              setTabErrors={setTabErrors}
              canEdit
            />
          </Card>
        </div>
      </div>
      <WizardFooter
        isNextEnabled={nextEnabled && !isSubmitting}
        step={currentStep}
        onNext={async () => {
          if (configProperties) {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: { step: currentStep.name, data: { configProperties } },
            });
          }
          setIsSubmitting(true);
          setSubmitError("");
          try {
            await onSubmitConfigureKerberos();
            if (isManualKdcPlan(kdcType)) {
              flushStateToDb("jump", 4);
              jumpToStep(4, true);
            } else {
              flushStateToDb("next");
              handleNextImperitive();
            }
          } catch (error) {
            setSubmitError(responseErrorMessage(
              error,
              "Ambari could not save the Kerberos configuration. Correct the problem and retry.",
            ));
          } finally {
            setIsSubmitting(false);
          }
        }}
        onCancel={() => {
          onExitPopUp(false, false);
        }}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}
