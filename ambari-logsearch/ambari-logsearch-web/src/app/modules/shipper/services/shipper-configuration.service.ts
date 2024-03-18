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
import {Response, ResponseOptions, ResponseType} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/catch';

import {HttpClientService} from '@app/services/http-client.service';
import {ShipperClusterServiceConfigurationModel} from '@modules/shipper/models/shipper-cluster-service-configuration.model';
import {ShipperClusterServiceValidationModel} from '@modules/shipper/models/shipper-cluster-service-validation.model';

@Injectable()
export class ShipperConfigurationService {

  constructor(
    private httpClientService: HttpClientService
  ) { }

  createResponseWithConfigBody(configuration: ShipperClusterServiceConfigurationModel, originalResponse?: Response): Response {
    return new Response(
      new ResponseOptions({
        body: configuration,
        status: originalResponse ? originalResponse.status : null,
        statusText: originalResponse ? originalResponse.statusText : null,
        headers: originalResponse ? originalResponse.headers : null,
        type: originalResponse ? originalResponse.type : ResponseType.Basic,
        url: originalResponse ? originalResponse.url : ''
      })
    );
  }

  addConfiguration(configuration: ShipperClusterServiceConfigurationModel): Observable<Response | Error> {
    return this.httpClientService.post(
      'shipperClusterServiceConfiguration',
      configuration.configuration,
      null,
      {
        cluster: configuration.cluster,
        service: configuration.service
      })
      .map((response: Response): Response => this.createResponseWithConfigBody(configuration, response))
      .catch((error: Response): Observable<Response> => {
        return Observable.of(error);
      });
  }

  updateConfiguration(configuration: ShipperClusterServiceConfigurationModel): Observable<Response> {
    return this.httpClientService.put(
      'shipperClusterServiceConfiguration',
      configuration.configuration,
      null,
      {
        cluster: configuration.cluster,
        service: configuration.service
      })
      .map((response: Response): Response => this.createResponseWithConfigBody(configuration, response))
      .catch((error: Response): Observable<Response> => {
        return Observable.of(error);
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

  testConfiguration(payload: ShipperClusterServiceValidationModel): Observable<Response> {
    const requestPayload: {[key: string]: any} = {
      shipperConfig: encodeURIComponent(payload.configuration),
      logId: payload.componentName,
      testEntry: payload.sampleData
    };
    return this.httpClientService.postFormData('shipperClusterServiceConfigurationTest', requestPayload, null, {
      cluster: payload.clusterName
    });
  }

}
