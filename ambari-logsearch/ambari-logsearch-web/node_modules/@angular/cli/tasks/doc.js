"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Task = require('../ember-cli/lib/models/task');
const opn = require('opn');
exports.DocTask = Task.extend({
    run: function (keyword, search) {
        const searchUrl = search ? `https://www.google.com/search?q=site%3Aangular.io+${keyword}` :
            `https://angular.io/api?query=${keyword}`;
        return opn(searchUrl, { wait: false });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/doc.js.map