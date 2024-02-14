import { FlattenPipe } from './flatten.pipe';
import { DeepPipe }  from './deep.pipe';


describe('FlattenPipe', () => {
    
    let pipe: FlattenPipe;
    let deep: DeepPipe;

    beforeEach(() => {
       pipe = new FlattenPipe(); 
       deep = new DeepPipe();
    });
   
    it('Should return the flattened array', () => {
        const value = [[1,2,3]];
        expect(pipe.transform(value)).toEqual([1,2,3]);
    });

    it('Should return the flattened array#2', () => {
        const value = [[1,2, [3,4,5]]];
        expect(pipe.transform(value)).toEqual([1,2,[3,4,5]]);
    });


    it('Should return the same array', () => {
        const value = [1,2,3];
        expect(pipe.transform(value)).toEqual([1,2,3]);
    });

    it('Should return a deep flattened array', () => {
        const value = [
            1, 2,
            [3, 4, 5, [6, 7, 8]],
            [9, 10, [11]],
            [12],
            13,
            [[[[14, 15]]]]
        ];
        
        expect(
            pipe.transform(deep.transform(value))
        ).toEqual([
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        ]);
    });

    it('Should return the input', () => {
        expect(pipe.transform('a')).toEqual('a');
    });

    it('Should return the input unwrapped', () => {
        const wrapped = deep.transform({ x: 1 });
        expect(pipe.transform(wrapped)).toEqual({ x: 1 });
    });
});