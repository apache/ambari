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

import {Component, Input, Output, EventEmitter} from '@angular/core';

@Component({
  selector: 'ul[data-component="dropdown-list"]',
  templateUrl: './dropdown-list.component.html',
  styleUrls: ['./dropdown-list.component.less']
})
export class DropdownListComponent {

  @Input()
  items: any[];

  @Input()
  defaultAction: Function;

  @Input()
  isMultipleChoice?: boolean = false;

  @Output()
  selectedItemChange: EventEmitter<any> = new EventEmitter();

  changeSelectedItem(options: any): void {
    this.selectedItemChange.emit(options);
  }

}
