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

import {Component, Input} from '@angular/core';
import {ListItem} from '@app/classes/list-item';
import {ServiceInjector} from '@app/classes/service-injector';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {UtilsService} from '@app/services/utils.service';

@Component({
  selector: 'dropdown-button',
  templateUrl: './dropdown-button.component.html',
  styleUrls: ['./dropdown-button.component.less']
})
export class DropdownButtonComponent {

  constructor(protected utils: UtilsService) {
    this.actions = ServiceInjector.injector.get(ComponentActionsService);
  }
  
  @Input()
  label?: string;

  @Input()
  buttonClass: string = '';

  @Input()
  iconClass?: string;

  @Input()
  hideCaret: boolean = false;

  @Input()
  showSelectedValue: boolean = true;

  @Input()
  options: ListItem[] = [];

  @Input()
  action?: string;

  @Input()
  additionalArgs: any[] = [];

  @Input()
  isMultipleChoice: boolean = false;

  @Input()
  isRightAlign: boolean = false;

  @Input()
  isDropup: boolean = false;

  private actions: ComponentActionsService;

  protected selectedItems?: ListItem[] = [];

  get selection(): ListItem[] {
    return this.selectedItems;
  }

  set selection(items: ListItem[]) {
    this.selectedItems = items;
  }

  updateSelection(item: ListItem): void {
    const action = this.action && this.actions[this.action];
    if (this.isMultipleChoice) {
      this.options.find((option: ListItem): boolean => {
        return this.utils.isEqual(option.value, item.value);
      }).isChecked = item.isChecked;
      const checkedItems = this.options.filter((option: ListItem): boolean => option.isChecked);
      this.selection = checkedItems;
      if (action) {
        action(checkedItems.map((option: ListItem): any => option.value), ...this.additionalArgs);
      }
    } else if (!this.utils.isEqual(this.selection[0], item)) {
      this.selection = [item];
      if (action) {
        action(item.value, ...this.additionalArgs);
      }
    }
  }

}
