/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
export type Properties = Record<string, string>;

export type QueueLabel = {
  name: string;
  capacity: number;
  maximumCapacity: number;
};

export type QueueConfig = {
  path: string;
  sourcePath: string;
  parentPath: string;
  name: string;
  depth: number;
  capacity: number;
  maximumCapacity: number;
  state: "RUNNING" | "STOPPED";
  aclAdministerQueue: string;
  aclSubmitApplications: string;
  userLimitFactor: number;
  minimumUserLimitPercent: number;
  maximumApplications: number | null;
  maximumAmResourcePercent: number | null;
  orderingPolicy: string;
  enableSizeBasedWeight: boolean;
  priority: number;
  maximumAllocationMb: number | null;
  maximumAllocationVcores: number | null;
  maximumApplicationLifetime: number | null;
  defaultApplicationLifetime: number | null;
  preemptionOverride: "inherit" | "enabled" | "disabled";
  labelsEnabled: boolean;
  accessAllLabels: boolean;
  accessibleLabels: string[];
  defaultNodeLabelExpression: string;
  labels: QueueLabel[];
};

export type SchedulerConfig = {
  maximumApplications: number;
  maximumAmResourcePercent: number;
  nodeLocalityDelay: number;
  resourceCalculator: string;
  queueMappings: string;
  queueMappingsOverride: boolean;
};

export type CapacityModel = {
  tag: string;
  clusterName: string;
  rawProperties: Properties;
  originalQueuePaths: string[];
  queues: QueueConfig[];
  scheduler: SchedulerConfig;
};

export type RuntimeInfo = {
  stackId: string;
  isOperator: boolean;
  isRmOffline: boolean;
  nodeLabelsEnabled: boolean;
  nodeLabelsConfigured: boolean;
  rangerEnabled: boolean;
  preemptionEnabled: boolean;
  rmQueueStates: Record<string, string>;
};

export type ConfigurationPayload = {
  items?: Array<{
    tag?: string;
    version?: number;
    properties?: Properties;
    Config?: { cluster_name?: string };
  }>;
};

export type ValidationIssue = {
  path: string;
  field: string;
  message: string;
};

export type VersionInfo = {
  tag: string;
  version?: number;
  created?: number;
};
