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
import {ComponentActionsService} from '@app/services/component-actions.service';
import {UtilsService} from '@app/services/utils.service';
import {DropdownButtonComponent} from '@app/components/dropdown-button/dropdown-button.component';

@Component({
  selector: 'filter-dropdown',
  templateUrl: '../dropdown-button/dropdown-button.component.html',
  styleUrls: ['../dropdown-button/dropdown-button.component.less', './filter-dropdown.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterDropdownComponent),
      multi: true
    }
  ]
})
export class FilterDropdownComponent extends DropdownButtonComponent implements ControlValueAccessor {

  constructor(protected actions: ComponentActionsService, protected utils: UtilsService) {
    super(actions, utils);
  }

  private onChange: (fn: any) => void;

  set value(newValue: any) {
    this.selectedValue = newValue;
    this.onChange(newValue);
  }

  writeValue() {
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
