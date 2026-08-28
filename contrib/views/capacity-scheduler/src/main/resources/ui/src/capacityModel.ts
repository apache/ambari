/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import type {
  CapacityModel,
  ConfigurationPayload,
  Properties,
  QueueConfig,
  QueueLabel,
  ValidationIssue,
} from "./types";

export const CAPACITY_PREFIX = "yarn.scheduler.capacity";

const numberValue = (value: string | undefined, fallback: number | null): number | null => {
  if (value === undefined || value === "") return fallback;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

const booleanValue = (value: string | boolean | undefined) => value === true || value === "true";

const itemFromPayload = (payload: ConfigurationPayload) => payload.items?.[0] ?? {};

const queueFromProperties = (
  path: string,
  parentPath: string,
  depth: number,
  properties: Properties,
  nodeLabels: string[],
): QueueConfig => {
  const base = `${CAPACITY_PREFIX}.${path}`;
  const accessibleKey = `${base}.accessible-node-labels`;
  const hasAccessibleLabels = Object.prototype.hasOwnProperty.call(properties, accessibleKey);
  const accessibleValue = properties[accessibleKey]?.trim() ?? "";
  const accessAllLabels = accessibleValue === "*";
  const accessibleLabels = accessAllLabels
    ? [...nodeLabels]
    : accessibleValue ? accessibleValue.split(",").map((label) => label.trim()).filter(Boolean) : [];
  const knownLabels = [...new Set([...nodeLabels, ...accessibleLabels])];
  const labels: QueueLabel[] = knownLabels.map((name) => ({
    name,
    capacity: numberValue(properties[`${accessibleKey}.${name}.capacity`], path === "root" ? 100 : 0) ?? 0,
    maximumCapacity: numberValue(properties[`${accessibleKey}.${name}.maximum-capacity`], 100) ?? 100,
  }));
  const disablePreemption = properties[`${base}.disable_preemption`];

  return {
    path,
    sourcePath: path,
    parentPath,
    name: path.split(".").pop() ?? path,
    depth,
    capacity: numberValue(properties[`${base}.capacity`], path === "root" ? 100 : 0) ?? 0,
    maximumCapacity: numberValue(properties[`${base}.maximum-capacity`], 100) ?? 100,
    state: properties[`${base}.state`] === "STOPPED" ? "STOPPED" : "RUNNING",
    aclAdministerQueue: properties[`${base}.acl_administer_queue`] ?? "*",
    aclSubmitApplications: properties[`${base}.acl_submit_applications`] ?? "*",
    userLimitFactor: numberValue(properties[`${base}.user-limit-factor`], 1) ?? 1,
    minimumUserLimitPercent: numberValue(properties[`${base}.minimum-user-limit-percent`], 100) ?? 100,
    maximumApplications: numberValue(properties[`${base}.maximum-applications`], null),
    maximumAmResourcePercent: numberValue(properties[`${base}.maximum-am-resource-percent`], null) === null
      ? null
      : (numberValue(properties[`${base}.maximum-am-resource-percent`], 0) ?? 0) * 100,
    orderingPolicy: properties[`${base}.ordering-policy`] ?? "fifo",
    enableSizeBasedWeight: booleanValue(properties[`${base}.ordering-policy.fair.enable-size-based-weight`]),
    priority: numberValue(properties[`${base}.priority`], 0) ?? 0,
    maximumAllocationMb: numberValue(properties[`${base}.maximum-allocation-mb`], null),
    maximumAllocationVcores: numberValue(properties[`${base}.maximum-allocation-vcores`], null),
    maximumApplicationLifetime: numberValue(properties[`${base}.maximum-application-lifetime`], null),
    defaultApplicationLifetime: numberValue(properties[`${base}.default-application-lifetime`], null),
    preemptionOverride: disablePreemption === undefined ? "inherit" : booleanValue(disablePreemption) ? "disabled" : "enabled",
    labelsEnabled: hasAccessibleLabels,
    accessAllLabels,
    accessibleLabels,
    defaultNodeLabelExpression: properties[`${base}.default-node-label-expression`] ?? "",
    labels,
  };
};

export const parseCapacityModel = (payload: ConfigurationPayload, nodeLabels: string[] = []): CapacityModel => {
  const item = itemFromPayload(payload);
  const properties = { ...(item.properties ?? {}) };
  const queues: QueueConfig[] = [];
  const visit = (path: string, parentPath: string, depth: number) => {
    const queue = queueFromProperties(path, parentPath, depth, properties, nodeLabels);
    queues.push(queue);
    const children = properties[`${CAPACITY_PREFIX}.${path}.queues`]
      ?.split(",")
      .map((name) => name.trim())
      .filter(Boolean) ?? [];
    children.forEach((name) => visit(`${path}.${name}`, path, depth + 1));
  };
  visit("root", "", 0);

  return {
    tag: item.tag ?? "",
    clusterName: item.Config?.cluster_name ?? "",
    rawProperties: properties,
    originalQueuePaths: queues.map((queue) => queue.path),
    queues,
    scheduler: {
      maximumApplications: numberValue(properties[`${CAPACITY_PREFIX}.maximum-applications`], 10000) ?? 10000,
      maximumAmResourcePercent: (numberValue(properties[`${CAPACITY_PREFIX}.maximum-am-resource-percent`], 0.1) ?? 0.1) * 100,
      nodeLocalityDelay: numberValue(properties[`${CAPACITY_PREFIX}.node-locality-delay`], 40) ?? 40,
      resourceCalculator: properties[`${CAPACITY_PREFIX}.resource-calculator`] ?? "org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator",
      queueMappings: properties[`${CAPACITY_PREFIX}.queue-mappings`] ?? "",
      queueMappingsOverride: booleanValue(properties[`${CAPACITY_PREFIX}.queue-mappings-override.enable`]),
    },
  };
};

const setProperty = (properties: Properties, key: string, value: string | number | boolean | null | undefined) => {
  if (value === null || value === undefined || value === "") delete properties[key];
  else properties[key] = String(value);
};

export const serializeCapacityModel = (model: CapacityModel): Properties => {
  const originalPaths = [...model.originalQueuePaths].sort((left, right) => right.length - left.length);
  const rawByQueue = new Map<string, Array<[string, string]>>();
  const properties: Properties = {};

  Object.entries(model.rawProperties).forEach(([key, value]) => {
    const relative = key.startsWith(`${CAPACITY_PREFIX}.`) ? key.slice(CAPACITY_PREFIX.length + 1) : "";
    const sourcePath = originalPaths.find((path) => relative.startsWith(`${path}.`));
    if (sourcePath) {
      const entries = rawByQueue.get(sourcePath) ?? [];
      entries.push([relative.slice(sourcePath.length + 1), value]);
      rawByQueue.set(sourcePath, entries);
    } else {
      properties[key] = value;
    }
  });

  model.queues.forEach((queue) => {
    const base = `${CAPACITY_PREFIX}.${queue.path}`;
    (rawByQueue.get(queue.sourcePath) ?? []).forEach(([suffix, value]) => { properties[`${base}.${suffix}`] = value; });
  });

  setProperty(properties, `${CAPACITY_PREFIX}.maximum-applications`, model.scheduler.maximumApplications);
  setProperty(properties, `${CAPACITY_PREFIX}.maximum-am-resource-percent`, model.scheduler.maximumAmResourcePercent / 100);
  setProperty(properties, `${CAPACITY_PREFIX}.node-locality-delay`, model.scheduler.nodeLocalityDelay);
  setProperty(properties, `${CAPACITY_PREFIX}.resource-calculator`, model.scheduler.resourceCalculator);
  setProperty(properties, `${CAPACITY_PREFIX}.queue-mappings`, model.scheduler.queueMappings || null);
  setProperty(properties, `${CAPACITY_PREFIX}.queue-mappings-override.enable`, model.scheduler.queueMappingsOverride);

  model.queues.forEach((queue) => {
    const base = `${CAPACITY_PREFIX}.${queue.path}`;
    const children = model.queues.filter((candidate) => candidate.parentPath === queue.path).map((candidate) => candidate.name);
    setProperty(properties, `${base}.queues`, children.length ? children.join(",") : null);
    setProperty(properties, `${base}.capacity`, queue.capacity);
    setProperty(properties, `${base}.maximum-capacity`, queue.maximumCapacity);
    setProperty(properties, `${base}.state`, queue.state);
    setProperty(properties, `${base}.acl_administer_queue`, queue.aclAdministerQueue);
    setProperty(properties, `${base}.acl_submit_applications`, queue.aclSubmitApplications);
    setProperty(properties, `${base}.user-limit-factor`, queue.userLimitFactor);
    setProperty(properties, `${base}.minimum-user-limit-percent`, queue.minimumUserLimitPercent);
    setProperty(properties, `${base}.maximum-applications`, queue.maximumApplications);
    setProperty(properties, `${base}.maximum-am-resource-percent`, queue.maximumAmResourcePercent === null ? null : queue.maximumAmResourcePercent / 100);
    setProperty(properties, `${base}.ordering-policy`, queue.orderingPolicy || null);
    setProperty(properties, `${base}.ordering-policy.fair.enable-size-based-weight`, queue.orderingPolicy === "fair" ? queue.enableSizeBasedWeight : null);
    setProperty(properties, `${base}.priority`, queue.priority || null);
    setProperty(properties, `${base}.maximum-allocation-mb`, queue.maximumAllocationMb);
    setProperty(properties, `${base}.maximum-allocation-vcores`, queue.maximumAllocationVcores);
    setProperty(properties, `${base}.maximum-application-lifetime`, children.length ? null : queue.maximumApplicationLifetime);
    setProperty(properties, `${base}.default-application-lifetime`, children.length ? null : queue.defaultApplicationLifetime);
    setProperty(properties, `${base}.disable_preemption`, queue.preemptionOverride === "inherit" ? null : queue.preemptionOverride === "disabled");
    setProperty(properties, `${base}.default-node-label-expression`, queue.defaultNodeLabelExpression || null);

    const accessibleKey = `${base}.accessible-node-labels`;
    if (!queue.labelsEnabled) delete properties[accessibleKey];
    else properties[accessibleKey] = queue.accessAllLabels ? "*" : queue.accessibleLabels.join(",");
    queue.labels.forEach((label) => {
      const accessible = queue.accessAllLabels || queue.accessibleLabels.includes(label.name);
      setProperty(properties, `${accessibleKey}.${label.name}.capacity`, queue.labelsEnabled && accessible ? label.capacity : null);
      setProperty(properties, `${accessibleKey}.${label.name}.maximum-capacity`, queue.labelsEnabled && accessible ? label.maximumCapacity : null);
    });
  });

  return Object.fromEntries(Object.entries(properties).sort(([left], [right]) => left.localeCompare(right)));
};

export const addQueue = (model: CapacityModel, parent: string, name: string): CapacityModel => {
  const siblings = model.queues.filter((queue) => queue.parentPath === parent);
  const available = Math.max(0, 100 - siblings.reduce((total, queue) => total + queue.capacity, 0));
  const parentQueue = model.queues.find((queue) => queue.path === parent);
  const path = `${parent}.${name}`;
  const queue: QueueConfig = {
    ...(parentQueue ?? model.queues[0]),
    path,
    sourcePath: "",
    parentPath: parent,
    name,
    depth: (parentQueue?.depth ?? 0) + 1,
    capacity: available,
    maximumCapacity: 100,
    state: "RUNNING",
    maximumApplications: null,
    maximumAmResourcePercent: null,
    maximumApplicationLifetime: null,
    defaultApplicationLifetime: null,
    labels: (parentQueue?.labels ?? []).map((label) => ({ ...label, capacity: 0, maximumCapacity: 100 })),
  };
  return { ...model, queues: [...model.queues, queue] };
};

export const renameQueue = (model: CapacityModel, path: string, name: string): CapacityModel => {
  const queue = model.queues.find((candidate) => candidate.path === path);
  if (!queue || path === "root") return model;
  const nextRoot = `${queue.parentPath}.${name}`;
  return {
    ...model,
    queues: model.queues.map((candidate) => {
      if (candidate.path !== path && !candidate.path.startsWith(`${path}.`)) return candidate;
      const nextPath = `${nextRoot}${candidate.path.slice(path.length)}`;
      const nextParent = candidate.parentPath === path
        ? nextRoot
        : candidate.parentPath.startsWith(`${path}.`)
          ? `${nextRoot}${candidate.parentPath.slice(path.length)}`
          : candidate.parentPath;
      return { ...candidate, path: nextPath, parentPath: nextParent, name: candidate.path === path ? name : candidate.name };
    }),
  };
};

export const deleteQueue = (model: CapacityModel, path: string): CapacityModel => path === "root"
  ? model
  : { ...model, queues: model.queues.filter((queue) => queue.path !== path && !queue.path.startsWith(`${path}.`)) };

export const validateCapacityModel = (model: CapacityModel): ValidationIssue[] => {
  const issues: ValidationIssue[] = [];
  const paths = new Set<string>();
  model.queues.forEach((queue) => {
    if (!queue.name.trim()) issues.push({ path: queue.path, field: "name", message: "Queue name is required." });
    if (/\s/.test(queue.name)) issues.push({ path: queue.path, field: "name", message: "Queue name cannot contain whitespace." });
    if (paths.has(queue.path.toLowerCase())) issues.push({ path: queue.path, field: "name", message: "Queue name must be unique." });
    paths.add(queue.path.toLowerCase());
    if (queue.capacity < 0 || queue.capacity > 100) issues.push({ path: queue.path, field: "capacity", message: "Capacity must be between 0 and 100." });
    if (queue.maximumCapacity < queue.capacity || queue.maximumCapacity > 100) issues.push({ path: queue.path, field: "maximumCapacity", message: "Maximum capacity must be between capacity and 100." });
    if (queue.minimumUserLimitPercent < 0 || queue.minimumUserLimitPercent > 100) issues.push({ path: queue.path, field: "minimumUserLimitPercent", message: "Minimum user limit must be between 0 and 100." });
    if (queue.maximumAmResourcePercent !== null && (queue.maximumAmResourcePercent < 0 || queue.maximumAmResourcePercent > 100)) issues.push({ path: queue.path, field: "maximumAmResourcePercent", message: "Maximum AM resource must be between 0 and 100." });
    if (queue.defaultNodeLabelExpression && !queue.accessAllLabels && !queue.accessibleLabels.includes(queue.defaultNodeLabelExpression)) issues.push({ path: queue.path, field: "defaultNodeLabelExpression", message: "Default label must be accessible to the queue." });
    if (queue.maximumApplicationLifetime !== null && queue.defaultApplicationLifetime !== null && queue.maximumApplicationLifetime > 0 && queue.defaultApplicationLifetime > queue.maximumApplicationLifetime) issues.push({ path: queue.path, field: "defaultApplicationLifetime", message: "Default lifetime cannot exceed maximum lifetime." });
    queue.labels.forEach((label) => {
      if (label.maximumCapacity < label.capacity || label.capacity < 0 || label.maximumCapacity > 100) issues.push({ path: queue.path, field: `label:${label.name}`, message: `Invalid capacity for node label ${label.name}.` });
    });
  });
  model.queues.forEach((parent) => {
    const children = model.queues.filter((queue) => queue.parentPath === parent.path);
    if (children.length) {
      const total = children.reduce((sum, queue) => sum + queue.capacity, 0);
      if (Math.abs(total - 100) > 0.01) issues.push({ path: parent.path, field: "childrenCapacity", message: `Child queue capacities total ${total}; expected 100.` });
      const labels = new Set(children.flatMap((queue) => queue.accessAllLabels ? queue.labels.map((label) => label.name) : queue.accessibleLabels));
      labels.forEach((label) => {
        const labelTotal = children.reduce((sum, queue) => sum + (queue.labels.find((entry) => entry.name === label)?.capacity ?? 0), 0);
        if (labelTotal > 0 && Math.abs(labelTotal - 100) > 0.01) issues.push({ path: parent.path, field: `label:${label}`, message: `Child capacity for label ${label} totals ${labelTotal}; expected 100.` });
      });
    }
  });
  if (model.scheduler.maximumApplications < 1) issues.push({ path: "scheduler", field: "maximumApplications", message: "Maximum applications must be positive." });
  if (model.scheduler.maximumAmResourcePercent < 0 || model.scheduler.maximumAmResourcePercent > 100) issues.push({ path: "scheduler", field: "maximumAmResourcePercent", message: "Maximum AM resource must be between 0 and 100." });
  const mappings = model.scheduler.queueMappings.trim();
  const leafNames = new Set(model.queues
    .filter((queue) => !model.queues.some((candidate) => candidate.parentPath === queue.path))
    .map((queue) => queue.name));
  if (mappings && mappings !== "u:%user:%primary_group" && mappings !== "u:%user:%user") {
    mappings.split(",").map((mapping) => mapping.trim()).forEach((mapping) => {
      const [kind, name, queue, ...extra] = mapping.split(":");
      if (extra.length || !["u", "g"].includes(kind) || !name || !queue || !leafNames.has(queue)) {
        issues.push({
          path: "scheduler",
          field: "queueMappings",
          message: `Invalid queue mapping "${mapping}". Use u|g:name:leaf_queue.`,
        });
      }
    });
  }
  return issues;
};

const escapeXml = (value: string) => value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&apos;");

export const propertiesToXml = (properties: Properties) => `<?xml version="1.0"?>\n<configuration>\n${Object.entries(properties).map(([name, value]) => `  <property>\n    <name>${escapeXml(name)}</name>\n    <value>${escapeXml(value)}</value>\n  </property>`).join("\n")}\n</configuration>`;

export const propertyDiff = (before: Properties, after: Properties) => [...new Set([...Object.keys(before), ...Object.keys(after)])]
  .sort()
  .filter((key) => before[key] !== after[key])
  .map((key) => ({ key, before: before[key], after: after[key] }));
