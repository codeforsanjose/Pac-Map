package com.codeforsanjose.maps.pacmap.zone

import java.util.*

data class FeatureCollection(
        val type: String,
        val crs: Crs,
        val features: Array<Feature>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureCollection

        if (type != other.type) return false
        if (crs != other.crs) return false
        if (!Arrays.equals(features, other.features)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + crs.hashCode()
        result = 31 * result + Arrays.hashCode(features)
        return result
    }
}