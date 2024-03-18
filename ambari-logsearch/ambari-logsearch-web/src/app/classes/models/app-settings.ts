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

import * as moment from 'moment-timezone';
import {HomogeneousObject} from '@app/classes/object';
import {Filter} from '@app/classes/models/filter';

export interface AppSettings {
  timeZone: string;
  logIndexFilters: HomogeneousObject<HomogeneousObject<Filter>>;
}

export const defaultSettings: AppSettings = {
  timeZone: moment.tz.guess(),
  logIndexFilters: {}
};
