/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015, Grumlimited Ltd (Romain Gallet)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of Geocalc nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.grum.geocalc;

import static java.lang.Math.*;

/**
 * Earth related calculations.
 *
 * @author rgallet
 */
public class EarthCalc {

    public static final double EARTH_DIAMETER = 6371.01 * 1000; //meters

    /**
     * Returns the distance between two points using spherical law of cosines.
     *
     * @param standPoint The stand point
     * @param forePoint  The fore point
     * @return The distance, in meters
     */
    public static double getDistance(Point standPoint, Point forePoint) {

        double diffLongitudes = toRadians(abs(forePoint.longitude - standPoint.longitude));
        double slat = toRadians(standPoint.latitude);
        double flat = toRadians(forePoint.latitude);

        //spherical law of cosines
        double c = acos((sin(slat) * sin(flat)) + (cos(slat) * cos(flat) * cos(diffLongitudes)));

        return EARTH_DIAMETER * c;
    }

    /**
     * Returns the distance between two points using Harvesine formula.
     *
     * @param standPoint The stand point
     * @param forePoint  The fore point
     * @return The distance, in meters
     */
    public static double getHarvesineDistance(Point standPoint, Point forePoint) {

        double diffLongitudes = toRadians(abs(forePoint.longitude - standPoint.longitude));
        double slat = toRadians(standPoint.latitude);
        double flat = toRadians(forePoint.latitude);

        // haversine formula
        double diffLatitudes = toRadians(abs(forePoint.latitude - standPoint.latitude));
        double a = sin(diffLatitudes / 2) * sin(diffLatitudes / 2) + cos(slat) * cos(flat) * sin(diffLongitudes / 2) * sin(diffLongitudes / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a)); //angular distance in radians

        return EARTH_DIAMETER * c;
    }

    /**
     * Calculate distance, (azimuth) bearing and final bearing between standPoint and forePoint.
     *
     * @param standPoint The stand point
     * @param forePoint  The fore point
     * @return Vincenty object which holds all 3 values
     */
    private static Vincenty getVincenty(Point standPoint, Point forePoint) {
        double ??1 = toRadians(standPoint.longitude);
        double ??2 = toRadians(forePoint.longitude);

        double ??1 = toRadians(standPoint.latitude);
        double ??2 = toRadians(forePoint.latitude);

        double a = 6_378_137;
        double b = 6_356_752.314245;
        double f = 1 / 298.257223563;

        double L = ??2 - ??1;
        double tanU1 = (1 - f) * tan(??1), cosU1 = 1 / sqrt((1 + tanU1 * tanU1)), sinU1 = tanU1 * cosU1;
        double tanU2 = (1 - f) * tan(??2), cosU2 = 1 / sqrt((1 + tanU2 * tanU2)), sinU2 = tanU2 * cosU2;

        double ?? = L, ????, iterationLimit = 100, cosSq??, ??, cos2??M, cos??, sin??, sin??, cos??;
        do {
            sin?? = sin(??);
            cos?? = cos(??);
            double sinSq?? = (cosU2 * sin??) * (cosU2 * sin??) + (cosU1 * sinU2 - sinU1 * cosU2 * cos??) * (cosU1 * sinU2 - sinU1 * cosU2 * cos??);
            sin?? = sqrt(sinSq??);
            if (sin?? == 0) return new Vincenty(0, 0, 0);  // co-incident points
            cos?? = sinU1 * sinU2 + cosU1 * cosU2 * cos??;
            ?? = atan2(sin??, cos??);
            double sin?? = cosU1 * cosU2 * sin?? / sin??;
            cosSq?? = 1 - sin?? * sin??;
            cos2??M = cos?? - 2 * sinU1 * sinU2 / cosSq??;

            if (Double.isNaN(cos2??M)) cos2??M = 0;  // equatorial line: cosSq??=0 (??6)
            double C = f / 16 * cosSq?? * (4 + f * (4 - 3 * cosSq??));
            ???? = ??;
            ?? = L + (1 - C) * f * sin?? * (?? + C * sin?? * (cos2??M + C * cos?? * (-1 + 2 * cos2??M * cos2??M)));
        } while (abs(?? - ????) > 1e-12 && --iterationLimit > 0);

        if (iterationLimit == 0) throw new IllegalStateException("Formula failed to converge");

        double uSq = cosSq?? * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double ???? = B * sin?? * (cos2??M + B / 4 * (cos?? * (-1 + 2 * cos2??M * cos2??M) -
                B / 6 * cos2??M * (-3 + 4 * sin?? * sin??) * (-3 + 4 * cos2??M * cos2??M)));

        double distance = b * A * (?? - ????);

        double initialBearing = atan2(cosU2 * sin??, cosU1 * sinU2 - sinU1 * cosU2 * cos??);
        initialBearing = (initialBearing + 2 * PI) % (2 * PI); //turning value to trigonometric direction

        double finalBearing = atan2(cosU1 * sin??, -sinU1 * cosU2 + cosU1 * sinU2 * cos??);
        finalBearing = (finalBearing + 2 * PI) % (2 * PI);  //turning value to trigonometric direction

        return new Vincenty(distance, toDegrees(initialBearing), toDegrees(finalBearing));

    }

    public static double getVincentyDistance(Point standPoint, Point forePoint) {
        return getVincenty(standPoint, forePoint).distance;
    }

    /**
     * Returns (azimuth) bearing using Vincenty formula.
     *
     * {@link http://www.movable-type.co.uk/scripts/latlong.html}
     *
     * @param standPoint The stand point
     * @param forePoint  The fore point
     * @return (azimuth) bearing in degrees to the North
     */
    public static double getVincentyBearing(Point standPoint, Point forePoint) {
        return getVincenty(standPoint, forePoint).initialBearing;
    }

    /**
     * Returns final bearing in direction of standPoint???forePoint using Vincenty formula.
     *
     * {@link http://www.movable-type.co.uk/scripts/latlong.html}
     *
     * @param standPoint The stand point
     * @param forePoint  The fore point
     * @return (azimuth) bearing in degrees to the North
     */
    public static double getVincentyFinalBearing(Point standPoint, Point forePoint) {
        return getVincenty(standPoint, forePoint).finalBearing;
    }

    /**
     * Returns the coordinates of a point which is "distance" away
     * from standPoint in the direction of "bearing"
     * <p>
     * Note: North is equal to 0 for bearing value
     *
     * {@link http://www.movable-type.co.uk/scripts/latlong.html}
     *
     * @param standPoint Origin
     * @param bearing    Direction in degrees
     * @param distance   distance in meters
     * @return forePoint coordinates
     */
    public static Point pointRadialDistance(Point standPoint, double bearing, double distance) {
        /**
         var ??2 = asin( sin(??1)*cos(d/R) + cos(??1)*sin(d/R)*cos(brng) );
         var ??2 = ??1 + atan2(sin(brng)*sin(d/R)*cos(??1), cos(d/R)-sin(??1)*sin(??2));
         */

        double rlat1 = toRadians(standPoint.latitude);
        double rlon1 = toRadians(standPoint.longitude);
        double rbearing = toRadians(bearing);
        double rdistance = distance / EARTH_DIAMETER; // normalize linear distance to radian angle

        double lat2 = asin(sin(rlat1) * cos(rdistance) + cos(rlat1) * sin(rdistance) * cos(rbearing));
        double lon2 = rlon1 + atan2(sin(rbearing) * sin(rdistance) * cos(rlat1), cos(rdistance) - sin(rlat1) * sin(lat2));

        return new Point(new RadianCoordinate(lat2), new RadianCoordinate(lon2));
    }

    /**
     * Returns the (azimuth) bearing, in decimal degrees, from standPoint to forePoint
     *
     * @param standPoint Origin point
     * @param forePoint  Destination point
     * @return (azimuth) bearing, in decimal degrees
     */
    public static double getBearing(Point standPoint, Point forePoint) {
        /**
         * Formula: ?? = atan2( 	sin(??long).cos(lat2), cos(lat1).sin(lat2) ??? sin(lat1).cos(lat2).cos(??long) )
         */

        double y = sin(toRadians(forePoint.longitude - standPoint.longitude)) * cos(toRadians(forePoint.latitude));
        double x = cos(toRadians(standPoint.latitude)) * sin(toRadians(forePoint.latitude))
                - sin(toRadians(standPoint.latitude)) * cos(toRadians(forePoint.latitude)) * cos(toRadians(forePoint.longitude - standPoint.longitude));

        double bearing = (atan2(y, x) + 2 * PI) % (2 * PI);

        return toDegrees(bearing);
    }

    /**
     * Returns an area around standPoint
     * 
     * {@link http://www.movable-type.co.uk/scripts/latlong.html}
     *
     * @param standPoint The centre of the area
     * @param distance   Distance around standPoint, im meters
     * @return The area
     */
    public static BoundingArea getBoundingArea(Point standPoint, double distance) {

        //45 degrees going north-west
        Point northWest = pointRadialDistance(standPoint, 45, distance);

        //225 degrees going south-east
        Point southEast = pointRadialDistance(standPoint, 225, distance);

        return new BoundingArea(northWest, southEast);
    }

    private static class Vincenty {
        /**
         * distance is the distance in meter
         * initialBearing is the initial bearing, or forward azimuth (in reference to North point), in degrees
         * finalBearing is the final bearing (in direction p1???p2), in degrees
         */
        double distance, initialBearing, finalBearing;

        public Vincenty(double distance, double initialBearing, double finalBearing) {
            this.distance = distance;
            this.initialBearing = initialBearing;
            this.finalBearing = finalBearing;
        }
    }
}
