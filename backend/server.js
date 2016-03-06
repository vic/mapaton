var axios = require('axios');
var _ = require('lodash');
var Q = require('q');
var assert = require('assert');

var elasticsearch = require('elasticsearch');

var elastic = new elasticsearch.Client({
    host: 'localhost:9200',
    log: 'error'
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
    var fromLocation = req.body.fromLocation;
    var toLocation = req.body.toLocation;
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
                                    origin: fromLocation,
                                    offset: '200m',
                                    scale: '1km'
                                }
                            },
                            weight: 2
                        },
                        {
                            gauss: {
                                'points.location': {
                                    origin: toLocation,
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
        var results = _.map(ok.hits.hits, function (hit) {
            var route = hit._source;
            var result = {};

            result.routeShortName = route.routeShortName;
            result.routeLongName = route.routeLongName;

            var path = _.map(route.points, function (point) {
                return [point.location.lat, point.location.lon].join(',');
            }).join('|');
            path = "color:0x0000ff|weight:5|" + path;

            var locations = [ [fromLocation.lat, fromLocation.lon].join(','),
                  [toLocation.lat, toLocation.lon].join(',') ].join('|');

            var markers = 'color:red|' + locations;

            var url = "https://maps.googleapis.com/maps/api/staticmap?";

            url = url + ["format=png", "maptype=roadmap", "language=es-MX", "size=400x400",
                         "region=mx", "key=AIzaSyCZv54cKAdO_yYZ8V05MxHVm51NAtDkX6g",
                         "center="+[toLocation.lat, toLocation.lon].join(','),
                         "path="+path, "markers="+markers, "visible="+locations].join('&')

            result.imageURL = url;

            console.log(result);

            return result;
        });
        res.send(JSON.stringify(results));
    }, function (err) {
        res.send(JSON.stringify(err));
    })
});

app.listen(3000, function () {
    console.log('Example app listening on port 3000!');
});
