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

import {Injectable} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Response} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import 'rxjs/add/operator/takeUntil';
import * as moment from 'moment-timezone';
import {ListItem} from '@app/classes/list-item.class';
import {Node} from '@app/models/node.model';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {HttpClientService} from '@app/services/http-client.service';

@Injectable()
export class FilteringService {

  constructor(private httpClient: HttpClientService, private appSettings: AppSettingsService, private clustersStorage: ClustersService, private componentsStorage: ComponentsService, private hostsStorage: HostsService) {
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
   * @param name {Node}
   * @returns {ListItem}
   */
  private getListItemFromNode(node: Node): ListItem {
    return {
      label: `${node.name} (${node.value})`,
      value: node.name
    };
  }

  private readonly defaultTimeZone = moment.tz.guess();

  private readonly paginationOptions = ['10', '25', '50', '100'];

  timeZone: string = this.defaultTimeZone;

  filters = {
    clusters: {
      label: 'filter.clusters',
      options: [],
      defaultValue: ''
    },
    timeRange: {
      options: [
        [
          {
            label: 'filter.timeRange.7d',
            value: {
              type: 'LAST',
              unit: 'd',
              interval: 7
            }
          },
          {
            label: 'filter.timeRange.30d',
            value: {
              type: 'LAST',
              unit: 'd',
              interval: 30
            }
          },
          {
            label: 'filter.timeRange.60d',
            value: {
              type: 'LAST',
              unit: 'd',
              interval: 60
            }
          },
          {
            label: 'filter.timeRange.90d',
            value: {
              type: 'LAST',
              unit: 'd',
              interval: 90
            }
          },
          {
            label: 'filter.timeRange.6m',
            value: {
              type: 'LAST',
              unit: 'M',
              interval: 6
            }
          },
          {
            label: 'filter.timeRange.1y',
            value: {
              type: 'LAST',
              unit: 'Y',
              interval: 1
            }
          },
          {
            label: 'filter.timeRange.2y',
            value: {
              type: 'LAST',
              unit: 'Y',
              interval: 2
            }
          },
          {
            label: 'filter.timeRange.5y',
            value: {
              type: 'LAST',
              unit: 'Y',
              interval: 5
            }
          }
        ],
        [
          {
            label: 'filter.timeRange.yesterday',
            value: {
              type: 'PAST',
              unit: 'd'
            }
          },
          // TODO implement time range calculation
          /*
          {
            label: 'filter.timeRange.beforeYesterday',
            value: {
              type: 'PAST',
              unit: 'd'
            }
          },
          {
            label: 'filter.timeRange.thisDayLastWeek',
            value: {
              type: 'PAST',
              unit: 'd'
            }
          },
          */
          {
            label: 'filter.timeRange.previousWeek',
            value: {
              type: 'PAST',
              unit: 'w'
            }
          },
          {
            label: 'filter.timeRange.previousMonth',
            value: {
              type: 'PAST',
              unit: 'M'
            }
          },
          {
            label: 'filter.timeRange.previousYear',
            value: {
              type: 'PAST',
              unit: 'Y'
            }
          }
        ],
        [
          {
            label: 'filter.timeRange.today',
            value: {
              type: 'CURRENT',
              unit: 'd'
            }
          },
          {
            label: 'filter.timeRange.thisWeek',
            value: {
              type: 'CURRENT',
              unit: 'w'
            }
          },
          {
            label: 'filter.timeRange.thisMonth',
            value: {
              type: 'CURRENT',
              unit: 'M'
            }
          },
          {
            label: 'filter.timeRange.thisYear',
            value: {
              type: 'CURRENT',
              unit: 'Y'
            }
          }
        ],
        [
          {
            label: 'filter.timeRange.5min',
            value: {
              type: 'LAST',
              unit: 'm',
              interval: 5
            }
          },
          {
            label: 'filter.timeRange.15min',
            value: {
              type: 'LAST',
              unit: 'm',
              interval: 15
            }
          },
          {
            label: 'filter.timeRange.30min',
            value: {
              type: 'LAST',
              unit: 'm',
              interval: 30
            }
          },
          {
            label: 'filter.timeRange.1hr',
            value: {
              type: 'LAST',
              unit: 'h',
              interval: 1
            }
          },
          {
            label: 'filter.timeRange.3hr',
            value: {
              type: 'LAST',
              unit: 'h',
              interval: 3
            }
          },
          {
            label: 'filter.timeRange.6hr',
            value: {
              type: 'LAST',
              unit: 'h',
              interval: 6
            }
          },
          {
            label: 'filter.timeRange.12hr',
            value: {
              type: 'LAST',
              unit: 'h',
              interval: 12
            }
          },
          {
            label: 'filter.timeRange.24hr',
            value: {
              type: 'LAST',
              unit: 'h',
              interval: 24
            }
          },
        ]
      ],
      defaultValue: {
        type: 'LAST',
        unit: 'h',
        interval: 1
      },
      defaultLabel: 'filter.timeRange.1hr'
    },
    components: {
      label: 'filter.components',
      iconClass: 'fa fa-cubes',
      options: [],
      defaultValue: ''
    },
    levels: {
      label: 'filter.levels',
      iconClass: 'fa fa-sort-amount-asc',
      options: [
        {
          label: 'levels.fatal',
          value: 'FATAL'
        },
        {
          label: 'levels.error',
          value: 'ERROR'
        },
        {
          label: 'levels.warn',
          value: 'WARN'
        },
        {
          label: 'levels.info',
          value: 'INFO'
        },
        {
          label: 'levels.debug',
          value: 'DEBUG'
        },
        {
          label: 'levels.trace',
          value: 'TRACE'
        },
        {
          label: 'levels.unknown',
          value: 'UNKNOWN'
        }
      ],
      defaultValue: ''
    },
    hosts: {
      label: 'filter.hosts',
      iconClass: 'fa fa-server',
      options: [],
      defaultValue: ''
    },
    sorting: {
      label: 'sorting.title',
      options: [
        {
          label: 'sorting.time.asc',
          value: {
            key: 'logtime',
            type: 'asc'
          }
        },
        {
          label: 'sorting.time.desc',
          value: {
            key: 'logtime',
            type: 'desc'
          }
        }
      ],
      defaultValue: '',
      defaultLabel: ''
    },
    pageSize: {
      label: 'pagination.title',
      options: this.paginationOptions.map(option => {
        return {
          label: option,
          value: option
        }
      }),
      defaultValue: '10',
      defaultLabel: '10'
    },
    page: {
      defaultValue: 0
    },
    query: {}
  };

  private filtersFormItems = Object.keys(this.filters).reduce((currentObject, key) => {
    let formControl = new FormControl(),
      item = {
        [key]: formControl
      };
    formControl.setValue(this.filters[key].defaultValue);
    return Object.assign(currentObject, item);
  }, {});

  filtersForm = new FormGroup(this.filtersFormItems);

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
    Observable.timer(0, 1000).takeUntil(this.stopTimer).subscribe(seconds => this.captureSeconds = seconds);
  }

  stopCaptureTimer(): void {
    const autoRefreshIntervalSeconds = this.autoRefreshInterval / 1000;
    this.stopCaptureTime = new Date().valueOf();
    this.captureSeconds = 0;
    this.stopTimer.next();
    Observable.timer(0, 1000).takeUntil(this.stopAutoRefreshCountdown).subscribe(seconds => {
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
    this.filtersForm.controls.timeRange.setValue({
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
