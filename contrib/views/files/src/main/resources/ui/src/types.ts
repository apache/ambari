/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
export type HdfsFile = {
  path: string;
  isDirectory: boolean;
  readAccess?: boolean;
  writeAccess?: boolean;
  executeAccess?: boolean;
  len: number;
  owner: string;
  group: string;
  permission: string;
  accessTime?: number | string;
  modificationTime?: number | string;
  blockSize?: number;
  replication?: number;
  isEncrypted?: boolean;
  isErasureCoded?: boolean;
  erasureCodingPolicyName?: string;
};

export type DirectoryListing = {
  files: HdfsFile[];
  meta: {
    path?: string;
    originalSize?: number;
    finalSize?: number;
    truncated?: boolean;
    nameFilter?: string;
  };
};

export type OperationResult = {
  success: boolean;
  message?: string;
  succeeded?: string[];
  failed?: string[];
  unprocessed?: string[];
};

export type ViewContext = {
  view: string;
  version: string;
  instance: string;
};

export type Notification = {
  id: number;
  level: "success" | "error" | "info";
  title: string;
  detail?: string;
};
