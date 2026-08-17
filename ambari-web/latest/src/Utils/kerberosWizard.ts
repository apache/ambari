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
import { getCardinalityValue } from "./numberUtils";

export const KDC_PLANS = {
  MIT: "Existing MIT KDC",
  ACTIVE_DIRECTORY: "Existing Active Directory",
  IPA: "Existing IPA",
  MANUAL: "Manage Kerberos principals and keytabs manually",
} as const;

export const KDC_TYPES: Record<string, string> = {
  [KDC_PLANS.MIT]: "mit-kdc",
  [KDC_PLANS.ACTIVE_DIRECTORY]: "active-directory",
  [KDC_PLANS.IPA]: "ipa",
  [KDC_PLANS.MANUAL]: "none",
};

type ConfigProperty = {
  type?: string;
  value?: unknown;
};

type ConfigProperties = Record<
  string,
  Record<string, { properties?: Record<string, ConfigProperty> }>
>;

type UpgradeCheckItem = {
  UpgradeChecks?: { status?: string; [key: string]: unknown };
  [key: string]: unknown;
};

type StackServiceComponent = {
  StackServiceComponents?: {
    component_name?: string;
    cardinality?: string;
  };
};

type StackService = {
  StackServices?: { service_name?: string };
  StackService?: { service_name?: string };
  components?: StackServiceComponent[];
};

type DescriptorFormProperty = Record<string, unknown> & {
  filename?: string;
  propertyName?: string;
  propertyValue?: unknown;
  recommendedValue?: unknown;
  value?: unknown;
};

type DescriptorFormValues = Record<
  string,
  Record<string, { properties?: Record<string, DescriptorFormProperty> }>
>;

type KdcPlanVisibility = {
  KERBEROS: Record<
    string,
    {
      properties: Record<
        string,
        Record<string, unknown> & { isVisible?: boolean; value?: unknown }
      >;
    }
  >;
};

export type DescriptorConfig = {
  name: string;
  filename: string;
  value: unknown;
};

type DescriptorIdentity = Record<string, unknown> & {
  name?: string;
  reference?: string;
};

type DescriptorResource = Record<string, unknown> & {
  configurations?: Array<Record<string, Record<string, unknown>>>;
  identities?: DescriptorIdentity[];
  components?: DescriptorResource[];
};

type KerberosDescriptor = DescriptorResource & {
  properties?: Record<string, unknown>;
  services?: DescriptorResource[];
};

export function isManualKdcPlan(plan: string): boolean {
  return plan === KDC_PLANS.MANUAL;
}

export function failedPreKerberizeChecks(
  response?: { items?: UpgradeCheckItem[] },
): UpgradeCheckItem[] {
  return (response?.items ?? []).filter(
    (item) => item.UpgradeChecks?.status === "FAIL",
  );
}

export function doesAppTimelineServerSupportKerberos(
  serviceComponentInfo?: { items?: StackService[] },
): boolean {
  const yarn = (serviceComponentInfo?.items ?? []).find(
    (service) =>
      service?.StackServices?.service_name === "YARN"
      || service?.StackService?.service_name === "YARN",
  );
  const timelineServer = (yarn?.components ?? []).find(
    (component) =>
      component?.StackServiceComponents?.component_name
        === "APP_TIMELINE_SERVER",
  );
  const cardinality = timelineServer?.StackServiceComponents?.cardinality;
  return Boolean(cardinality && getCardinalityValue(cardinality, false) > 0);
}

export function appTimelineServerHost(
  response?: { items?: Array<{ HostRoles?: { host_name?: string } }> },
): string {
  return response?.items?.[0]?.HostRoles?.host_name ?? "";
}

export function isMissingHostComponentError(error: unknown): boolean {
  const errorRecord = error && typeof error === "object"
    ? error as Record<string, unknown>
    : {};
  const response = errorRecord.response && typeof errorRecord.response === "object"
    ? errorRecord.response as Record<string, unknown>
    : {};
  const data = response.data && typeof response.data === "object"
    ? response.data as Record<string, unknown>
    : response.data;
  const status = response.status ?? errorRecord.status;
  const message = String(
    (data && typeof data === "object"
      ? (data as Record<string, unknown>).message
      : data)
      ?? errorRecord.message
      ?? "",
  );
  return status === 404 || message.includes("NoSuchResourceException");
}

type KerberosConfigurationActions = {
  deleteKerberosService: () => Promise<unknown>;
  createKerberosResources: () => Promise<unknown>;
  createConfigurations: () => Promise<unknown>;
  createKerberosAdminSession: () => Promise<unknown>;
};

export async function runKerberosConfiguration(
  plan: string,
  actions: KerberosConfigurationActions,
) {
  if (!isManualKdcPlan(plan)) {
    await actions.deleteKerberosService();
    await actions.createKerberosResources();
  }
  await actions.createConfigurations();
  await actions.createKerberosAdminSession();
}

type KerberosClientInstallActions = {
  getKerberosClientState: () => Promise<{
    ServiceComponentInfo?: { state?: string };
  }>;
  installKerberosService: () => Promise<unknown>;
  installKerberosClients: () => Promise<unknown>;
};

export async function runKerberosClientInstall(
  actions: KerberosClientInstallActions,
) {
  const response = await actions.getKerberosClientState();
  if (response?.ServiceComponentInfo?.state === "INIT") {
    return actions.installKerberosService();
  }
  return actions.installKerberosClients();
}

export function kerberosWizardPersistenceResetPayload() {
  return JSON.stringify({
    ENABLING_KERBEROS: JSON.stringify({ kerberosWizardSteps: {} }),
    CLUSTER_STATE: JSON.stringify({}),
  });
}

const KERBEROS_WIZARD_STEP_NAMES = [
  "",
  "GET_STARTED",
  "CONFIGURE_KERBEROS",
  "INSTALL_AND_TEST_KERBEROS_CLIENT",
  "CONFIGURE_IDENTITIES",
  "CONFIRM_CONFIGURATION",
  "STOP_SERVICES",
  "KERBERIZE_CLUSTER",
  "START_AND_TEST_SERVICES",
];

export function kerberosWizardStartPayload() {
  return JSON.stringify({
    ENABLING_KERBEROS: JSON.stringify({
      kerberosWizardSteps: {},
      activeStep: "GET_STARTED",
    }),
    CLUSTER_STATE: JSON.stringify({
      progressStatus: "ENABLING_KERBEROS",
      stepName: "GET_STARTED",
    }),
  });
}

export function kerberosWizardRecoveryPath(clusterState?: {
  progressStatus?: string;
  stepName?: string;
}): string {
  if (clusterState?.progressStatus !== "ENABLING_KERBEROS") {
    return "";
  }
  const step = KERBEROS_WIZARD_STEP_NAMES.indexOf(clusterState.stepName ?? "");
  return step > 0 ? `/main/admin/kerberos/enable/step${step}` : "";
}

export function shouldBlockKerberosWizardNavigation(
  currentPath: string,
  nextPath: string,
  allowExit: boolean,
): boolean {
  return currentPath.includes("/kerberos/enable/")
    && !currentPath.endsWith("/step8")
    && !allowExit
    && currentPath !== nextPath;
}

export function buildDesiredConfigTagQuery(
  desiredConfigs?: Record<string, { tag?: string }>,
): string {
  return Object.entries(desiredConfigs ?? {})
    .filter(([, config]) => Boolean(config.tag))
    .map(([type, config]) =>
      `(type=${encodeURIComponent(type)}&tag=${encodeURIComponent(config.tag ?? "")})`,
    )
    .join("|");
}

type KerberosRecommendationPayloadOptions = {
  hostNames: string[];
  serviceNames: string[];
  hostGroups?: {
    blueprint?: { host_groups?: unknown[] };
    blueprint_cluster_binding?: { host_groups?: unknown[] };
  };
  configurations: Array<{
    type?: string;
    properties?: Record<string, unknown>;
  }>;
  descriptorConfigs: Array<Partial<DescriptorConfig>>;
};

export function buildKerberosRecommendationPayload({
  hostNames,
  serviceNames,
  hostGroups,
  configurations,
  descriptorConfigs,
}: KerberosRecommendationPayloadOptions) {
  const blueprintConfigurations: Record<
    string,
    { properties: Record<string, unknown> }
  > = {};
  (configurations ?? []).forEach((configuration) => {
    if (configuration?.type) {
      blueprintConfigurations[configuration.type] = {
        properties: cloneDeep(configuration.properties ?? {}),
      };
    }
  });
  (descriptorConfigs ?? []).forEach((config) => {
    const type = configType(config?.filename ?? "");
    if (!type || type === "stackConfigs" || !config?.name) {
      return;
    }
    blueprintConfigurations[type] ??= { properties: {} };
    blueprintConfigurations[type].properties[config.name] = config.value;
  });

  return {
    recommend: "configurations",
    hosts: hostNames,
    services: Array.from(new Set(serviceNames)),
    recommendations: {
      blueprint: {
        host_groups: hostGroups?.blueprint?.host_groups ?? [],
        configurations: blueprintConfigurations,
      },
      blueprint_cluster_binding: {
        host_groups:
          hostGroups?.blueprint_cluster_binding?.host_groups ?? [],
      },
    },
  };
}

export function applyKerberosRecommendations<T>(
  configProperties: T,
  response?: {
    resources?: Array<{
      recommendations?: {
        blueprint?: {
          configurations?: Record<
            string,
            { properties?: Record<string, unknown> }
          >;
        };
      };
    }>;
  },
): T {
  const result = cloneDeep(configProperties);
  const recommendations = response?.resources?.[0]?.recommendations
    ?.blueprint?.configurations ?? {};

  Object.values(result as DescriptorFormValues).forEach((service) => {
    Object.values(service).forEach((category) => {
      Object.values(category.properties ?? {}).forEach((property) => {
        const type = configType(property.filename ?? "");
        const properties = recommendations?.[type]?.properties;
        const propertyName = property.propertyName;
        if (
          properties
          && propertyName
          && Object.prototype.hasOwnProperty.call(
            properties,
            propertyName,
          )
        ) {
          const recommendedValue = properties[propertyName];
          property.recommendedValue = recommendedValue;
          property.propertyValue = recommendedValue;
          property.value = recommendedValue;
        }
      });
    });
  });
  return result;
}

const AD_ONLY_PROPERTIES = new Set([
  "ldap_url",
  "container_dn",
  "ad_create_attributes_template",
  "password_length",
  "password_min_digits",
  "password_min_lowercase_letters",
  "password_min_punctuation",
  "password_min_uppercase_letters",
  "password_min_whitespace",
]);
const MIT_ONLY_PROPERTIES = new Set(["kdc_create_attributes"]);
const IPA_ONLY_PROPERTIES = new Set(["ipa_user_group"]);
const MANUAL_PROPERTIES = new Set(["kdc_type", "realm", "executable_search_paths"]);

export function applyKdcPlanVisibility<T>(
  configProperties: T,
  plan: string,
): T & KdcPlanVisibility {
  const result = cloneDeep(configProperties);
  const editableResult = result as T & KdcPlanVisibility;
  Object.values(editableResult.KERBEROS ?? {}).forEach((category) => {
    Object.entries(category.properties ?? {}).forEach(([name, rawProperty]) => {
      const property = rawProperty;
      if (plan === KDC_PLANS.MANUAL) {
        property.isVisible = MANUAL_PROPERTIES.has(name);
        return;
      }

      property.isVisible = !AD_ONLY_PROPERTIES.has(name)
        && !MIT_ONLY_PROPERTIES.has(name)
        && !IPA_ONLY_PROPERTIES.has(name);
      if (plan === KDC_PLANS.ACTIVE_DIRECTORY && AD_ONLY_PROPERTIES.has(name)) {
        property.isVisible = true;
      }
      if (plan === KDC_PLANS.MIT && MIT_ONLY_PROPERTIES.has(name)) {
        property.isVisible = true;
      }
      if (plan === KDC_PLANS.IPA && IPA_ONLY_PROPERTIES.has(name)) {
        property.isVisible = true;
      }
      if (name === "manage_identities") {
        property.isVisible = false;
        property.value = "true";
      }
    });
  });
  return editableResult;
}

export function buildKerberosConfigurationPayload(
  configProperties: ConfigProperties,
  plan: string,
) {
  const serviceConfigs = configProperties.KERBEROS ?? {};
  const configsByType: Record<string, { properties: Record<string, unknown> }> = {};

  Object.values(serviceConfigs).forEach((category) => {
    Object.entries(category.properties ?? {}).forEach(([name, property]) => {
      if (!property.type || name === "Test.KDC.Connection") {
        return;
      }
      configsByType[property.type] ??= { properties: {} };
      configsByType[property.type].properties[name] = property.value;
    });
  });

  const kerberosEnv = configsByType["kerberos-env"]?.properties;
  const krb5Conf = configsByType["krb5-conf"]?.properties;
  if (kerberosEnv) {
    kerberosEnv.kdc_type = KDC_TYPES[plan] ?? plan;
    if (plan === KDC_PLANS.MANUAL) {
      kerberosEnv.manage_identities = "false";
      kerberosEnv.install_packages = "false";
    } else {
      kerberosEnv.manage_identities = "true";
    }
    if (plan === KDC_PLANS.IPA) {
      kerberosEnv.install_packages = "false";
    }
  }
  if (krb5Conf && (plan === KDC_PLANS.MANUAL || plan === KDC_PLANS.IPA)) {
    krb5Conf.manage_krb5_conf = "false";
  }

  const desiredConfig = Object.entries(configsByType).map(([type, config]) => ({
    type,
    properties: config.properties,
    service_config_version_note:
      "This is the initial configuration created by Enable Kerberos wizard.",
  }));

  return [{ Clusters: { desired_config: desiredConfig } }];
}

export function collectDescriptorFormValues(formValues: unknown): DescriptorConfig[] {
  const configs: DescriptorConfig[] = [];
  Object.values(formValues as DescriptorFormValues).forEach((service) => {
    Object.values(service).forEach((category) => {
      Object.values(category.properties ?? {}).forEach((property) => {
        if (property.filename && property.propertyName) {
          configs.push({
            name: property.propertyName,
            filename: property.filename,
            value: property.value,
          });
        }
      });
    });
  });
  return configs;
}

function configType(filename: string): string {
  return filename.endsWith(".xml") ? filename.slice(0, -4) : filename;
}

function updateIdentities(
  identities: DescriptorIdentity[] | undefined,
  config: DescriptorConfig,
): boolean {
  let updated = false;
  (identities ?? []).forEach((identity) => {
    Object.entries(identity).forEach(([key, rawProperty]) => {
      if (key === "name" || !rawProperty || typeof rawProperty !== "object") {
        return;
      }
      const property = rawProperty as Record<string, unknown>;
      const configuredProperty = typeof property.configuration === "string"
        ? property.configuration.split("/")
        : undefined;
      const matchesConfiguredIdentity = configuredProperty
        && configuredProperty[0] === configType(config.filename)
        && configuredProperty[1] === config.name;
      const matchesInlineIdentity = !property.configuration
        && `${identity.name}_${key}` === config.name;
      if (matchesConfiguredIdentity || matchesInlineIdentity) {
        property[key === "keytab" ? "file" : "value"] = config.value;
        updated = true;
      }
    });
  });
  return updated;
}

function updateConfigurations(
  configurations: DescriptorResource["configurations"],
  config: DescriptorConfig,
): boolean {
  let updated = false;
  if (Array.isArray(configurations)) {
    configurations.forEach((configuration) => {
      const properties = configuration?.[configType(config.filename)];
      if (properties && Object.prototype.hasOwnProperty.call(properties, config.name)) {
        properties[config.name] = config.value;
        updated = true;
      }
    });
  }
  return updated;
}

export function updateKerberosDescriptor<T extends KerberosDescriptor>(
  descriptor: T,
  configs: DescriptorConfig[],
): T {
  const updatedDescriptor = cloneDeep(descriptor);

  configs.forEach((config) => {
    if (
      configType(config.filename) === "stackConfigs"
      && Object.prototype.hasOwnProperty.call(updatedDescriptor.properties ?? {}, config.name)
    ) {
      const properties = updatedDescriptor.properties ??= {};
      properties[config.name] = config.value;
    }

    updateIdentities(updatedDescriptor.identities, config);
    (updatedDescriptor.services ?? []).forEach((service) => {
      updateConfigurations(service.configurations, config);
      updateIdentities(service.identities, config);
      (service.components ?? []).forEach((component) => {
        updateConfigurations(component.configurations, config);
        updateIdentities(component.identities, config);
      });
    });
  });

  return updatedDescriptor;
}

export function removeDescriptorIdentityReferences<T extends KerberosDescriptor>(
  descriptor: T,
): T {
  const result = cloneDeep(descriptor);
  const isEditableIdentity = (identity: DescriptorIdentity) =>
    !identity?.reference && !String(identity?.name ?? "").startsWith("/");

  (result.services ?? []).forEach((service) => {
    service.identities = (service.identities ?? []).filter(isEditableIdentity);
    (service.components ?? []).forEach((component) => {
      component.identities = (component.identities ?? []).filter(isEditableIdentity);
    });
  });
  return result;
}
