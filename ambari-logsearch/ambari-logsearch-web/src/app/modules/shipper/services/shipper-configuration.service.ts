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
import { Injectable } from '@angular/core';
import {Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/catch';
import {Store} from '@ngrx/store';

import {HttpClientService} from '@app/services/http-client.service';
import {ShipperClusterServiceConfigurationModel} from '@modules/shipper/models/shipper-cluster-service-configuration.model';
import {ShipperClusterServiceValidationModel} from '@modules/shipper/models/shipper-cluster-service-validation.model';
import {ShipperCluster} from '@modules/shipper/models/shipper-cluster.type';

@Injectable()
export class ShipperConfigurationService {

  constructor(
    private httpClientService: HttpClientService
  ) { }

  addConfiguration(configuration: ShipperClusterServiceConfigurationModel): Observable<ShipperClusterServiceConfigurationModel | Error> {
    return this.httpClientService.post(
      'shipperClusterServiceConfiguration',
      configuration.configuration,
      null,
      {
        cluster: configuration.cluster,
        service: configuration.service
      })
      .map((response: Response): ShipperClusterServiceConfigurationModel => {
        return configuration;
      })
      .catch((error: Response): Observable<Error> => {
        return Observable.of(new Error(error.json().message || ''));
      });
  }

  updateConfiguration(configuration: ShipperClusterServiceConfigurationModel): Observable<ShipperClusterServiceConfigurationModel | Error> {
    return this.httpClientService.put(
      'shipperClusterServiceConfiguration',
      configuration.configuration,
      null,
      {
        cluster: configuration.cluster,
        service: configuration.service
      })
      .map((response: Response): ShipperClusterServiceConfigurationModel => {
        return configuration;
      })
      .catch((error: Response): Observable<Error> => {
        return Observable.of(new Error(error.json().message || ''));
      });
  }

  loadConfiguration(cluster: string, service: string): Observable<{[key: string]: any}> {
    return this.httpClientService.get('shipperClusterServiceConfiguration', null, {
      service: service,
      cluster: cluster
    })
    .map((response) => {
      return response.json();
    })
    .catch((error: Response): Observable<Error> => {
      return Observable.of(new Error(error.json().message || ''));
    });
  }

  testConfiguration(payload: ShipperClusterServiceValidationModel): Observable<any> {
    const requestPayload: {[key: string]: any} = {
      shipper_config: payload.configuration,
      log_id: payload.componentName,
      test_entry: payload.sampleData
    };
    return this.httpClientService.postFormData('shipperClusterServiceConfigurationTest', requestPayload, null, {
      cluster: payload.clusterName
    })
    .map((response: Response) => response.json());
  }

}
