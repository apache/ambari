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
import {Subject} from 'rxjs/Subject';
import * as moment from 'moment-timezone';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {HttpClientService} from '@app/services/http-client.service';

@Injectable()
export class FilteringService {

  constructor(private httpClient: HttpClientService, private appSettings: AppSettingsService, private clustersStorage: ClustersService, private componentsStorage: ComponentsService, private hostsStorage: HostsService) {
    appSettings.getParameter('timeZone').subscribe(value => this.timeZone = value || this.defaultTimeZone);
    clustersStorage.getAll().subscribe(clusters => {
      this.filters.clusters.options = [...this.filters.clusters.options, ...clusters.map(this.getListItem)];
    });
    componentsStorage.getAll().subscribe(components => {
      this.filters.components.options = [...this.filters.components.options, ...components.map(this.getListItem)];
    });
    hostsStorage.getAll().subscribe(hosts => {
      this.filters.hosts.options = [...this.filters.hosts.options, ...hosts.map(host => {
        return {
          label: `${host.name} (${host.value})`,
          value: host.name
        };
      })];
    });
  }

  private getListItem(name: string): any {
    return {
      label: name,
      value: name
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
    text: {
      label: 'filter.message',
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
          // TODO implement time range calculation
          /*
          {
            label: 'filter.timeRange.todaySoFar',
            value: {
              type: 'CURRENT',
              unit: 'd'
            }
          },
          */
          {
            label: 'filter.timeRange.thisWeek',
            value: {
              type: 'CURRENT',
              unit: 'w'
            }
          },
          // TODO implement time range calculation
          /*
          {
            label: 'filter.timeRange.thisWeekSoFar',
            value: {
              type: 'CURRENT',
              unit: 'w'
            }
          },
          */
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

  loadClusters(): void {
    this.httpClient.get('clusters').subscribe(response => {
      const clusterNames = response.json();
      if (clusterNames) {
        this.clustersStorage.addInstances(clusterNames);
      }
    });
  }

  loadComponents(): void {
    this.httpClient.get('components').subscribe(response => {
      const jsonResponse = response.json(),
        components = jsonResponse && jsonResponse.groupList;
      if (components) {
        const componentNames = components.map(component => component.type);
        this.componentsStorage.addInstances(componentNames);
      }
    });
  }

  loadHosts(): void {
    this.httpClient.get('hosts').subscribe(response => {
      const jsonResponse = response.json(),
        hosts = jsonResponse && jsonResponse.vNodeList;
      if (hosts) {
        this.hostsStorage.addInstances(hosts);
      }
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
    end_time: this.getEndTime,
    start_time: this.getStartTime,
    to: this.getEndTime,
    from: this.getStartTime,
    sortType: value => value && value.type,
    sortBy: value => value && value.key,
    page: value => value == null ? value : value.toString(),
    includeQuery: this.getQuery(false),
    excludeQuery: this.getQuery(true)
  };

}
