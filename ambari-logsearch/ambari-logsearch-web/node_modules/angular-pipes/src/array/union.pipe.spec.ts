import { UnionPipe } from './union.pipe';
import { DeepPipe } from './deep.pipe';


describe('UnionPipe', () => {
    
    let pipe: UnionPipe;
    let deepPipe: DeepPipe;
    
    beforeEach(() => {
       pipe = new UnionPipe();
       deepPipe = new DeepPipe(); 
    });
    
    it('Should return the union', () => {
       
       const a = [1, 1, 1, 2, 3, 3, 4, 5];
       const b = [1, 2];
       const result = pipe.transform(a, b)
       expect(result).toEqual([1, 2, 3, 4, 5]); 
       expect(a).toEqual([1, 1, 1, 2, 3, 3, 4, 5]); // Check integrity
       expect(b).toEqual([1, 2]); // Check integrity
    });
    
    it('Should return the union #2', () => {
       
       const result = pipe.transform([1, 2], [3, 4])
       expect(result).toEqual([1, 2, 3, 4]); 
    });
    
    it('Should return an empty array', () => {
       
       expect(pipe.transform('a')).toEqual([]); 
       expect(pipe.transform([], 'a')).toEqual([]); 
       expect(pipe.transform(deepPipe.transform({ a: 1 }), [])).toEqual([]); 
    });

    
    it ('Should return the union with no deep equal', () => {
        
        const collection = [
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } },
            { a: 2, b: { c: 3 } },
            { a: 1, b: { c: 2 } }
        ];

        const collection2 = [
          { a: 1, b: { c: 2 }}
        ];

        expect(pipe.transform(collection, collection2)).toEqual(collection.concat(collection2));
    });
    
    it ('Should return union with deep equal', () => {
        
        const collection = [
            { a: 1, b: { c: 2 } },
            { a: 2, b: { c: 3 } },
            { a: 2, b: { c: 3 } },
            { a: 1, b: { c: 2 } }
        ];

        const collection2 = [
          { a: 1, b: { c: 2 }},
          { a: 2, b: { c: 3 }},
          { a: 3, b: { c: 2 }}
        ];
        
        const deep = deepPipe.transform(collection);
        
        expect(pipe.transform(deep, collection2)).toEqual([
          { a: 1, b: { c: 2 } },
          { a: 2, b: { c: 3 } },
          { a: 3, b: { c: 2 } }
        ]);
    });
});