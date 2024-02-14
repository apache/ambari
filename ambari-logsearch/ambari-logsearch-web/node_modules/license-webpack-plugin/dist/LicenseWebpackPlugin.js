"use strict";
var __assign = (this && this.__assign) || Object.assign || function(t) {
    for (var s, i = 1, n = arguments.length; i < n; i++) {
        s = arguments[i];
        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[p] = s[p];
    }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
var path = require("path");
var fs = require("fs");
var ejs = require("ejs");
var webpack_sources_1 = require("webpack-sources");
var LicenseWebpackPluginError_1 = require("./LicenseWebpackPluginError");
var ErrorMessage_1 = require("./ErrorMessage");
var FileUtils_1 = require("./FileUtils");
var ModuleProcessor_1 = require("./ModuleProcessor");
var LicenseWebpackPlugin = (function () {
    function LicenseWebpackPlugin(options) {
        this.errors = [];
        if (!options || !options.pattern || !(options.pattern instanceof RegExp)) {
            throw new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.NO_PATTERN);
        }
        if (options.unacceptablePattern !== undefined &&
            options.unacceptablePattern !== null &&
            !(options.unacceptablePattern instanceof RegExp)) {
            throw new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.UNACCEPTABLE_PATTERN_NOT_REGEX);
        }
        this.options = __assign({
            licenseFilenames: [
                'LICENSE',
                'LICENSE.md',
                'LICENSE.txt',
                'license',
                'license.md',
                'license.txt'
            ],
            perChunkOutput: true,
            outputTemplate: path.resolve(__dirname, '../output.template.ejs'),
            outputFilename: options.perChunkOutput === false
                ? 'licenses.txt'
                : '[name].licenses.txt',
            suppressErrors: false,
            includePackagesWithoutLicense: false,
            abortOnUnacceptableLicense: false,
            addBanner: false,
            bannerTemplate: '/*! 3rd party license information is available at <%- filename %> */',
            includedChunks: [],
            excludedChunks: [],
            additionalPackages: []
        }, options);
        if (!FileUtils_1.FileUtils.isThere(this.options.outputTemplate)) {
            throw new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.OUTPUT_TEMPLATE_NOT_EXIST, this.options.outputTemplate);
        }
        var templateString = fs.readFileSync(this.options.outputTemplate, 'utf8');
        this.template = ejs.compile(templateString);
    }
    LicenseWebpackPlugin.prototype.apply = function (compiler) {
        var _this = this;
        this.buildRoot = this.findBuildRoot(compiler.context);
        this.moduleProcessor = new ModuleProcessor_1.ModuleProcessor(this.buildRoot, this.options, this.errors);
        compiler.plugin('emit', function (compilation, callback) {
            var totalChunkModuleMap = {};
            compilation.chunks.forEach(function (chunk) {
                if (_this.options.excludedChunks.indexOf(chunk.name) > -1) {
                    return;
                }
                if (_this.options.includedChunks.length > 0 &&
                    _this.options.includedChunks.indexOf(chunk.name) === -1) {
                    return;
                }
                var outputPath = compilation.getPath(_this.options.outputFilename, _this.options.perChunkOutput
                    ? {
                        chunk: chunk
                    }
                    : compilation);
                var chunkModuleMap = {};
                var moduleCallback = function (chunkModule) {
                    var packageName = _this.moduleProcessor.processFile(chunkModule.resource);
                    if (packageName) {
                        chunkModuleMap[packageName] = true;
                        totalChunkModuleMap[packageName] = true;
                    }
                };
                // scan all files used in compilation for this chunk
                if (typeof chunk.forEachModule === 'function') {
                    chunk.forEachModule(moduleCallback);
                }
                else {
                    chunk.modules.forEach(moduleCallback); // chunk.modules was deprecated in webpack v3
                }
                _this.options.additionalPackages.forEach(function (packageName) {
                    _this.moduleProcessor.processPackage(packageName);
                    chunkModuleMap[packageName] = true;
                    totalChunkModuleMap[packageName] = true;
                });
                var renderedFile = _this.renderLicenseFile(Object.keys(chunkModuleMap));
                // Only write license file if there is something to write.
                if (renderedFile.trim() !== '') {
                    if (_this.options.addBanner) {
                        chunk.files
                            .filter(function (file) { return /\.js$/.test(file); })
                            .forEach(function (file) {
                            compilation.assets[file] = new webpack_sources_1.ConcatSource(ejs.render(_this.options.bannerTemplate, {
                                filename: outputPath
                            }), '\n', compilation.assets[file]);
                        });
                    }
                    if (_this.options.perChunkOutput) {
                        compilation.assets[outputPath] = new webpack_sources_1.RawSource(renderedFile);
                    }
                }
            });
            if (!_this.options.perChunkOutput) {
                // produce master licenses file
                var outputPath = compilation.getPath(_this.options.outputFilename, compilation);
                var renderedFile = _this.renderLicenseFile(Object.keys(totalChunkModuleMap));
                if (renderedFile.trim() !== '') {
                    compilation.assets[outputPath] = new webpack_sources_1.RawSource(renderedFile);
                }
            }
            if (!_this.options.suppressErrors) {
                _this.errors.forEach(function (error) { return console.error(error.message); });
            }
            callback();
        });
    };
    LicenseWebpackPlugin.prototype.renderLicenseFile = function (packageNames) {
        var packages = packageNames.map(this.moduleProcessor.getPackageInfo, this.moduleProcessor);
        return this.template({ packages: packages });
    };
    LicenseWebpackPlugin.prototype.findBuildRoot = function (context) {
        var buildRoot = context;
        var lastPathSepIndex;
        if (buildRoot.indexOf(FileUtils_1.FileUtils.MODULE_DIR) > -1) {
            buildRoot = buildRoot.substring(0, buildRoot.indexOf(FileUtils_1.FileUtils.MODULE_DIR) - 1);
        }
        else {
            var oldBuildRoot = null;
            while (!FileUtils_1.FileUtils.isThere(path.join(buildRoot, FileUtils_1.FileUtils.MODULE_DIR))) {
                lastPathSepIndex = buildRoot.lastIndexOf(path.sep);
                if (lastPathSepIndex === -1 || oldBuildRoot === buildRoot) {
                    throw new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.NO_PROJECT_ROOT);
                }
                oldBuildRoot = buildRoot;
                buildRoot = buildRoot.substring(0, buildRoot.lastIndexOf(path.sep));
            }
        }
        return buildRoot;
    };
    return LicenseWebpackPlugin;
}());
exports.LicenseWebpackPlugin = LicenseWebpackPlugin;
