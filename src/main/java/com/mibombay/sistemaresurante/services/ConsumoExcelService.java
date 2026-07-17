package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ConsumoReporteDTO;
import com.mibombay.sistemaresurante.DTO.ConsumoResumenDTO;
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
import java.util.List;

@Service
public class ConsumoExcelService {

    private final EmpresaRepository empresaRepository;

    public ConsumoExcelService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public byte[] generar(List<ConsumoReporteDTO> reporte, LocalDate desde, LocalDate hasta,
                          Long empresaId, ConsumoResumenDTO resumen) throws IOException {
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        String fechaDesde = desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String fechaHasta = hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Consumo");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelStyle.setFont(labelFont);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.000"));

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(dataStyle);
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            CellStyle percStyle = workbook.createCellStyle();
            percStyle.cloneStyleFrom(dataStyle);
            percStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

            CellStyle negStyle = workbook.createCellStyle();
            negStyle.cloneStyleFrom(dataStyle);
            negStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            negStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font negFont = workbook.createFont();
            negFont.setColor(IndexedColors.DARK_RED.getIndex());
            negFont.setBold(true);
            negStyle.setFont(negFont);

            CellStyle posStyle = workbook.createCellStyle();
            posStyle.cloneStyleFrom(dataStyle);
            posStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            posStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font posFont = workbook.createFont();
            posFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            posFont.setBold(true);
            posStyle.setFont(posFont);

            CellStyle summaryLabelStyle = workbook.createCellStyle();
            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            summaryFont.setFontHeightInPoints((short) 11);
            summaryLabelStyle.setFont(summaryFont);
            summaryLabelStyle.setBorderBottom(BorderStyle.THIN);
            summaryLabelStyle.setBorderTop(BorderStyle.THIN);
            summaryLabelStyle.setBorderLeft(BorderStyle.THIN);
            summaryLabelStyle.setBorderRight(BorderStyle.THIN);

            CellStyle summaryValueStyle = workbook.createCellStyle();
            summaryValueStyle.cloneStyleFrom(summaryLabelStyle);
            summaryValueStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            int row = 0;
            Row r = sheet.createRow(row++);
            Cell c = r.createCell(0);
            c.setCellValue("Reporte de Consumo");
            c.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 13));

            r = sheet.createRow(row++);
            c = r.createCell(0);
            c.setCellValue("Empresa:");
            c.setCellStyle(labelStyle);
            r.createCell(1).setCellValue(empresa != null ? empresa.getNombre() : "—");

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Período:");
            r.getCell(0).setCellStyle(labelStyle);
            r.createCell(1).setCellValue(fechaDesde + " — " + fechaHasta);

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Generado:");
            r.getCell(0).setCellStyle(labelStyle);
            r.createCell(1).setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            row++;

            // Summary section
            if (resumen != null && resumen.getDiasConDatos() > 0) {
                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("RESUMEN COSTO COMIDA" + (resumen.isEsPromedio() ? " (Promedio)" : ""));
                c.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 13));

                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("Ventas");
                c.setCellStyle(summaryLabelStyle);
                c = r.createCell(1);
                c.setCellValue(valMon(resumen.getVentasTotales()));
                c.setCellStyle(summaryValueStyle);
                sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 1));

                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("Costo Ingredientes");
                c.setCellStyle(summaryLabelStyle);
                c = r.createCell(1);
                c.setCellValue(valMon(resumen.getCostoIngredientesVendidos()));
                c.setCellStyle(summaryValueStyle);

                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("Food Cost %");
                c.setCellStyle(summaryLabelStyle);
                c = r.createCell(1);
                c.setCellValue(resumen.getFoodCostPorcentaje().doubleValue() / 100);
                c.setCellStyle(percStyle);

                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("Merma");
                c.setCellStyle(summaryLabelStyle);
                c = r.createCell(1);
                c.setCellValue("$" + formatMon(resumen.getMermaValor()) + " (" + resumen.getMermaPorcentaje() + "%)");
                c.setCellStyle(dataStyle);
                sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 1, 2));

                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("Desperdicio");
                c.setCellStyle(summaryLabelStyle);
                c = r.createCell(1);
                c.setCellValue("$" + formatMon(resumen.getDesperdicioValor()) + " (" + resumen.getDesperdicioPorcentaje() + "%)");
                c.setCellStyle(dataStyle);
                sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 1, 2));

                r = sheet.createRow(row++);
                c = r.createCell(0);
                c.setCellValue("Diferencia");
                c.setCellStyle(summaryLabelStyle);
                c = r.createCell(1);
                c.setCellValue("$" + formatMon(resumen.getDiferenciaValor()) + " (" + resumen.getDiferenciaPorcentaje() + "%)");
                c.setCellStyle(dataStyle);
                sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 1, 2));

                row++;
            }

            // Table header
            String[] headers = {
                    "Item", "Tipo", "Unidad",
                    "Stock " + fechaDesde, "Compras", "Consumo",
                    "Merma", "$ Merma",
                    "Desperdicio", "$ Desperdicio",
                    "Dif. Inexplicada", "$ Diferencia",
                    "Stock " + fechaHasta, "Stock Real"
            };
            r = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                c = r.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (ConsumoReporteDTO dto : reporte) {
                r = sheet.createRow(row++);

                c = r.createCell(0);
                c.setCellValue(dto.getItemNombre());
                c.setCellStyle(dataStyle);

                c = r.createCell(1);
                c.setCellValue(dto.getItemTipo());
                c.setCellStyle(dataStyle);

                c = r.createCell(2);
                c.setCellValue(dto.getUnidadMedida() != null ? dto.getUnidadMedida() : "");
                c.setCellStyle(dataStyle);

                c = r.createCell(3);
                c.setCellValue(val(dto.getStockDesde()));
                if (dto.getStockDesde() != null) c.setCellStyle(numberStyle);

                c = r.createCell(4);
                c.setCellValue(val(dto.getCompras()));
                if (dto.getCompras() != null) c.setCellStyle(numberStyle);

                c = r.createCell(5);
                c.setCellValue(val(dto.getConsumo()));
                if (dto.getConsumo() != null) c.setCellStyle(numberStyle);

                c = r.createCell(6);
                c.setCellValue(val(dto.getMerma()));
                if (dto.getMerma() != null) c.setCellStyle(numberStyle);

                c = r.createCell(7);
                c.setCellValue(valMon(dto.getCostoMerma()));
                if (dto.getCostoMerma() != null) c.setCellStyle(moneyStyle);

                c = r.createCell(8);
                c.setCellValue(val(dto.getDesperdicio()));
                if (dto.getDesperdicio() != null) c.setCellStyle(numberStyle);

                c = r.createCell(9);
                c.setCellValue(valMon(dto.getCostoDesperdicio()));
                if (dto.getCostoDesperdicio() != null) c.setCellStyle(moneyStyle);

                c = r.createCell(10);
                BigDecimal diff = dto.getDiferencia() != null ? dto.getDiferencia() : BigDecimal.ZERO;
                c.setCellValue(diff.doubleValue());
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    c.setCellStyle(posStyle);
                } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                    c.setCellStyle(negStyle);
                } else {
                    c.setCellStyle(dataStyle);
                }

                c = r.createCell(11);
                c.setCellValue(valMon(dto.getCostoDiferencia()));
                if (dto.getCostoDiferencia() != null) c.setCellStyle(moneyStyle);

                c = r.createCell(12);
                c.setCellValue(val(dto.getStockHasta()));
                if (dto.getStockHasta() != null) c.setCellStyle(numberStyle);

                c = r.createCell(13);
                if (dto.getStockReal() != null) {
                    c.setCellValue(dto.getStockReal().doubleValue());
                    c.setCellStyle(numberStyle);
                } else {
                    c.setCellValue("—");
                    c.setCellStyle(dataStyle);
                }
            }

            r = sheet.createRow(row++);
            c = r.createCell(0);
            c.setCellValue("Total items: " + reporte.size());
            c.setCellStyle(labelStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 13));

            for (int i = 0; i < 14; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(0, 8000);

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private double val(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    private double valMon(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    private String formatMon(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
