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

import { cloneDeep, get } from "lodash";
import {
  ChangeEvent,
  Dispatch,
  SetStateAction,
  useEffect,
  useState,
} from "react";
import { Card, Form } from "react-bootstrap";
import { ConfigPropertiesType } from "../../CommonConfigs/types";
import { CredentialConfigType } from "../types/step7Types";
import { isValidUserName } from "../utils";
import TooltipInput from "../../../components/TooltipInput";

interface CredentialsTabProps {
  themes: object;
  configs: object;
  configProperties: ConfigPropertiesType;
  setConfigProperties: Dispatch<SetStateAction<ConfigPropertiesType>>;
  setIsNextEnabled: (enabled: boolean) => void;
}
export default function CredentialsTab({
  themes,
  configs,
  configProperties,
  setConfigProperties,
  setIsNextEnabled,
}: CredentialsTabProps) {
  const [allCredentials, setAllCredentials] = useState<CredentialConfigType[]>(
    () => processDataForCredentialsTab(configs, themes, configProperties)
  );

  useEffect(() => {
    setAllCredentials(
      processDataForCredentialsTab(configs, themes, configProperties)
    );
  }, [themes, configs, configProperties]);

  const updateConfig = (
    propertyType: CredentialPropertyType,
    value: string,
    config: CredentialConfigType
  ) => {
    const newConfig = cloneDeep(config);
    newConfig[propertyType].property_value = value;
    setAllCredentials((credentials) =>
      credentials.map((credential) =>
        credential === config ? newConfig : credential
      )
    );

    const targetProperty = config[propertyType] as StackCredentialProperty;
    const targetField =
      propertyType === "confirmPasswordProperty" ? "confirmPassword" : "value";
    setConfigProperties((current) =>
      updateCredentialConfigProperty(
        current,
        targetProperty,
        targetField,
        value
      )
    );
  };

  const isValidPassword = (config: object) => {
    return (
      get(config, "passwordProperty.property_value", "").length > 0 &&
      get(config, "passwordProperty.property_value", "") ===
        get(config, "confirmPasswordProperty.property_value", "")
    );
  };

  useEffect(() => {
    const allCredentialsValid = allCredentials.every((config) => {
      return (
        (get(config, "usernameProperty")
          ? isValidUserName(get(config, "usernameProperty.property_value", ""))
          : true) && isValidPassword(config)
      );
    });
    setIsNextEnabled(allCredentialsValid);
  }, [allCredentials, setIsNextEnabled]);

  const getTooltipInput = (
    config: CredentialConfigType,
    propertyType: CredentialPropertyType
  ) => {
    const property_display_name = get(
      config,
      propertyType + ".property_display_name",
      ""
    );
    const property_name = get(config, propertyType + ".property_name", "");
    const property_description = get(
      config,
      propertyType + ".property_description",
      ""
    );
    const property_value = get(config, propertyType + ".property_value", "");
    const isPasswordProperty = propertyType.toLowerCase().includes("password");
    const isInputValid = isPasswordProperty
      ? isValidPassword(config)
      : isValidUserName(property_value);

    let tooltipHeading = "";
    if (property_display_name && property_name) {
      tooltipHeading = property_display_name + " " + property_name;
    } else {
      if (isPasswordProperty) {
        tooltipHeading = "password";
      } else {
        tooltipHeading = "username";
      }
    }

    let tooltipMessage = property_description;
    if (isPasswordProperty) {
      tooltipMessage +=
        " For security purposes, password changes will not be shown in configuration version comparisons";
    }

    const formControlProps = {
      type: isPasswordProperty ? "password" : "text",
      value: property_value,
      onChange: (e: ChangeEvent<HTMLInputElement>) =>
        updateConfig(propertyType, e.target.value, config),
      className: isInputValid ? "" : "border-danger",
      placeholder: propertyType === "passwordProperty" ? "Type password" : "",
      "aria-label": `${get(config, "display-name", "Credentials")} ${
        propertyType === "usernameProperty"
          ? "Username"
          : propertyType === "passwordProperty"
            ? "Password"
            : "Confirm Password"
      }`,
    };

    return (
      <TooltipInput
        tooltipProps={{
          message: tooltipMessage,
          heading: tooltipHeading,
          placement: "left",
        }}
        formControlProps={formControlProps as any}
      />
    );
  };

  return (
    <div>
      <Card>
        <Card.Body>
          <div className="mb-4 text-muted">
            Please provide credentials for these services
          </div>
          <div className="d-flex text-muted">
            <div className="w-25 ps-3"></div>
            <div className="w-25 ps-3">Username*</div>
            <div className="w-25 ps-3">Password*</div>
            <div className="w-25 ps-3">Confirm Password*</div>
          </div>
          <hr className="mb-2" />
          <Form>
            {allCredentials.map((config) => {
              return (
                <div key={get(config, "name", "")}>
                  <div className="d-flex text-muted">
                    <div className="w-25 pt-3 ps-2">
                      {get(config, "display-name", "")}
                    </div>
                    <Form.Group className="w-25 p-2">
                      {get(config, "usernameProperty") ? (
                        getTooltipInput(config, "usernameProperty")
                      ) : (
                        <div className="pt-2 ps-2">N/A</div>
                      )}
                    </Form.Group>
                    <Form.Group className="w-25 p-2">
                      {getTooltipInput(config, "passwordProperty")}
                    </Form.Group>
                    <Form.Group className="w-25 p-2">
                      {getTooltipInput(config, "confirmPasswordProperty")}
                    </Form.Group>
                  </div>
                  <hr className="m-1" />
                </div>
              );
            })}
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
}

export const processDataForCredentialsTab = (
  configsData: object,
  themesData: object,
  configProperties: ConfigPropertiesType = {}
) => {
  const allConfigs: any[] = [];
  get(configsData, "items", []).forEach((item) => {
    get(item, "configurations", []).forEach((configuration) => {
      allConfigs.push(get(configuration, "StackConfigurations", {}));
    });
  });

  let tempCredentials: CredentialConfigType[] = [];
  get(themesData, "items", []).forEach((item) => {
    const themeServiceName = get(item, "StackServices.service_name", "");
    get(item, "themes", [])
      .filter(
        (theme) =>
          get(theme, "ThemeInfo.theme_data.Theme.name") === "credentials"
      )
      .forEach((theme) => {
        const currCredLayout: CredentialConfigType[] = [];

        get(
          theme,
          "ThemeInfo.theme_data.Theme.configuration.layouts",
          []
        ).forEach((layout) => {
          get(layout, "tabs", []).forEach((tab) => {
            get(tab, "layout.sections", []).forEach((section) => {
              get(section, "subsections", []).forEach((subsection: any) => {
                if (subsection) {
                  currCredLayout.push(cloneDeep(subsection));
                }
              });
            });
          });
        });

        if (currCredLayout.length) {
          get(
            theme,
            "ThemeInfo.theme_data.Theme.configuration.placement.configs",
            []
          ).forEach((config) => {
            const [propertyType, propertyName] = get(
              config,
              "config",
              ""
            ).split("/");
            const configMatchingProperty = allConfigs.find(
              (c) =>
                normalizeConfigType(get(c, "type", "")) ===
                  normalizeConfigType(propertyType) &&
                get(c, "property_name", "") === propertyName &&
                (!themeServiceName ||
                  get(c, "service_name", "") === themeServiceName)
            );
            if (configMatchingProperty) {
              const canonicalProperty = findCanonicalConfigProperty(
                configProperties,
                configMatchingProperty
              );
              const credentialProperty = {
                ...cloneDeep(configMatchingProperty),
                property_value:
                  canonicalProperty?.value ??
                  configMatchingProperty.property_value ??
                  "",
              };
              currCredLayout.forEach((section) => {
                if (
                  get(section, "name", "") ===
                  get(config, "subsection-name", "")
                ) {
                  if (
                    get(
                      credentialProperty,
                      "property_value_attributes.type",
                      ""
                    ) === "password"
                  ) {
                    section.passwordProperty = credentialProperty;
                    section.confirmPasswordProperty = {
                      ...cloneDeep(credentialProperty),
                      property_value:
                        canonicalProperty?.confirmPassword ??
                        credentialProperty.property_value,
                    };
                  } else {
                    section.usernameProperty = credentialProperty;
                  }
                }
              });
            }
          });
        }

        tempCredentials = [...tempCredentials, ...currCredLayout];
      });
  });
  return tempCredentials;
};

type CredentialPropertyType =
  | "usernameProperty"
  | "passwordProperty"
  | "confirmPasswordProperty";

type CredentialStateField = "value" | "confirmPassword";

type StackCredentialProperty = {
  property_name?: string;
  service_name?: string;
  type?: string;
  [key: string]: unknown;
};

const normalizeConfigType = (configType: string) =>
  configType.endsWith(".xml") ? configType.slice(0, -4) : configType;

const findCanonicalConfigProperty = (
  configProperties: ConfigPropertiesType,
  credentialProperty: StackCredentialProperty
) => {
  const serviceName = get(credentialProperty, "service_name", "");
  const propertyName = get(credentialProperty, "property_name", "");
  const configType = normalizeConfigType(get(credentialProperty, "type", ""));

  for (const service of Object.values(configProperties)) {
    for (const category of Object.values(service)) {
      for (const property of Object.values(category.properties || {})) {
        if (
          property.propertyName === propertyName &&
          property.serviceName === serviceName &&
          normalizeConfigType(property.type || property.fileName || "") ===
            configType
        ) {
          return property;
        }
      }
    }
  }
  return undefined;
};

const updateCredentialConfigProperty = (
  configProperties: ConfigPropertiesType,
  credentialProperty: StackCredentialProperty,
  field: CredentialStateField,
  value: string
) => {
  const updatedConfigProperties = cloneDeep(configProperties);
  const property = findCanonicalConfigProperty(
    updatedConfigProperties,
    credentialProperty
  );
  if (property) {
    property[field] = value;
  }
  return updatedConfigProperties;
};
