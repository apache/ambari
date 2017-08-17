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
import * as moment from 'moment-timezone';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {UtilsService} from '@app/services/utils.service';

@Injectable()
export class FilteringService {

  constructor(private appSettings: AppSettingsService, private clustersStorage: ClustersService, private componentsStorage: ComponentsService, private utils: UtilsService) {
    this.appSettings.getParameter('timeZone').subscribe(value => this.timeZone = value || this.defaultTimeZone);
    this.clustersStorage.getAll().subscribe(clusters => {
      this.filters.clusters.options = [...this.filters.clusters.options, ...clusters.map(this.getListItem)];
    });
    this.componentsStorage.getAll().subscribe(components => {
      this.filters.components.options = [...this.filters.components.options, ...components.map(this.getListItem)];
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
        {
          label: 'filter.timeRange.1hr',
          value: {
            type: 'LAST',
            unit: 'h',
            interval: 1
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
        {
          label: 'filter.timeRange.today',
          value: {
            type: 'CURRENT',
            unit: 'd'
          }
        },
        {
          label: 'filter.timeRange.yesterday',
          value: {
            type: 'PAST',
            unit: 'd'
          }
        },
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
          label: 'filter.timeRange.thisMonth',
          value: {
            type: 'CURRENT',
            unit: 'M'
          }
        },
        {
          label: 'filter.timeRange.lastMonth',
          value: {
            type: 'PAST',
            unit: 'M'
          }
        },
        {
          label: 'filter.timeRange.custom',
          value: {
            type: 'CUSTOM'
          }
        }
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
    sorting: {
      label: 'sorting.title',
      options: [
        {
          label: 'sorting.level.asc',
          value: {
            key: 'level',
            type: 'asc'
          }
        },
        {
          label: 'sorting.level.desc',
          value: {
            key: 'level',
            type: 'desc'
          }
        },
        {
          label: 'sorting.component.asc',
          value: {
            key: 'type',
            type: 'asc'
          }
        },
        {
          label: 'sorting.component.desc',
          value: {
            key: 'type',
            type: 'desc'
          }
        },
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
    }
  };

  timeZoneSelection = {
    options: moment.tz.names().map(zone => {
      // TODO map labels according to actual design requirements
      return {
        label: this.utils.getTimeZoneLabel(zone),
        value: zone
      };
    }),
    defaultValue: this.defaultTimeZone,
    defaultLabel: this.utils.getTimeZoneLabel(this.defaultTimeZone)
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

  private getStartTime(value: any, current: string): string {
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
        default:
          break;
      }
    }
    return time ? time.toISOString() : '';
  }

  private getEndTime(value: any): string {
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
        default:
          break;
      }
    }
    return time ? time.toISOString() : '';
  }

  readonly valueGetters = {
    end_time: this.getEndTime.bind(this),
    start_time: this.getStartTime.bind(this),
    to: this.getEndTime.bind(this),
    from: this.getStartTime.bind(this),
    sortType: value => value && value.type,
    sortBy: value => value && value.key,
    page: value => value == null ? value : value.toString()
  };

}
