"use strict";
const core_1 = require('@angular/core');
const Subject_1 = require('rxjs/Subject');
const icons_1 = require('./icons');
class NotificationsService {
    constructor() {
        this.emitter = new Subject_1.Subject();
        this.icons = icons_1.defaultIcons;
    }
    set(notification, to) {
        notification.id = notification.override && notification.override.id ? notification.override.id : Math.random().toString(36).substring(3);
        notification.click = new core_1.EventEmitter();
        this.emitter.next({ command: 'set', notification: notification, add: to });
        return notification;
    }
    ;
    getChangeEmitter() {
        return this.emitter;
    }
    success(title, content, override) {
        return this.set({
            title: title,
            content: content,
            type: 'success',
            icon: this.icons.success,
            override: override
        }, true);
    }
    error(title, content, override) {
        return this.set({ title: title, content: content, type: 'error', icon: this.icons.error, override: override }, true);
    }
    alert(title, content, override) {
        return this.set({ title: title, content: content, type: 'alert', icon: this.icons.alert, override: override }, true);
    }
    info(title, content, override) {
        return this.set({ title: title, content: content, type: 'info', icon: this.icons.info, override: override }, true);
    }
    bare(title, content, override) {
        return this.set({ title: title, content: content, type: 'bare', icon: 'bare', override: override }, true);
    }
    create(title, content, type, override) {
        return this.set({ title: title, content: content, type: type, icon: 'bare', override: override }, true);
    }
    html(html, type, override) {
        return this.set({ html: html, type: type, icon: 'bare', override: override, title: null, content: null }, true);
    }
    remove(id) {
        if (id)
            this.emitter.next({ command: 'clean', id: id });
        else
            this.emitter.next({ command: 'cleanAll' });
    }
}
NotificationsService.decorators = [
    { type: core_1.Injectable },
];
NotificationsService.ctorParameters = [];
exports.NotificationsService = NotificationsService;
//# sourceMappingURL=notifications.service.js.map