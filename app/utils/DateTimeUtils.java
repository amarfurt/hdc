package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {

	public static String getNow() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

}
