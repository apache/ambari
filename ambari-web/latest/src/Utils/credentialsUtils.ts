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

import { find, map } from "lodash";
import CredentialsApi from "../api/credentialsApi";

type CredentialResource = {
  principal: string;
  key: string;
  type: string;
};

type StoredCredential = {
  alias?: string;
  type?: string;
};

const credentialsUtils = {
  STORE_TYPES: {
    TEMPORARY: "temporary",
    PERSISTENT: "persisted",
    PERSISTENT_KEY: "persistent",
    TEMPORARY_KEY: "temporary",
    PERSISTENT_PATH: "storage.persistent",
    TEMPORARY_PATH: "storage.temporary",
  },
  ALIAS: {
    KDC_CREDENTIALS: "kdc.admin.credential",
  },
  createCredentials: async function (
    clusterName: string,
    alias: string,
    resource: CredentialResource,
  ) {
    return await CredentialsApi.createCredentials(clusterName, alias, {
      resource,
    });
  },
  getCredential: async function (
    clusterName: string,
    alias: string,
    successCallback: (data: unknown) => void,
    errorCallback: () => void,
  ) {
    try {
      const data = await CredentialsApi.getCredentials(clusterName, alias);
      if (successCallback) {
        successCallback(data);
      }
    } catch {
      errorCallback();
    }
  },
  updateCredentials: async function (
    clusterName: string,
    alias: string,
    resource: CredentialResource,
  ) {
    return await CredentialsApi.updateCredentials(clusterName, alias, {
      resource,
    });
  },
  createOrUpdateCredentials: async function (
    clusterName: string,
    alias: string,
    resource: CredentialResource,
  ) {
    try {
      await CredentialsApi.getCredentials(clusterName, alias);
      return await this.updateCredentials(clusterName, alias, resource);
    } catch (error: unknown) {
      const credentialError = error as {
        message?: string;
        status?: number;
        response?: { status?: number; data?: { message?: string } };
      };
      const message = String(
        credentialError.response?.data?.message || credentialError.message || "",
      );
      if (
        credentialError.response?.status === 404 ||
        credentialError.status === 404 ||
        /NoSuchResourceException|not found/i.test(message)
      ) {
        return await this.createCredentials(clusterName, alias, resource);
      }
      throw error;
    }
  },
  credentials: async function (
    clusterName: string,
    callback: (credentials: StoredCredential[]) => void,
  ) {
    const data = await CredentialsApi.listCredentials(clusterName);
    callback(map(data?.items, "Credential"));
  },
  removeCredentials: async function (clusterName: string, alias: string) {
    return await CredentialsApi.deleteCredentials(clusterName, alias);
  },
  storageInfo: async function (
    clusterName: string,
    callback: (storage: Record<string, boolean> | null) => void,
  ) {
    const json = await CredentialsApi.credentialsStoreInfo(clusterName);
    if (json.Clusters) {
      const storage = json?.Clusters?.credential_store_properties ?? {};
      const storeTypesObject: Record<string, boolean> = {};
      storeTypesObject[this.STORE_TYPES.PERSISTENT_KEY] =
        storage[this.STORE_TYPES.PERSISTENT_PATH] === "true";
      storeTypesObject[this.STORE_TYPES.TEMPORARY_KEY] =
        storage[this.STORE_TYPES.TEMPORARY_PATH] === "true";
      callback(storeTypesObject);
    } else {
      callback(null);
    }
  },
  isStorePersisted: function (clusterName: string) {
    return this.storeTypeStatus(clusterName, this.STORE_TYPES.PERSISTENT_KEY);
  },
  isStoreTemporary: function (clusterName: string) {
    return this.storeTypeStatus(clusterName, this.STORE_TYPES.TEMPORARY_KEY);
  },
  storeTypeStatus: function (clusterName: string, type: string) {
    this.storageInfo(clusterName, function (storage) {
      return storage?.[type];
    });
  },
  createCredentialResource: function (
    principal: string,
    key: string,
    type: string
  ) {
    return {
      principal: principal,
      key: key,
      type: type,
    };
  },
  isKDCCredentialsPersisted: function (credentials: StoredCredential[]) {
    const kdcCredentials = find(credentials, [
      "alias",
      this.ALIAS.KDC_CREDENTIALS,
    ]);
    if (kdcCredentials) {
      const type =
        kdcCredentials.type !== undefined
          ? kdcCredentials.type
          : this.STORE_TYPES.TEMPORARY;
      return type === this.STORE_TYPES.PERSISTENT;
    }
    return false;
  },
};

export default credentialsUtils;
