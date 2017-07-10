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

import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {Http} from '@angular/http';
import {FormsModule, FormControl, FormGroup} from '@angular/forms';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {FilteringService} from '@app/services/filtering.service';
import {UtilsService} from '@app/services/utils.service';
import {ComponentActionsService} from '@app/services/component-actions.service';

import {FilterTextFieldComponent} from './filter-text-field.component';

export function HttpLoaderFactory(http: Http) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

describe('FilterTextFieldComponent', () => {
  let component: FilterTextFieldComponent;
  let fixture: ComponentFixture<FilterTextFieldComponent>;
  const filtering = {
    filters: {
      f: {}
    }
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [FilterTextFieldComponent],
      imports: [
        FormsModule,
        TranslateModule.forRoot({
          provide: TranslateLoader,
          useFactory: HttpLoaderFactory,
          deps: [Http]
        }),
        StoreModule.provideStore({
          appSettings
        })
      ],
      providers: [
        AppSettingsService,
        {
          provide: FilteringService,
          useValue: filtering
        },
        UtilsService,
        ComponentActionsService
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FilterTextFieldComponent);
    component = fixture.componentInstance;
    component.filterName = 'f';
    component.form = new FormGroup({
      f: new FormControl()
    });
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
