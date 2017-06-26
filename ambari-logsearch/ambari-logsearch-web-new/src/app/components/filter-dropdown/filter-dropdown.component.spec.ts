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

import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {Http} from '@angular/http';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {FilteringService} from '@app/services/filtering.service';

import {FilterDropdownComponent} from './filter-dropdown.component';

export function HttpLoaderFactory(http: Http) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

describe('FilterDropdownComponent', () => {
  let component: FilterDropdownComponent;
  let fixture: ComponentFixture<FilterDropdownComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [FilterDropdownComponent],
      imports: [TranslateModule.forRoot({
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [Http]
      })],
      providers: [FilteringService],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FilterDropdownComponent);
    component = fixture.componentInstance;
    component.filterInstance = {
      options: [
        {
          value: 'v0',
          label: 'l0'
        },
        {
          value: 'v1',
          label: 'l1'
        }
      ]
    };
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('should take initial filter values from 1st item', () => {
    it('selectedValue', () => {
      expect(component.filterInstance.selectedValue).toEqual('v0');
    });

    it('selectedLabel', () => {
      expect(component.filterInstance.selectedLabel).toEqual('l0');
    });
  });

  describe('#setSelectedValue()', () => {
    beforeEach(() => {
      component.setSelectedValue({
        value: 'v2',
        label: 'l2'
      });
    });

    it('selectedValue', () => {
      expect(component.filterInstance.selectedValue).toEqual('v2');
    });

    it('selectedLabel', () => {
      expect(component.filterInstance.selectedLabel).toEqual('l2');
    });
  });
});
