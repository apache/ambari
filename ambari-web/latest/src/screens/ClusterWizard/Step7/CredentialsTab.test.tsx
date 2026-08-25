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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useEffect, useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { ConfigPropertiesType } from "../../CommonConfigs/types";
import CredentialsTab, { processDataForCredentialsTab } from "./CredentialsTab";

const stackProperty = (
  propertyName: string,
  propertyValue: string,
  type: "string" | "password"
) => ({
  property_name: propertyName,
  property_value: propertyValue,
  property_display_name: propertyName,
  property_description: `${propertyName} description`,
  property_value_attributes: { type },
  service_name: "HIVE",
  type: "hive-env.xml",
});

const usernameStackProperty = stackProperty(
  "hive_database_user",
  "stack-user",
  "string"
);
const passwordStackProperty = stackProperty(
  "hive_database_password",
  "stack-password",
  "password"
);

const configs = {
  items: [
    {
      configurations: [
        {
          StackConfigurations: {
            ...usernameStackProperty,
            property_value: "other-service-user",
            service_name: "HDFS",
          },
        },
      ],
    },
    {
      configurations: [
        { StackConfigurations: usernameStackProperty },
        { StackConfigurations: passwordStackProperty },
      ],
    },
  ],
};

const themes = {
  items: [
    {
      StackServices: { service_name: "HIVE" },
      themes: [
        {
          ThemeInfo: {
            theme_data: {
              Theme: {
                name: "credentials",
                configuration: {
                  layouts: [
                    {
                      tabs: [
                        {
                          layout: {
                            sections: [
                              {
                                subsections: [
                                  {
                                    name: "hive-credentials",
                                    "display-name": "Hive Metastore",
                                  },
                                ],
                              },
                            ],
                          },
                        },
                      ],
                    },
                  ],
                  placement: {
                    configs: [
                      {
                        config: "hive-env/hive_database_user",
                        "subsection-name": "hive-credentials",
                      },
                      {
                        config: "hive-env/hive_database_password",
                        "subsection-name": "hive-credentials",
                      },
                    ],
                  },
                },
              },
            },
          },
        },
      ],
    },
  ],
};

const canonicalProperty = (
  propertyName: string,
  value: string,
  type: "string" | "password"
) => ({
  propertyName,
  propertyDisplayname: propertyName,
  propertyValue: value,
  propertyAttributes: { type },
  previousValue: value,
  value,
  confirmPassword: type === "password" ? value : undefined,
  final: "false",
  fileName: "hive-env.xml",
  type: "hive-env",
  serviceName: "HIVE",
  isEditable: true,
});

const initialConfigProperties = {
  HIVE: {
    "hive-env": {
      errors: 0,
      properties: {
        hive_database_user: canonicalProperty(
          "hive_database_user",
          "cluster-user",
          "string"
        ),
        hive_database_password: canonicalProperty(
          "hive_database_password",
          "cluster-password",
          "password"
        ),
      },
    },
  },
} as ConfigPropertiesType;

const getCanonicalProperty = (
  configProperties: ConfigPropertiesType,
  propertyName: string
) => configProperties.HIVE["hive-env"].properties[propertyName];

describe("Step 7 credentials", () => {
  it("maps exact Theme properties to canonical values without mutating Theme data", () => {
    const originalThemes = structuredClone(themes);
    const credentials = processDataForCredentialsTab(
      configs,
      themes,
      initialConfigProperties
    );

    expect(credentials).toHaveLength(1);
    const credential = credentials[0] as unknown as CredentialConfigFixture;
    expect(credential.usernameProperty.property_value).toBe("cluster-user");
    expect(credential.passwordProperty.property_value).toBe(
      "cluster-password"
    );
    expect(credential.confirmPasswordProperty.property_value).toBe(
      "cluster-password"
    );
    expect(themes).toEqual(originalThemes);
  });

  it("writes username, password, and confirmation edits back to Step 7 state", async () => {
    const setIsNextEnabled = vi.fn();
    let latestConfigProperties = initialConfigProperties;

    function Harness() {
      const [configProperties, setConfigProperties] =
        useState(initialConfigProperties);
      useEffect(() => {
        latestConfigProperties = configProperties;
      }, [configProperties]);
      return (
        <CredentialsTab
          configs={configs}
          themes={themes}
          configProperties={configProperties}
          setConfigProperties={setConfigProperties}
          setIsNextEnabled={setIsNextEnabled}
        />
      );
    }

    render(<Harness />);

    const username = await screen.findByRole("textbox", {
      name: "Hive Metastore Username",
    });
    const password = screen.getByLabelText("Hive Metastore Password");
    const confirmation = screen.getByLabelText(
      "Hive Metastore Confirm Password"
    );

    expect(username).toHaveProperty("value", "cluster-user");
    expect(password).toHaveProperty("value", "cluster-password");
    expect(confirmation).toHaveProperty("value", "cluster-password");

    fireEvent.change(username, { target: { value: "edited-user" } });
    await waitFor(() =>
      expect(
        getCanonicalProperty(
          latestConfigProperties,
          "hive_database_user"
        ).value
      ).toBe("edited-user")
    );

    fireEvent.change(password, { target: { value: "edited-password" } });
    await waitFor(() => {
      expect(
        getCanonicalProperty(
          latestConfigProperties,
          "hive_database_password"
        ).value
      ).toBe("edited-password");
      expect(setIsNextEnabled).toHaveBeenLastCalledWith(false);
    });

    fireEvent.change(confirmation, {
      target: { value: "edited-password" },
    });
    await waitFor(() => {
      const canonicalPassword = getCanonicalProperty(
        latestConfigProperties,
        "hive_database_password"
      );
      expect(canonicalPassword.value).toBe("edited-password");
      expect(canonicalPassword.confirmPassword).toBe("edited-password");
      expect(setIsNextEnabled).toHaveBeenLastCalledWith(true);
      expect(screen.getByLabelText("Hive Metastore Password")).toHaveProperty(
        "value",
        "edited-password"
      );
      expect(
        screen.getByLabelText("Hive Metastore Confirm Password")
      ).toHaveProperty("value", "edited-password");
    });
  });
});

type CredentialConfigFixture = {
  usernameProperty: { property_value: string };
  passwordProperty: { property_value: string };
  confirmPasswordProperty: { property_value: string };
};
