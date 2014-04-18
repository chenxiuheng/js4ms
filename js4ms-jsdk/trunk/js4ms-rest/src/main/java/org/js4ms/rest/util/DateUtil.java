package org.js4ms.rest.util;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class DateUtil {

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1123 format.
     */
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1036 format.
     */
    public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

    /**
     * Date format pattern used to parse HTTP date headers in ANSI C
     * <code>asctime()</code> format.
     */
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    public static final ThreadLocalDateFormat DATE_FORMAT_RFC_1123;
    public static final ThreadLocalDateFormat DATE_FORMAT_RFC_1036;
    public static final ThreadLocalDateFormat DATE_FORMAT_ASCTIME;

    static {
        SimpleDateFormat format;
        format = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        DATE_FORMAT_RFC_1123 = new ThreadLocalDateFormat(format);

        format = new SimpleDateFormat(PATTERN_RFC1036, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        DATE_FORMAT_RFC_1036 = new ThreadLocalDateFormat(format);

        format = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        DATE_FORMAT_ASCTIME = new ThreadLocalDateFormat(format);
    }

    public static String toString(final Date date) {
        return DateUtil.DATE_FORMAT_RFC_1123.format(date);
    }

    public static Date toDate(final String dateString) throws ParseException {
        Date date;
        try {
            date = DATE_FORMAT_RFC_1123.parse(dateString);
        }
        catch (ParseException e) {
            try {
                date = DATE_FORMAT_RFC_1036.parse(dateString);
            }
            catch (ParseException e1) { 
                date = DATE_FORMAT_ASCTIME.parse(dateString);
            }
        }
        return date;
    }

    public static class ThreadLocalDateFormat extends ThreadLocal<SoftReference<DateFormat>> {

        DateFormat format;
        public ThreadLocalDateFormat(final DateFormat format) {
            this.format = format;
        }

        private SoftReference<DateFormat> createValue() {  
            return new SoftReference<DateFormat>((DateFormat)this.format.clone());  
        }  

        public String format(final Date date) {
            return get().get().format(date);
        }

        public Date parse(final String date) throws ParseException {
            return get().get().parse(date);
        }

        @Override
        public SoftReference<DateFormat> get() {
            SoftReference<DateFormat> value = super.get();
            if (value == null || value.get() == null) {
                value = createValue();
                set(value);
            }
            return value;
        }
        
    }

}
