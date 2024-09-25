package io.hhplus.tdd.point;


import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.UserNotFoundException;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.UserPoint;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface PointService {
    /**기본 기능
     * - 포인트 조회
     * - 포인트 충전/사용 내역 조회
     * - 포인트 충전
     * - 포인트 사용
     * */

    UserPoint getUserPointById(long id) throws UserNotFoundException;
    List<PointHistory> getUserPointHistories (long id) throws UserNotFoundException, IllegalArgumentException;
    UserPoint chargePoint(long id, long amount) throws UserNotFoundException;
    UserPoint usePoint(long id, long amount) throws UserNotFoundException,InsufficientPointsException;
}
