package com.codeforsanjose.maps.pacmap.zone


data class Geometry(
        val type: String,
        val coordinates: Array<Array<Array<*>>>)