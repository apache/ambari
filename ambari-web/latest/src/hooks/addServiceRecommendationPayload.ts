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

import { cloneDeep } from "lodash";
import { buildConfigsJSON } from "../screens/CommonConfigs/ConfigUtils";
import { ConfigPropertiesType } from "../screens/CommonConfigs/types";

type AddServiceRecommendationInput = {
  clusterId: number | string | null | undefined;
  configProperties: ConfigPropertiesType;
  hosts: string[];
  installedServices: string[];
  recommendations: Record<string, unknown>;
  selectedServices: string[];
};

const asRecord = (value: unknown): Record<string, unknown> =>
  value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};

export const buildAddServiceRecommendationPayload = ({
  clusterId,
  configProperties,
  hosts,
  installedServices,
  recommendations: recommendationInput,
  selectedServices,
}: AddServiceRecommendationInput) => {
  const recommendations = cloneDeep(recommendationInput);
  const blueprint = asRecord(recommendations.blueprint);
  blueprint.configurations = buildConfigsJSON(configProperties);
  recommendations.blueprint = blueprint;

  return {
    recommend: "configurations",
    hosts,
    services: [
      ...new Set([...installedServices, ...selectedServices, "MISC"]),
    ],
    user_context: {
      operation: "AddService",
      operation_details: selectedServices.join(","),
    },
    recommendations,
    clusterId: clusterId ?? null,
    autoComplete: "false",
    configsResponse: "false",
  };
};
