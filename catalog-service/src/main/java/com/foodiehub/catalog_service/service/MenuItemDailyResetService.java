package com.foodiehub.catalog_service.service;
import com.foodiehub.catalog_service.dao.MenuItemDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuItemDailyResetService {

    private final MenuItemDao menuItemDao;

    /*
     * Runs every day at 00:00.
     *
     * Cron format used by Spring:
     *
     * second minute hour day month day-of-week
     *
     * 0 0 0 * * *
     *       ↓
     * every day at midnight
     */
    @Scheduled(
            cron = "${catalog.daily-reset.cron:0 0 0 * * *}",
            zone = "${catalog.daily-reset.time-zone:Asia/Kolkata}"
    )
    @Transactional
    public void resetDailyQuantity() {

        int resetCount =
                menuItemDao.resetRemainingToday();

        log.info(
                "Daily menu item stock reset completed. "
                        + "Reset {} menu items.",
                resetCount
        );
    }
}