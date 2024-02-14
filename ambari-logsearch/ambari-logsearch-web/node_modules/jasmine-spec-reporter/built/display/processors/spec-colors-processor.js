"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var display_processor_1 = require("../display-processor");
var SpecColorsProcessor = (function (_super) {
    __extends(SpecColorsProcessor, _super);
    function SpecColorsProcessor() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    SpecColorsProcessor.prototype.displaySuccessfulSpec = function (spec, log) {
        return log.successful;
    };
    SpecColorsProcessor.prototype.displayFailedSpec = function (spec, log) {
        return log.failed;
    };
    SpecColorsProcessor.prototype.displayPendingSpec = function (spec, log) {
        return log.pending;
    };
    return SpecColorsProcessor;
}(display_processor_1.DisplayProcessor));
exports.SpecColorsProcessor = SpecColorsProcessor;
//# sourceMappingURL=spec-colors-processor.js.map