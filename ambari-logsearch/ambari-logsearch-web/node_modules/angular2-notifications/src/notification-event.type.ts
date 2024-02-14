import {Notification} from './notification.type';

export interface NotificationEvent {
  add?: boolean;
  command: string;
  id?: string;
  notification?: Notification;
}
