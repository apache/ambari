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
import {HomogeneousObject, LogLevelObject} from '@app/classes/object';
import {LevelOverridesConfig, LogIndexFilterComponentConfig} from '@app/classes/settings';
import {LogLevel} from '@app/classes/string';
import {Filter} from '@app/classes/models/filter';
import {LogsContainerService} from '@app/services/logs-container.service';
import {HttpClientService} from '@app/services/http-client.service';
import {UtilsService} from '@app/services/utils.service';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {TranslateService} from '@ngx-translate/core';
import {NotificationService} from '@modules/shared/services/notification.service';

@Injectable()
export class UserSettingsService {

  settingsFormGroup: FormGroup = new FormGroup({
    logIndexFilter: new FormControl()
  });

  currentValues = {
    logIndexFilter: {}
  };

  readonly levelNames = this.logsContainer.logLevels.map((level: LogLevelObject): LogLevel => level.name);

  constructor(
    private logsContainer: LogsContainerService,
    private httpClient: HttpClientService,
    private utils: UtilsService,
    private settingsStorage: AppSettingsService,
    private translateService: TranslateService,
    private notificationService: NotificationService) {
    settingsStorage.getParameter('logIndexFilters').subscribe((filters: HomogeneousObject<HomogeneousObject<Filter>>): void => {
      const configs = this.parseLogIndexFilterObjects(filters);
      this.settingsFormGroup.controls.logIndexFilter.setValue(configs);
    });
  }

  loadIndexFilterConfig(clusterNames: string[]): void {
    let processedRequests: number = 0;
    const allFilters: HomogeneousObject<Filter> = {};
    const totalCount = clusterNames.length;
    clusterNames.forEach((clusterName: string): void => {
      this.httpClient.get('logIndexFilters', null, {
        clusterName
      }).subscribe((response: Response): void => {
        const filters = response.json() && response.json().filter;
        if (filters) {
          Object.assign(allFilters, {
            [clusterName]: filters
          });
          if (++processedRequests === totalCount) {
            this.settingsStorage.setParameter('logIndexFilters', allFilters);
            this.currentValues.logIndexFilter = allFilters;
          }
        }
      });
    });
  }

  handleLogIndexFilterUpdate = (response: Response, cluster?: string): void => {
    const title: string = this.translateService.instant('logIndexFilter.update.title');
    const resultStr: string = response instanceof Response && response.ok ? 'success' : 'failed';
    const data: {[key: string]: any} = response instanceof Response ? response.json() : {};
    const message: string = this.translateService.instant(`logIndexFilter.update.${resultStr}`, {
      message: '',
      cluster: cluster || '',
      ...data
    });
    this.notificationService.addNotification({
      type: resultStr,
      title,
      message
    });
  }

  saveIndexFilterConfig(): void {
    const savedValue = this.currentValues.logIndexFilter;
    const newValue = this.settingsFormGroup.controls.logIndexFilter.value;
    const clusters = Object.keys(newValue);
    const storedValue = {};
    const addResponseHandler = (cluster: string) => {
      return (response: Response) => {
        this.handleLogIndexFilterUpdate(response, cluster);
      };
    };
    clusters.forEach((clusterName: string): void => {
      const savedConfig = savedValue[clusterName],
        newConfig = this.getLogIndexFilterObject(newValue[clusterName]);
      Object.assign(storedValue, {
        [clusterName]: newConfig
      });
      if (!this.utils.isEqual(savedConfig, newConfig)) {
        this.httpClient.put('logIndexFilters', {
          filter: newConfig
        }, null, {
          clusterName
        }).subscribe(addResponseHandler(clusterName), addResponseHandler(clusterName));
      }
    });
    this.settingsStorage.setParameter('logIndexFilters', storedValue);
  }

  /**
   * Convert log index filter data for usage in component
   * @param {HomogeneousObject<HomogeneousObject<Filter>>} filters
   * @returns {HomogeneousObject<LogIndexFilterComponentConfig[]>}
   */
  parseLogIndexFilterObjects(
    filters: HomogeneousObject<HomogeneousObject<Filter>>
  ): HomogeneousObject<LogIndexFilterComponentConfig[]> {
    const levels = this.levelNames;
    return filters ? Object.keys(filters).reduce((
      clustersCurrent: HomogeneousObject<LogIndexFilterComponentConfig[]>, clusterName: string
    ): HomogeneousObject<LogIndexFilterComponentConfig[]> => {
      const clusterConfigs = filters[clusterName],
        clusterParsedObject = Object.keys(clusterConfigs).map((componentName: string) => {
          const componentConfigs = clusterConfigs[componentName],
            levelProperties = levels.reduce((
              levelsCurrent: HomogeneousObject<LevelOverridesConfig>, levelName: LogLevel
            ): LevelOverridesConfig => {
              return Object.assign({}, levelsCurrent, {
                [levelName]: {
                  defaults: componentConfigs.defaultLevels.indexOf(levelName) > -1,
                  overrides: componentConfigs.overrideLevels.indexOf(levelName) > -1
                }
              });
            }, {});
          return Object.assign({
            name: componentName,
            label: componentConfigs.label,
            hasOverrides: false,
            hosts: componentConfigs.hosts.join(),
            expiryTime: componentConfigs.expiryTime
          }, levelProperties);
        });
      return Object.assign({}, clustersCurrent, {
        [clusterName]: clusterParsedObject
      });
    }, {}) : {};
  }

  /**
   * Convert data from log index filter component to format for PUT API call
   * @param configs
   * @returns {HomogeneousObject<Filter>}
   */
  private getLogIndexFilterObject(configs): HomogeneousObject<Filter> {
    const levelNames = this.levelNames;
    return configs.reduce((
      currentObject: HomogeneousObject<Filter>, componentConfig: LogIndexFilterComponentConfig
    ): HomogeneousObject<Filter> => {
      const hosts = componentConfig.hosts;
      return Object.assign({}, currentObject, {
        [componentConfig.name]: {
          defaultLevels: levelNames.filter((levelName: LogLevel): boolean => componentConfig[levelName].defaults),
          expiryTime: componentConfig.expiryTime,
          hosts: hosts ? hosts.split(',') : [],
          label: componentConfig.label,
          overrideLevels: levelNames.filter((levelName: LogLevel): boolean => componentConfig[levelName].overrides)
        }
      });
    }, {});
  }

  setTimeZone(timeZone: string): void {
    this.settingsStorage.setParameter('timeZone', timeZone);
  }

}
