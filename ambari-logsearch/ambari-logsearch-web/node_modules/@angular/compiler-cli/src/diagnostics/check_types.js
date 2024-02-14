"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var compiler_1 = require("@angular/compiler");
var ts = require("typescript");
var stubCancellationToken = {
    isCancellationRequested: function () { return false; },
    throwIfCancellationRequested: function () { }
};
var TypeChecker = (function () {
    function TypeChecker(program, tsOptions, compilerHost, aotCompilerHost, aotOptions, _analyzedModules, _generatedFiles) {
        this.program = program;
        this.tsOptions = tsOptions;
        this.compilerHost = compilerHost;
        this.aotCompilerHost = aotCompilerHost;
        this.aotOptions = aotOptions;
        this._analyzedModules = _analyzedModules;
        this._generatedFiles = _generatedFiles;
        this._currentCancellationToken = stubCancellationToken;
        this._partial = false;
    }
    TypeChecker.prototype.getDiagnostics = function (fileName, cancellationToken) {
        this._currentCancellationToken = cancellationToken || stubCancellationToken;
        try {
            return fileName ?
                this.diagnosticsByFileName.get(fileName) || [] : (_a = []).concat.apply(_a, Array.from(this.diagnosticsByFileName.values()));
        }
        finally {
            this._currentCancellationToken = stubCancellationToken;
        }
        var _a;
    };
    Object.defineProperty(TypeChecker.prototype, "partialResults", {
        get: function () { return this._partial; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "analyzedModules", {
        get: function () {
            return this._analyzedModules || (this._analyzedModules = this.aotCompiler.analyzeModulesSync(this.program.getSourceFiles().map(function (sf) { return sf.fileName; })));
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "diagnosticsByFileName", {
        get: function () {
            return this._diagnosticsByFile || this.createDiagnosticsByFile();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "diagnosticProgram", {
        get: function () {
            return this._diagnosticProgram || this.createDiagnosticProgram();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "generatedFiles", {
        get: function () {
            var result = this._generatedFiles;
            if (!result) {
                this._generatedFiles = result = this.aotCompiler.emitAllImpls(this.analyzedModules);
            }
            return result;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "aotCompiler", {
        get: function () {
            return this._aotCompiler || this.createCompilerAndReflector();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "reflector", {
        get: function () {
            var result = this._reflector;
            if (!result) {
                this.createCompilerAndReflector();
                result = this._reflector;
            }
            return result;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "factories", {
        get: function () {
            return this._factories || this.createFactories();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeChecker.prototype, "factoryNames", {
        get: function () {
            return this._factoryNames || (this.createFactories() && this._factoryNames);
        },
        enumerable: true,
        configurable: true
    });
    TypeChecker.prototype.createCompilerAndReflector = function () {
        var _a = compiler_1.createAotCompiler(this.aotCompilerHost, this.aotOptions), compiler = _a.compiler, reflector = _a.reflector;
        this._reflector = reflector;
        return this._aotCompiler = compiler;
    };
    TypeChecker.prototype.createDiagnosticProgram = function () {
        // Create a program that is all the files from the original program plus the factories.
        var existingFiles = this.program.getSourceFiles().map(function (source) { return source.fileName; });
        var host = new TypeCheckingHost(this.compilerHost, this.program, this.factories);
        return this._diagnosticProgram =
            ts.createProgram(existingFiles.concat(this.factoryNames), this.tsOptions, host);
    };
    TypeChecker.prototype.createFactories = function () {
        // Create all the factory files with enough information to map the diagnostics reported for the
        // created file back to the original source.
        var emitter = new compiler_1.TypeScriptEmitter();
        var factorySources = this.generatedFiles.filter(function (file) { return file.stmts != null && file.stmts.length; })
            .map(function (file) { return [file.genFileUrl, createFactoryInfo(emitter, file)]; });
        this._factories = new Map(factorySources);
        this._factoryNames = Array.from(this._factories.keys());
        return this._factories;
    };
    TypeChecker.prototype.createDiagnosticsByFile = function () {
        // Collect all the diagnostics binned by original source file name.
        var result = new Map();
        var diagnosticsFor = function (fileName) {
            var r = result.get(fileName);
            if (!r) {
                r = [];
                result.set(fileName, r);
            }
            return r;
        };
        var program = this.diagnosticProgram;
        for (var _i = 0, _a = this.factoryNames; _i < _a.length; _i++) {
            var factoryName = _a[_i];
            if (this._currentCancellationToken.isCancellationRequested())
                return result;
            var sourceFile = program.getSourceFile(factoryName);
            for (var _b = 0, _c = this.diagnosticProgram.getSemanticDiagnostics(sourceFile); _b < _c.length; _b++) {
                var diagnostic = _c[_b];
                var span = this.sourceSpanOf(diagnostic.file, diagnostic.start, diagnostic.length);
                if (span) {
                    var fileName = span.start.file.url;
                    var diagnosticsList = diagnosticsFor(fileName);
                    diagnosticsList.push({
                        message: diagnosticMessageToString(diagnostic.messageText),
                        category: diagnosticCategoryConverter(diagnostic.category), span: span
                    });
                }
            }
        }
        return result;
    };
    TypeChecker.prototype.sourceSpanOf = function (source, start, length) {
        // Find the corresponding TypeScript node
        var info = this.factories.get(source.fileName);
        if (info) {
            var _a = ts.getLineAndCharacterOfPosition(source, start), line = _a.line, character = _a.character;
            return info.context.spanOf(line, character);
        }
        return null;
    };
    return TypeChecker;
}());
exports.TypeChecker = TypeChecker;
function diagnosticMessageToString(message) {
    return ts.flattenDiagnosticMessageText(message, '\n');
}
function diagnosticCategoryConverter(kind) {
    // The diagnostics kind matches ts.DiagnosticCategory. Review this code if this changes.
    return kind;
}
function createFactoryInfo(emitter, file) {
    var _a = emitter.emitStatementsAndContext(file.srcFileUrl, file.genFileUrl, file.stmts), sourceText = _a.sourceText, context = _a.context;
    var source = ts.createSourceFile(file.genFileUrl, sourceText, ts.ScriptTarget.Latest, /* setParentNodes */ true);
    return { source: source, context: context };
}
var TypeCheckingHost = (function () {
    function TypeCheckingHost(host, originalProgram, factories) {
        this.host = host;
        this.originalProgram = originalProgram;
        this.factories = factories;
        this.writeFile = function () { throw new Error('Unexpected write in diagnostic program'); };
    }
    TypeCheckingHost.prototype.getSourceFile = function (fileName, languageVersion, onError) {
        var originalSource = this.originalProgram.getSourceFile(fileName);
        if (originalSource) {
            return originalSource;
        }
        var factoryInfo = this.factories.get(fileName);
        if (factoryInfo) {
            return factoryInfo.source;
        }
        return this.host.getSourceFile(fileName, languageVersion, onError);
    };
    TypeCheckingHost.prototype.getDefaultLibFileName = function (options) {
        return this.host.getDefaultLibFileName(options);
    };
    TypeCheckingHost.prototype.getCurrentDirectory = function () { return this.host.getCurrentDirectory(); };
    TypeCheckingHost.prototype.getDirectories = function (path) { return this.host.getDirectories(path); };
    TypeCheckingHost.prototype.getCanonicalFileName = function (fileName) {
        return this.host.getCanonicalFileName(fileName);
    };
    TypeCheckingHost.prototype.useCaseSensitiveFileNames = function () { return this.host.useCaseSensitiveFileNames(); };
    TypeCheckingHost.prototype.getNewLine = function () { return this.host.getNewLine(); };
    TypeCheckingHost.prototype.fileExists = function (fileName) {
        return this.factories.has(fileName) || this.host.fileExists(fileName);
    };
    TypeCheckingHost.prototype.readFile = function (fileName) {
        var factoryInfo = this.factories.get(fileName);
        return (factoryInfo && factoryInfo.source.text) || this.host.readFile(fileName);
    };
    return TypeCheckingHost;
}());
//# sourceMappingURL=check_types.js.map