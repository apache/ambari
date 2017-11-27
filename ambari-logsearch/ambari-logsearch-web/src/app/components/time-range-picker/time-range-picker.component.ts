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
import {Moment} from 'moment-timezone';
import {LogsContainerService} from '@app/services/logs-container.service';
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
export class TimeRangePickerComponent implements ControlValueAccessor {

  constructor(private logsContainer: LogsContainerService) {
  }

  startTime: Moment;

  endTime: Moment;

  private onChange: (fn: any) => void;

  get quickRanges(): (ListItem | TimeUnitListItem[])[] {
    return this.logsContainer.filters.timeRange.options;
  }

  private timeRange?: TimeUnitListItem;

  get selection(): TimeUnitListItem {
    return this.timeRange;
  }

  set selection(newValue: TimeUnitListItem) {
    this.timeRange = newValue;
    if (this.onChange) {
      this.onChange(newValue);
    }
    this.setEndTime(this.logsContainer.getEndTimeMoment(newValue));
    this.setStartTime(this.logsContainer.getStartTimeMoment(newValue, this.endTime));
  }

  setStartTime(timeObject: Moment): void {
    this.startTime = timeObject;
  }

  setEndTime(timeObject: Moment): void {
    this.endTime = timeObject;
  }

  setTimeRange(value: any, label: string): void {
    this.selection = {label, value};
  }

  setCustomTimeRange(): void {
    this.selection = {
      label: 'filter.timeRange.custom',
      value: {
        type: 'CUSTOM',
        start: this.startTime,
        end: this.endTime
      }
    };
  }

  writeValue(selection: TimeUnitListItem): void {
    this.selection = selection;
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched(): void {
  }

}
