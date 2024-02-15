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
  buttonClass = 'btn-link';

  @Input()
  iconClass?: string;

  @Input()
  hideCaret = false;

  @Input()
  showSelectedValue = true;

  @Input()
  isRightAlign = false;

  @Input()
  isDropup = false;

  @Input()
  showCommonLabelWithSelection = false;

  @Output()
  selectItem: EventEmitter<any> = new EventEmitter();

  // PROXY PROPERTIES TO DROPDOWN LIST COMPONENT
  private _options: ListItem[] = [];
  private originalOptions: ListItem[] = [];

  @Input()
  set options(options: ListItem[]) {
    this._options = options;
    this.originalOptions = options.map(option => Object.assign({}, option));
  }

  get options(): ListItem[] {
    return this._options;
  }

  @Input()
  listItemArguments: any[] = [];

  @Input()
  isMultipleChoice = false;

  @Input()
  useClearToDefaultSelection = false;

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

  clearSelection(silent: boolean = false) {
    let hasChange = false;
    this.options.forEach((item: ListItem) => {
      hasChange = hasChange || item.isChecked;
      item.isChecked = false;
    });
    if (!silent && hasChange) {
      this.selectItem.emit(this.isMultipleChoice ? [] : undefined);
    }
  }

  updateSelection(updates: ListItem | ListItem[], callOnChange: boolean = true): boolean {
    let hasChange = false;
    if (updates && (!Array.isArray(updates) || updates.length)) {
      const items: ListItem[] = Array.isArray(updates) ? updates : [updates];
      if (this.isMultipleChoice) {
        items.forEach((item: ListItem) => {
          if (this.originalOptions && this.originalOptions.length) {
            const itemToUpdate: ListItem = this.originalOptions.find((option: ListItem) => this.utils.isEqual(option.value, item.value));
            if (itemToUpdate) {
              hasChange = hasChange || itemToUpdate.isChecked !== item.isChecked;
              itemToUpdate.isChecked = item.isChecked;
            }
          }
        });
      } else {
        const selectedItem: ListItem = Array.isArray(updates) ? updates[0] : updates;
        this.options.forEach((item: ListItem) => {
          const checkedStateBefore = item.isChecked;
          item.isChecked = this.utils.isEqual(item.value, selectedItem.value);
          hasChange = hasChange || checkedStateBefore !== item.isChecked;
        });
      }
    } else {
      this.options.forEach((item: ListItem) => item.isChecked = false);
    }
    const checkedItems = this.options.filter((option: ListItem): boolean => option.isChecked);
    this.selection = checkedItems;
    if (hasChange) {
      const selectedValues = checkedItems.map((option: ListItem): any => option.value);
      this.selectItem.emit(this.isMultipleChoice ? selectedValues : selectedValues.shift());
    }
    return hasChange;
  }

}
