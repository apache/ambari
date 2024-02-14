import { WherePipe } from './where.pipe';


describe('WherePipe', () => {
    
    let pipe: WherePipe;
    
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
       pipe = new WherePipe(); 
    });
    
    it ('Should return only the 1 values', () => {
       
       const fn = function (item: any) {
           return item === 1;
       } 
       
       const value = [1, 2, 3, 1, 1, 4];
       expect(pipe.transform(value, fn)).toEqual([1, 1, 1]);
       expect(value).toEqual([1, 2, 3, 1, 1, 4]);
    });
    
    it('Should return the objects where a is 4', () => {
       
       const fn = function (item: any) {
           return item.a === 4;
       };
       
       expect(pipe.transform(array, fn))
            .toEqual([{
                a: 4,
                b: 3,
                d: { e : 8 }
            },
            {
                a: 4,
                b: 5,
                d: { e : 5}
            }
        ]); 
    });
    
    it('Should return the objects where a is 4', () => {

       expect(pipe.transform(array, ['a', 4]))
            .toEqual([{
                a: 4,
                b: 3,
                d: { e : 8 }
            },
            {
                a: 4,
                b: 5,
                d: { e : 5}
            }
        ]); 
    });
    
    it('Should return the objects where d.e is 4', () => {

       expect(pipe.transform(array, ['d.e', 4]))
            .toEqual([{
                a: 2,
                b: 3,
                d: { e : 4 }
            },
            {
                a: 3,
                b: 1,
                d: { e : 4}
            }
        ]); 
    });
    
    it('Should return the input', () => {
        expect((<any>pipe).transform([1])).toEqual([1]);
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a', null)).toEqual('a'); 
    });

    it('Should return the 2', () => {
        expect(pipe.transform([1, 2, 3, 4, 2, 2], 2)).toEqual([2, 2, 2]);
    });
    
});