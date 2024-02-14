import { TimeUnit } from '../../bs-moment/types';
export declare type CreateMatrixCb<T> = (date: Date) => T;
export interface MatrixOptions {
    height: number;
    width: number;
    initialDate: Date;
    shift: TimeUnit;
}
export declare function createMatrix<T>(options: MatrixOptions, fn: CreateMatrixCb<T>): T[][];
