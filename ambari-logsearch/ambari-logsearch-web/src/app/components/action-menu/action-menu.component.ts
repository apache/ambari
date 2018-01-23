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

import {Component} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UserSettingsService} from '@app/services/user-settings.service';

@Component({
  selector: 'action-menu',
  templateUrl: './action-menu.component.html',
  styleUrls: ['./action-menu.component.less']
})
export class ActionMenuComponent {

  constructor(private logsContainer: LogsContainerService, private settings: UserSettingsService) {
  }

  undo() {
  }

  redo() {
  }

  openHistory() {
  }

  openLogIndexFilter = (): void => {
    this.isLogIndexFilterDisplayed = true;
  };

  closeLogIndexFilter(): void {
    this.isLogIndexFilterDisplayed = false;
  }

  saveLogIndexFilter(): void {
    this.isLogIndexFilterDisplayed = false;
    this.settings.saveIndexFilterConfig();
  }

  refresh = (): void => {
    this.logsContainer.loadLogs();
  };

  //TODO implement history items
  readonly items = [
    {
      iconClass: 'fa fa-arrow-left',
      label: 'topMenu.undo',
      onClick: this.undo,
      subItems: [
        {
          label: 'Apply \'Last week\' filter'
        },
        {
          label: 'Clear all filters'
        },
        {
          label: 'Apply \'HDFS\' filter'
        },
        {
          label: 'Apply \'Errors\' filter'
        }
      ]
    },
    {
      iconClass: 'fa fa-arrow-right',
      label: 'topMenu.redo',
      onClick: this.redo,
      subItems: [
        {
          label: 'Apply \'Warnings\' filter'
        },
        {
          label: 'Switch to graph mode'
        },
        {
          label: 'Apply \'Custom Date\' filter'
        }
      ]
    },
    {
      iconClass: 'fa fa-history',
      label: 'topMenu.history',
      onClick: this.openHistory,
      isRightAlign: true,
      subItems: [
        {
          label: 'Apply \'Custom Date\' filter'
        },
        {
          label: 'Switch to graph mode'
        },
        {
          label: 'Apply \'Warnings\' filter'
        },
        {
          label: 'Apply \'Last week\' filter'
        },
        {
          label: 'Clear all filters'
        },
        {
          label: 'Apply \'HDFS\' filter'
        },
        {
          label: 'Apply \'Errors\' filter'
        }
      ]
    },
    {
      iconClass: 'fa fa-filter',
      label: 'topMenu.filter',
      onClick: this.openLogIndexFilter
    },
    {
      iconClass: 'fa fa-refresh',
      label: 'topMenu.refresh',
      onClick: this.refresh
    }
  ];

  isLogIndexFilterDisplayed: boolean = false;

  settingsForm: FormGroup = this.settings.settingsFormGroup;

}
