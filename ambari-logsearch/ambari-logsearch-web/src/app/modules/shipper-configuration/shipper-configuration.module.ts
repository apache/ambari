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

import { NgModule }       from '@angular/core';
import {ShipperConfigurationRoutingModule} from "@app/modules/shipper-configuration/shipper-configuration-routing.module";
import {ShipperConfigurationListComponent} from "@app/modules/shipper-configuration/shipper-configuration-list.component";
import {ShipperConfigurationFormComponent} from "@app/modules/shipper-configuration/shipper-configuration-form.component";
import {ShipperConfigurationService} from "@app/modules/shipper-configuration/shipper-configuration.service";

@NgModule({
  imports: [
    ShipperConfigurationRoutingModule
  ],
  declarations: [
    ShipperConfigurationListComponent,
    ShipperConfigurationFormComponent
  ],
  providers: [ ShipperConfigurationService ]
})
export class ShipperConfigurationModule {}
