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

export type CreatableAlertType = "PORT" | "WEB" | "SCRIPT" | "AGGREGATE";

export interface AlertCreationForm {
  type: CreatableAlertType | "";
  label: string;
  description: string;
  serviceName: string;
  componentName: string;
  scope: "ANY" | "HOST" | "SERVICE";
  interval: string;
  uri: string;
  defaultPort: string;
  connectionTimeout: string;
  scriptPath: string;
  aggregateAlertName: string;
  warningThreshold: string;
  criticalThreshold: string;
  okText: string;
  warningText: string;
  criticalText: string;
}

export const INITIAL_ALERT_CREATION_FORM: AlertCreationForm = {
  type: "",
  label: "",
  description: "",
  serviceName: "",
  componentName: "",
  scope: "ANY",
  interval: "1",
  uri: "",
  defaultPort: "",
  connectionTimeout: "5",
  scriptPath: "",
  aggregateAlertName: "",
  warningThreshold: "",
  criticalThreshold: "",
  okText: "",
  warningText: "",
  criticalText: "",
};

export function alertDefinitionName(label: string): string {
  return label.trim().toLowerCase().replace(/\s+/g, "_");
}

export function validateAlertCreation(form: AlertCreationForm): string[] {
  const errors: string[] = [];
  if (!form.type) errors.push("Select an Alert Type.");
  if (!form.label.trim()) errors.push("Alert Name is required.");
  if (!form.serviceName) errors.push("Service is required.");
  if (!Number.isInteger(Number(form.interval)) || Number(form.interval) < 1) errors.push("Check Interval must be a positive integer.");

  if (form.type === "PORT" || form.type === "WEB") {
    if (!form.uri.trim()) errors.push("URI is required.");
    if (!/^\d+$/.test(form.defaultPort) || Number(form.defaultPort) < 1 || Number(form.defaultPort) > 65535) {
      errors.push("Default Port must be between 1 and 65535.");
    }
  }
  if (form.type === "WEB" && (!Number.isFinite(Number(form.connectionTimeout)) || Number(form.connectionTimeout) <= 0)) {
    errors.push("Connection Timeout must be a positive number.");
  }
  if (form.type === "SCRIPT" && !form.scriptPath.trim()) errors.push("Script Path is required.");
  if (form.type === "AGGREGATE" && !form.aggregateAlertName.trim()) errors.push("Referenced Alert Name is required.");

  if (form.type === "PORT" || form.type === "AGGREGATE") {
    const warning = Number(form.warningThreshold);
    const critical = Number(form.criticalThreshold);
    if (!Number.isFinite(warning) || warning <= 0) errors.push("Warning Threshold must be positive.");
    if (!Number.isFinite(critical) || critical <= 0) errors.push("Critical Threshold must be positive.");
    if (Number.isFinite(warning) && Number.isFinite(critical) && warning > critical) {
      errors.push("Warning Threshold cannot be greater than Critical Threshold.");
    }
  }
  return errors;
}

function reporting(form: AlertCreationForm, includeValues: boolean) {
  const report: Record<string, unknown> = {
    ok: { text: form.okText },
    warning: includeValues
      ? { text: form.warningText, value: Number(form.warningThreshold) }
      : { text: form.warningText },
    critical: includeValues
      ? { text: form.criticalText, value: Number(form.criticalThreshold) }
      : { text: form.criticalText },
  };
  if (form.type === "AGGREGATE") {
    report.units = "%";
    report.type = "PERCENT";
  }
  return report;
}

export function buildAlertCreationPayload(form: AlertCreationForm): Record<string, unknown> {
  let source: Record<string, unknown>;
  if (form.type === "PORT") {
    source = {
      type: "PORT",
      uri: form.uri.trim(),
      default_port: Number(form.defaultPort),
      reporting: reporting(form, true),
    };
  } else if (form.type === "WEB") {
    source = {
      type: "WEB",
      uri: {
        http: form.uri.trim(),
        default_port: Number(form.defaultPort),
        connection_timeout: Number(form.connectionTimeout),
      },
      reporting: reporting(form, false),
    };
  } else if (form.type === "SCRIPT") {
    source = { type: "SCRIPT", path: form.scriptPath.trim(), parameters: [] };
  } else {
    source = {
      type: "AGGREGATE",
      alert_name: form.aggregateAlertName.trim(),
      reporting: reporting(form, true),
    };
  }

  const payload: Record<string, unknown> = {
    "AlertDefinition/name": alertDefinitionName(form.label),
    "AlertDefinition/label": form.label.trim(),
    "AlertDefinition/description": form.description,
    "AlertDefinition/service_name": form.serviceName,
    "AlertDefinition/scope": form.scope,
    "AlertDefinition/interval": Number(form.interval),
    "AlertDefinition/source": source,
  };
  if (form.componentName) payload["AlertDefinition/component_name"] = form.componentName;
  return payload;
}
