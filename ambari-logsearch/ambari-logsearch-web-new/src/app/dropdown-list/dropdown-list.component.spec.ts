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

import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {Http} from '@angular/http';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';

import {DropdownListComponent} from './dropdown-list.component';

export function HttpLoaderFactory(http: Http) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

describe('DropdownListComponent', () => {
  let component: DropdownListComponent;
  let fixture: ComponentFixture<DropdownListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [DropdownListComponent],
      imports: [TranslateModule.forRoot({
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [Http]
      })]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DropdownListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('#changeSelectedItem()', () => {

    beforeEach(() => {
      spyOn(component.selectedItemChange, 'emit').and.callFake(() => {});
    });

    describe('not a filter list', () => {
      it('event should not be emitted', () => {
        component.isFilter = false;
        component.changeSelectedItem({});
        expect(component.selectedItemChange.emit).not.toHaveBeenCalled();
      });
    });

    describe('filter list', () => {
      const options = {
        label: 'l',
        value: 'v'
      };

      beforeEach(() => {
        component.isFilter = true;
        component.changeSelectedItem(options);
      });

      it('event should be emitted', () => {
        expect(component.selectedItemChange.emit).toHaveBeenCalled();
      });

      it('event emitter should be called with correct arguments', () => {
        expect(component.selectedItemChange.emit).toHaveBeenCalledWith(options);
      });

    });

  });
});
