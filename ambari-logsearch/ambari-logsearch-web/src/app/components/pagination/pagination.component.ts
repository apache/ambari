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

import {Component, OnInit, Input} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {ListItem} from '@app/classes/list-item';
import {FilterCondition} from '@app/classes/filtering';

@Component({
  selector: 'pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.less']
})
export class PaginationComponent implements OnInit {

  ngOnInit() {
    this.setPageSizeFromString(this.filterInstance.defaultSelection[0].value);
    this.filtersForm.controls.pageSize.valueChanges.subscribe((selection: ListItem): void => {
      this.setPageSizeFromString(selection[0].value);
    });
  }

  @Input()
  filtersForm: FormGroup;

  @Input()
  filterInstance: FilterCondition;

  @Input()
  currentCount?: number;

  @Input()
  totalCount: number;

  private pageSize: number = 0;

  private setPageSizeFromString(value: string) {
    this.pageSize = parseInt(value);
  }

  private currentPage: number = 0;

  get numbersTranslateParams(): any {
    const pageSize = this.pageSize,
      startIndex = (this.currentPage * pageSize) + 1;
    return {
      startIndex,
      endIndex: startIndex + Math.min(pageSize, this.currentCount) - 1,
      totalCount: this.totalCount
    }
  }

  get pagesCount(): number {
    return Math.ceil(this.totalCount / this.pageSize);
  }

  setCurrentPage(pageNumber: number) {
    this.currentPage = pageNumber;
  }

}
