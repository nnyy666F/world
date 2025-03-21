package com.mirror.world;

import com.mirror.block.*;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.mirror.block.BlockExecutor.showError;

public class MainGuiCtrl {
    public BorderPane functionArea2;
    public Button runBtn;
    @FXML private SplitPane mainSplit;
    @FXML private SplitPane contentSplit;
    @FXML private ScrollPane codePanel;
    @FXML private StackPane functionArea;
    @FXML private VBox bgPanel;
    @FXML private VBox soundPanel;
    @FXML private VBox blockPalette;
    @FXML private VBox codeBlocks;
    @FXML private ComboBox<String> characterSelector;
    @FXML private TextField nameField;
    @FXML private CheckBox visibleCheckBox;
    @FXML private TextField xCoord;
    @FXML private TextField yCoord;
    @FXML private Slider sizeSlider;
    @FXML private Slider rotationSlider;
    @FXML private Pane stageArea;
    @FXML private CheckBox debugCheckbox;
    @FXML private ListView<String> blockCategories;
    private BlockExecutor currentExecutor;
    private StackPane dropLayer;
    private static final DataFormat BLOCK_DEF_FORMAT = new DataFormat("application/x-blockdef");

    private Text debugInfoText = new Text();
    private AnimationTimer debugTimer;
    private long lastUpdate = 0;
    private int frameCount = 0;
    private double fps = 0;
    private static MainGuiCtrl instance;
    private ObservableList<Character> characters = FXCollections.observableArrayList();
    private ObjectProperty<Character> currentCharacter = new SimpleObjectProperty<>();
    private ObservableList<String> characterNames = FXCollections.observableArrayList();

    private Map<String, List<BlockDef>> blockMap = initBlockMap();

    private Map<String, List<BlockDef>> initBlockMap() {
        Map<String, List<BlockDef>> map = new LinkedHashMap<>();
        List<BlockDef> controlBlocks = new ArrayList<>();
        controlBlocks.add(new BlockDef("当开始被点击", BlockType.EVENT, List.of(), 0));
        controlBlocks.add(new BlockDef("重复执行", BlockType.LOOP, List.of(ParamType.NUMBER), 1));
        map.put("控制", Collections.unmodifiableList(controlBlocks));
        List<BlockDef> motionBlocks = new ArrayList<>();
        motionBlocks.add(new BlockDef("移动", BlockType.COMMAND, List.of(ParamType.NUMBER), 0));
        motionBlocks.add(new BlockDef("右转", BlockType.COMMAND, List.of(ParamType.NUMBER), 0));
        map.put("运动", Collections.unmodifiableList(motionBlocks));
        return Collections.unmodifiableMap(map);
    }
    private final ObservableList<String> categoryOrder = FXCollections.observableArrayList(
            "控制", "运动", "外观", "声音", "逻辑", "运算"
    );
    @FXML
    public void initialize() {
        instance = this;
        codeBlocks.setPickOnBounds(true);
        blockPalette.setPickOnBounds(false);
        codeBlocks.setPickOnBounds(false);
        codePanel.setFitToWidth(true);
        codeBlocks.setStyle("-fx-background-color: transparent;");
        codePanel.setStyle("-fx-background: transparent;");
        initializeBlockSystem();
        updateCharacterNames();
        characterSelector.setItems(characterNames);
        characters.addListener((ListChangeListener<Character>) change -> {
            updateCharacterNames();
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(character ->
                            character.nameProperty().addListener((obs, oldVal, newVal) ->
                                    updateCharacterNames()
                            )
                    );
                }
            }
        });
        characterSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newName) -> {
            Character c = characters.stream()
                    .filter(ch -> ch.getName().equals(newName))
                    .findFirst()
                    .orElse(null);
            currentCharacter.set(c);
        });
        currentCharacter.addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                nameField.textProperty().unbindBidirectional(oldVal.nameProperty());
                visibleCheckBox.selectedProperty().unbindBidirectional(oldVal.visibleProperty());
                sizeSlider.valueProperty().unbindBidirectional(oldVal.sizeProperty());
                rotationSlider.valueProperty().unbindBidirectional(oldVal.rotationProperty());
                if (oldVal.getXPropertyListener() != null) {
                    oldVal.xProperty().removeListener(oldVal.getXPropertyListener());
                }
                if (oldVal.getYPropertyListener() != null) {
                    oldVal.yProperty().removeListener(oldVal.getYPropertyListener());
                }
                if (oldVal.getXTextListener() != null) {
                    xCoord.textProperty().removeListener(oldVal.getXTextListener());
                }
                if (oldVal.getYTextListener() != null) {
                    yCoord.textProperty().removeListener(oldVal.getYTextListener());
                }
            }
            if (newVal != null) {
                nameField.textProperty().bindBidirectional(newVal.nameProperty());
                visibleCheckBox.selectedProperty().bindBidirectional(newVal.visibleProperty());
                sizeSlider.valueProperty().bindBidirectional(newVal.sizeProperty());
                rotationSlider.valueProperty().bindBidirectional(newVal.rotationProperty());
                ChangeListener<Number> xPropListener = (o, oldX, newX) ->
                        xCoord.setText(String.format("%.1f", newX));
                ChangeListener<Number> yPropListener = (o, oldY, newY) ->
                        yCoord.setText(String.format("%.1f", newY));

                ChangeListener<String> xTxtListener = (o, oldT, newT) -> {
                    try {
                        newVal.setX(Double.parseDouble(newT));
                    } catch (NumberFormatException e) {
                        xCoord.setText(oldT);
                    }
                };
                ChangeListener<String> yTxtListener = (o, oldT, newT) -> {
                    try {
                        newVal.setY(Double.parseDouble(newT));
                    } catch (NumberFormatException e) {
                        yCoord.setText(oldT);
                    }
                };
                newVal.setXPropertyListener(xPropListener);
                newVal.setYPropertyListener(yPropListener);
                newVal.setXTextListener(xTxtListener);
                newVal.setYTextListener(yTxtListener);
                newVal.xProperty().addListener(xPropListener);
                newVal.yProperty().addListener(yPropListener);
                xCoord.textProperty().addListener(xTxtListener);
                yCoord.textProperty().addListener(yTxtListener);
                xCoord.setText(String.format("%.1f", newVal.getX()));
                yCoord.setText(String.format("%.1f", newVal.getY()));
            } else {
                xCoord.setText("");
                yCoord.setText("");
            }
        });

        StackPane debugLayer = new StackPane();
        debugLayer.setMouseTransparent(true);
        debugInfoText.setFill(Color.RED);
        debugInfoText.setFont(Font.font(16));
        debugLayer.getChildren().add(debugInfoText);
        StackPane.setAlignment(debugInfoText, Pos.TOP_LEFT);
        StackPane stageWrapper = new StackPane();
        stageWrapper.getChildren().addAll(stageArea, debugLayer);
        contentSplit.getItems().set(0, stageWrapper);
        debugCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            debugInfoText.setVisible(newVal);
            if (newVal) {
                startDebugTimer();
            } else {
                stopDebugTimer();
            }
        });
        codeBlocks.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        codeBlocks.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(BLOCK_DEF_FORMAT)) {
                BlockDef def = (BlockDef) db.getContent(BLOCK_DEF_FORMAT);
                addToCodeArea(def, event.getY());
                event.setDropCompleted(true);
            }
        });
        runBtn.setOnAction(e -> {
            if (currentExecutor != null) {
                currentExecutor.stop();
            }
            Character currentChar = currentCharacter.get();
            if (currentChar == null) {
                Platform.runLater(() -> showError("请先选择角色"));
                return;
            }

            List<BlockNode> blocks = codeBlocks.getChildren().stream()
                    .filter(BlockNode.class::isInstance)
                    .map(BlockNode.class::cast)
                    .collect(Collectors.toList());
            currentExecutor = new BlockExecutor(currentChar);
            runBtn.setDisable(true);
            runBtn.setText("执行中...");
            new Thread(() -> {
                try {
                    currentExecutor.execute(blocks);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("执行错误: " + ex.getMessage()));
                } finally {
                    Platform.runLater(() -> {
                        runBtn.setDisable(false);
                        runBtn.setText("运行");
                    });
                }
            }).start();
        });
        initializeCodeArea();
        dropLayer.setMouseTransparent(false);
    }
    private void initializeCodeArea() {
        codeBlocks.setMinHeight(100);

        VBox mainContainer = new VBox();
        mainContainer.getChildren().add(codeBlocks);
        dropLayer = new StackPane();
        dropLayer.setStyle("-fx-background-color: transparent;");
        dropLayer.prefHeightProperty().bind(codeBlocks.heightProperty());
        dropLayer.setOnDragEntered(event -> {
            if (isValidDrag(event)) {
                dropLayer.setStyle("-fx-background-color: rgba(33,150,243,0.1);");
                event.consume();
            }
        });
        dropLayer.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(BLOCK_DEF_FORMAT)) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
                showInsertPosition(event.getY());
            }
        });
        dropLayer.setOnDragExited(event -> {
            dropLayer.setStyle("-fx-background-color: transparent;");
            clearInsertIndicator();
            event.consume();
        });
        dropLayer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(BLOCK_DEF_FORMAT)) {
                BlockDef def = (BlockDef) db.getContent(BLOCK_DEF_FORMAT);
                addToCodeArea(def, event.getY());
                event.setDropCompleted(true);
                event.consume();
            }
        });
        codeBlocks.getChildren().add(0, dropLayer);

        StackPane layeredPane = new StackPane();
        layeredPane.getChildren().addAll(mainContainer, dropLayer);
        codePanel.setContent(layeredPane);
    }
    private void showInsertPosition(double y) {
        clearInsertIndicator();
        double accumulatedHeight = 0;
        int insertIndex = 0;
        for (Node node : codeBlocks.getChildren()) {
            if (node instanceof BlockNode) {
                Bounds bounds = node.getBoundsInParent();
                if (y < accumulatedHeight + bounds.getHeight()/2) {
                    break;
                }
                accumulatedHeight += bounds.getHeight();
                insertIndex++;
            }
        }
        Line indicator = new Line(0, 0, codeBlocks.getWidth() - 20, 0);
        indicator.setStroke(Color.web("#2196F3"));
        indicator.setStrokeWidth(2);
        indicator.setOpacity(0.8);
        indicator.setTranslateY(accumulatedHeight);
        FadeTransition ft = new FadeTransition(Duration.millis(200), indicator);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        codeBlocks.getChildren().add(indicator);
    }
    private void clearInsertIndicator() {
        codeBlocks.getChildren().removeIf(node -> node instanceof Line);
    }
    private boolean isValidDrag(DragEvent event) {
        return event.getGestureSource() != null &&
                !(event.getGestureSource() instanceof BlockNode) &&
                event.getDragboard().hasString();
    }
    private void addToCodeArea(BlockDef def, double dropY) {
        BlockNode newNode = createBlockNode(def);
        List<Node> existingBlocks = codeBlocks.getChildren().stream()
                .filter(n -> n instanceof BlockNode)
                .toList();
        newNode.setMouseTransparent(false);
        newNode.setFocusTraversable(true);
        newNode.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                codeBlocks.requestFocus();
            }
        });
        int insertIndex = 0;
        double accHeight = 0;
        for (Node node : existingBlocks) {
            double nodeHeight = node.getBoundsInParent().getHeight();
            if (dropY < accHeight + nodeHeight/2) break;
            accHeight += nodeHeight;
            insertIndex++;
        }
        codeBlocks.getChildren().add(insertIndex, newNode);
        Platform.runLater(() -> {
            Bounds bounds = newNode.getBoundsInParent();
            codePanel.setVvalue(bounds.getMaxY() / codeBlocks.getHeight());
        });
    }
    private String lightenColor(String hex) {
        try {
            Color c = Color.web(hex);
            return String.format("#%02X%02X%02X",
                    (int)(c.getRed() * 255 * 0.8),
                    (int)(c.getGreen() * 255 * 0.8),
                    (int)(c.getBlue() * 255 * 0.8)
            );
        } catch (Exception e) {
            return "#EEEEEE";
        }
    }
    private void initializeBlockSystem() {
        blockCategories.setItems(categoryOrder);
        blockCategories.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateBlockPalette(newVal);
        });
        blockCategories.getSelectionModel().selectFirst();
    }
    private final Map<String, String> blockColors = Collections.unmodifiableMap(
            new LinkedHashMap<>() {{
                put("控制", "#2196F3");
                put("运动", "#4CAF50");
                put("外观", "#9C27B0");
                put("声音", "#FF9800");
                put("逻辑", "#E91E63");
                put("运算", "#009688");
            }}
    );
    private void updateBlockPalette(String category) {
        blockPalette.getChildren().clear();
        blockMap.getOrDefault(category, List.of()).forEach(blockDef -> {
            Button btn = new Button(blockDef.getDisplayName());
            btn.setUserData(blockDef);
            String color = blockColors.getOrDefault(category, "#2196F3");
            btn.setStyle("-fx-background-color: " + color + "; "
                    + "-fx-text-fill: white; "
                    + "-fx-font-size: 14px; "
                    + "-fx-padding: 8 15; "
                    + "-fx-background-radius: 5; "
                    + "-fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + lightenColor(color) + "; "
                    + "-fx-text-fill: white; "
                    + "-fx-font-size: 14px; "
                    + "-fx-padding: 8 15; "
                    + "-fx-background-radius: 5;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; "
                    + "-fx-text-fill: white; "
                    + "-fx-font-size: 14px; "
                    + "-fx-padding: 8 15; "
                    + "-fx-background-radius: 5;"));
            blockPalette.getChildren().add(btn);
            btn.setOnDragDetected(event -> {
                Dragboard db = btn.startDragAndDrop(TransferMode.COPY);
                db.setContent(new ClipboardContent() {{
                    put(BLOCK_DEF_FORMAT, blockDef);
                }});
                Text previewText = new Text(blockDef.getDisplayName());
                previewText.setFill(Color.WHITE);
                StackPane dragView = new StackPane(previewText);
                dragView.setStyle("-fx-background-color: " + color + "; -fx-padding: 5 10;");

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                db.setDragView(dragView.snapshot(params, null));

                event.consume();
            });

            codeBlocks.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(BLOCK_DEF_FORMAT)) {
                    BlockDef def = (BlockDef) db.getContent(BLOCK_DEF_FORMAT);
                    addToCodeArea(def, event.getY());
                    event.setDropCompleted(true);
                }
                event.consume();
            });
        });
    }
    private void showBlockEditor(String dragData) {
        try {
            if (dragData == null) {
                throw new IllegalArgumentException("积木数据不能为空");
            }

            String[] parts = dragData.split("::", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("无效的积木格式: " + dragData);
            }

            String originalCategory = parts[0];
            String originalText = parts[1];

            TextInputDialog dialog = new TextInputDialog(originalText);
            dialog.setTitle("编辑积木");
            dialog.setHeaderText("正在修改 [" + originalCategory + "] 类别的积木");
            dialog.setContentText("请输入新名称:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newName -> {
                String newData = originalCategory + "::" + newName;
                updateBlockInPalette(dragData, newData);
                updateBlockPalette(originalCategory);
            });
        } catch (Exception e) {
            handleBlockError("积木编辑", e);
        }
    }
    private void handleBlockError(String operation, Exception e) {
        System.err.println("Block operation error [" + operation + "]: " +
                e.getMessage());
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("积木错误");
            alert.setHeaderText("操作无法完成");
            alert.setContentText("积木数据格式异常，请检查配置\n错误详情: " + e.getMessage());
            alert.showAndWait();
        });
    }
    private void updateBlockInPalette(String oldData, String newData) {
        String[] oldParts = oldData.split("::", 2);
        if (oldParts.length != 2) return;
        Map<String, List<BlockDef>> newBlockMap = new LinkedHashMap<>(blockMap);
        List<BlockDef> categoryBlocks = new ArrayList<>(newBlockMap.get(oldParts[0]));
        BlockDef oldDef = categoryBlocks.stream()
                .filter(b -> b.getDisplayName().equals(oldParts[1]))
                .findFirst()
                .orElse(null);

        if (oldDef != null) {
            String[] newParts = newData.split("::", 2);
            BlockDef newDef = new BlockDef(
                    newParts[1],
                    oldDef.getType(),
                    oldDef.getParams(),
                    oldDef.getSubstacks()
            );
            categoryBlocks.replaceAll(b -> b == oldDef ? newDef : b);
            newBlockMap.put(oldParts[0], Collections.unmodifiableList(categoryBlocks));
            blockMap = Collections.unmodifiableMap(newBlockMap);
        }
    }
    private void startDebugTimer() {
        debugTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdate > 0) {
                    long elapsedNanos = now - lastUpdate;
                    frameCount++;
                    if (elapsedNanos >= 1_000_000_000) {
                        fps = frameCount / (elapsedNanos / 1_000_000_000.0);
                        updateDebugInfo();
                        lastUpdate = now;
                        frameCount = 0;
                    }
                } else {
                    lastUpdate = now;
                }
            }
        };
        debugTimer.start();
    }
    private void stopDebugTimer() {
        if (debugTimer != null) {
            debugTimer.stop();
        }
        debugInfoText.setText("");
    }
    private void updateDebugInfo() {
        String info = String.format("FPS: %.1f\nFrame: %dms\n角色数: %d",
                fps,
                (int)(1000 / fps),
                characters.size()
        );
        debugInfoText.setText(info);
    }
    private void setupCoordinateBinding(TextField field, Function<Character, DoubleProperty> propGetter) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (currentCharacter.get() != null) {
                try {
                    double value = Double.parseDouble(newVal);
                    propGetter.apply(currentCharacter.get()).set(value);
                } catch (NumberFormatException e) {
                    field.setText(oldVal);
                }
            }
        });
    }
    private BlockNode createBlockNode(BlockDef def) {
        BlockNode node = new BlockNode(def);
        node.setStyle("-fx-background-radius: 5; -fx-padding: 10;");
        node.setSpacing(8);
        node.setOnContextMenuRequested(event -> {
            ContextMenu menu = new ContextMenu();
            MenuItem editItem = new MenuItem("编辑积木");
            MenuItem deleteItem = new MenuItem("删除积木");

            editItem.setOnAction(e -> showCodeBlockEditor(node));
            deleteItem.setOnAction(e -> codeBlocks.getChildren().remove(node));

            menu.getItems().addAll(editItem, deleteItem);
            menu.show(node, event.getScreenX(), event.getScreenY());
        });

        return node;
    }
    private void showCodeBlockEditor(BlockNode block) {
        TextInputDialog dialog = new TextInputDialog(block.getDefinition().getDisplayName());
        dialog.setTitle("编辑代码积木");
        dialog.setHeaderText("修改积木名称");

        dialog.showAndWait().ifPresent(newName -> {
            BlockDef newDef = new BlockDef(
                    newName,
                    block.getDefinition().getType(),
                    block.getDefinition().getParams(),
                    block.getDefinition().getSubstacks()
            );
            block.updateDefinition(newDef);
        });
    }

    @FXML
    private void showCodePanel() {
        codePanel.setVisible(true);
        bgPanel.setVisible(false);
        soundPanel.setVisible(false);
    }
    @FXML
    private void showBackgroundPanel() {
        codePanel.setVisible(false);
        bgPanel.setVisible(true);
        soundPanel.setVisible(false);
    }
    @FXML
    private void showSoundPanel() {
        codePanel.setVisible(false);
        bgPanel.setVisible(false);
        soundPanel.setVisible(true);
    }
    @FXML
    private void switchToLayout1() {
        contentSplit.setDividerPositions(0.7);
        mainSplit.setDividerPositions(0.7);
        functionArea.setVisible(true);
    }
    @FXML
    private void switchToLayout2() {
        contentSplit.setDividerPositions(0.5);
        mainSplit.setDividerPositions(0.5);
        functionArea.setVisible(true);
    }
    @FXML
    private void switchToFullScreen() {
        mainSplit.setDividerPositions(1.0);
        functionArea.setVisible(false);
    }
    @FXML
    private void createCharacter() {
        Character newChar = new Character("角色" + (characters.size() + 1));
        characters.add(newChar);
        stageArea.getChildren().add(newChar.getShape());
        characterSelector.getSelectionModel().select(newChar.getName());
        newChar.xProperty().addListener((obs, oldX, newX) -> {
            double maxX = stageArea.getWidth() - newChar.getShape().getBoundsInParent().getWidth();
            if (newX.doubleValue() > maxX) newChar.setX(maxX);
            else if (newX.doubleValue() < 0) newChar.setX(0);
        });
        newChar.yProperty().addListener((obs, oldY, newY) -> {
            double maxY = stageArea.getHeight() - newChar.getShape().getBoundsInParent().getHeight();
            if (newY.doubleValue() > maxY) newChar.setY(maxY);
            else if (newY.doubleValue() < 0) newChar.setY(0);
        });
        updateCharacterNames();
    }
    @FXML
    private void deleteCharacter() {
        Character toDelete = currentCharacter.get();
        if (toDelete != null) {
            stageArea.getChildren().remove(toDelete.getShape());
            characters.remove(toDelete);
            updateCharacterNames();
        }
    }
    @FXML
    private void updateCharacterNames() {
        List<String> names = characters.stream()
                .map(Character::getName)
                .collect(Collectors.toList());
        String selected = characterSelector.getSelectionModel().getSelectedItem();
        characterNames.setAll(names);

        if (names.contains(selected)) {
            characterSelector.getSelectionModel().select(selected);
        }
    }
    @FXML
    private void uploadBackground() {
    }
}