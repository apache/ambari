import 'rxjs/add/observable/of';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import { FileOperator, Rule, Source } from '../engine/interface';
import { FilePredicate, MergeStrategy, Tree } from '../tree/interface';
/**
 * A Source that returns an tree as its single value.
 */
export declare function source(tree: Tree): Source;
/**
 * A source that returns an empty tree.
 */
export declare function empty(): Source;
/**
 * Chain multiple rules into a single rule.
 */
export declare function chain(rules: Rule[]): Rule;
/**
 * Apply multiple rules to a source, and returns the source transformed.
 */
export declare function apply(source: Source, rules: Rule[]): Source;
/**
 * Merge an input tree with the source passed in.
 */
export declare function mergeWith(source: Source, strategy?: MergeStrategy): Rule;
export declare function noop(): Rule;
export declare function filter(predicate: FilePredicate<boolean>): Rule;
export declare function asSource(rule: Rule): Source;
export declare function branchAndMerge(rule: Rule): Rule;
export declare function when(predicate: FilePredicate<boolean>, operator: FileOperator): FileOperator;
export declare function forEach(operator: FileOperator): Rule;
