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
import {By} from '@angular/platform-browser';
import {TranslationModules} from '@app/test-config.spec';
import {HttpClientService} from '@app/services/http-client.service';

import {CollapsiblePanelComponent} from './collapsible-panel.component';

describe('CollapsiblePanelComponent', () => {
  let component: CollapsiblePanelComponent;
  let fixture: ComponentFixture<CollapsiblePanelComponent>;
  let de: DebugElement;
  let el: HTMLElement;

  beforeEach(async(() => {
    const httpClient = {
      get: () => {
        return {
          subscribe: () => {
          }
        }
      }
    };
    TestBed.configureTestingModule({
      declarations: [CollapsiblePanelComponent],
      imports: TranslationModules,
      providers: [
        {
          provide: HttpClientService,
          useValue: httpClient
        }
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollapsiblePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    de = fixture.debugElement.query(By.css('div.panel'));
    el = de.nativeElement;
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should call toggleCollapsed', () => {
    let mockEvent: MouseEvent = document.createEvent('MouseEvent');
    mockEvent.initEvent('click', true, true);
    spyOn(component,'toggleCollapsed');
    component.handleCollapseBtnClick(mockEvent);
    expect(component.toggleCollapsed).toHaveBeenCalled();
  });

  it('should prevent default action on event after toggle button click',() => {
    let mockEvent: MouseEvent = document.createEvent('MouseEvent');
    mockEvent.initEvent('click', true, true);
    spyOn(mockEvent,'preventDefault');
    component.handleCollapseBtnClick(mockEvent);
    expect(mockEvent.preventDefault).toHaveBeenCalled();
  });

  it('should negate the isCollapsed property', () => {
    let valueBefore = component.isCollapsed;
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.isCollapsed).toEqual(!valueBefore);
  });

  it('should add `panel-collapsed` css class to the element when the isCollapsed is true', () => {
    component.isCollapsed = true;
    fixture.detectChanges();
    expect(el.className).toContain('panel-collapsed');
  });

  it('should not have `panel-collapsed` css class on the element when the isCollapsed is false', () => {
    component.isCollapsed = false;
    fixture.detectChanges();
    expect(el.className).not.toContain('panel-collapsed');
  });

  it('should display the openTitle if presented and the isCollapsed property is false', () => {
    let title = 'Open title';
    let headingEl = el.querySelector('.panel-heading');
    component.openTitle = title;
    component.isCollapsed = false;
    fixture.detectChanges();
    expect(headingEl.textContent).toContain(title);
  });

  it('should display the collapsedTitle if it presented and the isCollapsed property is true', () => {
    let title = 'Collapsed title';
    let headingEl = el.querySelector('.panel-heading');
    component.collapsedTitle = title;
    component.isCollapsed = true;
    fixture.detectChanges();
    expect(headingEl.textContent).toContain(title);
  });

  it('should display the title if openTitle is not presented and the isCollapsed property is false', () => {
    let title = 'Title';
    let headingEl = el.querySelector('.panel-heading');
    component.openTitle = '';
    component.commonTitle = title;
    component.isCollapsed = false;
    fixture.detectChanges();
    expect(headingEl.textContent).toContain(title);
  });

  it('should display the title if collapsedTitle is not presented and the isCollapsed property is true', () => {
    let title = 'Title';
    let headingEl = el.querySelector('.panel-heading');
    component.collapsedTitle = '';
    component.commonTitle = title;
    component.isCollapsed = true;
    fixture.detectChanges();
    expect(headingEl.textContent).toContain(title);
  });

});
