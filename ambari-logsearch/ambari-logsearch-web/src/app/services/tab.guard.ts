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
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {LogTypeTab} from '@app/classes/models/log-type-tab';
import { LogsFilteringUtilsService } from '@app/services/logs-filtering-utils.service';

@Injectable()
export class TabGuard implements CanActivate {

  constructor (
    private routingUtilsService: RoutingUtilsService,
    private router: Router,
    private tabsStorageService: TabsService,
    private logsFilteringUtilsService: LogsFilteringUtilsService
  ) {}

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    const activeTabParam: string = this.routingUtilsService.getParamFromActivatedRouteSnapshot(state.root, 'activeTab');
    return this.tabsStorageService.getAll().switchMap((tabs: LogTypeTab[]) => {
      if (!activeTabParam && tabs && tabs.length) {
        const tab = tabs.find((currentTab: LogTypeTab) => currentTab.isActive);
        if (tab) {
          this.router.navigate(['/logs', ...this.logsFilteringUtilsService.getNavigationForTab(tab)]);
        }
      }
      const canActivate: boolean = !!activeTabParam && !!tabs.find((tab: LogTypeTab) => tab.id === activeTabParam);
      return Observable.of(canActivate);
    });
  }
}
