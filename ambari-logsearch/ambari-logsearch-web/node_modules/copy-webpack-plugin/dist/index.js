'use strict';

var _bluebird = require('bluebird');

var _bluebird2 = _interopRequireDefault(_bluebird);

var _lodash = require('lodash');

var _lodash2 = _interopRequireDefault(_lodash);

var _preProcessPattern = require('./preProcessPattern');

var _preProcessPattern2 = _interopRequireDefault(_preProcessPattern);

var _processPattern = require('./processPattern');

var _processPattern2 = _interopRequireDefault(_processPattern);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function CopyWebpackPlugin() {
    var patterns = arguments.length <= 0 || arguments[0] === undefined ? [] : arguments[0];
    var options = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];

    if (!Array.isArray(patterns)) {
        throw new Error('[copy-webpack-plugin] patterns must be an array');
    }

    // Defaults debug level to 'warning'
    options.debug = options.debug || 'warning';

    // Defaults debugging to info if only true is specified
    if (options.debug === true) {
        options.debug = 'info';
    }

    var debugLevels = ['warning', 'info', 'debug'];
    var debugLevelIndex = debugLevels.indexOf(options.debug);
    function log(msg, level) {
        if (level === 0) {
            msg = 'WARNING - ' + msg;
        } else {
            level = level || 1;
        }
        if (level <= debugLevelIndex) {
            console.log('[copy-webpack-plugin] ' + msg); // eslint-disable-line no-console
        }
    }

    function warning(msg) {
        log(msg, 0);
    }

    function info(msg) {
        log(msg, 1);
    }

    function debug(msg) {
        log(msg, 2);
    }

    var apply = function apply(compiler) {
        var fileDependencies = [];
        var contextDependencies = [];
        var written = {};

        compiler.plugin('emit', function (compilation, cb) {
            debug('starting emit');
            var callback = function callback() {
                debug('finishing emit');
                cb();
            };

            var globalRef = {
                info: info,
                debug: debug,
                warning: warning,
                compilation: compilation,
                written: written,
                fileDependencies: fileDependencies,
                contextDependencies: contextDependencies,
                context: compiler.options.context,
                output: compiler.options.output.path,
                ignore: options.ignore || [],
                copyUnmodified: options.copyUnmodified,
                concurrency: options.concurrency
            };

            if (globalRef.output === '/' && compiler.options.devServer && compiler.options.devServer.outputPath) {
                globalRef.output = compiler.options.devServer.outputPath;
            }

            _bluebird2.default.each(patterns, function (pattern) {
                // Identify absolute source of each pattern and destination type
                return (0, _preProcessPattern2.default)(globalRef, pattern).then(function (pattern) {
                    // Every source (from) is assumed to exist here
                    return (0, _processPattern2.default)(globalRef, pattern);
                });
            }).catch(function (err) {
                compilation.errors.push(err);
            }).finally(callback);
        });

        compiler.plugin('after-emit', function (compilation, cb) {
            debug('starting after-emit');
            var callback = function callback() {
                debug('finishing after-emit');
                cb();
            };

            // Add file dependencies if they're not already tracked
            _lodash2.default.forEach(fileDependencies, function (file) {
                if (_lodash2.default.includes(compilation.fileDependencies, file)) {
                    debug('not adding ' + file + ' to change tracking, because it\'s already tracked');
                } else {
                    debug('adding ' + file + ' to change tracking');
                    compilation.fileDependencies.push(file);
                }
            });

            // Add context dependencies if they're not already tracked
            _lodash2.default.forEach(contextDependencies, function (context) {
                if (_lodash2.default.includes(compilation.contextDependencies, context)) {
                    debug('not adding ' + context + ' to change tracking, because it\'s already tracked');
                } else {
                    debug('adding ' + context + ' to change tracking');
                    compilation.contextDependencies.push(context);
                }
            });

            callback();
        });
    };

    return {
        apply: apply
    };
}

CopyWebpackPlugin['default'] = CopyWebpackPlugin;
module.exports = CopyWebpackPlugin;