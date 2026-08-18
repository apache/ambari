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
import {
  buildAlertCreationPayload,
  INITIAL_ALERT_CREATION_FORM,
  validateAlertCreation,
  type AlertCreationForm,
  type CreatableAlertType,
} from "./alertCreation";

const form = (type: CreatableAlertType): AlertCreationForm => ({
  ...INITIAL_ALERT_CREATION_FORM,
  type,
  label: "Custom Health Check",
  description: "Created in React",
  serviceName: "HDFS",
  componentName: "NAMENODE",
  scope: "ANY",
  interval: "2",
  uri: "{{hdfs-site/dfs.namenode.http-address}}",
  defaultPort: "9870",
  connectionTimeout: "5",
  scriptPath: "BIGTOP/3.2.0/services/HDFS/package/alerts/custom.py",
  aggregateAlertName: "namenode_process",
  warningThreshold: "10",
  criticalThreshold: "20",
  okText: "OK",
  warningText: "Warning",
  criticalText: "Critical",
});

describe("alert creation contracts", () => {
  it.each(["PORT", "WEB", "SCRIPT", "AGGREGATE"] as CreatableAlertType[])("validates and builds %s", (type) => {
    const value = form(type);
    expect(validateAlertCreation(value)).toEqual([]);
    const payload = buildAlertCreationPayload(value);
    expect(payload).toMatchObject({
      "AlertDefinition/name": "custom_health_check",
      "AlertDefinition/label": "Custom Health Check",
      "AlertDefinition/service_name": "HDFS",
      "AlertDefinition/component_name": "NAMENODE",
      "AlertDefinition/source": { type },
    });
  });

  it("uses Classic-compatible Port source fields", () => {
    expect(buildAlertCreationPayload(form("PORT"))["AlertDefinition/source"]).toEqual({
      type: "PORT",
      uri: "{{hdfs-site/dfs.namenode.http-address}}",
      default_port: 9870,
      reporting: {
        ok: { text: "OK" },
        warning: { text: "Warning", value: 10 },
        critical: { text: "Critical", value: 20 },
      },
    });
  });

  it("repairs Script and Aggregate request paths beyond broken Classic wizard data", () => {
    expect(buildAlertCreationPayload(form("SCRIPT"))["AlertDefinition/source"]).toMatchObject({
      path: "BIGTOP/3.2.0/services/HDFS/package/alerts/custom.py",
      parameters: [],
    });
    expect(buildAlertCreationPayload(form("AGGREGATE"))["AlertDefinition/source"]).toMatchObject({
      alert_name: "namenode_process",
      reporting: { units: "%", type: "PERCENT" },
    });
  });

  it("rejects missing type-specific values and reversed thresholds", () => {
    const value = form("PORT");
    value.uri = "";
    value.warningThreshold = "30";
    value.criticalThreshold = "20";
    expect(validateAlertCreation(value)).toContain("URI is required.");
    expect(validateAlertCreation(value)).toContain("Warning Threshold cannot be greater than Critical Threshold.");
  });
});
