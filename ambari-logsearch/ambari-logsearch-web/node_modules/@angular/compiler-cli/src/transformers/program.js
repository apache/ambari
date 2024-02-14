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
var tsc_wrapped_1 = require("@angular/tsc-wrapped");
var fs_1 = require("fs");
var path = require("path");
var ts = require("typescript");
var compiler_host_1 = require("../compiler_host");
var check_types_1 = require("../diagnostics/check_types");
var api_1 = require("./api");
var node_emitter_transform_1 = require("./node_emitter_transform");
var GENERATED_FILES = /\.ngfactory\.js$|\.ngstyle\.js$|\.ngsummary\.js$/;
var SUMMARY_JSON_FILES = /\.ngsummary.json$/;
var emptyModules = {
    ngModules: [],
    ngModuleByPipeOrDirective: new Map(),
    files: []
};
var AngularCompilerProgram = (function () {
    function AngularCompilerProgram(rootNames, options, host, oldProgram) {
        this.rootNames = rootNames;
        this.options = options;
        this.host = host;
        this.oldProgram = oldProgram;
        this._structuralDiagnostics = [];
        this.oldTsProgram = oldProgram ? oldProgram.getTsProgram() : undefined;
        this.tsProgram = ts.createProgram(rootNames, options, host, this.oldTsProgram);
        this.srcNames = this.tsProgram.getSourceFiles().map(function (sf) { return sf.fileName; });
        this.aotCompilerHost = new compiler_host_1.CompilerHost(this.tsProgram, options, host);
        if (host.readResource) {
            this.aotCompilerHost.loadResource = host.readResource.bind(host);
        }
        var compiler = compiler_1.createAotCompiler(this.aotCompilerHost, options).compiler;
        this.compiler = compiler;
        this.collector = new tsc_wrapped_1.MetadataCollector({ quotedNames: true });
    }
    // Program implementation
    AngularCompilerProgram.prototype.getTsProgram = function () { return this.programWithStubs; };
    AngularCompilerProgram.prototype.getTsOptionDiagnostics = function (cancellationToken) {
        return this.tsProgram.getOptionsDiagnostics(cancellationToken);
    };
    AngularCompilerProgram.prototype.getNgOptionDiagnostics = function (cancellationToken) {
        return getNgOptionDiagnostics(this.options);
    };
    AngularCompilerProgram.prototype.getTsSyntacticDiagnostics = function (sourceFile, cancellationToken) {
        return this.tsProgram.getSyntacticDiagnostics(sourceFile, cancellationToken);
    };
    AngularCompilerProgram.prototype.getNgStructuralDiagnostics = function (cancellationToken) {
        return this.structuralDiagnostics;
    };
    AngularCompilerProgram.prototype.getTsSemanticDiagnostics = function (sourceFile, cancellationToken) {
        return this.programWithStubs.getSemanticDiagnostics(sourceFile, cancellationToken);
    };
    AngularCompilerProgram.prototype.getNgSemanticDiagnostics = function (fileName, cancellationToken) {
        var compilerDiagnostics = this.generatedFileDiagnostics;
        // If we have diagnostics during the parser phase the type check phase is not meaningful so skip
        // it.
        if (compilerDiagnostics && compilerDiagnostics.length)
            return compilerDiagnostics;
        return this.typeChecker.getDiagnostics(fileName, cancellationToken);
    };
    AngularCompilerProgram.prototype.loadNgStructureAsync = function () {
        var _this = this;
        return this.compiler.analyzeModulesAsync(this.rootNames)
            .catch(this.catchAnalysisError.bind(this))
            .then(function (analyzedModules) {
            if (_this._analyzedModules) {
                throw new Error('Angular structure loaded both synchronously and asynchronsly');
            }
            _this._analyzedModules = analyzedModules;
        });
    };
    AngularCompilerProgram.prototype.getLazyRoutes = function (cancellationToken) { return {}; };
    AngularCompilerProgram.prototype.emit = function (_a) {
        var _this = this;
        var _b = _a.emitFlags, emitFlags = _b === void 0 ? api_1.EmitFlags.Default : _b, cancellationToken = _a.cancellationToken;
        var emitMap = new Map();
        var result = this.programWithStubs.emit(
        /* targetSourceFile */ undefined, createWriteFileCallback(emitFlags, this.host, this.collector, this.options, emitMap), cancellationToken, (emitFlags & (api_1.EmitFlags.DTS | api_1.EmitFlags.JS)) == api_1.EmitFlags.DTS, {
            after: this.options.skipTemplateCodegen ? [] : [node_emitter_transform_1.getAngularEmitterTransformFactory(this.generatedFiles)]
        });
        this.generatedFiles.forEach(function (file) {
            if (file.source && file.source.length && SUMMARY_JSON_FILES.test(file.genFileUrl)) {
                // If we have emitted the ngsummary.ts file, ensure the ngsummary.json file is emitted to
                // the same location.
                var emittedFile = emitMap.get(file.srcFileUrl);
                var fileName = emittedFile ?
                    path.join(path.dirname(emittedFile), path.basename(file.genFileUrl)) :
                    file.genFileUrl;
                _this.host.writeFile(fileName, file.source, false, function (error) { });
            }
        });
        return result;
    };
    Object.defineProperty(AngularCompilerProgram.prototype, "analyzedModules", {
        // Private members
        get: function () {
            return this._analyzedModules || (this._analyzedModules = this.analyzeModules());
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "structuralDiagnostics", {
        get: function () {
            return this.analyzedModules && this._structuralDiagnostics;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "stubs", {
        get: function () {
            return this._stubs || (this._stubs = this.generateStubs());
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "stubFiles", {
        get: function () {
            return this._stubFiles ||
                (this._stubFiles = this.stubs.reduce(function (files, generatedFile) {
                    if (generatedFile.source || (generatedFile.stmts && generatedFile.stmts.length)) {
                        return files.concat([generatedFile.genFileUrl]);
                    }
                    return files;
                }, []));
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "programWithStubsHost", {
        get: function () {
            return this._programWithStubsHost || (this._programWithStubsHost = createProgramWithStubsHost(this.stubs, this.tsProgram, this.host));
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "programWithStubs", {
        get: function () {
            return this._programWithStubs || (this._programWithStubs = this.createProgramWithStubs());
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "generatedFiles", {
        get: function () {
            return this._generatedFiles || (this._generatedFiles = this.generateFiles());
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "typeChecker", {
        get: function () {
            return (this._typeChecker && !this._typeChecker.partialResults) ?
                this._typeChecker :
                (this._typeChecker = this.createTypeChecker());
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AngularCompilerProgram.prototype, "generatedFileDiagnostics", {
        get: function () {
            return this.generatedFiles && this._generatedFileDiagnostics;
        },
        enumerable: true,
        configurable: true
    });
    AngularCompilerProgram.prototype.catchAnalysisError = function (e) {
        if (compiler_1.isSyntaxError(e)) {
            var parserErrors = compiler_1.getParseErrors(e);
            if (parserErrors && parserErrors.length) {
                this._structuralDiagnostics =
                    parserErrors.map(function (e) { return ({
                        message: e.contextualMessage(),
                        category: api_1.DiagnosticCategory.Error,
                        span: e.span
                    }); });
            }
            else {
                this._structuralDiagnostics = [{ message: e.message, category: api_1.DiagnosticCategory.Error }];
            }
            this._analyzedModules = emptyModules;
            return emptyModules;
        }
        throw e;
    };
    AngularCompilerProgram.prototype.analyzeModules = function () {
        try {
            return this.compiler.analyzeModulesSync(this.srcNames);
        }
        catch (e) {
            return this.catchAnalysisError(e);
        }
    };
    AngularCompilerProgram.prototype.generateStubs = function () {
        return this.options.skipTemplateCodegen ? [] :
            this.options.generateCodeForLibraries === false ?
                this.compiler.emitAllStubs(this.analyzedModules) :
                this.compiler.emitPartialStubs(this.analyzedModules);
    };
    AngularCompilerProgram.prototype.generateFiles = function () {
        try {
            // Always generate the files if requested to ensure we capture any diagnostic errors but only
            // keep the results if we are not skipping template code generation.
            var result = this.compiler.emitAllImpls(this.analyzedModules);
            return this.options.skipTemplateCodegen ? [] : result;
        }
        catch (e) {
            if (compiler_1.isSyntaxError(e)) {
                this._generatedFileDiagnostics = [{ message: e.message, category: api_1.DiagnosticCategory.Error }];
                return [];
            }
            throw e;
        }
    };
    AngularCompilerProgram.prototype.createTypeChecker = function () {
        return new check_types_1.TypeChecker(this.tsProgram, this.options, this.host, this.aotCompilerHost, this.options, this.analyzedModules, this.generatedFiles);
    };
    AngularCompilerProgram.prototype.createProgramWithStubs = function () {
        // If we are skipping code generation just use the original program.
        // Otherwise, create a new program that includes the stub files.
        return this.options.skipTemplateCodegen ?
            this.tsProgram :
            ts.createProgram(this.rootNames.concat(this.stubFiles), this.options, this.programWithStubsHost);
    };
    return AngularCompilerProgram;
}());
function createProgram(_a) {
    var rootNames = _a.rootNames, options = _a.options, host = _a.host, oldProgram = _a.oldProgram;
    return new AngularCompilerProgram(rootNames, options, host, oldProgram);
}
exports.createProgram = createProgram;
function writeMetadata(emitFilePath, sourceFile, collector, ngOptions) {
    if (/\.js$/.test(emitFilePath)) {
        var path_1 = emitFilePath.replace(/\.js$/, '.metadata.json');
        // Beginning with 2.1, TypeScript transforms the source tree before emitting it.
        // We need the original, unmodified, tree which might be several levels back
        // depending on the number of transforms performed. All SourceFile's prior to 2.1
        // will appear to be the original source since they didn't include an original field.
        var collectableFile = sourceFile;
        while (collectableFile.original) {
            collectableFile = collectableFile.original;
        }
        var metadata = collector.getMetadata(collectableFile, !!ngOptions.strictMetadataEmit);
        if (metadata) {
            var metadataText = JSON.stringify([metadata]);
            fs_1.writeFileSync(path_1, metadataText, { encoding: 'utf-8' });
        }
    }
}
function createWriteFileCallback(emitFlags, host, collector, ngOptions, emitMap) {
    var withMetadata = function (fileName, data, writeByteOrderMark, onError, sourceFiles) {
        var generatedFile = GENERATED_FILES.test(fileName);
        if (!generatedFile || data != '') {
            host.writeFile(fileName, data, writeByteOrderMark, onError, sourceFiles);
        }
        if (!generatedFile && sourceFiles && sourceFiles.length == 1) {
            emitMap.set(sourceFiles[0].fileName, fileName);
            writeMetadata(fileName, sourceFiles[0], collector, ngOptions);
        }
    };
    var withoutMetadata = function (fileName, data, writeByteOrderMark, onError, sourceFiles) {
        var generatedFile = GENERATED_FILES.test(fileName);
        if (!generatedFile || data != '') {
            host.writeFile(fileName, data, writeByteOrderMark, onError, sourceFiles);
        }
        if (!generatedFile && sourceFiles && sourceFiles.length == 1) {
            emitMap.set(sourceFiles[0].fileName, fileName);
        }
    };
    return (emitFlags & api_1.EmitFlags.Metadata) != 0 ? withMetadata : withoutMetadata;
}
function getNgOptionDiagnostics(options) {
    if (options.annotationsAs) {
        switch (options.annotationsAs) {
            case 'decorators':
            case 'static fields':
                break;
            default:
                return [{
                        message: 'Angular compiler options "annotationsAs" only supports "static fields" and "decorators"',
                        category: api_1.DiagnosticCategory.Error
                    }];
        }
    }
    return [];
}
function createProgramWithStubsHost(generatedFiles, originalProgram, originalHost) {
    return new (function () {
        function class_1() {
            var _this = this;
            this.getDefaultLibFileName = function (options) {
                return originalHost.getDefaultLibFileName(options);
            };
            this.getCurrentDirectory = function () { return originalHost.getCurrentDirectory(); };
            this.getCanonicalFileName = function (fileName) { return originalHost.getCanonicalFileName(fileName); };
            this.useCaseSensitiveFileNames = function () { return originalHost.useCaseSensitiveFileNames(); };
            this.getNewLine = function () { return originalHost.getNewLine(); };
            this.realPath = function (p) { return p; };
            this.fileExists = function (fileName) {
                return _this.generatedFiles.has(fileName) || originalHost.fileExists(fileName);
            };
            this.generatedFiles =
                new Map(generatedFiles.filter(function (g) { return g.source || (g.stmts && g.stmts.length); })
                    .map(function (g) { return [g.genFileUrl, { g: g }]; }));
            this.writeFile = originalHost.writeFile;
            if (originalHost.getDirectories) {
                this.getDirectories = function (path) { return originalHost.getDirectories(path); };
            }
            if (originalHost.directoryExists) {
                this.directoryExists = function (directoryName) { return originalHost.directoryExists(directoryName); };
            }
            if (originalHost.getCancellationToken) {
                this.getCancellationToken = function () { return originalHost.getCancellationToken(); };
            }
            if (originalHost.getDefaultLibLocation) {
                this.getDefaultLibLocation = function () { return originalHost.getDefaultLibLocation(); };
            }
            if (originalHost.trace) {
                this.trace = function (s) { return originalHost.trace(s); };
            }
        }
        class_1.prototype.getSourceFile = function (fileName, languageVersion, onError) {
            var data = this.generatedFiles.get(fileName);
            if (data) {
                return data.s || (data.s = ts.createSourceFile(fileName, data.g.source || compiler_1.toTypeScript(data.g), languageVersion));
            }
            return originalProgram.getSourceFile(fileName) ||
                originalHost.getSourceFile(fileName, languageVersion, onError);
        };
        class_1.prototype.readFile = function (fileName) {
            var data = this.generatedFiles.get(fileName);
            if (data) {
                return data.g.source || compiler_1.toTypeScript(data.g);
            }
            return originalHost.readFile(fileName);
        };
        return class_1;
    }());
}
//# sourceMappingURL=program.js.map