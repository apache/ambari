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

import {FormGroup, FormControl} from '@angular/forms';
import {ListItem} from '@app/classes/list-item';

export interface TimeUnit {
  type: 'CURRENT' | 'LAST' | 'PAST';
  unit: 'ms' | 's' | 'm' | 'h' | 'd' | 'w' | 'M' | 'Y';
  interval?: number;
}

export interface TimeUnitListItem extends ListItem {
  value: TimeUnit;
}

export interface FilterCondition {
  label?: string;
  options?: (ListItem | TimeUnitListItem[])[];
  defaultValue?: string | number | {[key: string]: any};
  defaultLabel?: string;
  iconClass?: string;
}

const paginationOptions: string[] = ['10', '25', '50', '100'];

export const filters: {[key: string]: FilterCondition} = {
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
    options: paginationOptions.map((option: string): ListItem => {
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

export const filtersFormItemsMap: {[key: string]: string[]} = {
  serviceLogs: ['clusters', 'timeRange', 'components', 'levels', 'hosts', 'sorting', 'pageSize', 'page', 'query'],
  auditLogs: ['clusters', 'timeRange', 'sorting', 'pageSize', 'page', 'query'] // TODO add all the required fields
};

export const getFiltersForm = (listType: string): FormGroup => {
  const itemsList = filtersFormItemsMap[listType],
    keys = Object.keys(filters).filter((key: string): boolean => itemsList.indexOf(key) > -1),
    items = keys.reduce((currentObject: any, key: string): any => {
      let formControl = new FormControl(),
        item = {
          [key]: formControl
        };
      formControl.setValue(filters[key].defaultValue);
      return Object.assign(currentObject, item);
    }, {});
  return new FormGroup(items);
};
