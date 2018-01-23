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
import {SortingType} from '@app/classes/string';

export const defaultParams = {
  page: '0',
  pageSize: '10'
};

export class AuditLogsQueryParams extends QueryParams {
  clusters?: string;
  mustBe?: string;
  mustNot?: string;
  includeQuery?: string;
  excludeQuery?: string;
  from?: string;
  to?: string;
}

export class AuditLogsListQueryParams extends AuditLogsQueryParams {
  constructor(options: AuditLogsListQueryParams) {
    let finalParams = Object.assign({}, defaultParams, options);
    const page = parseInt(finalParams.page),
      pageSize = parseInt(finalParams.pageSize);
    finalParams.startIndex = isNaN(page) || isNaN(pageSize) ? '' : (page * pageSize).toString();
    super(finalParams);
  }
  page: string;
  pageSize: string;
  startIndex: string;
  sortBy?: string;
  sortType?: SortingType;
}
