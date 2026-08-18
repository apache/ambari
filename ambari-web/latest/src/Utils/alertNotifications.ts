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

export type AlertNotificationUiType = "EMAIL" | "SNMP" | "Custom SNMP" | "Alert Script";
export type AlertNotificationApiType = "EMAIL" | "AMBARI_SNMP" | "SNMP" | "ALERT_SCRIPT";

const PASSWORD_KEY = "ambari.dispatch.credential.password";

const BUILT_IN_KEYS: Record<AlertNotificationUiType, string[]> = {
  EMAIL: [
    "ambari.dispatch.recipients",
    "mail.smtp.host",
    "mail.smtp.port",
    "mail.smtp.from",
    "mail.smtp.auth",
    "ambari.dispatch.credential.username",
    PASSWORD_KEY,
    "mail.smtp.starttls.enable",
  ],
  SNMP: [
    "ambari.dispatch.snmp.version",
    "ambari.dispatch.snmp.community",
    "ambari.dispatch.recipients",
    "ambari.dispatch.snmp.port",
  ],
  "Custom SNMP": [
    "ambari.dispatch.snmp.version",
    "ambari.dispatch.snmp.community",
    "ambari.dispatch.recipients",
    "ambari.dispatch.snmp.port",
    "ambari.dispatch.snmp.oids.trap",
    "ambari.dispatch.snmp.oids.subject",
    "ambari.dispatch.snmp.oids.body",
  ],
  "Alert Script": [
    "ambari.dispatch-property.script",
    "ambari.dispatch-property.script.filename",
  ],
};

export interface AlertNotificationForm {
  name: string;
  description: string;
  type: AlertNotificationUiType;
  global: boolean;
  groups: number[];
  alertStates: string[];
  recipients?: string;
  smtpHost?: string;
  smtpPort?: string;
  emailFrom?: string;
  useAuthentication?: boolean;
  username?: string;
  password?: string;
  passwordConfirmation?: string;
  startTls?: boolean;
  snmpVersion?: string;
  snmpCommunity?: string;
  snmpHosts?: string;
  snmpPort?: string;
  snmpOid?: string;
  scriptDispatchProperty?: string;
  scriptFilename?: string;
  customProperties?: Array<{ name: string; value: string }>;
  existingProperties?: Record<string, unknown>;
  preserveSensitivePassword?: boolean;
}

export interface AlertNotificationPayload {
  AlertTarget: {
    name: string;
    description: string;
    global: boolean;
    notification_type: AlertNotificationApiType;
    alert_states: string[];
    properties: Record<string, unknown>;
    groups?: number[];
  };
}

export function notificationTypeToApi(type: AlertNotificationUiType): AlertNotificationApiType {
  if (type === "SNMP") return "AMBARI_SNMP";
  if (type === "Custom SNMP") return "SNMP";
  if (type === "Alert Script") return "ALERT_SCRIPT";
  return "EMAIL";
}

export function notificationTypeToUi(type: string): AlertNotificationUiType {
  if (type === "AMBARI_SNMP") return "SNMP";
  if (type === "SNMP") return "Custom SNMP";
  if (type === "ALERT_SCRIPT") return "Alert Script";
  return "EMAIL";
}

export function notificationBuiltInKeys(type: AlertNotificationUiType): string[] {
  return [...BUILT_IN_KEYS[type]];
}

function list(value = ""): string[] {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function validateAlertNotificationForm(form: AlertNotificationForm): string[] {
  const errors: string[] = [];
  if (!form.name.trim()) errors.push("Notification name is required.");
  else if (!/^[\s0-9a-z_-]+$/i.test(form.name)) errors.push("Notification name contains invalid characters.");
  if (!form.global && form.groups.length === 0) errors.push("Select at least one Alert Group.");
  if (form.alertStates.length === 0) errors.push("Select at least one severity.");

  if (form.type === "EMAIL") {
    const recipients = list(form.recipients);
    if (recipients.length === 0) errors.push("At least one Email recipient is required.");
    else if (recipients.some((recipient) => !isEmail(recipient))) errors.push("Enter valid Email recipients.");
    if (!form.smtpHost?.trim()) errors.push("SMTP Server is required.");
    if (!/^\d+$/.test(form.smtpPort?.trim() || "") || Number(form.smtpPort) < 1 || Number(form.smtpPort) > 65535) {
      errors.push("SMTP Port must be between 1 and 65535.");
    }
    if (!form.emailFrom?.trim()) errors.push("Email From is required.");
    else if (!isEmail(form.emailFrom.trim())) errors.push("Email From must be a valid address.");
    if (form.useAuthentication && !form.username?.trim()) errors.push("SMTP Username is required when authentication is enabled.");
    if (form.useAuthentication && !form.preserveSensitivePassword && !form.password) {
      errors.push("SMTP Password is required when authentication is enabled.");
    }
    if (form.password && form.password !== form.passwordConfirmation) errors.push("SMTP passwords do not match.");
  }

  if (form.type === "SNMP" || form.type === "Custom SNMP") {
    if (list(form.snmpHosts).length === 0) errors.push("At least one SNMP host is required.");
    if (!form.snmpCommunity?.trim()) errors.push("SNMP Community is required.");
    if (!/^\d+$/.test(form.snmpPort?.trim() || "") || Number(form.snmpPort) < 1 || Number(form.snmpPort) > 65535) {
      errors.push("SNMP Port must be between 1 and 65535.");
    }
    if (form.type === "Custom SNMP" && !form.snmpOid?.trim()) errors.push("Custom SNMP OID is required.");
  }

  if (form.type === "Alert Script" && !form.scriptFilename?.trim()) {
    errors.push("Script Filename is required.");
  }

  const builtIns = new Set(notificationBuiltInKeys(form.type));
  const names = new Set<string>();
  for (const property of form.customProperties || []) {
    const name = property.name.trim();
    if (!/^[A-Za-z0-9._*-]+$/.test(name)) errors.push(`Invalid custom property name: ${name || "(empty)"}.`);
    if (builtIns.has(name)) errors.push(`Custom property conflicts with a built-in property: ${name}.`);
    if (names.has(name)) errors.push(`Duplicate custom property: ${name}.`);
    names.add(name);
  }
  return errors;
}

export function buildAlertNotificationPayload(form: AlertNotificationForm): AlertNotificationPayload {
  const properties: Record<string, unknown> = {};

  if (form.type === "EMAIL") {
    properties["ambari.dispatch.recipients"] = list(form.recipients);
    properties["mail.smtp.host"] = form.smtpHost?.trim() || "";
    properties["mail.smtp.port"] = form.smtpPort?.trim() || "";
    properties["mail.smtp.from"] = form.emailFrom?.trim() || "";
    properties["mail.smtp.auth"] = Boolean(form.useAuthentication);
    if (form.useAuthentication) {
      properties["ambari.dispatch.credential.username"] = form.username?.trim() || "";
      const password = form.password || "";
      if (password) {
        properties[PASSWORD_KEY] = password;
      } else if (form.preserveSensitivePassword && form.existingProperties && PASSWORD_KEY in form.existingProperties) {
        properties[PASSWORD_KEY] = form.existingProperties[PASSWORD_KEY];
      }
      properties["mail.smtp.starttls.enable"] = Boolean(form.startTls);
    }
  } else if (form.type === "SNMP" || form.type === "Custom SNMP") {
    properties["ambari.dispatch.snmp.version"] = form.snmpVersion || "SNMPv1";
    properties["ambari.dispatch.snmp.community"] = form.snmpCommunity?.trim() || "";
    properties["ambari.dispatch.recipients"] = list(form.snmpHosts);
    properties["ambari.dispatch.snmp.port"] = form.snmpPort?.trim() || "";
    if (form.type === "Custom SNMP") {
      const oid = form.snmpOid?.trim() || "";
      properties["ambari.dispatch.snmp.oids.trap"] = oid;
      properties["ambari.dispatch.snmp.oids.subject"] = oid;
      properties["ambari.dispatch.snmp.oids.body"] = oid;
    }
  } else {
    const dispatchProperty = form.scriptDispatchProperty?.trim();
    const filename = form.scriptFilename?.trim();
    if (dispatchProperty) properties["ambari.dispatch-property.script"] = dispatchProperty;
    if (filename) properties["ambari.dispatch-property.script.filename"] = filename;
  }

  for (const property of form.customProperties || []) {
    properties[property.name.trim()] = property.value;
  }

  const target: AlertNotificationPayload["AlertTarget"] = {
    name: form.name.trim(),
    description: form.description,
    global: form.global,
    notification_type: notificationTypeToApi(form.type),
    alert_states: [...form.alertStates],
    properties,
  };
  if (!form.global) target.groups = [...form.groups];
  return { AlertTarget: target };
}
