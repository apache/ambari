"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
const path = require("path");
const glob = require("glob");
const denodeify = require("denodeify");
const is_directory_1 = require("../utilities/is-directory");
const flattenDeep = require('lodash/flattenDeep');
const globPromise = denodeify(glob);
const statPromise = denodeify(fs.stat);
// Adds an asset to the compilation assets;
function addAsset(compilation, asset) {
    const realPath = path.resolve(asset.originPath, asset.relativePath);
    // Make sure that asset keys use forward slashes, otherwise webpack dev server
    const servedPath = path.join(asset.destinationPath, asset.relativePath).replace(/\\/g, '/');
    // Don't re-add existing assets.
    if (compilation.assets[servedPath]) {
        return Promise.resolve();
    }
    // Read file and add it to assets;
    return statPromise(realPath)
        .then((stat) => compilation.assets[servedPath] = {
        size: () => stat.size,
        source: () => fs.readFileSync(realPath)
    });
}
class GlobCopyWebpackPlugin {
    constructor(options) {
        this.options = options;
    }
    apply(compiler) {
        let { patterns, globOptions } = this.options;
        const defaultCwd = globOptions.cwd || compiler.options.context;
        // Force nodir option, since we can't add dirs to assets.
        globOptions.nodir = true;
        // Process patterns.
        patterns = patterns.map(pattern => {
            // Convert all string patterns to Pattern type.
            pattern = typeof pattern === 'string' ? { glob: pattern } : pattern;
            // Add defaults
            // Input is always resolved relative to the defaultCwd (appRoot)
            pattern.input = path.resolve(defaultCwd, pattern.input || '');
            pattern.output = pattern.output || '';
            pattern.glob = pattern.glob || '';
            // Convert dir patterns to globs.
            if (is_directory_1.isDirectory(path.resolve(pattern.input, pattern.glob))) {
                pattern.glob = pattern.glob + '/**/*';
            }
            return pattern;
        });
        compiler.plugin('emit', (compilation, cb) => {
            // Create an array of promises for each pattern glob
            const globs = patterns.map((pattern) => new Promise((resolve, reject) => 
            // Individual patterns can override cwd
            globPromise(pattern.glob, Object.assign({}, globOptions, { cwd: pattern.input }))
                .then((globResults) => globResults.map(res => ({
                originPath: pattern.input,
                destinationPath: pattern.output,
                relativePath: res
            })))
                .then((asset) => resolve(asset))
                .catch(reject)));
            // Wait for all globs.
            Promise.all(globs)
                .then(assets => flattenDeep(assets))
                .then(assets => Promise.all(assets.map((asset) => addAsset(compilation, asset))))
                .then(() => cb());
        });
    }
}
exports.GlobCopyWebpackPlugin = GlobCopyWebpackPlugin;
//# sourceMappingURL=/users/hansl/sources/angular-cli/plugins/glob-copy-webpack-plugin.js.map