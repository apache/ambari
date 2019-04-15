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
import { LogTypeTab } from '@app/classes/models/log-type-tab';

// @ToDo remove duplication, this options are in the LogsContainerService
export const timeRangeFilterOptions = [{
    label: 'filter.timeRange.7d',
    value: {
      type: 'LAST',
      unit: 'd',
      interval: 7
    },
    group: 0
  },
  {
    label: 'filter.timeRange.30d',
    value: {
      type: 'LAST',
      unit: 'd',
      interval: 30
    },
    group: 0
  },
  {
    label: 'filter.timeRange.60d',
    value: {
      type: 'LAST',
      unit: 'd',
      interval: 60
    },
    group: 0
  },
  {
    label: 'filter.timeRange.90d',
    value: {
      type: 'LAST',
      unit: 'd',
      interval: 90
    },
    group: 0
  },
  {
    label: 'filter.timeRange.6m',
    value: {
      type: 'LAST',
      unit: 'M',
      interval: 6
    },
    group: 0
  },
  {
    label: 'filter.timeRange.1y',
    value: {
      type: 'LAST',
      unit: 'y',
      interval: 1
    },
    group: 0
  },
  {
    label: 'filter.timeRange.2y',
    value: {
      type: 'LAST',
      unit: 'y',
      interval: 2
    },
    group: 0
  },
  {
    label: 'filter.timeRange.5y',
    value: {
      type: 'LAST',
      unit: 'y',
      interval: 5
    },
    group: 0
  },
  {
    label: 'filter.timeRange.yesterday',
    value: {
      type: 'PAST',
      unit: 'd'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.previousWeek',
    value: {
      type: 'PAST',
      unit: 'w'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.previousMonth',
    value: {
      type: 'PAST',
      unit: 'M'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.previousYear',
    value: {
      type: 'PAST',
      unit: 'y'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.today',
    value: {
      type: 'CURRENT',
      unit: 'd'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.thisWeek',
    value: {
      type: 'CURRENT',
      unit: 'w'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.thisMonth',
    value: {
      type: 'CURRENT',
      unit: 'M'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.thisYear',
    value: {
      type: 'CURRENT',
      unit: 'y'
    },
    group: 1
  },
  {
    label: 'filter.timeRange.5min',
    value: {
      type: 'LAST',
      unit: 'm',
      interval: 5
    },
    group: 2
  },
  {
    label: 'filter.timeRange.15min',
    value: {
      type: 'LAST',
      unit: 'm',
      interval: 15
    },
    group: 2
  },
  {
    label: 'filter.timeRange.30min',
    value: {
      type: 'LAST',
      unit: 'm',
      interval: 30
    },
    group: 2
  },
  {
    label: 'filter.timeRange.1hr',
    value: {
      type: 'LAST',
      unit: 'h',
      interval: 1
    },
    group: 2
  },
  {
    label: 'filter.timeRange.3hr',
    value: {
      type: 'LAST',
      unit: 'h',
      interval: 3
    },
    group: 2
  },
  {
    label: 'filter.timeRange.6hr',
    value: {
      type: 'LAST',
      unit: 'h',
      interval: 6
    },
    group: 2
  },
  {
    label: 'filter.timeRange.12hr',
    value: {
      type: 'LAST',
      unit: 'h',
      interval: 12
    },
    group: 2
  },
  {
    label: 'filter.timeRange.24hr',
    value: {
      type: 'LAST',
      unit: 'h',
      interval: 24
    },
    group: 2
  }];

@Injectable()
export class LogsFilteringUtilsService {

  readonly defaultFilterSelections = {
    clusters: [],
    timeRange: {
      value: {
        type: 'LAST',
        unit: 'h',
        interval: 1
      },
      label: 'filter.timeRange.1hr'
    },
    components: [],
    levels: [],
    hosts: [],
    auditLogsSorting: {
      label: 'sorting.time.desc',
      value: {
        key: 'evtTime',
        type: 'desc'
      }
    },
    serviceLogsSorting: {
      label: 'sorting.time.desc',
      value: {
        key: 'logtime',
        type: 'desc'
      }
    },
    pageSize: [{
      label: '100',
      value: '100'
    }],
    page: 0,
    query: [],
    users: [],
    isUndoOrRedo: false
  };

  constructor(
    private utilsService: UtilsService
  ) { }

  getTimeRandeOptionsByGroup() {
    return timeRangeFilterOptions.reduce((groups: any, item: any) => {
      const groupItem = {...item};
      delete groupItem.group;
      groups[item.group] = groups[item.group] || [];
      groups[item.group].push(groupItem);
      return groups;
    }, []);
  }

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
            [parameter.name]: parameter.value
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
    return selection && selection[0] && selection[0].value ? selection[0].value.type : 'desc';
  }

  getSortKeyFromSortingListItem(selection: SortingListItem[] = []): string {
    return selection && selection[0] && selection[0].value ? selection[0].value.key : '';
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

  getParamsFromActiveFilter(activeFilter: any, activeLogsType: LogsType): {[key: string]: string} {
    const {...filters} = activeFilter;
    delete filters.isUndoOrRedo;
    return Object.keys(filters).reduce((currentParams, key) => {
      const newParams = {
        ...currentParams
      };
      if (filters[key] !== null && filters[key] !== undefined) {
        switch (key) {
          case 'auditLogsSorting':
          case 'serviceLogsSorting':
            if (`${activeLogsType}Sorting` === key) {
              const item = Array.isArray(filters[key]) ? filters[key][0] : filters[key];
              const itemValue = item && item.value;
              if (itemValue) {
                Object.assign(newParams, {
                  sortingKey: itemValue.key,
                  sortingType: itemValue.type,
                });
              }
            }
            break;
          case 'query' :
            if (filters[key] && Object.keys(filters[key]).length) {
              Object.assign(newParams, {
                [key]: JSON.stringify(filters[key])
              });
            }
            break;
          case 'timeRange' :
            if (filters[key].value) {
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
                  timeRangeUnit: timeRangeValue.unit
                });
                if (timeRangeValue.interval !== undefined) {
                  Object.assign(timeRangeParams, {
                    timeRangeInterval: timeRangeValue.interval
                  });
                }
              }
              Object.assign(newParams, timeRangeParams);
            }
            break;
          default:
            const customMethodName: string = 'get' + (key.charAt(0).toUpperCase()) + key.slice(1);
            const valueGetter: Function = (
              this[customMethodName] || this.defaultValueGetterFromListItem
            );
            const paramValue = valueGetter(filters[key]);
            if (paramValue !== null && paramValue !== undefined && paramValue !== '') {
              Object.assign(newParams, {
                [key]: paramValue
              });
            }
            break;
        }
      }
      return newParams;
    }, {});
  }

  private getListItemsFromListParamValue = (value: string): ListItem[] => {
    return value ? value.split(',').map(this.getListItemFromParamValue) : [];
  }

  private getListItemFromParamValue = (value: string): ListItem => {
    return Object.assign(this.utilsService.getListItemFromString(value), {
      isChecked: true
    });
  }

  getFilterFromParams(params: {[key: string]: string}, activeLogsType: LogsType): {[key: string]: any} {
    const filter: {[key: string]: any} = {};
    const paramsKeys: string[] = Object.keys(params);
    return paramsKeys.reduce((currentFilter, key) => {
      let newFilter = {};
      switch (key) {
        case 'clusters':
        case 'components':
        case 'hosts':
        case 'levels':
        case 'pageSize':
        case 'users':
          newFilter = {
            [key]: this.getListItemsFromListParamValue(params[key])
          };
          break;
        case 'page' :
          newFilter = {
            [key]: parseInt(params[key], 0)
          };
          break;
        case 'timeRangeType':
          const type = params.timeRangeType || 'LAST';
          const interval = params.timeRangeInterval && parseInt(params.timeRangeInterval, 0);
          const unit = params.timeRangeUnit;
          const timeRangeFilterValue: {[key: string]: any} = {type, unit, interval};
          let timeRangeFilterLabel = 'filter.timeRange.';
          const timeRangeOption = timeRangeFilterOptions.find((option: any) => {
            const value = option.value;
            return value.type === type && value.unit === timeRangeFilterValue.unit && value.interval === timeRangeFilterValue.interval;
          });
          if (timeRangeOption) {
            timeRangeFilterLabel = timeRangeOption.label;
          } else if (params.timeRangeType !== 'CUSTOM') {
            Object.assign(timeRangeFilterValue, {
              unit: params.timeRangeUnit,
              interval: parseInt(params.timeRangeInterval, 0)
            });
            timeRangeFilterLabel += `${timeRangeFilterValue.interval}${timeRangeFilterValue.unit}`;
          } else {
            Object.assign(timeRangeFilterValue, {
              start: moment(params.timeRangeStart),
              end: moment(params.timeRangeEnd)
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
            [sortingKey]: {
              label: `sorting.time.${params.sortingType}`,
              value: {
                key: params.sortingKey,
                type: params.sortingType
              }
            }
          };
          break;
        case 'query' :
          newFilter = {
            query: JSON.parse(params[key])
          };
          break;
      }
      return {...currentFilter, ...newFilter};
    }, filter);
  }

  getNavigationForTab(tab: LogTypeTab): any[] {
    const logsType = tab.appState && tab.appState.activeLogsType;
    return [tab.id, this.getParamsFromActiveFilter(tab.activeFilters || {}, logsType)];
  }

}
