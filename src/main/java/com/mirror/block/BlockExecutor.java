package com.mirror.block;

import com.mirror.world.Character;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class BlockExecutor {
    private final Character character;
    private volatile boolean isRunning = true;

    public BlockExecutor(Character character) {
        this.character = character;
    }
    public void execute(List<BlockNode> blocks) throws InterruptedException {
        for (BlockNode block : blocks) {
            executeBlock(block);
        }
    }
    private void executeBlock(BlockNode block) throws InterruptedException {
        BlockDef def = block.getDefinition();
        switch (def.getType()) {
            case COMMAND:
                handleCommand(block);
                break;

            case LOOP:
                int times = (int) block.getParamValue(0);
                for (int i = 0; i < times; i++) {
                    execute(block.getLogicalChildren());
                }
                break;

            case CONDITION:
                if ((boolean) block.getParamValue(0)) {
                    execute(block.getLogicalChildren());
                }
                break;
        }
    }
    private void handleCommand(BlockNode block) throws InterruptedException {
        Map<String, Object> params = block.getFreshParameters();
        String command = block.getDefinition().getDisplayName();
        switch (command) {
            case "移动":
                executeMove((double) params.get("0"));
                break;
            case "右转":
                executeRotate((double) params.get("0"));
                break;
        }
    }
    private void executeSubstack(BlockNode parent, int slotIndex) throws InterruptedException {
        for (BlockNode child : parent.getSubstack(slotIndex)) {
            executeBlock(child);
        }
    }
    private double getDoubleParam(Map<String, Object> params, int index) {
        Object value = params.getOrDefault(String.valueOf(index), 0.0);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }
    private int getIntParam(Map<String, Object> params, int index) {
        return (int) Math.round(getDoubleParam(params, index));
    }
    private void executeMove(double steps) throws InterruptedException {
        System.out.printf("移动: %.1f步%n", steps);
        syncUpdate(() -> {
            double radians = Math.toRadians(character.getRotation());
            character.setX(character.getX() + steps * Math.cos(radians));
            character.setY(character.getY() + steps * Math.sin(radians));
        });
    }
    private void executeRotate(double degrees) throws InterruptedException {
        System.out.printf("旋转: %.1f度%n", degrees);
        syncUpdate(() -> character.setRotation(character.getRotation() + degrees));
    }
    private void executeWait(double seconds) throws InterruptedException {
        System.out.printf("等待: %.1f秒%n", seconds);
        Thread.sleep((long)(seconds * 1000));
    }
    private void syncUpdate(Runnable task) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                task.run();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    public void stop() {
        isRunning = false;
    }

    public static void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("执行错误");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}