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

import {Component, forwardRef, Input, Output, EventEmitter} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'pagination-controls',
  templateUrl: './pagination-controls.component.html',
  styleUrls: ['./pagination-controls.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PaginationControlsComponent),
      multi: true
    }
  ]
})
export class PaginationControlsComponent implements ControlValueAccessor {

  private onChange: (fn: any) => void;

  currentPage: number = 0;

  @Input()
  totalCount: number;

  @Input()
  pagesCount: number;

  @Output()
  currentPageChange: EventEmitter<number> = new EventEmitter();

  get value(): number {
    return this.currentPage;
  }

  set value(newValue: number) {
    this.currentPage = newValue;
    this.currentPageChange.emit(newValue);
    this.onChange(newValue);
  }

  updateValue(isDecrement?: boolean) {
    isDecrement? this.value-- : this.value++;
  }

  writeValue() {
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
