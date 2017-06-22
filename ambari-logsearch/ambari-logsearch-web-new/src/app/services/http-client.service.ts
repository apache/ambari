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
import {AuditLogsQueryParams} from '@app/classes/queries/audit-logs-query-params.class';
import {ServiceLogsQueryParams} from '@app/classes/queries/service-logs-query-params.class';

@Injectable()
export class HttpClientService extends Http {

  constructor(backend: XHRBackend, defaultOptions: RequestOptions) {
    super(backend, defaultOptions);
  }

  private readonly apiPrefix = 'api/v1/';

  private readonly endPoints = {
    status: {
      url: 'status'
    },
    auditLogs: {
      url: 'audit/logs',
      params: opts => new AuditLogsQueryParams(opts)
    },
    serviceLogs: {
      url: 'service/logs',
      params: opts => new ServiceLogsQueryParams(opts)
    }
  };

  private readonly unauthorizedStatuses = [401, 403, 419];

  isAuthorized: boolean;

  generateUrlString(url: string): string {
    const preset = this.endPoints[url];
    return preset ? `${this.apiPrefix}${preset.url}` : url;
  }

  generateUrl(request: string | Request): string | Request {
    if (typeof request === 'string') {
      return this.generateUrlString(request);
    }
    if (request instanceof Request) {
      request.url = this.generateUrlString(request.url);
      return request;
    }
  }

  generateOptions(url: string, params: {[key: string]: string}): RequestOptionsArgs {
    const preset = this.endPoints[url];
    return {
      params: preset && preset.params ? preset.params(params) : params
    };
  }

  handleError(request: Observable<Response>): void {
    request.subscribe(null, (error: any) => {
      if (this.unauthorizedStatuses.indexOf(error.status) > -1) {
        this.isAuthorized = false;
      }
    });
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    let req = super.request(this.generateUrl(url), options);
    this.handleError(req);
    return req;
  }

  get(url, params?: {[key: string]: string}): Observable<Response> {
    return super.get(this.generateUrlString(url), this.generateOptions(url, params));
  }

}
