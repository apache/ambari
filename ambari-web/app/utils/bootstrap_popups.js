(function() {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.text=`setTimeout(()=>{const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
    console.log("List is",tooltipTriggerList)
    const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))},3000)`
    document.getElementsByTagName('head')[0].appendChild(script);
    })();
    
    (function() {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.text = `setTimeout(()=>{const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]')
    const popoverList = [...popoverTriggerList].map(popoverTriggerEl => new bootstrap.Popover(popoverTriggerEl))},3000)`;
    document.getElementsByTagName('head')[0].appendChild(script);
    })();