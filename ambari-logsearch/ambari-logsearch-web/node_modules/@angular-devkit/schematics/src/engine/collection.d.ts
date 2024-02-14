/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SchematicEngine } from './engine';
import { Collection, CollectionDescription, Schematic } from './interface';
export declare class CollectionImpl<CollectionT extends object, SchematicT extends object> implements Collection<CollectionT, SchematicT> {
    private _description;
    private _engine;
    constructor(_description: CollectionDescription<CollectionT>, _engine: SchematicEngine<CollectionT, SchematicT>);
    readonly description: CollectionDescription<CollectionT>;
    readonly name: string;
    createSchematic(name: string): Schematic<CollectionT, SchematicT>;
}
