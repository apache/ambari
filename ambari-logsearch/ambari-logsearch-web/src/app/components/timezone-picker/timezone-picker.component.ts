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

import {Component, OnInit} from '@angular/core';
import * as $ from 'jquery';
import '@vendor/js/WorldMapGenerator.min';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {UserSettingsService} from '@app/services/user-settings.service';

@Component({
  selector: 'timezone-picker',
  templateUrl: './timezone-picker.component.html',
  styleUrls: ['./timezone-picker.component.less']
})
export class TimeZonePickerComponent implements OnInit {

  constructor(private appSettings: AppSettingsService, private settingsService: UserSettingsService) {
  }

  ngOnInit() {
    this.appSettings.getParameter('timeZone').subscribe((value: string) => this.timeZone = value);
  }

  readonly mapElementId = 'timezone-map';

  private readonly mapOptions = {
    quickLink: [
      {
        PST: 'PST',
        MST: 'MST',
        CST: 'CST',
        EST: 'EST',
        GMT: 'GMT',
        LONDON: 'Europe/London',
        IST: 'IST'
      }
    ]
  };

  private mapElement: any;

  private timeZoneSelect: JQuery;

  isTimeZonePickerDisplayed: boolean = false;

  timeZone: string;

  setTimeZonePickerDisplay(isDisplayed: boolean): void {
    this.isTimeZonePickerDisplayed = isDisplayed;
  }

  initMap(): void {
    this.mapElement = $(`#${this.mapElementId}`);
    this.mapElement.WorldMapGenerator(this.mapOptions);
    this.timeZoneSelect = this.mapElement.find('select');
    this.timeZoneSelect.removeClass('btn btn-default').addClass('form-control').val(this.timeZone);
  }

  setTimeZone(): void {
    const timeZone = this.timeZoneSelect.val();
    this.settingsService.setTimeZone(timeZone);
    this.setTimeZonePickerDisplay(false);
  }

}
