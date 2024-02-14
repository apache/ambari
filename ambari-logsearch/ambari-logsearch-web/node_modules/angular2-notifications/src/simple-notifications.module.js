"use strict";
const core_1 = require('@angular/core');
const common_1 = require('@angular/common');
const notifications_service_1 = require('./notifications.service');
const simple_notifications_component_1 = require('./simple-notifications.component');
const notification_component_1 = require('./notification.component');
const max_pipe_1 = require('./max.pipe');
class SimpleNotificationsModule {
}
SimpleNotificationsModule.decorators = [
    { type: core_1.NgModule, args: [{
                imports: [common_1.CommonModule],
                declarations: [simple_notifications_component_1.SimpleNotificationsComponent, notification_component_1.NotificationComponent, max_pipe_1.MaxPipe],
                providers: [notifications_service_1.NotificationsService],
                exports: [simple_notifications_component_1.SimpleNotificationsComponent]
            },] },
];
SimpleNotificationsModule.ctorParameters = [];
exports.SimpleNotificationsModule = SimpleNotificationsModule;
//# sourceMappingURL=simple-notifications.module.js.map