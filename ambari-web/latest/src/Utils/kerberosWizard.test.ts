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

import { describe, expect, it, vi } from "vitest";
import {
  applyKerberosRecommendations,
  applyKdcPlanVisibility,
  appTimelineServerHost,
  buildDesiredConfigTagQuery,
  buildKerberosConfigurationPayload,
  buildKerberosRecommendationPayload,
  collectDescriptorFormValues,
  doesAppTimelineServerSupportKerberos,
  failedPreKerberizeChecks,
  isMissingHostComponentError,
  KDC_PLANS,
  kerberosWizardPersistenceResetPayload,
  kerberosWizardRecoveryPath,
  kerberosWizardStartPayload,
  removeDescriptorIdentityReferences,
  runKerberosClientInstall,
  runKerberosConfiguration,
  shouldBlockKerberosWizardNavigation,
  updateKerberosDescriptor,
} from "./kerberosWizard";

const configs = {
  KERBEROS: {
    KDC: {
      properties: {
        kdc_type: { type: "kerberos-env", value: KDC_PLANS.MANUAL },
      },
    },
    env: {
      properties: {
        manage_identities: { type: "kerberos-env", value: "true" },
        install_packages: { type: "kerberos-env", value: "true" },
      },
    },
    conf: {
      properties: {
        manage_krb5_conf: { type: "krb5-conf", value: "true" },
      },
    },
  },
};

describe("Kerberos wizard utilities", () => {
  it("forces all manual-mode management flags off", () => {
    const payload = buildKerberosConfigurationPayload(configs, KDC_PLANS.MANUAL);
    const desired = payload[0].Clusters.desired_config;

    expect(desired.find(({ type }) => type === "kerberos-env")?.properties).toMatchObject({
      kdc_type: "none",
      manage_identities: "false",
      install_packages: "false",
    });
    expect(desired.find(({ type }) => type === "krb5-conf")?.properties.manage_krb5_conf)
      .toBe("false");
  });

  it("shows only the manual exceptions and the selected automatic-mode fields", () => {
    const properties: {
      KERBEROS: Record<
        string,
        {
          properties: Record<
            string,
            { isVisible?: boolean; value?: unknown }
          >;
        }
      >;
    } = {
      KERBEROS: {
        KDC: { properties: {
          kdc_type: {}, realm: {}, ldap_url: {}, container_dn: {},
        } },
        Kadmin: { properties: { admin_principal: {}, admin_password: {} } },
        env: { properties: {
          executable_search_paths: {}, manage_identities: {},
          ad_create_attributes_template: {}, kdc_create_attributes: {}, ipa_user_group: {},
        } },
      },
    };

    const manual = applyKdcPlanVisibility(properties, KDC_PLANS.MANUAL);
    expect(manual.KERBEROS.KDC.properties.realm.isVisible).toBe(true);
    expect(manual.KERBEROS.Kadmin.properties.admin_principal.isVisible).toBe(false);
    expect(manual.KERBEROS.env.properties.executable_search_paths.isVisible).toBe(true);

    const ad = applyKdcPlanVisibility(properties, KDC_PLANS.ACTIVE_DIRECTORY);
    expect(ad.KERBEROS.KDC.properties.ldap_url.isVisible).toBe(true);
    expect(ad.KERBEROS.env.properties.ad_create_attributes_template.isVisible).toBe(true);
    expect(ad.KERBEROS.env.properties.kdc_create_attributes.isVisible).toBe(false);
    expect(ad.KERBEROS.env.properties.manage_identities).toMatchObject({
      isVisible: false,
      value: "true",
    });
  });

  it("forces IPA package and krb5.conf management off without disabling identities", () => {
    const payload = buildKerberosConfigurationPayload(configs, KDC_PLANS.IPA);
    const desired = payload[0].Clusters.desired_config;

    expect(desired.find(({ type }) => type === "kerberos-env")?.properties).toMatchObject({
      kdc_type: "ipa",
      manage_identities: "true",
      install_packages: "false",
    });
    expect(desired.find(({ type }) => type === "krb5-conf")?.properties.manage_krb5_conf)
      .toBe("false");
  });

  it("updates stack, configured, and inline identity values without mutating the source", () => {
    const descriptor = {
      properties: { realm: "OLD" },
      identities: [{ name: "spnego", principal: { value: "old" } }],
      services: [{
        name: "HDFS",
        configurations: [{ "hdfs-site": { "dfs.namenode.keytab.file": "/old" } }],
        identities: [{
          name: "namenode",
          keytab: { configuration: "hdfs-site/dfs.namenode.keytab.file" },
        }],
      }],
    };

    const updated = updateKerberosDescriptor(descriptor, [
      { name: "realm", filename: "stackConfigs", value: "NEW" },
      { name: "spnego_principal", filename: "cluster-env", value: "HTTP/_HOST@NEW" },
      { name: "dfs.namenode.keytab.file", filename: "hdfs-site", value: "/new" },
    ]);

    expect(updated.properties.realm).toBe("NEW");
    expect(updated.identities[0].principal.value).toBe("HTTP/_HOST@NEW");
    expect(updated.services[0].configurations[0]["hdfs-site"]["dfs.namenode.keytab.file"])
      .toBe("/new");
    expect(updated.services[0].identities[0]).toMatchObject({
      keytab: { file: "/new" },
    });
    expect(descriptor.properties.realm).toBe("OLD");
  });

  it("collects the current descriptor form value instead of the loaded value", () => {
    const values = collectDescriptorFormValues({
      KERBEROS_GENERAL: {
        General: {
          properties: {
            realm: {
              filename: "stackConfigs",
              propertyName: "realm",
              propertyValue: "OLD.EXAMPLE.COM",
              value: "NEW.EXAMPLE.COM",
            },
          },
        },
      },
    });

    expect(values).toEqual([
      {
        name: "realm",
        filename: "stackConfigs",
        value: "NEW.EXAMPLE.COM",
      },
    ]);
  });

  it("removes non-editable descriptor references before persistence", () => {
    const result = removeDescriptorIdentityReferences({
      identities: [{ name: "/ref" }, { name: "global" }],
      services: [{
        identities: [{ name: "service" }, { name: "alias", reference: "/ref" }],
        components: [{ identities: [{ name: "/component-ref" }, { name: "component" }] }],
      }],
    });

    expect(result.identities.map(({ name }) => name)).toEqual(["/ref", "global"]);
    expect(result.services[0].identities.map(({ name }) => name)).toEqual(["service"]);
    expect(result.services[0].components[0].identities.map(({ name }) => name))
      .toEqual(["component"]);
  });

  it("runs automatic configuration resources in dependency order", async () => {
    const calls: string[] = [];
    const action = (name: string) => vi.fn(async () => {
      calls.push(name);
    });

    await runKerberosConfiguration(KDC_PLANS.MIT, {
      deleteKerberosService: action("delete"),
      createKerberosResources: action("resources"),
      createConfigurations: action("configs"),
      createKerberosAdminSession: action("credentials"),
    });

    expect(calls).toEqual(["delete", "resources", "configs", "credentials"]);
  });

  it("skips Kerberos service/client resources in Manual mode", async () => {
    const deleteKerberosService = vi.fn();
    const createKerberosResources = vi.fn();
    const createConfigurations = vi.fn().mockResolvedValue(undefined);
    const createKerberosAdminSession = vi.fn().mockResolvedValue(undefined);

    await runKerberosConfiguration(KDC_PLANS.MANUAL, {
      deleteKerberosService,
      createKerberosResources,
      createConfigurations,
      createKerberosAdminSession,
    });

    expect(deleteKerberosService).not.toHaveBeenCalled();
    expect(createKerberosResources).not.toHaveBeenCalled();
    expect(createConfigurations).toHaveBeenCalledTimes(1);
    expect(createKerberosAdminSession).toHaveBeenCalledTimes(1);
  });

  it("stops automatic configuration after a failed prerequisite", async () => {
    const createKerberosResources = vi.fn().mockRejectedValue(
      new Error("resource creation failed"),
    );
    const createConfigurations = vi.fn();
    const createKerberosAdminSession = vi.fn();

    await expect(runKerberosConfiguration(KDC_PLANS.ACTIVE_DIRECTORY, {
      deleteKerberosService: vi.fn().mockResolvedValue(undefined),
      createKerberosResources,
      createConfigurations,
      createKerberosAdminSession,
    })).rejects.toThrow("resource creation failed");
    expect(createConfigurations).not.toHaveBeenCalled();
    expect(createKerberosAdminSession).not.toHaveBeenCalled();
  });

  it("installs the service when the Kerberos client component is INIT", async () => {
    const installKerberosService = vi.fn().mockResolvedValue({ Requests: { id: 1 } });
    const installKerberosClients = vi.fn();

    await expect(runKerberosClientInstall({
      getKerberosClientState: vi.fn().mockResolvedValue({
        ServiceComponentInfo: { state: "INIT" },
      }),
      installKerberosService,
      installKerberosClients,
    })).resolves.toEqual({ Requests: { id: 1 } });

    expect(installKerberosService).toHaveBeenCalledTimes(1);
    expect(installKerberosClients).not.toHaveBeenCalled();
  });

  it("installs all Kerberos client host-components when the service is initialized", async () => {
    const installKerberosService = vi.fn();
    const installKerberosClients = vi.fn().mockResolvedValue({ Requests: { id: 2 } });

    await expect(runKerberosClientInstall({
      getKerberosClientState: vi.fn().mockResolvedValue({
        ServiceComponentInfo: { state: "INSTALLED" },
      }),
      installKerberosService,
      installKerberosClients,
    })).resolves.toEqual({ Requests: { id: 2 } });

    expect(installKerberosService).not.toHaveBeenCalled();
    expect(installKerberosClients).toHaveBeenCalledTimes(1);
  });

  it("does not submit a Step 3 install after the component state request fails", async () => {
    const installKerberosService = vi.fn();
    const installKerberosClients = vi.fn();

    await expect(runKerberosClientInstall({
      getKerberosClientState: vi.fn().mockRejectedValue(new Error("state failed")),
      installKerberosService,
      installKerberosClients,
    })).rejects.toThrow("state failed");

    expect(installKerberosService).not.toHaveBeenCalled();
    expect(installKerberosClients).not.toHaveBeenCalled();
  });

  it("clears both wizard recovery namespaces on completion", () => {
    const payload = JSON.parse(kerberosWizardPersistenceResetPayload());

    expect(JSON.parse(payload.ENABLING_KERBEROS)).toEqual({
      kerberosWizardSteps: {},
    });
    expect(JSON.parse(payload.CLUSTER_STATE)).toEqual({});
    expect(JSON.parse(payload["wizard-data"])).toEqual({});
  });

  it("initializes and resolves Enable Kerberos recovery routes", () => {
    const payload = JSON.parse(kerberosWizardStartPayload("operator"));

    expect(JSON.parse(payload.ENABLING_KERBEROS)).toEqual({
      kerberosWizardSteps: {},
      activeStep: "GET_STARTED",
    });
    expect(JSON.parse(payload.CLUSTER_STATE)).toEqual({
      progressStatus: "ENABLING_KERBEROS",
      stepName: "GET_STARTED",
    });
    expect(JSON.parse(payload["wizard-data"])).toEqual({
      userName: "operator",
      controllerName: "kerberosWizardController",
    });
    expect(kerberosWizardRecoveryPath({
      progressStatus: "ENABLING_KERBEROS",
      stepName: "KERBERIZE_CLUSTER",
    })).toBe("/main/admin/kerberos/enable/step7");
    expect(kerberosWizardRecoveryPath({
      progressStatus: "ADDING_SERVICE",
      stepName: "KERBERIZE_CLUSTER",
    })).toBe("");

    expect(shouldBlockKerberosWizardNavigation(
      "/main/admin/kerberos/enable/step6",
      "/main/hosts",
      false,
    )).toBe(true);
    expect(shouldBlockKerberosWizardNavigation(
      "/main/admin/kerberos/enable/step8",
      "/main/admin/kerberos",
      false,
    )).toBe(false);
    expect(shouldBlockKerberosWizardNavigation(
      "/main/admin/kerberos/enable/step6",
      "/main/hosts",
      true,
    )).toBe(false);
  });

  it("builds and applies the Step 4 Stack Advisor contract", () => {
    expect(buildDesiredConfigTagQuery({
      "core-site": { tag: "version 1" },
      ignored: {},
    })).toBe("(type=core-site&tag=version%201)");

    const payload = buildKerberosRecommendationPayload({
      hostNames: ["host1"],
      serviceNames: ["HDFS", "KERBEROS", "HDFS"],
      hostGroups: {
        blueprint: { host_groups: [{ name: "host-group-1" }] },
        blueprint_cluster_binding: {
          host_groups: [{ name: "host-group-1", hosts: [{ fqdn: "host1" }] }],
        },
      },
      configurations: [{
        type: "hdfs-site",
        properties: { principal: "old" },
      }],
      descriptorConfigs: [
        { filename: "hdfs-site.xml", name: "principal", value: "descriptor" },
        { filename: "stackConfigs", name: "realm", value: "EXAMPLE.COM" },
      ],
    });

    expect(payload).toMatchObject({
      recommend: "configurations",
      hosts: ["host1"],
      services: ["HDFS", "KERBEROS"],
      recommendations: {
        blueprint: {
          configurations: {
            "hdfs-site": { properties: { principal: "descriptor" } },
          },
        },
      },
    });

    const result = applyKerberosRecommendations({
      KERBEROS_ADVANCED: {
        HDFS: {
          properties: {
            principal: {
              filename: "hdfs-site.xml",
              propertyName: "principal",
              propertyValue: "descriptor",
              value: "descriptor",
            },
          },
        },
      },
    }, {
      resources: [{
        recommendations: {
          blueprint: {
            configurations: {
              "hdfs-site": { properties: { principal: "recommended" } },
            },
          },
        },
      }],
    });

    expect(result.KERBEROS_ADVANCED.HDFS.properties.principal).toMatchObject({
      recommendedValue: "recommended",
      propertyValue: "recommended",
      value: "recommended",
    });
  });

  it("extracts only blocking pre-Kerberize checks", () => {
    const failed = failedPreKerberizeChecks({
      items: [
        { UpgradeChecks: { status: "PASS", check: "healthy" } },
        { UpgradeChecks: { status: "FAIL", check: "blocked" } },
        { UpgradeChecks: { status: "WARNING", check: "warning" } },
      ],
    });

    expect(failed).toEqual([
      { UpgradeChecks: { status: "FAIL", check: "blocked" } },
    ]);
  });

  it("derives ATS Kerberos support from the stack cardinality", () => {
    const stack = (cardinality: string) => ({
      items: [{
        StackServices: { service_name: "YARN" },
        components: [{
          StackServiceComponents: {
            component_name: "APP_TIMELINE_SERVER",
            cardinality,
          },
        }],
      }],
    });

    expect(doesAppTimelineServerSupportKerberos(stack("1"))).toBe(true);
    expect(doesAppTimelineServerSupportKerberos(stack("1+"))).toBe(true);
    expect(doesAppTimelineServerSupportKerberos(stack("0-1"))).toBe(false);
    expect(doesAppTimelineServerSupportKerberos({ items: [] })).toBe(false);
  });

  it("normalizes ATS discovery and idempotent deletion errors", () => {
    expect(appTimelineServerHost({
      items: [{ HostRoles: { host_name: "ats.example" } }],
    })).toBe("ats.example");
    expect(isMissingHostComponentError({ response: { status: 404 } })).toBe(true);
    expect(isMissingHostComponentError({
      response: { data: { message: "NoSuchResourceException" } },
    })).toBe(true);
    expect(isMissingHostComponentError({ response: { status: 500 } })).toBe(false);
  });
});
