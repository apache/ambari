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
import 'rxjs/add/operator/toPromise';
import {TranslateService} from '@ngx-translate/core';

import {AppStateService} from '@app/services/storage/app-state.service';
import {HttpClientService} from '@app/services/http-client.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {AuditFieldsDefinitionSet} from '@app/classes/object';
import {Observable} from 'rxjs/Observable';
import {UtilsService} from '@app/services/utils.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {NodeItem} from '@app/classes/models/node-item';
import {ComponentsService} from '@app/services/storage/components.service';

@Injectable()
export class AppLoadService {

  constructor(
    private httpClient: HttpClientService,
    private appStateService: AppStateService,
    private clustersStorage: ClustersService,
    private serviceLogsFieldsService: ServiceLogsFieldsService,
    private auditLogsFieldsService: AuditLogsFieldsService,
    private translationService: TranslateService,
    private hostStoreService: HostsService,
    private componentsStorageService: ComponentsService
  ) {
    this.appStateService.getParameter('isAuthorized').subscribe(this.initOnAuthorization);
    this.appStateService.setParameter('isInitialLoading', true);
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

  clearClusters(): void {
    this.clustersStorage.clear();
  }

  loadHosts(): Observable<Response> {
    this.hostStoreService.clear();
    const request = this.httpClient.get('hosts');
    request.subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        hosts = jsonResponse && jsonResponse.vNodeList;
      if (hosts) {
        this.hostStoreService.addInstances(hosts);
      }
    });
    return request;
  }

  clearHosts(): void {
    this.hostStoreService.clear();
  }

  loadComponents(): Observable<Response[]> {
    const requestComponentsData: Observable<Response> = this.httpClient.get('components');
    const requestComponentsName: Observable<Response> = this.httpClient.get('serviceComponentsName');
    const requests = Observable.combineLatest(requestComponentsName, requestComponentsData);
    requests.subscribe(([componentsNamesResponse, componentsDataResponse]: Response[]) => {
      const componentsNames = componentsNamesResponse.json();
      const componentsData = componentsDataResponse.json();
      const components = componentsData && componentsData.vNodeList.map((item): NodeItem => {
        const component = componentsNames.metadata.find(componentItem => componentItem.name === item.name);
        return Object.assign(item, {
          label: component && (component.label || item.name),
          group: component && component.group && {
            name: component.group,
            label: componentsNames.groups[component.group]
          },
          value: item.logLevelCount.reduce((currentValue: number, currentItem): number => {
            return currentValue + Number(currentItem.value);
          }, 0)
        });
      });
      if (components) {
        this.componentsStorageService.addInstances(components);
      }
    });
    return requests;
  }

  clearComponents(): void {
    this.componentsStorageService.clear();
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

  clearFieldsForLogs(): void {

  }

  initOnAuthorization = (isAuthorized): void => {
    if (isAuthorized) {
      this.loadClusters();
      this.loadHosts();
      this.loadComponents();
      // this.loadFieldsForLogs();
    } else {
      this.clearClusters();
      this.clearHosts();
      this.clearComponents();
    }
  }

  syncAuthorizedStateWithBackend(): Promise<any> {
    const statusRequest: Promise<Response> = this.httpClient.get('status').toPromise();
    statusRequest.then(
      () => this.appStateService.setParameters({
        isAuthorized: true,
        isInitialLoading: false
      }),
      () => this.appStateService.setParameters({
        isAuthorized: false,
        isInitialLoading: false
      })
    );
    return statusRequest;
  }

  setTranslationService() {
    this.translationService.setDefaultLang('en');
    return this.translationService.use('en').toPromise();
  }

}
