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
import {Tab} from '@app/classes/models/tab';
import {TranslationModules} from '@app/test-config.spec';

import {TabsComponent} from './tabs.component';

describe('TabsComponent', () => {
  let component: TabsComponent;
  let fixture: ComponentFixture<TabsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [TabsComponent],
      imports: TranslationModules
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TabsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('#switchTab()', () => {
    let activeTab;
    const tab = {
      id: 'tab0',
      type: '',
      isActive: true,
      label: '',
      appState: null
    };

    it('new active tab', () => {
      component.tabSwitched.subscribe((tab: Tab) => activeTab = tab);
      component.switchTab(tab);
      expect(activeTab).toEqual(tab);
    });
  });

  describe('#closeTab()', () => {
    const items = [
        {
          id: 'serviceLogs',
          type: '',
          isActive: false,
          label: '',
          appState: null
        },
        {
          id: 'auditLogs',
          type: '',
          isActive: false,
          label: '',
          appState: null
        },
        {
          id: 'newTab',
          type: '',
          isActive: false,
          label: '',
          appState: null
        }
      ],
      cases = [
        {
          closedTabIndex: 2,
          newActiveTabIndex: 1,
          title: 'last tab closed'
        },
        {
          closedTabIndex: 1,
          newActiveTabIndex: 2,
          title: 'not last tab closed'
        }
      ];

    cases.forEach(test => {
      let oldTab,
        newTab;
      describe(test.title, () => {
        beforeEach(() => {
          oldTab = null;
          newTab = null;
          component.items = items;
          component.tabClosed.subscribe((tabs: Tab[]): void => {
            oldTab = tabs[0];
            newTab = tabs[1];
          });
          component.closeTab(items[test.closedTabIndex]);
        });

        it('closed tab', () => {
          expect(oldTab).toEqual(items[test.closedTabIndex]);
        });

        it('new active tab', () => {
          expect(newTab).toEqual(items[test.newActiveTabIndex]);
        });
      });
    });
  });
});
