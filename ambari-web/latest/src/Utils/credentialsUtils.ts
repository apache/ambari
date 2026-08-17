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
    resource: any
  ) {
    return await CredentialsApi.createCredentials(clusterName, alias, {
      resource,
    });
  },
  getCredential: async function (
    clusterName: string,
    alias: string,
    successCallback: any,
    errorCallback: any
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
    resource: any
  ) {
    return await CredentialsApi.updateCredentials(clusterName, alias, {
      resource,
    });
  },
  createOrUpdateCredentials: async function (
    clusterName: string,
    alias: string,
    resource: any
  ) {
    try {
      await CredentialsApi.getCredentials(clusterName, alias);
      return await this.updateCredentials(clusterName, alias, resource);
    } catch (error: any) {
      const status = error?.response?.status ?? error?.status;
      if (status !== 404) {
        throw error;
      }
      return await this.createCredentials(clusterName, alias, resource);
    }
  },
  credentials: async function (clusterName: string, callback: Function) {
    const data = await CredentialsApi.listCredentials(clusterName);
    callback(map(data?.items, "Credential"));
  },
  removeCredentials: async function (clusterName: string, alias: string) {
    return await CredentialsApi.deleteCredentials(clusterName, alias);
  },
  storageInfo: async function (clusterName: string, callback?: Function) {
    const json = await CredentialsApi.credentialsStoreInfo(clusterName);
    if (json.Clusters) {
      const storage = json?.Clusters?.credential_store_properties ?? {};
      let storeTypesObject: any = {};
      storeTypesObject[this.STORE_TYPES.PERSISTENT_KEY] =
        storage[this.STORE_TYPES.PERSISTENT_PATH] === "true";
      storeTypesObject[this.STORE_TYPES.TEMPORARY_KEY] =
        storage[this.STORE_TYPES.TEMPORARY_PATH] === "true";
      callback?.(storeTypesObject);
      return storeTypesObject;
    } else {
      callback?.(null);
      return null;
    }
  },
  isStorePersisted: async function (clusterName: string) {
    return await this.storeTypeStatus(clusterName, this.STORE_TYPES.PERSISTENT_KEY);
  },
  isStoreTemporary: async function (clusterName: string) {
    return await this.storeTypeStatus(clusterName, this.STORE_TYPES.TEMPORARY_KEY);
  },
  storeTypeStatus: async function (clusterName: string, type: string) {
    const storage = await this.storageInfo(clusterName);
    return storage?.[type] ?? false;
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
  isKDCCredentialsPersisted: function (credentials: any) {
    var kdcCredentials = find(credentials, [
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
