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

import {PaginationControlsComponent} from './pagination-controls.component';

describe('PaginationControlsComponent', () => {
  let component: PaginationControlsComponent;
  let fixture: ComponentFixture<PaginationControlsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [PaginationControlsComponent]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PaginationControlsComponent);
    component = fixture.componentInstance;
    component.registerOnChange(() => {});
    component.pagesCount = 3;
    component.totalCount = 30;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should the hasNextPage function return true when the currentPage is less than the pagesCount', () => {
    component.pagesCount = 3;
    component.totalCount = 30;
    fixture.detectChanges();
    expect(component.hasNextPage()).toBe(true);
  });
  it('should the hasNextPage function return false when the currentPage is equal than the pagesCount', () => {
    component.currentPage = 3;
    fixture.detectChanges();
    expect(component.hasNextPage()).toBe(false);
  });
  it('should the hasNextPage function return false when the pagesCount is 0', () => {
    component.pagesCount = 0;
    component.totalCount = 0;
    component.currentPage = 0;
    fixture.detectChanges();
    expect(component.hasNextPage()).toBe(false);
  });

  it('should the hasPreviousPage function return true when the currentPage is greater than 0 and the pagesCount is greater than 0', () => {
    component.currentPage = 1;
    fixture.detectChanges();
    expect(component.hasPreviousPage()).toBe(true);
  });
  it('should the hasPreviousPage function return false when the currentPage is equal to 0', () => {
    component.currentPage = 0;
    fixture.detectChanges();
    expect(component.hasPreviousPage()).toBe(false);
  });
  it('should the hasPreviousPage function return false when the pagesCount is 0', () => {
    component.pagesCount = 0;
    component.totalCount = 0;
    fixture.detectChanges();
    expect(component.hasPreviousPage()).toBe(false);
  });

  it('should the setNextPage function increment the value/currentPage when it is less then the pagesCount', () => {
    let initialPage = 0;
    let pagesCount = 3;
    component.pagesCount = pagesCount;
    component.totalCount = 30;
    component.currentPage = initialPage;
    fixture.detectChanges();
    component.setNextPage();
    fixture.detectChanges();
    expect(component.currentPage).toEqual(initialPage + 1);
  });

  it('should not the setNextPage function increment the value/currentPage when it is on the last page', () => {
    let pagesCount = 3;
    component.pagesCount = pagesCount;
    component.totalCount = 30;
    component.currentPage = pagesCount - 1;
    fixture.detectChanges();
    component.setNextPage();
    fixture.detectChanges();
    expect(component.currentPage).toEqual(pagesCount - 1);
  });

  it('should the setPreviousPage function decrement the value/currentPage', () => {
    let initialPage = 1;
    component.pagesCount = 3;
    component.totalCount = 30;
    component.currentPage = initialPage;
    fixture.detectChanges();
    component.setPreviousPage();
    fixture.detectChanges();
    expect(component.currentPage).toEqual(initialPage - 1);
  });

  it('should not the setPreviousPage function decrement the value/currentPage when it is equal to 0', () => {
    component.pagesCount = 3;
    component.totalCount = 30;
    component.currentPage = 0;
    fixture.detectChanges();
    component.setPreviousPage();
    fixture.detectChanges();
    expect(component.currentPage).toEqual(0);
  });

  it('should the setFirstPage set the value/currentPage to 0', () => {
    component.pagesCount = 3;
    component.totalCount = 30;
    component.currentPage = 1;
    fixture.detectChanges();
    component.setFirstPage();
    fixture.detectChanges();
    expect(component.currentPage).toEqual(0);
  });


  it('should the setLastPage set the value/currentPage to the value of pagesCount', () => {
    component.setLastPage();
    fixture.detectChanges();
    expect(component.currentPage).toEqual(component.pagesCount - 1);
  });

});
