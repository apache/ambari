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

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AuthGuardService} from '@app/services/auth-guard.service';
import {CanDeactivateGuardService} from '@modules/shared/services/can-deactivate-guard.service';

import {ShipperConfigurationComponent} from './components/shipper-configuration/shipper-configuration.component';

const shipperRoutes: Routes = [{
  path: 'shipper/:cluster/add',
  component: ShipperConfigurationComponent,
  data: {
    breadcrumbs: ['shipperConfiguration.breadcrumbs.title', 'shipperConfiguration.breadcrumbs.add'],
    multiClusterFilter: false
  },
  canActivate: [AuthGuardService],
  canDeactivate: [CanDeactivateGuardService]
}, {
  path: 'shipper/:cluster/:service',
  component: ShipperConfigurationComponent,
  data: {
    breadcrumbs: ['shipperConfiguration.breadcrumbs.title', 'shipperConfiguration.breadcrumbs.update'],
    multiClusterFilter: false
  },
  canActivate: [AuthGuardService],
  canDeactivate: [CanDeactivateGuardService]
}, {
  path: 'shipper/:cluster',
  component: ShipperConfigurationComponent,
  data: {
    breadcrumbs: 'shipperConfiguration.breadcrumbs.title',
    multiClusterFilter: false
  },
  canActivate: [AuthGuardService]
}, {
  path: 'shipper',
  component: ShipperConfigurationComponent,
  data: {
    breadcrumbs: 'shipperConfiguration.breadcrumbs.title',
    multiClusterFilter: false
  },
  canActivate: [AuthGuardService]
}];

@NgModule({
  imports: [
    RouterModule.forChild(shipperRoutes)
  ],
  exports: [
    RouterModule
  ]
})
export class ShipperRoutingModule {
}
