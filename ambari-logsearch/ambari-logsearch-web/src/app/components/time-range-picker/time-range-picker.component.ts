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

import {Component, OnInit, Input, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Moment} from 'moment';
import {FilteringService} from '@app/services/filtering.service';
import {ListItem} from '@app/classes/list-item';
import {TimeUnitListItem} from '@app/classes/filtering';

@Component({
  selector: 'time-range-picker',
  templateUrl: './time-range-picker.component.html',
  styleUrls: ['./time-range-picker.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeRangePickerComponent),
      multi: true
    }
  ]
})
export class TimeRangePickerComponent implements OnInit, ControlValueAccessor {

  constructor(private filtering: FilteringService) {
  }

  ngOnInit() {
    this.selectedLabel = this.defaultLabel;
  }

  @Input()
  defaultLabel?: string;

  selectedLabel: string;

  startTime: Moment;

  endTime: Moment;

  private onChange: (fn: any) => void;

  get quickRanges(): (ListItem | TimeUnitListItem[])[] {
    return this.filtering.filters.timeRange.options;
  }

  private timeRange?: any;

  get value(): any {
    return this.timeRange;
  }

  set value(newValue: any) {
    this.timeRange = newValue;
    this.onChange(newValue);
  }

  setStartTime(timeObject: Moment): void {
    this.startTime = timeObject;
  }

  setEndTime(timeObject: Moment): void {
    this.endTime = timeObject;
  }

  setTimeRange(value: any, label: string) {
    this.value = value;
    this.selectedLabel = label;
  }

  setCustomTimeRange() {
    this.value = {
      type: 'CUSTOM',
      start: this.startTime,
      end: this.endTime
    };
    this.selectedLabel = 'filter.timeRange.custom';
  }

  writeValue() {
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
