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
import {FilterCondition, TimeUnitListItem} from '@app/classes/filtering';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject} from '@app/classes/object';
import {AuthService} from '@app/services/auth.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {Router} from '@angular/router';

@Component({
  selector: 'top-menu',
  templateUrl: './top-menu.component.html',
  styleUrls: ['./top-menu.component.less']
})
export class TopMenuComponent {

  constructor(private authService: AuthService, private logsContainer: LogsContainerService, private router: Router) {}

  get filtersForm(): FormGroup {
    return this.logsContainer.filtersForm;
  };

  get filters(): HomogeneousObject<FilterCondition> {
    return this.logsContainer.filters;
  };

  openSettings = (): void => {};

  /**
   * Request a logout action from AuthService
   */
  logout = (): void => {
    this.authService.logout();
  }

  navigateToShipperConfig = (): void => {
    this.router.navigate(['/shipper']);
  }

  readonly items = [
    {
      iconClass: 'fa fa-user grey',
      hideCaret: true,
      isRightAlign: true,
      subItems: [
        {
          label: 'common.settings',
          onSelect: this.openSettings,
          iconClass: 'fa fa-cog'
        },

        {
          label: 'topMenu.shipperConfiguration',
          onSelect: this.navigateToShipperConfig,
          iconClass: 'fa fa-file-code-o'
        },
        {
          isDivider: true
        },
        {
          label: 'authorization.logout',
          onSelect: this.logout,
          iconClass: 'fa fa-sign-out'
        }
      ]
    }
  ];

  get clusters(): (ListItem | TimeUnitListItem[])[] {
    return this.filters.clusters.options;
  }

  get isClustersFilterDisplayed(): boolean {
    return this.logsContainer.isFilterConditionDisplayed('clusters') && this.clusters.length > 1;
  }

}
