package com.tse.core_application.repository;


import com.tse.core_application.custom.model.ColumnName;
import com.tse.core_application.model.TableColumnsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableColumnsTypeRepository extends JpaRepository<TableColumnsType, Integer>{

	//  find columnName by columnType
	public List<ColumnName> findColumnNameByColumnType(Integer columnType);
	public List<ColumnName> findColumnNameByColumnTypeAndTableName(Integer columnType, String tableName);


}
