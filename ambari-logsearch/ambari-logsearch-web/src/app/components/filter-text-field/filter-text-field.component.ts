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
import {Subject} from 'rxjs/Subject';
import 'rxjs/add/operator/debounceTime';
import {UtilsService} from '@app/services/utils.service';

@Component({
  selector: 'filter-text-field',
  templateUrl: './filter-text-field.component.html',
  styleUrls: ['./filter-text-field.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterTextFieldComponent),
      multi: true
    }
  ]
})
export class FilterTextFieldComponent implements ControlValueAccessor {

  constructor(private utils: UtilsService) {
    this.valueSubject.debounceTime(this.debounceInterval).subscribe(value => this.updateValue({
      value
    }));
  }

  @Input()
  label: string;

  private selectedValue: string;

  private onChange: (fn: any) => void;

  private readonly debounceInterval = 1500;

  instantValue: string;

  private valueSubject = new Subject<string>();

  get value(): any {
    return this.selectedValue;
  }

  set value(newValue: any) {
    this.selectedValue = newValue;
    this.onChange(newValue);
  }

  updateValue(options: any) {
    const value = options && options.value;
    if (this.utils.valueHasChanged(this.selectedValue, value)) {
      this.value = value;
    }
  }

  writeValue() {
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

  updateInstantValue(value: string): void {
    this.valueSubject.next(value);
  }

}
