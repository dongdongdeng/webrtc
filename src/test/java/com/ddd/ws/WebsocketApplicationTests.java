package com.ddd.ws;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SpringBootTest
class WebsocketApplicationTests {
	private final String DATE_TO_CN_PATTERN = "yyyy年MM月dd日";

	public static final String FORMAT_FULL_TIME_NO_ZONE = "yyyy-MM-dd HH:mm:ss";

	public static Date toFormatDate(String dateTimeStr) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_FULL_TIME_NO_ZONE);
		DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
		return dateTime.toDate();
	}

	/**
	 * 日期格式转换yyyy-MM-dd'T'HH:mm:ss.SSSXXX  (yyyy-MM-dd'T'HH:mm:ss.SSSZ) TO  yyyy-MM-dd HH:mm:ss
	 * @throws ParseException
	 */
	public static String dealDateFormat(String oldDateStr) throws ParseException {
		//此格式只有  jdk 1.7才支持  yyyy-MM-dd'T'HH:mm:ss.SSSXXX
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");  //yyyy-MM-dd'T'HH:mm:ss.SSSZ
		Date  date = df.parse(oldDateStr);
		SimpleDateFormat df1 = new SimpleDateFormat ("EEE MMM dd HH:mm:ss Z yyyy", Locale.UK);
		Date date1 =  df1.parse(date.toString());
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//  Date date3 =  df2.parse(date1.toString());
		return df2.format(date1);
	}

	@Test
	void t() {
		String s = "asfasfsaf.jpg";

		s = s.replace(".jpg", ".min.jpg");

		System.out.println(s);
	}

	@Test
	void contextLoads() throws Exception{
		SimpleDateFormat dateToCnFormat = new SimpleDateFormat(DATE_TO_CN_PATTERN);


		System.out.println(dealDateFormat("2024-07-02T00:00:00.000+0800"));

		DateTime dt = new DateTime(toFormatDate(dealDateFormat("2024-07-02T00:00:00.000+0800")));
		if ((dt.getHourOfDay()==0) && (dt.getMinuteOfHour()==0)) {
			String expiredDate = dateToCnFormat.format(dt.minusDays(1).toDate());
			System.out.println(expiredDate);
		}
	}

}
