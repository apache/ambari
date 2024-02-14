export interface F0<R> {
    (): R;
}
export interface F1<A0, R> {
    (a0: A0): R;
}
export interface F2<A0, A1, R> {
    (a0: A0, a1: A1): R;
}
export interface F3<A0, A1, A2, R> {
    (a0: A0, a1: A1, a2: A2): R;
}
export declare class Maybe<T> {
    private t;
    static nothing: Maybe<any>;
    static lift<T>(t: T): Maybe<any>;
    static all<T0, T1>(t0: Maybe<T0>, t1: Maybe<T1>): Maybe<[T0, T1]>;
    bind<R>(fn: F1<T, Maybe<R>>): Maybe<R>;
    fmap<R>(fn: F1<T, R>): Maybe<R>;
    readonly isNothing: boolean;
    readonly isSomething: boolean;
    catch(def: () => Maybe<T>): Maybe<T>;
    unwrap(): T | undefined;
    private constructor(t);
}
export declare function unwrapFirst<T>(ts: Maybe<T>[]): T | undefined;
export declare function all<T>(...preds: F1<T, boolean>[]): F1<T, boolean>;
export declare function any<T>(...preds: F1<T, boolean>[]): F1<T, boolean>;
export declare function ifTrue<T>(pred: F1<T, boolean>): F1<T, Maybe<T>>;
export declare function listToMaybe<T>(ms: Maybe<T>[]): Maybe<T[]>;
