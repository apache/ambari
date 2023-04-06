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

import {Injector} from '@angular/core';
import {async, ComponentFixture, TestBed, inject} from '@angular/core/testing';
import {TranslationModules} from '@app/test-config.spec';
import {ServiceInjector} from '@app/classes/service-injector';
import {GraphLegendComponent} from '@app/components/graph-legend/graph-legend.component';
import {GraphLegendItemComponent} from '@app/components/graph-legend-item/graph-legend-item.component';
import {GraphTooltipComponent} from '@app/components/graph-tooltip/graph-tooltip.component';
import {UtilsService} from '@app/services/utils.service';

import {HorizontalHistogramComponent} from './horizontal-histogram.component';

describe('HorizontalHistogramComponent', () => {
  let component: HorizontalHistogramComponent;
  let fixture: ComponentFixture<HorizontalHistogramComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        HorizontalHistogramComponent,
        GraphLegendComponent,
        GraphLegendItemComponent,
        GraphTooltipComponent
      ],
      imports: [
        ...TranslationModules
      ],
      providers: [
        UtilsService
      ]
    })
    .compileComponents();
  }));

  beforeEach(inject([Injector], (injector: Injector) => {
    ServiceInjector.injector = injector;
    fixture = TestBed.createComponent(HorizontalHistogramComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
