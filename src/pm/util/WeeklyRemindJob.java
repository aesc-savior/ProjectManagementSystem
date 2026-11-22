package pm.util;

import pm.dao.WeeklyDao;
import pm.web.WeeklyServlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 每周五 18:00 检查周报未交情况，给本人和项目经理各写一条站内提醒
// 不需要的话把 pm.remind.enabled 设成 false
public class WeeklyRemindJob implements ServletContextListener {

    private ScheduledExecutorService pool;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!Conf.getBool("pm.remind.enabled", "PM_REMIND_ENABLED", true)) {
            log("周报催交定时任务已禁用");
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime target = now.with(DayOfWeek.FRIDAY).withHour(18).withMinute(0).withSecond(0).withNano(0);
        if (!target.isAfter(now)) {
            target = target.plusWeeks(1);
        }
        long delay = Math.max(60, Duration.between(now, target).getSeconds());
        pool = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "weekly-remind");
            t.setDaemon(true);
            return t;
        });
        pool.scheduleAtFixedRate(this::runOnce, delay, 7 * 24 * 3600L, TimeUnit.SECONDS);
        log("周报催交定时任务已启动，下次执行：" + target);
    }

    private void runOnce() {
        try {
            String week = WeeklyDao.currentWeek();
            int sent = WeeklyServlet.remindMissing(week, "", "0", true, "系统");
            log("周报催交已执行，周次 " + week + "，提醒 " + sent + " 人");
        } catch (Throwable t) {
            log("周报催交执行失败：" + t.getMessage());
        }
    }

    private void log(String msg) {
        System.out.println("[WeeklyRemindJob] " + msg);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}
