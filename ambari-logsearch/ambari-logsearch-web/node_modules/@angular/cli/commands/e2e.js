"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const SilentError = require('silent-error');
const override_options_1 = require("../utilities/override-options");
const config_1 = require("../models/config");
const serve_1 = require("./serve");
const check_port_1 = require("../utilities/check-port");
const common_tags_1 = require("common-tags");
const Command = require('../ember-cli/lib/models/command');
const E2eCommand = Command.extend({
    name: 'e2e',
    aliases: ['e'],
    description: 'Run e2e tests in existing project.',
    works: 'insideProject',
    availableOptions: override_options_1.overrideOptions([
        ...serve_1.baseServeCommandOptions,
        {
            name: 'config',
            type: String,
            aliases: ['c'],
            description: common_tags_1.oneLine `
        Use a specific config file.
        Defaults to the protractor config file in angular-cli.json.
      `
        },
        {
            name: 'specs',
            type: Array,
            default: [],
            aliases: ['sp'],
            description: common_tags_1.oneLine `
        Override specs in the protractor config.
        Can send in multiple specs by repeating flag (ng e2e --specs=spec1.ts --specs=spec2.ts).
      `
        },
        {
            name: 'element-explorer',
            type: Boolean,
            default: false,
            aliases: ['ee'],
            description: 'Start Protractor\'s Element Explorer for debugging.'
        },
        {
            name: 'webdriver-update',
            type: Boolean,
            default: true,
            aliases: ['wu'],
            description: 'Try to update webdriver.'
        },
        {
            name: 'serve',
            type: Boolean,
            default: true,
            aliases: ['s'],
            description: common_tags_1.oneLine `
        Compile and Serve the app.
        All non-reload related serve options are also available (e.g. --port=4400).
      `
        }
    ], [
        {
            name: 'port',
            default: 0,
            description: 'The port to use to serve the application.'
        },
        {
            name: 'watch',
            default: false,
            description: 'Run build when files change.'
        },
    ]),
    run: function (commandOptions) {
        const E2eTask = require('../tasks/e2e').E2eTask;
        const e2eTask = new E2eTask({
            ui: this.ui,
            project: this.project
        });
        if (!commandOptions.config) {
            const e2eConfig = config_1.CliConfig.fromProject().config.e2e;
            if (!e2eConfig.protractor.config) {
                throw new SilentError('No protractor config found in .angular-cli.json.');
            }
            commandOptions.config = e2eConfig.protractor.config;
        }
        if (commandOptions.serve) {
            const ServeTask = require('../tasks/serve').default;
            const serve = new ServeTask({
                ui: this.ui,
                project: this.project,
            });
            // Protractor will end the proccess, so we don't need to kill the dev server
            return new Promise((resolve, reject) => {
                let firstRebuild = true;
                function rebuildCb() {
                    // don't run re-run tests on subsequent rebuilds
                    if (firstRebuild) {
                        firstRebuild = false;
                        return resolve(e2eTask.run(commandOptions));
                    }
                }
                check_port_1.checkPort(commandOptions.port, commandOptions.host)
                    .then((port) => commandOptions.port = port)
                    .then(() => serve.run(commandOptions, rebuildCb))
                    .catch(reject);
            });
        }
        else {
            return e2eTask.run(commandOptions);
        }
    }
});
exports.default = E2eCommand;
//# sourceMappingURL=/users/hansl/sources/angular-cli/commands/e2e.js.map