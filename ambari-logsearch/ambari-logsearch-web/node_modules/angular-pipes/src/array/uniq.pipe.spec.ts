import { UniqPipe } from './uniq.pipe';
import { DeepPipe } from './deep.pipe';


describe('UniqPipe', () => {
    
    let pipe: UniqPipe;
    let deepPipe: DeepPipe;
    
    beforeEach(() => {
       pipe = new UniqPipe();
       deepPipe = new DeepPipe(); 
    });
    
    it('Should return unique values', () => {
       
       const value = [1, 1, 1, 2, 3, 3, 4, 5];
       const result = pipe.transform(value)
       expect(result).toEqual([1, 2, 3, 4, 5]); 
       expect(value).toEqual([1, 1, 1, 2, 3, 3, 4, 5]); // Check integrity
    });
    
    it('Should return unique values #2', () => {
       
       const result = pipe.transform(['a', 'a', 1, 'b', 3, 3, 4, 5])
       expect(result).toEqual(['a', 1, 'b', 3, 4, 5]); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a'); 
    });

    it('Should return the input unwrapped', () => {
        const wrapped = deepPipe.transform({ x: 1 });
        expect(pipe.transform(wrapped)).toEqual({ x: 1 });
    });
    
    it ('Should return the same values with no deep equal', () => {
        
        const collection = [
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } },
            { a: 2, b: { c: 3 } },
            { a: 1, b: { c: 2 } }
        ];
        
        expect(pipe.transform(collection)).toEqual([
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } },
            { a: 2, b: { c: 3 } },
            { a: 1, b: { c: 2 } }
        ]);
    });
    
    it ('Should return unique values with deep equal', () => {
        
        const collection = [
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } },
            { a: 2, b: { c: 3 } },
            { a: 1, b: { c: 2 } }
        ];
        
        const deep = deepPipe.transform(collection);
        
        expect(pipe.transform(deep)).toEqual([
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } }
        ]);
    });
});