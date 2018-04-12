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
import {Response} from '@angular/http';

import {Observable} from 'rxjs/Observable';

import {HttpClientService} from '@app/services/http-client.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {Router} from "@angular/router";
import {Subscription} from "rxjs/Subscription";

export const IS_AUTHORIZED_APP_STATE_KEY: string = 'isAuthorized';
export const IS_LOGIN_IN_PROGRESS_APP_STATE_KEY: string = 'isLoginInProgress';

/**
 * This service meant to be a single place where the authorization should happen.
 */
@Injectable()
export class AuthService {

  private subscriptions: Subscription[] = [];

  /**
   * A string set by any service or component (mainly from AuthGuard service) to redirect the application after the
   * authorization done.
   * @type string
   */
  redirectUrl: string;

  constructor(
    private httpClient: HttpClientService,
    private appState: AppStateService,
    private router: Router
  ) {
    this.subscriptions.push(this.appState.getParameter(IS_AUTHORIZED_APP_STATE_KEY).subscribe(
      this.onAppStateIsAuthorizedChanged
    ));
  }

  onAppStateIsAuthorizedChanged = (isAuthorized): void => {
    if (isAuthorized) {
      this.router.navigate([this.redirectUrl || '/']);
      this.redirectUrl = '';
    }
  }
  /**
   * The single entry point to request a login action.
   * @param {string} username
   * @param {string} password
   * @returns {Observable<Response>}
   */
  login(username: string, password: string): Observable<Response> {
    this.setLoginInProgressAppState(true);
    const response$ = this.httpClient.postFormData('login', {
      username: username,
      password: password
    });
    response$.subscribe(
      (resp: Response) => this.onLoginResponse(resp),
      (resp: Response) => this.onLoginError(resp)
    );
    return response$;
  }

  /**
   * The single unique entry point to request a logout action
   * @returns {Observable<boolean | Error>}
   */
  logout(): Observable<Response> {
    const response$ = this.httpClient.get('logout');
    response$.subscribe(
      (resp: Response) => this.onLogoutResponse(resp),
      (resp: Response) => this.onLogoutError(resp)
    );
    return response$;
  }

  /**
   * Set the isLoginInProgress state in AppState. The reason behind create a function for this is that we set this app
   * state from two different places so let's do always the same way.
   * @param {boolean} state the new value of the isLoginInProgress app state.
   */
  private setLoginInProgressAppState(state: boolean) {
    this.appState.setParameter(IS_LOGIN_IN_PROGRESS_APP_STATE_KEY, state);
  }

  /**
   * Set the isAuthorized state in AppState. The reason behind create a function for this is that we set this app
   * state from two different places so let's do always the same way.
   * @param {boolean} state The new value of the isAuthorized app state.
   */
  private setAuthorizedAppState(state: boolean) {
    this.appState.setParameter(IS_AUTHORIZED_APP_STATE_KEY, state);
  }

  /**
   * Handling the login success response. The goal is to set the authorized property of the appState.
   * @param resp
   */
  private onLoginResponse(resp: Response): void {
    this.setLoginInProgressAppState(false);
    if (resp && resp.ok) {
      this.setAuthorizedAppState(resp.ok);
    }
  }

  /**
   * Handling the login error response. The goal is to set the authorized property correctly of the appState.
   * @ToDo decide if we should have a loginError app state.
   * @param {Reponse} resp
   */
  private onLoginError(resp: Response): void {
    this.setLoginInProgressAppState(false);
    this.setAuthorizedAppState(false);
  }

  /**
   * Handling the logout success response. The goal is to set the authorized property of the appState.
   * @param {Response} resp
   */
  private onLogoutResponse(resp: Response): void {
    if (resp && resp.ok) {
      this.setAuthorizedAppState(false);
    }
  }

  /**
   * Handling the logout error response.
   * @ToDo decide if we should create a logoutError app state or not
   * @param {Response} resp
   */
  private onLogoutError(resp: Response): void {}

  /**
   * Simply return with the boolean value of the isAuthorized application state key.
   */
  public isAuthorized(): Observable<boolean> {
    return this.appState.getParameter(IS_AUTHORIZED_APP_STATE_KEY);
  }

}
