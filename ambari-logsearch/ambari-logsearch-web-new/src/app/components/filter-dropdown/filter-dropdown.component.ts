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

import {Component, AfterViewInit, Input, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR, FormGroup} from '@angular/forms';
import {FilteringService} from '@app/services/filtering.service';
import {ComponentActionsService} from '@app/services/component-actions.service';

@Component({
  selector: 'filter-dropdown',
  templateUrl: './filter-dropdown.component.html',
  styleUrls: ['./filter-dropdown.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterDropdownComponent),
      multi: true
    }
  ]
})
export class FilterDropdownComponent implements AfterViewInit, ControlValueAccessor {

  constructor(private filtering: FilteringService, private actions: ComponentActionsService) {
  }

  ngAfterViewInit() {
    const callback = this.customOnChange ?
      (value => this.actions[this.customOnChange](value)) : (() => this.filtering.filteringSubject.next(null));
    this.form.controls[this.filterName].valueChanges.subscribe(callback);
  }

  @Input()
  options: any[];

  @Input()
  customOnChange: string;

  @Input()
  form: FormGroup;

  @Input()
  filterName: string;

  private onChange: (fn: any) => void;

  get filterInstance(): any {
    return this.filtering.filters[this.filterName];
  }

  get value(): any {
    return this.filterInstance.selectedValue;
  }

  set value(newValue: any) {
    if (this.filtering.valueHasChanged(this.filterInstance.selectedValue, newValue)) {
      this.filterInstance.selectedValue = newValue;
      this.onChange(newValue);
    }
  }

  writeValue(options: any) {
    const value = options && options.value;
    if (this.filtering.valueHasChanged(this.filterInstance.selectedValue, value)) {
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
