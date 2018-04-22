let express = require('express');
let jsonfile = require('jsonfile');
let router = express.Router();
let data = jsonfile.readFileSync(__dirname + '/../data/working_grouped_tazs.geojson');

// GET specific region
router.get('/:region', function(req, res, next) {
	if (data.features.hasOwnProperty(req.params.region)) {
		res.json(data.features[req.params.region].geometry.coordinates);
	} else {
		res.json({});
	}
});

// GET general area
router.get('/', function(req, res, next) {
	res.json(data);
});

module.exports = router;
