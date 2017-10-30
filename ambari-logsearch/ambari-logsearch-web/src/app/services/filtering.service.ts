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

import {Injectable, Input} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Response} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import 'rxjs/add/operator/takeUntil';
import * as moment from 'moment-timezone';
import {ListItem} from '@app/classes/list-item';
import {FilterCondition, filters} from '@app/classes/filtering';
import {Node} from '@app/classes/models/node';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {HttpClientService} from '@app/services/http-client.service';

@Injectable()
export class FilteringService {

  constructor(private httpClient: HttpClientService, private appSettings: AppSettingsService, private clustersStorage: ClustersService, private componentsStorage: ComponentsService, private hostsStorage: HostsService, private appState: AppStateService) {
    this.loadClusters();
    this.loadComponents();
    this.loadHosts();
    appSettings.getParameter('timeZone').subscribe(value => this.timeZone = value || this.defaultTimeZone);
    clustersStorage.getAll().subscribe((clusters: string[]): void => {
      this.filters.clusters.options = [...this.filters.clusters.options, ...clusters.map(this.getListItemFromString)];
    });
    componentsStorage.getAll().subscribe((components: Node[]): void => {
     this.filters.components.options = [...this.filters.components.options, ...components.map(this.getListItemFromNode)];
    });
    hostsStorage.getAll().subscribe((hosts: Node[]): void => {
      this.filters.hosts.options = [...this.filters.hosts.options, ...hosts.map(this.getListItemFromNode)];
    });
    appState.getParameter('activeFiltersForm').subscribe((form: FormGroup) => this.activeFiltersForm = form);
  }

  /**
   * Get instance for dropdown list from string
   * @param name {string}
   * @returns {ListItem}
   */
  private getListItemFromString(name: string): ListItem {
    return {
      label: name,
      value: name
    };
  }

  /**
   * Get instance for dropdown list from Node object
   * @param node {Node}
   * @returns {ListItem}
   */
  private getListItemFromNode(node: Node): ListItem {
    return {
      label: `${node.name} (${node.value})`,
      value: node.name
    };
  }

  private readonly defaultTimeZone = moment.tz.guess();

  timeZone: string = this.defaultTimeZone;

  /**
   * A configurable property to indicate the maximum capture time in milliseconds.
   * @type {number}
   * @default 600000 (10 minutes)
   */
  @Input()
  maximumCaptureTimeLimit: number = 600000;

  filters: {[key: string]: FilterCondition} = Object.assign({}, filters);

  activeFiltersForm: FormGroup;

  queryParameterNameChange: Subject<any> = new Subject();

  queryParameterAdd: Subject<any> = new Subject();

  private stopTimer: Subject<any> = new Subject();

  private stopAutoRefreshCountdown: Subject<any> = new Subject();

  captureSeconds: number = 0;

  private readonly autoRefreshInterval: number = 30000;

  autoRefreshRemainingSeconds: number = 0;

  private startCaptureTime: number;

  private stopCaptureTime: number;

  startCaptureTimer(): void {
    this.startCaptureTime = new Date().valueOf();
    const maxCaptureTimeInSeconds = this.maximumCaptureTimeLimit / 1000;
    Observable.timer(0, 1000).takeUntil(this.stopTimer).subscribe((seconds: number): void => {
      this.captureSeconds = seconds;
      if (this.captureSeconds >= maxCaptureTimeInSeconds) {
        this.stopCaptureTimer();
      }
    });
  }

  stopCaptureTimer(): void {
    const autoRefreshIntervalSeconds = this.autoRefreshInterval / 1000;
    this.stopCaptureTime = new Date().valueOf();
    this.captureSeconds = 0;
    this.stopTimer.next();
    this.setCustomTimeRange(this.startCaptureTime, this.stopCaptureTime);
    Observable.timer(0, 1000).takeUntil(this.stopAutoRefreshCountdown).subscribe((seconds: number): void => {
      this.autoRefreshRemainingSeconds = autoRefreshIntervalSeconds - seconds;
      if (!this.autoRefreshRemainingSeconds) {
        this.stopAutoRefreshCountdown.next();
        this.setCustomTimeRange(this.startCaptureTime, this.stopCaptureTime);
      }
    });
  }

  loadClusters(): void {
    this.httpClient.get('clusters').subscribe((response: Response): void => {
      const clusterNames = response.json();
      if (clusterNames) {
        this.clustersStorage.addInstances(clusterNames);
      }
    });
  }

  loadComponents(): void {
    this.httpClient.get('components').subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        components = jsonResponse && jsonResponse.vNodeList.map((item): Node => Object.assign(item, {
            value: item.logLevelCount.reduce((currentValue: number, currentItem): number => {
              return currentValue + Number(currentItem.value);
            }, 0)
          }));
      if (components) {
        this.componentsStorage.addInstances(components);
      }
    });
  }

  loadHosts(): void {
    this.httpClient.get('hosts').subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        hosts = jsonResponse && jsonResponse.vNodeList;
      if (hosts) {
        this.hostsStorage.addInstances(hosts);
      }
    });
  }

  setCustomTimeRange(startTime: number, endTime: number): void {
    this.activeFiltersForm.controls.timeRange.setValue({
      type: 'CUSTOM',
      start: moment(startTime),
      end: moment(endTime)
    });
  }

  private getStartTime = (value: any, current: string): string => {
    let time;
    if (value) {
      const endTime = moment(moment(current).valueOf());
      switch (value.type) {
        case 'LAST':
          time = endTime.subtract(value.interval, value.unit);
          break;
        case 'CURRENT':
          time = moment().tz(this.timeZone).startOf(value.unit);
          break;
        case 'PAST':
          time = endTime.startOf(value.unit);
          break;
        case 'CUSTOM':
          time = value.start;
          break;
        default:
          break;
      }
    }
    return time ? time.toISOString() : '';
  };

  private getEndTime = (value: any): string => {
    let time;
    if (value) {
      switch (value.type) {
        case 'LAST':
          time = moment();
          break;
        case 'CURRENT':
          time = moment().tz(this.timeZone).endOf(value.unit);
          break;
        case 'PAST':
          time = moment().tz(this.timeZone).startOf(value.unit).millisecond(-1);
          break;
        case 'CUSTOM':
          time = value.end;
          break;
        default:
          break;
      }
    }
    return time ? time.toISOString() : '';
  };

  private getQuery(isExclude: boolean): (value: any[]) => string {
    return (value: any[]): string => {
      let parameters;
      if (value && value.length) {
        parameters = value.filter(item => item.isExclude === isExclude).map(parameter => {
          return {
            [parameter.name]: parameter.value.replace(/\s/g, '+')
          };
        });
      }
      return parameters && parameters.length ? JSON.stringify(parameters) : '';
    }
  }

  readonly valueGetters = {
    to: this.getEndTime,
    from: this.getStartTime,
    sortType: value => value && value.type,
    sortBy: value => value && value.key,
    page: value => value == null ? value : value.toString(),
    includeQuery: this.getQuery(false),
    excludeQuery: this.getQuery(true)
  };

}
