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
import {AppStateService} from '@app/services/storage/app-state.service';
import {Observable} from 'rxjs/Observable';
import {Options} from 'angular2-notifications/src/options.type';
import {notificationIcons} from '@modules/shared/services/notification.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.less', '../modules/shared/notifications.less']
})
export class AppComponent {

  isAuthorized$: Observable<boolean> = this.appState.getParameter('isAuthorized');

  private notificationServiceOptions: Options = {
    timeOut: 5000,
    showProgressBar: true,
    pauseOnHover: true,
    preventLastDuplicates: 'visible',
    theClass: 'app-notification',
    icons: notificationIcons
  };

  constructor(
    private appState: AppStateService
  ) {}

}
