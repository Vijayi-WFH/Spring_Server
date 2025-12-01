package com.tse.core_application.repository;

import com.tse.core_application.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;

//@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {

}
