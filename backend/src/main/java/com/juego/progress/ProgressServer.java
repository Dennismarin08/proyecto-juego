package com.juego.progress;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ProgressServer {
    private static final String DB_URL = "jdbc:h2:./progress-data;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws Exception {
        createDatabase();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/progress", new ProgressHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Progress server started at http://localhost:8080/api/progress");
    }

    private static void createDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS progress ("
                    + "userId VARCHAR(100) PRIMARY KEY,"
                    + "level INT,"
                    + "subWave INT,"
                    + "score INT,"
                    + "energy INT,"
                    + "updatedAt BIGINT"
                    + ")");
        }
    }

    static class ProgressHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleGet(exchange);
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handlePost(exchange);
                } else {
                    sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, GSON.toJson(Map.of("error", e.getMessage())));
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException, SQLException {
            URI requestUri = exchange.getRequestURI();
            String query = requestUri.getRawQuery();
            Map<String, String> params = parseQuery(query);
            String userId = params.get("userId");
            if (userId == null || userId.isBlank()) {
                sendResponse(exchange, 400, GSON.toJson(Map.of("error", "userId is required")));
                return;
            }

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = connection.prepareStatement("SELECT level, subWave, score, energy FROM progress WHERE userId = ?")) {
                statement.setString(1, userId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", userId);
                        data.put("level", rs.getInt("level"));
                        data.put("subWave", rs.getInt("subWave"));
                        data.put("score", rs.getInt("score"));
                        data.put("energy", rs.getInt("energy"));
                        sendResponse(exchange, 200, GSON.toJson(data));
                    } else {
                        sendResponse(exchange, 404, GSON.toJson(Map.of("error", "No progress found")));
                    }
                }
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException, SQLException {
            JsonObject body = JsonParser.parseReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();
            String userId = body.has("userId") ? body.get("userId").getAsString() : null;
            int level = body.has("level") ? body.get("level").getAsInt() : 1;
            int subWave = body.has("subWave") ? body.get("subWave").getAsInt() : 1;
            int score = body.has("score") ? body.get("score").getAsInt() : 0;
            int energy = body.has("energy") ? body.get("energy").getAsInt() : 0;

            if (userId == null || userId.isBlank()) {
                sendResponse(exchange, 400, GSON.toJson(Map.of("error", "userId is required")));
                return;
            }

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = connection.prepareStatement(
                         "MERGE INTO progress (userId, level, subWave, score, energy, updatedAt) KEY(userId) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, userId);
                statement.setInt(2, level);
                statement.setInt(3, subWave);
                statement.setInt(4, score);
                statement.setInt(5, energy);
                statement.setLong(6, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }

            sendResponse(exchange, 200, GSON.toJson(Map.of("status", "saved", "userId", userId, "level", level, "subWave", subWave, "score", score, "energy", energy)));
        }

        private void addCorsHeaders(Headers headers) {
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null || query.isBlank()) return params;
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
            return params;
        }
    }
}
