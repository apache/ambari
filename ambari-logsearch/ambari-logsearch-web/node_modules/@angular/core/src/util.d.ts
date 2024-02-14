declare const _global: {
    [name: string]: any;
};
export { _global as global };
export declare function getSymbolIterator(): string | symbol;
export declare function scheduleMicroTask(fn: Function): void;
export declare function looseIdentical(a: any, b: any): boolean;
export declare function stringify(token: any): string;
