"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const build_1 = require("./build");
const Command = require('../ember-cli/lib/models/command');
// defaults for BuildOptions
exports.baseEjectCommandOptions = [
    ...build_1.baseBuildCommandOptions,
    {
        name: 'force',
        type: Boolean,
        description: 'Overwrite any webpack.config.js and npm scripts already existing.'
    },
    {
        name: 'app',
        type: String,
        aliases: ['a'],
        description: 'Specifies app name to use.'
    }
];
const EjectCommand = Command.extend({
    name: 'eject',
    description: 'Ejects your app and output the proper webpack configuration and scripts.',
    availableOptions: exports.baseEjectCommandOptions,
    run: function (commandOptions) {
        // Default vendor chunk to false when build optimizer is on.
        if (commandOptions.vendorChunk === undefined) {
            commandOptions.vendorChunk = !commandOptions.buildOptimizer;
        }
        const EjectTask = require('../tasks/eject').default;
        const ejectTask = new EjectTask({
            project: this.project,
            ui: this.ui,
        });
        return ejectTask.run(commandOptions);
    }
});
exports.default = EjectCommand;
//# sourceMappingURL=/users/hansl/sources/angular-cli/commands/eject.js.map