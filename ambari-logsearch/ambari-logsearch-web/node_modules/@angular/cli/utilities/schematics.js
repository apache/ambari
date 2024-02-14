"use strict";
/**
 * Refer to the angular shematics library to let the dependency validator
 * know it is used..
 *
 * require('@schematics/angular')
 */
Object.defineProperty(exports, "__esModule", { value: true });
const schematics_1 = require("@angular-devkit/schematics");
const tools_1 = require("@angular-devkit/schematics/tools");
const json_schema_1 = require("@ngtools/json-schema");
require("rxjs/add/operator/concatMap");
require("rxjs/add/operator/map");
const SilentError = require('silent-error');
function getEngineHost() {
    const engineHost = new tools_1.NodeModulesEngineHost();
    return engineHost;
}
exports.getEngineHost = getEngineHost;
function getCollection(collectionName) {
    const engineHost = getEngineHost();
    const engine = new schematics_1.SchematicEngine(engineHost);
    // Add support for schemaJson.
    engineHost.registerOptionsTransform((schematic, options) => {
        if (schematic.schema) {
            const SchemaMetaClass = json_schema_1.SchemaClassFactory(schematic.schemaJson);
            const schemaClass = new SchemaMetaClass(options);
            return schemaClass.$$root();
        }
        return options;
    });
    const collection = engine.createCollection(collectionName);
    if (collection === null) {
        throw new SilentError(`Invalid collection (${collectionName}).`);
    }
    return collection;
}
exports.getCollection = getCollection;
function getSchematic(collection, schematicName) {
    return collection.createSchematic(schematicName);
}
exports.getSchematic = getSchematic;
//# sourceMappingURL=/users/hansl/sources/angular-cli/utilities/schematics.js.map