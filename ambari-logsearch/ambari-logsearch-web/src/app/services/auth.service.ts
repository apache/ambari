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

/**
 * This service meant to be a single place where the authorization should happen.
 */
@Injectable()
export class AuthService {

  constructor(private httpClient: HttpClientService, private appState: AppStateService) {}

  /**
   * The single entry point to request a login action.
   * @param {string} username
   * @param {string} password
   * @returns {Observable<Response>}
   */
  login(username: string, password: string): Observable<Response> {
    this.setLoginInProgressAppState(true);
    let obs = this.httpClient.postFormData('login', {
      username: username,
      password: password
    });
    obs.subscribe(
      (resp: Response) => this.onLoginResponse(resp),
      (resp: Response) => this.onLoginError(resp)
    );
    return obs;
  }

  /**
   * The single unique entry point to request a logout action
   * @returns {Observable<boolean | Error>}
   */
  logout(): Observable<Response> {
    let obs = this.httpClient.get('logout');
    obs.subscribe(
      (resp: Response) => this.onLogoutResponse(resp),
      (resp: Response) => this.onLogoutError(resp)
    );
    return obs;
  }

  /**
   * Set the isLoginInProgress state in AppState. The reason behind create a function for this is that we set this app
   * state from two different places so let's do always the same way.
   * @param {boolean} state the new value of the isLoginInProgress app state.
   */
  private setLoginInProgressAppState(state: boolean) {
    this.appState.setParameter('isLoginInProgress', state);
  }

  /**
   * Set the isAuthorized state in AppState. The reason behind create a function for this is that we set this app
   * state from two different places so let's do always the same way.
   * @param {boolean} state The new value of the isAuthorized app state.
   */
  private setAuthorizedAppState(state: boolean) {
    this.appState.setParameter('isAuthorized', state);
  }

  /**
   * Handling the login success response. The goal is to set the authorized property of the appState.
   * @param resp
   */
  private onLoginResponse(resp: Response): void {
    if (resp && resp.ok) {
      this.setLoginInProgressAppState(false);
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

}
