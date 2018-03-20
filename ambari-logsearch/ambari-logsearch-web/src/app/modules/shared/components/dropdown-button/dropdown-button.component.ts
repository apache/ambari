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
import {ListItem} from '@app/classes/list-item';
import {UtilsService} from '@app/services/utils.service';

@Component({
  selector: 'dropdown-button',
  templateUrl: './dropdown-button.component.html',
  styleUrls: ['./dropdown-button.component.less']
})
export class DropdownButtonComponent {

  @Input()
  label?: string;

  @Input()
  buttonClass: string = 'btn-link';

  @Input()
  iconClass?: string;

  @Input()
  hideCaret: boolean = false;

  @Input()
  showSelectedValue: boolean = true;


  @Input() options: ListItem[] = [];

  @Input()
  listItemArguments: any[] = [];

  @Input()
  isMultipleChoice: boolean = false;

  @Input()
  isRightAlign: boolean = false;

  @Input()
  isDropup: boolean = false;

  @Input()
  showCommonLabelWithSelection: boolean = false;

  @Output()
  selectItem: EventEmitter<any> = new EventEmitter();

  protected selectedItems?: ListItem[] = [];

  get selection(): ListItem[] {
    return this.selectedItems;
  }

  set selection(items: ListItem[]) {
    this.selectedItems = items;
  }

  // TODO handle case of selections with multiple items
  /**
   * Indicates whether selection can be displayed at the moment, i.e. it's not empty, not multiple
   * and set to be displayed by showSelectedValue flag
   * @returns {boolean}
   */
  get isSelectionDisplayable(): boolean {
    return this.showSelectedValue && !this.isMultipleChoice && this.selection.length > 0;
  }

  constructor(protected utils: UtilsService) {}

  updateSelection(item: ListItem): void {
    if (this.isMultipleChoice) {
      this.options.find((option: ListItem): boolean => {
        return this.utils.isEqual(option.value, item.value);
      }).isChecked = item.isChecked;
      const checkedItems = this.options.filter((option: ListItem): boolean => option.isChecked);
      this.selection = checkedItems;
      this.selectItem.emit(checkedItems.map((option: ListItem): any => option.value));
    } else if (!this.utils.isEqual(this.selection[0], item)) {
      this.selection = [item];
      this.selectItem.emit(item.value);
    }
  }

}
