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

import { describe, expect, it } from "vitest";
import { ConfigPropertiesType } from "../screens/CommonConfigs/types";
import { buildAddServiceRecommendationPayload } from "./addServiceRecommendationPayload";

const configs: ConfigPropertiesType = {
  HIVE: {
    "hive-site": {
      errors: 0,
      properties: {
        canonical: {
          propertyName: "hive.metastore.uris",
          propertyDisplayname: "Metastore URIs",
          propertyValue: "stack-default",
          propertyAttributes: { type: "string" },
          previousValue: "stack-default",
          value: "thrift://edited:9083",
          confirmPassword: "must-not-leak",
          final: "false",
          type: "hive-site",
          serviceName: "HIVE",
          isEditable: true,
        },
        actionOnly: {
          propertyName: "test_db_connection",
          propertyDisplayname: "Test connection",
          propertyValue: "",
          propertyAttributes: { type: "button" },
          previousValue: "",
          value: "clicked",
          final: "false",
          type: "hive-site",
          serviceName: "HIVE",
          isEditable: true,
          isRequiredByAgent: false,
        },
      },
    },
  },
};

describe("Add Service recommendation payload", () => {
  it("uses canonical edited values without reintroducing stack defaults or UI-only state", () => {
    const recommendationInput = {
      blueprint: { host_groups: [{ name: "host_group_0" }] },
    };
    const result = buildAddServiceRecommendationPayload({
      clusterId: 7,
      configProperties: configs,
      hosts: ["host1"],
      installedServices: ["HDFS"],
      recommendations: recommendationInput,
      selectedServices: ["HIVE"],
    });

    expect(result.services).toEqual(["HDFS", "HIVE", "MISC"]);
    expect(result.user_context).toEqual({
      operation: "AddService",
      operation_details: "HIVE",
    });
    expect(result.recommendations.blueprint).toMatchObject({
      configurations: {
        "hive-site": {
          properties: { "hive.metastore.uris": "thrift://edited:9083" },
        },
      },
    });
    expect(JSON.stringify(result)).not.toContain("test_db_connection");
    expect(JSON.stringify(result)).not.toContain("confirmPassword");
    expect(recommendationInput).toEqual({
      blueprint: { host_groups: [{ name: "host_group_0" }] },
    });
  });
});
