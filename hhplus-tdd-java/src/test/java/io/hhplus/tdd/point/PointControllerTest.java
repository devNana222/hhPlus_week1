package io.hhplus.tdd.point;

import io.hhplus.tdd.ApiControllerAdvice;
import io.hhplus.tdd.Exception.UserNotFoundException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.UserPoint;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;


@WebMvcTest(PointController.class)
@Import(ApiControllerAdvice.class)
public class PointControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;


    /**
     * 포인트 조회
     * 1. 존재하는 유저 테스트
     * 2. 존재하지 않는 유저 테스트
     * */
    @Test
    @DisplayName("GET /point/1111 존재하는 유저 테스트")
    public void testPoint_Success() throws Exception {
        //given
        UserPoint mockUserPoint = new UserPoint(1111, 100,1000);

        //when
        when(pointService.getUserPointById(1111)).thenReturn(mockUserPoint);

        //then
        mockMvc.perform(get("/point/1111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1111))
                .andExpect(jsonPath("$.point").value(100));
    }


    /**
     * 포인트 충전/사용 히스토리 조회 테스트
     * 1. 존재하는 유저
     *  1-1. 유저는 존재하나 히스토리가 존재하지 않음.
     *  1-2. 유저는 존재하나 충전 or 사용 히스토리만 있는 경우
     * 2. 존재하지 않는 유저
     * */

    @Test
    @DisplayName("GET /point/1111/histories 존재하는 유저에 대한 테스트")
    public void testHistory_Success() throws Exception{
        long cursor = 1;

        //given
        List<PointHistory> pointHistories = List.of(
                new PointHistory(cursor++, 1111, 500, TransactionType.CHARGE, 100 ),
                new PointHistory(cursor++, 1111, 1500, TransactionType.CHARGE, 150 ),
                new PointHistory(cursor++, 1111, 2500, TransactionType.USE, 102 ),
                new PointHistory(cursor++, 1111, 500, TransactionType.CHARGE, 102 )
        );

        //when
        when(pointService.getUserPointHistories(1111)).thenReturn(pointHistories);

        mockMvc.perform(get("/point/1111/histories"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$[0].userId").value(1111))
                .andExpect(jsonPath("$[0].amount").value(500))
                .andExpect(jsonPath("$[0].amount").value(500))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].amount").value(1500))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].amount").value(2500))
                .andExpect(jsonPath("$[2].type").value("USE"))
                .andExpect(jsonPath("$[3].amount").value(500))
                .andExpect(jsonPath("$[3].id").value(4));


    }


    @Test
    @DisplayName("GET /point/1234/histories 존재하는 유저가 포인트 내역이 없는 경우")
    public void testHistory_Empty() throws Exception {
        //when
        when(pointService.getUserPointHistories(1234)).thenReturn(Collections.emptyList());

        //then
        mockMvc.perform(get("/point/1234/histories"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /point/2222/histories 존재하지 않는 유저 테스트")
    public void testHistory_UserNotFound() throws Exception {

        //when
        when(pointService.getUserPointHistories(2222)).thenThrow(new UserNotFoundException("UserNotFound"));

        //then
        mockMvc.perform(get("/point/2222/histories"))
                .andExpect(status().isNotFound())
                .andDo(print())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("UserNotFound"));

    }

    @Test
    @DisplayName("GET /point/-1111/histories 잘못된 파라미터가 들어왔을 때")
    public void testHistory_BadRequest() throws Exception {
        long invalidId = -1111;

        mockMvc.perform(get("/point/{id}/histories", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ID must be a positive number."))
                .andDo(print());
    }



    /**
     * 포인트 충전
     * 1. 양수인 정수로 충전하는지
     * 1-1. 충전 성공 시 히스토리에 쌓이는지
     * 2. 회원인 사람만 충전
     * */
    @Test
    @DisplayName("PATCH /point/1111/charge 포인트가 정상적으로 충전되는지 확인.")
    public void testCharge_Success() throws Exception {
        // given
        long userId = 1111;
        long chargeAmount = 1500;

        UserPoint userPoint = new UserPoint(1111, 500, 10000);

        UserPoint afterCharge = new UserPoint(1111, 2000, 10000);
        // when
        when(pointService.chargePoint(userId, chargeAmount)).thenReturn(afterCharge);

        ObjectMapper objectMapper = new ObjectMapper();

        // 요청 본문에 보낼 객체 생성 (단순 객체나 Map 가능)
        Map<String, Long> requestBody = new HashMap<>();
        requestBody.put("amount", chargeAmount);


        // when
        ResultActions resultActions = mockMvc.perform(
                patch("/point/{id}/charge", userPoint.id())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userPoint.id()))
                .andExpect(jsonPath("$.point").value(userPoint.point() + chargeAmount));
    }

}
