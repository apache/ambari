"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Task = require('../ember-cli/lib/models/task');
const stringUtils = require('ember-cli-string-utils');
const config_1 = require("../models/config");
const schematics_1 = require("../utilities/schematics");
exports.default = Task.extend({
    run: function (options) {
        const collectionName = options.collectionName ||
            config_1.CliConfig.getValue('defaults.schematics.collection');
        const collection = schematics_1.getCollection(collectionName);
        const schematic = schematics_1.getSchematic(collection, options.schematicName);
        const properties = schematic.description.schemaJson.properties;
        const keys = Object.keys(properties);
        const availableOptions = keys
            .map(key => (Object.assign({}, properties[key], { name: stringUtils.dasherize(key) })))
            .map(opt => {
            let type;
            switch (opt.type) {
                case 'string':
                    type = String;
                    break;
                case 'boolean':
                    type = Boolean;
                    break;
            }
            let aliases = [];
            if (opt.alias) {
                aliases = [...aliases, opt.alias];
            }
            if (opt.aliases) {
                aliases = [...aliases, ...opt.aliases];
            }
            return Object.assign({}, opt, { aliases,
                type, default: undefined // do not carry over schematics defaults
             });
        });
        return Promise.resolve(availableOptions);
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/schematic-get-options.js.map