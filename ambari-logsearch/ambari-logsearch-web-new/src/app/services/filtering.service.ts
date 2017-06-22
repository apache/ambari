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
import {Subject} from 'rxjs/Subject';
import * as moment from 'moment-timezone';

@Injectable()
export class FilteringService {

  constructor() {
  }

  // TODO implement loading of real options data
  filters = {
    clusters: {
      label: 'filter.clusters',
      options: [
        {
          label: 'filter.all',
          value: ''
        },
        {
          label: 'cl0',
          value: 'cl0'
        },
        {
          label: 'cl1',
          value: 'cl1'
        },
        {
          label: 'cl2',
          value: 'cl2'
        },
        {
          label: 'cl3',
          value: 'cl3'
        },
        {
          label: 'cl4',
          value: 'cl4'
        }
      ],
      selectedValue: '',
      selectedLabel: '',
      paramName: 'clusters',
    },
    text: {
      label: 'filter.message',
      selectedValue: ''
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
      selectedValue: '',
      selectedLabel: ''
    },
    timeZone: {
      options: moment.tz.names().map(zone => {
        // TODO map labels according to actual design requirements
        return {
          label: `${zone} (${moment.tz(zone).format('Z')})`,
          value: zone
        };
      }),
      selectedValue: '',
      selectedLabel: ''
    },
    components: {
      label: 'filter.components',
      iconClass: 'fa fa-cubes',
      options: [
        {
          label: 'filter.all',
          value: ''
        },
        {
          label: 'ambari_agent',
          value: 'ambari_agent'
        },
        {
          label: 'ams_collector',
          value: 'ams_collector'
        },
        {
          label: 'zookeeper_server',
          value: 'zookeeper_server'
        },
        {
          label: 'zookeeper_client',
          value: 'zookeeper_client'
        }
      ],
      selectedValue: '',
      selectedLabel: ''
    },
    levels: {
      label: 'filter.levels',
      iconClass: 'fa fa-sort-amount-asc',
      options: [
        {
          label: 'filter.all',
          value: ''
        },
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
      selectedValue: '',
      selectedLabel: ''
    }
  };

  readonly valueGetters = {
    end_time: value => {
      let time;
      if (value) {
        switch (value.type) {
          case 'LAST':
            time = moment();
            break;
          case 'CURRENT':
            // TODO consider user's timezone
            time = moment().endOf(value.unit);
            break;
          case 'PAST':
            // TODO consider user's timezone
            time = moment().startOf(value.unit).millisecond(-1);
            break;
          default:
            break;
        }
      }
      return time ? time.toISOString() : '';
    },
    start_time: (value, current) => {
      let time;
      if (value) {
        const endTime = moment(moment(current).valueOf());
        switch (value.type) {
          case 'LAST':
            time = endTime.subtract(value.interval, value.unit);
            break;
          case 'CURRENT':
            // TODO consider user's timezone
            time = moment().startOf(value.unit);
            break;
          case 'PAST':
            // TODO consider user's timezone
            time = endTime.startOf(value.unit);
            break;
          default:
            break;
        }
      }
      return time ? time.toISOString() : '';
    }
  };

  filteringSubject = new Subject();

}
