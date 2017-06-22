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

import {Component, OnInit, Input} from '@angular/core';
import {FilteringService} from '@app/services/filtering.service';

@Component({
  selector: 'filter-dropdown',
  templateUrl: './filter-dropdown.component.html',
  styleUrls: ['./filter-dropdown.component.less']
})
export class FilterDropdownComponent implements OnInit {

  constructor(private filtering: FilteringService) {
  }

  ngOnInit() {
    this.filterInstance.selectedValue = this.filterInstance.options[0].value;
    this.filterInstance.selectedLabel = this.filterInstance.options[0].label;
  }

  @Input()
  filterInstance: any;

  @Input()
  options: any[];

  setSelectedValue(options: any): void {
    if (this.filterInstance.selectedValue !== options.value) {
      this.filterInstance.selectedValue = options.value;
      this.filterInstance.selectedLabel = options.label;
      this.filtering.filteringSubject.next(null);
    }
  };

}
