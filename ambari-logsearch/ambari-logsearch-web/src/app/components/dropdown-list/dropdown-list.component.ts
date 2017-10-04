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

import {Component, AfterViewInit, Input, Output, EventEmitter, ViewChildren, ViewContainerRef, QueryList} from '@angular/core';
import {ListItem} from '@app/classes/list-item';
import {ComponentGeneratorService} from '@app/services/component-generator.service';
import {ComponentActionsService} from '@app/services/component-actions.service';

@Component({
  selector: 'ul[data-component="dropdown-list"]',
  templateUrl: './dropdown-list.component.html',
  styleUrls: ['./dropdown-list.component.less']
})
export class DropdownListComponent implements AfterViewInit {

  constructor(private componentGenerator: ComponentGeneratorService, private actions: ComponentActionsService) {
  }

  ngAfterViewInit() {
    const setter = this.additionalLabelComponentSetter;
    if (setter) {
      this.containers.forEach((container, index) => this.componentGenerator[setter](this.items[index].value, container));
    }
  }

  @Input()
  items: ListItem[];

  @Input()
  defaultAction: Function;

  @Input()
  isMultipleChoice?: boolean = false;

  @Input()
  additionalLabelComponentSetter?: string;

  @Input()
  actionArguments: any[] = [];

  @Output()
  selectedItemChange: EventEmitter<ListItem> = new EventEmitter();

  @ViewChildren('additionalComponent', {
    read: ViewContainerRef
  })
  containers: QueryList<ViewContainerRef>;

  changeSelectedItem(options: ListItem): void {
    if (options.action) {
      this.actions[options.action](...this.actionArguments);
    }
    this.selectedItemChange.emit(options);
  }

}
