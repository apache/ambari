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

import { ServiceApi } from '../api/serviceApi';

export async function redirectToAdminView(adminPage = "") {
//   if (!localStorage.getItem('app.isAuthorized.CLUSTER.UPGRADE_DOWNGRADE_STACK')) {
//     window.location.replace('/#/login');
//     return;
//   }

  try {
    const data = await ServiceApi.getAmbariServerVersion();
    const components = data?.components || [];

    if (Array.isArray(components)) {
      const mappedVersions = components
        .filter(component => 
          component?.RootServiceComponents?.component_name === 'AMBARI_SERVER' &&
          component?.RootServiceComponents?.component_version
        )
        .map(component => component.RootServiceComponents.component_version);

      if (mappedVersions.length > 0) {
        const sortedVersions = mappedVersions.sort();
        const latestVersion = sortedVersions[sortedVersions.length - 1].replace(/[^\d.-]/g, '');
        const appRoot = window.location.origin;
        window.location.replace(`${appRoot}/views/ADMIN_VIEW/${latestVersion}/INSTANCE/#/${adminPage}`);
      }
    }
  } catch (error) {
    console.error('Error getting admin version:', error);
    window.location.replace('/#/main/views/index');
  }
}