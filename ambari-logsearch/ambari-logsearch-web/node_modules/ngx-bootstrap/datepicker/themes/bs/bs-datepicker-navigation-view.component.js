import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { BsNavigationDirection } from '../../models/index';
var BsDatepickerNavigationViewComponent = (function () {
    function BsDatepickerNavigationViewComponent() {
        this.onNavigate = new EventEmitter();
        this.onViewMode = new EventEmitter();
    }
    BsDatepickerNavigationViewComponent.prototype.navTo = function (down) {
        this.onNavigate.emit(down
            ? BsNavigationDirection.DOWN
            : BsNavigationDirection.UP);
    };
    BsDatepickerNavigationViewComponent.prototype.view = function (viewMode) {
        this.onViewMode.emit(viewMode);
    };
    BsDatepickerNavigationViewComponent.decorators = [
        { type: Component, args: [{
                    selector: 'bs-datepicker-navigation-view',
                    changeDetection: ChangeDetectionStrategy.OnPush,
                    template: "\n    <button class=\"previous\"\n            [disabled]=\"calendar.disableLeftArrow\"\n            [style.visibility]=\"calendar.hideLeftArrow ? 'hidden' : 'visible'\"\n            (click)=\"navTo(true)\"><span>&lsaquo;</span>\n    </button>\n\n    <button class=\"current\"\n            *ngIf=\"calendar.monthTitle\"\n            (click)=\"view('month')\"\n    ><span>{{ calendar.monthTitle }}</span>\n    </button>\n\n    <button class=\"current\" (click)=\"view('year')\"\n    ><span>{{ calendar.yearTitle }}</span></button>\n\n    <button class=\"next\"\n            [disabled]=\"calendar.disableRightArrow\"\n            [style.visibility]=\"calendar.hideRightArrow ? 'hidden' : 'visible'\"\n            (click)=\"navTo(false)\"><span>&rsaquo;</span>\n    </button>\n  "
                },] },
    ];
    /** @nocollapse */
    BsDatepickerNavigationViewComponent.ctorParameters = function () { return []; };
    BsDatepickerNavigationViewComponent.propDecorators = {
        'calendar': [{ type: Input },],
        'onNavigate': [{ type: Output },],
        'onViewMode': [{ type: Output },],
    };
    return BsDatepickerNavigationViewComponent;
}());
export { BsDatepickerNavigationViewComponent };
//# sourceMappingURL=bs-datepicker-navigation-view.component.js.map