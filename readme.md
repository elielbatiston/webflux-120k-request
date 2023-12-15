let objs = [];

for (var i = 0; i < 200000; i++) {    
var customer = new Object();
customer.userId = crypto.randomuserId();
objs.push(customer);
}

var json = JSON.stringify(objs);

console.log(json);
