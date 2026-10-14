package com.example.export

import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.domain.time.DayTimeReport
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxWorkbookWriter {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatMinutes(millis: Long): String {
        val totalMinutes = millis / 60000.0
        return String.format("%.1f", totalMinutes)
    }

    private fun formatHours(millis: Long): String {
        val hours = millis / 3600000.0
        return String.format("%.2f", hours)
    }

    fun writeWorkbook(
        out: OutputStream,
        dailyReports: List<DayTimeReport>,
        sessions: List<ActivitySessionEntity>,
        tasks: List<TimetableTaskEntity>,
        focusRuns: List<FocusRunEntity>,
        reminders: List<ReminderEntity>
    ) {
        val zos = ZipOutputStream(out)

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet6.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet7.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet8.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
        zos.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        zos.write(relsXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet6.xml"/>
  <Relationship Id="rId7" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet7.xml"/>
  <Relationship Id="rId8" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet8.xml"/>
  <Relationship Id="rId9" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
        zos.write(wbRelsXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        // 4. xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Daily Summary" sheetId="1" r:id="rId1"/>
    <sheet name="Weekly Summary" sheetId="2" r:id="rId2"/>
    <sheet name="Monthly Summary" sheetId="3" r:id="rId3"/>
    <sheet name="Activity Breakdown" sheetId="4" r:id="rId4"/>
    <sheet name="Activity Sessions" sheetId="5" r:id="rId5"/>
    <sheet name="Timetable &amp; Planned" sheetId="6" r:id="rId6"/>
    <sheet name="Focus Sessions" sheetId="7" r:id="rId7"/>
    <sheet name="Reminder History" sheetId="8" r:id="rId8"/>
  </sheets>
</workbook>"""
        zos.write(workbookXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        // 5. xl/styles.xml (Professional fonts, bold headers with gray-blue fill, borders)
        zos.putNextEntry(ZipEntry("xl/styles.xml"))
        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF1E293B"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE2E8F0"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="medium"><color rgb="FF94A3B8"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>"""
        zos.write(stylesXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        // 6. Sheets 1 to 8
        writeSheet(zos, "xl/worksheets/sheet1.xml", buildDailySummarySheet(dailyReports))
        writeSheet(zos, "xl/worksheets/sheet2.xml", buildWeeklySummarySheet(dailyReports))
        writeSheet(zos, "xl/worksheets/sheet3.xml", buildMonthlySummarySheet(dailyReports))
        writeSheet(zos, "xl/worksheets/sheet4.xml", buildActivityBreakdownSheet(dailyReports))
        writeSheet(zos, "xl/worksheets/sheet5.xml", buildActivitySessionsSheet(sessions))
        writeSheet(zos, "xl/worksheets/sheet6.xml", buildTimetableSheet(tasks, dailyReports))
        writeSheet(zos, "xl/worksheets/sheet7.xml", buildFocusSessionsSheet(focusRuns))
        writeSheet(zos, "xl/worksheets/sheet8.xml", buildReminderHistorySheet(reminders))

        zos.finish()
        zos.flush()
    }

    private fun writeSheet(zos: ZipOutputStream, entryName: String, sheetXml: String) {
        zos.putNextEntry(ZipEntry(entryName))
        zos.write(sheetXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildSheetEnvelope(columnWidths: List<Double>, rowsXml: String): String {
        val colsBuilder = StringBuilder("<cols>")
        columnWidths.forEachIndexed { index, width ->
            colsBuilder.append("""<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>""")
        }
        colsBuilder.append("</cols>")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0">
      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  $colsBuilder
  <sheetData>
    $rowsXml
  </sheetData>
</worksheet>"""
    }

    private fun buildDailySummarySheet(reports: List<DayTimeReport>): String {
        val rows = StringBuilder()
        // Header
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Date</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Tracked (Hours)</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Break (Hours)</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Untracked (Hours)</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Planned (Hours)</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Actual Linked (Hours)</t></is></c>
          <c r="G1" t="inlineStr" s="1"><is><t>Completeness (%)</t></is></c>
        </row>""")

        var rowIdx = 2
        for (r in reports) {
            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx" t="inlineStr"><is><t>${r.dateStr}</t></is></c>
              <c r="B$rowIdx"><v>${formatHours(r.trackedMillis)}</v></c>
              <c r="C$rowIdx"><v>${formatHours(r.breakMillis)}</v></c>
              <c r="D$rowIdx"><v>${formatHours(r.untrackedMillis)}</v></c>
              <c r="E$rowIdx"><v>${formatHours(r.plannedMillis)}</v></c>
              <c r="F$rowIdx"><v>${formatHours(r.actualLinkedPlannedMillis)}</v></c>
              <c r="G$rowIdx"><v>${String.format("%.1f", r.trackingCompletenessPercent)}</v></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(15.0, 16.0, 15.0, 18.0, 16.0, 20.0, 18.0), rows.toString())
    }

    private fun buildWeeklySummarySheet(reports: List<DayTimeReport>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Week Window</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Total Tracked (Hours)</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Daily Average (Hours)</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Total Break (Hours)</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Total Untracked (Hours)</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Planned vs Actual (Hours)</t></is></c>
        </row>""")

        val chunks = reports.chunked(7)
        var rowIdx = 2
        for (chunk in chunks) {
            val start = chunk.first().dateStr
            val end = chunk.last().dateStr
            val tracked = chunk.sumOf { it.trackedMillis }
            val breaks = chunk.sumOf { it.breakMillis }
            val untracked = chunk.sumOf { it.untrackedMillis }
            val planned = chunk.sumOf { it.plannedMillis }
            val actual = chunk.sumOf { it.actualLinkedPlannedMillis }
            val avgTracked = if (chunk.isNotEmpty()) tracked / chunk.size else 0L

            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx" t="inlineStr"><is><t>$start to $end</t></is></c>
              <c r="B$rowIdx"><v>${formatHours(tracked)}</v></c>
              <c r="C$rowIdx"><v>${formatHours(avgTracked)}</v></c>
              <c r="D$rowIdx"><v>${formatHours(breaks)}</v></c>
              <c r="E$rowIdx"><v>${formatHours(untracked)}</v></c>
              <c r="F$rowIdx" t="inlineStr"><is><t>${formatHours(actual)} / ${formatHours(planned)}</t></is></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(24.0, 20.0, 20.0, 18.0, 20.0, 24.0), rows.toString())
    }

    private fun buildMonthlySummarySheet(reports: List<DayTimeReport>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Month</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Days Evaluated</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Total Tracked (Hours)</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Daily Average (Hours)</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Total Untracked (Hours)</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Avg Completeness (%)</t></is></c>
        </row>""")

        val byMonth = reports.groupBy { it.dateStr.take(7) }
        var rowIdx = 2
        for ((month, list) in byMonth) {
            val tracked = list.sumOf { it.trackedMillis }
            val untracked = list.sumOf { it.untrackedMillis }
            val avgTracked = if (list.isNotEmpty()) tracked / list.size else 0L
            val avgCompleteness = if (list.isNotEmpty()) list.map { it.trackingCompletenessPercent }.average() else 0.0

            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx" t="inlineStr"><is><t>$month</t></is></c>
              <c r="B$rowIdx"><v>${list.size}</v></c>
              <c r="C$rowIdx"><v>${formatHours(tracked)}</v></c>
              <c r="D$rowIdx"><v>${formatHours(avgTracked)}</v></c>
              <c r="E$rowIdx"><v>${formatHours(untracked)}</v></c>
              <c r="F$rowIdx"><v>${String.format("%.1f", avgCompleteness)}</v></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(16.0, 16.0, 20.0, 20.0, 22.0, 20.0), rows.toString())
    }

    private fun buildActivityBreakdownSheet(reports: List<DayTimeReport>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Activity Name</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Category</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Total Hours</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Total Minutes</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Session Count</t></is></c>
        </row>""")

        val activityMap = mutableMapOf<String, Triple<String, Long, Int>>()
        for (r in reports) {
            for (b in r.activityBreakdowns) {
                val current = activityMap[b.activityName] ?: Triple(b.category, 0L, 0)
                activityMap[b.activityName] = Triple(
                    b.category,
                    current.second + b.durationMillis,
                    current.third + b.sessionCount
                )
            }
        }

        var rowIdx = 2
        for ((name, info) in activityMap.toList().sortedByDescending { it.second.second }) {
            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx" t="inlineStr"><is><t>${escapeXml(name)}</t></is></c>
              <c r="B$rowIdx" t="inlineStr"><is><t>${escapeXml(info.first)}</t></is></c>
              <c r="C$rowIdx"><v>${formatHours(info.second)}</v></c>
              <c r="D$rowIdx"><v>${formatMinutes(info.second)}</v></c>
              <c r="E$rowIdx"><v>${info.third}</v></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(22.0, 18.0, 16.0, 16.0, 15.0), rows.toString())
    }

    private fun buildActivitySessionsSheet(sessions: List<ActivitySessionEntity>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Session ID</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Activity Name</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Category</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Start Time</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>End Time</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Duration (Minutes)</t></is></c>
          <c r="G1" t="inlineStr" s="1"><is><t>Duration (Hours)</t></is></c>
          <c r="H1" t="inlineStr" s="1"><is><t>Is Break</t></is></c>
          <c r="I1" t="inlineStr" s="1"><is><t>Notes</t></is></c>
        </row>""")

        var rowIdx = 2
        for (s in sessions) {
            val startStr = dateTimeFormatter.format(Instant.ofEpochMilli(s.startTime))
            val endStr = if (s.endTime > 0) dateTimeFormatter.format(Instant.ofEpochMilli(s.endTime)) else "Active"
            val durationMillis = if (s.endTime > 0) s.endTime - s.startTime else (System.currentTimeMillis() - s.startTime)

            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx"><v>${s.id}</v></c>
              <c r="B$rowIdx" t="inlineStr"><is><t>${escapeXml(s.activityNameSnapshot)}</t></is></c>
              <c r="C$rowIdx" t="inlineStr"><is><t>${escapeXml(s.categorySnapshot)}</t></is></c>
              <c r="D$rowIdx" t="inlineStr"><is><t>$startStr</t></is></c>
              <c r="E$rowIdx" t="inlineStr"><is><t>$endStr</t></is></c>
              <c r="F$rowIdx"><v>${formatMinutes(durationMillis)}</v></c>
              <c r="G$rowIdx"><v>${formatHours(durationMillis)}</v></c>
              <c r="H$rowIdx" t="inlineStr"><is><t>${if (s.isBreak) "Yes" else "No"}</t></is></c>
              <c r="I$rowIdx" t="inlineStr"><is><t>${escapeXml(s.notes)}</t></is></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(12.0, 22.0, 18.0, 22.0, 22.0, 18.0, 18.0, 12.0, 30.0), rows.toString())
    }

    private fun buildTimetableSheet(tasks: List<TimetableTaskEntity>, reports: List<DayTimeReport>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Date</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Time Slot</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Task Title</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Category</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Planned (Minutes)</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Status</t></is></c>
        </row>""")

        var rowIdx = 2
        for (t in tasks.sortedBy { it.dateStr + "_" + it.slotIndex }) {
            val hour = t.slotIndex / 2
            val min = (t.slotIndex % 2) * 30
            val timeSlotStr = String.format("%02d:%02d", hour, min)

            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx" t="inlineStr"><is><t>${t.dateStr}</t></is></c>
              <c r="B$rowIdx" t="inlineStr"><is><t>$timeSlotStr</t></is></c>
              <c r="C$rowIdx" t="inlineStr"><is><t>${escapeXml(t.title)}</t></is></c>
              <c r="D$rowIdx" t="inlineStr"><is><t>${escapeXml(t.category)}</t></is></c>
              <c r="E$rowIdx"><v>${t.durationMinutes}</v></c>
              <c r="F$rowIdx" t="inlineStr"><is><t>${if (t.isCompleted) "Completed" else "Planned"}</t></is></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(15.0, 14.0, 28.0, 18.0, 18.0, 14.0), rows.toString())
    }

    private fun buildFocusSessionsSheet(focusRuns: List<FocusRunEntity>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Run ID</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Linked Activity</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Start Time</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>End Time</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Cycles Completed</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Focus Completed (Minutes)</t></is></c>
          <c r="G1" t="inlineStr" s="1"><is><t>Break Completed (Minutes)</t></is></c>
          <c r="H1" t="inlineStr" s="1"><is><t>Status</t></is></c>
        </row>""")

        var rowIdx = 2
        for (r in focusRuns) {
            val startStr = dateTimeFormatter.format(Instant.ofEpochMilli(r.startTime))
            val endStr = if (r.endTime != null && r.endTime > 0) dateTimeFormatter.format(Instant.ofEpochMilli(r.endTime)) else "Active"

            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx"><v>${r.id}</v></c>
              <c r="B$rowIdx" t="inlineStr"><is><t>${escapeXml(r.linkedActivityName ?: "None")}</t></is></c>
              <c r="C$rowIdx" t="inlineStr"><is><t>$startStr</t></is></c>
              <c r="D$rowIdx" t="inlineStr"><is><t>$endStr</t></is></c>
              <c r="E$rowIdx"><v>${r.cyclesCompleted}</v></c>
              <c r="F$rowIdx"><v>${String.format("%.1f", r.totalFocusSecondsCompleted / 60.0)}</v></c>
              <c r="G$rowIdx"><v>${String.format("%.1f", r.totalBreakSecondsCompleted / 60.0)}</v></c>
              <c r="H$rowIdx" t="inlineStr"><is><t>${if (r.isFinished) "Finished" else "Running"}</t></is></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(10.0, 22.0, 22.0, 22.0, 18.0, 24.0, 24.0, 14.0), rows.toString())
    }

    private fun buildReminderHistorySheet(reminders: List<ReminderEntity>): String {
        val rows = StringBuilder()
        rows.append("""<row r="1">
          <c r="A1" t="inlineStr" s="1"><is><t>Reminder ID</t></is></c>
          <c r="B1" t="inlineStr" s="1"><is><t>Title</t></is></c>
          <c r="C1" t="inlineStr" s="1"><is><t>Description</t></is></c>
          <c r="D1" t="inlineStr" s="1"><is><t>Scheduled Time</t></is></c>
          <c r="E1" t="inlineStr" s="1"><is><t>Sound</t></is></c>
          <c r="F1" t="inlineStr" s="1"><is><t>Vibration</t></is></c>
          <c r="G1" t="inlineStr" s="1"><is><t>Snooze Count</t></is></c>
          <c r="H1" t="inlineStr" s="1"><is><t>Status</t></is></c>
        </row>""")

        var rowIdx = 2
        for (rem in reminders) {
            val schedStr = dateTimeFormatter.format(Instant.ofEpochMilli(rem.triggerTime))
            val status = when {
                rem.isCompleted -> "Completed"
                rem.isCancelled -> "Cancelled"
                rem.triggerTime < System.currentTimeMillis() -> "Expired"
                else -> "Scheduled"
            }

            rows.append("""<row r="$rowIdx">
              <c r="A$rowIdx"><v>${rem.id}</v></c>
              <c r="B$rowIdx" t="inlineStr"><is><t>${escapeXml(rem.title)}</t></is></c>
              <c r="C$rowIdx" t="inlineStr"><is><t>${escapeXml(rem.description)}</t></is></c>
              <c r="D$rowIdx" t="inlineStr"><is><t>$schedStr</t></is></c>
              <c r="E$rowIdx" t="inlineStr"><is><t>${if (rem.isSoundEnabled) "Yes" else "No"}</t></is></c>
              <c r="F$rowIdx" t="inlineStr"><is><t>${if (rem.isVibrationEnabled) "Yes" else "No"}</t></is></c>
              <c r="G$rowIdx"><v>${rem.snoozeCount}</v></c>
              <c r="H$rowIdx" t="inlineStr"><is><t>$status</t></is></c>
            </row>""")
            rowIdx++
        }

        return buildSheetEnvelope(listOf(12.0, 24.0, 28.0, 22.0, 10.0, 12.0, 14.0, 15.0), rows.toString())
    }
}
