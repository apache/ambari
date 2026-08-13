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
import { Utility } from './Utility';

interface DbData {
    app: {
      loginName: string;
      authenticated: boolean;
      user?: any;
      auth?: any;
      configs: any[];
      tags: any[];
      tables: {
        filterConditions: Record<string, any>;
        displayLength: Record<string, any>;
        startIndex: Record<string, any>;
        sortingConditions: Record<string, any>;
        selectedItems: Record<string, any>;
      };
    };
    Installer: Record<string, any>;
    AddHost: Record<string, any>;
    AddService: Record<string, any>;
    WidgetWizard: Record<string, any>;
    KerberosWizard: Record<string, any>;
    ReassignMaster: Record<string, any>;
    AddSecurity: Record<string, any>;
    HighAvailabilityWizard: Record<string, any>;
    RollbackHighAvailabilityWizard: Record<string, any>;
    tmp: Record<string, any>;
    [key: string]: any;
  }

  const InitialData: DbData = {
    app: {
      loginName: '',
      authenticated: false,
      configs: [],
      tags: [],
      tables: {
        filterConditions: {},
        displayLength: {},
        startIndex: {},
        sortingConditions: {},
        selectedItems: {}
      }
    },
    Installer: {},
    AddHost: {},
    AddService: {},
    WidgetWizard: {},
    KerberosWizard: {},
    ReassignMaster: {},
    AddSecurity: {},
    HighAvailabilityWizard: {},
    RollbackHighAvailabilityWizard: {},
    tmp: {}
  };

  class Database {
    private data: DbData;

    constructor() {
      this.data = this.getDb() || InitialData;
    }

    private checkNamespace(data: DbData, namespace: string): boolean {
      if (!namespace) return false;
      if (!data[namespace]) {
        data[namespace] = {};
      }
      return true;
    }

    getDb(): DbData | null {
      try {
        const stored = localStorage.getItem('ambari');
        if (!stored) return null;
        try {
          return JSON.parse(Utility.decryptData(stored));
        } catch {
          return JSON.parse(stored);
        }
      } catch (e) {
        console.error('Error reading from localStorage:', e);
        return null;
      }
    }

    private setDb(data: DbData): void {
      try {
        localStorage.setItem('ambari', Utility.encryptData(JSON.stringify(data)));
      } catch (e) {
        console.error('Error writing to localStorage:', e);
      }
    }

    getItem(key: string): string | null {
      const value = localStorage.getItem(key);
      if (value === null) return null;
      const decrypted = Utility.decryptData(value);
      try {
        JSON.parse(decrypted);
        return decrypted;
      } catch {
        return value;
      }
    }
    setItem(key: string, value: string): void {
      try {
        // Always encrypt before storing to ensure consistency
        const encrypted = Utility.encryptData(value);
        localStorage.setItem(key, encrypted);
      } catch (e) {
        console.error(`Error encrypting data for key ${key}:`, e);
        // Fallback to storing unencrypted data
        localStorage.setItem(key, value);
        console.warn(`Stored unencrypted data for key ${key} due to encryption failure`);
      }
    }

    cleanUp(): void {
      this.data = JSON.parse(JSON.stringify(InitialData));
      this.setDb(this.data);
    }

    clearSession(): void {
      const data = this.getDb() || this.getInitialData();
      data.app = {
        ...data.app,
        loginName: '',
        authenticated: false,
      };
      delete data.app.user;
      delete data.app.auth;
      this.data = data;
      this.setDb(data);
    }

    createNameSpace(namespace: string): void {
      const data = this.getDb() || this.getInitialData();
      if (!data[namespace]) {
        data[namespace] = {};
        this.data = data;
        this.setDb(data);
      }
    }

    // Core get/set methods
    get(namespace: string, key: string): any {
      const data = this.getDb();
      if (!data || !this.checkNamespace(data, namespace)) return null;
      return key.includes('user-pref') ? 
        data[namespace][key] : 
        this.getNestedValue(data[namespace], key);
    }

    set(namespace: string, key: string, value: any): void {
      const data = this.getDb() || this.getInitialData();
      if (!this.checkNamespace(data, namespace)) return;
      if (key.includes('user-pref')) {
        data[namespace][key] = value;
      } else {
        this.setNestedValue(data[namespace], key, value);
      }
      this.data = data;
      this.setDb(data);
    }

    setSession(loginName: string, user: unknown, authorizations: string[]): void {
      const data = this.getDb() || this.getInitialData();
      data.app = {
        ...data.app,
        loginName: encodeURIComponent(loginName),
        authenticated: true,
        user,
        auth: authorizations,
      };
      this.data = data;
      this.setDb(data);
    }

    private getNestedValue(obj: any, path: string): any {
      return path.split('.').reduce((acc, part) => acc && acc[part], obj);
    }

    private setNestedValue(obj: any, path: string, value: any): void {
      const parts = path.split('.');
      const last = parts.pop()!;
      const target = parts.reduce((acc, part) => {
        if (!acc[part]) acc[part] = {};
        return acc[part];
      }, obj);
      target[last] = value;
    }

    getInitialData(): DbData {
      return JSON.parse(JSON.stringify(InitialData));
    }

  }

  export const db = new Database()
