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
import {Response} from '@angular/http';
import 'rxjs/add/operator/finally';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuthService} from '@app/services/auth.service';

@Component({
  selector: 'login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.less']
})
export class LoginFormComponent {

  constructor(private authService: AuthService, private appState: AppStateService) {
    appState.getParameter('isLoginInProgress').subscribe(value => this.isLoginInProgress = value);
  }

  username: string;

  password: string;

  isLoginAlertDisplayed: boolean;

  isLoginInProgress: boolean;

  /**
   * Handling the response from the login action. Actually the goal only to show or hide the login error alert.
   * When it gets error response it shows.
   * @param {Response} resp
   */
  private onLoginError = (resp: Response): void => {
    this.isLoginAlertDisplayed = true;
  };
  /**
   * Handling the response from the login action. Actually the goal only to show or hide the login error alert.
   * When it gets success response it hides.
   * @param {Response} resp
   */
  private onLoginSuccess = (resp: Response): void => {
    this.isLoginAlertDisplayed = false;
  };

  login() {
    this.authService.login(this.username,this.password).subscribe(this.onLoginSuccess, this.onLoginError);
  }

}
