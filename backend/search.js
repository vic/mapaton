var axios = require('axios');
var _ = require('lodash');
var Q = require('q');
var MongoClient = require('mongodb').MongoClient;
var assert = require('assert');
var elasticsearch = require('elasticsearch');

var elastic = new elasticsearch.Client({
    host: 'localhost:9200',
    log: 'trace'
});

var url = 'mongodb://localhost:27017/trails';

function findNear(db, callback) {
    var cursor = db.collection('trails').find({
        "st_asgeojson": {
            $near: {
                $geometry: {
                    "type": "Point",
                    "coordinates": [-99.17238235473633,
                                    19.37933243547929
                                   ]
                },
                $maxDistance: 400
            }
        }
    });
    cursor.each(function(err, doc) {
        assert.equal(err, null);
        if (doc != null) {
            console.dir(doc);
        } else {
            callback();
        }
    });
}

function allPoints(db, callback) {
    db.collection('trails').find().each(function (err, doc) {
        assert.equal(err, null);
        if (!doc) {
            return callback();
        }

        var points = _.map(_.flatten(doc.st_asgeojson.coordinates), function(point) {
            var lat = _.flatten([point])[1];
            var lon = _.flatten([point])[0];
            assert.notEqual(lat, null);
            assert.notEqual(lon, null);
            var p = {location: {lat: lat, lon: lon}}
            //console.log(p)
            return p;
        });

        var trail = {
            routeId: doc.route_id,
            routeShortName: doc.route_short_name,
            routeLongName: doc.route_long_name,
            points: points
        }


        elastic.create({
            index: 'trails',
            type: 'trail',
            body: trail
        }, function (err, response) {
            //assert.equal(err, null);
        })

    });
}


MongoClient.connect(url, function(err, db) {
    assert.equal(null, err);
    console.log("Connected correctly to server.");

    allPoints(db, function() {
        db.close();
    });

});
