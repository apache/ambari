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

import {HttpModule, Http, BrowserXhr, XSRFStrategy, ResponseOptions, XHRBackend} from '@angular/http';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {Injector} from "@angular/core";
import {InMemoryBackendService} from "angular-in-memory-web-api";
import {mockApiDataService} from "@app/services/mock-api-data.service";
import {HttpClientService} from "@app/services/http-client.service";

function HttpLoaderFactory(http: Http) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

export const TranslationModules = [
  HttpModule,
  TranslateModule.forRoot({
    loader: {
      provide: TranslateLoader,
      useFactory: HttpLoaderFactory,
      deps: [Http]
    }
  })
];

export const MockHttpRequestModules = [
  HttpClientService,
  {
    provide: XHRBackend,
    useFactory: getTestXHRBackend,
    deps: [Injector, BrowserXhr, XSRFStrategy, ResponseOptions]
  }
];

export function getTestXHRBackend(injector: Injector, browser: BrowserXhr, xsrf: XSRFStrategy, options: ResponseOptions) {
  return new InMemoryBackendService(
    injector,
    new mockApiDataService(),
    {
      passThruUnknownUrl: true,
      rootPath: ''
    }
  )
}
