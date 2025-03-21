package com.mirror.block;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

public class BlockContainer extends VBox {
    private final ObservableList<BlockNode> children = FXCollections.observableArrayList();

    public BlockContainer() {
        spacingProperty().bind(Bindings.createDoubleBinding(
                () -> (double) (getChildren().isEmpty() ? 0 : 5)
        ));

        children.addListener((ListChangeListener<BlockNode>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    getChildren().addAll(c.getAddedSubList());
                }
                if (c.wasRemoved()) {
                    getChildren().removeAll(c.getRemoved());
                }
            }
        });
    }

    public void addBlock(BlockNode block, int index) {
        children.add(index, block);
    }
}