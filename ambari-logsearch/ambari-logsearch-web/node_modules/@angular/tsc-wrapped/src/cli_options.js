"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
var CliOptions = (function () {
    function CliOptions(_a) {
        var _b = _a.basePath, basePath = _b === void 0 ? null : _b;
        this.basePath = basePath;
    }
    return CliOptions;
}());
exports.CliOptions = CliOptions;
var I18nExtractionCliOptions = (function (_super) {
    __extends(I18nExtractionCliOptions, _super);
    function I18nExtractionCliOptions(_a) {
        var _b = _a.i18nFormat, i18nFormat = _b === void 0 ? null : _b, _c = _a.locale, locale = _c === void 0 ? null : _c, _d = _a.outFile, outFile = _d === void 0 ? null : _d;
        var _this = _super.call(this, {}) || this;
        _this.i18nFormat = i18nFormat;
        _this.locale = locale;
        _this.outFile = outFile;
        return _this;
    }
    return I18nExtractionCliOptions;
}(CliOptions));
exports.I18nExtractionCliOptions = I18nExtractionCliOptions;
var NgcCliOptions = (function (_super) {
    __extends(NgcCliOptions, _super);
    function NgcCliOptions(_a) {
        var _b = _a.i18nFormat, i18nFormat = _b === void 0 ? null : _b, _c = _a.i18nFile, i18nFile = _c === void 0 ? null : _c, _d = _a.locale, locale = _d === void 0 ? null : _d, _e = _a.missingTranslation, missingTranslation = _e === void 0 ? null : _e, _f = _a.basePath, basePath = _f === void 0 ? null : _f;
        var _this = _super.call(this, { basePath: basePath }) || this;
        _this.i18nFormat = i18nFormat;
        _this.i18nFile = i18nFile;
        _this.locale = locale;
        _this.missingTranslation = missingTranslation;
        return _this;
    }
    return NgcCliOptions;
}(CliOptions));
exports.NgcCliOptions = NgcCliOptions;
//# sourceMappingURL=cli_options.js.map