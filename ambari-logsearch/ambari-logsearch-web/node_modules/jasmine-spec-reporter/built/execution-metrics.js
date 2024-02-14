"use strict";
var ExecutionMetrics = (function () {
    function ExecutionMetrics() {
        this.successfulSpecs = 0;
        this.failedSpecs = 0;
        this.pendingSpecs = 0;
        this.skippedSpecs = 0;
        this.totalSpecsDefined = 0;
        this.executedSpecs = 0;
        this.random = false;
    }
    ExecutionMetrics.pluralize = function (count) {
        return count > 1 ? "s" : "";
    };
    ExecutionMetrics.prototype.start = function (info) {
        this.startTime = (new Date()).getTime();
        this.totalSpecsDefined = info && info.totalSpecsDefined ? info.totalSpecsDefined : 0;
    };
    ExecutionMetrics.prototype.stop = function (info) {
        var totalSpecs = this.failedSpecs + this.successfulSpecs + this.pendingSpecs;
        this.duration = this.formatDuration((new Date()).getTime() - this.startTime);
        this.executedSpecs = this.failedSpecs + this.successfulSpecs;
        this.totalSpecsDefined = this.totalSpecsDefined ? this.totalSpecsDefined : totalSpecs;
        this.skippedSpecs = this.totalSpecsDefined - totalSpecs;
        this.random = info && info.order && info.order.random;
        this.seed = info && info.order && info.order.seed;
    };
    ExecutionMetrics.prototype.startSpec = function () {
        this.specStartTime = (new Date()).getTime();
    };
    ExecutionMetrics.prototype.stopSpec = function (spec) {
        spec.duration = this.formatDuration((new Date()).getTime() - this.specStartTime);
    };
    ExecutionMetrics.prototype.formatDuration = function (durationInMs) {
        var duration = "";
        var durationInSecs = durationInMs / 1000;
        var durationInMins;
        var durationInHrs;
        if (durationInSecs < 1) {
            return durationInSecs + " sec" + ExecutionMetrics.pluralize(durationInSecs);
        }
        durationInSecs = Math.round(durationInSecs);
        if (durationInSecs < 60) {
            return durationInSecs + " sec" + ExecutionMetrics.pluralize(durationInSecs);
        }
        durationInMins = Math.floor(durationInSecs / 60);
        durationInSecs = durationInSecs % 60;
        if (durationInSecs) {
            duration = " " + durationInSecs + " sec" + ExecutionMetrics.pluralize(durationInSecs);
        }
        if (durationInMins < 60) {
            return durationInMins + " min" + ExecutionMetrics.pluralize(durationInMins) + duration;
        }
        durationInHrs = Math.floor(durationInMins / 60);
        durationInMins = durationInMins % 60;
        if (durationInMins) {
            duration = " " + durationInMins + " min" + ExecutionMetrics.pluralize(durationInMins) + duration;
        }
        return durationInHrs + " hour" + ExecutionMetrics.pluralize(durationInHrs) + duration;
    };
    return ExecutionMetrics;
}());
exports.ExecutionMetrics = ExecutionMetrics;
//# sourceMappingURL=execution-metrics.js.map