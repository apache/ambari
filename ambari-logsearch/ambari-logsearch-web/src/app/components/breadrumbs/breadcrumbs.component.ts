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
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {ActivatedRouteSnapshot, NavigationEnd, Router, RoutesRecognized} from '@angular/router';
import {Title} from '@angular/platform-browser';
import {TranslateService} from '@ngx-translate/core';
import {Observable} from 'rxjs/Observable';

export interface BreadCrumb {
  text: string;
  path: string[];
}

@Component({
  selector: 'breadcrumbs',
  templateUrl: './breadcrumbs.component.html',
  styleUrls: ['./breadcrumbs.component.less']
})
export class BreadcrumbsComponent implements OnInit, OnDestroy {

  private subscriptions: Subscription[] = [];

  private crumbs: BreadCrumb[];

  @Input()
  addRootFirst: boolean = true;

  constructor(
    private router: Router,
    private titleService: Title,
    private translateService: TranslateService
  ) { }

  ngOnInit() {
    this.subscriptions.push(
      this.router.events.filter((event) => event instanceof NavigationEnd).subscribe(this.onNavigationEnd)
    );
    this.onNavigationEnd();
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  getCrumbsFromRouterStateSnapshot(routerStateSnapshot: ActivatedRouteSnapshot): BreadCrumb[] {
    let level = routerStateSnapshot;
    const path: string[] = [];
    const breadcrumbs: BreadCrumb[] = [];
    while (level) {
      if (level.url.length) {
        path.push(
          (level.parent ? '' : '/') // start with trailing slash if this is the root
          + level.url.reduce((url, segment) => url += ('/' + segment.path), '') // build up the url by its segments
        );
        if (level.data.breadcrumbs) {
          let crumbs = level.data.breadcrumbs;
          if (!Array.isArray(crumbs)) {
            crumbs = [crumbs];
          }
          crumbs.forEach(breadcrumbTitle => breadcrumbs.push({
            text: breadcrumbTitle,
            path: path
          }));
        }
      }
      level = level.firstChild;
    }
    return breadcrumbs;
  }

  setPageTite(pageTitle) {
    Observable.combineLatest(
      this.translateService.get('common.title'),
      pageTitle ? this.translateService.get(pageTitle) : Observable.of('')
    ).first().subscribe(([commonTitle, pageTite]) => {
      this.titleService.setTitle(pageTitle ? `${commonTitle} - ${pageTite}` : commonTitle);
    });
  }

  onNavigationEnd = (): void => {
    this.crumbs = this.getCrumbsFromRouterStateSnapshot(this.router.routerState.snapshot.root);
    if (this.crumbs.length) {
      this.setPageTite(this.crumbs[this.crumbs.length - 1].text);
    }
  }

}
