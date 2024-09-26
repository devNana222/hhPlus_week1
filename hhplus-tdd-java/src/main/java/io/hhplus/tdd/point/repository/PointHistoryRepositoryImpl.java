package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.dto.PointHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PointHistoryRepositoryImpl implements PointHistoryRepository{
    PointHistoryTable pointHistoryTable = new PointHistoryTable();

    @Override
    public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis) {
        return pointHistoryTable.insert(userId, amount, type, updateMillis);
    }

    @Override
    public List<PointHistory> selectAllByUserId(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}
