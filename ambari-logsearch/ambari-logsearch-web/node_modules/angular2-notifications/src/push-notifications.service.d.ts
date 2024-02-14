import { PushNotification, Permission } from './push-notification.type';
export declare class PushNotificationsService {
    permission: Permission;
    constructor();
    requestPermission(): void;
    isSupported(): boolean;
    create(title: string, options?: PushNotification): any;
}
