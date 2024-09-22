package io.hhplus.tdd.point;

import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.UserNotFoundException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointServiceImpl(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // 포인트 조회
    @Override
    public UserPoint getUserPointById(long id) throws UserNotFoundException {

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint.point() == 0 && userPoint.updateMillis() == 0) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        return userPoint;
    }

    //포인트 충전, 사용 히스토리 조회
    @Override
    public List<PointHistory> getUserPointHistories(long id) throws UserNotFoundException {
        List<PointHistory> userPointList = pointHistoryTable.selectAllByUserId(id);

        if(userPointList.isEmpty()) {
            throw new UserNotFoundException("User with ID " + id + " has no history of charging/using.");
        }

        return userPointList;
    }

    //포인트 충전
    @Override
    public UserPoint chargePoint(long id, long amount) throws UserNotFoundException {

        if(amount <= 0){
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        UserPoint updatedPoint = userPointTable.insertOrUpdate(id, amount);

        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        log.info(pointHistoryTable.selectAllByUserId(id).toString()); //검증

        return updatedPoint;
    }

    //포인트 사용
    @Override
    public UserPoint usePoint(long id, long amount) throws UserNotFoundException, InsufficientPointsException {
        if(amount <= 0){
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        UserPoint userPoint = userPointTable.selectById(id);

        if(userPoint.point() < amount){
            log.error("User doesn't have enough charging points.");

            throw new InsufficientPointsException("User doesn't have enough charging points.");
        }

        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(id, amount);
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return updatedUserPoint;
    }
}
