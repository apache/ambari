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

import {Injector, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed, inject} from '@angular/core/testing';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';
import {TranslationModules} from '@app/test-config.spec';
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {UtilsService} from '@app/services/utils.service';
import {ServiceInjector} from '@app/classes/service-injector';
import {GraphLegendComponent} from '@app/components/graph-legend/graph-legend.component';

import {TimeLineGraphComponent} from './time-line-graph.component';

describe('TimeLineGraphComponent', () => {
  let component: TimeLineGraphComponent;
  let fixture: ComponentFixture<TimeLineGraphComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        TimeLineGraphComponent,
        GraphLegendComponent
      ],
      imports: [
        MomentModule,
        MomentTimezoneModule,
        ...TranslationModules,
        StoreModule.provideStore({
          appSettings
        })
      ],
      providers: [
        UtilsService,
        AppSettingsService
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(inject([Injector], (injector: Injector) => {
    ServiceInjector.injector = injector;
    fixture = TestBed.createComponent(TimeLineGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
