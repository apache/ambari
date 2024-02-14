/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { FilePredicate, MergeStrategy } from './tree/interface';
import { Tree as TreeInterface } from './tree/interface';
export { SchematicsError } from './exception/exception';
export * from './tree/action';
export * from './engine/collection';
export * from './engine/engine';
export * from './engine/interface';
export * from './tree/interface';
export * from './rules/base';
export * from './rules/move';
export * from './rules/random';
export * from './rules/schematic';
export * from './rules/template';
export * from './rules/url';
export * from './tree/empty';
export * from './tree/filesystem';
export * from './tree/memory-host';
export * from './tree/virtual';
export { UpdateRecorder } from './tree/interface';
export * from './engine/schematic';
export * from './sink/dryrun';
export { FileSystemSink } from './sink/filesystem';
export * from './utility/path';
export interface TreeConstructor {
    empty(): TreeInterface;
    branch(tree: TreeInterface): TreeInterface;
    merge(tree: TreeInterface, other: TreeInterface, strategy?: MergeStrategy): TreeInterface;
    partition(tree: TreeInterface, predicate: FilePredicate<boolean>): [TreeInterface, TreeInterface];
    optimize(tree: TreeInterface): TreeInterface;
}
export declare type Tree = TreeInterface;
export declare const Tree: TreeConstructor;
