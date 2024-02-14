"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const url = require("url");
const common_tags_1 = require("common-tags");
const config_1 = require("../models/config");
const require_project_module_1 = require("../utilities/require-project-module");
const app_utils_1 = require("../utilities/app-utils");
const Task = require('../ember-cli/lib/models/task');
const SilentError = require('silent-error');
exports.E2eTask = Task.extend({
    run: function (e2eTaskOptions) {
        const projectConfig = config_1.CliConfig.fromProject().config;
        const projectRoot = this.project.root;
        const protractorLauncher = require_project_module_1.requireProjectModule(projectRoot, 'protractor/built/launcher');
        const appConfig = app_utils_1.getAppFromConfig(e2eTaskOptions.app);
        if (projectConfig.project && projectConfig.project.ejected) {
            throw new SilentError('An ejected project cannot use the build command anymore.');
        }
        if (appConfig.platform === 'server') {
            throw new SilentError('ng test for platform server applications is coming soon!');
        }
        return new Promise(function () {
            let promise = Promise.resolve();
            let additionalProtractorConfig = {
                elementExplorer: e2eTaskOptions.elementExplorer
            };
            // use serve url as override for protractors baseUrl
            if (e2eTaskOptions.serve && e2eTaskOptions.publicHost) {
                let publicHost = e2eTaskOptions.publicHost;
                if (!/^\w+:\/\//.test(publicHost)) {
                    publicHost = `${e2eTaskOptions.ssl ? 'https' : 'http'}://${publicHost}`;
                }
                const clientUrl = url.parse(publicHost);
                e2eTaskOptions.publicHost = clientUrl.host;
                additionalProtractorConfig.baseUrl = url.format(clientUrl);
            }
            else if (e2eTaskOptions.serve) {
                additionalProtractorConfig.baseUrl = url.format({
                    protocol: e2eTaskOptions.ssl ? 'https' : 'http',
                    hostname: e2eTaskOptions.host,
                    port: e2eTaskOptions.port.toString()
                });
            }
            else if (e2eTaskOptions.baseHref) {
                additionalProtractorConfig.baseUrl = e2eTaskOptions.baseHref;
            }
            else if (e2eTaskOptions.port) {
                additionalProtractorConfig.baseUrl = url.format({
                    protocol: e2eTaskOptions.ssl ? 'https' : 'http',
                    hostname: e2eTaskOptions.host,
                    port: e2eTaskOptions.port.toString()
                });
            }
            if (e2eTaskOptions.specs.length !== 0) {
                additionalProtractorConfig['specs'] = e2eTaskOptions.specs;
            }
            if (e2eTaskOptions.webdriverUpdate) {
                // The webdriver-manager update command can only be accessed via a deep import.
                const webdriverDeepImport = 'webdriver-manager/built/lib/cmds/update';
                let webdriverUpdate;
                try {
                    // When using npm, webdriver is within protractor/node_modules.
                    webdriverUpdate = require_project_module_1.requireProjectModule(projectRoot, `protractor/node_modules/${webdriverDeepImport}`);
                }
                catch (e) {
                    try {
                        // When using yarn, webdriver is found as a root module.
                        webdriverUpdate = require_project_module_1.requireProjectModule(projectRoot, webdriverDeepImport);
                    }
                    catch (e) {
                        throw new SilentError(common_tags_1.stripIndents `
              Cannot automatically find webdriver-manager to update.
              Update webdriver-manager manually and run 'ng e2e --no-webdriver-update' instead.
            `);
                    }
                }
                // run `webdriver-manager update --standalone false --gecko false --quiet`
                // if you change this, update the command comment in prev line, and in `eject` task
                promise = promise.then(() => webdriverUpdate.program.run({
                    standalone: false,
                    gecko: false,
                    quiet: true
                }));
            }
            // Don't call resolve(), protractor will manage exiting the process itself
            return promise.then(() => protractorLauncher.init(e2eTaskOptions.config, additionalProtractorConfig));
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/e2e.js.map