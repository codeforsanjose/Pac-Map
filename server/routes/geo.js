let express = require('express');
let jsonfile = require('jsonfile');
let router = express.Router();
let data = jsonfile.readFileSync('data/working_grouped_tazs.geojson');

/* GET users listing. */
router.get('/:region', function(req, res, next) {
	res.json(data);
});

module.exports = router;
