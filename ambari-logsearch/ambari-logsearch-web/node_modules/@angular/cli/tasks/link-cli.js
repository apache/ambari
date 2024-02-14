"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Task = require('../ember-cli/lib/models/task');
const chalk = require("chalk");
const child_process_1 = require("child_process");
exports.default = Task.extend({
    run: function () {
        const ui = this.ui;
        let packageManager = this.packageManager;
        if (packageManager === 'default') {
            packageManager = 'npm';
        }
        return new Promise(function (resolve, reject) {
            child_process_1.exec(`${packageManager} link @angular/cli`, (err) => {
                if (err) {
                    ui.writeLine(chalk.red(`Couldn't do '${packageManager} link @angular/cli'.`));
                    reject();
                }
                else {
                    ui.writeLine(chalk.green('Successfully linked to @angular/cli.'));
                    resolve();
                }
            });
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/link-cli.js.map