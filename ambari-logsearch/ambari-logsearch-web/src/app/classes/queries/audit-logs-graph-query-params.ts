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

import {QueryParams} from '@app/classes/queries/query-params';

export class AuditLogsGraphQueryParams extends QueryParams {
  constructor(options: AuditLogsGraphQueryParams) {
    let unit;
    const diffTimeStamp = new Date(options.to).valueOf() - new Date(options.from).valueOf();
    switch (true) {
      case diffTimeStamp <= 1000:
        unit = '+100MILLISECOND';
        break;
      case diffTimeStamp <= 30000:
        unit = '+500MILLISECOND';
        break;
      case diffTimeStamp <= 60000:
        unit = '+2SECOND';
        break;
      case diffTimeStamp < 1800000:
        unit = '+1MINUTE';
        break;
      case diffTimeStamp < 7200000:
        unit = '+2MINUTE';
        break;
      case diffTimeStamp < 21600000:
        unit = '+5MINUTE';
        break;
      case diffTimeStamp <= 36000000:
        unit = '+10MINUTE';
        break;
      case diffTimeStamp <= 86400000:
        unit = '+1HOUR';
        break;
      case diffTimeStamp < 1296000000:
        unit = '+8HOUR';
        break;
      case diffTimeStamp <= 7776000000:
        unit = '+1DAY';
        break;
      default:
        unit = '+1MONTH';
        break;
    }
    options.unit = unit;
    super(options);
  }

  from: string;
  to: string;
  unit?: string;
  includeQuery?: string;
  excludeQuery?: string;
}
