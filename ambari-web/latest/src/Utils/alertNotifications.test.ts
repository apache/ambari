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
  buildAlertNotificationPayload,
  notificationTypeToApi,
  notificationTypeToUi,
  validateAlertNotificationForm,
  type AlertNotificationForm,
} from "./alertNotifications";

const base = (type: AlertNotificationForm["type"]): AlertNotificationForm => ({
  name: "target",
  description: "description",
  type,
  global: false,
  groups: [1, 2],
  alertStates: ["CRITICAL", "WARNING"],
});

describe("alert notification contracts", () => {
  it("maps API and UI notification types in both directions", () => {
    expect(notificationTypeToApi("SNMP")).toBe("AMBARI_SNMP");
    expect(notificationTypeToApi("Custom SNMP")).toBe("SNMP");
    expect(notificationTypeToApi("Alert Script")).toBe("ALERT_SCRIPT");
    expect(notificationTypeToUi("AMBARI_SNMP")).toBe("SNMP");
    expect(notificationTypeToUi("SNMP")).toBe("Custom SNMP");
    expect(notificationTypeToUi("ALERT_SCRIPT")).toBe("Alert Script");
  });

  it("builds authenticated Email and preserves an unchanged sensitive password", () => {
    const payload = buildAlertNotificationPayload({
      ...base("EMAIL"),
      recipients: "one@example.test, two@example.test",
      smtpHost: "smtp.example.test",
      smtpPort: "587",
      emailFrom: "ambari@example.test",
      useAuthentication: true,
      username: "ambari",
      password: "",
      passwordConfirmation: "",
      startTls: true,
      preserveSensitivePassword: true,
      existingProperties: { "ambari.dispatch.credential.password": "********" },
    });

    expect(payload.AlertTarget.notification_type).toBe("EMAIL");
    expect(payload.AlertTarget.properties).toMatchObject({
      "ambari.dispatch.recipients": ["one@example.test", "two@example.test"],
      "mail.smtp.auth": true,
      "ambari.dispatch.credential.username": "ambari",
      "ambari.dispatch.credential.password": "********",
      "mail.smtp.starttls.enable": true,
    });
  });

  it("builds built-in SNMP with recipients instead of an unsupported host key", () => {
    const payload = buildAlertNotificationPayload({
      ...base("SNMP"),
      snmpVersion: "SNMPv2c",
      snmpCommunity: "public",
      snmpHosts: "host-1,host-2",
      snmpPort: "162",
    });

    expect(payload.AlertTarget.notification_type).toBe("AMBARI_SNMP");
    expect(payload.AlertTarget.properties["ambari.dispatch.recipients"]).toEqual(["host-1", "host-2"]);
    expect(payload.AlertTarget.properties).not.toHaveProperty("ambari.dispatch.snmp.host");
  });

  it("builds Custom SNMP with all three OID keys", () => {
    const payload = buildAlertNotificationPayload({
      ...base("Custom SNMP"),
      snmpCommunity: "private",
      snmpHosts: "host-1",
      snmpPort: "162",
      snmpOid: "1.3.6.1.4.1",
    });

    expect(payload.AlertTarget.notification_type).toBe("SNMP");
    expect(payload.AlertTarget.properties).toMatchObject({
      "ambari.dispatch.snmp.oids.trap": "1.3.6.1.4.1",
      "ambari.dispatch.snmp.oids.subject": "1.3.6.1.4.1",
      "ambari.dispatch.snmp.oids.body": "1.3.6.1.4.1",
    });
  });

  it("builds Alert Script with the Classic property names", () => {
    const payload = buildAlertNotificationPayload({
      ...base("Alert Script"),
      scriptDispatchProperty: "scripts",
      scriptFilename: "notify.py",
    });

    expect(payload.AlertTarget.notification_type).toBe("ALERT_SCRIPT");
    expect(payload.AlertTarget.properties).toEqual({
      "ambari.dispatch-property.script": "scripts",
      "ambari.dispatch-property.script.filename": "notify.py",
    });
  });

  it("rejects custom property conflicts with corrected built-in keys", () => {
    const errors = validateAlertNotificationForm({
      ...base("Custom SNMP"),
      snmpCommunity: "private",
      snmpHosts: "host-1",
      snmpPort: "162",
      snmpOid: "1.3.6",
      customProperties: [
        { name: "ambari.dispatch.snmp.oids.body", value: "duplicate" },
        { name: "invalid key", value: "bad" },
      ],
    });

    expect(errors).toContain("Custom property conflicts with a built-in property: ambari.dispatch.snmp.oids.body.");
    expect(errors).toContain("Invalid custom property name: invalid key.");
  });

  it("validates addresses, names, and new authenticated passwords", () => {
    const errors = validateAlertNotificationForm({
      ...base("EMAIL"),
      name: "invalid/name",
      recipients: "not-an-address",
      smtpHost: "smtp.example.test",
      smtpPort: "587",
      emailFrom: "also-invalid",
      useAuthentication: true,
      username: "ambari",
      password: "",
      passwordConfirmation: "different",
    });

    expect(errors).toContain("Notification name contains invalid characters.");
    expect(errors).toContain("Enter valid Email recipients.");
    expect(errors).toContain("Email From must be a valid address.");
    expect(errors).toContain("SMTP Password is required when authentication is enabled.");
  });
});
