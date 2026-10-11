package com.example.configtool.presentation.gui.table;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.example.configtool.domain.validation.ValidationIssue;

/** Read-only Swing table projection of structured {@link ValidationIssue} values. */
public final class ValidationTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Severity", "Code", "Location", "Explanation", "Suggested action"};
    private List<ValidationIssue> issues = List.of();

    /** @param issues complete issue list to display */
    public void setIssues(List<ValidationIssue> issues) {
        this.issues = List.copyOf(issues);
        fireTableDataChanged();
    }

    /** @return validation issue represented by the model row */
    public ValidationIssue issueAt(int modelRow) {
        return issues.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return issues.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ValidationIssue issue = issues.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> issue.severity().name();
            case 1 -> issue.code();
            case 2 -> issue.location();
            case 3 -> issue.message();
            case 4 -> issue.suggestion();
            default -> throw new IndexOutOfBoundsException("Unknown column " + columnIndex);
        };
    }
}
