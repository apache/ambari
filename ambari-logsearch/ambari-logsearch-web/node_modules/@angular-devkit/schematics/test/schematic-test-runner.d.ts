/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Tree } from '@angular-devkit/schematics';
import { Observable } from 'rxjs/Observable';
export interface SchematicSchemaT {
}
export declare class SchematicTestRunner {
    private collectionName;
    private engineHost;
    private engine;
    private collection;
    constructor(collectionName: string);
    private prepareCollection();
    runSchematicAsync(schematicName: string, opts?: SchematicSchemaT, tree?: Tree): Observable<Tree>;
    runSchematic(schematicName: string, opts?: SchematicSchemaT, tree?: Tree): Tree;
}
