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

import {Injectable} from '@angular/core';

import {NotificationsService as Angular2NotificationsService} from 'angular2-notifications';
import {Notification} from 'angular2-notifications/src/notification.type';

import {NotificationInterface} from '../interfaces/notification.interface';
import {Icons, defaultIcons} from 'angular2-notifications/src/icons';
import {TranslateService} from '@ngx-translate/core';

export enum NotificationType {
  SUCCESS = 'success',
  INFO = 'info',
  ERROR = 'error',
  ALERT = 'alert'
}

export const notificationIcons: Icons = {
  success: `<i class="fa fa-check"></i>`,
  info: `<i class="fa fa-info-circle"></i>`,
  error: `<i class="fa fa-exclamation-circle"></i>`,
  alert: `<i class="fa fa-bell"></i>`
};
Object.assign(defaultIcons, notificationIcons);

@Injectable()
export class NotificationService {

  constructor(
    private notificationService: Angular2NotificationsService,
    private translateService: TranslateService
  ) { }

  addNotification(payload: NotificationInterface): Notification {
    const {message, title, ...config} = payload;
    const method: string = typeof this.notificationService[config.type] === 'function' ? config.type : 'info';
    return this.notificationService[method](
      this.translateService.instant(title),
      this.translateService.instant(message),
      {...config, icon: notificationIcons[method] || notificationIcons['info']}
    );
  }

}
