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
import {FormGroup, FormControl} from '@angular/forms';
import {Response} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import 'rxjs/add/observable/combineLatest';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/takeUntil';
import * as moment from 'moment-timezone';
import {HttpClientService} from '@app/services/http-client.service';
import {UtilsService} from '@app/services/utils.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {AuditLogsGraphDataService} from '@app/services/storage/audit-logs-graph-data.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry';
import {
  FilterCondition, TimeUnitListItem, SortingListItem, SearchBoxParameter, SearchBoxParameterTriggered
} from '@app/classes/filtering';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject, LogLevelObject} from '@app/classes/object';
import {LogsType, ScrollType, SortingType} from '@app/classes/string';
import {Tab} from '@app/classes/models/tab';
import {LogField} from '@app/classes/models/log-field';
import {AuditLog} from '@app/classes/models/audit-log';
import {AuditLogField} from '@app/classes/models/audit-log-field';
import {ServiceLog} from '@app/classes/models/service-log';
import {ServiceLogField} from '@app/classes/models/service-log-field';
import {BarGraph} from '@app/classes/models/bar-graph';
import {NodeItem} from '@app/classes/models/node-item';
import {CommonEntry} from '@app/classes/models/common-entry';

@Injectable()
export class LogsContainerService {

  constructor(
    private httpClient: HttpClientService, private utils: UtilsService,
    private tabsStorage: TabsService, private componentsStorage: ComponentsService, private hostsStorage: HostsService,
    private appState: AppStateService, private auditLogsStorage: AuditLogsService,
    private auditLogsGraphStorage: AuditLogsGraphDataService, private auditLogsFieldsStorage: AuditLogsFieldsService,
    private serviceLogsStorage: ServiceLogsService, private serviceLogsFieldsStorage: ServiceLogsFieldsService,
    private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private clustersStorage: ClustersService,
    private serviceLogsTruncatedStorage: ServiceLogsTruncatedService, private appSettings: AppSettingsService
  ) {
    const formItems = Object.keys(this.filters).reduce((currentObject: any, key: string): HomogeneousObject<FormControl> => {
      let formControl = new FormControl(),
        item = {
          [key]: formControl
        };
      formControl.setValue(this.filters[key].defaultSelection);
      return Object.assign(currentObject, item);
    }, {});
    this.filtersForm = new FormGroup(formItems);
    this.loadClusters();
    this.loadComponents();
    this.loadHosts();
    appState.getParameter('activeLog').subscribe((value: ActiveServiceLogEntry | null) => this.activeLog = value);
    appState.getParameter('isServiceLogsFileView').subscribe((value: boolean) => this.isServiceLogsFileView = value);
    appState.getParameter('activeLogsType').subscribe((value: LogsType) => this.activeLogsType = value);
    appSettings.getParameter('timeZone').subscribe((value: string) => this.timeZone = value || this.defaultTimeZone);
    tabsStorage.mapCollection((tab: Tab): Tab => {
      let currentAppState = tab.appState || {};
      const appState = Object.assign({}, currentAppState, {
        activeFilters: this.getFiltersData(tab.appState.activeLogsType)
      });
      return Object.assign({}, tab, {
        appState
      });
    });
    appState.getParameter('activeFilters').subscribe((filters: object): void => {
      this.filtersFormChange.next();
      if (filters) {
        const controls = this.filtersForm.controls;
        Object.keys(controls).forEach((key: string): void => {
          controls[key].setValue(filters.hasOwnProperty(key) ? filters[key] : null);
        });
      }
      this.loadLogs();
      this.filtersForm.valueChanges
        .distinctUntilChanged(this.isFormUnchanged)
        .takeUntil(this.filtersFormChange)
        .subscribe((value): void => {
          this.tabsStorage.mapCollection((tab: Tab): Tab => {
            const currentAppState = tab.appState || {},
              appState = Object.assign({}, currentAppState, tab.isActive ? {
                activeFilters: value
              } : null);
            return Object.assign({}, tab, {
              appState
            });
          });
          this.loadLogs();
      });
    });
    this.auditLogsSource.subscribe((logs: AuditLog[]): void => {
      const userNames = logs.map((log: AuditLog): string => log.reqUser);
      this.utils.pushUniqueValues(
        this.filters.users.options, userNames.map(this.utils.getListItemFromString),
        this.compareFilterOptions
      );
    });
  }

  private readonly paginationOptions: string[] = ['10', '25', '50', '100'];

  readonly logLevels: LogLevelObject[] = [
    {
      name: 'FATAL',
      label: 'levels.fatal',
      color: '#830A0A'
    },
    {
      name: 'ERROR',
      label: 'levels.error',
      color: '#E81D1D'
    },
    {
      name: 'WARN',
      label: 'levels.warn',
      color: '#FF8916'
    },
    {
      name: 'INFO',
      label: 'levels.info',
      color: '#2577B5'
    },
    {
      name: 'DEBUG',
      label: 'levels.debug',
      color: '#65E8FF'
    },
    {
      name: 'TRACE',
      label: 'levels.trace',
      color: '#888'
    },
    {
      name: 'UNKNOWN',
      label: 'levels.unknown',
      color: '#BDBDBD'
    }
  ];

  filters: HomogeneousObject<FilterCondition> = {
    clusters: {
      label: 'filter.clusters',
      options: [],
      defaultSelection: [],
      fieldName: 'cluster'
    },
    timeRange: {
      label: 'logs.duration',
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
              unit: 'y'
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
              unit: 'y'
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
      defaultSelection: {
        value: {
          type: 'LAST',
          unit: 'h',
          interval: 1
        },
        label: 'filter.timeRange.1hr'
      }
    },
    components: {
      label: 'filter.components',
      iconClass: 'fa fa-cubes',
      options: [],
      defaultSelection: [],
      fieldName: 'type'
    },
    levels: {
      label: 'filter.levels',
      iconClass: 'fa fa-sort-amount-asc',
      options: this.logLevels.map((level: LogLevelObject): ListItem => {
        return {
          label: level.label,
          value: level.name
        };
      }),
      defaultSelection: [],
      fieldName: 'level'
    },
    hosts: {
      label: 'filter.hosts',
      iconClass: 'fa fa-server',
      options: [],
      defaultSelection: [],
      fieldName: 'host'
    },
    auditLogsSorting: {
      label: 'sorting.title',
      options: [
        {
          label: 'sorting.time.asc',
          value: {
            key: 'evtTime',
            type: 'asc'
          }
        },
        {
          label: 'sorting.time.desc',
          value: {
            key: 'evtTime',
            type: 'desc'
          }
        }
      ],
      defaultSelection: [
        {
          label: 'sorting.time.desc',
          value: {
            key: 'evtTime',
            type: 'desc'
          }
        }
      ]
    },
    serviceLogsSorting: {
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
      defaultSelection: [
        {
          label: 'sorting.time.desc',
          value: {
            key: 'logtime',
            type: 'desc'
          }
        }
      ]
    },
    pageSize: {
      label: 'pagination.title',
      options: this.paginationOptions.map((option: string): ListItem => {
        return {
          label: option,
          value: option
        }
      }),
      defaultSelection: [
        {
          label: '10',
          value: '10'
        }
      ]
    },
    page: {
      defaultSelection: 0
    },
    query: {
      defaultSelection: []
    },
    users: {
      label: 'filter.users',
      iconClass: 'fa fa-server',
      options: [],
      defaultSelection: [],
      fieldName: 'reqUser'
    },
    isUndoOrRedo: {
      defaultSelection: false
    }
  };

  private readonly filtersMapping = {
    clusters: ['clusters'],
    timeRange: ['to', 'from'],
    components: ['mustBe'],
    levels: ['level'],
    hosts: ['hostList'],
    auditLogsSorting: ['sortType', 'sortBy'],
    serviceLogsSorting: ['sortType', 'sortBy'],
    pageSize: ['pageSize'],
    page: ['page'],
    query: ['includeQuery', 'excludeQuery'],
    users: ['userList']
  };

  private readonly graphFilters = {
    clusters: ['clusters'],
    timeRange: ['to', 'from'],
    components: ['mustBe'],
    levels: ['level'],
    hosts: ['hostList'],
    query: ['includeQuery', 'excludeQuery'],
    users: ['userList']
  };

  readonly customTimeRangeKey: string = 'filter.timeRange.custom';

  readonly topResourcesCount: string = '10';

  readonly topUsersCount: string = '6';

  readonly logsTypeMap = {
    auditLogs: {
      logsModel: this.auditLogsStorage,
      fieldsModel: this.auditLogsFieldsStorage,
      listFilters: ['clusters', 'timeRange', 'auditLogsSorting', 'pageSize', 'page', 'query', 'users'],
      topResourcesFilters: ['clusters', 'timeRange', 'query'],
      graphFilters: ['clusters', 'timeRange', 'query'],
      graphRequestName: 'auditLogsGraph',
      graphModel: this.auditLogsGraphStorage
    },
    serviceLogs: {
      logsModel: this.serviceLogsStorage,
      fieldsModel: this.serviceLogsFieldsStorage,
      listFilters: [
        'clusters', 'timeRange', 'components', 'levels', 'hosts', 'serviceLogsSorting', 'pageSize', 'page', 'query'
      ],
      graphFilters: ['clusters', 'timeRange', 'components', 'levels', 'hosts', 'query'],
      graphRequestName: 'serviceLogsHistogram',
      graphModel: this.serviceLogsHistogramStorage
    }
  };

  private readonly defaultTimeZone = moment.tz.guess();

  readonly queryContextMenuItems: ListItem[] = [
    {
      label: 'logs.addToQuery',
      iconClass: 'fa fa-search-plus',
      value: false // 'isExclude' is false
    },
    {
      label: 'logs.excludeFromQuery',
      iconClass: 'fa fa-search-minus',
      value: true // 'isExclude' is true
    }
  ];

  timeZone: string = this.defaultTimeZone;

  totalCount: number = 0;

  /**
   * A configurable property to indicate the maximum capture time in milliseconds.
   * @type {number}
   * @default 600000 (10 minutes)
   */
  private readonly maximumCaptureTimeLimit: number = 600000;

  isServiceLogsFileView: boolean = false;

  filtersForm: FormGroup;

  activeLog: ActiveServiceLogEntry | null = null;

  activeLogsType: LogsType;

  filtersFormChange: Subject<void> = new Subject();

  private columnsMapper<FieldT extends LogField>(fields: FieldT[]): ListItem[] {
    return fields.filter((field: FieldT): boolean => field.isAvailable).map((field: FieldT): ListItem => {
      return {
        value: field.name,
        label: field.displayName || field.name,
        isChecked: field.isDisplayed
      };
    });
  }

  private logsMapper<LogT extends AuditLog & ServiceLog>(result: [LogT[], ListItem[]]): LogT[] {
    const [logs, fields] = result;
    if (fields.length) {
      const names = fields.map((field: ListItem): string => field.value);
      return logs.map((log: LogT): LogT => {
        return names.reduce((currentObject: object, key: string) => Object.assign(currentObject, {
          [key]: log[key]
        }), {}) as LogT;
      });
    } else {
      return [];
    }
  }

  private auditLogsSource: Observable<AuditLog[]> = this.auditLogsStorage.getAll();

  private serviceLogsSource: Observable<ServiceLog[]> = this.serviceLogsStorage.getAll();

  auditLogsColumns: Observable<ListItem[]> = this.auditLogsFieldsStorage.getAll().map(this.columnsMapper);

  serviceLogsColumns: Observable<ListItem[]> = this.serviceLogsFieldsStorage.getAll().map(this.columnsMapper);

  serviceLogs: Observable<ServiceLog[]> = Observable.combineLatest(
    this.serviceLogsSource, this.serviceLogsColumns
  ).map(this.logsMapper);

  auditLogs: Observable<AuditLog[]> = Observable.combineLatest(
    this.auditLogsSource, this.auditLogsColumns
  ).map(this.logsMapper);

  queryParameterNameChange: Subject<SearchBoxParameterTriggered> = new Subject();

  queryParameterAdd: Subject<SearchBoxParameter> = new Subject();

  private stopTimer: Subject<void> = new Subject();

  private stopAutoRefreshCountdown: Subject<void> = new Subject();

  captureSeconds: number = 0;

  private readonly autoRefreshInterval: number = 30000;

  autoRefreshRemainingSeconds: number = 0;

  private startCaptureTime: number;

  private stopCaptureTime: number;

  topUsersGraphData: HomogeneousObject<HomogeneousObject<number>> = {};

  topResourcesGraphData: HomogeneousObject<HomogeneousObject<number>> = {};

  /**
   * Compares two options list items by values (so that isChecked flags are ignored)
   * @param {ListItem} sourceItem
   * @param {ListItem} newItem
   * @returns {boolean}
   */
  private compareFilterOptions = (sourceItem: ListItem, newItem: ListItem): boolean => {
    return this.utils.isEqual(sourceItem.value, newItem.value);
  };

  private isFormUnchanged = (valueA: object, valueB: object): boolean => {
    const trackedControlNames = this.logsTypeMap[this.activeLogsType].listFilters;
    for (let name of trackedControlNames) {
      if (!this.utils.isEqual(valueA[name], valueB[name])) {
        return false;
      }
    }
    return true;
  };

  loadLogs = (logsType: LogsType = this.activeLogsType): void => {
    this.httpClient.get(logsType, this.getParams('listFilters')).subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        model = this.logsTypeMap[logsType].logsModel;
      model.clear();
      if (jsonResponse) {
        const logs = jsonResponse.logList,
          count = jsonResponse.totalCount || 0;
        if (logs) {
          model.addInstances(logs);
        }
        this.totalCount = count;
      }
    });
    this.httpClient.get(this.logsTypeMap[logsType].graphRequestName, this.getParams('graphFilters')).subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        model = this.logsTypeMap[logsType].graphModel;
      model.clear();
      if (jsonResponse) {
        const graphData = jsonResponse.graphData;
        if (graphData) {
          model.addInstances(graphData);
        }
      }
    });
    if (logsType === 'auditLogs') {
      this.httpClient.get('topAuditLogsResources', this.getParams('topResourcesFilters', {
        field: 'resource'
      }), {
        number: this.topResourcesCount
      }).subscribe((response: Response): void => {
        const jsonResponse = response.json();
        if (jsonResponse) {
          const data = jsonResponse.graphData;
          if (data) {
            this.topResourcesGraphData = this.parseAuditLogsTopData(data);
          }
        }
      });
      this.httpClient.get('topAuditLogsResources', this.getParams('topResourcesFilters', {
        field: 'reqUser'
      }), {
        number: this.topUsersCount
      }).subscribe((response: Response): void => {
        const jsonResponse = response.json();
        if (jsonResponse) {
          const data = jsonResponse.graphData;
          if (data) {
            this.topUsersGraphData = this.parseAuditLogsTopData(data);
          }
        }
      });
    }
  };

  loadLogContext(id: string, hostName: string, componentName: string, scrollType: ScrollType = ''): void {
    const params = {
      id: id,
      host_name: hostName,
      component_name: componentName,
      scrollType: scrollType
    };
    this.httpClient.get('serviceLogsTruncated', params).subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (!scrollType) {
        this.serviceLogsTruncatedStorage.clear();
      }
      if (jsonResponse) {
        const logs = jsonResponse.logList;
        if (logs) {
          if (scrollType === 'before') {
            this.serviceLogsTruncatedStorage.addInstancesToStart(logs);
          } else {
            this.serviceLogsTruncatedStorage.addInstances(logs);
          }
          if (!scrollType) {
            this.appState.setParameters({
              isServiceLogContextView: true,
              activeLog: params
            });
          }
        }
      }
    });
  }

  private parseAuditLogsTopData(data: BarGraph[]): HomogeneousObject<HomogeneousObject<number>> {
    return data.reduce((currentObject: HomogeneousObject<HomogeneousObject<number>>, currentItem: BarGraph): HomogeneousObject<HomogeneousObject<number>> => Object.assign(currentObject, {
      [currentItem.name]: currentItem.dataCount.reduce((currentDataObject: HomogeneousObject<number>, currentDataItem: CommonEntry): HomogeneousObject<number> => {
        return Object.assign(currentDataObject, {
          [currentDataItem.name]: currentDataItem.value
        });
      }, {})
    }), {});
  }

  private getParams(
    filtersMapName: string, additionalParams: HomogeneousObject<string> = {}, logsType: LogsType = this.activeLogsType
  ): HomogeneousObject<string> {
    let params = {};
    this.logsTypeMap[logsType][filtersMapName].forEach((key: string): void => {
      const inputValue = this.filtersForm.getRawValue()[key],
        paramNames = this.filtersMapping[key];
      paramNames.forEach((paramName: string): void => {
        let value;
        const valueGetter = this.valueGetters[paramName] || this.defaultValueGetter;
        if (paramName === 'from') {
          value = valueGetter(inputValue, params['to']);
        } else {
          value = valueGetter(inputValue);
        }
        if (value != null && value !== '') {
          params[paramName] = value;
        }
      });
    }, this);
    return Object.assign({}, params, additionalParams);
  }

  getGraphData(data: BarGraph[], keys?: string[]): HomogeneousObject<HomogeneousObject<number>> {
    let graphData = {};
    data.forEach(type => {
      const name = type.name;
      type.dataCount.forEach(entry => {
        const timeStamp = new Date(entry.name).valueOf();
        if (!graphData[timeStamp]) {
          let initialValue = {};
          if (keys) {
            keys.forEach((key: string) => initialValue[key] = 0);
          }
          graphData[timeStamp] = initialValue;
        }
        graphData[timeStamp][name] = Number(entry.value);
      });
    });
    return graphData;
  }

  loadColumnsNames(): void {
    this.httpClient.get('serviceLogsFields').subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.serviceLogsFieldsStorage.addInstances(this.getColumnsArray(jsonResponse, ServiceLogField));
      }
    });
    this.httpClient.get('auditLogsFields').subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.auditLogsFieldsStorage.addInstances(this.getColumnsArray(jsonResponse, AuditLogField));
      }
    });
  }

  private getColumnsArray(keysObject: any, fieldClass: any): any[] {
    return Object.keys(keysObject).map((key: string): {fieldClass} => new fieldClass(key));
  }

  getStartTimeMoment = (selection: TimeUnitListItem, end: moment.Moment): moment.Moment | undefined => {
    let time;
    const value = selection && selection.value;
    if (value) {
      const endTime = end.clone();
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
        case 'CUSTOM':
          time = value.start;
          break;
        default:
          break;
      }
    }
    return time;
  };

  private getStartTime = (selection: TimeUnitListItem, current: string): string => {
    const startMoment = this.getStartTimeMoment(selection, moment(moment(current).valueOf()));
    return startMoment ? startMoment.toISOString() : '';
  };

  getEndTimeMoment = (selection: TimeUnitListItem): moment.Moment | undefined => {
    let time;
    const value = selection && selection.value;
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
        case 'CUSTOM':
          time = value.end;
          break;
        default:
          break;
      }
    }
    return time;
  };

  private getEndTime = (selection: TimeUnitListItem): string => {
    const endMoment = this.getEndTimeMoment(selection);
    return endMoment ? endMoment.toISOString() : '';
  };

  private getQuery(isExclude: boolean): (value: SearchBoxParameter[]) => string {
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
    }
  }

  private getSortType(selection: SortingListItem[] = []): SortingType {
    return selection[0] && selection[0].value ? selection[0].value.type : 'desc';
  }

  private getSortKey(selection: SortingListItem[] = []): string {
    return selection[0] && selection[0].value ? selection[0].value.key : '';
  }

  private getPage(value: number | undefined): string | undefined {
    return typeof value === 'undefined' ? value : value.toString();
  }

  private defaultValueGetter(selection: ListItem | ListItem[] | null): string {
    if (Array.isArray(selection)) {
      return selection.map((item: ListItem): any => item.value).join(',');
    } else if (selection) {
      return selection.value;
    } else {
      return '';
    }
  }

  private readonly valueGetters = {
    to: this.getEndTime,
    from: this.getStartTime,
    sortType: this.getSortType,
    sortBy: this.getSortKey,
    page: this.getPage,
    includeQuery: this.getQuery(false),
    excludeQuery: this.getQuery(true)
  };

  switchTab(activeTab: Tab): void {
    this.tabsStorage.mapCollection((tab: Tab): Tab => {
      return Object.assign({}, tab, {
        isActive: tab.id === activeTab.id
      });
    });
    this.appState.setParameters(activeTab.appState);
  }

  startCaptureTimer(): void {
    this.startCaptureTime = new Date().valueOf();
    const maxCaptureTimeInSeconds = this.maximumCaptureTimeLimit / 1000;
    Observable.timer(0, 1000).takeUntil(this.stopTimer).subscribe((seconds: number): void => {
      this.captureSeconds = seconds;
      if (this.captureSeconds >= maxCaptureTimeInSeconds) {
        this.stopCaptureTimer();
      }
    });
  }

  stopCaptureTimer(): void {
    const autoRefreshIntervalSeconds = this.autoRefreshInterval / 1000;
    this.stopCaptureTime = new Date().valueOf();
    this.captureSeconds = 0;
    this.stopTimer.next();
    this.setCustomTimeRange(this.startCaptureTime, this.stopCaptureTime);
    Observable.timer(0, 1000).takeUntil(this.stopAutoRefreshCountdown).subscribe((seconds: number): void => {
      this.autoRefreshRemainingSeconds = autoRefreshIntervalSeconds - seconds;
      if (!this.autoRefreshRemainingSeconds) {
        this.stopAutoRefreshCountdown.next();
        this.setCustomTimeRange(this.startCaptureTime, this.stopCaptureTime);
      }
    });
  }

  loadClusters(): Observable<Response> {
    const request = this.httpClient.get('clusters');
    request.subscribe((response: Response): void => {
      const clusterNames = response.json();
      if (clusterNames) {
        this.utils.pushUniqueValues(this.filters.clusters.options, clusterNames.map(this.utils.getListItemFromString));
        this.clustersStorage.addInstances(clusterNames);
      }
    });
    return request;
  }

  loadComponents(): Observable<Response> {
    const request = this.httpClient.get('components');
    request.subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        components = jsonResponse && jsonResponse.vNodeList.map((item): NodeItem => Object.assign(item, {
            value: item.logLevelCount.reduce((currentValue: number, currentItem): number => {
              return currentValue + Number(currentItem.value);
            }, 0)
          }));
      if (components) {
        this.utils.pushUniqueValues(this.filters.components.options, components.map(this.utils.getListItemFromNode));
        this.componentsStorage.addInstances(components);
      }
    });
    return request;
  }

  loadHosts(): Observable<Response> {
    const request = this.httpClient.get('hosts');
    request.subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        hosts = jsonResponse && jsonResponse.vNodeList;
      if (hosts) {
        this.utils.pushUniqueValues(this.filters.hosts.options, hosts.map(this.utils.getListItemFromNode));
        this.hostsStorage.addInstances(hosts);
      }
    });
    return request;
  }

  setCustomTimeRange(startTime: number, endTime: number): void {
    this.filtersForm.controls.timeRange.setValue({
      label: this.customTimeRangeKey,
      value: {
        type: 'CUSTOM',
        start: moment(startTime),
        end: moment(endTime)
      }
    });
  }

  getFiltersData(listType: string): object {
    const itemsList = this.logsTypeMap[listType].listFilters,
      keys = Object.keys(this.filters).filter((key: string): boolean => itemsList.indexOf(key) > -1);
    return keys.reduce((currentObject: object, key: string): object => {
      return Object.assign(currentObject, {
        [key]: this.filters[key].defaultSelection
      });
    }, {});
  }

  isFilterConditionDisplayed(key: string): boolean {
    return this.logsTypeMap[this.activeLogsType].listFilters.indexOf(key) > -1
      && Boolean(this.filtersForm.controls[key]);
  }

  updateSelectedColumns(columnNames: string[], logsType: string): void {
    this.logsTypeMap[logsType].fieldsModel.mapCollection(item => Object.assign({}, item, {
      isDisplayed: columnNames.indexOf(item.name) > -1
    }));
  }

  openServiceLog(log: ServiceLog): void {
    const tab = {
      id: log.id,
      isCloseable: true,
      label: `${log.host} >> ${log.type}`,
      appState: {
        activeLogsType: 'serviceLogs',
        isServiceLogsFileView: true,
        activeLog: {
          id: log.id,
          host_name: log.host,
          component_name: log.type
        },
        activeFilters: Object.assign(this.getFiltersData('serviceLogs'), {
          components: this.filters.components.options.find((option: ListItem): boolean => {
            return option.value === log.type;
          }),
          hosts: this.filters.hosts.options.find((option: ListItem): boolean => {
            return option.value === log.host;
          })
        })
      }
    };
    this.tabsStorage.addInstance(tab);
    this.switchTab(tab);
  }

}
