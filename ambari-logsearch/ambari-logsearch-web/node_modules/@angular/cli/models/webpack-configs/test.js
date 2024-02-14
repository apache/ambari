"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
const glob = require("glob");
const webpack = require("webpack");
const config_1 = require("../config");
/**
 * Enumerate loaders and their dependencies from this file to let the dependency validator
 * know they are used.
 *
 * require('istanbul-instrumenter-loader')
 *
 */
function getTestConfig(testConfig) {
    const configPath = config_1.CliConfig.configFilePath();
    const projectRoot = path.dirname(configPath);
    const appConfig = config_1.CliConfig.fromProject().config.apps[0];
    const nodeModules = path.resolve(projectRoot, 'node_modules');
    const extraRules = [];
    const extraPlugins = [];
    if (testConfig.codeCoverage && config_1.CliConfig.fromProject()) {
        const codeCoverageExclude = config_1.CliConfig.fromProject().get('test.codeCoverage.exclude');
        let exclude = [
            /\.(e2e|spec)\.ts$/,
            /node_modules/
        ];
        if (codeCoverageExclude) {
            codeCoverageExclude.forEach((excludeGlob) => {
                const excludeFiles = glob
                    .sync(path.join(projectRoot, excludeGlob), { nodir: true })
                    .map(file => path.normalize(file));
                exclude.push(...excludeFiles);
            });
        }
        extraRules.push({
            test: /\.(js|ts)$/, loader: 'istanbul-instrumenter-loader',
            options: { esModules: true },
            enforce: 'post',
            exclude
        });
    }
    return {
        devtool: testConfig.sourcemaps ? 'inline-source-map' : 'eval',
        entry: {
            main: path.resolve(projectRoot, appConfig.root, appConfig.test)
        },
        module: {
            rules: [].concat(extraRules)
        },
        plugins: [
            new webpack.optimize.CommonsChunkPlugin({
                minChunks: Infinity,
                name: 'inline'
            }),
            new webpack.optimize.CommonsChunkPlugin({
                name: 'vendor',
                chunks: ['main'],
                minChunks: (module) => module.resource && module.resource.startsWith(nodeModules)
            })
        ].concat(extraPlugins)
    };
}
exports.getTestConfig = getTestConfig;
//# sourceMappingURL=/users/hansl/sources/angular-cli/models/webpack-configs/test.js.map