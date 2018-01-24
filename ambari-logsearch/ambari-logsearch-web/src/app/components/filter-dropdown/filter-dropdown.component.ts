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
import {UtilsService} from '@app/services/utils.service';
import {DropdownButtonComponent} from '@app/components/dropdown-button/dropdown-button.component';
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

  constructor(protected utils: UtilsService) {
    super(utils);
  }

  private onChange: (fn: any) => void;

  get selection(): ListItem[] {
    return this.selectedItems;
  }

  set selection(items: ListItem[]) {
    this.selectedItems = items;
    if (this.isMultipleChoice) {
      this.options.forEach((option: ListItem): void => {
        const selectionItem = items.find((item: ListItem): boolean => this.utils.isEqual(item.value, option.value));
        option.isChecked = Boolean(selectionItem);
      });
    }
    if (this.onChange) {
      this.onChange(items);
    }
  }

  writeValue(items: ListItem[]) {
    this.selection = items;
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
