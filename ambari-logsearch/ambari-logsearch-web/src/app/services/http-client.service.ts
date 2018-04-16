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
import 'rxjs/add/operator/first';
import 'rxjs/add/observable/throw';
import {
  Http, XHRBackend, Request, RequestOptions, RequestOptionsArgs, Response, Headers, URLSearchParams
} from '@angular/http';
import {HomogeneousObject} from '@app/classes/object';
import {AuditLogsListQueryParams} from '@app/classes/queries/audit-logs-query-params';
import {AuditLogsGraphQueryParams} from '@app/classes/queries/audit-logs-graph-query-params';
import {AuditLogsTopResourcesQueryParams} from '@app/classes/queries/audit-logs-top-resources-query-params';
import {ServiceLogsQueryParams} from '@app/classes/queries/service-logs-query-params';
import {ServiceLogsHistogramQueryParams} from '@app/classes/queries/service-logs-histogram-query-params';
import {ServiceLogsTruncatedQueryParams} from '@app/classes/queries/service-logs-truncated-query-params';
import {AppStateService} from '@app/services/storage/app-state.service';

@Injectable()
export class HttpClientService extends Http {

  private readonly apiPrefix = 'api/v1/';

  private readonly endPoints = {
    status: {
      url: 'status'
    },
    auditLogs: {
      url: 'audit/logs',
      params: opts => new AuditLogsListQueryParams(opts)
    },
    auditLogsGraph: {
      url: 'audit/logs/bargraph',
      params: opts => new AuditLogsGraphQueryParams(opts)
    },
    auditLogsFields: {
      url: 'audit/logs/schema/fields'
    },
    serviceLogs: {
      url: 'service/logs',
      params: opts => new ServiceLogsQueryParams(opts)
    },
    serviceLogsHistogram: {
      url: 'service/logs/histogram',
      params: opts => new ServiceLogsHistogramQueryParams(opts)
    },
    serviceLogsFields: {
      url: 'service/logs/schema/fields'
    },
    serviceLogsTruncated: {
      url: 'service/logs/truncated',
      params: opts => new ServiceLogsTruncatedQueryParams(opts)
    },
    components: {
      url: 'service/logs/components/levels/counts'
    },
    serviceComponentsName: {
      url: 'service/logs/components'
    },
    clusters: {
      url: 'service/logs/clusters'
    },
    hosts: {
      url: 'service/logs/tree'
    },
    topAuditLogsResources: {
      url: variables => `audit/logs/resources/${variables.number}`,
      params: opts => new AuditLogsTopResourcesQueryParams(opts)
    },
    logIndexFilters: {
      url: variables => `shipper/filters/${variables.clusterName}/level`
    },

    shipperClusterServiceList: {
      url: variables => `shipper/input/${variables.cluster}/services`
    },
    shipperClusterServiceConfiguration: {
      url: variables => `shipper/input/${variables.cluster}/services/${variables.service}`
    },
    shipperClusterServiceConfigurationTest: {
      url: variables => `shipper/input/${variables.cluster}/test`
    }
  };

  private readonly unauthorizedStatuses = [401, 403, 419];

  constructor(backend: XHRBackend, defaultOptions: RequestOptions, private appState: AppStateService) {
    super(backend, defaultOptions);
  }

  private generateUrlString(url: string, urlVariables?: HomogeneousObject<string>): string {
    const preset = this.endPoints[url];
    let generatedUrl: string;
    if (preset) {
      const urlExpression = preset.url;
      let path: string;
      if (typeof urlExpression === 'function') {
        path = preset.url(urlVariables);
      } else if (typeof urlExpression === 'string') {
        path = preset.url;
      }
      generatedUrl = `${this.apiPrefix}${path}`;
    } else {
      generatedUrl = url;
    }
    return generatedUrl;
  }

  private generateUrl(request: string | Request): string | Request {
    if (typeof request === 'string') {
      return this.generateUrlString(request);
    }
    if (request instanceof Request) {
      request.url = this.generateUrlString(request.url);
      return request;
    }
  }

  private generateOptions(url: string, params: HomogeneousObject<string>): RequestOptionsArgs {
    const preset = this.endPoints[url],
      rawParams = preset && preset.params ? preset.params(params) : params;
    if (rawParams) {
      const paramsString = Object.keys(rawParams).map((key: string): string => `${key}=${rawParams[key]}`).join('&'),
        urlParams = new URLSearchParams(paramsString, {
          encodeKey: key => key,
          encodeValue: value => encodeURIComponent(value)
        });
      return {
        params: urlParams
      };
    } else {
      return {
        params: rawParams
      };
    }
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    const handleResponseError = (error) => {
      let handled: boolean = false;
      if (this.unauthorizedStatuses.indexOf(error.status) > -1) {
        this.appState.setParameter('isAuthorized', false);
        handled = true;
      }
      return handled;
    };
    const req: Observable<Response> = super.request(this.generateUrl(url), options).first()
      .map(response => response)
      .catch((error: any) => {
        return handleResponseError(error) ? Observable.of(error) : Observable.throw(error);
      });
    return req;
  }

  get(url: string, params?: HomogeneousObject<string>, urlVariables?: HomogeneousObject<string>): Observable<Response> {
    return super.get(this.generateUrlString(url, urlVariables), this.generateOptions(url, params));
  }

  put(url: string, body: any, params?: HomogeneousObject<string>, urlVariables?: HomogeneousObject<string>): Observable<Response> {
    return super.put(this.generateUrlString(url, urlVariables), body, this.generateOptions(url, params));
  }

  post(url: string, body: any, params?: HomogeneousObject<string>, urlVariables?: HomogeneousObject<string>): Observable<Response> {
    return super.post(this.generateUrlString(url, urlVariables), body, this.generateOptions(url, params));
  }

  postFormData(
    url: string,
    params: HomogeneousObject<string>,
    options?: RequestOptionsArgs,
    urlVariables?: HomogeneousObject<string>): Observable<Response> {
    const encodedParams = this.generateOptions(url, params).params;
    let body;
    if (encodedParams && encodedParams instanceof URLSearchParams) {
      body = encodedParams.rawParams;
    }
    const requestOptions = Object.assign({}, options);
    if (!requestOptions.headers) {
      requestOptions.headers = new Headers();
    }
    requestOptions.headers.append('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    return super.post(this.generateUrlString(url, urlVariables), body, requestOptions);
  }

}
