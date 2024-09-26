package io.hhplus.tdd.point.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;

@Component
class UserPointTest {

    @Test
    public void testHasInsufficientPoint_True() {
        // given
        UserPoint userPoint = new UserPoint(111L, 500L, System.currentTimeMillis());

        // when
        boolean result = userPoint.hasInsufficientPoint(600L);

        // then
        assertTrue(result, "포인트가 부족할 때 true를 반환해야 합니다.");
    }

    @Test
    public void testHasInsufficientPoint_False() {
        // given
        UserPoint userPoint = new UserPoint(111L, 1000L, System.currentTimeMillis());

        // when
        boolean result = userPoint.hasInsufficientPoint(500L);

        // then
        assertFalse(result, "포인트가 충분할 때 false를 반환해야 합니다.");
    }
}