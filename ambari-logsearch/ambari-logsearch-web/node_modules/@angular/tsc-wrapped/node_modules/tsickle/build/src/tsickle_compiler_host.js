"use strict";
var path = require("path");
var source_map_1 = require("source-map");
var ts = require("typescript");
var decorator_annotator_1 = require("./decorator-annotator");
var es5processor_1 = require("./es5processor");
var modules_manifest_1 = require("./modules_manifest");
var sourceMapUtils = require("./source_map_utils");
var tsickle = require("./tsickle");
var tsickle_1 = require("./tsickle");
/**
 * Tsickle can perform 2 different precompilation transforms - decorator downleveling
 * and closurization.  Both require tsc to have already type checked their
 * input, so they can't both be run in one call to tsc. If you only want one of
 * the transforms, you can specify it in the constructor, if you want both, you'll
 * have to specify it by calling reconfigureForRun() with the appropriate Pass.
 */
var Pass;
(function (Pass) {
    Pass[Pass["NONE"] = 0] = "NONE";
    Pass[Pass["DECORATOR_DOWNLEVEL"] = 1] = "DECORATOR_DOWNLEVEL";
    Pass[Pass["CLOSURIZE"] = 2] = "CLOSURIZE";
})(Pass = exports.Pass || (exports.Pass = {}));
var ANNOTATION_SUPPORT = "\ninterface DecoratorInvocation {\n  type: Function;\n  args?: any[];\n}\n";
/**
 * TsickleCompilerHost does tsickle processing of input files, including
 * closure type annotation processing, decorator downleveling and
 * require -> googmodule rewriting.
 */
var TsickleCompilerHost = (function () {
    function TsickleCompilerHost(delegate, tscOptions, options, environment) {
        this.delegate = delegate;
        this.tscOptions = tscOptions;
        this.options = options;
        this.environment = environment;
        // The manifest of JS modules output by the compiler.
        this.modulesManifest = new modules_manifest_1.ModulesManifest();
        /** Error messages produced by tsickle, if any. */
        this.diagnostics = [];
        /** externs.js files produced by tsickle, if any. */
        this.externs = {};
        this.sourceFileToPreexistingSourceMap = new Map();
        this.preexistingSourceMaps = new Map();
        this.decoratorDownlevelSourceMaps = new Map();
        this.tsickleSourceMaps = new Map();
        // ts.CompilerHost includes a bunch of optional methods.  If they're
        // present on the delegate host, we want to delegate them.
        if (this.delegate.getCancellationToken) {
            this.getCancellationToken = this.delegate.getCancellationToken.bind(this.delegate);
        }
        if (this.delegate.getDefaultLibLocation) {
            this.getDefaultLibLocation = this.delegate.getDefaultLibLocation.bind(this.delegate);
        }
        if (this.delegate.resolveModuleNames) {
            this.resolveModuleNames = this.delegate.resolveModuleNames.bind(this.delegate);
        }
        if (this.delegate.resolveTypeReferenceDirectives) {
            this.resolveTypeReferenceDirectives =
                this.delegate.resolveTypeReferenceDirectives.bind(this.delegate);
        }
        if (this.delegate.getEnvironmentVariable) {
            this.getEnvironmentVariable = this.delegate.getEnvironmentVariable.bind(this.delegate);
        }
        if (this.delegate.trace) {
            this.trace = this.delegate.trace.bind(this.delegate);
        }
        if (this.delegate.directoryExists) {
            this.directoryExists = this.delegate.directoryExists.bind(this.delegate);
        }
        if (this.delegate.realpath) {
            this.delegate.realpath = this.delegate.realpath.bind(this.delegate);
        }
    }
    /**
     * Tsickle can perform 2 kinds of precompilation source transforms - decorator
     * downleveling and closurization.  They can't be run in the same run of the
     * typescript compiler, because they both depend on type information that comes
     * from running the compiler.  We need to use the same compiler host to run both
     * so we have all the source map data when finally write out.  Thus if we want
     * to run both transforms, we call reconfigureForRun() between the calls to
     * ts.createProgram().
     */
    TsickleCompilerHost.prototype.reconfigureForRun = function (oldProgram, pass) {
        this.runConfiguration = { oldProgram: oldProgram, pass: pass };
    };
    TsickleCompilerHost.prototype.getSourceFile = function (fileName, languageVersion, onError) {
        if (this.runConfiguration === undefined || this.runConfiguration.pass === Pass.NONE) {
            var sourceFile_1 = this.delegate.getSourceFile(fileName, languageVersion, onError);
            return this.stripAndStoreExistingSourceMap(sourceFile_1);
        }
        var sourceFile = this.runConfiguration.oldProgram.getSourceFile(fileName);
        switch (this.runConfiguration.pass) {
            case Pass.DECORATOR_DOWNLEVEL:
                return this.downlevelDecorators(sourceFile, this.runConfiguration.oldProgram, fileName, languageVersion);
            case Pass.CLOSURIZE:
                return this.closurize(sourceFile, this.runConfiguration.oldProgram, fileName, languageVersion);
            default:
                throw new Error('tried to use TsickleCompilerHost with unknown pass enum');
        }
    };
    TsickleCompilerHost.prototype.writeFile = function (fileName, content, writeByteOrderMark, onError, sourceFiles) {
        if (path.extname(fileName) !== '.map') {
            if (!tsickle_1.isDtsFileName(fileName) && this.tscOptions.inlineSourceMap) {
                content = this.combineInlineSourceMaps(fileName, content);
            }
            if (this.options.googmodule && !tsickle_1.isDtsFileName(fileName)) {
                content = this.convertCommonJsToGoogModule(fileName, content);
            }
        }
        else {
            content = this.combineSourceMaps(fileName, content);
        }
        this.delegate.writeFile(fileName, content, writeByteOrderMark, onError, sourceFiles);
    };
    TsickleCompilerHost.prototype.getSourceMapKeyForPathAndName = function (outputFilePath, sourceFileName) {
        var fileDir = path.dirname(outputFilePath);
        return this.getCanonicalFileName(path.resolve(fileDir, sourceFileName));
    };
    TsickleCompilerHost.prototype.getSourceMapKeyForSourceFile = function (sourceFile) {
        return this.getCanonicalFileName(path.resolve(sourceFile.path));
    };
    TsickleCompilerHost.prototype.stripAndStoreExistingSourceMap = function (sourceFile) {
        // Because tsc doesn't have strict null checks, it can pass us an
        // undefined sourceFile, but we can't acknowledge that it does, because
        // we have to comply with their interface, which doesn't allow
        // undefined as far as we're concerned
        if (sourceFile && sourceMapUtils.containsInlineSourceMap(sourceFile.text)) {
            var sourceMapJson = sourceMapUtils.extractInlineSourceMap(sourceFile.text);
            var sourceMap_1 = sourceMapUtils.sourceMapTextToGenerator(sourceMapJson);
            var stripedSourceText = sourceMapUtils.removeInlineSourceMap(sourceFile.text);
            var stripedSourceFile = ts.createSourceFile(sourceFile.fileName, stripedSourceText, sourceFile.languageVersion);
            this.sourceFileToPreexistingSourceMap.set(stripedSourceFile, sourceMap_1);
            return stripedSourceFile;
        }
        return sourceFile;
    };
    TsickleCompilerHost.prototype.combineSourceMaps = function (filePath, tscSourceMapText) {
        var _this = this;
        // We stripe inline source maps off source files before they've been parsed
        // which is before they have path properties, so we need to construct the
        // map of sourceMapKey to preexistingSourceMap after the whole program has been
        // loaded.
        if (this.sourceFileToPreexistingSourceMap.size > 0 && this.preexistingSourceMaps.size === 0) {
            this.sourceFileToPreexistingSourceMap.forEach(function (sourceMap, sourceFile) {
                var sourceMapKey = _this.getSourceMapKeyForSourceFile(sourceFile);
                _this.preexistingSourceMaps.set(sourceMapKey, sourceMap);
            });
        }
        var tscSourceMapConsumer = sourceMapUtils.sourceMapTextToConsumer(tscSourceMapText);
        var tscSourceMapGenerator = sourceMapUtils.sourceMapConsumerToGenerator(tscSourceMapConsumer);
        if (this.tsickleSourceMaps.size > 0) {
            // TODO(lucassloan): remove when the .d.ts has the correct types
            for (var _i = 0, _a = tscSourceMapConsumer.sources; _i < _a.length; _i++) {
                var sourceFileName = _a[_i];
                var sourceMapKey = this.getSourceMapKeyForPathAndName(filePath, sourceFileName);
                var tsickleSourceMapGenerator = this.tsickleSourceMaps.get(sourceMapKey);
                var tsickleSourceMapConsumer = sourceMapUtils.sourceMapGeneratorToConsumer(tsickleSourceMapGenerator, sourceFileName, sourceFileName);
                tscSourceMapGenerator.applySourceMap(tsickleSourceMapConsumer);
            }
        }
        if (this.decoratorDownlevelSourceMaps.size > 0) {
            // TODO(lucassloan): remove when the .d.ts has the correct types
            for (var _b = 0, _c = tscSourceMapConsumer.sources; _b < _c.length; _b++) {
                var sourceFileName = _c[_b];
                var sourceMapKey = this.getSourceMapKeyForPathAndName(filePath, sourceFileName);
                var decoratorDownlevelSourceMapGenerator = this.decoratorDownlevelSourceMaps.get(sourceMapKey);
                var decoratorDownlevelSourceMapConsumer = sourceMapUtils.sourceMapGeneratorToConsumer(decoratorDownlevelSourceMapGenerator, sourceFileName, sourceFileName);
                tscSourceMapGenerator.applySourceMap(decoratorDownlevelSourceMapConsumer);
            }
        }
        if (this.preexistingSourceMaps.size > 0) {
            // TODO(lucassloan): remove when the .d.ts has the correct types
            for (var _d = 0, _e = tscSourceMapConsumer.sources; _d < _e.length; _d++) {
                var sourceFileName = _e[_d];
                var sourceMapKey = this.getSourceMapKeyForPathAndName(filePath, sourceFileName);
                var preexistingSourceMapGenerator = this.preexistingSourceMaps.get(sourceMapKey);
                if (preexistingSourceMapGenerator) {
                    var preexistingSourceMapConsumer = sourceMapUtils.sourceMapGeneratorToConsumer(preexistingSourceMapGenerator, sourceFileName);
                    tscSourceMapGenerator.applySourceMap(preexistingSourceMapConsumer);
                }
            }
        }
        return tscSourceMapGenerator.toString();
    };
    TsickleCompilerHost.prototype.combineInlineSourceMaps = function (filePath, compiledJsWithInlineSourceMap) {
        var sourceMapJson = sourceMapUtils.extractInlineSourceMap(compiledJsWithInlineSourceMap);
        var composedSourceMap = this.combineSourceMaps(filePath, sourceMapJson);
        return sourceMapUtils.setInlineSourceMap(compiledJsWithInlineSourceMap, composedSourceMap);
    };
    TsickleCompilerHost.prototype.convertCommonJsToGoogModule = function (fileName, content) {
        var moduleId = this.environment.fileNameToModuleId(fileName);
        var _a = es5processor_1.processES5(fileName, moduleId, content, this.environment.pathToModuleName.bind(this.environment), this.options.es5Mode, this.options.prelude), output = _a.output, referencedModules = _a.referencedModules;
        var moduleName = this.environment.pathToModuleName('', fileName);
        this.modulesManifest.addModule(fileName, moduleName);
        for (var _i = 0, referencedModules_1 = referencedModules; _i < referencedModules_1.length; _i++) {
            var referenced = referencedModules_1[_i];
            this.modulesManifest.addReferencedModule(fileName, referenced);
        }
        return output;
    };
    TsickleCompilerHost.prototype.downlevelDecorators = function (sourceFile, program, fileName, languageVersion) {
        this.decoratorDownlevelSourceMaps.set(this.getSourceMapKeyForSourceFile(sourceFile), new source_map_1.SourceMapGenerator());
        if (this.environment.shouldSkipTsickleProcessing(fileName))
            return sourceFile;
        var fileContent = sourceFile.text;
        var converted = decorator_annotator_1.convertDecorators(program.getTypeChecker(), sourceFile);
        if (converted.diagnostics) {
            (_a = this.diagnostics).push.apply(_a, converted.diagnostics);
        }
        if (converted.output === fileContent) {
            // No changes; reuse the existing parse.
            return sourceFile;
        }
        fileContent = converted.output + ANNOTATION_SUPPORT;
        this.decoratorDownlevelSourceMaps.set(this.getSourceMapKeyForSourceFile(sourceFile), converted.sourceMap);
        return ts.createSourceFile(fileName, fileContent, languageVersion, true);
        var _a;
    };
    TsickleCompilerHost.prototype.closurize = function (sourceFile, program, fileName, languageVersion) {
        this.tsickleSourceMaps.set(this.getSourceMapKeyForSourceFile(sourceFile), new source_map_1.SourceMapGenerator());
        var isDefinitions = tsickle_1.isDtsFileName(fileName);
        // Don't tsickle-process any d.ts that isn't a compilation target;
        // this means we don't process e.g. lib.d.ts.
        if (isDefinitions && this.environment.shouldSkipTsickleProcessing(fileName))
            return sourceFile;
        var _a = tsickle.annotate(program, sourceFile, this.environment.pathToModuleName.bind(this.environment), this.options, this.delegate, this.tscOptions), output = _a.output, externs = _a.externs, diagnostics = _a.diagnostics, sourceMap = _a.sourceMap;
        if (externs) {
            this.externs[fileName] = externs;
        }
        if (this.environment.shouldIgnoreWarningsForPath(sourceFile.path)) {
            // All diagnostics (including warnings) are treated as errors.
            // If we've decided to ignore them, just discard them.
            // Warnings include stuff like "don't use @type in your jsdoc"; tsickle
            // warns and then fixes up the code to be Closure-compatible anyway.
            diagnostics = diagnostics.filter(function (d) { return d.category === ts.DiagnosticCategory.Error; });
        }
        this.diagnostics = diagnostics;
        this.tsickleSourceMaps.set(this.getSourceMapKeyForSourceFile(sourceFile), sourceMap);
        return ts.createSourceFile(fileName, output, languageVersion, true);
    };
    /** Concatenate all generated externs definitions together into a string. */
    TsickleCompilerHost.prototype.getGeneratedExterns = function () {
        var allExterns = tsickle.EXTERNS_HEADER;
        for (var _i = 0, _a = Object.keys(this.externs); _i < _a.length; _i++) {
            var fileName = _a[_i];
            allExterns += "// externs from " + fileName + ":\n";
            allExterns += this.externs[fileName];
        }
        return allExterns;
    };
    // Delegate everything else to the original compiler host.
    TsickleCompilerHost.prototype.fileExists = function (fileName) {
        return this.delegate.fileExists(fileName);
    };
    TsickleCompilerHost.prototype.getCurrentDirectory = function () {
        return this.delegate.getCurrentDirectory();
    };
    ;
    TsickleCompilerHost.prototype.useCaseSensitiveFileNames = function () {
        return this.delegate.useCaseSensitiveFileNames();
    };
    TsickleCompilerHost.prototype.getNewLine = function () {
        return this.delegate.getNewLine();
    };
    TsickleCompilerHost.prototype.getDirectories = function (path) {
        return this.delegate.getDirectories(path);
    };
    TsickleCompilerHost.prototype.readFile = function (fileName) {
        return this.delegate.readFile(fileName);
    };
    TsickleCompilerHost.prototype.getDefaultLibFileName = function (options) {
        return this.delegate.getDefaultLibFileName(options);
    };
    TsickleCompilerHost.prototype.getCanonicalFileName = function (fileName) {
        return this.delegate.getCanonicalFileName(fileName);
    };
    return TsickleCompilerHost;
}());
exports.TsickleCompilerHost = TsickleCompilerHost;

//# sourceMappingURL=tsickle_compiler_host.js.map
