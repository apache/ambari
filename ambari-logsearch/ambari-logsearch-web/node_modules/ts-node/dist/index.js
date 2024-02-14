"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var path_1 = require("path");
var fs_1 = require("fs");
var os_1 = require("os");
var sourceMapSupport = require("source-map-support");
var extend = require("xtend");
var mkdirp = require("mkdirp");
var crypto = require("crypto");
var yn = require("yn");
var arrify = require("arrify");
var make_error_1 = require("make-error");
var tsconfig_1 = require("tsconfig");
var pkg = require('../package.json');
exports.VERSION = pkg.version;
var DEFAULTS = {
    getFile: getFile,
    fileExists: fileExists,
    cache: yn(process.env['TS_NODE_CACHE']),
    cacheDirectory: process.env['TS_NODE_CACHE_DIRECTORY'],
    disableWarnings: yn(process.env['TS_NODE_DISABLE_WARNINGS']),
    compiler: process.env['TS_NODE_COMPILER'],
    compilerOptions: parse(process.env['TS_NODE_COMPILER_OPTIONS']),
    project: process.env['TS_NODE_PROJECT'],
    ignore: split(process.env['TS_NODE_IGNORE']),
    ignoreWarnings: split(process.env['TS_NODE_IGNORE_WARNINGS']),
    fast: yn(process.env['TS_NODE_FAST'])
};
function split(value) {
    return value ? value.split(/ *, */g) : undefined;
}
exports.split = split;
function parse(value) {
    return value ? JSON.parse(value) : undefined;
}
exports.parse = parse;
function slash(value) {
    return value.replace(/\\/g, '/');
}
exports.slash = slash;
function register(options) {
    if (options === void 0) { options = {}; }
    var compiler = options.compiler || 'typescript';
    var emptyFileListWarnings = [18002, 18003];
    var ignoreWarnings = arrify(options.ignoreWarnings || DEFAULTS.ignoreWarnings || []).concat(emptyFileListWarnings).map(Number);
    var disableWarnings = !!(options.disableWarnings == null ? DEFAULTS.disableWarnings : options.disableWarnings);
    var getFile = options.getFile || DEFAULTS.getFile;
    var fileExists = options.fileExists || DEFAULTS.fileExists;
    var shouldCache = !!(options.cache == null ? DEFAULTS.cache : options.cache);
    var fast = !!(options.fast == null ? DEFAULTS.fast : options.fast);
    var project = options.project || DEFAULTS.project;
    var cacheDirectory = options.cacheDirectory || DEFAULTS.cacheDirectory || path_1.join(os_1.tmpdir(), 'ts-node');
    var compilerOptions = extend(DEFAULTS.compilerOptions, options.compilerOptions);
    var originalJsHandler = require.extensions['.js'];
    var result;
    var ignore = arrify((typeof options.ignore === 'boolean' ?
        (options.ignore === false ? [] : undefined) :
        (options.ignore || DEFAULTS.ignore)) ||
        ['/node_modules/']).map(function (str) { return new RegExp(str); });
    function load() {
        var cache = { contents: {}, versions: {}, sourceMaps: {} };
        sourceMapSupport.install({
            environment: 'node',
            retrieveSourceMap: function (fileName) {
                if (cache.sourceMaps[fileName]) {
                    return {
                        url: cache.sourceMaps[fileName],
                        map: getFile(cache.sourceMaps[fileName])
                    };
                }
            }
        });
        var cwd = process.cwd();
        var ts = require(compiler);
        var config = readConfig(compilerOptions, project, cwd, ts);
        var configDiagnostics = filterDiagnostics(config.errors, ignoreWarnings, disableWarnings);
        var extensions = ['.ts', '.tsx'];
        var cachedir = path_1.join(path_1.resolve(cwd, cacheDirectory), getCompilerDigest({ version: ts.version, fast: fast, ignoreWarnings: ignoreWarnings, disableWarnings: disableWarnings, config: config, compiler: compiler }));
        mkdirp.sync(cachedir);
        if (configDiagnostics.length) {
            throw new TSError(formatDiagnostics(configDiagnostics, cwd, ts, 0));
        }
        if (config.options.allowJs) {
            extensions.push('.js');
            registerExtension('.js', ignore, service, originalJsHandler);
        }
        for (var _i = 0, _a = config.fileNames; _i < _a.length; _i++) {
            var fileName = _a[_i];
            if (/\.d\.ts$/.test(fileName)) {
                cache.versions[fileName] = 1;
            }
        }
        function getExtension(fileName) {
            if (config.options.jsx === ts.JsxEmit.Preserve && path_1.extname(fileName) === '.tsx') {
                return '.jsx';
            }
            return '.js';
        }
        var getOutput = function (code, fileName, lineOffset) {
            if (lineOffset === void 0) { lineOffset = 0; }
            var result = ts.transpileModule(code, {
                fileName: fileName,
                compilerOptions: config.options,
                reportDiagnostics: true
            });
            var diagnosticList = result.diagnostics ?
                filterDiagnostics(result.diagnostics, ignoreWarnings, disableWarnings) :
                [];
            if (diagnosticList.length) {
                throw new TSError(formatDiagnostics(diagnosticList, cwd, ts, lineOffset));
            }
            return [result.outputText, result.sourceMapText];
        };
        var compile = readThrough(cachedir, shouldCache, getFile, fileExists, cache, getOutput, getExtension);
        var getTypeInfo = function (fileName, position) {
            throw new TypeError("No type information available under \"--fast\" mode");
        };
        if (!fast) {
            var addVersion_1 = function (fileName) {
                if (!cache.versions.hasOwnProperty(fileName)) {
                    cache.versions[fileName] = 1;
                }
            };
            var addCache_1 = function (code, fileName) {
                cache.contents[fileName] = code;
                cache.versions[fileName] += 1;
            };
            var serviceHost = {
                getScriptFileNames: function () { return Object.keys(cache.versions); },
                getScriptVersion: function (fileName) { return String(cache.versions[fileName]); },
                getScriptSnapshot: function (fileName) {
                    if (!cache.contents.hasOwnProperty(fileName)) {
                        if (!fileExists(fileName)) {
                            return undefined;
                        }
                        cache.contents[fileName] = getFile(fileName);
                    }
                    return ts.ScriptSnapshot.fromString(cache.contents[fileName]);
                },
                getDirectories: getDirectories,
                directoryExists: directoryExists,
                getNewLine: function () { return os_1.EOL; },
                getCurrentDirectory: function () { return cwd; },
                getCompilationSettings: function () { return config.options; },
                getDefaultLibFileName: function (options) { return ts.getDefaultLibFilePath(config.options); }
            };
            var service_1 = ts.createLanguageService(serviceHost);
            getOutput = function (code, fileName, lineOffset) {
                if (lineOffset === void 0) { lineOffset = 0; }
                var output = service_1.getEmitOutput(fileName);
                var diagnostics = service_1.getCompilerOptionsDiagnostics()
                    .concat(service_1.getSyntacticDiagnostics(fileName))
                    .concat(service_1.getSemanticDiagnostics(fileName));
                var diagnosticList = filterDiagnostics(diagnostics, ignoreWarnings, disableWarnings);
                if (diagnosticList.length) {
                    throw new TSError(formatDiagnostics(diagnosticList, cwd, ts, lineOffset));
                }
                if (output.emitSkipped) {
                    throw new TypeError(path_1.relative(cwd, fileName) + ": Emit skipped");
                }
                if (output.outputFiles.length === 0) {
                    throw new TypeError('Unable to require `.d.ts` file.\n' +
                        'This is usually the result of a faulty configuration or import. ' +
                        'Make sure there is a `.js`, `.json` or another executable extension and ' +
                        'loader (attached before `ts-node`) available alongside ' +
                        ("`" + path_1.basename(fileName) + "`."));
                }
                return [output.outputFiles[1].text, output.outputFiles[0].text];
            };
            compile = readThrough(cachedir, shouldCache, getFile, fileExists, cache, function (code, fileName, lineOffset) {
                addVersion_1(fileName);
                addCache_1(code, fileName);
                return getOutput(code, fileName, lineOffset);
            }, getExtension);
            getTypeInfo = function (fileName, position) {
                addVersion_1(fileName);
                var info = service_1.getQuickInfoAtPosition(fileName, position);
                var name = ts.displayPartsToString(info ? info.displayParts : []);
                var comment = ts.displayPartsToString(info ? info.documentation : []);
                return { name: name, comment: comment };
            };
        }
        return { cwd: cwd, compile: compile, getTypeInfo: getTypeInfo, extensions: extensions };
    }
    function service() {
        return result || (result = load());
    }
    registerExtension('.ts', ignore, service, originalJsHandler);
    registerExtension('.tsx', ignore, service, originalJsHandler);
    if (!options.lazy) {
        service();
    }
    return service;
}
exports.register = register;
function shouldIgnore(filename, ignore, service) {
    var relname = slash(filename);
    return ignore.some(function (x) { return x.test(relname); });
}
function registerExtension(ext, ignore, service, originalHandler) {
    var old = require.extensions[ext] || originalHandler;
    require.extensions[ext] = function (m, filename) {
        if (shouldIgnore(filename, ignore, service)) {
            return old(m, filename);
        }
        var _compile = m._compile;
        m._compile = function (code, fileName) {
            return _compile.call(this, service().compile(code, fileName), fileName);
        };
        return old(m, filename);
    };
}
function readConfig(compilerOptions, project, cwd, ts) {
    var result = tsconfig_1.loadSync(cwd, typeof project === 'string' ? project : undefined);
    result.config.compilerOptions = extend({
        target: 'es5',
        module: 'commonjs'
    }, result.config.compilerOptions, compilerOptions, {
        sourceMap: true,
        inlineSourceMap: false,
        inlineSources: true,
        declaration: false,
        noEmit: false,
        outDir: '$$ts-node$$'
    });
    delete result.config.compilerOptions.out;
    delete result.config.compilerOptions.outFile;
    delete result.config.compilerOptions.declarationDir;
    var basePath = result.path ? path_1.dirname(result.path) : cwd;
    if (typeof ts.parseConfigFile === 'function') {
        return ts.parseConfigFile(result.config, ts.sys, basePath);
    }
    if (typeof ts.parseJsonConfigFileContent === 'function') {
        return ts.parseJsonConfigFileContent(result.config, ts.sys, basePath, null, result.path);
    }
    throw new TypeError('Could not find a compatible `parseConfigFile` function');
}
function readThrough(cachedir, shouldCache, getFile, fileExists, cache, compile, getExtension) {
    if (shouldCache === false) {
        return function (code, fileName, lineOffset) {
            var cachePath = path_1.join(cachedir, getCacheName(code, fileName));
            var extension = getExtension(fileName);
            var sourceMapPath = "" + cachePath + extension + ".map";
            var out = compile(code, fileName, lineOffset);
            cache.sourceMaps[fileName] = sourceMapPath;
            var output = updateOutput(out[0], fileName, extension, sourceMapPath);
            var sourceMap = updateSourceMap(out[1], fileName);
            fs_1.writeFileSync(sourceMapPath, sourceMap);
            return output;
        };
    }
    return function (code, fileName, lineOffset) {
        var cachePath = path_1.join(cachedir, getCacheName(code, fileName));
        var extension = getExtension(fileName);
        var outputPath = "" + cachePath + extension;
        var sourceMapPath = outputPath + ".map";
        cache.sourceMaps[fileName] = sourceMapPath;
        if (fileExists(outputPath)) {
            return getFile(outputPath);
        }
        var out = compile(code, fileName, lineOffset);
        var output = updateOutput(out[0], fileName, extension, sourceMapPath);
        var sourceMap = updateSourceMap(out[1], fileName);
        fs_1.writeFileSync(outputPath, output);
        fs_1.writeFileSync(sourceMapPath, sourceMap);
        return output;
    };
}
function updateOutput(outputText, fileName, extension, sourceMapPath) {
    var ext = path_1.extname(fileName);
    var originalPath = path_1.basename(fileName).slice(0, -ext.length) + (extension + ".map");
    return outputText.slice(0, -originalPath.length) + sourceMapPath.replace(/\\/g, '/');
}
function updateSourceMap(sourceMapText, fileName) {
    var sourceMap = JSON.parse(sourceMapText);
    sourceMap.file = fileName;
    sourceMap.sources = [fileName];
    delete sourceMap.sourceRoot;
    return JSON.stringify(sourceMap);
}
function getCacheName(sourceCode, fileName) {
    return crypto.createHash('sha1')
        .update(path_1.extname(fileName), 'utf8')
        .update('\0', 'utf8')
        .update(sourceCode, 'utf8')
        .digest('hex');
}
function getCompilerDigest(opts) {
    return crypto.createHash('sha1').update(JSON.stringify(opts), 'utf8').digest('hex');
}
function fileExists(fileName) {
    try {
        var stats = fs_1.statSync(fileName);
        return stats.isFile() || stats.isFIFO();
    }
    catch (err) {
        return false;
    }
}
exports.fileExists = fileExists;
function getDirectories(path) {
    return fs_1.readdirSync(path).filter(function (name) { return directoryExists(path_1.join(path, name)); });
}
exports.getDirectories = getDirectories;
function directoryExists(path) {
    try {
        return fs_1.statSync(path).isDirectory();
    }
    catch (err) {
        return false;
    }
}
exports.directoryExists = directoryExists;
function getFile(fileName) {
    return fs_1.readFileSync(fileName, 'utf8');
}
exports.getFile = getFile;
function filterDiagnostics(diagnostics, ignore, disable) {
    if (disable) {
        return [];
    }
    return diagnostics.filter(function (x) { return ignore.indexOf(x.code); });
}
function formatDiagnostics(diagnostics, cwd, ts, lineOffset) {
    return diagnostics.map(function (x) { return formatDiagnostic(x, cwd, ts, lineOffset); });
}
exports.formatDiagnostics = formatDiagnostics;
function formatDiagnostic(diagnostic, cwd, ts, lineOffset) {
    var messageText = ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n');
    if (diagnostic.file) {
        var path = path_1.relative(cwd, diagnostic.file.fileName);
        var _a = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start), line = _a.line, character = _a.character;
        var message = path + " (" + (line + 1 + lineOffset) + "," + (character + 1) + "): " + messageText + " (" + diagnostic.code + ")";
        return { message: message, code: diagnostic.code };
    }
    return { message: messageText + " (" + diagnostic.code + ")", code: diagnostic.code };
}
exports.formatDiagnostic = formatDiagnostic;
var TSError = (function (_super) {
    __extends(TSError, _super);
    function TSError(diagnostics) {
        var _this = _super.call(this, "\u2A2F Unable to compile TypeScript\n" + diagnostics.map(function (x) { return x.message; }).join('\n')) || this;
        _this.diagnostics = diagnostics;
        _this.name = 'TSError';
        return _this;
    }
    return TSError;
}(make_error_1.BaseError));
exports.TSError = TSError;
//# sourceMappingURL=index.js.map