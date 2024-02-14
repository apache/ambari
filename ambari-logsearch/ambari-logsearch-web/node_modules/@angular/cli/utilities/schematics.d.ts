/**
 * Refer to the angular shematics library to let the dependency validator
 * know it is used..
 *
 * require('@schematics/angular')
 */
import { Collection, Schematic } from '@angular-devkit/schematics';
import { NodeModulesEngineHost } from '@angular-devkit/schematics/tools';
import 'rxjs/add/operator/concatMap';
import 'rxjs/add/operator/map';
export declare function getEngineHost(): NodeModulesEngineHost;
export declare function getCollection(collectionName: string): Collection<any, any>;
export declare function getSchematic(collection: Collection<any, any>, schematicName: string): Schematic<any, any>;
