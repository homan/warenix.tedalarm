package org.dyndns.warenix.tedalarm;

/**
 * TedAlarn understand which day is holiday so it won't wake you on these
 * special days.
 * 
 * @author warenix
 * 
 */
public class TedAlarm {
	/**
	 * a user named description of this alarm, may be the purupose of this alarm
	 * or anything else for user to identify it
	 */
	public String description;
	/**
	 * database id
	 */
	public long id;
	/**
	 * daily hour and minutes converted to millionseconds
	 */
	public long startTime;
	/**
	 * 1 means schedueld, 0 otherwise
	 */
	public long scheduled;
	/**
	 * repeat interval
	 */
	public long repeatMask;

	public String toString() {
		return String
				.format("id[%d] startTime[%d] scheduled[%d] repeatMask[%d] description[%s]",
						id, startTime, scheduled, repeatMask, description);
	}

}
