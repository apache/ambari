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

import { NgModule, APP_INITIALIZER } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

import { AppLoadService } from './services/app-load.service';

export function check_if_authorized(appLoadService: AppLoadService) {
  return () => appLoadService.syncAuthorizedStateWithBackend();
}
export function set_translation_service(appLoadService: AppLoadService) {
  return () => appLoadService.setTranslationService();
}

@NgModule({
  imports: [HttpClientModule],
  providers: [
    AppLoadService,
    { provide: APP_INITIALIZER, useFactory: set_translation_service, deps: [AppLoadService], multi: true },
    { provide: APP_INITIALIZER, useFactory: check_if_authorized, deps: [AppLoadService], multi: true }
  ]
})
export class AppLoadModule { }
