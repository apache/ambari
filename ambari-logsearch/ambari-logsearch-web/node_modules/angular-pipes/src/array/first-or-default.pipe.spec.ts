import { FirstOrDefaultPipe } from './first-or-default.pipe';

describe('FirstOrDefaultPipe', () => {
    
    let pipe: FirstOrDefaultPipe;
    
    const array: any[] = [
        {
            a: 2,
            b: 3,
            d: { e : 4}
        },
        {
            a: 4,
            b: 3,
            d: { e : 8 }
        },
        {
            a: 3,
            b: 1,
            d: { e : 4}
        },
        {
            a: 4,
            b: 5,
            d: { e : 5}
        }
    ]
    
    beforeEach(() => {
       pipe = new FirstOrDefaultPipe(); 
    });
    
    it ('Should return only the 1 values', () => {
       
       const fn = function (item: any) {
           return item === 1;
       } 
       
       const value = [1, 2, 3, 1, 1, 4];
       expect(pipe.transform(value, fn)).toEqual(1);
    });
    
    it('Should return the objects where a is 4', () => {
       
       const fn = function (item: any) {
           return item.a === 4;
       };
       
       expect(pipe.transform(array, fn))
            .toEqual({
                a: 4,
                b: 3,
                d: { e : 8 }
            }
        ); 
    });
    
    it('Should return the objects where a is 4', () => {

       expect(pipe.transform(array, ['a', 4]))
            .toEqual({
                a: 4,
                b: 3,
                d: { e : 8 }
            }
        ); 
    });
    
    it('Should return the objects where d.e is 4', () => {

       expect(pipe.transform(array, ['d.e', 4]))
            .toEqual({
                a: 2,
                b: 3,
                d: { e : 4 }
            }); 
    });
    
    it('Should return the input', () => {
        expect((<any>pipe).transform([1])).toEqual([1]);
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a', null)).toEqual('a'); 
    });

    it('Should return the 2', () => {
        expect(pipe.transform([1, 2, 3, 4, 2, 2], 2)).toEqual(2);
    });

    it('Should return the default value', () => {
        expect(pipe.transform([1,2,3], (item) => item === 4, 5)).toEqual(5);
    });

    it('Should not return the default value', () => {
        expect(pipe.transform([1,2,3], (item) => item === 4)).toBeUndefined();
    });
    
});