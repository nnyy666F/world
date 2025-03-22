package com.mirror.world;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class Character {
    private final String id;
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty x = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();
    private ChangeListener<Number> xPropertyListener;
    private ChangeListener<Number> yPropertyListener;
    private ChangeListener<String> xTextListener;
    private ChangeListener<String> yTextListener;
    private final BooleanProperty visible = new SimpleBooleanProperty(true);
    private final DoubleProperty size = new SimpleDoubleProperty(100);
    private final DoubleProperty rotation = new SimpleDoubleProperty(0);
    private final Pane shape;

    public Character(String name) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name.set(name);
        this.x.set(0);
        this.y.set(0);

        this.shape = new Pane();
        shape.setPrefSize(50, 50);
        shape.setStyle("-fx-background-color: #2196F3; -fx-border-color: #0D47A1; -fx-border-width: 2;");

        x.addListener((obs, oldVal, newVal) -> shape.setLayoutX(newVal.doubleValue()));
        y.addListener((obs, oldVal, newVal) -> shape.setLayoutY(newVal.doubleValue()));
        visible.addListener((obs, oldVal, newVal) -> shape.setVisible(newVal));
        size.addListener((obs, oldVal, newVal) -> {
            double scale = newVal.doubleValue() / 100;
            shape.setScaleX(scale);
            shape.setScaleY(scale);
        });
        rotation.addListener((obs, oldVal, newVal) -> shape.setRotate(newVal.doubleValue()));
        setupDragHandlers();
    }
    private static class Delta {
        double x, y;
    }
    private void setupDragHandlers() {
        final Delta dragDelta = new Delta();
        shape.setOnMousePressed(event -> {
            shape.toFront();
            dragDelta.x = event.getX();
            dragDelta.y = event.getY();
//            shape.setMouseTransparent(true);
            shape.setEffect(new DropShadow(10, Color.GRAY));
            event.consume();
        });
        shape.setOnMouseDragged(event -> {
            double newX = event.getX() + shape.getLayoutX() - dragDelta.x;
            double newY = event.getY() + shape.getLayoutY() - dragDelta.y;
            Pane stage = (Pane) shape.getParent();
            newX = Math.max(0, Math.min(newX, stage.getWidth() - shape.getWidth()));
            newY = Math.max(0, Math.min(newY, stage.getHeight() - shape.getHeight()));
            setX(newX);
            setY(newY);
            event.consume();
        });
        shape.setOnMouseReleased(event -> {
            shape.setMouseTransparent(false);
            shape.setEffect(null);
        });
    }
    public String getName() { return name.get(); }
    public double getX() { return x.get(); }
    public double getY() { return y.get(); }
    public boolean isVisible() { return visible.get(); }
    public double getSize() { return size.get(); }
    public double getRotation() { return rotation.get(); }
//    public Rectangle getRectangle() { return rectangle; }
    public Pane getShape() {return shape;}
    public void setName(String name) { this.name.set(name); }
    public void setX(double x) { this.x.set(x); }
    public void setY(double y) { this.y.set(y); }
    public void setVisible(boolean visible) { this.visible.set(visible); }
    public void setSize(double size) { this.size.set(size); }
    public void setRotation(double rotation) { this.rotation.set(rotation); }
    public StringProperty nameProperty() { return name; }
    public DoubleProperty xProperty() { return x; }
    public DoubleProperty yProperty() { return y; }
    public BooleanProperty visibleProperty() { return visible; }
    public DoubleProperty sizeProperty() { return size; }
    public DoubleProperty rotationProperty() { return rotation; }
    public ChangeListener<Number> getXPropertyListener() { return xPropertyListener; }
    public ChangeListener<Number> getYPropertyListener() { return yPropertyListener; }
    public ChangeListener<String> getXTextListener() { return xTextListener; }
    public ChangeListener<String> getYTextListener() { return yTextListener; }

    public void setXPropertyListener(ChangeListener<Number> l) { xPropertyListener = l; }
    public void setYPropertyListener(ChangeListener<Number> l) { yPropertyListener = l; }
    public void setXTextListener(ChangeListener<String> l) { xTextListener = l; }
    public void setYTextListener(ChangeListener<String> l) { yTextListener = l; }
}