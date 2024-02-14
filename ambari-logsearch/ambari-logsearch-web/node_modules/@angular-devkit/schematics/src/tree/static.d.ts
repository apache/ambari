import { FilePredicate, MergeStrategy, Tree } from './interface';
import { VirtualTree } from './virtual';
export declare function empty(): VirtualTree;
export declare function branch(tree: Tree): Tree;
export declare function merge(tree: Tree, other: Tree, strategy?: MergeStrategy): Tree;
export declare function partition(tree: Tree, predicate: FilePredicate<boolean>): [Tree, Tree];
export declare function optimize(tree: Tree): VirtualTree;
