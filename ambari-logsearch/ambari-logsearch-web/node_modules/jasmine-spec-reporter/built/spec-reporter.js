"use strict";
var configuration_parser_1 = require("./configuration-parser");
var execution_display_1 = require("./execution-display");
var execution_metrics_1 = require("./execution-metrics");
var SpecReporter = (function () {
    function SpecReporter(configuration) {
        this.started = false;
        this.finished = false;
        this.configuration = configuration_parser_1.ConfigurationParser.parse(configuration);
        this.display = new execution_display_1.ExecutionDisplay(this.configuration);
        this.metrics = new execution_metrics_1.ExecutionMetrics();
    }
    SpecReporter.prototype.jasmineStarted = function (info) {
        this.started = true;
        this.metrics.start(info);
        this.display.jasmineStarted(info);
    };
    SpecReporter.prototype.jasmineDone = function (info) {
        this.metrics.stop(info);
        this.display.summary(this.metrics);
        this.finished = true;
    };
    SpecReporter.prototype.suiteStarted = function (suite) {
        this.display.suiteStarted(suite);
    };
    SpecReporter.prototype.suiteDone = function () {
        this.display.suiteDone();
    };
    SpecReporter.prototype.specStarted = function (spec) {
        this.metrics.startSpec();
        this.display.specStarted(spec);
    };
    SpecReporter.prototype.specDone = function (spec) {
        this.metrics.stopSpec(spec);
        if (spec.status === "pending") {
            this.metrics.pendingSpecs++;
            this.display.pending(spec);
        }
        else if (spec.status === "passed") {
            this.metrics.successfulSpecs++;
            this.display.successful(spec);
        }
        else if (spec.status === "failed") {
            this.metrics.failedSpecs++;
            this.display.failed(spec);
        }
    };
    return SpecReporter;
}());
exports.SpecReporter = SpecReporter;
//# sourceMappingURL=spec-reporter.js.map