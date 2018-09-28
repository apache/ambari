/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, Input, Output, EventEmitter} from '@angular/core';
import {LogTypeTab} from '@app/classes/models/log-type-tab';
import { LogsFilteringUtilsService } from '@app/services/logs-filtering-utils.service';

export enum TabsSwitchMode {
  Click = 'CLICK',
  RouteSegment = 'ROUTE_SEGMENT',
  RouteFragment= 'ROUTE_FRAGMENT',
  RouteParam = 'ROUTE_PARAM'
};

@Component({
  selector: 'tabs',
  templateUrl: './tabs.component.html',
  styleUrls: ['./tabs.component.less']
})
export class TabsComponent {

  @Input()
  items: LogTypeTab[] = [];

  @Input()
  switchMode: TabsSwitchMode = TabsSwitchMode.Click;

  @Input()
  basePathForRoutingMode: string[];

  @Input()
  paramNameForRouteParamMode: string;

  @Input()
  queryParams: {[key: string]: any};

  @Input()
  queryParamsHandling = 'merge';

  @Output()
  tabSwitched: EventEmitter<LogTypeTab> = new EventEmitter();

  @Output()
  tabClosed: EventEmitter<LogTypeTab[]> = new EventEmitter();

  constructor(
    private logsFilterUtilsService: LogsFilteringUtilsService
  ) {}

  switchTab(tab: LogTypeTab, event?: MouseEvent): void {
    if (event) {
      event.preventDefault();
    }
    this.items.forEach((item: LogTypeTab) => item.isActive = item.id === tab.id);
    this.tabSwitched.emit(tab);
  }

  closeTab(tab: LogTypeTab): void {
    const tabs = this.items,
      tabsCount = tabs.length,
      newActiveTab = tabs[tabsCount - 1] === tab ? tabs[tabsCount - 2] : tabs[tabsCount - 1];
    this.tabClosed.emit([tab, newActiveTab]);
  }

  /**
   * Get a route array for router.navigate.
   * @ToDo it's been called too many times. Check what is this.
   * @param tab {LogTypeTab}
   */
  getRouterLinkForTab(tab: LogTypeTab): (string | {[key: string]: any})[] | string {
    let link: (string | {[key: string]: any})[] | string;
    switch (this.switchMode) {
      case TabsSwitchMode.RouteSegment:
        link = [...this.basePathForRoutingMode, ...this.logsFilterUtilsService.getNavigationForTab(tab)];
        break;
      case TabsSwitchMode.RouteParam:
        link = [...this.basePathForRoutingMode, {
          [this.paramNameForRouteParamMode]: tab.id
        }];
        break;
      case TabsSwitchMode.RouteFragment:
        link = [...this.basePathForRoutingMode];
        break;
      default:
        link = '#';
        break;
    }
    return link;
  }

}
