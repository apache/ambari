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
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NgStringPipesModule} from 'angular-pipes';

import {LogMessageComponent} from './log-message.component';

describe('LogMessageComponent', () => {
  let component: LogMessageComponent;
  let fixture: ComponentFixture<LogMessageComponent>;
  const messages = {
    noNewLine: 'There is no newline here.',
    withNewLine: `This is the first line.
    This is the second one.`
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [NgStringPipesModule],
      declarations: [ LogMessageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogMessageComponent);
    component = fixture.componentInstance;
    component.message = messages.withNewLine;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('event handler should call the toggleOpen method', () => {
    let mockEvent: MouseEvent = document.createEvent('MouseEvent');
    mockEvent.initEvent('click', true, true);
    spyOn(component,'toggleOpen');
    component.onCaretClick(mockEvent);
    expect(component.toggleOpen).toHaveBeenCalled();
  });

  it('event handler should prevent the default behaviour of the action', () => {
    let mockEvent: MouseEvent = document.createEvent('MouseEvent');
    mockEvent.initEvent('click', true, true);
    spyOn(mockEvent,'preventDefault');
    component.onCaretClick(mockEvent);
    expect(mockEvent.preventDefault).toHaveBeenCalled();
  });

  it('calling the toggleOpen method should negate the isOpen property', () => {
    let currentState = component.isOpen;
    component.toggleOpen();
    expect(component.isOpen).toEqual(!currentState);
  });

  it('should set the addCaret prop to TRUE if the message prop has new line character.', () => {
    component.message = messages.withNewLine;
    component.checkAddCaret();
    expect(component['addCaret']).toEqual(true);
  });

});
