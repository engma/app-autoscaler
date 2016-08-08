package org.cloudfoundry.autoscaler.scheduler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Helper class for validating the data
 */
public class DataValidationHelper {

	/**
	 * Checks if the specified object is null.
	 * 
	 * @param object
	 * @return true if not null otherwise false
	 */
	public static boolean isNotNull(Object object) {
		if (object == null)
			return false;
		return true;
	}

	/**
	 * Checks if specified string is not empty (not null and not blank)
	 * 
	 * @param string
	 * @return true or false
	 */
	public static boolean isNotEmpty(String string) {
		if (isNotNull(string) && !string.isEmpty())
			return true;
		return false;
	}

	/**
	 * Checks if specified array is not empty (not null and not empty)
	 * 
	 * @param array
	 * @return
	 */
	public static boolean isNotEmpty(int[] array) {
		if (isNotNull(array) && array.length > 0)
			return true;
		return false;
	}

	/**
	 * Checks if the timezone is valid
	 * 
	 * @param timeZoneId
	 * @return
	 */
	public static boolean isValidTimeZone(String timeZoneId) {
		if (isNotNull(timeZoneId)) {
			List<String> supportedTimeZones = Arrays.asList(DateHelper.supportedTimezones);
			if (supportedTimeZones.contains(timeZoneId)) {
				return true;
			}
			return false;
		} else {
			return false;
		}
	}

	/**
	 * Checks if the specified date time is after now (current time).
	 * @param dateTime
	 * @param timeZone
	 * @return
	 */
	public static boolean isLaterThanNow(Date dateTime, TimeZone timeZone) {
		Calendar calToCompare = DateHelper.getCalendarDate(dateTime, timeZone);
		Calendar calNow = Calendar.getInstance(timeZone);
		if (calToCompare.after(calNow)) {
			return true;
		}
		return false;
	}

	public static boolean isDateAfterOrEqualsNow(Date date, TimeZone policyTimeZone) {
		Date compareTo = DateHelper.getDateWithZoneOffset(date, policyTimeZone);

		Calendar calNow = Calendar.getInstance();
		calNow.set(Calendar.HOUR_OF_DAY, 0);
		calNow.set(Calendar.MINUTE, 0);
		calNow.set(Calendar.SECOND, 0);
		calNow.set(Calendar.MILLISECOND, 0);
		Date now = DateHelper.getDateWithZoneOffset(calNow.getTime(), policyTimeZone);

		return (compareTo.compareTo(now) >= 0);
	}

	/**
	 * Checks id the end date time is after start date time
	 * @param endDateTime
	 * @param startDateTime
	 * @return
	 */
	public static boolean isAfter(Date endDateTime, Date startDateTime) {
		if (isNotNull(endDateTime) && isNotNull(startDateTime)) {
			if (endDateTime.after(startDateTime)) {
				return true;
			}
		}
		return false;

	}

	public static boolean isBetweenMinAndMaxValues(int[] array, int lowerLimit, int upperLimit) {
		Arrays.sort(array);
		int minValue = array[0];
		int maxValue = array[array.length - 1];

		return (minValue >= lowerLimit && maxValue <= upperLimit);
	}

	public static boolean isElementUnique(int[] array) {
		boolean isValid = true;
		Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < array.length; i++) {
			if (!set.add(array[i])) { // Duplicate value found.
				isValid = false;
			}
		}

		return isValid;
	}

	public static List<String[]> isNotOverlapRecurringSchedules(List<RecurringScheduleTime> scheduleTimes) {
		List<String[]> overlapDateTimeValidationErrorMsgList = new ArrayList<>();

		if (scheduleTimes != null && !scheduleTimes.isEmpty()) {
			Collections.sort(scheduleTimes);

			for (int firstIndex = 0; firstIndex < scheduleTimes.size(); firstIndex++) {
				for (int secondIndex = firstIndex + 1; secondIndex < scheduleTimes.size(); secondIndex++) {
					RecurringScheduleTime current = scheduleTimes.get(firstIndex);
					RecurringScheduleTime next = scheduleTimes.get(secondIndex);

					//both are dayOfWeek
					if (current.hasDayOfWeek() && next.hasDayOfWeek()) {
						// check overlap
						String[] overlapDateTimeValidationErrorMsg = validateTimeOverlapping(current, next,
								current.getDayOfWeek(), next.getDayOfWeek());
						if (overlapDateTimeValidationErrorMsg != null) {
							overlapDateTimeValidationErrorMsgList.add(overlapDateTimeValidationErrorMsg);
						}
					}

					if (current.hasDayOfMonth() && next.hasDayOfMonth()) {
						// check overlap
						String[] overlapDateTimeValidationErrorMsg = validateTimeOverlapping(current, next,
								current.getDayOfMonth(), next.getDayOfMonth());
						if (overlapDateTimeValidationErrorMsg != null) {
							overlapDateTimeValidationErrorMsgList.add(overlapDateTimeValidationErrorMsg);
						}
					}
				}
			}
		}
		return overlapDateTimeValidationErrorMsgList;
	}

	private static String[] validateTimeOverlapping(RecurringScheduleTime current, RecurringScheduleTime next,
			List<Integer> currentDays, List<Integer> nextDays) {
		String[] overlapDateTimeValidationErrorMsg = null;

		if (current.getStartTime().compareTo(next.getStartTime()) == 0) {
			if (hasSameElement(currentDays, nextDays)) {
				overlapDateTimeValidationErrorMsg = new String[] { current.getScheduleIdentifier(), "start_time",
						next.getScheduleIdentifier(), "start_time" };
			}

		} else if (current.getEndTime().compareTo(next.getStartTime()) >= 0) {
			if (hasSameElement(currentDays, nextDays)) {
				overlapDateTimeValidationErrorMsg = new String[] { current.getScheduleIdentifier(), "end_time",
						next.getScheduleIdentifier(), "start_time" };
			}
		}

		return overlapDateTimeValidationErrorMsg;
	}

	private static boolean hasSameElement(List<Integer> firstList, List<Integer> secondList) {
		for (Integer element : firstList) {
			if (secondList.contains(element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method is given a collection of SpecificDateScheduleDateTime (holding the schedule 
	 * identifier and its start date time and end date time). It traverses through the collection 
	 * to check if the the date time between different schedules overlap. If there is an overlap 
	 * then an error message is added to a collection and collection of messages is returned.
	 * 
	 * @param scheduleStartEndTimeList
	 * @return - List of date time overlap validation messages
	 */
	public static List<String[]> isNotOverlapForSpecificDate(
			List<SpecificDateScheduleDateTime> scheduleStartEndTimeList) {
		List<String[]> overlapDateTimeValidationErrorMsgList = new ArrayList<>();
		if (scheduleStartEndTimeList != null && !scheduleStartEndTimeList.isEmpty()) {

			Collections.sort(scheduleStartEndTimeList);

			for (int index = 0; index < scheduleStartEndTimeList.size() - 1; index++) {
				SpecificDateScheduleDateTime current = scheduleStartEndTimeList.get(index);
				SpecificDateScheduleDateTime next = scheduleStartEndTimeList.get(index + 1);

				// Check for date time overlaps and create a validation error message string array
				if (current.getStartDateTime().compareTo(next.getStartDateTime()) == 0) {

					// startDateTime values are equal, so an overlap. Set up a message for validation error
					String[] overlapDateTimeValidationErrorMsg = { current.getScheduleIdentifier(), "start_date_time",
							next.getScheduleIdentifier(), "start_date_time" };
					overlapDateTimeValidationErrorMsgList.add(overlapDateTimeValidationErrorMsg);
				}
				// current startDateTime was earlier than next startDateTime, so following check
				else if (current.getEndDateTime().compareTo(next.getStartDateTime()) >= 0) {
					// endDateTime of current is later than or equal to startDateTime of next. Set up a message for validation error
					String[] overlapDateTimeValidationErrorMsg = { current.getScheduleIdentifier(), "end_date_time",
							next.getScheduleIdentifier(), "start_date_time" };
					overlapDateTimeValidationErrorMsgList.add(overlapDateTimeValidationErrorMsg);
				}
			}
		}
		return overlapDateTimeValidationErrorMsgList;
	}

}