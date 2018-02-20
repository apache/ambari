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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TakeUntilDestroy} from "angular2-take-until-destroy";

@Component({
  selector: 'main-container',
  templateUrl: './main-container.component.html',
  styleUrls: ['./main-container.component.less']
})
@TakeUntilDestroy
export class MainContainerComponent implements OnInit, OnDestroy{

  componentDestroy;
  constructor(private appState: AppStateService) {}

  ngOnInit() {
    this.appState.getParameter('isAuthorized').takeUntil(this.componentDestroy())
      .subscribe((value: boolean) => this.isAuthorized = value);
    this.appState.getParameter('isInitialLoading').takeUntil(this.componentDestroy())
      .subscribe((value: boolean) => this.isInitialLoading = value);
  }

  ngOnDestroy() {}

  isAuthorized: boolean = false;

  isInitialLoading: boolean = false;

}
