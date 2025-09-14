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

import { cloneDeep, get, set } from "lodash";
import { useEffect, useState } from "react";
import {Card, Form } from "react-bootstrap";
import { CredentialConfigType } from "../types/step7Types";
import { isValidUserName } from "../utils";
import TooltipInput from "../../../components/TooltipInput";

interface CredentialsTabProps {
  themes: Object;
  configs: Object;
  setIsNextEnabled:Function;
}
export default function CredentialsTab({
  themes,
  configs,
  setIsNextEnabled
}: CredentialsTabProps) {
  const [allCredentials, setAllCredentials] = useState<CredentialConfigType[]>(
    []
  );

  useEffect(() => {
    setAllCredentials(processDataForCredentialsTab(configs, themes));
  }, [themes, configs]);

  const updateConfig = (key: string, value: string, config: any) => {
    let newConfig = { ...config };
    set(newConfig, key, value);
    setAllCredentials(
      allCredentials.map((c) => (c === config ? newConfig : c))
    );
  };

  const isValidPassword = (config: Object) => {
    return (
      get(config, "passwordProperty.property_value", "").length > 0 &&
      get(config, "passwordProperty.property_value", "") ===
        get(config, "confirmPasswordProperty.property_value", "")
    );
  };

  useEffect(()=>{
    const allCredentialsValid=allCredentials.every((config) => {
      return (
        (get(config, "usernameProperty")
          ? isValidUserName(get(config, "usernameProperty.property_value", ""))
          : true) && isValidPassword(config)
      );
    });
    console.log("All Credentials Valid",allCredentialsValid)
    if(allCredentialsValid){
      setIsNextEnabled(true);
    }
    else{
      setIsNextEnabled(false);
    }
  },[allCredentials])


  const getTooltipInput = (config: any, propertyType: string) => {
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
      onChange: (e: any) =>
        updateConfig(propertyType + ".property_value", e.target.value, config),
      className: isInputValid ? "" : "border-danger",
      placeholder: propertyType === "passwordProperty" ? "Type password" : "",
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
  configsData: Object,
  themesData: Object
) => {
  let allConfigs: any[] = [];
  get(configsData, "items", []).forEach((item) => {
    get(item, "configurations", []).forEach((configuration) => {
      allConfigs.push(get(configuration, "StackConfigurations", {}));
    });
  });

  let tempCredentials: CredentialConfigType[] = [];
  get(themesData, "items", []).forEach((item) => {
    get(item, "themes", [])
      .filter(
        (theme) =>
          get(theme, "ThemeInfo.theme_data.Theme.name") === "credentials"
      )
      .forEach((theme) => {
        let currCredLayout: CredentialConfigType[] = [];

        get(
          theme,
          "ThemeInfo.theme_data.Theme.configuration.layouts",
          []
        ).forEach((layout) => {
          get(layout, "tabs", []).forEach((tab) => {
            get(tab, "layout.sections", []).forEach((section) => {
              get(section, "subsections", []).forEach((subsection: any) => {
                if (subsection) {
                  currCredLayout.push(subsection);
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
                get(c, "type", "").startsWith(propertyType) &&
                get(c, "property_name", "") === propertyName
            );
            if (configMatchingProperty) {
              currCredLayout.forEach((section) => {
                if (
                  get(section, "name", "") ===
                  get(config, "subsection-name", "")
                ) {
                  if (
                    get(
                      configMatchingProperty,
                      "property_value_attributes.type",
                      ""
                    ) === "password"
                  ) {
                    set(section, "passwordProperty", configMatchingProperty);
                    set(
                      section,
                      "confirmPasswordProperty",
                      cloneDeep(configMatchingProperty)
                    );
                  } else {
                    set(section, "usernameProperty", configMatchingProperty);
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
