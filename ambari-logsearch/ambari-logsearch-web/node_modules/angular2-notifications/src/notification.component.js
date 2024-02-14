"use strict";
const core_1 = require('@angular/core');
const platform_browser_1 = require('@angular/platform-browser');
const notifications_service_1 = require('./notifications.service');
class NotificationComponent {
    constructor(notificationService, domSanitizer, zone) {
        this.notificationService = notificationService;
        this.domSanitizer = domSanitizer;
        this.zone = zone;
        this.progressWidth = 0;
        this.stopTime = false;
        this.count = 0;
        this.instance = () => {
            this.zone.runOutsideAngular(() => {
                this.zone.run(() => this.diff = (new Date().getTime() - this.start) - (this.count * this.speed));
                if (this.count++ === this.steps)
                    this.zone.run(() => this.remove());
                else if (!this.stopTime) {
                    if (this.showProgressBar)
                        this.zone.run(() => this.progressWidth += 100 / this.steps);
                    this.timer = setTimeout(this.instance, (this.speed - this.diff));
                }
            });
        };
    }
    ngOnInit() {
        if (this.animate) {
            this.item.state = this.animate;
        }
        if (this.item.override) {
            this.attachOverrides();
        }
        if (this.timeOut !== 0) {
            this.startTimeOut();
        }
        this.safeSvg = this.domSanitizer.bypassSecurityTrustHtml(this.item.icon);
    }
    startTimeOut() {
        this.steps = this.timeOut / 10;
        this.speed = this.timeOut / this.steps;
        this.start = new Date().getTime();
        this.zone.runOutsideAngular(() => this.timer = setTimeout(this.instance, this.speed));
    }
    onEnter() {
        if (this.pauseOnHover) {
            this.stopTime = true;
        }
    }
    onLeave() {
        if (this.pauseOnHover) {
            this.stopTime = false;
            setTimeout(this.instance, (this.speed - this.diff));
        }
    }
    setPosition() {
        return this.position !== 0 ? this.position * 90 : 0;
    }
    onClick($e) {
        this.item.click.emit($e);
        if (this.clickToClose) {
            this.remove();
        }
    }
    attachOverrides() {
        Object.keys(this.item.override).forEach(a => {
            if (this.hasOwnProperty(a)) {
                this[a] = this.item.override[a];
            }
        });
    }
    ngOnDestroy() {
        clearTimeout(this.timer);
    }
    remove() {
        if (this.animate) {
            this.item.state = this.animate + 'Out';
            this.zone.runOutsideAngular(() => {
                setTimeout(() => {
                    this.zone.run(() => this.notificationService.set(this.item, false));
                }, 310);
            });
        }
        else {
            this.notificationService.set(this.item, false);
        }
    }
}
NotificationComponent.decorators = [
    { type: core_1.Component, args: [{
                selector: 'simple-notification',
                encapsulation: core_1.ViewEncapsulation.None,
                animations: [
                    core_1.trigger('enterLeave', [
                        core_1.state('fromRight', core_1.style({ opacity: 1, transform: 'translateX(0)' })),
                        core_1.transition('* => fromRight', [
                            core_1.style({ opacity: 0, transform: 'translateX(5%)' }),
                            core_1.animate('400ms ease-in-out')
                        ]),
                        core_1.state('fromRightOut', core_1.style({ opacity: 0, transform: 'translateX(-5%)' })),
                        core_1.transition('fromRight => fromRightOut', [
                            core_1.style({ opacity: 1, transform: 'translateX(0)' }),
                            core_1.animate('300ms ease-in-out')
                        ]),
                        core_1.state('fromLeft', core_1.style({ opacity: 1, transform: 'translateX(0)' })),
                        core_1.transition('* => fromLeft', [
                            core_1.style({ opacity: 0, transform: 'translateX(-5%)' }),
                            core_1.animate('400ms ease-in-out')
                        ]),
                        core_1.state('fromLeftOut', core_1.style({ opacity: 0, transform: 'translateX(5%)' })),
                        core_1.transition('fromLeft => fromLeftOut', [
                            core_1.style({ opacity: 1, transform: 'translateX(0)' }),
                            core_1.animate('300ms ease-in-out')
                        ]),
                        core_1.state('scale', core_1.style({ opacity: 1, transform: 'scale(1)' })),
                        core_1.transition('* => scale', [
                            core_1.style({ opacity: 0, transform: 'scale(0)' }),
                            core_1.animate('400ms ease-in-out')
                        ]),
                        core_1.state('scaleOut', core_1.style({ opacity: 0, transform: 'scale(0)' })),
                        core_1.transition('scale => scaleOut', [
                            core_1.style({ opacity: 1, transform: 'scale(1)' }),
                            core_1.animate('400ms ease-in-out')
                        ]),
                        core_1.state('rotate', core_1.style({ opacity: 1, transform: 'rotate(0deg)' })),
                        core_1.transition('* => rotate', [
                            core_1.style({ opacity: 0, transform: 'rotate(5deg)' }),
                            core_1.animate('400ms ease-in-out')
                        ]),
                        core_1.state('rotateOut', core_1.style({ opacity: 0, transform: 'rotate(-5deg)' })),
                        core_1.transition('rotate => rotateOut', [
                            core_1.style({ opacity: 1, transform: 'rotate(0deg)' }),
                            core_1.animate('400ms ease-in-out')
                        ])
                    ])
                ],
                template: `
        <div class="simple-notification"
            [@enterLeave]="item.state"
            (click)="onClick($e)"
            [class]="theClass"

            [ngClass]="{
                'alert': item.type === 'alert',
                'error': item.type === 'error',
                'success': item.type === 'success',
                'info': item.type === 'info',
                'bare': item.type === 'bare',
                'rtl-mode': rtl
            }"

            (mouseenter)="onEnter()"
            (mouseleave)="onLeave()">

            <div *ngIf="!item.html">
                <div class="sn-title">{{item.title}}</div>
                <div class="sn-content">{{item.content | max:maxLength}}</div>

                <div *ngIf="item.type !== 'bare'" [innerHTML]="safeSvg"></div>
            </div>
            <div *ngIf="item.html" [innerHTML]="item.html"></div>

            <div class="sn-progress-loader" *ngIf="showProgressBar">
                <span [ngStyle]="{'width': progressWidth + '%'}"></span>
            </div>

        </div>
    `,
                styles: [`
        .simple-notification {
            width: 100%;
            padding: 10px 20px;
            box-sizing: border-box;
            position: relative;
            float: left;
            margin-bottom: 10px;
            color: #fff;
            cursor: pointer;
            transition: all 0.5s;
        }

        .simple-notification .sn-title {
            margin: 0;
            padding: 0 50px 0 0;
            line-height: 30px;
            font-size: 20px;
        }

        .simple-notification .sn-content {
            margin: 0;
            font-size: 16px;
            padding: 0 50px 0 0;
            line-height: 20px;
        }

        .simple-notification svg {
            position: absolute;
            box-sizing: border-box;
            top: 0;
            right: 0;
            width: 70px;
            height: 70px;
            padding: 10px;
            fill: #fff;
        }

        .simple-notification.rtl-mode {
            direction: rtl;
        }

        .simple-notification.rtl-mode .sn-content {
            padding: 0 0 0 50px;
        }

        .simple-notification.rtl-mode svg {
            left: 0;
            right: auto;
        }

        .simple-notification.error { background: #F44336; }
        .simple-notification.success { background: #8BC34A; }
        .simple-notification.alert { background: #ffdb5b; }
        .simple-notification.info { background: #03A9F4; }

        .simple-notification .sn-progress-loader {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 5px;
        }

        .simple-notification .sn-progress-loader span {
            float: left;
            height: 100%;
        }

        .simple-notification.success .sn-progress-loader span { background: #689F38; }
        .simple-notification.error .sn-progress-loader span { background: #D32F2F; }
        .simple-notification.alert .sn-progress-loader span { background: #edc242; }
        .simple-notification.info .sn-progress-loader span { background: #0288D1; }
        .simple-notification.bare .sn-progress-loader span { background: #ccc; }
    `]
            },] },
];
NotificationComponent.ctorParameters = [
    { type: notifications_service_1.NotificationsService, },
    { type: platform_browser_1.DomSanitizer, },
    { type: core_1.NgZone, },
];
NotificationComponent.propDecorators = {
    'timeOut': [{ type: core_1.Input },],
    'showProgressBar': [{ type: core_1.Input },],
    'pauseOnHover': [{ type: core_1.Input },],
    'clickToClose': [{ type: core_1.Input },],
    'maxLength': [{ type: core_1.Input },],
    'theClass': [{ type: core_1.Input },],
    'rtl': [{ type: core_1.Input },],
    'animate': [{ type: core_1.Input },],
    'position': [{ type: core_1.Input },],
    'item': [{ type: core_1.Input },],
};
exports.NotificationComponent = NotificationComponent;
//# sourceMappingURL=notification.component.js.map