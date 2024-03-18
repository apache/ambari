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
    if (this.isValidValue(newValue)) { // this is the last validation check
      this.currentPage = newValue;
      this.currentPageChange.emit(newValue);
    } else {
      throw new Error(`Invalid value ${newValue}. The currentPage should be between 0 and ${this.pagesCount}.`);
    }
  }

  /**
   * A simple check if the given value is valid for the current pagination instance
   * @param {number} value The new value to test
   * @returns {boolean}
   */
  private isValidValue(value: number): boolean {
    return value <= this.pagesCount || value >= 0;
  }

  /**
   * The goal is to set the value to the first page... obviously to zero. It is just to have a centralized api for that.
   */
  setFirstPage(): void {
    this._setValueByUserInput(0);
  }

  /**
   * The goal is to set the value to the last page which is the pagesCount property anyway.
   */
  setLastPage(): void {
    this._setValueByUserInput(this.pagesCount - 1);
  }

  /**
   * The goal is to decrease the value (currentPage) property if it is possible (checking with 'hasPreviousPage').
   * @returns {number} The new value of the currentPage
   */
  setPreviousPage(): number {
    if (this.hasPreviousPage()) {
      this._setValueByUserInput(this.value - 1);
    }
    return this.value;
  }

  /**
   * The goal is to increase the value (currentPage) property if it is possible (checking with 'hasNextPage').
   * @returns {number} The new value of the currentPage
   */
  setNextPage(): number {
    if (this.hasNextPage()) {
      this._setValueByUserInput(this.value + 1);
    }
    return this.value;
  }

  /**
   * The goal is to have a single source of true to check if we can set a next page or not.
   * @returns {boolean}
   */
  hasNextPage(): boolean {
    return this.pagesCount > 0 && this.value < this.pagesCount - 1;
  }

  /**
   * The goal is to have a single source of true to check if we can set a previous page or not.
   * @returns {boolean}
   */
  hasPreviousPage(): boolean {
    return this.pagesCount > 0 && this.value > 0;
  }

  private _setValueByUserInput(value) {
    this.value = value;
    this._onChange(this.value);
  }

  private _onChange(value) {
    if (this.onChange) {
      this.onChange(value);
    }
  }

  writeValue(value: number) {
    this.value = value;
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
