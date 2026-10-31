package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositDetails;
import org.ikigaidigital.application.Withdrawal;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeDepositController.class)
class TimeDepositControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    ListTimeDepositsUseCase listTimeDeposits;

    @MockitoBean
    UpdateAllBalancesUseCase updateAllBalances;

    @Test
    void getReturnsEveryDepositWithTheAgreedFields() throws Exception {
        given(listTimeDeposits.listAll()).willReturn(List.of(
            new TimeDepositDetails(new TimeDeposit(1, "basic", 1000.50, 45),
                List.of(new Withdrawal(7, new BigDecimal("100.00"), LocalDate.of(2024, 1, 15)))),
            new TimeDepositDetails(new TimeDeposit(2, "premium", 5000.00, 100), List.of())));

        mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].planType").value("basic"))
            .andExpect(jsonPath("$[0].balance").value(1000.50))
            .andExpect(jsonPath("$[0].days").value(45))
            .andExpect(jsonPath("$[0].withdrawals[0].id").value(7))
            .andExpect(jsonPath("$[0].withdrawals[0].amount").value(100.0))
            .andExpect(jsonPath("$[0].withdrawals[0].date").value("2024-01-15"))
            .andExpect(jsonPath("$[1].withdrawals").isEmpty());
    }

    @Test
    void postTriggersTheBalanceUpdateAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/time-deposits/balance-updates"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(updateAllBalances).updateAllBalances();
    }

    @Test
    void theApplicationExposesExactlyTwoEndpoints() {
        var ownEndpoints = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("org.ikigaidigital"))
            .map(entry -> describe(entry.getKey()))
            .toList();

        assertThat(ownEndpoints).containsExactlyInAnyOrder(
            "GET /time-deposits",
            "POST /time-deposits/balance-updates");
    }

    private String describe(RequestMappingInfo info) {
        RequestMethod method = info.getMethodsCondition().getMethods().iterator().next();
        return method + " " + info.getPathPatternsCondition().getPatterns().iterator().next();
    }
}
