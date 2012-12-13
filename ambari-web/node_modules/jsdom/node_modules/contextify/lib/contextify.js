var ContextifyContext = require('bindings')('contextify').ContextifyContext;

module.exports = function Contextify (sandbox) {
    if (typeof sandbox != 'object') {
        sandbox = {};
    }
    var ctx = new ContextifyContext(sandbox);

    sandbox.run = function () {
        return ctx.run.apply(ctx, arguments);
    };

    sandbox.getGlobal = function () {
        return ctx.getGlobal();
    }

    sandbox.dispose = function () {
        sandbox.run = function () {
            throw new Error("Called run() after dispose().");
        };
        sandbox.getGlobal = function () {
            throw new Error("Called getGlobal() after dispose().");
        };
        sandbox.dispose = function () {
            throw new Error("Called dispose() after dispose().");
        };
        ctx = null;
    }
    return sandbox;
}
