package io.hhplus.tdd.point;

import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.UserNotFoundException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointServiceImpl pointService;


    @Test
    @DisplayName("🟢성공적인 포인트 조회")
    public void testPoint_Success() throws Exception {
        // given
        Long userId = 111L;
        Long point = 500L;
        long currentTime = System.currentTimeMillis();

        UserPoint userPoint = new UserPoint(userId, point, currentTime);

        // when
        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // then
        UserPoint result = pointService.getUserPointById(userId);

        assertEquals(result.id(), userId);
        assertEquals(result.point(), point);
        assertEquals(result.updateMillis(), currentTime);
    }

    @Test
    @DisplayName("🔴없는 회원의 포인트 조회")
    public void testPoint_Fail() throws Exception {
        Long userId = 222L;

        assertThrows(UserNotFoundException.class, () -> {
            pointService.getUserPointById(userId);
        });
    }

    @Test
    @DisplayName("🟢성공적인 포인트 히스토리 조회")
    public void testPointHistory_Success() throws Exception {
        // given
        long count = 1;
        long currentTime = System.currentTimeMillis();
        long userId = 111L;

        List<PointHistory> pointHistories = List.of(
                new PointHistory(count, userId, 500, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(count++, userId, 300, TransactionType.USE, System.currentTimeMillis()),
                new PointHistory(count++, userId, 200, TransactionType.CHARGE, currentTime)
        );
        // when: 서비스 메서드 호출
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistories);

        List<PointHistory> result = pointService.getUserPointHistories(userId);

        // then: 결과 검증
        assertEquals(pointHistories.size(), result.size());
        assertEquals(pointHistories.get(0), result.get(0));
        assertEquals(pointHistories.get(1), result.get(1));
    }

    @Test
    @DisplayName("🔴존재하지 않는 유저의 포인트 히스토리 조회 실패")
    public void testGetUserPointHistories_UserNotFound() throws Exception {
        long userId = 222L;

        // given
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.emptyList());

        // when & then
        assertThrows(UserNotFoundException.class, () -> {
            pointService.getUserPointHistories(userId);
        });
    }

    @Test
    @DisplayName("🟢유저 포인트 충전 테스트 - 성공 케이스")
    public void testChargePoint_Success() {
        // given
        long userId = 1111;
        long chargeAmount = 1500;
        long currentPoint = 1000;

        // 기존 포인트 상태 모킹
        UserPoint existingPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        // 포인트 충전 결과 모킹
        UserPoint updatedPoint = new UserPoint(userId, currentPoint + chargeAmount, System.currentTimeMillis());
        when(userPointTable.insertOrUpdate(userId, currentPoint + chargeAmount)).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        assertEquals(updatedPoint.id(), result.id());
        assertEquals(updatedPoint.point(), result.point());
        assertEquals(updatedPoint.updateMillis(), result.updateMillis());

        // verify: pointHistoryTable에 기록이 추가되었는지 확인
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("🔴유저 포인트 충전 테스트 - 잘못된 금액일 때 예외 발생")
    public void testChargePoint_InvalidAmount() {
        // given
        long userId = 1111;
        long invalidAmount = -500; // 음수 금액

        // when & then: 잘못된 금액으로 충전 시 예외 발생 확인
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, invalidAmount);
        });

        // verify: 히스토리나 포인트 업데이트가 일어나지 않았는지 검증
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("🔴유저 포인트 충전 테스트 - 최소 충전금액 미달 시 예외 발생")
    public void testChargePoint_LessAmount() {
        // given
        long userId = 1111;
        long invalidAmount = 500; //1000원 미만으로 충전

        // when & then: 잘못된 금액으로 충전 시 예외 발생 확인
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, invalidAmount);
        });

        // verify: 히스토리나 포인트 업데이트가 일어나지 않았는지 검증
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("🟢유저 포인트 사용 테스트 - 성공 케이스")
    public void testUsePoint_Success() throws Exception {
        long userId = 111L;
        long initialPoint = 2000L;
        long useAmount = 500L;
        long remainingPoint = initialPoint - useAmount;

        // 주어진 유저 포인트 설정
        UserPoint userPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, remainingPoint, System.currentTimeMillis());

        // Mock 설정
        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, remainingPoint)).thenReturn(updatedUserPoint);

        // when: 포인트 사용 메서드 호출
        UserPoint result = pointService.usePoint(userId, useAmount);

        // then: 결과 검증
        assertEquals(updatedUserPoint, result);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("🔴유저 포인트 사용 테스트 - 실패 케이스 / 정상적인 금액 데이터가 아닐 때")
    public void testUsePoint_InvalidAmount() {
        // given
        long userId = 1111;
        long invalidAmount = -500; // 음수 금액

        // when & then: 잘못된 금액으로 충전 시 예외 발생 확인
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, invalidAmount);
        });

        // verify: 히스토리나 포인트 업데이트가 일어나지 않았는지 검증
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("🔴유저 포인트 사용 테스트 - 실패 케이스 / 사용 금액이 실제 금액보다 많을 때")
    public void testUsePoint_LessAmount() {
        // given
        long userId = 1111;
        long invalidAmount = 5000;

        // 주어진 유저 포인트 설정
        UserPoint userPoint = new UserPoint(userId, 3000, System.currentTimeMillis());

        // Mock 설정
        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when & then: 가진 금액보다 많은 포인트 사용 시 예외 발생 확인
        assertThrows(InsufficientPointsException.class, () -> {
            pointService.usePoint(userId, invalidAmount);
        });

        // verify: 히스토리나 포인트 업데이트가 일어나지 않았는지 검증
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("🔴유저 포인트 사용 테스트 - 실패 케이스 / 최대 사용금액을 넘었을 때")
    public void testUsePoint_OverAmount() {
        // given
        long userId = 1111;
        long invalidAmount = 7000;

        // when & then: 잘못된 금액으로 충전 시 예외 발생 확인
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, invalidAmount);
        });

        // verify: 히스토리나 포인트 업데이트가 일어나지 않았는지 검증
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }
}
