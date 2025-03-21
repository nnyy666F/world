package com.mirror.block;

import javafx.beans.binding.Bindings;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class BlockConnection {
    public static void connect(BlockNode source, BlockNode target) {
        Line line = new Line();
        line.setStroke(Color.GRAY);
        line.startXProperty().bind(
                Bindings.createDoubleBinding(() ->
                                source.localToScene(source.getBoundsInLocal()).getMaxX(),
                        source.boundsInLocalProperty()
                )
        );
        line.startYProperty().bind(
                Bindings.createDoubleBinding(() ->
                                source.localToScene(source.getBoundsInLocal()).getCenterY(),
                        source.boundsInLocalProperty()
                )
        );

    }
}