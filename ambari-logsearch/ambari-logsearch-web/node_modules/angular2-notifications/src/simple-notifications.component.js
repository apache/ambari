"use strict";
const core_1 = require('@angular/core');
const notifications_service_1 = require('./notifications.service');
class SimpleNotificationsComponent {
    constructor(_service) {
        this._service = _service;
        this.onCreate = new core_1.EventEmitter();
        this.onDestroy = new core_1.EventEmitter();
        this.notifications = [];
        this.position = ['bottom', 'right'];
        this.lastOnBottom = true;
        this.maxStack = 8;
        this.preventLastDuplicates = false;
        this.preventDuplicates = false;
        this.timeOut = 0;
        this.maxLength = 0;
        this.clickToClose = true;
        this.showProgressBar = true;
        this.pauseOnHover = true;
        this.theClass = '';
        this.rtl = false;
        this.animate = 'fromRight';
    }
    set options(opt) {
        this.attachChanges(opt);
    }
    ngOnInit() {
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
                    if (item.add)
                        this.add(item.notification);
                    else
                        this.defaultBehavior(item);
                    break;
                default:
                    this.defaultBehavior(item);
                    break;
            }
        });
    }
    defaultBehavior(value) {
        this.notifications.splice(this.notifications.indexOf(value.notification), 1);
        this.onDestroy.emit(this.buildEmit(value.notification, false));
    }
    add(item) {
        item.createdOn = new Date();
        let toBlock = this.preventLastDuplicates || this.preventDuplicates ? this.block(item) : false;
        this.lastNotificationCreated = item;
        if (!toBlock) {
            if (this.lastOnBottom) {
                if (this.notifications.length >= this.maxStack)
                    this.notifications.splice(0, 1);
                this.notifications.push(item);
            }
            else {
                if (this.notifications.length >= this.maxStack)
                    this.notifications.splice(this.notifications.length - 1, 1);
                this.notifications.splice(0, 0, item);
            }
            this.onCreate.emit(this.buildEmit(item, true));
        }
    }
    block(item) {
        let toCheck = item.html ? this.checkHtml : this.checkStandard;
        if (this.preventDuplicates && this.notifications.length > 0) {
            for (let i = 0; i < this.notifications.length; i++) {
                if (toCheck(this.notifications[i], item)) {
                    return true;
                }
            }
        }
        if (this.preventLastDuplicates) {
            let comp;
            if (this.preventLastDuplicates === 'visible' && this.notifications.length > 0) {
                if (this.lastOnBottom) {
                    comp = this.notifications[this.notifications.length - 1];
                }
                else {
                    comp = this.notifications[0];
                }
            }
            else if (this.preventLastDuplicates === 'all' && this.lastNotificationCreated) {
                comp = this.lastNotificationCreated;
            }
            else {
                return false;
            }
            return toCheck(comp, item);
        }
        return false;
    }
    checkStandard(checker, item) {
        return checker.type === item.type && checker.title === item.title && checker.content === item.content;
    }
    checkHtml(checker, item) {
        return checker.html ? checker.type === item.type && checker.title === item.title && checker.content === item.content && checker.html === item.html : false;
    }
    attachChanges(options) {
        Object.keys(options).forEach(a => {
            if (this.hasOwnProperty(a)) {
                this[a] = options[a];
            }
        });
    }
    buildEmit(notification, to) {
        let toEmit = {
            createdOn: notification.createdOn,
            type: notification.type,
            icon: notification.icon,
            id: notification.id
        };
        if (notification.html) {
            toEmit.html = notification.html;
        }
        else {
            toEmit.title = notification.title;
            toEmit.content = notification.content;
        }
        if (!to) {
            toEmit.destroyedOn = new Date();
        }
        return toEmit;
    }
    cleanSingle(id) {
        let indexOfDelete = 0;
        let doDelete = false;
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
    ngOnDestroy() {
        if (this.listener) {
            this.listener.unsubscribe();
        }
    }
}
SimpleNotificationsComponent.decorators = [
    { type: core_1.Component, args: [{
                selector: 'simple-notifications',
                encapsulation: core_1.ViewEncapsulation.None,
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
            },] },
];
SimpleNotificationsComponent.ctorParameters = [
    { type: notifications_service_1.NotificationsService, },
];
SimpleNotificationsComponent.propDecorators = {
    'options': [{ type: core_1.Input },],
    'onCreate': [{ type: core_1.Output },],
    'onDestroy': [{ type: core_1.Output },],
};
exports.SimpleNotificationsComponent = SimpleNotificationsComponent;
//# sourceMappingURL=simple-notifications.component.js.map