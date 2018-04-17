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
import {ListItem} from '@app/classes/list-item';
import {CustomTimeRange, SearchBoxParameter, SortingListItem, TimeUnit, TimeUnitListItem} from '@app/classes/filtering';
import * as moment from 'moment-timezone';
import {HomogeneousObject} from '@app/classes/object';
import {LogsType, SortingType} from '@app/classes/string';
import {UtilsService} from '@app/services/utils.service';

// @ToDo remove duplication, this options are in the LogContainerService
const timeRangeFilterOptions = [{
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
      unit: 'y',
      interval: 1
    }
  },
  {
    label: 'filter.timeRange.2y',
    value: {
      type: 'LAST',
      unit: 'y',
      interval: 2
    }
  },
  {
    label: 'filter.timeRange.5y',
    value: {
      type: 'LAST',
      unit: 'y',
      interval: 5
    }
  }, {
    label: 'filter.timeRange.yesterday',
    value: {
      type: 'PAST',
      unit: 'd'
    }
  },
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
      unit: 'y'
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
      unit: 'y'
    }
  }, {
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
  }];

@Injectable()
export class LogsFilteringUtilsService {

  constructor(
    private utilsService: UtilsService
  ) { }

  getStartTimeMomentFromTimeUnitListItem(selection: TimeUnitListItem, end: moment.Moment, timeZone: string): moment.Moment | undefined {
    let time;
    const value = selection && selection.value;
    if (value) {
      const endTime = end.clone();
      switch (value.type) {
        case 'LAST':
          time = endTime.subtract(value.interval, value.unit);
          break;
        case 'CURRENT':
          time = moment().tz(timeZone).startOf(value.unit);
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
    return time;
  }

  getStartTimeFromTimeUnitListItem(selection: TimeUnitListItem, current: string, timeZone: string): string {
    const startMoment = this.getStartTimeMomentFromTimeUnitListItem(selection, moment(moment(current).valueOf()), timeZone);
    return startMoment ? startMoment.toISOString() : '';
  }

  getEndTimeMomentFromTimeUnitListItem(selection: TimeUnitListItem, timeZone: string): moment.Moment | undefined {
    let time;
    const value = selection && selection.value;
    if (value) {
      switch (value.type) {
        case 'LAST':
          time = moment();
          break;
        case 'CURRENT':
          time = moment().tz(timeZone).endOf(value.unit);
          break;
        case 'PAST':
          time = moment().tz(timeZone).startOf(value.unit).millisecond(-1);
          break;
        case 'CUSTOM':
          time = value.end;
          break;
        default:
          break;
      }
    }
    return time;
  }

  getEndTimeFromTimeUnitListItem(selection: TimeUnitListItem, timeZone: string): string {
    const endMoment = this.getEndTimeMomentFromTimeUnitListItem(selection, timeZone);
    return endMoment ? endMoment.toISOString() : '';
  }

  getQuery(isExclude: boolean): (value: SearchBoxParameter[]) => string {
    return (value: SearchBoxParameter[]): string => {
      let parameters;
      if (value && value.length) {
        parameters = value.filter((item: SearchBoxParameter): boolean => {
          return item.isExclude === isExclude;
        }).map((parameter: SearchBoxParameter): HomogeneousObject<string> => {
          return {
            [parameter.name]: parameter.value.replace(/\s/g, '+')
          };
        });
      }
      return parameters && parameters.length ? JSON.stringify(parameters) : '';
    };
  }

  getIncludeQuery(value: SearchBoxParameter[]) {
    return this.getQuery(false)(value);
  }

  getExcludeQuery(value: SearchBoxParameter[]) {
    return this.getQuery(true)(value);
  }

  getSortTypeFromSortingListItem(selection: SortingListItem[] = []): SortingType {
    return selection[0] && selection[0].value ? selection[0].value.type : 'desc';
  }

  getSortKeyFromSortingListItem(selection: SortingListItem[] = []): string {
    return selection[0] && selection[0].value ? selection[0].value.key : '';
  }

  getPage(value: number | undefined): string | undefined {
    return typeof value === 'undefined' ? value : value.toString();
  }

  defaultValueGetterFromListItem(selection: ListItem | ListItem[] | null): string {
    if (Array.isArray(selection)) {
      return selection.map((item: ListItem): any => item.value).join(',');
    } else if (selection) {
      return selection.value;
    } else {
      return '';
    }
  }

  getQueryParamsFromActiveFilter(activeFilter: any, activeLogsType: LogsType): {[key: string]: string} {
    const {...filters} = activeFilter;
    delete filters.isUndoOrRedo;
    return Object.keys(filters).reduce((currentParams, key) => {
      const newParams = {
        ...currentParams
      };
      switch (key) {
        case 'auditLogsSorting':
        case 'serviceLogsSorting':
          if (`${activeLogsType}Sorting` === key) {
            const item = Array.isArray(filters[key]) ? filters[key][0] : filters[key];
            const itemValue = item && item.value;
            Object.assign(newParams, {
              sortingKey: itemValue.key,
              sortingType: itemValue.type,
            });
          }
          break;
        case 'query' :
          Object.assign(newParams, {
            [key]: JSON.stringify(filters[key])
          });
          break;
        case 'timeRange' :
          const timeRangeValue: TimeUnit | CustomTimeRange = filters[key].value;
          const timeRangeParams: {[key: string]: string} = {
            timeRangeType: timeRangeValue.type
          };
          if (timeRangeValue.type === 'CUSTOM') {
            Object.assign(timeRangeParams, {
              timeRangeStart: timeRangeValue.start.toISOString(),
              timeRangeEnd: timeRangeValue.end.toISOString()
            });
          } else {
            Object.assign(timeRangeParams, {
              timeRangeInterval: timeRangeValue.interval,
              timeRangeUnit: timeRangeValue.unit
            });
          }
          Object.assign(newParams, timeRangeParams);
          break;
        default:
          const customMethodName: string = 'get' + (key.charAt(0).toUpperCase()) + key.slice(1);
          const valueGetter: Function = (
            this[customMethodName] || this.defaultValueGetterFromListItem
          );
          const paramValue = valueGetter(filters[key]);
            Object.assign(newParams, {
              ...newParams,
              [key]: paramValue
            });
          break;
      }
      return newParams;
    }, {});
  }

  private getListItemsFromListQueryParamValue = (value: string): ListItem[] => {
    return value ? value.split(',').map(this.getListItemFromQueryParamValue) : [];
  }

  private getListItemFromQueryParamValue = (value: string): ListItem => {
    return Object.assign(this.utilsService.getListItemFromString(value), {
      isChecked: true
    });
  }

  getFilterFromQueryParams(queryParams: {[key: string]: string}, activeLogsType: LogsType): {[key: string]: any} {
    const filter: {[key: string]: any} = {};
    const queryParamsKeys: string[] = Object.keys(queryParams);
    return queryParamsKeys.reduce((currentFilter, key) => {
      let newFilter = {};
      switch (key) {
        case 'clusters':
        case 'components':
        case 'hosts':
        case 'levels':
        case 'pageSize':
        case 'users':
          newFilter = {
            [key]: this.getListItemsFromListQueryParamValue(queryParams[key])
          };
          break;
        case 'page' :
          newFilter = {
            [key]: parseInt(queryParams[key], 0)
          };
          break;
        case 'timeRangeType':
          const type = queryParams.timeRangeType || 'LAST';
          const interval = parseInt(queryParams.timeRangeInterval, 0);
          const unit = queryParams.timeRangeUnit;
          const timeRangeFilterValue: {[key: string]: any} = {type, unit, interval};
          let timeRangeFilterLabel = 'filter.timeRange.';
          const timeRangeOption = timeRangeFilterOptions.find((option: any) => {
            const value = option.value;
            return value.type === type && value.unit === timeRangeFilterValue.unit && value.interval === timeRangeFilterValue.interval;
          });
          if (timeRangeOption) {
            timeRangeFilterLabel = timeRangeOption.label;
          } else if (queryParams.timeRangeType !== 'CUSTOM') {
            Object.assign(timeRangeFilterValue, {
              unit: queryParams.timeRangeUnit,
              interval: parseInt(queryParams.timeRangeInterval, 0)
            });
            timeRangeFilterLabel += `${timeRangeFilterValue.interval}${timeRangeFilterValue.unit}`;
          } else {
            Object.assign(timeRangeFilterValue, {
              start: moment(queryParams.timeRangeStart),
              end: moment(queryParams.timeRangeEnd)
            });
            timeRangeFilterLabel += 'custom';
          }
          newFilter = {
            timeRange: {
              label: timeRangeFilterLabel,
              value: timeRangeFilterValue
            }
          };
          break;
        case 'sortingKey' :
          const sortingKey = `${activeLogsType}Sorting`;
          newFilter = {
            [sortingKey]: [{
              label: `sorting.time.${queryParams.sortingType}`,
              value: {
                key: queryParams.sortingKey,
                type: queryParams.sortingType
              }
            }]
          }
          break;
        case 'query' :
          newFilter = {
            query: JSON.parse(queryParams[key])
          };
          break;
      }
      return {...currentFilter, ...newFilter};
    }, filter);
  }

}
