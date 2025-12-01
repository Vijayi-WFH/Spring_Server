package com.tse.core.repository.supplements;

import com.tse.core.model.supplements.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByPrimaryEmail(String primaryEmail);

    // to check if user exists by primary email
    boolean existsByPrimaryEmail(String primaryEmail);

    User findByUserId(Long userId);

    List<User> findAllUserByManagingUserId(Long userId);

    @Query("select u.userId from User u where u.managingUserId = :userId")
    List<Long> findAllUserIdByManagingUserId(Long userId);
}
