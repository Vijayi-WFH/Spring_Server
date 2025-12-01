package com.tse.core_application.service.Impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.custom.model.ColumnName;
import com.tse.core_application.repository.TableColumnsTypeRepository;

@Service
public class TableColumnsTypeService {
	
	@Autowired
	private TableColumnsTypeRepository tableColumnsTypeRepository;
	
	ObjectMapper objectMapper = new ObjectMapper();
	
	
	//  to find all columnName by columnType
	public List<ColumnName> getUpdatableFields(int columntype) {
		List<ColumnName> columnNames = tableColumnsTypeRepository.findColumnNameByColumnType(columntype);
		return columnNames;
	}


	// to list all basic updatable fields from Db
	public ArrayList<String> getDbBasicFields() {

		ArrayList<String> arrayListBasicFields = new ArrayList<String>();
		List<ColumnName> basicColumnNames = tableColumnsTypeRepository.findColumnNameByColumnType(1);

		for (ColumnName columnName : basicColumnNames) {
			HashMap<String, Object> mapDbBasicField = objectMapper.convertValue(columnName, HashMap.class);
			Object value1 = mapDbBasicField.get("columnName");
			String stringValue1 = (String) value1;
			arrayListBasicFields.add(stringValue1);
		}
		return arrayListBasicFields;
	}

	// to list all essential updatable fields from Db
	public ArrayList<String> getDbEssentialFields() {

		ArrayList<String> arrayListEssentialFields = new ArrayList<String>();
		List<ColumnName> essentialColumnNames = tableColumnsTypeRepository.findColumnNameByColumnType(2);

		for (ColumnName columnName : essentialColumnNames) {
			HashMap<String, Object> mapDbEssentialField = objectMapper.convertValue(columnName, HashMap.class);
			Object value1 = mapDbEssentialField.get("columnName");
			String stringValue1 = (String) value1;
			arrayListEssentialFields.add(stringValue1);
		}
		return arrayListEssentialFields;
	}

	public ArrayList<String> getMeetingDbBasicFields() {

		ArrayList<String> arrayListBasicFields = new ArrayList<String>();
		List<ColumnName> basicColumnNames = tableColumnsTypeRepository.findColumnNameByColumnTypeAndTableName(1, "Meeting");

		for (ColumnName columnName : basicColumnNames) {
			String stringValue1 = columnName.getColumnName();
			arrayListBasicFields.add(stringValue1);
		}
		return arrayListBasicFields;
	}

	
}
