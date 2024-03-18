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
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {LogTypeTab} from '@app/classes/models/log-type-tab';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class LogsBreadcrumbsResolverService implements Resolve<string[]> {

  constructor(
    private routingUtilService: RoutingUtilsService,
    private tabStoreService: TabsService
  ) { }

  resolve(route: ActivatedRouteSnapshot, routerStateSnapshot: RouterStateSnapshot): Observable<string[]> {
    const activeTabParam: string = this.routingUtilService.getParamFromActivatedRouteSnapshot(route, 'activeTab');
    const breadcrumbs: string[] = ['logs.title'];
    return this.tabStoreService.findInCollection((tab: LogTypeTab) => tab.id === activeTabParam).first().map((tab: LogTypeTab) => {
      breadcrumbs.push(tab.label);
      return breadcrumbs;
    });
  }

}
