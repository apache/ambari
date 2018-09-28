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
import {LogsContainerComponent} from '@app/components/logs-container/logs-container.component';
import {LoginFormComponent} from '@app/components/login-form/login-form.component';
import {AuthGuardService} from '@app/services/auth-guard.service';
import {TabGuard} from '@app/services/tab.guard';
import {LogsBreadcrumbsResolverService} from '@app/services/logs-breadcrumbs-resolver.service';
import {LoginScreenGuardService} from '@app/services/login-screen-guard.service';

const appRoutes: Routes = [{
    path: 'login',
    component: LoginFormComponent,
    data: {
      breadcrumbs: 'login.title'
    },
    canActivate: [LoginScreenGuardService]
  }, {
    path: 'logs/:activeTab',
    component: LogsContainerComponent,
    data: {
      breadcrumbs: 'logs.title',
      multiClusterFilter: true,
      clusterParamKey: 'clusters'
    },
    resolve: {
      breadcrumbs: LogsBreadcrumbsResolverService
    },
    canActivate: [AuthGuardService, TabGuard]
  }, {
    path: 'logs',
    redirectTo: '/logs/serviceLogs',
    pathMatch: 'full'
  }, {
    path: '',
    redirectTo: '/logs/serviceLogs',
    pathMatch: 'full'
  }, {
    path: '**',
    redirectTo: '/logs/serviceLogs'
  }
];

@NgModule({
  imports: [
    RouterModule.forRoot(
      appRoutes,
      { enableTracing: false, useHash: true }
    )
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule {}
