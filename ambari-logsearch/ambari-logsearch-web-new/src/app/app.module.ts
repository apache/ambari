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

import {BrowserModule} from '@angular/platform-browser';
import {NgModule, CUSTOM_ELEMENTS_SCHEMA, Injector} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpModule, Http, XHRBackend, BrowserXhr, ResponseOptions, XSRFStrategy} from '@angular/http';
import {InMemoryBackendService} from 'angular-in-memory-web-api';
import {AlertModule} from 'ngx-bootstrap';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {StoreModule} from '@ngrx/store';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';
import {environment} from '../environments/environment';
import {mockApiDataService} from '@app/services/mock-api-data.service'
import {HttpClientService} from '@app/services/http-client.service';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {FilteringService} from '@app/services/filtering.service';

import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {BarGraphsService} from '@app/services/storage/bar-graphs.service';
import {GraphsService} from '@app/services/storage/graphs.service';
import {NodesService} from '@app/services/storage/nodes.service';
import {UserConfigsService} from '@app/services/storage/user-configs.service';
import {FiltersService} from '@app/services/storage/filters.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {reducer} from '@app/services/storage/reducers.service';

import {AppComponent} from '@app/components/app.component';
import {LoginFormComponent} from '@app/components/login-form/login-form.component';
import {TopMenuComponent} from '@app/components/top-menu/top-menu.component';
import {MenuButtonComponent} from '@app/components/menu-button/menu-button.component';
import {MainContainerComponent} from '@app/components/main-container/main-container.component';
import {FiltersPanelComponent} from '@app/components/filters-panel/filters-panel.component';
import {FilterDropdownComponent} from '@app/components/filter-dropdown/filter-dropdown.component';
import {DropdownListComponent} from '@app/components/dropdown-list/dropdown-list.component';
import {FilterTextFieldComponent} from '@app/components/filter-text-field/filter-text-field.component';
import {FilterButtonComponent} from '@app/components/filter-button/filter-button.component';
import {AccordionPanelComponent} from '@app/components/accordion-panel/accordion-panel.component';
import {LogsListComponent} from '@app/components/logs-list/logs-list.component';

export function HttpLoaderFactory(http: Http) {
  // adding 'static' parameter to step over mock data request
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json?static=true');
}

export function getXHRBackend(injector: Injector, browser: BrowserXhr, xsrf: XSRFStrategy, options: ResponseOptions): any {
  if (environment.production) {
    return new XHRBackend(browser, options, xsrf);
  } else {
    return new InMemoryBackendService(
      injector,
      new mockApiDataService(),
      {
        passThruUnknownUrl: true,
        rootPath: ''
      }
    );
  }
}

@NgModule({
  declarations: [
    AppComponent,
    LoginFormComponent,
    TopMenuComponent,
    MenuButtonComponent,
    MainContainerComponent,
    FiltersPanelComponent,
    DropdownListComponent,
    FilterDropdownComponent,
    FilterTextFieldComponent,
    FilterButtonComponent,
    AccordionPanelComponent,
    LogsListComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpModule,
    AlertModule.forRoot(),
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [Http]
      }
    }),
    StoreModule.provideStore(reducer),
    MomentModule,
    MomentTimezoneModule
  ],
  providers: [
    HttpClientService,
    ComponentActionsService,
    FilteringService,
    AppSettingsService,
    AppStateService,
    AuditLogsService,
    ServiceLogsService,
    BarGraphsService,
    GraphsService,
    NodesService,
    UserConfigsService,
    FiltersService,
    ClustersService,
    ComponentsService,
    {
      provide: XHRBackend,
      useFactory: getXHRBackend,
      deps: [Injector, BrowserXhr, XSRFStrategy, ResponseOptions]
    }
  ],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {
}
