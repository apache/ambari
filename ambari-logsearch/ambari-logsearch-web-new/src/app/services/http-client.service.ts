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
import {Observable} from 'rxjs/Observable';
import {Http, XHRBackend, Request, RequestOptions, RequestOptionsArgs, Response} from '@angular/http';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class HttpClientService extends Http {

  constructor(backend: XHRBackend, defaultOptions: RequestOptions) {
    super(backend, defaultOptions);
  }

  private readonly apiPrefix = 'api/v1/';

  private readonly urls = {
    status: 'status',
    auditLogs: 'audit/logs'
  };

  private readonly unauthorizedStatuses = [401, 403, 419];

  isAuthorized: boolean;

  generateUrlString(url: string): string {
    const presetUrl = this.urls[url];
    return presetUrl ? (this.apiPrefix + this.urls[url]) : url;
  }

  generateUrl(url: string | Request): string | Request {
    if (typeof url === 'string') {
      return this.generateUrlString(url);
    }
    if (url instanceof Request) {
      url.url = this.generateUrlString(url.url);
      return url;
    }
  }

  handleError(request: Observable<Response>) {
    request.subscribe(null, (error: any) => {
      if (this.unauthorizedStatuses.indexOf(error.status) > -1) {
        this.isAuthorized = false;
      }
    });
  }

  request(url: string | Request, options?: RequestOptionsArgs):Observable<Response> {
    let req = super.request(this.generateUrl(url), options);
    this.handleError(req);
    return req;
  }

  get(url: string, options?: RequestOptionsArgs):Observable<Response> {
    return super.get(this.generateUrlString(url), options);
  }

  post(url: string, body: any, options?: RequestOptionsArgs):Observable<Response> {
    return super.post(this.generateUrlString(url), body, options);
  }

  put(url: string, body: any, options?: RequestOptionsArgs):Observable<Response> {
    return super.put(this.generateUrlString(url), body, options);
  }

  delete(url: string, options?: RequestOptionsArgs):Observable<Response> {
    return super.delete(this.generateUrlString(url), options);
  }

  patch(url: string, body: any, options?: RequestOptionsArgs):Observable<Response> {
    return super.patch(this.generateUrlString(url), body, options);
  }

  head(url: string, options?: RequestOptionsArgs):Observable<Response> {
    return super.head(this.generateUrlString(url), options);
  }

  options(url: string, options?: RequestOptionsArgs):Observable<Response> {
    return super.options(this.generateUrlString(url), options);
  }

}
