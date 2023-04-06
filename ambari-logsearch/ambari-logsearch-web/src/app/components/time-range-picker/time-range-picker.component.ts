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
import {LogsFilteringUtilsService} from '@app/services/logs-filtering-utils.service';

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

  startTime: Moment;

  endTime: Moment;

  private onChange: (fn: any) => void;

  private timeRange?: TimeUnitListItem;

  constructor(
    private logsContainer: LogsContainerService,
    private logsFilteringUtilsService: LogsFilteringUtilsService
  ) {}

  get quickRanges(): (ListItem | TimeUnitListItem[])[] {
    return this.logsContainer.filters.timeRange.options;
  }

  get selection(): TimeUnitListItem {
    return this.timeRange;
  }

  set selection(newValue: TimeUnitListItem) {
    this.timeRange = newValue;
    this.setEndTime(this.logsFilteringUtilsService.getEndTimeMomentFromTimeUnitListItem(newValue, this.logsContainer.timeZone));
    this.setStartTime(this.logsFilteringUtilsService.getStartTimeMomentFromTimeUnitListItem(
      newValue, this.endTime, this.logsContainer.timeZone
    ));
  }

  setStartTime(timeObject: Moment): void {
    this.startTime = timeObject;
  }

  setEndTime(timeObject: Moment): void {
    this.endTime = timeObject;
  }

  setTimeRange(value: any, label: string): void {
    this.selection = {label, value};
    this._onChange(this.selection);
  }

  setCustomTimeRange(): void {
    this.selection = {
      label: this.logsContainer.customTimeRangeKey,
      value: {
        type: 'CUSTOM',
        start: this.startTime,
        end: this.endTime
      }
    };
    this._onChange(this.selection);
  }

  private _onChange(value: TimeUnitListItem): void {
    if (this.onChange) {
      this.onChange(value);
    }
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
