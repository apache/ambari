"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var path = require("path");
var tsickle = require("tsickle");
var ts = require("typescript");
var bundler_1 = require("./bundler");
var cli_options_1 = require("./cli_options");
var compiler_host_1 = require("./compiler_host");
var index_writer_1 = require("./index_writer");
var tsc_1 = require("./tsc");
var vinyl_file_1 = require("./vinyl_file");
var tsc_2 = require("./tsc");
exports.UserError = tsc_2.UserError;
var DTS = /\.d\.ts$/;
var JS_EXT = /(\.js|)$/;
var TS_EXT = /\.ts$/;
function createBundleIndexHost(ngOptions, rootFiles, host) {
    var files = rootFiles.filter(function (f) { return !DTS.test(f); });
    if (files.length != 1) {
        return {
            host: host,
            errors: [{
                    file: null,
                    start: null,
                    length: null,
                    messageText: 'Angular compiler option "flatModuleIndex" requires one and only one .ts file in the "files" field.',
                    category: ts.DiagnosticCategory.Error,
                    code: 0
                }]
        };
    }
    var file = files[0];
    var indexModule = file.replace(/\.ts$/, '');
    var bundler = new bundler_1.MetadataBundler(indexModule, ngOptions.flatModuleId, new bundler_1.CompilerHostAdapter(host));
    var metadataBundle = bundler.getMetadataBundle();
    var metadata = JSON.stringify(metadataBundle.metadata);
    var name = path.join(path.dirname(indexModule), ngOptions.flatModuleOutFile.replace(JS_EXT, '.ts'));
    var libraryIndex = "./" + path.basename(indexModule);
    var content = index_writer_1.privateEntriesToIndex(libraryIndex, metadataBundle.privates);
    host = new compiler_host_1.SyntheticIndexHost(host, { name: name, content: content, metadata: metadata });
    return { host: host, indexName: name };
}
exports.createBundleIndexHost = createBundleIndexHost;
function main(project, cliOptions, codegen, options) {
    try {
        var projectDir = project;
        // project is vinyl like file object
        if (vinyl_file_1.isVinylFile(project)) {
            projectDir = path.dirname(project.path);
        }
        else if (fs.lstatSync(project).isFile()) {
            projectDir = path.dirname(project);
        }
        // file names in tsconfig are resolved relative to this absolute path
        var basePath_1 = path.resolve(process.cwd(), cliOptions.basePath || projectDir);
        // read the configuration options from wherever you store them
        var _a = tsc_1.tsc.readConfiguration(project, basePath_1, options), parsed_1 = _a.parsed, ngOptions_1 = _a.ngOptions;
        ngOptions_1.basePath = basePath_1;
        var rootFileNames_1 = parsed_1.fileNames.slice(0);
        var createProgram_1 = function (host, oldProgram) {
            return ts.createProgram(rootFileNames_1.slice(0), parsed_1.options, host, oldProgram);
        };
        var addGeneratedFileName_1 = function (genFileName) {
            if (genFileName.startsWith(basePath_1) && TS_EXT.exec(genFileName)) {
                rootFileNames_1.push(genFileName);
            }
        };
        var diagnostics_1 = parsed_1.options.diagnostics;
        if (diagnostics_1)
            ts.performance.enable();
        var host_1 = ts.createCompilerHost(parsed_1.options, true);
        // If the compilation is a flat module index then produce the flat module index
        // metadata and the synthetic flat module index.
        if (ngOptions_1.flatModuleOutFile && !ngOptions_1.skipMetadataEmit) {
            var _b = createBundleIndexHost(ngOptions_1, rootFileNames_1, host_1), bundleHost = _b.host, indexName = _b.indexName, errors_1 = _b.errors;
            if (errors_1)
                tsc_1.check(errors_1);
            if (indexName)
                addGeneratedFileName_1(indexName);
            host_1 = bundleHost;
        }
        var tsickleCompilerHostOptions = { googmodule: false, untyped: true, convertIndexImportShorthand: true };
        var tsickleHost = {
            shouldSkipTsickleProcessing: function (fileName) { return /\.d\.ts$/.test(fileName); },
            pathToModuleName: function (context, importPath) { return ''; },
            shouldIgnoreWarningsForPath: function (filePath) { return false; },
            fileNameToModuleId: function (fileName) { return fileName; },
        };
        var tsickleCompilerHost_1 = new tsickle.TsickleCompilerHost(host_1, ngOptions_1, tsickleCompilerHostOptions, tsickleHost);
        var program_1 = createProgram_1(tsickleCompilerHost_1);
        var errors = program_1.getOptionsDiagnostics();
        tsc_1.check(errors);
        if (ngOptions_1.skipTemplateCodegen || !codegen) {
            codegen = function () { return Promise.resolve([]); };
        }
        if (diagnostics_1)
            console.time('NG codegen');
        return codegen(ngOptions_1, cliOptions, program_1, host_1).then(function (genFiles) {
            if (diagnostics_1)
                console.timeEnd('NG codegen');
            // Add the generated files to the configuration so they will become part of the program.
            if (ngOptions_1.alwaysCompileGeneratedCode) {
                genFiles.forEach(function (genFileName) { return addGeneratedFileName_1(genFileName); });
            }
            var definitionsHost = tsickleCompilerHost_1;
            if (!ngOptions_1.skipMetadataEmit) {
                // if tsickle is not not used for emitting, but we do use the MetadataWriterHost,
                // it also needs to emit the js files.
                var emitJsFiles = ngOptions_1.annotationsAs === 'decorators' && !ngOptions_1.annotateForClosureCompiler;
                definitionsHost = new compiler_host_1.MetadataWriterHost(tsickleCompilerHost_1, ngOptions_1, emitJsFiles);
            }
            // Create a new program since codegen files were created after making the old program
            var programWithCodegen = createProgram_1(definitionsHost, program_1);
            tsc_1.tsc.typeCheck(host_1, programWithCodegen);
            var programForJsEmit = programWithCodegen;
            if (ngOptions_1.annotationsAs !== 'decorators') {
                if (diagnostics_1)
                    console.time('NG downlevel');
                tsickleCompilerHost_1.reconfigureForRun(programForJsEmit, tsickle.Pass.DECORATOR_DOWNLEVEL);
                // A program can be re-used only once; save the programWithCodegen to be reused by
                // metadataWriter
                programForJsEmit = createProgram_1(tsickleCompilerHost_1);
                tsc_1.check(tsickleCompilerHost_1.diagnostics);
                if (diagnostics_1)
                    console.timeEnd('NG downlevel');
            }
            if (ngOptions_1.annotateForClosureCompiler) {
                if (diagnostics_1)
                    console.time('NG JSDoc');
                tsickleCompilerHost_1.reconfigureForRun(programForJsEmit, tsickle.Pass.CLOSURIZE);
                programForJsEmit = createProgram_1(tsickleCompilerHost_1);
                tsc_1.check(tsickleCompilerHost_1.diagnostics);
                if (diagnostics_1)
                    console.timeEnd('NG JSDoc');
            }
            // Emit *.js and *.js.map
            tsc_1.tsc.emit(programForJsEmit);
            // Emit *.d.ts and maybe *.metadata.json
            // Not in the same emit pass with above, because tsickle erases
            // decorators which we want to read or document.
            // Do this emit second since TypeScript will create missing directories for us
            // in the standard emit.
            tsc_1.tsc.emit(programWithCodegen);
            if (diagnostics_1) {
                ts.performance.forEachMeasure(function (name, duration) { console.error("TS " + name + ": " + duration + "ms"); });
            }
        });
    }
    catch (e) {
        return Promise.reject(e);
    }
}
exports.main = main;
// CLI entry point
if (require.main === module) {
    var args_1 = process.argv.slice(2);
    var _a = ts.parseCommandLine(args_1), options = _a.options, errors = _a.errors;
    tsc_1.check(errors);
    var project = options.project || '.';
    // TODO(alexeagle): command line should be TSC-compatible, remove "CliOptions" here
    var cliOptions = new cli_options_1.CliOptions(require('minimist')(args_1));
    main(project, cliOptions, null, options)
        .then(function (exitCode) { return process.exit(exitCode); })
        .catch(function (e) {
        console.error(e.stack);
        console.error('Compilation failed');
        process.exit(1);
    });
}
//# sourceMappingURL=main.js.map