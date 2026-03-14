package org.ssl.vpn4j.utils;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssl.vpn4j.enums.SystemType;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Shell命令执行工具类
 * 支持同步/异步执行、超时控制、流式处理等
 */
public class ShellExecutorUtils {

    private static final Logger logger = LoggerFactory.getLogger(ShellExecutorUtils.class);

    // 默认配置
    private static final long DEFAULT_TIMEOUT = 60000L; // 60秒
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Shell执行结果封装类
     */
    @Getter
    public static class ExecuteResult {
        private final int exitCode;
        private final String output;
        private final String error;
        private final long executionTime;
        private final boolean success;

        public ExecuteResult(int exitCode, String output, String error, long executionTime) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
            this.executionTime = executionTime;
            this.success = exitCode == 0;
        }

        @Override
        public String toString() {
            return String.format(
                "ExecuteResult{exitCode=%d, success=%s, time=%dms, outputLength=%d, errorLength=%d}",
                exitCode, success, executionTime,
                output != null ? output.length() : 0,
                error != null ? error.length() : 0
            );
        }
    }

    /**
     * 同步执行Shell命令（默认超时60秒）
     * @param command 命令字符串或命令数组
     * @return 执行结果
     */
    public static ExecuteResult execute(String command) {
        return execute(command, DEFAULT_TIMEOUT, null, null);
    }

    /**
     * 同步执行Shell命令（默认超时60秒）
     * @param command_linux 命令字符串或命令数组
     * @param command_windows 命令字符串或命令数组
     * @return 执行结果
     */
    public static ExecuteResult executeBySystem(String command_linux, String command_windows) {
        String command = SystemTypeUtil.getSystemType() == SystemType.WINDOWS ? command_windows : command_linux;
        return execute(command, DEFAULT_TIMEOUT, null, null);
    }

    public static Future<ExecuteResult> executeAsync(String command_linux, String command_windows) {
        String command = SystemTypeUtil.getSystemType() == SystemType.WINDOWS ? command_windows : command_linux;
        return executeAsync(command);
    }

    /**
     * 在同一个窗口中执行多个命令
     * @param commands_linux Linux命令列表
     * @param commands_windows Windows命令列表
     * @return 每个命令对应的输出结果集合
     */
    public static List<String> executeMultipleBySystem(List<String> commands_linux, List<String> commands_windows) {
        boolean isWindows = SystemTypeUtil.getSystemType() == SystemType.WINDOWS;
        List<String> commands = isWindows ? commands_windows : commands_linux;
        if (commands == null || commands.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用唯一的定界符来分割不同命令的输出
        String delimiter = "---CMD_PART_SEP---";
        StringBuilder joinedCommand = new StringBuilder();

        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            // 如果是 Windows 且命令自带了 powershell 前缀，尝试提取核心命令以避免嵌套
            if (isWindows && cmd.toLowerCase().contains("powershell")) {
                cmd = cmd.replaceAll("(?i)powershell\\s+-command\\s+\"(.*)\"", "$1")
                         .replaceAll("(?i)powershell\\s+\"(.*)\"", "$1");
            }
            joinedCommand.append(cmd);
            if (i < commands.size() - 1) {
                joinedCommand.append("; echo '").append(delimiter).append("'; ");
            }
        }

        // 执行合并后的命令
        String finalCmd;
        if (isWindows) {
            // 对内部引号进行转义，确保外层包装正确
            String escaped = joinedCommand.toString().replace("\"", "\\\"");
            finalCmd = "powershell -NoProfile -NonInteractive -Command \"& {" + escaped + "}\"";
        } else {
            finalCmd = joinedCommand.toString();
        }
        ExecuteResult res = execute(finalCmd);

        // 解析输出
        if (res.getOutput() == null || res.getOutput().trim().isEmpty()) {
            List<String> emptyResults = new ArrayList<>();
            for (int i = 0; i < commands.size(); i++) emptyResults.add("");
            return emptyResults;
        }

        // 使用 Pattern.quote 确保定界符准确切割
        String[] parts = res.getOutput().split(java.util.regex.Pattern.quote(delimiter));
        List<String> results = new ArrayList<>();
        for (String part : parts) {
            results.add(part.trim());
        }

        // 补齐结果
        while (results.size() < commands.size()) {
            results.add("");
        }

        return results;
    }

    /**
     * 同步执行Shell命令
     * @param command 命令字符串
     * @param timeout 超时时间（毫秒）
     * @return 执行结果
     */
    public static ExecuteResult execute(String command, long timeout) {
        return execute(command, timeout, null, null);
    }

    /**
     * 同步执行Shell命令
     * @param command 命令数组
     * @param timeout 超时时间（毫秒）
     * @return 执行结果
     */
    public static ExecuteResult execute(String[] command, long timeout) {
        return execute(command, timeout, null, null);
    }

    /**
     * 同步执行Shell命令（完整参数）
     * @param command 命令字符串
     * @param timeout 超时时间（毫秒）
     * @param env 环境变量
     * @param workingDir 工作目录
     * @return 执行结果
     */
    public static ExecuteResult execute(String command, long timeout,
                                        Map<String, String> env,
                                        File workingDir) {
        String[] commandArray;
        if (SystemTypeUtil.getSystemType() == SystemType.WINDOWS) {
            commandArray = new String[]{"cmd.exe", "/c", command};
        } else {
            commandArray = new String[]{"sh", "-c", command};
        }
        return execute(commandArray, timeout, env, workingDir);
    }

    /**
     * 同步执行Shell命令（完整参数）
     * @param commandArray 命令数组
     * @param timeout 超时时间（毫秒）
     * @param env 环境变量
     * @param workingDir 工作目录
     * @return 执行结果
     */
    public static ExecuteResult execute(String[] commandArray, long timeout,
                                        Map<String, String> env,
                                        File workingDir) {
        long startTime = System.currentTimeMillis();
        Process process = null;
        Future<String> outputFuture = null;
        Future<String> errorFuture = null;
        ExecutorService executor = null;

        try {
            // 构建ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);

            // 设置环境变量
            if (env != null && !env.isEmpty()) {
                Map<String, String> processEnv = processBuilder.environment();
                processEnv.putAll(env);
            }

            // 设置工作目录
            if (workingDir != null) {
                processBuilder.directory(workingDir);
            }

            // 重定向错误流到输出流（可选）
            processBuilder.redirectErrorStream(false);

            // 记录执行的命令
            String commandStr = String.join(" ", commandArray);
            logger.info("Executing shell command: {}", commandStr);

            // 启动进程
            process = processBuilder.start();

            // 创建线程池处理输出流
            executor = Executors.newFixedThreadPool(2);

            // 异步读取标准输出和错误输出
            Process finalProcess = process;
            outputFuture = executor.submit(() -> readStream(finalProcess.getInputStream()));
            Process finalProcess1 = process;
            errorFuture = executor.submit(() -> readStream(finalProcess1.getErrorStream()));

            // 等待进程完成
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);

            if (!finished) {
                throw new TimeoutException("Command execution timeout after " + timeout + "ms");
            }

            // 获取执行结果
            int exitCode = process.exitValue();
            String output = outputFuture.get(timeout, TimeUnit.MILLISECONDS);
            String error = errorFuture.get(timeout, TimeUnit.MILLISECONDS);

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            ExecuteResult result = new ExecuteResult(exitCode, output, error, executionTime);

            // 根据退出码记录日志
            if (exitCode == 0) {
                logger.debug("Command executed successfully: {}, time: {}ms", commandStr, executionTime);
            } else {
                logger.warn("Command executed with exit code {}: {}, error: {}",
                    exitCode, commandStr, error);
            }

            return result;

        } catch (TimeoutException e) {
            logger.error("Command execution timeout: {}", e.getMessage());
            throw new RuntimeException("Command execution timeout", e);
        } catch (IOException e) {
            logger.error("IO error while executing command", e);
            throw new RuntimeException("Failed to execute command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Command execution interrupted", e);
            throw new RuntimeException("Command execution interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Error reading command output", e);
            throw new RuntimeException("Error reading command output", e);
        } finally {
            // 清理资源
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            // 取消任务
            if (outputFuture != null && !outputFuture.isDone()) {
                outputFuture.cancel(true);
            }
            if (errorFuture != null && !errorFuture.isDone()) {
                errorFuture.cancel(true);
            }

            // 关闭线程池
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * 异步执行Shell命令
     * @param command 命令
     * @return Future对象，可用于取消任务
     */
    public static Future<ExecuteResult> executeAsync(String command) {
        String[] commandArray;
        if (SystemTypeUtil.getSystemType() == SystemType.WINDOWS) {
            commandArray = new String[]{"cmd.exe", "/c", command};
        } else {
            commandArray = new String[]{"sh", "-c", command};
        }
        return executeAsync(commandArray, DEFAULT_TIMEOUT, null, null, null);
    }

    /**
     * 异步执行Shell命令
     * @param command 命令
     * @param callback 回调接口
     * @return Future对象，可用于取消任务
     */
    public static Future<ExecuteResult> executeAsync(String command, ExecuteCallback callback) {
        String[] commandArray;
        if (SystemTypeUtil.getSystemType() == SystemType.WINDOWS) {
            commandArray = new String[]{"cmd.exe", "/c", command};
        } else {
            commandArray = new String[]{"sh", "-c", command};
        }
        return executeAsync(commandArray, DEFAULT_TIMEOUT, null, null, callback);
    }

    /**
     * 异步执行Shell命令
     * @param commandArray 命令数组
     * @param callback 回调接口
     * @return Future对象
     */
    public static Future<ExecuteResult> executeAsync(String[] commandArray, ExecuteCallback callback) {
        return executeAsync(commandArray, DEFAULT_TIMEOUT, null, null, callback);
    }

    /**
     * 异步执行Shell命令
     * @param commandArray 命令数组
     * @param timeout 超时时间
     * @param callback 回调接口
     * @return Future对象
     */
    public static Future<ExecuteResult> executeAsync(String[] commandArray, long timeout,
                                                     Map<String, String> env,
                                                     File workingDir,
                                                     ExecuteCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            try {
                ExecuteResult result = execute(commandArray, timeout, env, workingDir);
                if (callback != null) {
                    if (result.isSuccess()) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(result);
                    }
                }
                return result;
            } catch (Exception e) {
                if (callback != null) {
                    callback.onException(e);
                }
                throw e;
            } finally {
                executor.shutdown();
            }
        });
    }

    /**
     * 流式执行Shell命令（实时输出）
     * @param command 命令
     * @param lineHandler 行处理器
     * @return 执行结果
     */
    public static ExecuteResult executeWithStream(String command, LineHandler lineHandler) {
        String[] commandArray;
        if (SystemTypeUtil.getSystemType() == SystemType.WINDOWS) {
            commandArray = new String[]{"cmd.exe", "/c", command};
        } else {
            commandArray = new String[]{"sh", "-c", command};
        }
        return executeWithStream(commandArray, DEFAULT_TIMEOUT,
            null, null, lineHandler);
    }

    /**
     * 流式执行Shell命令
     * @param commandArray 命令数组
     * @param timeout 超时时间
     * @param lineHandler 行处理器
     * @return 执行结果
     */
    public static ExecuteResult executeWithStream(String[] commandArray, long timeout,
                                                  Map<String, String> env,
                                                  File workingDir,
                                                  LineHandler lineHandler) {
        long startTime = System.currentTimeMillis();
        Process process = null;
        BufferedReader outputReader = null;
        BufferedReader errorReader = null;
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);

            if (env != null) {
                processBuilder.environment().putAll(env);
            }

            if (workingDir != null) {
                processBuilder.directory(workingDir);
            }

            process = processBuilder.start();

            // 启动线程处理输出流
            Process finalProcess = process;
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(finalProcess.getInputStream(), DEFAULT_CHARSET))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        if (lineHandler != null) {
                            lineHandler.handleOutputLine(line);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error reading output stream", e);
                }
            });

            Process finalProcess1 = process;
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(finalProcess1.getErrorStream(), DEFAULT_CHARSET))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                        if (lineHandler != null) {
                            lineHandler.handleErrorLine(line);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error reading error stream", e);
                }
            });

            outputThread.start();
            errorThread.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);

            if (!finished) {
                throw new TimeoutException("Command execution timeout");
            }

            // 等待输出线程完成
            outputThread.join(1000);
            errorThread.join(1000);

            int exitCode = process.exitValue();
            long executionTime = System.currentTimeMillis() - startTime;

            return new ExecuteResult(exitCode, output.toString(), error.toString(), executionTime);

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute command with stream", e);
        } finally {
            closeQuietly(outputReader);
            closeQuietly(errorReader);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 读取流内容
     */
    private static String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, DEFAULT_CHARSET))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 安静关闭流
     */
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logger.debug("Error closing stream", e);
            }
        }
    }

    /**
     * 执行回调接口
     */
    public interface ExecuteCallback {
        void onSuccess(ExecuteResult result);
        void onError(ExecuteResult result);
        void onException(Exception e);
    }

    /**
     * 行处理器接口
     */
    public interface LineHandler {
        void handleOutputLine(String line);
        void handleErrorLine(String line);

        /**
         * 默认实现：输出到控制台
         */
        static LineHandler consolePrinter() {
            return new LineHandler() {
                @Override
                public void handleOutputLine(String line) {
                    System.out.println("[OUT] " + line);
                }

                @Override
                public void handleErrorLine(String line) {
                    System.err.println("[ERR] " + line);
                }
            };
        }

        /**
         * 日志记录器实现
         */
        static LineHandler loggerPrinter() {
            return new LineHandler() {
                @Override
                public void handleOutputLine(String line) {
                    logger.info("Shell output: {}", line);
                }

                @Override
                public void handleErrorLine(String line) {
                    logger.error("Shell error: {}", line);
                }
            };
        }
    }
}
