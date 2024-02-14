import { WithoutPipe } from './without.pipe';
import { DeepPipe } from './deep.pipe';


describe('WithoutPipe', () => {
    
    let pipe: WithoutPipe;
    let deepPipe: DeepPipe;
    
    beforeEach(() => {
       pipe = new WithoutPipe(); 
       deepPipe = new DeepPipe();
    });
    
    it('Should return values without 1', () => {
       
       const value = [1, 1, 1, 2, 3, 3, 4, 5];
       const result = pipe.transform(value, 1)
       expect(result).toEqual([2, 3, 3, 4, 5]); 
       expect(value).toEqual([1, 1, 1, 2, 3, 3, 4, 5]);
    });
    
    it('Should return values witout 1 and 2', () => {
       
       const result = pipe.transform([1, 1, 2, 3, 4, 2, 5], 1, 2);
       expect(result).toEqual([3, 4, 5]); 
    });

    it('Should return the input unwrapped', () => {
        const wrapped = deepPipe.transform({ x: 1 });
        expect(pipe.transform(wrapped)).toEqual({ x: 1 });
    });
    
    it('Should return an empty array', () => {
       
       expect(pipe.transform([1], 1)).toEqual([]); 
    });
    
    it('Should return an empty array', () => {
       
       expect(pipe.transform([], 1)).toEqual([]); 
    });
    
    it('Should return the values without ... with deep equal', () => {
        
        const collection = [
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } },
            { a: 2, b: { c: 5 } },
            { a: 1, b: { c: 4 } }
        ];
        
        const deep = deepPipe.transform(collection);
        
        expect(pipe.transform(deep, { a: 2, b: { c: 5 }}, { a: 1, b: { c: 4 }})).toEqual([
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } }
        ]);
        
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a', null)).toEqual('a'); 
    });
});