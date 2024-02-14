import { BytesPipe } from './bytes.pipe';
        


describe('BytesPipe', () => {
    
    let pipe: BytesPipe;
    
    beforeEach(() => {
       pipe = new BytesPipe(); 
    });
    
    
    it('Should return 150 B', () => {
       
        const result = pipe.transform(150, 0);
        expect(result).toEqual('150 B');
    });
    
    
    it('Should return 155.57 B', () => {
       
        const result = pipe.transform(155.56791, 2);
        expect(result).toEqual('155.57 B');
    });
    
    it('Should return 155.5 B', () => {
       
        const result = pipe.transform(155.5, 1);
        expect(result).toEqual('155.5 B');
    });
    
    
    it('Should return 1 KB', () => {
       
        const result = pipe.transform(1024, 0);
        expect(result).toEqual('1 KB');
    });

    it('Should return 1 KB #2', () => {
       
        const result = pipe.transform(1, 0, 'KB');
        expect(result).toEqual('1 KB');
    });
    
    it('Should return 890 KB', () => {
       
        const kb = 1024 * 890;
        const result = pipe.transform(kb, 0);
        expect(result).toEqual('890 KB');
    });

    it('Should return 890 KB #2', () => {
       
        const result = pipe.transform(890, 0, 'KB');
        expect(result).toEqual('890 KB');
    });
    
    
    it('Should return 1023 KB', () => {
       
        const kb = 1024 * 1023;
        const result = pipe.transform(kb, 0);
        expect(result).toEqual('1023 KB');
    });
    
    it('Should return 241 MB', () => {
       
        const mb = 1024 * 1024 * 240.5691;
        const result = pipe.transform(mb, 0);
        expect(result).toEqual('241 MB');
    });

     it('Should return 241 MB', () => {
       
        const mb = 240.5691 / 1024;
        const result = pipe.transform(mb, 0, 'GB');
        expect(result).toEqual('241 MB');
    });
    
    it('Should return 240.54 MB', () => {
       
        const mb = 1024 * 1024 * 240.5411;
        const result = pipe.transform(mb, 2);
        expect(result).toEqual('240.54 MB');
    });
    
    it('Should return 1023 MB', () => {
       
        const mb = 1024 * 1024 * 1023;
        const result = pipe.transform(mb, 2);
        expect(result).toEqual('1023 MB');
    });

     it('Should return 1023 MB #2', () => {
       
        const kb = 1024 * 1023;
        const result = pipe.transform(kb, 2, 'KB');
        expect(result).toEqual('1023 MB');
    });
    
    it('Should return 1023 GB', () => {
       
        const gb = 1024 * 1024 * 1024 * 1023;
        const result = pipe.transform(gb, 2);
        expect(result).toEqual('1023 GB');
    });

      it('Should return 1.03 TB', () => {
       
        const gb = 1024 * 1024 * 1024 * 1059;
        const result = pipe.transform(gb, 2);
        expect(result).toEqual('1.03 TB');
    });


    it('Should return the input', () => {
        expect(pipe.transform('a')).toEqual('a');
    });
    
});