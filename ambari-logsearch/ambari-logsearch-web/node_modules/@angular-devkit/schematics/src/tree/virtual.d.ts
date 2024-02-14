/// <reference types="node" />
import { SchematicPath } from '../utility/path';
import { Action, ActionList } from './action';
import { FileEntry, MergeStrategy, Tree, UpdateRecorder } from './interface';
/**
 * The root class of most trees.
 */
export declare class VirtualTree implements Tree {
    protected _root: Map<SchematicPath, FileEntry>;
    protected _actions: ActionList;
    protected _cacheMap: Map<SchematicPath, FileEntry>;
    /**
     * Normalize the path. Made available to subclasses to overload.
     * @param path The path to normalize.
     * @returns {string} A path that is resolved and normalized.
     */
    protected _normalizePath(path: string): SchematicPath;
    /**
     * A list of file names contained by this Tree.
     * @returns {[string]} File paths.
     */
    readonly files: string[];
    readonly root: Map<SchematicPath, FileEntry>;
    readonly staging: Map<SchematicPath, FileEntry>;
    get(path: string): FileEntry | null;
    has(path: string): boolean;
    set(entry: FileEntry): Map<SchematicPath, FileEntry>;
    exists(path: string): boolean;
    read(path: string): Buffer | null;
    beginUpdate(path: string): UpdateRecorder;
    commitUpdate(record: UpdateRecorder): void;
    overwrite(path: string, content: Buffer | string): void;
    create(path: string, content: Buffer | string): void;
    rename(path: string, to: string): void;
    delete(path: string): void;
    protected _overwrite(path: SchematicPath, content: Buffer, action?: Action): void;
    protected _create(path: SchematicPath, content: Buffer, action?: Action): void;
    protected _rename(path: SchematicPath, to: SchematicPath, action?: Action, force?: boolean): void;
    protected _delete(path: SchematicPath, action?: Action): void;
    apply(action: Action, strategy: MergeStrategy): void;
    readonly actions: Action[];
    /**
     * Allow subclasses to copy to a tree their own properties.
     * @return {Tree}
     * @private
     */
    protected _copyTo<T extends VirtualTree>(tree: T): void;
    branch(): Tree;
    merge(other: Tree, strategy?: MergeStrategy): void;
    optimize(): void;
    static branch(tree: Tree): Tree;
    static merge(tree: Tree, other: Tree, strategy?: MergeStrategy): Tree;
    static optimize(tree: Tree): VirtualTree;
}
