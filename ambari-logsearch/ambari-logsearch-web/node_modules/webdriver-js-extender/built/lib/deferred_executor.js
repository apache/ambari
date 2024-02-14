"use strict";
/*
 * Wraps a promised {@link Executor}, ensuring no commands are executed until
 * the wrapped executor has been fully resolved.
 *
 * selenium-webdriver uses this internally, and we overwrite it to give it the
 * defineCommand() function
 *
 * Based off of
 * https://github.com/SeleniumHQ/selenium/blob/master/javascript/node/selenium-webdriver/lib/command.js#L240
 *
 * @implements {Executor}
 */
var DeferredExecutor = (function () {
    /**
     * @param {!Promise<Executor>} delegate The promised delegate, which
     *     may be provided by any promise-like thenable object.
     */
    function DeferredExecutor(delegate) {
        /** @override */
        this.execute = function (command) {
            return delegate.then(function (executor) {
                return executor.execute(command);
            });
        };
        this.defineCommand = function (name, method, path) {
            delegate.then(function (executor) {
                executor.defineCommand(name, method, path);
            });
        };
    }
    return DeferredExecutor;
}());
exports.DeferredExecutor = DeferredExecutor;
//# sourceMappingURL=deferred_executor.js.map