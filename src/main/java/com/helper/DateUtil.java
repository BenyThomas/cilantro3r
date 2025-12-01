/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author samichael
 */
@Component
public class DateUtil {

    public static String formatDate(String date, String inDateFormat, String outDateFormat) throws ParseException {
        Date initDate = new SimpleDateFormat(inDateFormat).parse(date);
        SimpleDateFormat formatter = new SimpleDateFormat(outDateFormat);
        String parsedDate = formatter.format(initDate);
        return parsedDate;
    }

    public static String format_short_month_date(String deliverydate) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("dd-MMM-yy")
                .toFormatter(Locale.UK);
        LocalDate ld = LocalDate.parse(deliverydate, formatter);
        return ld.toString();
    }
    public static String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    public static String now(String format) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        String formatDateTime = now.format(formatter);
        return formatDateTime;
    }

    public static String now() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formatDateTime = now.format(formatter);
        return formatDateTime;
    }

    public static String nowLong() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formatDateTime = now.format(formatter);
        return formatDateTime;
    }

    public static String yesterday() {
//        LocalDateTime now = LocalDateTime.now();
        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    public static String yesterdayDefault() {
//        LocalDateTime now = LocalDateTime.now();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    public static String todayRubikonFormat() {
//        LocalDateTime now = LocalDateTime.now();
        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    public static String getMondayDatePreviouwWeek() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        c.add(Calendar.DAY_OF_WEEK, -7);
        return dateFormat.format(c.getTime());
    }

    public static String getMondaySameWeek() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return dateFormat.format(c.getTime());
    }

    public static String datetimeToStr(LocalDateTime datetime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        String formatDateTime = datetime.format(formatter);
        return formatDateTime;
    }

    public static String datetimeToStr(LocalDateTime datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formatDateTime = datetime.format(formatter);
        return formatDateTime;
    }

    public static String datetimeToStr(Date datetime, String format) {
        return new SimpleDateFormat(format).format(datetime);
    }

    public static String datetimeToStr(Date datetime) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(datetime);
    }

    public static Date strToDate(String str) throws ParseException {
        Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(str);
        return date1;
    }

    public static Date strToDate(String str, String format) throws ParseException {
        Date date1 = new SimpleDateFormat(format).parse(str);
        return date1;
    }

    public static String tomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return tomorrow.toString();

    }

    public static String previosDay(int day) {
        LocalDate previosday = LocalDate.now().minusDays(day);
        return previosday.toString();
    }

    public static String previosDay(int day, String date, String incomingFormat) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(incomingFormat);
        //convert String to LocalDate
        LocalDate localDate = LocalDate.parse(date, formatter);
        LocalDate previosday = localDate.minusDays(day);
        return previosday.toString();
    }

    public static String previosDay(int day, String date) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDate previosday = localDate.minusDays(day);
        return previosday.toString();
    }

    public static boolean isBefore(String firstdate, String seconddate) {
        boolean notBefore = LocalDate.parse(firstdate).isBefore(LocalDate.parse(seconddate));
        return notBefore;
    }

    public static boolean isAfter(String firstdate, String seconddate) {
        boolean isAfter = LocalDate.parse(firstdate).isAfter(LocalDate.parse(seconddate));
        return isAfter;
    }

    public static boolean isExpired(String now, String expire) {
        Timestamp tsNow = Timestamp.valueOf(now);
        Timestamp tsExpire = Timestamp.valueOf(expire);
        boolean isAfter;
        //compares tsNow with tsExpire
        int b3 = tsNow.compareTo(tsExpire);
        //tsNow value is greater"
        //tsExpire value is greater
        if (b3 == 0) {
            //Both values are equal
            isAfter = false;
        } else {
            isAfter = b3 > 0;
        }
        return isAfter;
    }

    public static Long noOfDaysBetween(String dateBeforeString, String dateAfterString) {
        //String dateBeforeString = "2017-05-24";
        //String dateAfterString = "2017-07-29";
        //Parsing the date
        LocalDate dateBefore = LocalDate.parse(dateBeforeString);
        LocalDate dateAfter = LocalDate.parse(dateAfterString);
        //calculating number of days in between
        long noOfDaysBetween = ChronoUnit.DAYS.between(dateBefore, dateAfter);
        //displaying the number of days
        return noOfDaysBetween;
    }

    public static Date getAddDate(int addType, int lengh) {
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(addType, lengh);
        Date expirationDate = c.getTime();
        return expirationDate;
    }

    public void ramadhanDate() {
        //first day of Ramadan, 9th month
        HijrahDate ramadan = HijrahDate.now()
                .with(ChronoField.DAY_OF_MONTH, 1).with(ChronoField.MONTH_OF_YEAR, 9);
        //HijrahDate -> LocalDate
        System.out.println("\n--- Ramandan This Year ---");
        System.out.println("Start : " + LocalDate.from(ramadan));
        //until the end of the month
        System.out.println("End : " + LocalDate.from(ramadan.with(TemporalAdjusters.lastDayOfMonth())));
    }

    public static XMLGregorianCalendar dateToGregorianCalendar(String date, String format)
            throws DatatypeConfigurationException, ParseException {
        String dateTimeString = DateUtil.formatDate(date, format, "yyyy-MM-dd'T'HH:mm:ss");
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(dateTimeString);
    }

    public static XMLGregorianCalendar nowDateXMLGregorianCalendar() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formatDateTime = now.format(formatter);
            String dateTimeString = DateUtil.formatDate(formatDateTime, "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss");
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(dateTimeString);
        } catch (Exception ex) {
            Logger.getLogger(DateUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Date toDate(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.toGregorianCalendar().getTime();
    }

    public static XMLGregorianCalendar getXMLGregorianCalendarNow() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        return now;
    }
    public static XMLGregorianCalendar getXMLGregorianCalendarAhead(int daysAhead) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        // Add days to the GregorianCalendar
        gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, daysAhead);

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        XMLGregorianCalendar futureDate = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        return futureDate;
    }
    public static XMLGregorianCalendar getXMLGregorianCalendarNextDay(int daysAhead,int hour, int minute, int second) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        // Move the date to the next day
        gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, daysAhead);

        // Set specific time on the next day
        gregorianCalendar.set(GregorianCalendar.HOUR_OF_DAY, hour);
        gregorianCalendar.set(GregorianCalendar.MINUTE, minute);
        gregorianCalendar.set(GregorianCalendar.SECOND, second);
        gregorianCalendar.set(GregorianCalendar.MILLISECOND, 0); // Optional: set milliseconds to 0

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        XMLGregorianCalendar nextDay = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        return nextDay;
    }
    public static String convertXmlGregorianToString(XMLGregorianCalendar xc, String dateFormat) {
        DateFormat df = new SimpleDateFormat(dateFormat);
        GregorianCalendar gCalendar = xc.toGregorianCalendar();
        //Converted to date object
        Date date = gCalendar.getTime();
        //Formatted to String value
        String dateString = df.format(date);
        return dateString;
    }

    public static XMLGregorianCalendar convertDateToXmlGregorian(String txnDate, String dateFormat) {
        try {
            DatatypeFactory datatypeFactory = null;
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            datatypeFactory = DatatypeFactory.newInstance();
            Date date = format.parse(txnDate);
            GregorianCalendar cal2 = new GregorianCalendar();
            cal2.setTime(date);
            return datatypeFactory.newXMLGregorianCalendar(cal2);
        } catch (Exception ex) {
            Logger.getLogger(DateUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String getDifferenceBtwTime(Date startDateTime,Date endDateTime) {

        long timeDifferenceMilliseconds = endDateTime.getTime() - startDateTime.getTime();
        long diffSeconds = timeDifferenceMilliseconds / 1000;
        long diffMinutes = timeDifferenceMilliseconds / (60 * 1000);
        long diffHours = timeDifferenceMilliseconds / (60 * 60 * 1000);
        long diffDays = timeDifferenceMilliseconds / (60 * 60 * 1000 * 24);
        long diffWeeks = timeDifferenceMilliseconds / (60 * 60 * 1000 * 24 * 7);
        long diffMonths = (long) (timeDifferenceMilliseconds / (60 * 60 * 1000 * 24 * 30.41666666));
        long diffYears = (long) (timeDifferenceMilliseconds / (1000 * 60 * 60 * 24 * 365));

        if (diffSeconds < 1) {
            return "one sec";
        } else if (diffMinutes < 1) {
            return diffSeconds + " seconds";
        } else if (diffHours < 1) {
            return diffMinutes + " minutes";
        } else if (diffDays < 1) {
            return diffHours + " hours";
        } else if (diffWeeks < 1) {
            return diffDays + " days";
        } else if (diffMonths < 1) {
            return diffWeeks + " weeks";
        } else if (diffYears < 12) {
            return diffMonths + " months";
        } else {
            return diffYears + " years";
        }
    }
    public static String getDifferenceBtwTime(Date dateTime) {

        long timeDifferenceMilliseconds = new Date().getTime() - dateTime.getTime();
        long diffSeconds = timeDifferenceMilliseconds / 1000;
        long diffMinutes = timeDifferenceMilliseconds / (60 * 1000);
        long diffHours = timeDifferenceMilliseconds / (60 * 60 * 1000);
        long diffDays = timeDifferenceMilliseconds / (60 * 60 * 1000 * 24);
        long diffWeeks = timeDifferenceMilliseconds / (60 * 60 * 1000 * 24 * 7);
        long diffMonths = (long) (timeDifferenceMilliseconds / (60 * 60 * 1000 * 24 * 30.41666666));
        long diffYears = (long) (timeDifferenceMilliseconds / (1000 * 60 * 60 * 24 * 365));

        if (diffSeconds < 1) {
            return "one sec";
        } else if (diffMinutes < 1) {
            return diffSeconds + " seconds";
        } else if (diffHours < 1) {
            return diffMinutes + " minutes";
        } else if (diffDays < 1) {
            return diffHours + " hours";
        } else if (diffWeeks < 1) {
            return diffDays + " days";
        } else if (diffMonths < 1) {
            return diffWeeks + " weeks";
        } else if (diffYears < 12) {
            return diffMonths + " months";
        } else {
            return diffYears + " years";
        }
    }

  public static String getDifferenceBtwTime1(Date dateTime) {

        long timeDifferenceMilliseconds = new Date().getTime() - dateTime.getTime();
        long diffSeconds = timeDifferenceMilliseconds / 1000;
        long diffMinutes = timeDifferenceMilliseconds / (60 * 1000);
        long diffHours = timeDifferenceMilliseconds / (60 * 60 * 1000);
        long diffDays = timeDifferenceMilliseconds / (60 * 60 * 1000 * 24);
        long diffWeeks = timeDifferenceMilliseconds / (60 * 60 * 1000 * 24 * 7);
        long diffMonths = (long) (timeDifferenceMilliseconds / (60 * 60 * 1000 * 24 * 30.41666666));
        long diffYears = (long) (timeDifferenceMilliseconds / (1000 * 60 * 60 * 24 * 365));

        if (diffSeconds < 1) {
            return "one sec ago";
        } else if (diffMinutes < 1) {
            return diffSeconds + " seconds ago";
        } else if (diffHours < 1) {
            return diffMinutes + " minutes ago";
        } else if (diffDays < 1) {
            return diffHours + " hours ago";
        } else if (diffWeeks < 1) {
            return diffDays + " days ago";
        } else if (diffMonths < 1) {
            return diffWeeks + " weeks ago";
        } else if (diffYears < 12) {
            return diffMonths + " months ago";
        } else {
            return diffYears + " years ago";
        }
    }


    public static String convertExcelDateToDateTime(double excelDate) {
        // Excel's epoch starts on 1900-01-01
        LocalDateTime dateTime = LocalDateTime.of(1899, 12, 30, 0, 0)
                .plusDays((long) excelDate)
                .plusSeconds((long) ((excelDate % 1) * 86400));

        // Define the output format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return dateTime.format(formatter);
    }
}
