"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const common_tags_1 = require("common-tags");
const config_1 = require("../models/config");
const Command = require('../ember-cli/lib/models/command');
exports.default = Command.extend({
    name: 'lint',
    aliases: ['l'],
    description: 'Lints code in existing project.',
    works: 'insideProject',
    availableOptions: [
        {
            name: 'fix',
            type: Boolean,
            default: false,
            description: 'Fixes linting errors (may overwrite linted files).'
        },
        {
            name: 'type-check',
            type: Boolean,
            default: false,
            description: 'Controls the type check for linting.'
        },
        {
            name: 'force',
            type: Boolean,
            default: false,
            description: 'Succeeds even if there was linting errors.'
        },
        {
            name: 'format',
            aliases: ['t'],
            type: String,
            default: 'prose',
            description: common_tags_1.oneLine `
        Output format (prose, json, stylish, verbose, pmd, msbuild, checkstyle, vso, fileslist).
      `
        }
    ],
    run: function (commandOptions) {
        const LintTask = require('../tasks/lint').default;
        const lintTask = new LintTask({
            ui: this.ui,
            project: this.project
        });
        return lintTask.run(Object.assign({}, commandOptions, { configs: config_1.CliConfig.fromProject().config.lint }));
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/commands/lint.js.map