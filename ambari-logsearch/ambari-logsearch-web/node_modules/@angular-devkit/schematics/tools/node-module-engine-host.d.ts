/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { CollectionDescription, RuleFactory, SchematicDescription } from '@angular-devkit/schematics';
import { FileSystemCollectionDescription, FileSystemSchematicDescription } from './description';
import { FileSystemEngineHostBase } from './file-system-engine-host-base';
/**
 * Used to simplify typings.
 */
export declare type FileSystemCollectionDesc = CollectionDescription<FileSystemCollectionDescription>;
export declare type FileSystemSchematicDesc = SchematicDescription<FileSystemCollectionDescription, FileSystemSchematicDescription>;
/**
 * A simple EngineHost that uses NodeModules to resolve collections.
 */
export declare class NodeModulesEngineHost extends FileSystemEngineHostBase {
    protected _resolveCollectionPath(name: string): string | null;
    protected _resolveReferenceString(refString: string, parentPath: string): {
        ref: RuleFactory<{}>;
        path: string;
    } | null;
    protected _transformCollectionDescription(name: string, desc: Partial<FileSystemCollectionDesc>): CollectionDescription<FileSystemCollectionDescription> | null;
    protected _transformSchematicDescription(_name: string, _collection: FileSystemCollectionDesc, desc: Partial<FileSystemSchematicDesc>): SchematicDescription<FileSystemCollectionDescription, FileSystemSchematicDescription> | null;
}
