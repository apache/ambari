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

import {Component, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TakeUntilDestroy} from "angular2-take-until-destroy";
import {Observable} from "rxjs/Observable";
import {Options} from 'angular2-notifications/src/options.type';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.less']
})
export class AppComponent implements OnInit {

  isAuthorized$: Observable<boolean> = this.appState.getParameter('isAuthorized');

  private notificationServiceOptions: Options = {
    timeOut: 5000,
    showProgressBar: true,
    pauseOnHover: true,
    preventLastDuplicates: 'visible'
  };

  constructor(
    private translate: TranslateService,
    private appState: AppStateService
  ) {}

  ngOnInit() {
    this.appState.setParameter('isInitialLoading', true);
    this.translate.setDefaultLang('en');
    this.translate.use('en');
  }

}
