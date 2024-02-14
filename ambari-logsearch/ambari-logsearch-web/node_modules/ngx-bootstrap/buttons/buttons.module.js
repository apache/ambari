import { NgModule } from '@angular/core';
import { ButtonCheckboxDirective } from './button-checkbox.directive';
import { ButtonRadioDirective } from './button-radio.directive';
var ButtonsModule = (function () {
    function ButtonsModule() {
    }
    ButtonsModule.forRoot = function () {
        return { ngModule: ButtonsModule, providers: [] };
    };
    ButtonsModule.decorators = [
        { type: NgModule, args: [{
                    declarations: [ButtonCheckboxDirective, ButtonRadioDirective],
                    exports: [ButtonCheckboxDirective, ButtonRadioDirective]
                },] },
    ];
    /** @nocollapse */
    ButtonsModule.ctorParameters = function () { return []; };
    return ButtonsModule;
}());
export { ButtonsModule };
//# sourceMappingURL=buttons.module.js.map