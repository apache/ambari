"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
const common_tags_1 = require("common-tags");
const webpack_1 = require("@ngtools/webpack");
const SilentError = require('silent-error');
const g = global;
const webpackLoader = g['angularCliIsLocal']
    ? g.angularCliPackages['@ngtools/webpack'].main
    : '@ngtools/webpack';
function _createAotPlugin(wco, options) {
    const { appConfig, projectRoot, buildOptions } = wco;
    // Read the environment, and set it in the compiler host.
    let hostReplacementPaths = {};
    // process environment file replacement
    if (appConfig.environments) {
        if (!appConfig.environmentSource) {
            let migrationMessage = '';
            if ('source' in appConfig.environments) {
                migrationMessage = '\n\n' + common_tags_1.stripIndent `
          A new environmentSource entry replaces the previous source entry inside environments.

          To migrate angular-cli.json follow the example below:

          Before:

          "environments": {
            "source": "environments/environment.ts",
            "dev": "environments/environment.ts",
            "prod": "environments/environment.prod.ts"
          }


          After:

          "environmentSource": "environments/environment.ts",
          "environments": {
            "dev": "environments/environment.ts",
            "prod": "environments/environment.prod.ts"
          }
        `;
            }
            throw new SilentError(`Environment configuration does not contain "environmentSource" entry.${migrationMessage}`);
        }
        if (!(buildOptions.environment in appConfig.environments)) {
            throw new SilentError(`Environment "${buildOptions.environment}" does not exist.`);
        }
        const appRoot = path.resolve(projectRoot, appConfig.root);
        const sourcePath = appConfig.environmentSource;
        const envFile = appConfig.environments[buildOptions.environment];
        hostReplacementPaths = {
            [path.resolve(appRoot, sourcePath)]: path.resolve(appRoot, envFile)
        };
    }
    return new webpack_1.AotPlugin(Object.assign({}, {
        mainPath: path.join(projectRoot, appConfig.root, appConfig.main),
        i18nFile: buildOptions.i18nFile,
        i18nFormat: buildOptions.i18nFormat,
        locale: buildOptions.locale,
        replaceExport: appConfig.platform === 'server',
        missingTranslation: buildOptions.missingTranslation,
        hostReplacementPaths,
        // If we don't explicitely list excludes, it will default to `['**/*.spec.ts']`.
        exclude: []
    }, options));
}
exports.getNonAotConfig = function (wco) {
    const { appConfig, projectRoot } = wco;
    const tsConfigPath = path.resolve(projectRoot, appConfig.root, appConfig.tsconfig);
    return {
        module: { rules: [{ test: /\.ts$/, loader: webpackLoader }] },
        plugins: [_createAotPlugin(wco, { tsConfigPath, skipCodeGeneration: true })]
    };
};
exports.getAotConfig = function (wco) {
    const { projectRoot, buildOptions, appConfig } = wco;
    const tsConfigPath = path.resolve(projectRoot, appConfig.root, appConfig.tsconfig);
    const testTsConfigPath = path.resolve(projectRoot, appConfig.root, appConfig.testTsconfig);
    let pluginOptions = { tsConfigPath };
    // Fallback to exclude spec files from AoT compilation on projects using a shared tsconfig.
    if (testTsConfigPath === tsConfigPath) {
        let exclude = ['**/*.spec.ts'];
        if (appConfig.test) {
            exclude.push(path.join(projectRoot, appConfig.root, appConfig.test));
        }
        pluginOptions.exclude = exclude;
    }
    let boLoader = [];
    if (buildOptions.buildOptimizer) {
        boLoader = [{
                loader: '@angular-devkit/build-optimizer/webpack-loader',
                options: { sourceMap: buildOptions.sourcemaps }
            }];
    }
    return {
        module: { rules: [{ test: /\.ts$/, use: [...boLoader, webpackLoader] }] },
        plugins: [_createAotPlugin(wco, pluginOptions)]
    };
};
exports.getNonAotTestConfig = function (wco) {
    const { projectRoot, appConfig } = wco;
    const tsConfigPath = path.resolve(projectRoot, appConfig.root, appConfig.testTsconfig);
    const appTsConfigPath = path.resolve(projectRoot, appConfig.root, appConfig.tsconfig);
    let pluginOptions = { tsConfigPath, skipCodeGeneration: true };
    // Fallback to correct module format on projects using a shared tsconfig.
    if (tsConfigPath === appTsConfigPath) {
        pluginOptions.compilerOptions = { module: 'commonjs' };
    }
    return {
        module: { rules: [{ test: /\.ts$/, loader: webpackLoader }] },
        plugins: [_createAotPlugin(wco, pluginOptions)]
    };
};
//# sourceMappingURL=/users/hansl/sources/angular-cli/models/webpack-configs/typescript.js.map