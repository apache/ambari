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

import {Component, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {DropdownButtonComponent} from '@modules/shared/components/dropdown-button/dropdown-button.component';
import {ListItem} from '@app/classes/list-item';

@Component({
  selector: 'filter-dropdown',
  templateUrl: '../dropdown-button/dropdown-button.component.html',
  styleUrls: ['../dropdown-button/dropdown-button.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterDropdownComponent),
      multi: true
    }
  ]
})
export class FilterDropdownComponent extends DropdownButtonComponent implements ControlValueAccessor {

  private onChange;

  get selection(): ListItem[] {
    return this.selectedItems;
  }

  set selection(items: ListItem[]) {
    this.selectedItems = Array.isArray(items) ? items : [items];
    if (this.isMultipleChoice && this.options) {
      this.options.forEach((option: ListItem): void => {
        const selectionItem = this.selectedItems.find((item: ListItem): boolean => this.utils.isEqual(item.value, option.value));
        option.isChecked = Boolean(selectionItem);
      });
    }
  }

  private _onChange(value) {
    if (this.onChange) {
      this.onChange(value);
    }
  }

  updateSelection(updates: ListItem | ListItem[], callOnChange: boolean = true): boolean {
    const hasChange = super.updateSelection(updates);
    if (hasChange && callOnChange) {
      this._onChange(this.selection);
    }
    return hasChange;
  }

  writeValue(items: ListItem[]) {
    this.selection = items || [];
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
