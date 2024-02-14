"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Command = require('../ember-cli/lib/models/command');
const stringUtils = require('ember-cli-string-utils');
const chalk = require("chalk");
function pickOne(of) {
    return of[Math.floor(Math.random() * of.length)];
}
const MakeThisAwesomeCommand = Command.extend({
    name: 'make-this-awesome',
    works: 'insideProject',
    hidden: true,
    run: function (commandOptions, rawArgs) {
        this[stringUtils.camelize(this.name)](commandOptions, rawArgs);
        return Promise.resolve();
    },
    makeThisAwesome: function () {
        const phrase = pickOne([
            `You're on it, there's nothing for me to do!`,
            `Let's take a look... nope, it's all good!`,
            `You're doing fine.`,
            `You're already doing great.`,
            `Nothing to do; already awesome. Exiting.`,
            `Error 418: As Awesome As Can Get.`
        ]);
        console.log(chalk.green(phrase));
    }
});
exports.default = MakeThisAwesomeCommand;
//# sourceMappingURL=/users/hansl/sources/angular-cli/commands/easter-egg.js.map