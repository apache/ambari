"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Command = require('../ember-cli/lib/models/command');
const doc_1 = require("../tasks/doc");
const DocCommand = Command.extend({
    name: 'doc',
    description: 'Opens the official Angular API documentation for a given keyword.',
    works: 'everywhere',
    availableOptions: [
        {
            name: 'search',
            aliases: ['s'],
            type: Boolean,
            default: false,
            description: 'Search whole angular.io instead of just api.'
        }
    ],
    anonymousOptions: [
        '<keyword>'
    ],
    run: function (commandOptions, rawArgs) {
        const keyword = rawArgs[0];
        const docTask = new doc_1.DocTask({
            ui: this.ui,
            project: this.project
        });
        return docTask.run(keyword, commandOptions.search);
    }
});
exports.default = DocCommand;
//# sourceMappingURL=/users/hansl/sources/angular-cli/commands/doc.js.map