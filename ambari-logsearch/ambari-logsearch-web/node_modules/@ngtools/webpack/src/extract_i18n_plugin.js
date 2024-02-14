"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// @ignoreDep @angular/compiler-cli
const ts = require("typescript");
const path = require("path");
const fs = require("fs");
const { __NGTOOLS_PRIVATE_API_2, VERSION } = require('@angular/compiler-cli');
const resource_loader_1 = require("./resource_loader");
class ExtractI18nPlugin {
    constructor(options) {
        this._compiler = null;
        this._compilation = null;
        this._compilerOptions = null;
        this._angularCompilerOptions = null;
        this._setupOptions(options);
    }
    _setupOptions(options) {
        if (!options.hasOwnProperty('tsConfigPath')) {
            throw new Error('Must specify "tsConfigPath" in the configuration of @ngtools/webpack.');
        }
        // TS represents paths internally with '/' and expects the tsconfig path to be in this format
        this._tsConfigPath = options.tsConfigPath.replace(/\\/g, '/');
        // Check the base path.
        const maybeBasePath = path.resolve(process.cwd(), this._tsConfigPath);
        let basePath = maybeBasePath;
        if (fs.statSync(maybeBasePath).isFile()) {
            basePath = path.dirname(basePath);
        }
        if (options.hasOwnProperty('basePath')) {
            basePath = path.resolve(process.cwd(), options.basePath);
        }
        let tsConfigJson = null;
        try {
            tsConfigJson = JSON.parse(fs.readFileSync(this._tsConfigPath, 'utf8'));
        }
        catch (err) {
            throw new Error(`An error happened while parsing ${this._tsConfigPath} JSON: ${err}.`);
        }
        const tsConfig = ts.parseJsonConfigFileContent(tsConfigJson, ts.sys, basePath, undefined, this._tsConfigPath);
        let fileNames = tsConfig.fileNames;
        if (options.hasOwnProperty('exclude')) {
            let exclude = typeof options.exclude == 'string'
                ? [options.exclude] : options.exclude;
            exclude.forEach((pattern) => {
                const basePathPattern = '(' + basePath.replace(/\\/g, '/')
                    .replace(/[\-\[\]\/{}()+?.\\^$|*]/g, '\\$&') + ')?';
                pattern = pattern
                    .replace(/\\/g, '/')
                    .replace(/[\-\[\]{}()+?.\\^$|]/g, '\\$&')
                    .replace(/\*\*/g, '(?:.*)')
                    .replace(/\*/g, '(?:[^/]*)')
                    .replace(/^/, basePathPattern);
                const re = new RegExp('^' + pattern + '$');
                fileNames = fileNames.filter(x => !x.replace(/\\/g, '/').match(re));
            });
        }
        else {
            fileNames = fileNames.filter(fileName => !/\.spec\.ts$/.test(fileName));
        }
        this._rootFilePath = fileNames;
        // By default messages will be generated in basePath
        let genDir = basePath;
        if (options.hasOwnProperty('genDir')) {
            genDir = path.resolve(process.cwd(), options.genDir);
        }
        this._compilerOptions = tsConfig.options;
        this._angularCompilerOptions = Object.assign({ genDir }, this._compilerOptions, tsConfig.raw['angularCompilerOptions'], { basePath });
        this._basePath = basePath;
        this._genDir = genDir;
        // this._compilerHost = new WebpackCompilerHost(this._compilerOptions, this._basePath);
        this._compilerHost = ts.createCompilerHost(this._compilerOptions, true);
        this._program = ts.createProgram(this._rootFilePath, this._compilerOptions, this._compilerHost);
        if (options.hasOwnProperty('i18nFormat')) {
            this._i18nFormat = options.i18nFormat;
        }
        if (options.hasOwnProperty('locale')) {
            if (VERSION.major === '2') {
                console.warn("The option '--locale' is only available on the xi18n command"
                    + ' starting from Angular v4, please update to a newer version.', '\n\n');
            }
            this._locale = options.locale;
        }
        if (options.hasOwnProperty('outFile')) {
            if (VERSION.major === '2') {
                console.warn("The option '--out-file' is only available on the xi18n command"
                    + ' starting from Angular v4, please update to a newer version.', '\n\n');
            }
            this._outFile = options.outFile;
        }
    }
    apply(compiler) {
        this._compiler = compiler;
        compiler.plugin('make', (compilation, cb) => this._make(compilation, cb));
        compiler.plugin('after-emit', (compilation, cb) => {
            this._donePromise = null;
            this._compilation = null;
            compilation._ngToolsWebpackXi18nPluginInstance = null;
            cb();
        });
    }
    _make(compilation, cb) {
        this._compilation = compilation;
        if (this._compilation._ngToolsWebpackXi18nPluginInstance) {
            return cb(new Error('An @ngtools/webpack xi18n plugin already exist for ' +
                'this compilation.'));
        }
        if (!this._compilation._ngToolsWebpackPluginInstance) {
            return cb(new Error('An @ngtools/webpack aot plugin does not exists ' +
                'for this compilation'));
        }
        this._compilation._ngToolsWebpackXi18nPluginInstance = this;
        this._resourceLoader = new resource_loader_1.WebpackResourceLoader(compilation);
        this._donePromise = Promise.resolve()
            .then(() => {
            return __NGTOOLS_PRIVATE_API_2.extractI18n({
                basePath: this._basePath,
                compilerOptions: this._compilerOptions,
                program: this._program,
                host: this._compilerHost,
                angularCompilerOptions: this._angularCompilerOptions,
                i18nFormat: this._i18nFormat || '',
                locale: this._locale,
                outFile: this._outFile,
                readResource: (path) => this._resourceLoader.get(path)
            });
        })
            .then(() => cb(), (err) => {
            this._compilation.errors.push(err);
            cb(err);
        });
    }
}
exports.ExtractI18nPlugin = ExtractI18nPlugin;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/extract_i18n_plugin.js.map