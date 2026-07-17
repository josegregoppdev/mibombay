package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.FoodCostDiarioDTO;
import com.mibombay.sistemaresurante.DTO.FoodCostItemDTO;
import com.mibombay.sistemaresurante.DTO.FoodCostResumenDTO;
import com.mibombay.sistemaresurante.models.Empresa;
import com.mibombay.sistemaresurante.repositories.EmpresaRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class FoodCostExcelService {

    private final EmpresaRepository empresaRepository;

    public FoodCostExcelService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public byte[] generar(FoodCostDiarioDTO data, Long empresaId) throws IOException {
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        FoodCostResumenDTO r = data.getResumen();

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Food Cost");

            CellStyle titleStyle = estiloTitulo(wb);
            CellStyle labelStyle = estiloLabel(wb);
            CellStyle headerStyle = estiloHeader(wb);
            CellStyle dataStyle = estiloData(wb);
            CellStyle moneyStyle = estiloMoney(wb);
            CellStyle pctStyle = estiloPct(wb);

            int row = 0;
            Row rw = sheet.createRow(row++);
            Cell c = rw.createCell(0);
            c.setCellValue("Reporte Food Cost Diario");
            c.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 4));

            rw = sheet.createRow(row++);
            rw.createCell(0).setCellValue("Empresa:");
            rw.getCell(0).setCellStyle(labelStyle);
            rw.createCell(1).setCellValue(empresa != null ? empresa.getNombre() : "—");

            rw = sheet.createRow(row++);
            rw.createCell(0).setCellValue("Fecha:");
            rw.getCell(0).setCellStyle(labelStyle);
            rw.createCell(1).setCellValue(r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            rw = sheet.createRow(row++);
            rw.createCell(0).setCellValue("Generado:");
            rw.getCell(0).setCellStyle(labelStyle);
            rw.createCell(1).setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            row++;

            // --- Resumen ---
            rw = sheet.createRow(row++);
            c = rw.createCell(0);
            c.setCellValue("Resumen del Día");
            c.setCellStyle(labelStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 4));

            String[][] resumenRows = {
                    {"Ventas Totales", "$ " + fmtMon(r.getVentasTotales())},
                    {"Costo Ingredientes Vendidos", "$ " + fmtMon(r.getCostoIngredientesVendidos())},
                    {"Food Cost %", fmtPct(r.getFoodCostPorcentaje()) + "%"},
                    {"Inventario Inicial ($)", "$ " + fmtMon(r.getInventarioInicialValor())},
                    {"Compras ($)", "$ " + fmtMon(r.getComprasValor())},
                    {"Inventario Final ($)", "$ " + fmtMon(r.getInventarioFinalValor())},
                    {"Costo Alimentos Contable", "$ " + fmtMon(r.getCostoAlimentosContable())},
                    {"Food Cost Contable %", fmtPct(r.getFoodCostContablePorcentaje()) + "%"},
                    {"Merma ($)", "$ " + fmtMon(r.getMermaValor()) + " (" + fmtPct(r.getMermaPorcentaje()) + "%)"},
                    {"Desperdicio ($)", "$ " + fmtMon(r.getDesperdicioValor()) + " (" + fmtPct(r.getDesperdicioPorcentaje()) + "%)"},
            };

            for (String[] rowData : resumenRows) {
                rw = sheet.createRow(row++);
                c = rw.createCell(0);
                c.setCellValue(rowData[0]);
                c.setCellStyle(dataStyle);
                c = rw.createCell(1);
                c.setCellValue(rowData[1]);
                c.setCellStyle(dataStyle);
            }

            row++;

            // --- Items (Ingredientes + Productos sin receta) ---
            rw = sheet.createRow(row++);
            c = rw.createCell(0);
            c.setCellValue("Costo Comida por Item");
            c.setCellStyle(labelStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 6));

            String[] headers = {"Item", "Tipo", "Cant. Consumida", "Unidad", "Precio Costo Unit.", "Grupo ($)", "% Costo"};
            rw = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                c = rw.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (FoodCostItemDTO item : data.getItems()) {
                rw = sheet.createRow(row++);
                rw.createCell(0).setCellValue(item.getItemNombre());
                rw.getCell(0).setCellStyle(dataStyle);

                rw.createCell(1).setCellValue("INGREDIENTE".equals(item.getItemTipo()) ? "Ingrediente" : "CONSUMIBLE".equals(item.getItemTipo()) ? "Consumible" : "Producto");
                rw.getCell(1).setCellStyle(dataStyle);

                c = rw.createCell(2);
                c.setCellValue(item.getCantidadConsumida().doubleValue());
                c.setCellStyle(pctStyle);

                rw.createCell(3).setCellValue(item.getUnidadMedida());
                rw.getCell(3).setCellStyle(dataStyle);

                c = rw.createCell(4);
                c.setCellValue(item.getPrecioCostoUnitario().doubleValue());
                c.setCellStyle(moneyStyle);

                c = rw.createCell(5);
                c.setCellValue(item.getCostoGrupo().doubleValue());
                c.setCellStyle(moneyStyle);

                c = rw.createCell(6);
                c.setCellValue(item.getPorcentajeDelCosto().doubleValue());
                c.setCellStyle(pctStyle);
            }

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(0, 8000);

            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private CellStyle estiloTitulo(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }

    private CellStyle estiloLabel(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private CellStyle estiloHeader(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle estiloData(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle estiloMoney(Workbook wb) {
        CellStyle s = estiloData(wb);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private CellStyle estiloPct(Workbook wb) {
        CellStyle s = estiloData(wb);
        s.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        return s;
    }

    private String fmtMon(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) == 0) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String fmtPct(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) == 0) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
