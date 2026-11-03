package com.balansync;

import java.io.IOException;
import java.util.Locale;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.beans.property.ReadOnlyStringWrapper;

public class MainController {
    @FXML
    private Label messageLabel;

    @FXML
    private HBox adjustmentSectionTabs;

    @FXML
    private StackPane adjustmentSectionContent;

    @FXML
    private TextField adjustmentAccountSearchField;

    @FXML
    private TableView<AdjustmentEntry> adjustingEntriesTable;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentDateColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentAccountColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentDescriptionColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentDebitColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentCreditColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentActionsColumn;

    @FXML
    private TextField adjustmentsSearchField;

    @FXML
    private TableView<AdjustmentEntry> adjustmentsTable;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentsDateColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentsAccountColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentsDescriptionColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentsDebitColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentsCreditColumn;

    @FXML
    private TableColumn<AdjustmentEntry, String> adjustmentsActionsColumn;

    @FXML
    private TextField worksheetSearchField;

    @FXML
    private TableView<Object> worksheetTable;

    @FXML
    private TextField unadjustedTrialBalanceSearchField;

    @FXML
    private TableView<Object> unadjustedTrialBalanceTable;

    @FXML
    private TextField adjustedTrialBalanceSearchField;

    @FXML
    private TableView<Object> adjustedTrialBalanceTable;

    @FXML
    private TextField closingEntriesSearchField;

    @FXML
    private TableView<Object> closingEntriesTable;

    @FXML
    private TextField balanceSheetSearchField;

    @FXML
    private TableView<Object> balanceSheetTable;

    private final ObservableList<AdjustmentEntry> adjustmentEntries = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (adjustmentAccountSearchField != null || adjustingEntriesTable != null) {
            configureAdjustmentSearch(
                    adjustmentAccountSearchField,
                    adjustingEntriesTable,
                    adjustmentDateColumn,
                    adjustmentAccountColumn,
                    adjustmentDescriptionColumn,
                    adjustmentDebitColumn,
                    adjustmentCreditColumn,
                    adjustmentActionsColumn);
        }
        if (adjustmentsSearchField != null || adjustmentsTable != null) {
            configureAdjustmentSearch(
                    adjustmentsSearchField,
                    adjustmentsTable,
                    adjustmentsDateColumn,
                    adjustmentsAccountColumn,
                    adjustmentsDescriptionColumn,
                    adjustmentsDebitColumn,
                    adjustmentsCreditColumn,
                    adjustmentsActionsColumn);
        }
        configureTableSearch(worksheetSearchField, worksheetTable);
        configureTableSearch(unadjustedTrialBalanceSearchField, unadjustedTrialBalanceTable);
        configureTableSearch(adjustedTrialBalanceSearchField, adjustedTrialBalanceTable);
        configureTableSearch(closingEntriesSearchField, closingEntriesTable);
        configureTableSearch(balanceSheetSearchField, balanceSheetTable);
    }

    private void configureTableSearch(TextField searchField, TableView<Object> table) {
        if (searchField == null && table == null) {
            return;
        }
        if (searchField == null || table == null) {
            throw new IllegalStateException("Adjustment workflow search controls are not fully defined.");
        }

        FilteredList<Object> filteredItems = new FilteredList<>(table.getItems());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filteredItems.setPredicate(item -> query.isEmpty()
                    || table.getColumns().stream().anyMatch(column -> {
                        Object value = column.getCellData(item);
                        return value != null && value.toString().toLowerCase(Locale.ROOT).contains(query);
                    }));
        });
        table.setItems(filteredItems);
    }

    private void configureAdjustmentSearch(
            TextField searchField,
            TableView<AdjustmentEntry> table,
            TableColumn<AdjustmentEntry, String> dateColumn,
            TableColumn<AdjustmentEntry, String> accountColumn,
            TableColumn<AdjustmentEntry, String> descriptionColumn,
            TableColumn<AdjustmentEntry, String> debitColumn,
            TableColumn<AdjustmentEntry, String> creditColumn,
            TableColumn<AdjustmentEntry, String> actionsColumn) {
        if (searchField == null || table == null || dateColumn == null || accountColumn == null
                || descriptionColumn == null || debitColumn == null || creditColumn == null
                || actionsColumn == null) {
            throw new IllegalStateException("Adjustment search controls are not fully defined.");
        }

        dateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().date()));
        accountColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().account()));
        descriptionColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().description()));
        debitColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().debit()));
        creditColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().credit()));
        actionsColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().actions()));

        FilteredList<AdjustmentEntry> filteredEntries = new FilteredList<>(adjustmentEntries);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filteredEntries.setPredicate(entry -> query.isEmpty()
                    || entry.account().toLowerCase(Locale.ROOT).contains(query));
        });
        table.setItems(filteredEntries);
    }

    @FXML
    private void selectAdjustmentSection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedTab)) {
            throw new IllegalStateException("Adjustment section action must come from a button.");
        }
        if (adjustmentSectionTabs == null || adjustmentSectionContent == null) {
            throw new IllegalStateException("Adjustment section controls are not initialized.");
        }

        String sectionId = switch (selectedTab.getId()) {
            case "adjustingEntriesTab" -> "adjustingEntriesSection";
            case "unadjustedTrialBalanceTab" -> "unadjustedTrialBalanceSection";
            case "balanceSheetTab" -> "balanceSheetSection";
            case "adjustmentsTab" -> "adjustmentsSection";
            case "adjustedTrialBalanceTab" -> "adjustedTrialBalanceSection";
            case "worksheetTab" -> "worksheetSection";
            case "closingEntriesTab" -> "closingEntriesSection";
            default -> throw new IllegalArgumentException("Unknown adjustment section: " + selectedTab.getId());
        };

        Node selectedSection = adjustmentSectionContent.getChildren().stream()
                .filter(section -> sectionId.equals(section.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Adjustment section not found: " + sectionId));

        for (Node section : adjustmentSectionContent.getChildren()) {
            boolean active = section == selectedSection;
            section.setVisible(active);
            section.setManaged(active);
        }
        for (Node tab : adjustmentSectionTabs.getChildren()) {
            if (tab instanceof Button button) {
                boolean active = button == selectedTab;
                button.setStyle(active
                        ? "-fx-background-color: #176B45; -fx-text-fill: white; -fx-background-radius: 7; -fx-font-weight: bold; -fx-cursor: hand;"
                        : "-fx-background-color: #F1FAF6; -fx-text-fill: #006B5B; -fx-background-radius: 7; -fx-cursor: hand;");
            }
        }
    }

    @FXML
    private void showDashboard(ActionEvent event) throws IOException {
        showView(event, "dashboard-view.fxml");
    }

    @FXML
    private void showJournalEntries(ActionEvent event) throws IOException {
        showView(event, "journal-entry.fxml");
    }

    @FXML
    private void showGeneralLedger(ActionEvent event) throws IOException {
        showView(event, "general-ledgers.fxml");
    }

    @FXML
    private void showAdjustments(ActionEvent event) throws IOException {
        showView(event, "adjustments.fxml");
    }

    @FXML
    private void showChartOfAccounts(ActionEvent event) throws IOException {
        showView(event, "chart-of-accounts.fxml");
    }

    @FXML
    private void showReports(ActionEvent event) throws IOException {
        showView(event, "reports.fxml");
    }

    @FXML
    private void showBalanceSheet(ActionEvent event) throws IOException {
        FXMLLoader loader = showView(event, "adjustments.fxml");
        Button balanceSheetTab = (Button) loader.getNamespace().get("balanceSheetTab");
        if (balanceSheetTab == null) {
            throw new IllegalStateException("Adjustments view must define the Balance Sheet tab.");
        }
        balanceSheetTab.fire();
    }

    @FXML
    private void showSettings(ActionEvent event) throws IOException {
        showView(event, "settings.fxml");
    }

    private FXMLLoader showView(ActionEvent event, String viewName) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(viewName));
        Parent view = loader.load();
        Node source = (Node) event.getSource();
        source.getScene().setRoot(view);
        if ("dashboard-view.fxml".equals(viewName)) {
            App.configureResponsiveDashboard(loader);
        } else {
            App.configureSidebarNavigation(loader, viewName);
        }
        return loader;
    }

    private record AdjustmentEntry(
            String date,
            String account,
            String description,
            String debit,
            String credit,
            String actions) {
    }
}
