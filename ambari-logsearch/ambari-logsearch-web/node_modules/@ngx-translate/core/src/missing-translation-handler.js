import { Injectable } from "@angular/core";
var MissingTranslationHandler = (function () {
    function MissingTranslationHandler() {
    }
    return MissingTranslationHandler;
}());
export { MissingTranslationHandler };
/**
 * This handler is just a placeholder that does nothing, in case you don't need a missing translation handler at all
 */
var FakeMissingTranslationHandler = (function () {
    function FakeMissingTranslationHandler() {
    }
    FakeMissingTranslationHandler.prototype.handle = function (params) {
        return params.key;
    };
    return FakeMissingTranslationHandler;
}());
export { FakeMissingTranslationHandler };
FakeMissingTranslationHandler.decorators = [
    { type: Injectable },
];
/** @nocollapse */
FakeMissingTranslationHandler.ctorParameters = function () { return []; };
