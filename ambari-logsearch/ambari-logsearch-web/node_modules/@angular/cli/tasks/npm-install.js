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
        ui.writeLine(chalk.green(`Installing packages for tooling via ${packageManager}.`));
        let installCommand = `${packageManager} install`;
        if (packageManager === 'npm') {
            installCommand = `${packageManager} --quiet install`;
        }
        return new Promise((resolve, reject) => {
            child_process_1.exec(installCommand, (err, _stdout, stderr) => {
                if (err) {
                    ui.writeLine(stderr);
                    const message = 'Package install failed, see above.';
                    ui.writeLine(chalk.red(message));
                    reject(message);
                }
                else {
                    ui.writeLine(chalk.green(`Installed packages for tooling via ${packageManager}.`));
                    resolve();
                }
            });
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/npm-install.js.map