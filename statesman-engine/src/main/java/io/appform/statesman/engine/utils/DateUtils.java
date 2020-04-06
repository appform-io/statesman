package io.appform.statesman.engine.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {
    public static int weekOfYear() {
        ZoneId zoneId = ZoneId.of("Asia/Calcutta");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
