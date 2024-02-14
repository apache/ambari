"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const link_cli_1 = require("../tasks/link-cli");
const npm_install_1 = require("../tasks/npm-install");
const validate_project_name_1 = require("../utilities/validate-project-name");
const check_package_manager_1 = require("../utilities/check-package-manager");
const config_1 = require("../models/config");
const Task = require('../ember-cli/lib/models/task');
const SilentError = require('silent-error');
const GitInit = require('../tasks/git-init');
const packageJson = require('../package.json');
exports.default = Task.extend({
    run: function (commandOptions, rawArgs) {
        if (commandOptions.dryRun) {
            commandOptions.skipInstall = true;
        }
        // needs an explicit check in case it's just 'undefined'
        // due to passing of options from 'new' and 'addon'
        let gitInit;
        if (commandOptions.skipGit === false) {
            gitInit = new GitInit({
                ui: this.ui,
                project: this.project
            });
        }
        const packageManager = config_1.CliConfig.fromGlobal().get('packageManager');
        let npmInstall;
        if (!commandOptions.skipInstall) {
            npmInstall = new npm_install_1.default({
                ui: this.ui,
                project: this.project,
                packageManager
            });
        }
        let linkCli;
        if (commandOptions.linkCli) {
            linkCli = new link_cli_1.default({
                ui: this.ui,
                project: this.project,
                packageManager
            });
        }
        const project = this.project;
        const packageName = commandOptions.name !== '.' && commandOptions.name || project.name();
        if (commandOptions.style === undefined) {
            commandOptions.style = config_1.CliConfig.fromGlobal().get('defaults.styleExt');
        }
        if (!packageName) {
            const message = 'The `ng ' + this.name + '` command requires a ' +
                'package.json in current folder with name attribute or a specified name via arguments. ' +
                'For more details, use `ng help`.';
            return Promise.reject(new SilentError(message));
        }
        validate_project_name_1.validateProjectName(packageName);
        const SchematicRunTask = require('../tasks/schematic-run').default;
        const schematicRunTask = new SchematicRunTask({
            ui: this.ui,
            project: this.project
        });
        const cwd = this.project.root;
        const schematicName = config_1.CliConfig.fromGlobal().get('defaults.schematics.newApp');
        commandOptions.version = packageJson.version;
        const runOptions = {
            taskOptions: commandOptions,
            workingDir: cwd,
            emptyHost: true,
            collectionName: commandOptions.collectionName,
            schematicName
        };
        return schematicRunTask.run(runOptions)
            .then(function () {
            if (!commandOptions.dryRun) {
                process.chdir(commandOptions.directory);
            }
        })
            .then(function () {
            if (!commandOptions.skipInstall) {
                return check_package_manager_1.checkYarnOrCNPM().then(() => npmInstall.run());
            }
        })
            .then(function () {
            if (!commandOptions.dryRun && commandOptions.skipGit === false) {
                return gitInit.run(commandOptions, rawArgs);
            }
        })
            .then(function () {
            if (!commandOptions.dryRun && commandOptions.linkCli) {
                return linkCli.run();
            }
        })
            .then(() => {
            if (!commandOptions.dryRun) {
                this.ui.writeLine(chalk.green(`Project '${packageName}' successfully created.`));
            }
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/init.js.map