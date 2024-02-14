import { PluckPipe } from './pluck.pipe';


describe('PluckPipe', () => {
    
    let pipe: PluckPipe;
    let array: any[];
    
    beforeEach(() => {
       pipe = new PluckPipe(); 
       array = [{
           a: 1,
           b: {
               c: {
                   d: 2
               },
               e: 3
           }
       },
       {
           a: 2,
           b: {
               c: {
                   d: 3
               },
               e: 4
           }
       },
       {
           a: 3,
           b: {
               c: {
                   d: 4
               },
               e: 5
           }
       }]
    });
    
    it('Should return an array with the a field only', () => {
       
       expect(pipe.transform(array, 'a')).toEqual([1, 2, 3]); 
    });
    
    it('Should return an array with the field b.c.d', () => {
       
       expect(pipe.transform(array, 'b.c.d')).toEqual([2, 3, 4]); 
    });
    
    
    it('Should return an array with the field b.e', () => {
       
       expect(pipe.transform(array, 'b.e')).toEqual([3, 4, 5]); 
    });
    
    it('Should return an array with the undefined values', () => {
       
       expect(pipe.transform(array, 'b.e.f')).toEqual([undefined, undefined, undefined]); 
    });
    
    it('Should return an array with the c object', () => {
       
       expect(pipe.transform(array, 'b.c')).toEqual([{ d: 2 }, { d: 3 }, { d: 4 }]); 
    });
    
    
    it('Should return the original array', () => {
       
       // undefined to avoid typescript error
       expect(pipe.transform(array, undefined)).toEqual(array); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a', 'a')).toEqual('a'); 
    });
    
})