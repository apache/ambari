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

import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {StoreModule} from '@ngrx/store';

import {TranslationModules} from '@app/test-config.spec';
import {UtilsService} from '@app/services/utils.service';

import {SearchBoxComponent} from './search-box.component';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {ComponentLabelPipe} from '@app/pipes/component-label';

describe('SearchBoxComponent', () => {
  let component: SearchBoxComponent;
  let fixture: ComponentFixture<SearchBoxComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ComponentLabelPipe,
        SearchBoxComponent
      ],
      imports: [
        ...TranslationModules,
        StoreModule.provideStore({
          components
        })
      ],
      providers: [
        ComponentsService,
        UtilsService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchBoxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('#activeItemValueOptions()', () => {
    const cases = [
      {
        itemsOptions: null,
        activeItem: {
          value: 'v0'
        },
        result: [],
        title: 'no options available'
      },
      {
        itemsOptions: {
          v1: [
            {
              value: 'v2'
            }
          ]
        },
        activeItem: null,
        result: [],
        title: 'no active item'
      },
      {
        itemsOptions: {},
        activeItem: {
          value: 'v3'
        },
        result: [],
        title: 'empty itemsOptions object'
      },
      {
        itemsOptions: {
          v4: [
            {
              value: 'v5'
            }
          ]
        },
        activeItem: {
          value: 'v6'
        },
        result: [],
        title: 'no options available for active item'
      },
      {
        itemsOptions: {
          v7: [
            {
              value: 'v8'
            },
            {
              value: 'v9'
            }
          ]
        },
        activeItem: {
          value: 'v7'
        },
        result: [
          {
            value: 'v8'
          },
          {
            value: 'v9'
          }
        ],
        title: 'options are available for active item'
      }
    ];

    cases.forEach(test => {
      it(test.title, () => {
        component.itemsOptions = test.itemsOptions;
        component.activeItem = test.activeItem;
        expect(component.activeItemValueOptions).toEqual(test.result);
      });
    });
  });
});
