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

import {Component, ViewChild, OnInit, OnDestroy} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/finally';
import {Subscription} from 'rxjs/Subscription';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuthService} from '@app/services/auth.service';
import {TranslateService} from '@ngx-translate/core';
import {FormGroup} from '@angular/forms';

@Component({
  selector: 'login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.less']
})
export class LoginFormComponent implements OnInit, OnDestroy {

  username: string;

  password: string;

  isLoginAlertDisplayed: boolean;

  isLoginInProgress$: Observable<boolean> = this.appState.getParameter('isLoginInProgress');

  errorMessage: string;

  @ViewChild('loginForm')
  loginForm: FormGroup;

  subscriptions: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private appState: AppStateService,
    private translateService: TranslateService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.loginForm.valueChanges.subscribe(this.onLoginFormChange)
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  onLoginFormChange = (event) => {
    this.isLoginAlertDisplayed = false;
  }

  private onLoginSuccess = (result: Boolean): void => {
    this.isLoginAlertDisplayed = false;
    this.errorMessage = '';
  }

  private onLoginError = (resp: Boolean): void => {
    this.translateService.get('authorization.error.401').first().subscribe((message: string) => {
      this.errorMessage = message;
      this.isLoginAlertDisplayed = true;
    });
  }

  login() {
    this.authService.login(this.username, this.password).subscribe(this.onLoginSuccess, this.onLoginError);
  }

}
