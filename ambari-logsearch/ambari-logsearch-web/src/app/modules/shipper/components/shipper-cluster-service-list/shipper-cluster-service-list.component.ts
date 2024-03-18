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

import {Component, EventEmitter, Input, Output} from '@angular/core';
import 'rxjs/add/operator/startWith';
import {ShipperCluster} from '@modules/shipper/models/shipper-cluster.type';
import {ShipperClusterService} from '@modules/shipper/models/shipper-cluster-service.type';

@Component({
  selector: 'shipper-cluster-service-list',
  templateUrl: './shipper-cluster-service-list.component.html',
  styleUrls: ['./shipper-cluster-service-list.component.less']
})
export class ShipperClusterServiceListComponent {

  @Input()
  clusterName: ShipperCluster;

  @Input()
  serviceNamesList: ShipperClusterService[];

  @Input()
  selectedServiceName: ShipperClusterService;

  @Input()
  basePath: string[];

  @Output()
  selectionChange: EventEmitter<ShipperClusterService> = new EventEmitter<ShipperClusterService>();

  constructor() {}

  onShipperClusterServiceItemSelect = (serviceName: ShipperClusterService, event?: MouseEvent): void => {
    this.selectedServiceName = serviceName;
    this.selectionChange.emit(this.selectedServiceName);
    event && event.preventDefault();
  }

}
