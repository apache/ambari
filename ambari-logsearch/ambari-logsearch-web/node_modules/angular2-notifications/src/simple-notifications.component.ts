import {Component, EventEmitter, OnInit, OnDestroy, ViewEncapsulation, Input, Output} from '@angular/core';
import {Notification} from './notification.type';
import {NotificationsService} from './notifications.service';
import {Options} from './options.type';
import {Subscription} from 'rxjs/Subscription';

@Component({
    selector: 'simple-notifications',
    encapsulation: ViewEncapsulation.None,
    template: `
        <div class="simple-notification-wrapper" [ngClass]="position">
            <simple-notification
                *ngFor="let a of notifications; let i = index"
                [item]="a"
                [timeOut]="timeOut"
                [clickToClose]="clickToClose"
                [maxLength]="maxLength"
                [showProgressBar]="showProgressBar"
                [pauseOnHover]="pauseOnHover"
                [theClass]="theClass"
                [rtl]="rtl"
                [animate]="animate"
                [position]="i"
                >
            </simple-notification>
        </div>
    `,
    styles: [`
        .simple-notification-wrapper {
            position: fixed;
            width: 300px;
            z-index: 1000;
        }
        
        .simple-notification-wrapper.left { left: 20px; }
        .simple-notification-wrapper.top { top: 20px; }
        .simple-notification-wrapper.right { right: 20px; }
        .simple-notification-wrapper.bottom { bottom: 20px; }
        
        @media (max-width: 340px) {
            .simple-notification-wrapper {
                width: auto;
                left: 20px;
                right: 20px;
            }
        }
    `]
})

export class SimpleNotificationsComponent implements OnInit, OnDestroy {

    @Input() set options(opt: Options) {
        this.attachChanges(opt);
    }

    @Output() onCreate = new EventEmitter();
    @Output() onDestroy = new EventEmitter();

    public notifications: Notification[] = [];
    public position: ['top' | 'bottom', 'right' | 'left'] = ['bottom', 'right'];

    private lastNotificationCreated: Notification;
    private listener: Subscription;

    // Received values
    private lastOnBottom: boolean = true;
    private maxStack: number = 8;
    private preventLastDuplicates: any = false;
    private preventDuplicates: boolean = false;

    // Sent values
    private timeOut: number = 0;
    private maxLength: number = 0;
    private clickToClose: boolean = true;
    private showProgressBar: boolean = true;
    private pauseOnHover: boolean = true;
    private theClass: string = '';
    private rtl: boolean = false;
    private animate: 'fromRight' | 'fromLeft' | 'rotate' | 'scale' = 'fromRight';

    constructor(private _service: NotificationsService) {}

    ngOnInit(): void {
        // Listen for changes in the service
        this.listener = this._service.getChangeEmitter()
            .subscribe(item => {
                switch (item.command) {
                    case 'cleanAll':
                        this.notifications = [];
                        break;

                    case 'clean':
                        this.cleanSingle(item.id);
                        break;

                    case 'set':
                        if (item.add) this.add(item.notification);
                        else this.defaultBehavior(item);
                        break;

                    default:
                        this.defaultBehavior(item);
                        break;
                }
            });
    }

    // Default behavior on event
    defaultBehavior(value: any): void {
        this.notifications.splice(this.notifications.indexOf(value.notification), 1);
        this.onDestroy.emit(this.buildEmit(value.notification, false));
    }


    // Add the new notification to the notification array
    add(item: Notification): void {
        item.createdOn = new Date();

        let toBlock: boolean = this.preventLastDuplicates || this.preventDuplicates ? this.block(item) : false;

        // Save this as the last created notification
        this.lastNotificationCreated = item;

        if (!toBlock) {
            // Check if the notification should be added at the start or the end of the array
            if (this.lastOnBottom) {
                if (this.notifications.length >= this.maxStack) this.notifications.splice(0, 1);
                this.notifications.push(item);
            } else {
                if (this.notifications.length >= this.maxStack) this.notifications.splice(this.notifications.length - 1, 1);
                this.notifications.splice(0, 0, item);
            }

            this.onCreate.emit(this.buildEmit(item, true));
        }
    }

    // Check if notifications should be prevented
    block(item: Notification): boolean {

        let toCheck = item.html ? this.checkHtml : this.checkStandard;

        if (this.preventDuplicates && this.notifications.length > 0) {
            for (let i = 0; i < this.notifications.length; i++) {
                if (toCheck(this.notifications[i], item)) {
                    return true;
                }
            }
        }

        if (this.preventLastDuplicates) {

            let comp: Notification;

            if (this.preventLastDuplicates === 'visible' && this.notifications.length > 0) {
                if (this.lastOnBottom) {
                    comp = this.notifications[this.notifications.length - 1];
                } else {
                    comp = this.notifications[0];
                }
            } else if (this.preventLastDuplicates === 'all' && this.lastNotificationCreated) {
                comp = this.lastNotificationCreated;
            } else {
                return false;
            }
            return toCheck(comp, item);
        }

        return false;
    }

    checkStandard(checker: Notification, item: Notification): boolean {
        return checker.type === item.type && checker.title === item.title && checker.content === item.content;
    }

    checkHtml(checker: Notification, item: Notification): boolean {
        return checker.html ? checker.type === item.type && checker.title === item.title && checker.content === item.content && checker.html === item.html : false;
    }

    // Attach all the changes received in the options object
    attachChanges(options: any): void {
        Object.keys(options).forEach(a => {
            if (this.hasOwnProperty(a)) {
                this[a] = options[a];
            }
        });
    }

    buildEmit(notification: Notification, to: boolean) {
        let toEmit: Notification = {
            createdOn: notification.createdOn,
            type: notification.type,
            icon: notification.icon,
            id: notification.id
        };

        if (notification.html) {
            toEmit.html = notification.html;
        } else {
            toEmit.title = notification.title;
            toEmit.content = notification.content;
        }

        if (!to) {
            toEmit.destroyedOn = new Date();
        }

        return toEmit;
    }

    cleanSingle(id: string): void {
        let indexOfDelete: number = 0;
        let doDelete: boolean = false;

        this.notifications.forEach((notification, idx) => {
            if (notification.id === id) {
                indexOfDelete = idx;
                doDelete = true;
            }
        });

        if (doDelete) {
            this.notifications.splice(indexOfDelete, 1);
        }
    }

    ngOnDestroy(): void {
        if (this.listener) {
            this.listener.unsubscribe();
        }
    }
}
