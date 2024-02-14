'use strict';

var YES_MATCH_SCORE_THRESHOLD = 2;
var NO_MATCH_SCORE_THRESHOLD = 1.25;

var yMatch = {
	5: 0.25,
	6: 0.25,
	7: 0.25,
	t: 0.75,
	y: 1,
	u: 0.75,
	g: 0.25,
	h: 0.25,
	k: 0.25
};

var eMatch = {
	2: 0.25,
	3: 0.25,
	4: 0.25,
	w: 0.75,
	e: 1,
	r: 0.75,
	s: 0.25,
	d: 0.25,
	f: 0.25
};

var sMatch = {
	q: 0.25,
	w: 0.25,
	e: 0.25,
	a: 0.75,
	s: 1,
	d: 0.75,
	z: 0.25,
	x: 0.25,
	c: 0.25
};

var nMatch = {
	h: 0.25,
	j: 0.25,
	k: 0.25,
	b: 0.75,
	n: 1,
	m: 0.75
};

var oMatch = {
	9: 0.25,
	0: 0.25,
	i: 0.75,
	o: 1,
	p: 0.75,
	k: 0.25,
	l: 0.25
};

function getYesMatchScore(val) {
	var score = 0;
	var y = val[0];
	var e = val[1];
	var s = val[2];

	if ({}.hasOwnProperty.call(yMatch, y)) {
		score += yMatch[y];
	}

	if ({}.hasOwnProperty.call(eMatch, e)) {
		score += eMatch[e];
	}

	if ({}.hasOwnProperty.call(sMatch, s)) {
		score += sMatch[s];
	}

	return score;
}

function getNoMatchScore(val) {
	var score = 0;
	var n = val[0];
	var o = val[1];

	if ({}.hasOwnProperty.call(nMatch, n)) {
		score += nMatch[n];
	}

	if ({}.hasOwnProperty.call(oMatch, o)) {
		score += oMatch[o];
	}

	return score;
}

module.exports = function (val, opts) {
	if (getYesMatchScore(val) >= YES_MATCH_SCORE_THRESHOLD) {
		return true;
	}

	if (getNoMatchScore(val) >= NO_MATCH_SCORE_THRESHOLD) {
		return false;
	}

	return opts.default;
};
