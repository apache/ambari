import { ChangeDetectionStrategy, Input, Component } from '@angular/core';
import { PopoverConfig } from './popover.config';
import { isBs3 } from '../utils/ng2-bootstrap-config';
var PopoverContainerComponent = (function () {
    function PopoverContainerComponent(config) {
        Object.assign(this, config);
    }
    Object.defineProperty(PopoverContainerComponent.prototype, "isBs3", {
        get: function () {
            return isBs3();
        },
        enumerable: true,
        configurable: true
    });
    PopoverContainerComponent.decorators = [
        { type: Component, args: [{
                    selector: 'popover-container',
                    changeDetection: ChangeDetectionStrategy.OnPush,
                    // tslint:disable-next-line
                    host: {
                        '[class]': '"popover in popover-" + placement + " " + "bs-popover-" + placement + " " + placement + " " + containerClass',
                        '[class.show]': '!isBs3',
                        role: 'tooltip',
                        style: 'display:block;'
                    },
                    styles: ["\n    :host.bs-popover-top .arrow, :host.bs-popover-bottom .arrow {\n      left: calc(50% - 5px);\n    }\n    :host.bs-popover-left .arrow, :host.bs-popover-right .arrow {\n      top: calc(50% - 2.5px);\n    }\n  "],
                    template: "\n<div class=\"popover-arrow arrow\"></div>\n<h3 class=\"popover-title popover-header\" *ngIf=\"title\">{{title}}</h3><div class=\"popover-content popover-body\"><ng-content></ng-content></div>\n    "
                },] },
    ];
    /** @nocollapse */
    PopoverContainerComponent.ctorParameters = function () { return [
        { type: PopoverConfig, },
    ]; };
    PopoverContainerComponent.propDecorators = {
        'placement': [{ type: Input },],
        'title': [{ type: Input },],
    };
    return PopoverContainerComponent;
}());
export { PopoverContainerComponent };
//# sourceMappingURL=popover-container.component.js.map