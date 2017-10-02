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
import 'rxjs/add/operator/finally';
import {HttpClientService} from '@app/services/http-client.service';
import {AppStateService} from '@app/services/storage/app-state.service';

@Component({
  selector: 'login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.less']
})
export class LoginFormComponent {

  constructor(private httpClient: HttpClientService, private appState: AppStateService) {
    appState.getParameter('isLoginInProgress').subscribe(value => this.isLoginInProgress = value);
  }

  username: string;

  password: string;

  isLoginAlertDisplayed: boolean;

  isLoginInProgress: boolean;

  private setIsAuthorized(value: boolean): void {
    this.appState.setParameters({
      isAuthorized: value,
      isLoginInProgress: false
    });
    this.isLoginAlertDisplayed = !value;
  }

  login() {
    this.appState.setParameter('isLoginInProgress', true);
    this.httpClient.postFormData('login', {
      username: this.username,
      password: this.password
    }).subscribe(() => this.setIsAuthorized(true), () => this.setIsAuthorized(false));
  }

}
