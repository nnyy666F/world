package com.mirror.block;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import java.util.*;
import java.util.stream.Collectors;

public class BlockNode extends HBox {
    private BlockDef definition;
    private final List<BlockNode> logicalChildren = new ArrayList<>();
    private final Map<Integer, Node> inputControls = new LinkedHashMap<>();
    private final List<StackPane> substackSlots = new ArrayList<>();
    private final List<TextField> numberFields = new ArrayList<>();

    private static final String[] STYLE_CLASSES = {"block-node"};
    public static final Map<BlockType, String> TYPE_COLORS = new HashMap<>() {{
        put(BlockType.EVENT, "#2196F3");
        put(BlockType.LOOP, "#4CAF50");
        put(BlockType.COMMAND, "#9C27B0");
        put(BlockType.CONDITION, "#E91E63");
    }};
    private static final Map<BlockType, String> TYPE_CLASSES = Map.of(
            BlockType.EVENT, "event-block",
            BlockType.LOOP, "loop-block",
            BlockType.COMMAND, "command-block",
            BlockType.CONDITION, "condition-block"
    );
    private String getColorByType(BlockType type) {
        return TYPE_COLORS.getOrDefault(type, "#2196F3");
    }
    public BlockNode(BlockDef definition) {
        this.definition = definition;
        buildUI();
        setupDragHandlers();
    }
    public void updateDefinition(BlockDef newDef) {
        this.definition = newDef;
        getChildren().clear();
        buildUI();
    }
    private void buildUI() {
        getStyleClass().setAll(STYLE_CLASSES);
        getStyleClass().clear();
        getStyleClass().add("block-node");
        getStyleClass().add("block-type-" + definition.getType().name().toLowerCase());
        String color = TYPE_COLORS.getOrDefault(definition.getType(), "#2196F3");
        getChildren().clear();
        getStyleClass().add("block-node-" + UUID.randomUUID().toString());
        String inputStyle = "-fx-background-color: white;" +
                "-fx-text-fill: #333;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 3 5;";
        getStyleClass().add(TYPE_CLASSES.get(definition.getType()));
        inputControls.values().forEach(control -> {
            if (control instanceof TextField) {
                ((TextField) control).setStyle(inputStyle);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setStyle(inputStyle);
            }
        });
        Text grip = new Text("≡");
        grip.setFill(Color.WHITE);
        grip.setOnDragDetected(e -> {
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            db.setDragView(snapshot(params, null));
            ClipboardContent content = new ClipboardContent();
            content.put(DataFormat.lookupMimeType("block/def"), definition);
            db.setContent(content);
            e.consume();
        });
        Label mainLabel = new Label(definition.getDisplayName());
        mainLabel.setTextFill(Color.WHITE);
        HBox paramsBox = new HBox(5);
        paramsBox.setAlignment(Pos.CENTER_LEFT);
        int paramIndex = 0;
        for (ParamType paramType : definition.getParams()) {
            Node inputControl = createInputControl(paramType);
            inputControls.put(paramIndex++, inputControl);
            paramsBox.getChildren().add(inputControl);
        }
        VBox substacksBox = new VBox(3);
        for (int i = 0; i < definition.getSubstacks(); i++) {
            StackPane slot = createSubstackSlot();
            substackSlots.add(slot);
            substacksBox.getChildren().add(slot);
        }
        HBox content = new HBox(10, grip, mainLabel, paramsBox);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0));
        VBox mainContainer = new VBox(5, content, substacksBox);
        mainContainer.setStyle("-fx-background-color: transparent;");
        getChildren().add(mainContainer);
        TextField tf = new TextField("0");
        tf.setStyle("-fx-background-color: white; -fx-text-fill: black;");
    }
    public List<BlockNode> getLogicalChildren() {
        List<BlockNode> children = new ArrayList<>();
        for (StackPane slot : substackSlots) {
            children.addAll(slot.getChildren().stream()
                    .filter(BlockNode.class::isInstance)
                    .map(BlockNode.class::cast)
                    .collect(Collectors.toList()));
        }
        return Collections.unmodifiableList(children);
    }
    private Node createInputControl(ParamType paramType) {
        return switch (paramType) {
            case NUMBER -> {
                TextField tf = new TextField("10");
                tf.setStyle("-fx-background-color: white !important;" +
                        "-fx-text-fill: black !important;" +
                        "-fx-pref-width: 60;");
                tf.getStyleClass().add("block-node-textfield");
                tf.setPrefWidth(60);
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.matches("-?\\d*\\.?\\d*")) {
                        tf.setText(oldVal);
                    }
                });
                tf.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1) {
                        tf.requestFocus();
                        tf.selectAll();
                    }
                });
                yield tf;
            }
            case BOOLEAN -> {
                ComboBox<String> cb = new ComboBox<>();
                cb.getItems().addAll("真", "假");
                cb.setValue("真");
                cb.setPrefWidth(80);
                cb.setEditable(false);
                yield cb;
            }
            default -> new Label("?");
        };
    }
    private StackPane createSubstackSlot() {
        StackPane slot = new StackPane();
        slot.setMinSize(100, 30);
        slot.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-border-radius: 3;");
        slot.setOnDragOver(e -> {
            if (isValidSubstackDrag(e.getDragboard())) {
                e.acceptTransferModes(TransferMode.MOVE);
                slot.setStyle("-fx-background-color: rgba(255,255,255,0.3);");
            }
        });
        slot.setOnDragExited(e -> slot.setStyle("-fx-background-color: rgba(255,255,255,0.1);"));
        slot.setOnDragDropped(e -> {
            BlockNode source = (BlockNode) e.getGestureSource();
            if (source != null && source != this) {
                slot.getChildren().add(source);
                logicalChildren.add(source);
                e.setDropCompleted(true);
            }
        });
        return slot;
    }
    private boolean isValidSubstackDrag(Dragboard db) {
        return db.hasContent(DataFormat.lookupMimeType("block/def"))
                && db.getContent(DataFormat.lookupMimeType("block/def")) instanceof BlockDef;
    }
    private void setupDragHandlers() {
        setOnDragDetected(e -> {
            StackPane dragPreview = new StackPane();
            dragPreview.setStyle(getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");
            dragPreview.getChildren().add(new Label(definition.getDisplayName()));
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            db.setDragView(dragPreview.snapshot(params, null));
            db.setContent(new ClipboardContent());
            e.consume();
        });
    }
    public Object getParamValue(int index) {
        Node control = inputControls.get(index);
        if (control instanceof TextField tf) {
            return Double.parseDouble(tf.getText());
        } else if (control instanceof ComboBox<?> cb) {
            return cb.getValue().equals("真");
        }
        return null;
    }
    public List<BlockNode> getSubstack(int slotIndex) {
        return substackSlots.get(slotIndex).getChildren()
                .stream()
                .filter(BlockNode.class::isInstance)
                .map(BlockNode.class::cast)
                .collect(Collectors.toList());
    }
    public Map<String, Object> getFreshParameters() {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < inputControls.size(); i++) {
            params.put(String.valueOf(i), getParamValue(i));
        }
        return params;
    }
    public BlockDef getDefinition() {
        return definition;
    }
}