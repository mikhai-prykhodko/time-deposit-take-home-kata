package org.ikigaidigital.application;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAllBalancesLoggingTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(UpdateAllBalancesService.class);

    @BeforeEach
    void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void stopCapturing() {
        logger.detachAppender(appender);
    }

    @Test
    void logsOneLineWithTheNumberOfUpdatedDepositsPerRun() {
        var repository = new InMemoryTimeDepositRepository()
            .add(new TimeDeposit(1, "basic", 1200.00, 31))
            .add(new TimeDeposit(2, "premium", 1200.00, 46));

        new UpdateAllBalancesService(repository, new TimeDepositCalculator()).updateAllBalances();

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
            .startsWith("Balance update finished: 2 time deposits updated in ")
            .endsWith(" ms");
    }
}
