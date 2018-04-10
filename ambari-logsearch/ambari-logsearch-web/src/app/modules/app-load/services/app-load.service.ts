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

import {AppStateService} from 'app/services/storage/app-state.service';
import {HttpClientService} from 'app/services/http-client.service';
import {ClustersService} from 'app/services/storage/clusters.service';
import {ServiceLogsFieldsService} from 'app/services/storage/service-logs-fields.service';
import {AuditLogsFieldsService} from 'app/services/storage/audit-logs-fields.service';
import {AuditFieldsDefinitionSet, LogField} from 'app/classes/object';
import {Observable} from 'rxjs/Observable';
import {HostsService} from 'app/services/storage/hosts.service';
import {NodeItem} from 'app/classes/models/node-item';
import {ComponentsService} from 'app/services/storage/components.service';
import {DataAvailabilityValues} from 'app/classes/string';

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

  loadClusters(): Observable<Response> {
    this.clustersStorage.clear();
    this.appStateService.setParameter('clustersDataState', DataAvailabilityValues.LOADING);
    const response$: Observable<Response> = this.httpClient.get('clusters').filter((response: Response) => response.ok).first();
    response$.subscribe(
      (response: Response) => {
        const clusterNames = response.json();
        if (clusterNames) {
          this.clustersStorage.addInstances(clusterNames);
          this.appStateService.setParameter('clustersDataState', DataAvailabilityValues.AVAILABLE);
        }
      },
      (errorResponse: Response) => {
        this.clustersStorage.addInstances([]);
        this.appStateService.setParameter('clustersDataState', DataAvailabilityValues.ERROR);
      }
    );
    return response$;
  }

  clearClusters(): void {
    this.clustersStorage.clear();
  }

  loadHosts(): Observable<Response> {
    this.hostStoreService.clear();
    this.appStateService.setParameter('hostsDataState', DataAvailabilityValues.LOADING);
    const response$ = this.httpClient.get('hosts').filter((response: Response) => response.ok);
    response$.subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        hosts = jsonResponse && jsonResponse.vNodeList;
      if (hosts) {
        this.hostStoreService.addInstances(hosts);
        this.appStateService.setParameter('hostsDataState', DataAvailabilityValues.AVAILABLE);
      }
    }, () => {
      this.hostStoreService.addInstances([]);
      this.appStateService.setParameter('hostsDataState', DataAvailabilityValues.ERROR);
    });
    return response$;
  }

  clearHosts(): void {
    this.hostStoreService.clear();
  }

  loadComponents(): Observable<[{[key: string]: any}, {[key: string]: any}]> {
    this.appStateService.setParameter('componentsDataState', DataAvailabilityValues.LOADING);
    const responseComponentsData$: Observable<Response> = this.httpClient.get('components').first()
      .filter((response: Response) => response.ok)
      .map((response: Response) => response.json());
    const responseComponentsName$: Observable<Response> = this.httpClient.get('serviceComponentsName').first()
      .filter((response: Response) => response.ok)
      .map((response: Response) => response.json());
    const responses$ = Observable.combineLatest(responseComponentsName$, responseComponentsData$);
    responses$.subscribe(([componentsNames, componentsData]: [{[key: string]: any}, {[key: string]: any}]) => {
      const components = componentsData && componentsData.vNodeList && componentsData.vNodeList.map((item): NodeItem => {
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
        this.appStateService.setParameter('componentsDataState', DataAvailabilityValues.AVAILABLE);
      }
    });
    return responses$;
  }

  clearComponents(): void {
    this.componentsStorageService.clear();
  }

  loadFieldsForLogs(): Observable<[LogField[], AuditFieldsDefinitionSet]> {
    const serviceLogsFieldsResponse$: Observable<LogField[]> = this.httpClient.get('serviceLogsFields')
      .filter((response: Response) => response.ok)
      .map((response: Response) => {
        return response.json();
      });
    const auditLogsFieldsResponse$: Observable<AuditFieldsDefinitionSet> = this.httpClient.get('auditLogsFields')
      .filter((response: Response) => response.ok)
      .map((response: Response) => {
        return response.json();
      });
    const responses$: Observable<[LogField[], AuditFieldsDefinitionSet]> = Observable.combineLatest(
      serviceLogsFieldsResponse$, auditLogsFieldsResponse$
    );
    responses$.subscribe(([serviceLogsFieldsResponse, auditLogsFieldsResponse]: [LogField[], AuditFieldsDefinitionSet]) => {
      this.serviceLogsFieldsService.addInstances(serviceLogsFieldsResponse);
      this.auditLogsFieldsService.setParameters(auditLogsFieldsResponse);
    });
    return responses$;
  }

  initOnAuthorization = (isAuthorized): void => {
    if (isAuthorized) {
      this.appStateService.setParameter('baseDataSetState', DataAvailabilityValues.LOADING);
      Observable.combineLatest(
        this.loadClusters(),
        this.loadHosts(),
        this.loadComponents()
      ).first().subscribe(() => {
        this.appStateService.setParameter('baseDataSetState', DataAvailabilityValues.AVAILABLE);
      });
    } else {
      this.clearClusters();
      this.clearHosts();
      this.clearComponents();
    }
  }

  syncAuthorizedStateWithBackend(): Promise<any> {
    const setAuthorization = (isAuthorized: boolean) => {
      this.appStateService.setParameters({
        isAuthorized,
        isInitialLoading: false
      });
    };
    return this.httpClient.get('status').toPromise()
      .then(
        (response: Response) => setAuthorization(response.ok),
        (response: Response) => setAuthorization(false)
      );
  }

  setTranslationService() {
    this.translationService.setDefaultLang('en');
    return this.translationService.use('en').toPromise();
  }

}
