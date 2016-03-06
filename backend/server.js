var axios = require('axios');
var _ = require('lodash');
var Q = require('q');
var assert = require('assert');

var elasticsearch = require('elasticsearch');

var elastic = new elasticsearch.Client({
    host: 'localhost:9200',
    log: 'trace'
});

var express = require('express');
var app = express();

var bodyParser = require('body-parser')
app.use( bodyParser.json() );       // to support JSON-encoded bodies
app.use(bodyParser.urlencoded({     // to support URL-encoded bodies
    extended: true
}));

app.get('/', function (req, res) {
    res.send('Hello World!');
});

/* fromLocation, toLocation */
app.post('/routesNearTrip', function (req, res) {
    elastic.search({
        index: 'trails',
        type: 'trail',
        body: {
            query: {
                function_score: {
                    functions: [
                        {
                            gauss: {
                                'points.location': {
                                    origin: req.body.fromLocation,
                                    offset: '200m',
                                    scale: '1km'
                                }
                            },
                            weight: 2
                        },
                        {
                            gauss: {
                                'points.location': {
                                    origin: req.body.toLocation,
                                    offset: '200m',
                                    scale: '1km'
                                }
                            },
                            weight: 2
                        }
                    ]
                }
            }
        }
    }).then(function (ok) {
        res.send(JSON.stringify(ok));
    }, function (err) {
        res.send(JSON.stringify(err));
    })
});

app.listen(3000, function () {
    console.log('Example app listening on port 3000!');
});
