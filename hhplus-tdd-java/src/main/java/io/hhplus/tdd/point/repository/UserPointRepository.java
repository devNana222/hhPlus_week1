package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.dto.UserPoint;

public interface UserPointRepository {
    UserPoint insertOrUpdate(long id, long amount);
    UserPoint selectById(Long id);
}
