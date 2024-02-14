"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var fs_1 = require("fs");
var path_1 = require("path");
var ts = require("typescript");
var collector_1 = require("./collector");
function formatDiagnostics(d) {
    var host = {
        getCurrentDirectory: function () { return ts.sys.getCurrentDirectory(); },
        getNewLine: function () { return ts.sys.newLine; },
        getCanonicalFileName: function (f) { return f; }
    };
    return ts.formatDiagnostics(d, host);
}
exports.formatDiagnostics = formatDiagnostics;
/**
 * Implementation of CompilerHost that forwards all methods to another instance.
 * Useful for partial implementations to override only methods they care about.
 */
var DelegatingHost = (function () {
    function DelegatingHost(delegate) {
        var _this = this;
        this.delegate = delegate;
        this.getSourceFile = function (fileName, languageVersion, onError) {
            return _this.delegate.getSourceFile(fileName, languageVersion, onError);
        };
        this.getCancellationToken = function () { return _this.delegate.getCancellationToken(); };
        this.getDefaultLibFileName = function (options) {
            return _this.delegate.getDefaultLibFileName(options);
        };
        this.getDefaultLibLocation = function () { return _this.delegate.getDefaultLibLocation(); };
        this.writeFile = this.delegate.writeFile;
        this.getCurrentDirectory = function () { return _this.delegate.getCurrentDirectory(); };
        this.getDirectories = function (path) {
            return _this.delegate.getDirectories ? _this.delegate.getDirectories(path) : [];
        };
        this.getCanonicalFileName = function (fileName) { return _this.delegate.getCanonicalFileName(fileName); };
        this.useCaseSensitiveFileNames = function () { return _this.delegate.useCaseSensitiveFileNames(); };
        this.getNewLine = function () { return _this.delegate.getNewLine(); };
        this.fileExists = function (fileName) { return _this.delegate.fileExists(fileName); };
        this.readFile = function (fileName) { return _this.delegate.readFile(fileName); };
        this.trace = function (s) { return _this.delegate.trace(s); };
        this.directoryExists = function (directoryName) { return _this.delegate.directoryExists(directoryName); };
    }
    return DelegatingHost;
}());
exports.DelegatingHost = DelegatingHost;
var IGNORED_FILES = /\.ngfactory\.js$|\.ngstyle\.js$/;
var DTS = /\.d\.ts$/;
var MetadataWriterHost = (function (_super) {
    __extends(MetadataWriterHost, _super);
    function MetadataWriterHost(delegate, ngOptions, emitAllFiles) {
        var _this = _super.call(this, delegate) || this;
        _this.ngOptions = ngOptions;
        _this.emitAllFiles = emitAllFiles;
        _this.metadataCollector = new collector_1.MetadataCollector({ quotedNames: true });
        _this.metadataCollector1 = new collector_1.MetadataCollector({ version: 1 });
        _this.writeFile = function (fileName, data, writeByteOrderMark, onError, sourceFiles) {
            var isDts = /\.d\.ts$/.test(fileName);
            if (_this.emitAllFiles || isDts) {
                // Let the original file be written first; this takes care of creating parent directories
                _this.delegate.writeFile(fileName, data, writeByteOrderMark, onError, sourceFiles);
            }
            if (isDts) {
                // TODO: remove this early return after https://github.com/Microsoft/TypeScript/pull/8412
                // is released
                return;
            }
            if (IGNORED_FILES.test(fileName)) {
                return;
            }
            if (!sourceFiles) {
                throw new Error('Metadata emit requires the sourceFiles are passed to WriteFileCallback. ' +
                    'Update to TypeScript ^1.9.0-dev');
            }
            if (sourceFiles.length > 1) {
                throw new Error('Bundled emit with --out is not supported');
            }
            if (!_this.ngOptions.skipMetadataEmit && !_this.ngOptions.flatModuleOutFile) {
                _this.writeMetadata(fileName, sourceFiles[0]);
            }
        };
        return _this;
    }
    MetadataWriterHost.prototype.writeMetadata = function (emitFilePath, sourceFile) {
        // TODO: replace with DTS filePath when https://github.com/Microsoft/TypeScript/pull/8412 is
        // released
        if (/\.js$/.test(emitFilePath)) {
            var path_2 = emitFilePath.replace(/*DTS*/ /\.js$/, '.metadata.json');
            // Beginning with 2.1, TypeScript transforms the source tree before emitting it.
            // We need the original, unmodified, tree which might be several levels back
            // depending on the number of transforms performed. All SourceFile's prior to 2.1
            // will appear to be the original source since they didn't include an original field.
            var collectableFile = sourceFile;
            while (collectableFile.original) {
                collectableFile = collectableFile.original;
            }
            var metadata = this.metadataCollector.getMetadata(collectableFile, !!this.ngOptions.strictMetadataEmit);
            var metadata1 = this.metadataCollector1.getMetadata(collectableFile, false);
            var metadatas = [metadata, metadata1].filter(function (e) { return !!e; });
            if (metadatas.length) {
                var metadataText = JSON.stringify(metadatas);
                fs_1.writeFileSync(path_2, metadataText, { encoding: 'utf-8' });
            }
        }
    };
    return MetadataWriterHost;
}(DelegatingHost));
exports.MetadataWriterHost = MetadataWriterHost;
var SyntheticIndexHost = (function (_super) {
    __extends(SyntheticIndexHost, _super);
    function SyntheticIndexHost(delegate, syntheticIndex) {
        var _this = _super.call(this, delegate) || this;
        _this.fileExists = function (fileName) {
            return path_1.normalize(fileName) == _this.normalSyntheticIndexName ||
                _this.delegate.fileExists(fileName);
        };
        _this.readFile = function (fileName) {
            return path_1.normalize(fileName) == _this.normalSyntheticIndexName ?
                _this.indexContent :
                _this.delegate.readFile(fileName);
        };
        _this.getSourceFile = function (fileName, languageVersion, onError) {
            if (path_1.normalize(fileName) == _this.normalSyntheticIndexName) {
                return ts.createSourceFile(fileName, _this.indexContent, languageVersion, true);
            }
            return _this.delegate.getSourceFile(fileName, languageVersion, onError);
        };
        _this.writeFile = function (fileName, data, writeByteOrderMark, onError, sourceFiles) {
            _this.delegate.writeFile(fileName, data, writeByteOrderMark, onError, sourceFiles);
            if (fileName.match(DTS) && sourceFiles && sourceFiles.length == 1 &&
                path_1.normalize(sourceFiles[0].fileName) == _this.normalSyntheticIndexName) {
                // If we are writing the synthetic index, write the metadata along side.
                var metadataName = fileName.replace(DTS, '.metadata.json');
                fs_1.writeFileSync(metadataName, _this.indexMetadata, 'utf8');
            }
        };
        _this.normalSyntheticIndexName = path_1.normalize(syntheticIndex.name);
        _this.indexContent = syntheticIndex.content;
        _this.indexMetadata = syntheticIndex.metadata;
        return _this;
    }
    return SyntheticIndexHost;
}(DelegatingHost));
exports.SyntheticIndexHost = SyntheticIndexHost;
//# sourceMappingURL=compiler_host.js.map