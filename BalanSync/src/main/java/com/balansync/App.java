package com.balansync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    private static final double COMPACT_WIDTH = 760.0;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("dashboard-view.fxml"));
        BorderPane root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("BalanSync");

        configureResponsiveDashboard(loader);
        stage.show();
    }

    static void configureResponsiveDashboard(FXMLLoader loader) {
        BorderPane root = (BorderPane) loader.getRoot();
        VBox sidebar = requireNode(loader, "sidebar", VBox.class);
        Label accountName = requireNode(loader, "accountName", Label.class);
        GridPane summaryCards = requireNode(loader, "summaryCards", GridPane.class);
        FlowPane analyticsLayout = requireNode(loader, "analyticsLayout", FlowPane.class);
        GridPane transactionLayout = requireNode(loader, "transactionLayout", GridPane.class);
        Node greetingIllustration = requireNode(loader, "greetingIllustration", Node.class);
        BarChart<String, Number> incomeExpenseChart = requireNode(loader, "incomeExpenseChart", BarChart.class);
        XYChart.Series<String, Number> incomeExpenseSeries = new XYChart.Series<>();
        incomeExpenseSeries.getData().addAll(
                new XYChart.Data<>("Income", 0.0),
                new XYChart.Data<>("Expenses", 0.0));
        incomeExpenseChart.getData().setAll(incomeExpenseSeries);
        Map<Button, String> navigationLabels = new LinkedHashMap<>();
        collectNavigationButtons(sidebar, navigationLabels);
        setActiveNavigation(loader, "dashboardButton");
        root.widthProperty();
        root.widthProperty()
                .addListener((observable, oldWidth, newWidth) -> updateDashboardCompactContent(
                        newWidth.doubleValue(), greetingIllustration, accountName));
        summaryCards.widthProperty().addListener(
                (observable, oldWidth, newWidth) -> layoutSummaryCards(summaryCards, newWidth.doubleValue()));
        analyticsLayout.widthProperty().addListener(
                (observable, oldWidth, newWidth) -> resizeAnalyticsPanels(analyticsLayout, newWidth.doubleValue()));
        transactionLayout.widthProperty().addListener(
                (observable, oldWidth, newWidth) -> layoutTransactionPanels(transactionLayout, newWidth.doubleValue()));

        updateDashboardCompactContent(root.getWidth(), greetingIllustration, accountName);
        layoutSummaryCards(summaryCards, summaryCards.getWidth());
        resizeAnalyticsPanels(analyticsLayout, analyticsLayout.getWidth());
        layoutTransactionPanels(transactionLayout, transactionLayout.getWidth());
    }

    static void configureResponsiveAccountingScreen(FXMLLoader loader, String viewName) {
        if (!(loader.getRoot() instanceof BorderPane root)) {
            return;
        }

        Node left = root.getLeft();
        if (!(left instanceof VBox sidebar)) {
            return;
        }

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setPrefWidth(220.0);
        sidebarScroll.setMinWidth(0.0);
        sidebarScroll.setStyle("-fx-background: #123D2D; -fx-background-color: #123D2D;");
        sidebar.setMinWidth(0.0);
        sidebar.setMaxWidth(Double.MAX_VALUE);
        root.setLeft(sidebarScroll);

        HBox brandHeader = null;
        VBox brandDetails = null;
        StackPane brandLogo = null;
        if (!sidebar.getChildren().isEmpty() && sidebar.getChildren().get(0) instanceof HBox header) {
            brandHeader = header;
            for (Node child : header.getChildren()) {
                if (child instanceof VBox details) {
                    brandDetails = details;
                } else if (child instanceof StackPane logo) {
                    brandLogo = logo;
                }
            }
        }

        Map<Button, String> navigationLabels = new LinkedHashMap<>();
        collectNavigationButtons(sidebar, navigationLabels);
        setActiveNavigation(loader, navigationButtonId(viewName));
        Map<Button, Node> navigationGraphics = new LinkedHashMap<>();
        Map<Button, Node> compactGraphics = new LinkedHashMap<>();
        navigationLabels.forEach((button, label) -> {
            navigationGraphics.put(button, button.getGraphic());
            if (button.getGraphic() == null && !label.isEmpty()) {
                Label compactIcon = new Label(label.substring(0, 1));
                compactIcon.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                compactGraphics.put(button, compactIcon);
            }
            button.setAccessibleText(label.trim());
            button.setTooltip(new Tooltip(label.trim()));
        });

        Node center = root.getCenter();
        VBox content = null;
        ScrollPane contentScroll = null;
        if (center instanceof VBox centerContent) {
            content = centerContent;
            contentScroll = new ScrollPane(content);
            contentScroll.setFitToWidth(true);
            contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            contentScroll.setStyle("-fx-background: #F8F7FC; -fx-background-color: #F8F7FC;");
            root.setCenter(contentScroll);
        } else if (center instanceof ScrollPane scrollPane
                && scrollPane.getContent() instanceof VBox centerContent) {
            content = centerContent;
            contentScroll = scrollPane;
            contentScroll.setFitToWidth(true);
            contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }

        if (content == null || contentScroll == null) {
            return;
        }

        content.setMinWidth(0.0);
        content.setPrefWidth(Region.USE_COMPUTED_SIZE);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMinHeight(0.0);
        content.setPrefHeight(Region.USE_COMPUTED_SIZE);
        content.setMaxHeight(Double.MAX_VALUE);
        List<ResponsiveBody> responsiveBodies = new java.util.ArrayList<>();
        for (int index = 0; index < content.getChildren().size(); index++) {
            Node child = content.getChildren().get(index);
            if (child instanceof Region region) {
                region.setMinWidth(0.0);
                region.setPrefWidth(Region.USE_COMPUTED_SIZE);
                region.setMaxWidth(Double.MAX_VALUE);
            }
            if (child instanceof GridPane grid) {
                grid.getColumnConstraints().clear();
                grid.getRowConstraints().clear();
                grid.widthProperty().addListener(
                        (observable, oldWidth, newWidth) -> layoutResponsiveCards(grid, newWidth.doubleValue()));
                layoutResponsiveCards(grid, grid.getWidth());
            } else if (child instanceof HBox header && header.getChildren().size() >= 3
                    && header.getChildren().get(2) instanceof StackPane decoration) {
                VBox title = header.getChildren().stream()
                        .filter(VBox.class::isInstance)
                        .map(VBox.class::cast)
                        .findFirst()
                        .orElse(null);
                if (title != null) {
                    title.setMinWidth(0.0);
                    title.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(title, Priority.ALWAYS);
                }
                header.widthProperty().addListener((observable, oldWidth, newWidth) -> {
                    boolean compactHeader = newWidth.doubleValue() < 620.0;
                    decoration.setVisible(!compactHeader);
                    decoration.setManaged(!compactHeader);
                });
            }

            if (child instanceof HBox horizontal && horizontal.getPrefHeight() >= 350.0
                    && horizontal.getChildren().size() >= 2) {
                responsiveBodies.add(createResponsiveBody(content, index, horizontal));
            }
        }

        ScrollPane finalContentScroll = contentScroll;
        VBox finalContent = content;
        VBox finalBrandDetails = brandDetails;
        HBox finalBrandHeader = brandHeader;
        StackPane finalBrandLogo = brandLogo;
        content.widthProperty()
                .addListener((observable, oldWidth, newWidth) -> updateResponsiveAccountingLayout(root.getWidth(),
                        newWidth.doubleValue(),
                        finalContentScroll, sidebarScroll, finalBrandHeader, finalBrandDetails,
                        finalBrandLogo, navigationLabels, navigationGraphics, compactGraphics,
                        responsiveBodies));
        root.widthProperty()
                .addListener((observable, oldWidth, newWidth) -> updateResponsiveAccountingLayout(
                        newWidth.doubleValue(), finalContent.getWidth(),
                        finalContentScroll, sidebarScroll, finalBrandHeader, finalBrandDetails,
                        finalBrandLogo, navigationLabels, navigationGraphics, compactGraphics,
                        responsiveBodies));
        updateResponsiveAccountingLayout(root.getWidth(), content.getWidth(), contentScroll,
                sidebarScroll, brandHeader, brandDetails, brandLogo, navigationLabels,
                navigationGraphics, compactGraphics, responsiveBodies);
    }

    static void configureSidebarNavigation(FXMLLoader loader, String viewName) {
        setActiveNavigation(loader, navigationButtonId(viewName));
    }

    private static ResponsiveBody createResponsiveBody(VBox content, int index, HBox horizontal) {
        List<Node> panels = List.copyOf(horizontal.getChildren());
        VBox vertical = new VBox(horizontal.getSpacing());
        vertical.setFillWidth(true);
        vertical.setPadding(horizontal.getPadding());
        vertical.setStyle(horizontal.getStyle());
        vertical.setMinWidth(0.0);
        vertical.setMaxWidth(Double.MAX_VALUE);
        panels.forEach(panel -> {
            if (panel instanceof Region region) {
                region.setMinWidth(0.0);
                region.setPrefWidth(Region.USE_COMPUTED_SIZE);
                region.setMaxWidth(Double.MAX_VALUE);
                VBox.setVgrow(region, Priority.SOMETIMES);
            }
        });

        StackPane switcher = new StackPane(horizontal, vertical);
        switcher.setMinWidth(0.0);
        switcher.setMaxWidth(Double.MAX_VALUE);
        switcher.setPrefWidth(Region.USE_COMPUTED_SIZE);
        content.getChildren().set(index, switcher);
        return new ResponsiveBody(horizontal, vertical, panels);
    }

    private static void updateResponsiveAccountingLayout(
            double windowWidth,
            double contentWidth,
            ScrollPane contentScroll,
            ScrollPane sidebarScroll,
            HBox brandHeader,
            VBox brandDetails,
            StackPane brandLogo,
            Map<Button, String> navigationLabels,
            Map<Button, Node> navigationGraphics,
            Map<Button, Node> compactGraphics,
            List<ResponsiveBody> responsiveBodies) {
        if (contentWidth <= 0.0) {
            return;
        }

        boolean compactNavigation = windowWidth < COMPACT_WIDTH;
        double sidebarWidth = compactNavigation ? 74.0 : 220.0;
        sidebarScroll.setPrefWidth(sidebarWidth);
        sidebarScroll.setMinWidth(sidebarWidth);
        sidebarScroll.setMaxWidth(sidebarWidth);
        if (brandDetails != null) {
            brandDetails.setVisible(!compactNavigation);
            brandDetails.setManaged(!compactNavigation);
        }
        if (brandHeader != null) {
            brandHeader.setSpacing(compactNavigation ? 0.0 : 20.0);
            brandHeader.setPrefHeight(compactNavigation ? 56.0 : 100.0);
        }
        if (brandLogo != null) {
            brandLogo.setPrefWidth(compactNavigation ? 44.0 : 62.0);
        }

        navigationLabels.forEach((button, label) -> {
            button.setText(compactNavigation ? "" : label);
            button.setContentDisplay(compactNavigation ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            button.setGraphic(compactNavigation
                    ? compactGraphics.getOrDefault(button, navigationGraphics.get(button))
                    : navigationGraphics.get(button));
            button.setPrefWidth(compactNavigation ? 44.0 : 190.0);
            button.setMaxWidth(compactNavigation ? 44.0 : Double.MAX_VALUE);
            button.setMinWidth(compactNavigation ? 44.0 : 0.0);
        });

        boolean stackPanels = contentWidth < 850.0;
        responsiveBodies.forEach(body -> body.setStacked(stackPanels));
        contentScroll.setFitToWidth(true);
    }

    private static void layoutResponsiveCards(GridPane grid, double width) {
        List<Node> cards = grid.getChildren().stream()
                .filter(VBox.class::isInstance)
                .toList();
        if (width <= 0.0 || cards.isEmpty()) {
            return;
        }

        int columns = width < 520.0 ? 1 : width < 920.0 ? 2 : Math.min(4, cards.size());
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / columns);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setMinWidth(0.0);
            grid.getColumnConstraints().add(constraints);
        }

        int rows = (cards.size() + columns - 1) / columns;
        for (int row = 0; row < rows; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setVgrow(Priority.SOMETIMES);
            grid.getRowConstraints().add(constraints);
        }

        double cardWidth = (width - grid.getHgap() * (columns - 1)) / columns;
        for (int index = 0; index < cards.size(); index++) {
            Region card = (Region) cards.get(index);
            GridPane.setColumnIndex(card, index % columns);
            GridPane.setRowIndex(card, index / columns);
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setVgrow(card, Priority.ALWAYS);
            card.setMinWidth(0.0);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setPrefWidth(cardWidth);
            for (Node child : ((VBox) card).getChildren()) {
                if (child instanceof Label label) {
                    label.setWrapText(true);
                    label.setMinWidth(0.0);
                    label.setMaxWidth(Double.MAX_VALUE);
                } else if (child instanceof HBox heading) {
                    for (Node headingChild : heading.getChildren()) {
                        if (headingChild instanceof Label label) {
                            label.setWrapText(true);
                            label.setMinWidth(0.0);
                            label.setMaxWidth(Double.MAX_VALUE);
                            HBox.setHgrow(label, Priority.ALWAYS);
                        }
                    }
                }
            }
        }
    }

    private static void setActiveNavigation(FXMLLoader loader, String activeButtonId) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; "
                + "-fx-font-size: 15px; -fx-alignment: CENTER_LEFT; -fx-padding: 10; "
                + "-fx-background-radius: 8; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #267A56; -fx-text-fill: white; "
                + "-fx-font-size: 15px; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; "
                + "-fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;";
        for (String buttonId : List.of("dashboardButton", "journalEntriesButton", "generalLedgerButton",
                "adjustmentsButton", "chartOfAccountsButton", "reportsButton", "settingsButton")) {
            Button button = requireNode(loader, buttonId, Button.class);
            button.setStyle(buttonId.equals(activeButtonId) ? activeStyle : normalStyle);
        }
    }

    private static String navigationButtonId(String viewName) {
        return switch (viewName) {
            case "dashboard-view.fxml" -> "dashboardButton";
            case "journal-entry.fxml" -> "journalEntriesButton";
            case "general-ledgers.fxml" -> "generalLedgerButton";
            case "adjustments.fxml" -> "adjustmentsButton";
            case "chart-of-accounts.fxml" -> "chartOfAccountsButton";
            case "reports.fxml" -> "reportsButton";
            case "settings.fxml" -> "settingsButton";
            default -> throw new IllegalArgumentException("Unknown accounting view: " + viewName);
        };
    }

    private static final class ResponsiveBody {
        private final HBox horizontal;
        private final VBox vertical;
        private final List<Node> panels;
        private boolean stacked;

        private ResponsiveBody(HBox horizontal, VBox vertical, List<Node> panels) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.panels = panels;
        }

        private void setStacked(boolean shouldStack) {
            if (stacked == shouldStack) {
                return;
            }
            horizontal.getChildren().clear();
            vertical.getChildren().clear();
            (shouldStack ? vertical.getChildren() : horizontal.getChildren()).setAll(panels);
            horizontal.setVisible(!shouldStack);
            horizontal.setManaged(!shouldStack);
            vertical.setVisible(shouldStack);
            vertical.setManaged(shouldStack);
            stacked = shouldStack;
        }
    }

    private static <T> T requireNode(FXMLLoader loader, String id, Class<T> type) {
        Object node = loader.getNamespace().get(id);
        if (!type.isInstance(node)) {
            node = findNode(loader.getRoot(), id, type);
        }
        if (!type.isInstance(node)) {
            throw new IllegalStateException("Dashboard FXML must define fx:id \"" + id
                    + "\" as a " + type.getSimpleName() + ".");
        }
        return type.cast(node);
    }

    private static <T> T findNode(Node node, String id, Class<T> type) {
        if (type.isInstance(node) && id.equals(node.getId())) {
            return type.cast(node);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T match = findNode(child, id, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void collectNavigationButtons(Parent parent, Map<Button, String> buttons) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button button) {
                String label = button.getText();
                buttons.put(button, label);
                button.setAccessibleText(label);
                button.setTooltip(new Tooltip(label.trim()));
            }
            if (child instanceof Parent nestedParent) {
                collectNavigationButtons(nestedParent, buttons);
            }
        }
    }

    private static void updateDashboardCompactContent(
            double width,
            Node greetingIllustration,
            Label accountName) {
        boolean compact = width < COMPACT_WIDTH;
        greetingIllustration.setVisible(!compact);
        greetingIllustration.setManaged(!compact);
        accountName.setVisible(!compact);
        accountName.setManaged(!compact);
    }

    private static void layoutSummaryCards(GridPane grid, double width) {
        if (width <= 0.0) {
            return;
        }

        int columns = width < 520.0 ? 1 : width < 920.0 ? 2 : 4;
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / columns);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setMinWidth(0.0);
            grid.getColumnConstraints().add(constraints);
        }

        int rows = (grid.getChildren().size() + columns - 1) / columns;
        for (int row = 0; row < rows; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setVgrow(Priority.SOMETIMES);
            grid.getRowConstraints().add(constraints);
        }

        double cardWidth = (width - grid.getHgap() * (columns - 1)) / columns;
        for (int index = 0; index < grid.getChildren().size(); index++) {
            Region card = (Region) grid.getChildren().get(index);
            GridPane.setColumnIndex(card, index % columns);
            GridPane.setRowIndex(card, index / columns);
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setVgrow(card, Priority.ALWAYS);
            card.setMinWidth(0.0);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setPrefWidth(cardWidth);

            if (card instanceof VBox cardContent) {
                for (Node child : cardContent.getChildren()) {
                    if (child instanceof Label label) {
                        label.setWrapText(true);
                        label.setMinWidth(0.0);
                    } else if (child instanceof HBox heading) {
                        for (Node headingChild : heading.getChildren()) {
                            if (headingChild instanceof Label label) {
                                label.setWrapText(true);
                                label.setMinWidth(0.0);
                                label.setMaxWidth(Double.MAX_VALUE);
                                HBox.setHgrow(label, Priority.ALWAYS);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void resizeAnalyticsPanels(FlowPane pane, double width) {
        int itemCount = pane.getChildren().size();
        if (width <= 0.0 || itemCount == 0) {
            return;
        }

        double[] widths = new double[itemCount];
        if (width >= 1000.0 && itemCount == 3) {
            double availableWidth = Math.max(0.0, width - pane.getHgap() * (itemCount - 1));
            widths[0] = availableWidth * 0.504;
            widths[1] = availableWidth * 0.2475;
            widths[2] = availableWidth * 0.2475;
        } else if (width >= COMPACT_WIDTH && itemCount == 3) {
            double pairedWidth = Math.max(0.0, width - pane.getHgap());
            widths[0] = pairedWidth * 0.60;
            widths[1] = pairedWidth * 0.40;
            widths[2] = width;
        } else {
            for (int index = 0; index < itemCount; index++) {
                widths[index] = width;
            }
        }

        for (int index = 0; index < itemCount; index++) {
            Node child = pane.getChildren().get(index);
            if (child instanceof Region region) {
                region.setMinWidth(0.0);
                region.setMaxWidth(Double.MAX_VALUE);
                region.setPrefWidth(widths[index]);
            }
        }
    }

    private static void layoutTransactionPanels(GridPane grid, double width) {
        if (width <= 0.0 || grid.getChildren().size() < 2) {
            return;
        }

        boolean sideBySide = width >= 900.0;
        int columns = sideBySide ? 2 : 1;
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(sideBySide ? (column == 0 ? 70.0 : 30.0) : 100.0);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setMinWidth(0.0);
            grid.getColumnConstraints().add(constraints);
        }

        if (!sideBySide) {
            for (int row = 0; row < grid.getChildren().size(); row++) {
                RowConstraints constraints = new RowConstraints();
                constraints.setVgrow(Priority.SOMETIMES);
                grid.getRowConstraints().add(constraints);
            }
        }

        for (int index = 0; index < grid.getChildren().size(); index++) {
            Node child = grid.getChildren().get(index);
            GridPane.setColumnIndex(child, sideBySide ? index : 0);
            GridPane.setRowIndex(child, sideBySide ? 0 : index);
            GridPane.setHgrow(child, Priority.ALWAYS);
            GridPane.setVgrow(child, Priority.ALWAYS);
            if (child instanceof Region region) {
                region.setMinWidth(0.0);
                region.setMaxWidth(Double.MAX_VALUE);
                region.setPrefWidth(sideBySide
                        ? Math.max(0.0, (width - grid.getHgap()) * (index == 0 ? 0.70 : 0.30))
                        : width);
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}