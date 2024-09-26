package io.hhplus.tdd.point;

import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.UserNotFoundException;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@AllArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    private final ReentrantLock lock = new ReentrantLock();  // 동시성 제어를 위한 Lock

    // 포인트 조회
    @Override
    public UserPoint getUserPointById(long id) throws UserNotFoundException {

        UserPoint userPoint = userPointRepository.selectById(id);

        if (userPoint == null) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }

        return userPoint;
    }

    //포인트 충전, 사용 히스토리 조회
    @Override
    public List<PointHistory> getUserPointHistories(long id) throws UserNotFoundException, IllegalArgumentException {
        List<PointHistory> userPointList = pointHistoryRepository.selectAllByUserId(id);

        if(userPointList.isEmpty()) {
            throw new UserNotFoundException("User with ID " + id + " has no history of charging/using.");
        }

        return userPointList;
    }

    //포인트 충전
    @Override
    public UserPoint chargePoint(long id, long amount) throws UserNotFoundException {
        lock.lock();  // 자원에 대한 잠금
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }

            if (amount < 1000) {
                throw new IllegalArgumentException("Amount must be more than 1000.");
            }

            UserPoint userPoint = userPointRepository.selectById(id);
            UserPoint updatedPoint = userPointRepository.insertOrUpdate(id, userPoint.point() + amount);

            pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            log.info(pointHistoryRepository.selectAllByUserId(id).toString()); //검증

            return updatedPoint;
        }
        finally {
            lock.unlock();
        }
    }



    //포인트 사용
    @Override
    public UserPoint usePoint(long id, long amount) throws UserNotFoundException, InsufficientPointsException {
        lock.lock();  // 자원에 대한 잠금
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }

            if (amount > 5000) {
                throw new IllegalArgumentException("Amount must be less than 5000.");
            }

            UserPoint userPoint = userPointRepository.selectById(id);

            if (userPoint.point() < amount) {
                log.error("User doesn't have enough charging points.");

                throw new InsufficientPointsException("User doesn't have enough charging points.");
            }

            long afterPoint = userPoint.point() - amount;

            UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(id, afterPoint);
            pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            log.info(pointHistoryRepository.selectAllByUserId(id).toString()); //검증

            return updatedUserPoint;
        }
        finally {
            lock.unlock();
        }
    }
}
