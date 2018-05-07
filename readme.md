# Pac-Map

<p align="center">
![Pac-Map icon](https://raw.githubusercontent.com/codeforsanjose/Pac-Map/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)
<p>

There are roads in [Mapillary](https://www.mapillary.com/) that are missing crowd-sourced image data. Contributors of
Mapillary need directions to drive through these roads and quickly identify unmapped roads.

This app helps a driver navigate through unmapped roads so they can provide image data through apps like Mapillary.

![Map of San Jose showing gaps in mapped areas](https://raw.githubusercontent.com/codeforsanjose/Pac-Map/master/server/public/images/SanJose_Mapillary_2018-05-01.png)

## Project Goals and Status

**Project status:** Phase 1 in development

### Phase 1 (MVP):
- [x] display a map to the user
- [x] allow the user to select a map area from a list of areas that need to be driven
- [x] overlay a polygon on the map, highlighting the area that needs to be driven
- [ ] trace the user's position on the map, drawing a line where the user has been

### Phase 2:
- [ ] allow the user to log in to their OSM account
- [ ] allow the user to mark a map area as complete

### Phase 3:
- [ ] implement a solution to the [route inspection problem](https://en.wikipedia.org/wiki/Route_inspection_problem)
(also called Chinese postman problem) to optimize routes, minimizing the amount of time and fuel spent to cover an area
- [ ] provide turn-by-turn navigation along the provided route

### Phase 4:
- [ ] gamification?!



## Notes and Existing Art:

### Routing:
* osrm            https://github.com/Project-OSRM/osrm-backend
* graph hopper    https://www.graphhopper.com/
* valhalla        https://mapzen.com/blog/introducing-valhalla/

### Maps:
* https://www.openstreetmap.org/

### Android Libraries:
* https://github.com/osmdroid/osmdroid
* https://wiki.openstreetmap.org/wiki/Android#Maps
* https://wiki.openstreetmap.org/wiki/Mapbox_GL
* https://www.mapbox.com/android-docs/map-sdk/overview/
