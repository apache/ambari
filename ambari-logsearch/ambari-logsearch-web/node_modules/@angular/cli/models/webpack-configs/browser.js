"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
const webpack = require("webpack");
const path = require("path");
const HtmlWebpackPlugin = require('html-webpack-plugin');
const package_chunk_sort_1 = require("../../utilities/package-chunk-sort");
const base_href_webpack_1 = require("../../lib/base-href-webpack");
const utils_1 = require("./utils");
function getBrowserConfig(wco) {
    const { projectRoot, buildOptions, appConfig } = wco;
    const appRoot = path.resolve(projectRoot, appConfig.root);
    let extraPlugins = [];
    // figure out which are the lazy loaded entry points
    const lazyChunks = utils_1.lazyChunksFilter([
        ...utils_1.extraEntryParser(appConfig.scripts, appRoot, 'scripts'),
        ...utils_1.extraEntryParser(appConfig.styles, appRoot, 'styles')
    ]);
    if (buildOptions.vendorChunk) {
        // Separate modules from node_modules into a vendor chunk.
        const nodeModules = path.resolve(projectRoot, 'node_modules');
        // Resolves all symlink to get the actual node modules folder.
        const realNodeModules = fs.realpathSync(nodeModules);
        // --aot puts the generated *.ngfactory.ts in src/$$_gendir/node_modules.
        const genDirNodeModules = path.resolve(appRoot, '$$_gendir', 'node_modules');
        extraPlugins.push(new webpack.optimize.CommonsChunkPlugin({
            name: 'vendor',
            chunks: ['main'],
            minChunks: (module) => {
                return module.resource
                    && (module.resource.startsWith(nodeModules)
                        || module.resource.startsWith(genDirNodeModules)
                        || module.resource.startsWith(realNodeModules));
            }
        }));
    }
    if (buildOptions.sourcemaps) {
        extraPlugins.push(new webpack.SourceMapDevToolPlugin({
            filename: '[file].map[query]',
            moduleFilenameTemplate: '[resource-path]',
            fallbackModuleFilenameTemplate: '[resource-path]?[hash]',
            sourceRoot: 'webpack:///'
        }));
    }
    if (buildOptions.commonChunk) {
        extraPlugins.push(new webpack.optimize.CommonsChunkPlugin({
            name: 'main',
            async: 'common',
            children: true,
            minChunks: 2
        }));
    }
    return {
        plugins: [
            new HtmlWebpackPlugin({
                template: path.resolve(appRoot, appConfig.index),
                filename: path.resolve(buildOptions.outputPath, appConfig.index),
                chunksSortMode: package_chunk_sort_1.packageChunkSort(appConfig),
                excludeChunks: lazyChunks,
                xhtml: true,
                minify: buildOptions.target === 'production' ? {
                    caseSensitive: true,
                    collapseWhitespace: true,
                    keepClosingSlash: true
                } : false
            }),
            new base_href_webpack_1.BaseHrefWebpackPlugin({
                baseHref: buildOptions.baseHref
            }),
            new webpack.optimize.CommonsChunkPlugin({
                minChunks: Infinity,
                name: 'inline'
            })
        ].concat(extraPlugins)
    };
}
exports.getBrowserConfig = getBrowserConfig;
//# sourceMappingURL=/users/hansl/sources/angular-cli/models/webpack-configs/browser.js.map