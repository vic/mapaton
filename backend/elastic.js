var fs = require('fs');
var elastic = require('elasticsearch');
var _ = require('lodash');

var routes = JSON.parse(fs.readFileSync(process.argv[0]));
