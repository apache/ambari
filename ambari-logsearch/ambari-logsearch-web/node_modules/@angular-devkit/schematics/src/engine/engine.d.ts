/// <reference types="node" />
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import 'rxjs/add/operator/map';
import { Url } from 'url';
import { BaseException } from '../exception/exception';
import { MergeStrategy } from '../tree/interface';
import { Collection, Engine, EngineHost, Schematic, Source } from './interface';
export declare class UnknownUrlSourceProtocol extends BaseException {
    constructor(url: string);
}
export declare class UnknownCollectionException extends BaseException {
    constructor(name: string);
}
export declare class UnknownSchematicException extends BaseException {
    constructor(name: string, collection: Collection<{}, {}>);
}
export declare class SchematicEngine<CollectionT extends object, SchematicT extends object> implements Engine<CollectionT, SchematicT> {
    private _host;
    private _collectionCache;
    private _schematicCache;
    constructor(_host: EngineHost<CollectionT, SchematicT>);
    readonly defaultMergeStrategy: MergeStrategy;
    createCollection(name: string): Collection<CollectionT, SchematicT>;
    createSchematic(name: string, collection: Collection<CollectionT, SchematicT>): Schematic<CollectionT, SchematicT>;
    transformOptions<OptionT extends object, ResultT extends object>(schematic: Schematic<CollectionT, SchematicT>, options: OptionT): ResultT;
    createSourceFromUrl(url: Url): Source;
}
