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
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {CollectionModelService} from '@app/classes/models/store';
import {LogsContainerService} from '@app/services/logs-container.service';
import {AuthService} from '@app/services/auth.service';
import {ServiceLog} from '@app/classes/models/service-log';
import {ListItem} from '@app/classes/list-item';

@Injectable()
export class ComponentActionsService {

  constructor(
    private appSettings: AppSettingsService, private tabsStorage: TabsService, private authService: AuthService,
    private logsContainer: LogsContainerService
  ) {
  }

  //TODO implement actions

  undo() {
  }

  redo() {
  }

  refresh(): void {
    this.logsContainer.loadLogs();
  }

  openHistory() {
  }

  copyLog(log: ServiceLog): void {
    if (document.queryCommandSupported('copy')) {
      const text = log.log_message,
        node = document.createElement('textarea');
      node.value = text;
      Object.assign(node.style, {
        position: 'fixed',
        top: '0',
        left: '0',
        width: '1px',
        height: '1px',
        border: 'none',
        outline: 'none',
        boxShadow: 'none',
        backgroundColor: 'transparent',
        padding: '0'
      });
      document.body.appendChild(node);
      node.select();
      if (document.queryCommandEnabled('copy')) {
        document.execCommand('copy');
      } else {
        // TODO open failed alert
      }
      // TODO success alert
      document.body.removeChild(node);
    } else {
      // TODO failed alert
    }
  }

  openLog(log: ServiceLog): void {
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
        activeFilters: Object.assign(this.logsContainer.getFiltersData('serviceLogs'), {
          components: this.logsContainer.filters.components.options.find((option: ListItem): boolean => {
            return option.value === log.type;
          }),
          hosts: this.logsContainer.filters.hosts.options.find((option: ListItem): boolean => {
            return option.value === log.host;
          })
        })
      }
    };
    this.tabsStorage.addInstance(tab);
    this.logsContainer.switchTab(tab);
  }

  openContext(log: ServiceLog): void {
    this.logsContainer.loadLogContext(log.id, log.host, log.type);
  }

  startCapture(): void {
    this.logsContainer.startCaptureTimer();
  }

  stopCapture(): void {
    this.logsContainer.stopCaptureTimer();
  }

  setTimeZone(timeZone: string): void {
    this.appSettings.setParameter('timeZone', timeZone);
  }

  updateSelectedColumns(columnNames: string[], model: CollectionModelService): void {
    model.mapCollection(item => Object.assign({}, item, {
      isDisplayed: columnNames.indexOf(item.name) > -1
    }));
  }

  proceedWithExclude = (item: string): void => this.logsContainer.queryParameterNameChange.next({
    item: {
      value: item
    },
    isExclude: true
  });

  /**
   * Request a login action from the AuthService
   * @param {string} username
   * @param {string} password
   */
  login(username: string, password: string): void {
    this.authService.login(username, password);
  }

  /**
   * Request a logout action from AuthService
   */
  logout(): void {
    this.authService.logout();
  }

}
