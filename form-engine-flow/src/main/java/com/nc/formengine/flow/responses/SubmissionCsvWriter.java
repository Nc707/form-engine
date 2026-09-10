package com.nc.formengine.flow.responses;

import java.util.List;

/**
 * Writes rows as RFC 4180 CSV.
 *
 * <p>Two deliberate departures from "join with commas". A cell is quoted whenever it holds a comma,
 * a quote or a newline, and inner quotes are doubled — a free-text answer contains all three sooner
 * or later. And a cell starting with {@code = + - @} is prefixed with an apostrophe, because
 * spreadsheets read those as formulas, which turns someone's answer into code on the reader's
 * machine.
 *
 * <p>The output is UTF-8 with a byte-order mark, which is what makes Excel read a label outside plain
 * ASCII as the label rather than as mojibake.
 */
final class SubmissionCsvWriter {

    static final String BYTE_ORDER_MARK = "﻿";

    /** RFC 4180 says CRLF, and it is what spreadsheets on Windows expect. */
    private static final String LINE_SEPARATOR = "\r\n";

    private static final String FORMULA_STARTERS = "=+-@";

    private SubmissionCsvWriter() {
    }

    static String write(List<String> header, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder(BYTE_ORDER_MARK);
        appendRow(csv, header);
        for (List<String> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(cells.get(i)));
        }
        csv.append(LINE_SEPARATOR);
    }

    private static String escape(String cell) {
        if (cell == null || cell.isEmpty()) {
            return "";
        }

        String safe = FORMULA_STARTERS.indexOf(cell.charAt(0)) >= 0 ? "'" + cell : cell;

        if (safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0
                || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }
}
