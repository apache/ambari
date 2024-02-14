export interface Host {
    write(path: string, content: string): Promise<void>;
    read(path: string): Promise<string>;
}
export declare const NodeHost: Host;
export interface Change {
    apply(host: Host): Promise<void>;
    readonly path: string | null;
    readonly order: number;
    readonly description: string;
}
/**
 * An operation that does nothing.
 */
export declare class NoopChange implements Change {
    description: string;
    order: number;
    path: string;
    apply(): Promise<void>;
}
/**
 * An operation that mixes two or more changes, and merge them (in order).
 * Can only apply to a single file. Use a ChangeManager to apply changes to multiple
 * files.
 */
export declare class MultiChange implements Change {
    private _path;
    private _changes;
    constructor(...changes: (Change[] | Change)[]);
    appendChange(change: Change): void;
    readonly description: string;
    readonly order: number;
    readonly path: string;
    apply(host: Host): Promise<void>;
}
/**
 * Will add text to the source code.
 */
export declare class InsertChange implements Change {
    path: string;
    private pos;
    private toAdd;
    order: number;
    description: string;
    constructor(path: string, pos: number, toAdd: string);
    /**
     * This method does not insert spaces if there is none in the original string.
     */
    apply(host: Host): Promise<any>;
}
/**
 * Will remove text from the source code.
 */
export declare class RemoveChange implements Change {
    path: string;
    private pos;
    private toRemove;
    order: number;
    description: string;
    constructor(path: string, pos: number, toRemove: string);
    apply(host: Host): Promise<any>;
}
/**
 * Will replace text from the source code.
 */
export declare class ReplaceChange implements Change {
    path: string;
    private pos;
    private oldText;
    private newText;
    order: number;
    description: string;
    constructor(path: string, pos: number, oldText: string, newText: string);
    apply(host: Host): Promise<any>;
}
