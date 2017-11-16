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
import {TranslateService} from '@ngx-translate/core';
import {AppStateService} from '@app/services/storage/app-state.service';
import {HttpClientService} from '@app/services/http-client.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.less']
})

export class AppComponent {

  constructor(private httpClient: HttpClientService, private translate: TranslateService, private appState: AppStateService) {
    appState.getParameter('isAuthorized').subscribe((value: boolean) => this.isAuthorized = value);
    appState.setParameter('isInitialLoading', true);
    httpClient.get('status').subscribe(() => appState.setParameters({
      isAuthorized: true,
      isInitialLoading: false
    }), () => appState.setParameter('isInitialLoading', false));
    translate.setDefaultLang('en');
    translate.use('en');
  }

  isAuthorized: boolean = false;

}
