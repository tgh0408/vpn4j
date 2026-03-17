package org.ssl.vpn4j.schedule.cron;

public interface VpnSchedule {

    String getScheduleName();

    String getScheduleDescription();

    String getCron();

    void run();
}
