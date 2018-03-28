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

import {Component, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {ListItem} from '@app/classes/list-item';
import {UtilsService} from '@app/services/utils.service';
import {MenuButtonComponent} from '@app/components/menu-button/menu-button.component';

@Component({
  selector: 'filter-button',
  templateUrl: '../menu-button/menu-button.component.html',
  styleUrls: ['../menu-button/menu-button.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterButtonComponent),
      multi: true
    }
  ]
})
export class FilterButtonComponent extends MenuButtonComponent implements ControlValueAccessor {

  private selectedItems: ListItem[] = [];

  private onChange: (fn: any) => void;

  constructor(private utils: UtilsService) {
    super();
  }

  get selection(): ListItem[] {
    return this.selectedItems;
  }

  set selection(items: ListItem[]) {
    this.selectedItems = items;
    if (this.onChange) {
      this.onChange(items);
    }
  }

  updateSelection(item: ListItem): void {
    if (this.isMultipleChoice) {
      const itemIndex = this.subItems.findIndex((option: ListItem): boolean => {
        return this.utils.isEqual(option.value, item.value);
      });
      if (itemIndex > -1) {
        this.subItems[itemIndex].isChecked = item.isChecked;
        this.selection = this.subItems.filter((option: ListItem): boolean => option.isChecked);
      }
    } else if (!this.utils.isEqual(this.selection[0], item)) {
      this.selection = [item];
    }
  }

  writeValue(items: ListItem[]) {
    if (items && items.length) {
      items.forEach((item) => {
        this.updateSelection({
          ...item,
          isChecked: true
        });
      });
    }
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
