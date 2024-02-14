/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/concatMap';
import { BaseException } from '../exception/exception';
import { Tree } from '../tree/interface';
import { Collection, Engine, RuleFactory, Schematic, SchematicDescription } from './interface';
export declare class InvalidSchematicsNameException extends BaseException {
    constructor(name: string);
}
export declare class SchematicImpl<CollectionT extends object, SchematicT extends object> implements Schematic<CollectionT, SchematicT> {
    private _description;
    private _factory;
    private _collection;
    private _engine;
    constructor(_description: SchematicDescription<CollectionT, SchematicT>, _factory: RuleFactory<any>, _collection: Collection<CollectionT, SchematicT>, _engine: Engine<CollectionT, SchematicT>);
    readonly description: SchematicDescription<CollectionT, SchematicT>;
    readonly collection: Collection<CollectionT, SchematicT>;
    call<OptionT extends object>(options: OptionT, host: Observable<Tree>): Observable<Tree>;
}
