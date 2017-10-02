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

import {Component, ContentChild, TemplateRef} from '@angular/core';
import {HttpClientService} from '@app/services/http-client.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {AuditLogField} from '@app/models/audit-log-field.model';
import {ServiceLogField} from '@app/models/service-log-field.model';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry.class';

@Component({
  selector: 'main-container',
  templateUrl: './main-container.component.html',
  styleUrls: ['./main-container.component.less']
})
export class MainContainerComponent {

  constructor(private httpClient: HttpClientService, private appState: AppStateService, private auditLogsFieldsStorage: AuditLogsFieldsService, private serviceLogsFieldsStorage: ServiceLogsFieldsService) {
    this.loadColumnsNames();
    appState.getParameter('isAuthorized').subscribe((value: boolean) => this.isAuthorized = value);
    appState.getParameter('isInitialLoading').subscribe((value: boolean) => this.isInitialLoading = value);
    appState.getParameter('isServiceLogsFileView').subscribe((value: boolean) => this.isServiceLogsFileView = value);
    appState.getParameter('activeLog').subscribe((value: ActiveServiceLogEntry | null) => {
      if (value) {
        this.activeLogHostName = value.host_name;
        this.activeLogComponentName = value.component_name;
      } else {
        this.activeLogHostName = '';
        this.activeLogComponentName = '';
      }
    });
  }

  @ContentChild(TemplateRef)
  template;

  isAuthorized: boolean = false;

  isInitialLoading: boolean = false;

  isServiceLogsFileView: boolean = false;

  activeLogHostName: string = '';

  activeLogComponentName: string = '';

  private loadColumnsNames(): void {
    this.httpClient.get('serviceLogsFields').subscribe(response => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.serviceLogsFieldsStorage.addInstances(this.getColumnsArray(jsonResponse, ServiceLogField));
      }
    });
    this.httpClient.get('auditLogsFields').subscribe(response => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.auditLogsFieldsStorage.addInstances(this.getColumnsArray(jsonResponse, AuditLogField));
      }
    });
  }

  private getColumnsArray(keysObject: any, fieldClass: any): any[] {
    return Object.keys(keysObject).map(key => new fieldClass(key));
  }

  closeLog(): void {
    this.appState.setParameters({
      isServiceLogsFileView: false,
      activeLog: null
    });
  }

}
