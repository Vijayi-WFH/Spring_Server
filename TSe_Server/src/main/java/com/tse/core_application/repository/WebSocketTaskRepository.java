package com.tse.core_application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tse.core_application.model.Task;
//import springbootaddUpdateTask.Model.Task;

@Repository
public interface WebSocketTaskRepository extends JpaRepository<Task, Long> {

	//  This method will fire sql query on "Task" table
	@Query("select immediateAttentionFrom from Task t where t.taskId = :i")
	public String getAllImmediateAttentionFrom(@Param("i") Long taskId);
	
	//  This method will fire sql query on "Task" table
	@Query("select immediateAttention from Task t where t.taskId = :i")
	public String getImmediateAttention(@Param("i") Long taskId);
	
}
