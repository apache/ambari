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
import {TypeaheadModule} from 'ngx-bootstrap';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {StoreModule} from '@ngrx/store';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';

import {environment} from '@envs/environment';

import {ServiceInjector} from '@app/classes/service-injector';

import {mockApiDataService} from '@app/services/mock-api-data.service'
import {HttpClientService} from '@app/services/http-client.service';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {UtilsService} from '@app/services/utils.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ComponentGeneratorService} from '@app/services/component-generator.service';

import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {GraphsService} from '@app/services/storage/graphs.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {UserConfigsService} from '@app/services/storage/user-configs.service';
import {FiltersService} from '@app/services/storage/filters.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {AuthService} from '@app/services/auth.service';
import {reducer} from '@app/services/storage/reducers.service';

import {AppComponent} from '@app/components/app.component';
import {LoginFormComponent} from '@app/components/login-form/login-form.component';
import {TopMenuComponent} from '@app/components/top-menu/top-menu.component';
import {MenuButtonComponent} from '@app/components/menu-button/menu-button.component';
import {MainContainerComponent} from '@app/components/main-container/main-container.component';
import {FiltersPanelComponent} from '@app/components/filters-panel/filters-panel.component';
import {FilterDropdownComponent} from '@app/components/filter-dropdown/filter-dropdown.component';
import {DropdownListComponent} from '@app/components/dropdown-list/dropdown-list.component';
import {FilterButtonComponent} from '@app/components/filter-button/filter-button.component';
import {AccordionPanelComponent} from '@app/components/accordion-panel/accordion-panel.component';
import {CollapsiblePanelComponent} from '@app/components/collapsible-panel/collapsible-panel.component';
import {LogMessageComponent} from '@app/components/log-message/log-message.component';
import {LogLevelComponent} from '@app/components/log-level/log-level.component';
import {DropdownButtonComponent} from '@app/components/dropdown-button/dropdown-button.component';
import {PaginationComponent} from '@app/components/pagination/pagination.component';
import {PaginationControlsComponent} from '@app/components/pagination-controls/pagination-controls.component';
import {TimeHistogramComponent} from '@app/components/time-histogram/time-histogram.component';
import {LogsContainerComponent} from '@app/components/logs-container/logs-container.component';
import {ActionMenuComponent} from "@app/components/action-menu/action-menu.component";
import {ModalComponent} from '@app/components/modal/modal.component';
import {TimeZonePickerComponent} from '@app/components/timezone-picker/timezone-picker.component';
import {NodeBarComponent} from '@app/components/node-bar/node-bar.component';
import {SearchBoxComponent} from '@app/components/search-box/search-box.component';
import {TimeRangePickerComponent} from '@app/components/time-range-picker/time-range-picker.component';
import {DatePickerComponent} from '@app/components/date-picker/date-picker.component';
import {LogContextComponent} from '@app/components/log-context/log-context.component';
import {LogFileEntryComponent} from '@app/components/log-file-entry/log-file-entry.component';
import {TabsComponent} from '@app/components/tabs/tabs.component';
import {ServiceLogsTableComponent} from '@app/components/service-logs-table/service-logs-table.component';
import {AuditLogsTableComponent} from '@app/components/audit-logs-table/audit-logs-table.component';
import {AuditLogsEntriesComponent} from '@app/components/audit-logs-entries/audit-logs-entries.component';
import {GraphLegendComponent} from '@app/components/graph-legend/graph-legend.component';
import {HorizontalHistogramComponent} from '@app/components/horizontal-histogram/horizontal-histogram.component';
import {GraphTooltipComponent} from '@app/components/graph-tooltip/graph-tooltip.component';
import {GraphLegendItemComponent} from '@app/components/graph-legend-item/graph-legend-item.component';

import {TimeZoneAbbrPipe} from '@app/pipes/timezone-abbr.pipe';
import {TimerSecondsPipe} from '@app/pipes/timer-seconds.pipe';

export function HttpLoaderFactory(http: Http): TranslateHttpLoader {
  // adding 'static' parameter to step over mock data request
  return new TranslateHttpLoader(http, 'resources/assets/i18n/', '.json?static=true');
}

export function getXHRBackend(injector: Injector, browser: BrowserXhr, xsrf: XSRFStrategy, options: ResponseOptions): XHRBackend | InMemoryBackendService {
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
    FilterButtonComponent,
    AccordionPanelComponent,
    CollapsiblePanelComponent,
    LogLevelComponent,
    LogMessageComponent,
    DropdownButtonComponent,
    PaginationComponent,
    PaginationControlsComponent,
    TimeHistogramComponent,
    LogsContainerComponent,
    ActionMenuComponent,
    ModalComponent,
    TimeZonePickerComponent,
    NodeBarComponent,
    SearchBoxComponent,
    TimeRangePickerComponent,
    DatePickerComponent,
    LogContextComponent,
    LogFileEntryComponent,
    TabsComponent,
    ServiceLogsTableComponent,
    AuditLogsTableComponent,
    AuditLogsEntriesComponent,
    GraphLegendComponent,
    HorizontalHistogramComponent,
    GraphTooltipComponent,
    GraphLegendItemComponent,
    TimeZoneAbbrPipe,
    TimerSecondsPipe
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpModule,
    TypeaheadModule.forRoot(),
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
    UtilsService,
    LogsContainerService,
    ComponentGeneratorService,
    AppSettingsService,
    AppStateService,
    AuditLogsService,
    ServiceLogsService,
    ServiceLogsHistogramDataService,
    ServiceLogsTruncatedService,
    GraphsService,
    HostsService,
    UserConfigsService,
    FiltersService,
    ClustersService,
    ComponentsService,
    ServiceLogsFieldsService,
    AuditLogsFieldsService,
    TabsService,
    {
      provide: XHRBackend,
      useFactory: getXHRBackend,
      deps: [Injector, BrowserXhr, XSRFStrategy, ResponseOptions]
    },
    AuthService
  ],
  bootstrap: [AppComponent],
  entryComponents: [NodeBarComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {
  constructor(private injector: Injector) {
    ServiceInjector.injector = this.injector;
  }
}
