package me.cychen.rts.util;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.event.SchedulerIntervalEvent;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;

//import javafx.scene.paint.Color;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by cy on 3/25/2017.
 */
public class ExcelLogHandler {
    private static String DEFAULT_XLSX_FILE_PATH = "out.xlsx";
    XSSFWorkbook workbook;
    XSSFSheet sheet;
    int columnOffset = 1;
    int rowIndex = 0;

    public ExcelLogHandler() {
        workbook = new XSSFWorkbook();
        createNewSheet();

        // Set default column width
        sheet.setDefaultColumnWidth(1);

        // Set title column.
        sheet.setColumnWidth(0, 4000);
    }

    public void createNewSheet() {
        sheet = workbook.createSheet("log");
    }

    public void genRowSchedulerIntervalEvents(EventContainer inEvents) {
        Row row = sheet.createRow(rowIndex++);

        // Set title column.
        row.createCell(0).setCellValue("Schedule");

        for (SchedulerIntervalEvent thisScheduleEvent : inEvents.getSchedulerEvents()) {
            for (long i=thisScheduleEvent.getOrgBeginTimestamp(); i<thisScheduleEvent.getOrgEndTimestamp(); i++) {
                Cell cell = row.createCell((int)(i+columnOffset));
                cell.setCellValue(thisScheduleEvent.getTask().getId());
            }
        }
    }

    public void genRowBusyIntervals(BusyIntervalEventContainer inBis) {
        Row row = sheet.createRow(rowIndex++);

        // Set title column.
        row.createCell(0).setCellValue("Busy Intervals");

        for (BusyIntervalEvent thisBi : inBis.getBusyIntervals()) {
            for (long i=thisBi.getOrgBeginTimestamp(); i<thisBi.getOrgEndTimestamp(); i++) {
                Cell cell = row.createCell((int)(i+columnOffset));
                cell.setCellValue("B");
            }
        }
    }

    private void setColumnWidthRange(int inIndexBegin, int inIndexEnd, int inWidth) {
        for (int i=inIndexBegin; i<=inIndexEnd; i++) {
            sheet.setColumnWidth(i, inWidth);
        }
    }


//    private void setCellColor(XSSFCell inCell) {
//        XSSFCellStyle cellStyle = workbook.createCellStyle();
//        cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
//        cellStyle.setFillForegroundColor(XSSFColor.toXSSFColor());
//        //new XSSFColor()
//        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        inCell.setCellStyle(cellStyle);
//    }

    public Boolean saveAndClose(String inFilePath) {
        String filePath;
        if (inFilePath == null) {
            filePath = DEFAULT_XLSX_FILE_PATH;
        } else {
            filePath = inFilePath;
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath);
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
