"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var path = require("path");
var ts = require("typescript");
var EXT = /(\.ts|\.d\.ts|\.js|\.jsx|\.tsx)$/;
var DTS = /\.d\.ts$/;
var NODE_MODULES = '/node_modules/';
var IS_GENERATED = /\.(ngfactory|ngstyle|ngsummary)$/;
var SHALLOW_IMPORT = /^((\w|-)+|(@(\w|-)+(\/(\w|-)+)+))$/;
function createModuleFilenameResolver(tsHost, options) {
    var host = createModuleFilenameResolverHost(tsHost);
    return options.rootDirs && options.rootDirs.length > 0 ?
        new MultipleRootDirModuleFilenameResolver(host, options) :
        new SingleRootDirModuleFilenameResolver(host, options);
}
exports.createModuleFilenameResolver = createModuleFilenameResolver;
var SingleRootDirModuleFilenameResolver = (function () {
    function SingleRootDirModuleFilenameResolver(host, options) {
        this.host = host;
        this.options = options;
        this.moduleFileNames = new Map();
        // normalize the path so that it never ends with '/'.
        this.basePath = path.normalize(path.join(options.basePath, '.')).replace(/\\/g, '/');
        this.genDir = path.normalize(path.join(options.genDir, '.')).replace(/\\/g, '/');
        var genPath = path.relative(this.basePath, this.genDir);
        this.isGenDirChildOfRootDir = genPath === '' || !genPath.startsWith('..');
    }
    SingleRootDirModuleFilenameResolver.prototype.moduleNameToFileName = function (m, containingFile) {
        var key = m + ':' + (containingFile || '');
        var result = this.moduleFileNames.get(key) || null;
        if (!result) {
            if (!containingFile) {
                if (m.indexOf('.') === 0) {
                    throw new Error('Resolution of relative paths requires a containing file.');
                }
                // Any containing file gives the same result for absolute imports
                containingFile = this.getNgCanonicalFileName(path.join(this.basePath, 'index.ts'));
            }
            m = m.replace(EXT, '');
            var resolved = ts.resolveModuleName(m, containingFile.replace(/\\/g, '/'), this.options, this.host)
                .resolvedModule;
            result = resolved ? this.getNgCanonicalFileName(resolved.resolvedFileName) : null;
            this.moduleFileNames.set(key, result);
        }
        return result;
    };
    /**
     * We want a moduleId that will appear in import statements in the generated code.
     * These need to be in a form that system.js can load, so absolute file paths don't work.
     *
     * The `containingFile` is always in the `genDir`, where as the `importedFile` can be in
     * `genDir`, `node_module` or `basePath`.  The `importedFile` is either a generated file or
     * existing file.
     *
     *               | genDir   | node_module |  rootDir
     * --------------+----------+-------------+----------
     * generated     | relative |   relative  |   n/a
     * existing file |   n/a    |   absolute  |  relative(*)
     *
     * NOTE: (*) the relative path is computed depending on `isGenDirChildOfRootDir`.
     */
    SingleRootDirModuleFilenameResolver.prototype.fileNameToModuleName = function (importedFile, containingFile) {
        // If a file does not yet exist (because we compile it later), we still need to
        // assume it exists it so that the `resolve` method works!
        if (!this.host.fileExists(importedFile)) {
            this.host.assumeFileExists(importedFile);
        }
        containingFile = this.rewriteGenDirPath(containingFile);
        var containingDir = path.dirname(containingFile);
        // drop extension
        importedFile = importedFile.replace(EXT, '');
        var nodeModulesIndex = importedFile.indexOf(NODE_MODULES);
        var importModule = nodeModulesIndex === -1 ?
            null :
            importedFile.substring(nodeModulesIndex + NODE_MODULES.length);
        var isGeneratedFile = IS_GENERATED.test(importedFile);
        if (isGeneratedFile) {
            // rewrite to genDir path
            if (importModule) {
                // it is generated, therefore we do a relative path to the factory
                return this.dotRelative(containingDir, this.genDir + NODE_MODULES + importModule);
            }
            else {
                // assume that import is also in `genDir`
                importedFile = this.rewriteGenDirPath(importedFile);
                return this.dotRelative(containingDir, importedFile);
            }
        }
        else {
            // user code import
            if (importModule) {
                return importModule;
            }
            else {
                if (!this.isGenDirChildOfRootDir) {
                    // assume that they are on top of each other.
                    importedFile = importedFile.replace(this.basePath, this.genDir);
                }
                if (SHALLOW_IMPORT.test(importedFile)) {
                    return importedFile;
                }
                return this.dotRelative(containingDir, importedFile);
            }
        }
    };
    // We use absolute paths on disk as canonical.
    SingleRootDirModuleFilenameResolver.prototype.getNgCanonicalFileName = function (fileName) { return fileName; };
    SingleRootDirModuleFilenameResolver.prototype.assumeFileExists = function (fileName) { this.host.assumeFileExists(fileName); };
    SingleRootDirModuleFilenameResolver.prototype.dotRelative = function (from, to) {
        var rPath = path.relative(from, to).replace(/\\/g, '/');
        return rPath.startsWith('.') ? rPath : './' + rPath;
    };
    /**
     * Moves the path into `genDir` folder while preserving the `node_modules` directory.
     */
    SingleRootDirModuleFilenameResolver.prototype.rewriteGenDirPath = function (filepath) {
        var nodeModulesIndex = filepath.indexOf(NODE_MODULES);
        if (nodeModulesIndex !== -1) {
            // If we are in node_module, transplant them into `genDir`.
            return path.join(this.genDir, filepath.substring(nodeModulesIndex));
        }
        else {
            // pretend that containing file is on top of the `genDir` to normalize the paths.
            // we apply the `genDir` => `rootDir` delta through `rootDirPrefix` later.
            return filepath.replace(this.basePath, this.genDir);
        }
    };
    return SingleRootDirModuleFilenameResolver;
}());
/**
 * This version of the AotCompilerHost expects that the program will be compiled
 * and executed with a "path mapped" directory structure, where generated files
 * are in a parallel tree with the sources, and imported using a `./` relative
 * import. This requires using TS `rootDirs` option and also teaching the module
 * loader what to do.
 */
var MultipleRootDirModuleFilenameResolver = (function () {
    function MultipleRootDirModuleFilenameResolver(host, options) {
        this.host = host;
        this.options = options;
        // normalize the path so that it never ends with '/'.
        this.basePath = path.normalize(path.join(options.basePath, '.')).replace(/\\/g, '/');
    }
    MultipleRootDirModuleFilenameResolver.prototype.getNgCanonicalFileName = function (fileName) {
        if (!fileName)
            return fileName;
        // NB: the rootDirs should have been sorted longest-first
        for (var _i = 0, _a = this.options.rootDirs || []; _i < _a.length; _i++) {
            var dir = _a[_i];
            if (fileName.indexOf(dir) === 0) {
                fileName = fileName.substring(dir.length);
            }
        }
        return fileName;
    };
    MultipleRootDirModuleFilenameResolver.prototype.assumeFileExists = function (fileName) { this.host.assumeFileExists(fileName); };
    MultipleRootDirModuleFilenameResolver.prototype.moduleNameToFileName = function (m, containingFile) {
        if (!containingFile) {
            if (m.indexOf('.') === 0) {
                throw new Error('Resolution of relative paths requires a containing file.');
            }
            // Any containing file gives the same result for absolute imports
            containingFile = this.getNgCanonicalFileName(path.join(this.basePath, 'index.ts'));
        }
        for (var _i = 0, _a = this.options.rootDirs || ['']; _i < _a.length; _i++) {
            var root = _a[_i];
            var rootedContainingFile = path.join(root, containingFile);
            var resolved = ts.resolveModuleName(m, rootedContainingFile, this.options, this.host).resolvedModule;
            if (resolved) {
                if (this.options.traceResolution) {
                    console.error('resolve', m, containingFile, '=>', resolved.resolvedFileName);
                }
                return this.getNgCanonicalFileName(resolved.resolvedFileName);
            }
        }
        return null;
    };
    /**
     * We want a moduleId that will appear in import statements in the generated code.
     * These need to be in a form that system.js can load, so absolute file paths don't work.
     * Relativize the paths by checking candidate prefixes of the absolute path, to see if
     * they are resolvable by the moduleResolution strategy from the CompilerHost.
     */
    MultipleRootDirModuleFilenameResolver.prototype.fileNameToModuleName = function (importedFile, containingFile) {
        var _this = this;
        if (this.options.traceResolution) {
            console.error('getImportPath from containingFile', containingFile, 'to importedFile', importedFile);
        }
        // If a file does not yet exist (because we compile it later), we still need to
        // assume it exists so that the `resolve` method works!
        if (!this.host.fileExists(importedFile)) {
            if (this.options.rootDirs && this.options.rootDirs.length > 0) {
                this.host.assumeFileExists(path.join(this.options.rootDirs[0], importedFile));
            }
            else {
                this.host.assumeFileExists(importedFile);
            }
        }
        var resolvable = function (candidate) {
            var resolved = _this.moduleNameToFileName(candidate, importedFile);
            return resolved && resolved.replace(EXT, '') === importedFile.replace(EXT, '');
        };
        var importModuleName = importedFile.replace(EXT, '');
        var parts = importModuleName.split(path.sep).filter(function (p) { return !!p; });
        var foundRelativeImport;
        for (var index = parts.length - 1; index >= 0; index--) {
            var candidate_1 = parts.slice(index, parts.length).join(path.sep);
            if (resolvable(candidate_1)) {
                return candidate_1;
            }
            candidate_1 = '.' + path.sep + candidate_1;
            if (resolvable(candidate_1)) {
                foundRelativeImport = candidate_1;
            }
        }
        if (foundRelativeImport)
            return foundRelativeImport;
        // Try a relative import
        var candidate = path.relative(path.dirname(containingFile), importModuleName);
        if (resolvable(candidate)) {
            return candidate;
        }
        throw new Error("Unable to find any resolvable import for " + importedFile + " relative to " + containingFile);
    };
    return MultipleRootDirModuleFilenameResolver;
}());
function createModuleFilenameResolverHost(host) {
    var assumedExists = new Set();
    var resolveModuleNameHost = Object.create(host);
    // When calling ts.resolveModuleName, additional allow checks for .d.ts files to be done based on
    // checks for .ngsummary.json files, so that our codegen depends on fewer inputs and requires
    // to be called less often.
    // This is needed as we use ts.resolveModuleName in reflector_host and it should be able to
    // resolve summary file names.
    resolveModuleNameHost.fileExists = function (fileName) {
        if (assumedExists.has(fileName)) {
            return true;
        }
        if (host.fileExists(fileName)) {
            return true;
        }
        if (DTS.test(fileName)) {
            var base = fileName.substring(0, fileName.length - 5);
            return host.fileExists(base + '.ngsummary.json');
        }
        return false;
    };
    resolveModuleNameHost.assumeFileExists = function (fileName) { return assumedExists.add(fileName); };
    // Make sure we do not `host.realpath()` from TS as we do not want to resolve symlinks.
    // https://github.com/Microsoft/TypeScript/issues/9552
    resolveModuleNameHost.realpath = function (fileName) { return fileName; };
    return resolveModuleNameHost;
}
//# sourceMappingURL=module_filename_resolver.js.map