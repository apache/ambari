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
import * as moment from 'moment-timezone';

@Injectable()
export class FilteringService {

  constructor() {
  }

  // TODO implement loading of real options data
  readonly filters = {
    clusters: {
      label: 'filter.clusters',
      options: [
        {
          label: 'filter.all',
          value: 'ALL'
        },
        {
          label: 'c0',
          value: 'c0'
        },
        {
          label: 'c1',
          value: 'c1'
        }
      ],
      selectedValue: '',
      selectedLabel: ''
    },
    text: {
      label: 'filter.message',
      value: ''
    },
    timeRange: {
      options: [
        {
          label: 'filter.timeRange.1hr',
          value: '1HR'
        },
        {
          label: 'filter.timeRange.24hr',
          value: '24HR'
        },
        {
          label: 'filter.timeRange.today',
          value: 'TODAY'
        },
        {
          label: 'filter.timeRange.yesterday',
          value: 'YESTERDAY'
        },
        {
          label: 'filter.timeRange.7d',
          value: '7D'
        },
        {
          label: 'filter.timeRange.30d',
          value: '30d'
        },
        {
          label: 'filter.timeRange.thisMonth',
          value: 'THIS_MONTH'
        },
        {
          label: 'filter.timeRange.lastMonth',
          value: 'LAST_MONTH'
        },
        {
          label: 'filter.timeRange.custom',
          value: 'CUSTOM'
        }
      ],
      selectedValue: '',
      selectedLabel: ''
    },
    timeZone: {
      options: moment.tz.names().map(zone => {
        return {
          label: zone,
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
          label: 'DataNode',
          value: 'DATANODE'
        },
        {
          label: 'NameNode',
          value: 'NAMENODE'
        },
        {
          label: 'ZooKeeper Server',
          value: 'ZOOKEEPER_SERVER'
        },
        {
          label: 'Metrics Collector',
          value: 'METRICS_COLLECTOR'
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

}
