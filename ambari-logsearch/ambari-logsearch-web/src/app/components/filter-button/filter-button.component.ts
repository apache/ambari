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

import {Component, Input, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {ComponentActionsService} from '@app/services/component-actions.service';
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

  constructor(protected actions: ComponentActionsService, private utils: UtilsService) {
    super(actions);
  }

  @Input()
  defaultValue?: string;

  private selectedValue: any;

  private onChange: (fn: any) => void;

  get value(): any {
    return this.selectedValue;
  }

  set value(newValue: any) {
    this.selectedValue = newValue;
    this.onChange(newValue);
  }

  updateValue(options: any): void {
    const value = options && options.value;
    if (this.isMultipleChoice) {
      this.value = this.utils.updateMultiSelectValue(this.value, value, options.isChecked);
    } else {
      if (this.utils.valueHasChanged(this.selectedValue, value)) {
        this.value = value;
      }
    }
  }

  writeValue() {
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
