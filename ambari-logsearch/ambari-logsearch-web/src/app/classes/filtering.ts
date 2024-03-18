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

import {Moment, unitOfTime} from 'moment';
import {ListItem} from '@app/classes/list-item';
import {TimeRangeType, SortingType} from '@app/classes/string';

export interface TimeUnit {
  type: TimeRangeType;
  unit: unitOfTime.DurationConstructor;
  interval?: number;
}

export interface CustomTimeRange {
  type: 'CUSTOM';
  start?: Moment;
  end?: Moment;
}

export interface SortingConditions {
  key: string;
  type: SortingType;
}

export interface TimeUnitListItem extends ListItem {
  value: TimeUnit | CustomTimeRange;
}

export interface SortingListItem extends ListItem {
  value: SortingConditions;
}

export interface FilterCondition {
  label?: string;
  options?: (ListItem | TimeUnitListItem[])[];
  defaultSelection?: ListItem | ListItem[] | number | boolean;
  iconClass?: string;
  fieldName?: string;
}

export interface SearchBoxParameter {
  name: string;
  value: string;
  isExclude: boolean;
}

export interface SearchBoxParameterProcessed extends SearchBoxParameter {
  id: number;
  label: string;
}

export interface SearchBoxParameterTriggered {
  item: ListItem;
  isExclude: boolean;
}
