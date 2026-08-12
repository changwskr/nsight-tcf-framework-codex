package nhnis.fw.commons.util;

public class LunarCalendar {

    private final int[][] lunarMonthTable;

    private boolean isLunarLeapMonth;

    public LunarCalendar() {
        this.lunarMonthTable = new int[][] {
            { 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 5, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 1, 2, 3, 2, 1, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 1 },
            { 2, 2, 1, 2, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 2, 4, 1, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2 },
            { 1, 5, 1, 2, 1, 2, 1, 2, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 5, 1, 2, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2 },
            { 2, 2, 1, 2, 5, 1, 2, 1, 2, 1, 1, 2 },
            { 2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1 },
            { 2, 3, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 5, 2, 2, 1, 2, 2 },
            { 1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2 },
            { 2, 1, 2, 2, 3, 2, 1, 1, 2, 1, 2, 2 },
            { 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 1, 2 },
            { 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1, 1 },
            { 2, 1, 2, 5, 2, 1, 2, 2, 1, 2, 1, 2 },
            { 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1 },
            { 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2 },
            { 1, 5, 1, 2, 1, 1, 2, 2, 1, 2, 2, 2 },
            { 1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 2, 1, 1, 5, 1, 2, 1, 2, 2, 1 },
            { 2, 2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2 },
            { 1, 2, 2, 1, 6, 1, 2, 1, 2, 1, 1, 2 },
            { 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2 },
            { 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1 },
            { 2, 1, 4, 1, 2, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 2, 1, 1, 2, 1, 4, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 2, 1, 2, 2, 4, 1, 1, 2, 1, 2, 1 },
            { 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 1, 2 },
            { 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2 },
            { 1, 1, 2, 4, 1, 2, 1, 2, 2, 1, 2, 2 },
            { 1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2 },
            { 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2 },
            { 2, 5, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 2, 3, 2, 1, 2, 1, 2 },
            { 2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 2, 4, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 2, 1, 2, 2, 1, 2, 2 },
            { 1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2 },
            { 2, 1, 4, 1, 1, 2, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 2, 1, 1, 5, 2, 1, 2, 2 },
            { 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 2, 1, 2, 5, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2, 1 },
            { 2, 1, 2, 3, 2, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 2 },
            { 1, 2, 5, 2, 1, 1, 2, 1, 1, 2, 2, 1 },
            { 2, 2, 1, 2, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 2, 1, 2, 1, 5, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 5, 2, 1, 2, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 2, 1 },
            { 2, 2, 1, 5, 1, 2, 1, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 2, 1, 5, 2, 1, 1, 2 },
            { 2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 1 },
            { 2, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 1, 6, 1, 2, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2 },
            { 1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2, 2 },
            { 2, 1, 2, 3, 2, 1, 1, 2, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2 },
            { 2, 1, 2, 2, 1, 1, 2, 1, 1, 5, 2, 2 },
            { 1, 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2 },
            { 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 1, 1 },
            { 2, 1, 2, 2, 1, 5, 2, 2, 1, 2, 1, 2 },
            { 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1 },
            { 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2 },
            { 1, 2, 1, 1, 5, 1, 2, 1, 2, 2, 2, 2 },
            { 1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2 },
            { 1, 2, 5, 2, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2 },
            { 1, 2, 2, 1, 2, 2, 1, 5, 2, 1, 1, 2 },
            { 1, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2 },
            { 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1 },
            { 2, 1, 1, 2, 3, 2, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1 },
            { 2, 2, 2, 3, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2 },
            { 1, 5, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 1 },
            { 2, 1, 2, 1, 2, 1, 5, 2, 2, 1, 2, 2 },
            { 1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2 },
            { 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 1, 5, 1, 2, 1, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 1, 6, 2, 1, 2, 1, 1, 2, 1, 2, 1 },
            { 2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 2, 1, 2, 1, 2, 5, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2, 1 },
            { 2, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2 },
            { 2, 1, 1, 2, 3, 2, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2 },
            { 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 2, 1, 2, 5, 2, 1, 1, 2, 1, 2, 1, 2 },
            { 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1, 2 },
            { 1, 5, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 5, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2 },
            { 1, 2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 2 },
            { 1, 2, 2, 1, 5, 1, 2, 1, 1, 2, 2, 1 },
            { 2, 2, 1, 2, 2, 1, 1, 2, 1, 1, 2, 2 },
            { 1, 2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 5, 2, 1, 2, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 5, 2 },
            { 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1 },
            { 2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 4, 1, 1, 2, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2 },
            { 2, 2, 1, 2, 1, 2, 1, 2, 1, 1, 2, 1 },
            { 2, 2, 1, 2, 5, 2, 1, 2, 1, 2, 1, 1 },
            { 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 1 },
            { 2, 1, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2 },
            { 1, 5, 1, 2, 1, 2, 1, 2, 2, 2, 1, 2 },
            { 1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2, 2 }
        };
    }

    public enum LUNAR_SOLAR {
        TO_SOLAR, TO_LUNAR;
    }

    public String solar2lunar(String solarDateStr) throws IllegalArgumentException {
        String tempSolarDateStr = solarDateStr.replaceAll("\\D", "");
        int year = Integer.parseInt(tempSolarDateStr.substring(0, 4));
        int month = Integer.parseInt(tempSolarDateStr.substring(4, 6));
        int day = Integer.parseInt(tempSolarDateStr.substring(6, 8));
        return lunarCalc(year, month, day, LUNAR_SOLAR.TO_LUNAR, false);
    }

    public String lunar2solar(String lunarDateStr, boolean isLunarLeapMonth) throws
            IllegalArgumentException {
        String tempLunarDateStr = lunarDateStr.replaceAll("\\D", "");
        int year = Integer.parseInt(tempLunarDateStr.substring(0, 4));
        int month = Integer.parseInt(tempLunarDateStr.substring(4, 6));
        int day = Integer.parseInt(tempLunarDateStr.substring(6, 8));
        return lunarCalc(year, month, day, LUNAR_SOLAR.TO_SOLAR, isLunarLeapMonth);
    }

    public String solar2lunar(int year, int month, int day) throws
            IllegalArgumentException {
        return lunarCalc(year, month, day, LUNAR_SOLAR.TO_LUNAR, false);
    }

    public String lunar2solar(int year, int month, int day, boolean isLunarLeapMonth)
            throws IllegalArgumentException {
        return lunarCalc(year, month, day, LUNAR_SOLAR.TO_SOLAR, isLunarLeapMonth);
    }

    public String lunarCalc(int year, int month, int day, LUNAR_SOLAR type,
            boolean isLeapMonth) throws IllegalArgumentException {
        int solYear = 0;
        int solMonth = 0;
        int solDay = 0;
        int lunYear = 0;
        int lunMonth = 0;
        int lunDay = 0;
        if (year < 1900 || year > 2040)
            throw new IllegalArgumentException("Unsupported year range : " + year);

        int[] solMonthDay = { 31, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if (year >= 2000) {
            solYear = 2000;
            solMonth = 1;
            solDay = 1;
            lunYear = 1999;
            lunMonth = 11;
            lunDay = 25;
            this.isLunarLeapMonth = false;
            solMonthDay[1] = 29;
        } else if (year >= 1970) {
            solYear = 1970;
            solMonth = 1;
            solDay = 1;
            lunYear = 1969;
            lunMonth = 11;
            lunDay = 24;
            this.isLunarLeapMonth = false;
            solMonthDay[1] = 28;
        } else if (year >= 1940) {
            solYear = 1940;
            solMonth = 1;
            solDay = 1;
            lunYear = 1939;
            lunMonth = 11;
            lunDay = 22;
            this.isLunarLeapMonth = false;
            solMonthDay[1] = 29;
        } else {
            solYear = 1900;
            solMonth = 1;
            solDay = 1;
            lunYear = 1899;
            lunMonth = 12;
            lunDay = 1;
            this.isLunarLeapMonth = false;
            solMonthDay[1] = 28;
        }
        int lunIndex = lunYear - 1899;
        do {
            byte b = 0;
            if (type.equals(LUNAR_SOLAR.TO_LUNAR) && year == solYear
                    && month == solMonth && day == solDay)
                return String.format("%04d%02d%02d",
                        new Object[] { Integer.valueOf(lunYear),
                                Integer.valueOf(lunMonth), Integer.valueOf(lunDay) });
            if (type.equals(LUNAR_SOLAR.TO_SOLAR) && year == lunYear
                    && month == lunMonth && day == lunDay
                    && isLeapMonth == this.isLunarLeapMonth)
                return String.format("%04d%02d%02d",
                        new Object[] { Integer.valueOf(solYear),
                                Integer.valueOf(solMonth), Integer.valueOf(solDay) });
            if (solMonth == 12 && solDay == 31) {
                solYear++;
                solMonth = 1;
                solDay = 1;
                if (solYear % 400 == 0) {
                    solMonthDay[1] = 29;
                } else if (solYear % 100 == 0) {
                    solMonthDay[1] = 28;
                } else if (solYear % 4 == 0) {
                    solMonthDay[1] = 29;
                } else {
                    solMonthDay[1] = 28;
                }
            } else if (solMonthDay[solMonth - 1] == solDay) {
                solMonth++;
                solDay = 1;
            } else {
                solDay++;
            }

            if (lunMonth == 12 && ((this.lunarMonthTable[lunIndex][lunMonth - 1]
                    == 1 && lunDay == 29)
                    || (this.lunarMonthTable[lunIndex][lunMonth - 1] == 2
                    && lunDay == 30))) {
                lunYear++;
                lunMonth = 1;
                lunDay = 1;
                lunIndex = lunYear - 1899;
                if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 1) {
                    b = 29;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 2) {
                    b = 30;
                }
            } else if (lunDay == b) {
                if (this.lunarMonthTable[lunIndex][lunMonth - 1] >= 3
                        && !this.isLunarLeapMonth) {
                    lunDay = 1;
                    this.isLunarLeapMonth = true;
                } else {
                    lunMonth++;
                    lunDay = 1;
                    this.isLunarLeapMonth = false;
                }
                if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 1) {
                    b = 29;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 2) {
                    b = 30;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 3) {
                    b = 29;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 4
                        && !this.isLunarLeapMonth) {
                    b = 29;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 4
                        && this.isLunarLeapMonth) {
                    b = 30;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 5
                        && !this.isLunarLeapMonth) {
                    b = 30;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 5
                        && this.isLunarLeapMonth) {
                    b = 29;
                } else if (this.lunarMonthTable[lunIndex][lunMonth - 1] == 6) {
                    b = 30;
                }
            } else {
                lunDay++;
            }
        } while (!type.equals(LUNAR_SOLAR.TO_SOLAR) || !isLeapMonth
                || lunYear <= year || lunMonth <= month);
        throw new IllegalArgumentException("Input date is not a leap year : " + year);
    }

    public boolean isLeapMonth() {
        return this.isLunarLeapMonth;
    }
}
