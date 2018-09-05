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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DataLoadingIndicatorComponent } from '@app/modules/shared/components/data-loading-indicator/data-loading-indicator.component';
import { TranslationModules } from '@app/test-config.spec';
import { DataAvailabilityStatesStore, dataAvailabilityStates } from '@app/modules/app-load/stores/data-availability-state.store';
import { StoreModule } from '@ngrx/store';
import { LoadingIndicatorComponent } from '@app/modules/shared/components/loading-indicator/loading-indicator.component';

describe('DataLoadingIndicatorComponent', () => {
  let component: DataLoadingIndicatorComponent;
  let fixture: ComponentFixture<DataLoadingIndicatorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        ...TranslationModules,
        StoreModule.provideStore({
          dataAvailabilityStates
        })
      ],
      declarations: [
        LoadingIndicatorComponent,
        DataLoadingIndicatorComponent
      ],
      providers: [
        DataAvailabilityStatesStore
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DataLoadingIndicatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

});
