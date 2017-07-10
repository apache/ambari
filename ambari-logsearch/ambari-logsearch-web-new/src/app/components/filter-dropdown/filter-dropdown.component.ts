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

import {Component, Input, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR, FormGroup} from '@angular/forms';
import {FilteringService} from '@app/services/filtering.service';
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

  constructor(protected filtering: FilteringService, protected actions: ComponentActionsService, protected utils: UtilsService) {
    super(filtering, actions, utils);
  }

  ngOnInit() {
  }

  @Input()
  form: FormGroup;

  @Input()
  filterName: string;

  private onChange: (fn: any) => void;

  get filterInstance(): any {
    return this.filtering.filters[this.filterName];
  }

  get label(): string {
    return this.filterInstance.label;
  }

  get defaultValue(): any {
    return this.filterInstance.defaultValue;
  }

  get defaultLabel(): any {
    return this.filterInstance.defaultLabel;
  }

  get value(): any {
    return this.filterInstance.selectedValue == null ? this.defaultValue : this.filterInstance.selectedValue;
  }

  set value(newValue: any) {
    if (this.utils.valueHasChanged(this.filterInstance.selectedValue, newValue)) {
      this.filterInstance.selectedValue = newValue;
      this.onChange(newValue);
    }
  }

  get selectedLabel(): string {
    return this.filterInstance.selectedLabel == null ? this.defaultLabel : this.filterInstance.selectedLabel;
  }

  writeValue(options: any): void {
    const value = options && options.value;
    if (this.utils.valueHasChanged(this.filterInstance.selectedValue, value)) {
      this.filterInstance.selectedValue = value;
      this.filterInstance.selectedLabel = options.label;
    }
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
