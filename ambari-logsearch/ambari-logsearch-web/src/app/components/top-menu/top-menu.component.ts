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
import {LogsContainerService} from '@app/services/logs-container.service';

@Component({
  selector: 'top-menu',
  templateUrl: './top-menu.component.html',
  styleUrls: ['./top-menu.component.less']
})
export class TopMenuComponent {

  constructor(private logsContainer: LogsContainerService) {
  }

  get filtersForm(): FormGroup {
    return this.logsContainer.filtersForm;
  };

  get filters(): HomogeneousObject<FilterCondition> {
    return this.logsContainer.filters;
  };

  //TODO implement loading of real data into subItems
  readonly items = [
    {
      iconClass: 'fa fa-user grey',
      hideCaret: true,
      isRightAlign: true,
      subItems: [
        {
          label: 'Options'
        },
        {
          label: 'authorization.logout',
          action: 'logout'
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
