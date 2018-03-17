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
import "rxjs/add/operator/toPromise";

import {AppStateService} from "@app/services/storage/app-state.service";
import {HttpClientService} from "@app/services/http-client.service";
import {ClustersService} from "@app/services/storage/clusters.service";
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {AuditFieldsDefinitionSet} from '@app/classes/object';

@Injectable()
export class AppLoadService {

  constructor(
    private httpClient: HttpClientService,
    private appStateService: AppStateService,
    private clustersStorage: ClustersService,
    private serviceLogsFieldsService: ServiceLogsFieldsService,
    private auditLogsFieldsService: AuditLogsFieldsService
  ) {
    this.appStateService.getParameter('isAuthorized').subscribe(this.initOnAuthorization);
  }

  loadClusters(): void {
    this.clustersStorage.clear();
    this.httpClient.get('clusters').first()
      .subscribe(
        (response: Response) => {
          const clusterNames = response.json();
          if (clusterNames) {
            this.clustersStorage.addInstances(clusterNames);
          }
        },
        (errorResponse: Response) => {
          this.clustersStorage.addInstances([]);
        }
      );
  }

  loadFieldsForLogs(): void {
    this.httpClient.get('serviceLogsFields').subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.serviceLogsFieldsService.addInstances(jsonResponse);
      }
    });
    this.httpClient.get('auditLogsFields').subscribe((response: Response): void => {
      const jsonResponse: AuditFieldsDefinitionSet = response.json();
      if (jsonResponse) {
        this.auditLogsFieldsService.setParameters(jsonResponse);
      }
    });
  }

  initOnAuthorization = (isAuthorized): void => {
    if (isAuthorized) {
      this.loadClusters();
        //this.loadFieldsForLogs();
    }
  }

  syncAuthorizedStateWithBackend(): Promise<any> {
    const statusRequest: Promise<Response> = this.httpClient.get('status').toPromise();
    return statusRequest.then(
      () => this.appStateService.setParameters({
          isAuthorized: true,
          isInitialLoading: false
        }),
      () => this.appStateService.setParameters({
          isAuthorized: false,
          isInitialLoading: false
        })
    );
  }
}
