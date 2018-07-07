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
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {Http} from '@angular/http';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';

import {TypeaheadModule} from 'ngx-bootstrap';

import {SharedModule} from '@modules/shared/shared.module';

import {TranslateService as AppTranslateService} from '@app/services/translate.service';

import {ShipperRoutingModule} from './shipper-routing.module';
import {ShipperClusterServiceListComponent} from './components/shipper-cluster-service-list/shipper-cluster-service-list.component';
import {ShipperServiceConfigurationFormComponent} from './components/shipper-service-configuration-form/shipper-service-configuration-form.component';
import {ShipperConfigurationStore} from './stores/shipper-configuration.store';
import {ShipperConfigurationComponent} from './components/shipper-configuration/shipper-configuration.component';
import {ShipperClusterServiceListService} from './services/shipper-cluster-service-list.service';
import {ShipperConfigurationService} from './services/shipper-configuration.service';
import {ShipperGuard} from '@modules/shipper/services/shipper.guard';

@NgModule({
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    SharedModule,
    TypeaheadModule.forRoot(),
    ShipperRoutingModule,
    TranslateModule.forChild({
      loader: {
        provide: TranslateLoader,
        useFactory: AppTranslateService.httpLoaderFactory,
        deps: [Http]
      }
    })
  ],
  declarations: [
    ShipperClusterServiceListComponent,
    ShipperServiceConfigurationFormComponent,
    ShipperConfigurationComponent
  ],
  providers: [
    ShipperConfigurationStore,
    ShipperConfigurationService,
    ShipperClusterServiceListService,
    ShipperGuard
  ]
})
export class ShipperModule {}
