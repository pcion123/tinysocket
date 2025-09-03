package com.vscodelife.demo.client.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 命令管理器
 * 負責處理客戶端的所有指令和聊天訊息，並管理主要的用戶交互循環
 */
public class CommandManager implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final Map<String, Consumer<Object>> commands = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Consumer<Object> defaultHandler;

    private static volatile CommandManager instance;

    private CommandManager() {
        commands.put("help", this::handleHelpCommand);
        commands.put("quit", this::handleQuitCommand);
        commands.put("exit", this::handleQuitCommand);
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            synchronized (CommandManager.class) {
                if (instance == null) {
                    instance = new CommandManager();
                }
            }
        }
        return instance;
    }

    public void registerCommand(String command, Consumer<Object> handler) {
        registerCommand(command, handler, false);
    }

    public void registerCommand(String command, Consumer<Object> handler, boolean defaultCommand) {
        commands.put(command, handler);

        if (defaultCommand) {
            defaultHandler = handler;
        }

        logger.debug("Registered command: {}", command);
    }

    /**
     * 啟動主要的用戶交互循環
     * 這個方法會一直運行直到收到退出命令
     */
    @Override
    public void run() {
        try {
            // 創建 JLine Terminal 和 LineReader
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // 顯示幫助信息
            printHelp();

            // 主循環：只有收到退出命令才會退出，與客戶端連接狀態無關
            while (running.get()) {
                try {
                    // 使用 JLine 讀取用戶輸入
                    String input = lineReader.readLine("聊天室> ");

                    if (input != null && !input.trim().isEmpty()) {
                        // 清除當前行
                        terminal.writer().print("\033[1A\033[2K"); // 向上移動一行並清除
                        terminal.flush();

                        // 處理用戶輸入
                        handleInput(input.trim());
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("ctrl-c")) {
                        // 用戶按下 Ctrl+C，正常退出
                        System.out.println("\n正在退出客戶端...");
                        running.set(false);
                        break;
                    }
                    logger.error("處理用戶輸入時發生錯誤", e);
                    // 發生錯誤時不退出，繼續等待用戶輸入
                }
            }

            terminal.close();
        } catch (Exception e) {
            logger.error("初始化終端時發生錯誤", e);
            throw new RuntimeException("無法初始化用戶界面", e);
        }
    }

    /**
     * 處理用戶輸入（訊息或指令）
     * 
     * @param input 用戶輸入的字符串
     */
    public void handleInput(String input) {
        if (input.startsWith("/")) {
            handleCommand(input);
        } else {
            if (defaultHandler != null) {
                defaultHandler.accept(input);
            } else {
                handleHelpCommand(input);
            }
        }
    }

    /**
     * 處理指令
     * 
     * @param input 完整的指令字符串（包含 /）
     */
    private void handleCommand(String input) {
        // 移除開頭的 /
        String commandLine = input.substring(1);
        String[] parts = commandLine.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        Consumer<Object> handler = commands.get(command);
        if (handler != null) {
            handler.accept(parts);
        } else {
            System.out.println("未知指令: /" + command + "，輸入 '/help' 查看可用指令");
        }
    }

    /**
     * 處理幫助命令
     */
    private void handleHelpCommand(Object params) {
        printHelp();
    }

    /**
     * 處理退出命令
     */
    private void handleQuitCommand(Object params) {
        System.out.println("正在退出客戶端...");
        running.set(false);
    }

    /**
     * 顯示幫助信息
     */
    public void printHelp() {
        System.out.println("=== 聊天室客戶端使用說明 ===");
        System.out.println("直接輸入文字即可發送聊天訊息");
        System.out.println("指令需要以 / 開頭:");
        System.out.println("/help    - 顯示此幫助信息");
        System.out.println("/users   - 獲取在線用戶列表");
        System.out.println("/quit    - 退出客戶端");
        System.out.println("========================");
        System.out.println("提示: 支援方向鍵瀏覽歷史記錄、Tab自動完成");
        System.out.println("按 Ctrl+C 可隨時退出");
        System.out.println("========================");
    }
}
