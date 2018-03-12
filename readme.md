# Pac-Map

The project goal is to give drivers an easy way to see which roads have been driven.

Minimum Viable Product Features:
--------------------------------

* display a map to the user
* overlay a polygon on the map, highlighting the area that needs to be driven
* trace the user's position on the map, drawing a line where the user has been


Stretch goals and future features:
----------------------------------

* implement a solution to the [route inspection problem](https://en.wikipedia.org/wiki/Route_inspection_problem)
(also called Chinese postman problem) to optimize routes, minimizing the amount of time and fuel spent to cover an area
* provide turn-by-turn navigation along the provided route



## Notes and Existing Art:

Routing:
--------
osrm            https://github.com/Project-OSRM/osrm-backend
graph hopper    https://www.graphhopper.com/
valhalla        https://mapzen.com/blog/introducing-valhalla/



Maps:
-----
https://www.openstreetmap.org/


Android Libraries:
------------------
https://github.com/osmdroid/osmdroid
https://wiki.openstreetmap.org/wiki/Android#Maps
https://wiki.openstreetmap.org/wiki/Mapbox_GL
https://www.mapbox.com/android-docs/map-sdk/overview/
