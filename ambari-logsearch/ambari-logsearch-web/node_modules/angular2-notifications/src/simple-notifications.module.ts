import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NotificationsService} from './notifications.service';
import {SimpleNotificationsComponent} from './simple-notifications.component';
import {NotificationComponent} from './notification.component';
import {MaxPipe} from './max.pipe';

@NgModule({
  imports: [CommonModule],
  declarations: [SimpleNotificationsComponent, NotificationComponent, MaxPipe],
  providers: [NotificationsService],
  exports: [SimpleNotificationsComponent]
})
export class SimpleNotificationsModule {
}
