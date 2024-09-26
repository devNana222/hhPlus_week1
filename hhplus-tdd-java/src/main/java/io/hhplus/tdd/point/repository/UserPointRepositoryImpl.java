package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.UserPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserPointRepositoryImpl implements UserPointRepository {
    UserPointTable userPointTable = new UserPointTable();

    @Override
    public UserPoint insertOrUpdate(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }

    @Override
    public UserPoint selectById(Long id) {
        return userPointTable.selectById(id);
    }
}
