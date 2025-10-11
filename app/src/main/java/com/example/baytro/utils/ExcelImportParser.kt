package com.example.baytro.utils

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

object ExcelImportParser {

    data class BuildingRow(
        val name: String,
        val floors: String,
        val address: String,
        val status: String,
        val billingDate: String,
        val paymentStart: String,
        val paymentDue: String,
        val rowIndex: Int
    )

    data class RoomRow(
        val buildingName: String,
        val address: String,
        val floor: String,
        val roomNumber: String,
        val size: String,
        val status: String,
        val rentalFee: String,
        val interior: String,
        val rowIndex: Int
    )

    data class ParsedWorkbook(
        val buildings: List<BuildingRow>,
        val rooms: List<RoomRow>,
        val errors: List<String>
    )

    fun parse(input: InputStream): ParsedWorkbook {
        input.use { stream ->
            val workbook = XSSFWorkbook(stream)
            val errors = mutableListOf<String>()

            val buildingRows = workbook.getSheet("Buildings")?.let { sheet ->
                sheet.asSequence()
                    .drop(1) // skip header
                    .mapNotNull { row ->
                        val idx = row.rowNum + 1
                        try {
                            BuildingRow(
                                name = getString(row.getCell(0)),
                                floors = getString(row.getCell(1)),
                                address = getString(row.getCell(2)),
                                status = getString(row.getCell(3)),
                                billingDate = getString(row.getCell(4)),
                                paymentStart = getString(row.getCell(5)),
                                paymentDue = getString(row.getCell(6)),
                                rowIndex = idx
                            )
                        } catch (e: Exception) {
                            errors.add("Buildings row $idx: ${e.message}")
                            null
                        }
                    }
                    .filter { row ->
                        // ignore completely empty lines
                        listOf(
                            row.name, row.floors, row.address, row.status,
                            row.billingDate, row.paymentStart, row.paymentDue
                        ).any { it.isNotBlank() }
                    }
                    .toList()
            } ?: emptyList()

            val roomRows = workbook.getSheet("Rooms")?.let { sheet ->
                sheet.asSequence()
                    .drop(1)
                    .mapNotNull { row ->
                        val idx = row.rowNum + 1
                        try {
                            RoomRow(
                                buildingName = getString(row.getCell(0)),
                                address = getString(row.getCell(1)),
                                floor = getString(row.getCell(2)),
                                roomNumber = getString(row.getCell(3)),
                                size = getString(row.getCell(4)),
                                status = getString(row.getCell(5)),
                                rentalFee = getString(row.getCell(6)),
                                interior = getString(row.getCell(7)),
                                rowIndex = idx
                            )
                        } catch (e: Exception) {
                            errors.add("Rooms row $idx: ${e.message}")
                            null
                        }
                    }
                    .filter { row ->
                        listOf(
                            row.buildingName, row.address, row.floor, row.roomNumber,
                            row.size, row.status, row.rentalFee, row.interior
                        ).any { it.isNotBlank() }
                    }
                    .toList()
            } ?: emptyList()

            workbook.close()
            return ParsedWorkbook(buildings = buildingRows, rooms = roomRows, errors = errors)
        }
    }

    private fun getString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                val dbl = cell.numericCellValue
                if (dbl % 1.0 == 0.0) dbl.toLong().toString() else dbl.toString()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> try {
                cell.stringCellValue.trim()
            } catch (_: Exception) {
                // fallback attempt
                try {
                    val dbl = cell.numericCellValue
                    if (dbl % 1.0 == 0.0) dbl.toLong().toString() else dbl.toString()
                } catch (_: Exception) {
                    ""
                }
            }
            else -> ""
        }
    }

    // Allow iterating rows
    private fun org.apache.poi.ss.usermodel.Sheet.asSequence(): Sequence<Row> =
        sequence {
            val last = lastRowNum
            for (i in 0..last) {
                getRow(i)?.let { yield(it) }
            }
        }
}