import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

LocalTime calculateSunrise(int year=new Date().getYear() + 1900, int month=new Date().getMonth() + 1, int day=new Date().getDate()) {
    final double ZENITH = 90.83333 // Official zenith for sunrise/sunset
    LocalDate date = LocalDate.of(year, month, day)
    double latitude = location.latitude
    double longitude = location.longitude
    int dayOfYear = date.dayOfYear
    
    double lngHour = longitude / 15
    double t = dayOfYear + ((6 - lngHour) / 24)

    // Mean anomaly
    double M = (0.9856 * t) - 3.289
    
    // Sun's true longitude
    double L = (M + (1.916 * Math.sin(Math.toRadians(M))) + (0.020 * Math.sin(Math.toRadians(2 * M))) + 282.634) % 360

    // Right ascension
    double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))))
    RA = RA % 360

    // Adjust RA to be in the same quadrant as L
    double Lquadrant = (Math.floor(L / 90)) * 90
    double RAquadrant = (Math.floor(RA / 90)) * 90
    RA = RA + (Lquadrant - RAquadrant)

    // Convert RA into hours
    RA = RA / 15

    // Calculate declination of the sun
    double sinDec = 0.39782 * Math.sin(Math.toRadians(L))
    double cosDec = Math.cos(Math.asin(sinDec))

    // Calculate the sun's local hour angle
    double cosH = (Math.cos(Math.toRadians(ZENITH)) - (sinDec * Math.sin(Math.toRadians(latitude)))) / (cosDec * Math.cos(Math.toRadians(latitude)))

    if (cosH > 1) {
        return -1 //no sunrise at this location for this date 
    }

    // Calculate H and convert into hours
    double H = 360 - Math.toDegrees(Math.acos(cosH))
    H = H / 15

    // Calculate local mean time
    double T = H + RA - (0.06571 * t) - 6.622

    // Adjust time back to UTC
    double UT = (T - lngHour) % 24
    if (UT < 0) UT += 24

    // Convert UT to Local Time Zone
    ZoneId zone = ZoneId.systemDefault()
    ZonedDateTime utcTime = date.atTime((int) UT, (int) ((UT % 1) * 60)).atZone(ZoneId.of("UTC"))
    ZonedDateTime localTime = utcTime.withZoneSameInstant(zone)

    // Return the local sunrise time
    return localTime.toLocalTime()
}
    
LocalTime calculateSunset(int year=new Date().getYear() + 1900, int month=new Date().getMonth() + 1, int day=new Date().getDate()) {
    final double ZENITH = 90.83333 // Official zenith for sunrise/sunset
    LocalDate date=LocalDate.of(year,month,day)
    int dayOfYear = date.dayOfYear
    double latitude = location.latitude
    double longitude = location.longitude 

    // Approximate time in hours
    double lngHour = longitude / 15
    double t = dayOfYear + ((18 - lngHour) / 24)

    // Sun's mean anomaly
    double M = (0.9856 * t) - 3.289

    // Sun's true longitude
    double L = M + (1.916 * Math.sin(Math.toRadians(M))) + (0.020 * Math.sin(2 * Math.toRadians(M))) + 282.634
    L = (L + 360) % 360

    // Sun's right ascension
    double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))))
    RA = (RA + 360) % 360

    // Right ascension value needs to be in the same quadrant as L
    double Lquadrant = (Math.floor(L / 90)) * 90
    double RAquadrant = (Math.floor(RA / 90)) * 90
    RA = RA + (Lquadrant - RAquadrant)

    // Convert RA into hours
    RA = RA / 15

    // Sun's declination
    double sinDec = 0.39782 * Math.sin(Math.toRadians(L))
    double cosDec = Math.cos(Math.asin(sinDec))

    // Sun's local hour angle
    double cosH = (Math.cos(Math.toRadians(ZENITH)) - (sinDec * Math.sin(Math.toRadians(latitude)))) / (cosDec * Math.cos(Math.toRadians(latitude)))
    if (cosH > 1) {
        return -1  // Sun never sets
    } else if (cosH < -1) {
        return -1  // Sun never rises
    }
    
    // H = local hour angle in degrees
    double H = Math.toDegrees(Math.acos(cosH)) / 15

    // Local mean time of sunset
    double T = H + RA - (0.06571 * t) - 6.622

    // Adjust back to UTC
    double UT = (T - lngHour) % 24
    if (UT < 0) UT += 24

    // Convert UT to Local Time Zone
    ZoneId zone = ZoneId.systemDefault()
    ZonedDateTime utcTime = date.atTime((int) UT, (int) ((UT % 1) * 60)).atZone(ZoneId.of("UTC"))
    ZonedDateTime localTime = utcTime.withZoneSameInstant(zone)

    // Return the local sunset time
    return localTime.toLocalTime()
}
