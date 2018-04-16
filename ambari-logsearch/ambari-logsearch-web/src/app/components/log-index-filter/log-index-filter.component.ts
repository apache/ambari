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

import {Component, OnInit, Output, EventEmitter, forwardRef, OnDestroy} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {Moment} from 'moment';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject, LogLevelObject} from '@app/classes/object';
import {LogIndexFilterComponentConfig} from '@app/classes/settings';
import {LogLevel} from '@app/classes/string';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UserSettingsService} from '@app/services/user-settings.service';
import {UtilsService} from '@app/services/utils.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'log-index-filter',
  templateUrl: './log-index-filter.component.html',
  styleUrls: ['./log-index-filter.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LogIndexFilterComponent),
      multi: true
    }
  ]
})
export class LogIndexFilterComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Output()
  changeIsSubmitDisabled: EventEmitter<boolean> =  new EventEmitter();

  private onChange: (fn: any) => void;

  readonly columns: LogLevelObject[] = this.logsContainer.logLevels;

  readonly levelNames: LogLevel[] = this.columns.map((level: LogLevelObject): LogLevel => level.name);

  clusters: Observable<string[]> = this.clustersStorage.getAll();

  hosts: Observable<string[]> = this.hostsStorage.getAll();

  clustersListItems: Observable<ListItem[]> = this.clusters.map((clusterNames: string[]): ListItem[] => {
    return clusterNames.map(this.utils.getListItemFromString);
  });

  activeClusterName: string = '';

  private subscriptions: Subscription[] = [];

  /**
   * Configs for all clusters
   */
  private configs: HomogeneousObject<LogIndexFilterComponentConfig[]>;

  constructor(
    private logsContainer: LogsContainerService, private settingsService: UserSettingsService,
    private utils: UtilsService, private clustersStorage: ClustersService, private hostsStorage: HostsService
  ) {
  }

  ngOnInit() {
    this.changeIsSubmitDisabled.emit(true);
    this.subscriptions.push(
      this.clusters.subscribe((clusters: string[]) => this.settingsService.loadIndexFilterConfig(clusters))
    );
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  /**
   * Configs for selected cluster
   * @returns {LogIndexFilterComponentConfig[]}
   */
  get activeClusterConfigs(): LogIndexFilterComponentConfig[] {
    return this.configs[this.activeClusterName];
  }

  /**
   * Select or unselect checkboxes for all log levels for given component
   * @param {string} componentName
   * @param {boolean} isChecked
   * @param {boolean} isOverride - indicates whether levels for override are processed
   */
  processAllLevelsForComponent(componentName: string, isChecked: boolean, isOverride: boolean = false): void {
    const componentConfig = this.getComponentConfigs(componentName),
      key = isOverride ? 'overrides' : 'defaults';
    this.levelNames.forEach((levelName: LogLevel) => componentConfig[levelName][key] = isChecked);
    this.updateValue();
  }

  /**
   * Select or unselect checkboxes for all components for given log level
   * @param {LogLevel} levelName
   * @param {boolean} isChecked
   */
  processAllComponentsForLevel(levelName: LogLevel, isChecked: boolean): void {
    this.activeClusterConfigs.forEach((component: LogIndexFilterComponentConfig): void => {
      component[levelName].defaults = isChecked;
      component[levelName].overrides = isChecked;
    });
    this.updateValue();
  }

  /**
   * Indicates whether all log levels for given component are checked
   * @param {string} componentName
   * @param {string} isOverride - indicates whether levels for override are overviewed
   * @returns {boolean}
   */
  isAllLevelsCheckedForComponent(componentName: string, isOverride: boolean = false): boolean {
    const componentConfig = this.getComponentConfigs(componentName),
      key = isOverride ? 'overrides' : 'defaults';
    return this.levelNames.every((levelName: LogLevel): boolean => componentConfig[levelName][key]);
  }

  /**
   * Indicates whether all components for given log level are checked
   * @param {LogLevel} levelName
   * @returns {boolean}
   */
  isAllComponentsCheckedForLevel(levelName: LogLevel): boolean {
    return this.activeClusterConfigs.every((component: LogIndexFilterComponentConfig): boolean => {
      return component[levelName].defaults;
    });
  }

  setActiveCluster(clusterName: string): void {
    this.activeClusterName = clusterName;
    this.changeIsSubmitDisabled.emit(false);
  }

  getCheckBoxId(componentName: string, levelName: string, isOverride: boolean = false): string {
    return `component_${componentName}_level_${levelName}${isOverride ? '_override' : ''}`;
  }

  setExpiryTime(time: Moment, componentConfig): void {
    componentConfig.expiryTime = time.toISOString();
  }

  private getComponentConfigs(componentName: string) {
    return this.activeClusterConfigs.find((component: LogIndexFilterComponentConfig): boolean => {
      return component.name === componentName;
    });
  }

  writeValue(filters: HomogeneousObject<LogIndexFilterComponentConfig[]>): void {
    this.configs = filters;
    this.updateValue();
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched(): void {
  }

  updateValue(): void {
    if (this.onChange) {
      this.onChange(this.configs);
    }
  }

}
