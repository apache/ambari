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
import {NgModule, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule, Http} from '@angular/http';
import {InMemoryWebApiModule} from 'angular-in-memory-web-api';
import {mockApiDataService} from './services/mock-api-data.service'
import {AlertModule} from 'ngx-bootstrap';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {StoreModule} from '@ngrx/store';
import {HttpClientService} from '@app/services/http-client.service';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {FilteringService} from '@app/services/filtering.service';

import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {BarGraphsService, barGraphs} from '@app/services/storage/bar-graphs.service';
import {GraphsService, graphs} from '@app/services/storage/graphs.service';
import {NodesService, nodes} from '@app/services/storage/nodes.service';
import {UserConfigsService, userConfigs} from '@app/services/storage/user-configs.service';
import {FiltersService, filters} from '@app/services/storage/filters.service';

import {AppComponent} from '@app/app.component';
import {LoginFormComponent} from '@app/login-form/login-form.component';
import {TopMenuComponent} from '@app/top-menu/top-menu.component';
import {MenuButtonComponent} from '@app/menu-button/menu-button.component';
import {MainContainerComponent} from '@app/main-container/main-container.component';
import {FiltersPanelComponent} from '@app/filters-panel/filters-panel.component';
import {FilterDropdownComponent} from '@app/filter-dropdown/filter-dropdown.component';
import {DropdownListComponent} from '@app/dropdown-list/dropdown-list.component';
import {FilterTextFieldComponent} from '@app/filter-text-field/filter-text-field.component';
import {AccordionPanelComponent} from '@app/accordion-panel/accordion-panel.component';
import {LogsListComponent} from '@app/logs-list/logs-list.component';

export function HttpLoaderFactory(http: Http) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
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
    AccordionPanelComponent,
    LogsListComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    InMemoryWebApiModule.forRoot(mockApiDataService, {
      passThruUnknownUrl: true,
      rootPath: ''
    }),
    AlertModule.forRoot(),
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [Http]
      }
    }),
    StoreModule.provideStore({
      auditLogs,
      serviceLogs,
      barGraphs,
      graphs,
      nodes,
      userConfigs,
      filters
    })
  ],
  providers: [
    HttpClientService,
    ComponentActionsService,
    FilteringService,
    AuditLogsService,
    ServiceLogsService,
    BarGraphsService,
    GraphsService,
    NodesService,
    UserConfigsService,
    FiltersService
  ],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {
}
