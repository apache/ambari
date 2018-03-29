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

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {Http} from '@angular/http';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NotificationsService as Angular2NotificationsService} from 'angular2-notifications/src/notifications.service';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';

import {TranslateService as AppTranslateService} from '@app/services/translate.service';

import {NotificationService} from './services/notification.service';

import {CanDeactivateGuardService} from './services/can-deactivate-guard.service';
import {DisableControlDirective} from './directives/disable-control.directive';

import {DropdownButtonComponent} from './components/dropdown-button/dropdown-button.component';
import {DropdownListComponent} from './components/dropdown-list/dropdown-list.component';
import {FilterDropdownComponent} from './components/filter-dropdown/filter-dropdown.component';
import {ModalComponent} from './components/modal/modal.component';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    BrowserAnimationsModule,
    TranslateModule.forChild({
      loader: {
        provide: TranslateLoader,
        useFactory: AppTranslateService.httpLoaderFactory,
        deps: [Http]
      }
    })
  ],
  declarations: [
    DisableControlDirective,
    DropdownButtonComponent,
    DropdownListComponent,
    FilterDropdownComponent,
    ModalComponent
  ],
  providers: [
    NotificationService,
    CanDeactivateGuardService,
    Angular2NotificationsService
  ],
  exports: [
    DisableControlDirective,
    DropdownButtonComponent,
    DropdownListComponent,
    FilterDropdownComponent,
    ModalComponent
  ]
})
export class SharedModule { }
