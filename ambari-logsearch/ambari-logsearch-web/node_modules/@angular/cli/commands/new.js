"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
const path = require("path");
const chalk = require("chalk");
const init_1 = require("./init");
const config_1 = require("../models/config");
const validate_project_name_1 = require("../utilities/validate-project-name");
const common_tags_1 = require("common-tags");
const Command = require('../ember-cli/lib/models/command');
const Project = require('../ember-cli/lib/models/project');
const SilentError = require('silent-error');
const NewCommand = Command.extend({
    name: 'new',
    aliases: ['n'],
    description: `Creates a new directory and a new Angular app eg. "ng new [name]".`,
    works: 'outsideProject',
    availableOptions: [
        {
            name: 'dry-run',
            type: Boolean,
            default: false,
            aliases: ['d'],
            description: common_tags_1.oneLine `
        Run through without making any changes.
        Will list all files that would have been created when running "ng new".
      `
        },
        {
            name: 'verbose',
            type: Boolean,
            default: false,
            aliases: ['v'],
            description: 'Adds more details to output logging.'
        },
        {
            name: 'link-cli',
            type: Boolean,
            default: false,
            aliases: ['lc'],
            description: 'Automatically link the `@angular/cli` package.',
            hidden: true
        },
        {
            name: 'skip-install',
            type: Boolean,
            default: false,
            aliases: ['si'],
            description: 'Skip installing packages.'
        },
        {
            name: 'skip-git',
            type: Boolean,
            default: false,
            aliases: ['sg'],
            description: 'Skip initializing a git repository.'
        },
        {
            name: 'skip-commit',
            type: Boolean,
            default: false,
            aliases: ['sc'],
            description: 'Skip committing the first commit to git.'
        },
        {
            name: 'collection',
            type: String,
            aliases: ['c'],
            description: 'Schematics collection to use.'
        }
    ],
    isProject: function (projectPath) {
        return config_1.CliConfig.fromProject(projectPath) !== null;
    },
    getCollectionName(rawArgs) {
        let collectionName = config_1.CliConfig.fromGlobal().get('defaults.schematics.collection');
        if (rawArgs) {
            const parsedArgs = this.parseArgs(rawArgs, false);
            if (parsedArgs.options.collection) {
                collectionName = parsedArgs.options.collection;
            }
        }
        return collectionName;
    },
    beforeRun: function (rawArgs) {
        const isHelp = ['--help', '-h'].includes(rawArgs[0]);
        if (isHelp) {
            return;
        }
        const schematicName = config_1.CliConfig.getValue('defaults.schematics.newApp');
        if (/^\d/.test(rawArgs[1])) {
            SilentError.debugOrThrow('@angular/cli/commands/generate', `The \`ng new ${rawArgs[0]}\` file name cannot begin with a digit.`);
        }
        const SchematicGetOptionsTask = require('../tasks/schematic-get-options').default;
        const getOptionsTask = new SchematicGetOptionsTask({
            ui: this.ui,
            project: this.project
        });
        return getOptionsTask.run({
            schematicName,
            collectionName: this.getCollectionName(rawArgs)
        })
            .then((availableOptions) => {
            this.registerOptions({
                availableOptions: availableOptions
            });
        });
    },
    run: function (commandOptions, rawArgs) {
        const packageName = rawArgs.shift();
        if (!packageName) {
            return Promise.reject(new SilentError(`The "ng ${this.name}" command requires a name argument to be specified eg. ` +
                chalk.yellow('ng new [name] ') +
                `For more details, use "ng help".`));
        }
        validate_project_name_1.validateProjectName(packageName);
        commandOptions.name = packageName;
        if (commandOptions.dryRun) {
            commandOptions.skipGit = true;
        }
        commandOptions.directory = commandOptions.directory || packageName;
        const directoryName = path.join(process.cwd(), commandOptions.directory);
        if (fs.existsSync(directoryName) && this.isProject(directoryName)) {
            throw new SilentError(common_tags_1.oneLine `
        Directory ${directoryName} exists and is already an Angular CLI project.
      `);
        }
        if (commandOptions.collection) {
            commandOptions.collectionName = commandOptions.collection;
        }
        else {
            commandOptions.collectionName = this.getCollectionName(rawArgs);
        }
        const initCommand = new init_1.default({
            ui: this.ui,
            tasks: this.tasks,
            project: Project.nullProject(this.ui, this.cli)
        });
        return Promise.resolve()
            .then(initCommand.run.bind(initCommand, commandOptions, rawArgs));
    }
});
NewCommand.overrideCore = true;
exports.default = NewCommand;
//# sourceMappingURL=/users/hansl/sources/angular-cli/commands/new.js.map