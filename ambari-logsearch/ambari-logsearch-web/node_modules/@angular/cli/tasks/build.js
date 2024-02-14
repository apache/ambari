"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs-extra");
const path = require("path");
const webpack = require("webpack");
const app_utils_1 = require("../utilities/app-utils");
const webpack_config_1 = require("../models/webpack-config");
const utils_1 = require("../models/webpack-configs/utils");
const config_1 = require("../models/config");
const stats_1 = require("../utilities/stats");
const Task = require('../ember-cli/lib/models/task');
const SilentError = require('silent-error');
exports.default = Task.extend({
    run: function (runTaskOptions) {
        const config = config_1.CliConfig.fromProject().config;
        const app = app_utils_1.getAppFromConfig(runTaskOptions.app);
        const outputPath = runTaskOptions.outputPath || app.outDir;
        if (this.project.root === path.resolve(outputPath)) {
            throw new SilentError('Output path MUST not be project root directory!');
        }
        if (config.project && config.project.ejected) {
            throw new SilentError('An ejected project cannot use the build command anymore.');
        }
        if (runTaskOptions.deleteOutputPath) {
            fs.removeSync(path.resolve(this.project.root, outputPath));
        }
        const webpackConfig = new webpack_config_1.NgCliWebpackConfig(runTaskOptions, app).buildConfig();
        const webpackCompiler = webpack(webpackConfig);
        const statsConfig = utils_1.getWebpackStatsConfig(runTaskOptions.verbose);
        return new Promise((resolve, reject) => {
            const callback = (err, stats) => {
                if (err) {
                    return reject(err);
                }
                const json = stats.toJson('verbose');
                if (runTaskOptions.verbose) {
                    this.ui.writeLine(stats.toString(statsConfig));
                }
                else {
                    this.ui.writeLine(stats_1.statsToString(json, statsConfig));
                }
                if (stats.hasWarnings()) {
                    this.ui.writeLine(stats_1.statsWarningsToString(json, statsConfig));
                }
                if (stats.hasErrors()) {
                    this.ui.writeError(stats_1.statsErrorsToString(json, statsConfig));
                }
                if (runTaskOptions.watch) {
                    return;
                }
                else if (runTaskOptions.statsJson) {
                    fs.writeFileSync(path.resolve(this.project.root, outputPath, 'stats.json'), JSON.stringify(stats.toJson(), null, 2));
                }
                if (stats.hasErrors()) {
                    reject();
                }
                else {
                    resolve();
                }
            };
            if (runTaskOptions.watch) {
                webpackCompiler.watch({ poll: runTaskOptions.poll }, callback);
            }
            else {
                webpackCompiler.run(callback);
            }
        })
            .catch((err) => {
            if (err) {
                this.ui.writeError('\nAn error occured during the build:\n' + ((err && err.stack) || err));
            }
            throw err;
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/build.js.map