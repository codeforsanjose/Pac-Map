package com.codeforsanjose.maps.pacmap.zone

data class Feature (
        val type: String,
        val properties: Property,
        val geometry: Geometry
)