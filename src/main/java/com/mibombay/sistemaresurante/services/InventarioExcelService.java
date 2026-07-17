package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.models.Empresa;
import com.mibombay.sistemaresurante.models.InventarioFisico;
import com.mibombay.sistemaresurante.models.InventarioFisicoDetalle;
import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.repositories.EmpresaRepository;
import com.mibombay.sistemaresurante.repositories.InventarioFisicoDetalleRepository;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InventarioExcelService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioFisicoDetalleRepository detalleRepository;

    public InventarioExcelService(EmpresaRepository empresaRepository,
                                  UsuarioRepository usuarioRepository,
                                  InventarioFisicoDetalleRepository detalleRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.detalleRepository = detalleRepository;
    }

    public byte[] generar(InventarioFisico inventario) throws IOException {
        Empresa empresa = empresaRepository.findById(inventario.getEmpresaId()).orElse(null);
        String usuarioNombre = usuarioRepository.findById(inventario.getUsuarioId())
                .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                .orElse("Usuario #" + inventario.getUsuarioId());
        List<InventarioFisicoDetalle> detalles = detalleRepository
                .findByInventarioFisicoIdAndActivoTrueOrderByItemTipoAscItemNombreAsc(inventario.getId());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inventario Físico");

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

            CellStyle diffPosStyle = workbook.createCellStyle();
            diffPosStyle.cloneStyleFrom(dataStyle);
            diffPosStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            diffPosStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font diffFont = workbook.createFont();
            diffFont.setBold(true);
            diffFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            diffPosStyle.setFont(diffFont);

            CellStyle diffNegStyle = workbook.createCellStyle();
            diffNegStyle.cloneStyleFrom(dataStyle);
            diffNegStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            diffNegStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font diffNegFont = workbook.createFont();
            diffNegFont.setBold(true);
            diffNegFont.setColor(IndexedColors.DARK_RED.getIndex());
            diffNegStyle.setFont(diffNegFont);

            CellStyle diffZeroStyle = workbook.createCellStyle();
            diffZeroStyle.cloneStyleFrom(dataStyle);
            Font diffZeroFont = workbook.createFont();
            diffZeroFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            diffZeroStyle.setFont(diffZeroFont);

            int row = 0;
            Row r = sheet.createRow(row++);
            Cell c = r.createCell(0);
            c.setCellValue("Inventario Físico");
            c.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            r = sheet.createRow(row++);
            c = r.createCell(0);
            c.setCellValue("Empresa:");
            c.setCellStyle(labelStyle);
            r.createCell(1).setCellValue(empresa != null ? empresa.getNombre() : "—");

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Fecha:");
            r.getCell(0).setCellStyle(labelStyle);
            r.createCell(1).setCellValue(inventario.getFecha().format(DateTimeFormatter.ISO_DATE));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Responsable:");
            r.getCell(0).setCellStyle(labelStyle);
            r.createCell(1).setCellValue(usuarioNombre);

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Estado:");
            r.getCell(0).setCellStyle(labelStyle);
            r.createCell(1).setCellValue(inventario.getEstado().name());

            if (inventario.getFechaConfirmacion() != null) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Confirmado:");
                r.getCell(0).setCellStyle(labelStyle);
                r.createCell(1).setCellValue(inventario.getFechaConfirmacion()
                        .format(DateTimeFormatter.ISO_DATE_TIME));
            }

            if (inventario.getObservaciones() != null && !inventario.getObservaciones().isBlank()) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Observaciones:");
                r.getCell(0).setCellStyle(labelStyle);
                r.createCell(1).setCellValue(inventario.getObservaciones());
            }

            row++;

            String[] headers = {"Item", "Tipo", "Unidad", "Stock Sistema", "Stock Físico", "Diferencia"};
            r = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                c = r.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int ajustes = 0;
            for (InventarioFisicoDetalle d : detalles) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(d.getItemNombre());
                r.createCell(1).setCellValue(d.getItemTipo());
                r.createCell(2).setCellValue(d.getUnidadMedida() != null ? d.getUnidadMedida() : "");
                r.createCell(3).setCellValue(d.getStockSistema().doubleValue());
                r.createCell(4).setCellValue(d.getStockFisico() != null ? d.getStockFisico().doubleValue() : 0);
                BigDecimal diff = d.getDiferencia() != null ? d.getDiferencia().negate() : BigDecimal.ZERO;
                Cell diffCell = r.createCell(5);
                diffCell.setCellValue(diff.doubleValue());
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    diffCell.setCellStyle(diffPosStyle);
                    ajustes++;
                } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                    diffCell.setCellStyle(diffNegStyle);
                    ajustes++;
                } else {
                    diffCell.setCellStyle(diffZeroStyle);
                }
                for (int i = 0; i < 5; i++) {
                    if (r.getCell(i).getCellStyle() == null) {
                        r.getCell(i).setCellStyle(dataStyle);
                    }
                }
                r.getCell(0).setCellStyle(dataStyle);
                r.getCell(1).setCellStyle(dataStyle);
                r.getCell(2).setCellStyle(dataStyle);
                r.getCell(3).setCellStyle(dataStyle);
                r.getCell(4).setCellStyle(dataStyle);
            }

            r = sheet.createRow(row++);
            c = r.createCell(0);
            c.setCellValue("Total items: " + detalles.size() + " | Items con diferencia: " + ajustes);
            c.setCellStyle(labelStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(0, 8000);

            sheet.createFreezePane(0, row - detalles.size() - 1);

            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
