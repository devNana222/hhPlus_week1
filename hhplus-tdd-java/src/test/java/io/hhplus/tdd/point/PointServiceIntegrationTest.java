package io.hhplus.tdd.point;

import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.UserNotFoundException;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PointServiceIntegrationTest {
    @Autowired
    PointService pointService;

    @Autowired
    PointHistoryRepository pointHistoryRepository;

    @Autowired
    UserPointRepository userPointRepository;

//데이터베이스연결해서 통합테스트

    @BeforeEach
    public void test_SetUp(){
        pointService.chargePoint(111L, 1000L);
    }

    @Test
    @DisplayName("🟢하나의 아이디로 응답값을 도출한다.")
    public void testGetPoint() {
        long id = 111L;

        UserPoint point = pointService.getUserPointById(id);
        assertEquals(point.id(), 111L);
        assertEquals(point.point(), 1000L);
    }

    @Test
    @DisplayName("🟢같은 유저에게 순차적으로 충전/사용 요청 후 리스트 출력- 성공케이스")
    public void testChargePointMulti(){
        long id = 111L;
        try{
            pointService.chargePoint(id, 7000L);
            pointService.usePoint(id,5000L);
            pointService.chargePoint(id,2500L);
            Thread.sleep(1500);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }

        UserPoint point = pointService.getUserPointById(id);
        assertEquals(point.id(), id);
        assertEquals(point.point(), 5500L);

        List<PointHistory> pointHistories = pointHistoryRepository.selectAllByUserId(id);

        assertEquals(pointHistories.size(), 4);
        assertEquals(pointHistories.get(0).userId(), id);
        assertEquals(pointHistories.get(0).amount(), 1000L);
        assertEquals(pointHistories.get(1).amount(), 7000L);
        assertEquals(pointHistories.get(1).type(), TransactionType.CHARGE);
        assertEquals(pointHistories.get(2).amount(), 5000L);
        assertEquals(pointHistories.get(2).type(), TransactionType.USE);
        assertEquals(pointHistories.get(3).amount(), 2500L);
    }

    @Test
    @DisplayName("🟢비동기 충전/사용 테스트 - 성공")
    public void testUseChargePoint(){
        long id = 111L;
        int NUMBER_OF_THREADS = 10; // 원하는 스레드 수

        long originPoint = pointService.getUserPointById(id).point();

        // CompletableFuture 리스트 생성
        List<CompletableFuture<Void>> futures = IntStream.range(0, NUMBER_OF_THREADS)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    // 포인트 1000L 충전 후, 100L 사용
                    pointService.chargePoint(id, 1000L);
                    pointService.usePoint(id, 100L);
                }))
                .collect(Collectors.toList());

        // 모든 CompletableFuture 작업들이 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        UserPoint result = userPointRepository.selectById(id);

        // 포인트 충전 및 사용의 결과를 검증 (충전한 포인트와 사용한 포인트 간의 차이 * 스레드 수)
        assertAll(
                () -> assertEquals(originPoint + (1000L - 100L) * NUMBER_OF_THREADS, result.point())
        );
    }


}
