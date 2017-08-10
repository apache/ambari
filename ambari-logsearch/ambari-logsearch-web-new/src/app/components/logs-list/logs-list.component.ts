/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, Input} from '@angular/core';
import {FormGroup} from '@angular/forms';
import 'rxjs/add/operator/map';
import {FilteringService} from '@app/services/filtering.service';

@Component({
  selector: 'logs-list',
  templateUrl: './logs-list.component.html',
  styleUrls: ['./logs-list.component.less']
})
export class LogsListComponent {

  constructor(private filtering: FilteringService) {
  }

  @Input()
  logs: any[] = [];

  @Input()
  totalCount: number = 0;

  timeFormat: string = 'DD/MM/YYYY HH:mm:ss';

  get timeZone(): string {
    return this.filtering.timeZone;
  }

  get filters(): any {
    return this.filtering.filters;
  }
  
  get filtersForm(): FormGroup {
    return this.filtering.filtersForm;
  }

}
