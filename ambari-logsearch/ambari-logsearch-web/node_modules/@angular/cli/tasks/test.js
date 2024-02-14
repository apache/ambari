"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
const config_1 = require("../models/config");
const require_project_module_1 = require("../utilities/require-project-module");
const app_utils_1 = require("../utilities/app-utils");
const Task = require('../ember-cli/lib/models/task');
const SilentError = require('silent-error');
exports.default = Task.extend({
    run: function (options) {
        const projectConfig = config_1.CliConfig.fromProject().config;
        const projectRoot = this.project.root;
        const appConfig = app_utils_1.getAppFromConfig(options.app);
        if (projectConfig.project && projectConfig.project.ejected) {
            throw new SilentError('An ejected project cannot use the build command anymore.');
        }
        if (appConfig.platform === 'server') {
            throw new SilentError('ng test for platform server applications is coming soon!');
        }
        return new Promise((resolve) => {
            const karma = require_project_module_1.requireProjectModule(projectRoot, 'karma');
            const karmaConfig = path.join(projectRoot, options.config ||
                config_1.CliConfig.getValue('test.karma.config'));
            let karmaOptions = Object.assign({}, options);
            // Convert browsers from a string to an array
            if (options.browsers) {
                karmaOptions.browsers = options.browsers.split(',');
            }
            karmaOptions.angularCli = {
                codeCoverage: options.codeCoverage,
                sourcemaps: options.sourcemaps,
                progress: options.progress,
                poll: options.poll,
                environment: options.environment,
                app: options.app
            };
            // Assign additional karmaConfig options to the local ngapp config
            karmaOptions.configFile = karmaConfig;
            // :shipit:
            const karmaServer = new karma.Server(karmaOptions, resolve);
            karmaServer.start();
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/test.js.map