"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs_1 = require("fs");
const vm = require("vm");
const path = require("path");
const NodeTemplatePlugin = require('webpack/lib/node/NodeTemplatePlugin');
const NodeTargetPlugin = require('webpack/lib/node/NodeTargetPlugin');
const LoaderTargetPlugin = require('webpack/lib/LoaderTargetPlugin');
const SingleEntryPlugin = require('webpack/lib/SingleEntryPlugin');
class WebpackResourceLoader {
    constructor(_parentCompilation) {
        this._parentCompilation = _parentCompilation;
        this._uniqueId = 0;
        this._context = _parentCompilation.context;
    }
    _compile(filePath, _content) {
        const compilerName = `compiler(${this._uniqueId++})`;
        const outputOptions = { filename: filePath };
        const relativePath = path.relative(this._context || '', filePath);
        const childCompiler = this._parentCompilation.createChildCompiler(relativePath, outputOptions);
        childCompiler.context = this._context;
        childCompiler.apply(new NodeTemplatePlugin(outputOptions), new NodeTargetPlugin(), new SingleEntryPlugin(this._context, filePath), new LoaderTargetPlugin('node'));
        // Store the result of the parent compilation before we start the child compilation
        let assetsBeforeCompilation = Object.assign({}, this._parentCompilation.assets[outputOptions.filename]);
        // Fix for "Uncaught TypeError: __webpack_require__(...) is not a function"
        // Hot module replacement requires that every child compiler has its own
        // cache. @see https://github.com/ampedandwired/html-webpack-plugin/pull/179
        childCompiler.plugin('compilation', function (compilation) {
            if (compilation.cache) {
                if (!compilation.cache[compilerName]) {
                    compilation.cache[compilerName] = {};
                }
                compilation.cache = compilation.cache[compilerName];
            }
        });
        // Compile and return a promise
        return new Promise((resolve, reject) => {
            childCompiler.runAsChild((err, entries, childCompilation) => {
                // Resolve / reject the promise
                if (childCompilation && childCompilation.errors && childCompilation.errors.length) {
                    const errorDetails = childCompilation.errors.map(function (error) {
                        return error.message + (error.error ? ':\n' + error.error : '');
                    }).join('\n');
                    reject(new Error('Child compilation failed:\n' + errorDetails));
                }
                else if (err) {
                    reject(err);
                }
                else {
                    // Replace [hash] placeholders in filename
                    const outputName = this._parentCompilation.mainTemplate.applyPluginsWaterfall('asset-path', outputOptions.filename, {
                        hash: childCompilation.hash,
                        chunk: entries[0]
                    });
                    // Restore the parent compilation to the state like it was before the child compilation.
                    Object.keys(childCompilation.assets).forEach((fileName) => {
                        // If it wasn't there and it's a source file (absolute path) - delete it.
                        if (assetsBeforeCompilation[fileName] === undefined && path.isAbsolute(fileName)) {
                            delete this._parentCompilation.assets[fileName];
                        }
                        else {
                            // Otherwise, add it to the parent compilation.
                            this._parentCompilation.assets[fileName] = childCompilation.assets[fileName];
                        }
                    });
                    resolve({
                        // Hash of the template entry point.
                        hash: entries[0].hash,
                        // Output name.
                        outputName: outputName,
                        // Compiled code.
                        content: childCompilation.assets[outputName].source()
                    });
                }
            });
        });
    }
    _evaluate(fileName, source) {
        try {
            const vmContext = vm.createContext(Object.assign({ require: require }, global));
            const vmScript = new vm.Script(source, { filename: fileName });
            // Evaluate code and cast to string
            let newSource;
            newSource = vmScript.runInContext(vmContext);
            if (typeof newSource == 'string') {
                return Promise.resolve(newSource);
            }
            return Promise.reject('The loader "' + fileName + '" didn\'t return a string.');
        }
        catch (e) {
            return Promise.reject(e);
        }
    }
    get(filePath) {
        return this._compile(filePath, fs_1.readFileSync(filePath, 'utf8'))
            .then((result) => this._evaluate(result.outputName, result.content));
    }
}
exports.WebpackResourceLoader = WebpackResourceLoader;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/resource_loader.js.map