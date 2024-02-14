var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
import { Observable } from "rxjs/Observable";
import { Injectable } from "@angular/core";
var TranslateLoader = (function () {
    function TranslateLoader() {
    }
    return TranslateLoader;
}());
export { TranslateLoader };
/**
 * This loader is just a placeholder that does nothing, in case you don't need a loader at all
 */
var TranslateFakeLoader = (function (_super) {
    __extends(TranslateFakeLoader, _super);
    function TranslateFakeLoader() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    TranslateFakeLoader.prototype.getTranslation = function (lang) {
        return Observable.of({});
    };
    return TranslateFakeLoader;
}(TranslateLoader));
export { TranslateFakeLoader };
TranslateFakeLoader.decorators = [
    { type: Injectable },
];
/** @nocollapse */
TranslateFakeLoader.ctorParameters = function () { return []; };
