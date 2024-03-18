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
import {DebugElement} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {LogLevelComponent} from './log-level.component';
import {By} from '@angular/platform-browser';

describe('LogLevelComponent', () => {
  let component: LogLevelComponent;
  let fixture: ComponentFixture<LogLevelComponent>;
  let de: DebugElement;
  let el: HTMLElement;
  let logLevelMap = {
    warn: 'fa-exclamation-triangle',
    fatal: 'fa-exclamation-circle',
    error: 'fa-exclamation-circle',
    info: 'fa-info-circle',
    debug: 'fa-bug',
    trace: 'fa-random',
    unknown: 'fa-question-circle'
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogLevelComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogLevelComponent);
    component = fixture.componentInstance;
    component.logEntry = {level: 'unknown'};
    fixture.detectChanges();
    de = fixture.debugElement.query(By.css('i.fa'));
    el = de.nativeElement;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  Object.keys(logLevelMap).forEach((level) => {
    describe(level, () => {
      beforeEach(() => {
        component.logEntry = {level: level};
        fixture.detectChanges();
      });
      it(`should return with the ${logLevelMap[level]} css class for ${level} log level`, () => {
        expect(component.cssClass).toEqual(logLevelMap[level]);
      });
      it(`should set the ${logLevelMap[level]} css class on the icon element`, () => {
        expect(el.classList).toContain(logLevelMap[level]);
      });
    });
  });

});
