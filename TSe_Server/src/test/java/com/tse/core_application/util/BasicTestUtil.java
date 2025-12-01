package com.tse.core_application.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.dto.TestRequestDTO;
import com.tse.core_application.dto.TestResponseDTO;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicTestUtil {

    public static Map<Long, TestRequestDTO> readXlsxFileData(Map<Long, TestRequestDTO> requestDTOMap, String fileName, String sheetName) throws IOException, InvalidFormatException {
        File myFile = new File(fileName);

        FileInputStream fis = new FileInputStream(myFile);
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheet(sheetName);
        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        // Traversing over each row of XLSX file
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if(row.getRowNum()==0)
                continue;
            // For each row, iterate through each columns
            Iterator<Cell> cellIterator = row.cellIterator();
            TestRequestDTO testRequestDTO = new TestRequestDTO();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        System.out.print(cell.getStringCellValue() + "\t");
                        if (cell.getColumnIndex() != 1)
                            testRequestDTO.setRequest(cell.getStringCellValue());

                        else
                            testRequestDTO.setMethodName(cell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        System.out.print(cell.getNumericCellValue() + "\t");
                        testRequestDTO.setId(Double.valueOf(cell.getNumericCellValue()).longValue());
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        System.out.print(cell.getBooleanCellValue() + "\t");
                        testRequestDTO.setActualStatus(cell.getBooleanCellValue());
                        break;
                    default:
                }
            }
            if(testRequestDTO.getId()!=null) {
                requestDTOMap.put(testRequestDTO.getId(), testRequestDTO);
            }
        }
        return requestDTOMap;
    }

    public static void createFailedTestCaseReport(Map<Long, TestResponseDTO> responseMap, String fileName, String sheetName) throws IOException, InvalidFormatException {

        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(fileName));
//        XSSFSheet sheet = workbook.createSheet("TSE Failed Tests Report");
        if(workbook.getSheetIndex(sheetName)==-1){
            XSSFSheet sheet = workbook.createSheet(sheetName);
        }
        XSSFSheet sheet = workbook.getSheet(sheetName);

        AtomicInteger rowCount = new AtomicInteger(1);


        Row startingRow = sheet.createRow(0);
        Cell startingRowCell0 = startingRow.createCell(0);
        startingRowCell0.setCellValue("ID");
        Cell startingRowCell1 = startingRow.createCell(1);
        startingRowCell1.setCellValue("Request");
        Cell startingRowCell2 = startingRow.createCell(2);
        startingRowCell2.setCellValue("Error Localized Message");
        Cell startingRowCell3 = startingRow.createCell(3);
        startingRowCell3.setCellValue("Error Message");
        Cell startingRowCell4 = startingRow.createCell(4);
        startingRowCell4.setCellValue("Cause");
        Cell startingRowCell5 = startingRow.createCell(5);
        startingRowCell5.setCellValue("Status");


        responseMap.forEach((k, v) -> {

            Row row = sheet.createRow(rowCount.getAndIncrement());

            int columnCount = 0;

            Cell cell1 = row.createCell(columnCount++);
            cell1.setCellValue(String.valueOf(v.getId()));


            Cell cell2 = row.createCell(columnCount++);
            cell2.setCellValue(v.getRequestedPayload());

            Cell cell3 = row.createCell(columnCount++);
            cell3.setCellValue(v.getErrorLocalMessage());

            Cell cell4 = row.createCell(columnCount++);
            cell4.setCellValue(v.getErrorMsg());

            Cell cell5 = row.createCell(columnCount++);
            cell5.setCellValue(v.getStackTrace());

            Cell cell6 = row.createCell(columnCount);
            cell6.setCellValue(v.isActualStatus());

        });

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);

        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
            workbook.close();
        }


    }

    public static void markFailure(Map<Long, TestResponseDTO> responseMap, Long k, TestRequestDTO v, String localizedMessage, String message, String stackTrace, boolean status) {
        responseMap.put(k, new TestResponseDTO(k, v.getRequest(), localizedMessage, message, stackTrace, status));
    }

    public static ObjectMapper staticObjectMapper() {
        return new ObjectMapper();
    }

}
