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
const InitialData = {
  'app': {
    'loginName': '',
    'authenticated': false,
    'configs': [],
    'tags': [],
    'tables': {
      'filterConditions': {},
      'displayLength': {},
      'startIndex': {},
      'sortingConditions': {},
      'selectedItems': {}
    }
  },

  'Installer': {},
  'AddHost': {},
  'AddService': {},
  'WidgetWizard': {},
  'KerberosWizard': {},
  'ReassignMaster': {},
  'AddSecurity': {},
  'AddAlertDefinition': {
    content: {}
  },
  'HighAvailabilityWizard': {},
  'RMHighAvailabilityWizard': {},
  'AddHawqStandbyWizard': {},
  'RemoveHawqStandbyWizard': {},
  'ActivateHawqStandbyWizard': {},
  'RAHighAvailabilityWizard': {},
  'NameNodeFederationWizard': {},
  'RollbackHighAvailabilityWizard': {},
  'MultipleNameNodeWizard': {},
  'MainAdminStackAndUpgrade': {},
  'KerberosDisable': {},
  'tmp': {}

};
export const LocalStorageOps = {

  setItem(key: string, value: string) {
    localStorage.setItem(key, value);
  },

  getItem(key: string) {
    return localStorage.getItem(key);
  },

  cleanUpLocalStorage() {
    localStorage.setItem("ambari", JSON.stringify(InitialData));
  }
}
