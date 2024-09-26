package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.dto.PointHistory;

import java.util.List;

public interface PointHistoryRepository {
    PointHistory insert(long userId, long amount, TransactionType type, long updateMillis);
    List<PointHistory> selectAllByUserId(long userId);
}
