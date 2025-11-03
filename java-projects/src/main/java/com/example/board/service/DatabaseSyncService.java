package com.example.board.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데이터베이스 동기화 서비스
 * 개발 서버에서 운영 서버로 데이터를 동기화하는 기능 제공
 */
@Service
@Slf4j
public class DatabaseSyncService {
    
    @Value("${spring.datasource.url}")
    private String localDbUrl;
    
    @Value("${spring.datasource.username}")
    private String localDbUsername;
    
    @Value("${spring.datasource.password}")
    private String localDbPassword;
    
    @Value("${remote.db.host}")
    private String remoteDbHost;
    
    @Value("${remote.db.port}")
    private int remoteDbPort;
    
    @Value("${remote.db.database}")
    private String remoteDbDatabase;
    
    @Value("${remote.db.username}")
    private String remoteDbUsername;
    
    @Value("${remote.db.password}")
    private String remoteDbPassword;
    
    /**
     * 로컬 데이터베이스(개발 서버)의 테이블 목록 조회
     */
    public List<Map<String, Object>> getLocalTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        
        try (Connection conn = getLocalConnection()) {
            // 테이블 목록과 정보 조회
            try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT tablename 
                FROM pg_tables 
                WHERE schemaname = 'public' 
                ORDER BY tablename
                """)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String tableName = rs.getString("tablename");
                    Map<String, Object> tableInfo = getTableInfo(conn, tableName, true);
                    tables.add(tableInfo);
                }
            }
            
        } catch (SQLException e) {
            log.error("로컬 테이블 목록 조회 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return tables;
    }
    
    /**
     * 원격 데이터베이스(운영 서버)의 테이블 목록 조회
     */
    public List<Map<String, Object>> getRemoteTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        
        try (Connection conn = getRemoteConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT tablename 
                FROM pg_tables 
                WHERE schemaname = 'public' 
                ORDER BY tablename
                """)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String tableName = rs.getString("tablename");
                    Map<String, Object> tableInfo = getTableInfo(conn, tableName, false);
                    tables.add(tableInfo);
                }
            }
            
        } catch (SQLException e) {
            log.error("원격 테이블 목록 조회 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return tables;
    }
    
    /**
     * 테이블 정보 조회 (로우 개수, 최종 갱신일)
     */
    private Map<String, Object> getTableInfo(Connection conn, String tableName, boolean isLocal) {
        Map<String, Object> info = new HashMap<>();
        info.put("tableName", tableName);
        info.put("isLocal", isLocal);
        
        try {
            // 테이블 코멘트(설명) 조회
            try (PreparedStatement commentStmt = conn.prepareStatement("""
                SELECT obj_description(c.oid, 'pg_class') as table_comment
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relname = ?
                  AND c.relkind = 'r'
                """)) {
                commentStmt.setString(1, tableName);
                ResultSet commentRs = commentStmt.executeQuery();
                if (commentRs.next()) {
                    String comment = commentRs.getString("table_comment");
                    info.put("tableComment", comment != null ? comment : "");
                } else {
                    info.put("tableComment", "");
                }
            } catch (SQLException e) {
                // 코멘트가 없거나 조회 실패 시 빈 문자열
                info.put("tableComment", "");
            }
            
            // 로우 개수 조회
            try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName)) {
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    info.put("rowCount", countRs.getLong(1));
                }
            }
            
            // 최종 갱신일 조회 (created_at 또는 updated_at 컬럼이 있는 경우)
            Timestamp lastUpdated = null;
            try {
                // created_at 컬럼 확인
                try (PreparedStatement colStmt = conn.prepareStatement("""
                    SELECT column_name 
                    FROM information_schema.columns 
                    WHERE table_schema = 'public' 
                      AND table_name = ? 
                      AND column_name IN ('created_at', 'updated_at')
                    ORDER BY CASE column_name 
                        WHEN 'updated_at' THEN 1 
                        WHEN 'created_at' THEN 2 
                    END
                    LIMIT 1
                    """)) {
                    colStmt.setString(1, tableName);
                    ResultSet colRs = colStmt.executeQuery();
                    if (colRs.next()) {
                        String dateColumn = colRs.getString("column_name");
                        try (PreparedStatement dateStmt = conn.prepareStatement(
                                "SELECT MAX(" + dateColumn + ") FROM " + tableName)) {
                            ResultSet dateRs = dateStmt.executeQuery();
                            if (dateRs.next()) {
                                lastUpdated = dateRs.getTimestamp(1);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                // created_at/updated_at 컬럼이 없는 경우 무시
                log.debug("테이블 {}에 날짜 컬럼이 없습니다: {}", tableName, e.getMessage());
            }
            
            info.put("lastUpdated", lastUpdated != null ? 
                lastUpdated.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            
        } catch (SQLException e) {
            log.error("테이블 정보 조회 중 오류 발생 (테이블: {}): {}", tableName, e.getMessage());
            info.put("rowCount", 0L);
            info.put("lastUpdated", null);
            if (!info.containsKey("tableComment")) {
                info.put("tableComment", "");
            }
        }
        
        return info;
    }
    
    /**
     * 테이블 동기화 (개발 서버 → 운영 서버)
     * 연결을 자동으로 생성하고 닫습니다.
     */
    public Map<String, Object> syncTable(String tableName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("테이블 동기화 시작: {}", tableName);
            
            // 1. 로컬에서 데이터 조회
            List<Map<String, Object>> localData = getTableData(getLocalConnection(), tableName);
            
            // 2. 원격에서 기존 데이터 삭제 (TRUNCATE 또는 DELETE)
            try (Connection remoteConn = getRemoteConnection()) {
                // 원격 테이블이 없으면 생성
                createTableIfNotExists(remoteConn, tableName, getLocalConnection());
                
                // 테이블 코멘트 동기화
                syncTableComment(remoteConn, tableName, getLocalConnection());
                
                // 기존 데이터 삭제
                try (PreparedStatement truncateStmt = remoteConn.prepareStatement("TRUNCATE TABLE " + tableName + " CASCADE")) {
                    truncateStmt.execute();
                }
                
                // 3. 원격에 데이터 삽입
                if (!localData.isEmpty()) {
                    insertDataToRemote(remoteConn, tableName, localData);
                }
                
                result.put("success", true);
                result.put("message", String.format("테이블 '%s' 동기화 완료 (%d건)", tableName, localData.size()));
                result.put("syncedRows", localData.size());
                
                log.info("테이블 동기화 완료: {} ({}건)", tableName, localData.size());
                
            }
            
        } catch (Exception e) {
            log.error("테이블 동기화 중 오류 발생 (테이블: {}): {}", tableName, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "동기화 중 오류 발생: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 테이블 동기화 (개발 서버 → 운영 서버)
     * 기존 연결을 재사용합니다. (전체 동기화용)
     */
    private Map<String, Object> syncTableWithConnection(String tableName, Connection localConn, Connection remoteConn) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("테이블 동기화 시작: {}", tableName);
            
            // 1. 로컬에서 데이터 조회
            List<Map<String, Object>> localData = getTableData(localConn, tableName);
            
            // 2. 원격 테이블이 없으면 생성
            createTableIfNotExists(remoteConn, tableName, localConn);
            
            // 3. 테이블 코멘트 동기화
            syncTableComment(remoteConn, tableName, localConn);
            
            // 4. 기존 데이터 삭제
            try (PreparedStatement truncateStmt = remoteConn.prepareStatement("TRUNCATE TABLE " + tableName + " CASCADE")) {
                truncateStmt.execute();
            }
            
            // 5. 원격에 데이터 삽입
            if (!localData.isEmpty()) {
                insertDataToRemote(remoteConn, tableName, localData);
            }
            
            result.put("success", true);
            result.put("message", String.format("테이블 '%s' 동기화 완료 (%d건)", tableName, localData.size()));
            result.put("syncedRows", localData.size());
            
            log.info("테이블 동기화 완료: {} ({}건)", tableName, localData.size());
            
        } catch (Exception e) {
            log.error("테이블 동기화 중 오류 발생 (테이블: {}): {}", tableName, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "동기화 중 오류 발생: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 모든 테이블 동기화 (개발 서버 → 운영 서버)
     * 모든 테이블을 순차적으로 동기화하고 결과를 반환합니다.
     * 연결을 한 번만 열고 모든 테이블에 재사용합니다.
     */
    public Map<String, Object> syncAllTables() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tableResults = new ArrayList<>();
        
        // 연결을 한 번만 열고 재사용
        Connection localConn = null;
        Connection remoteConn = null;
        
        try {
            log.info("전체 테이블 동기화 시작");
            
            // 1. 연결 열기 (한 번만)
            localConn = getLocalConnection();
            remoteConn = getRemoteConnection();
            
            log.info("로컬/원격 데이터베이스 연결 완료");
            
            // 2. 로컬 테이블 목록 조회
            List<Map<String, Object>> localTables = getLocalTables();
            
            if (localTables == null || localTables.isEmpty()) {
                result.put("success", false);
                result.put("message", "동기화할 테이블이 없습니다.");
                return result;
            }
            
            int totalTables = localTables.size();
            int successCount = 0;
            int failCount = 0;
            long totalSyncedRows = 0;
            
            // 3. 각 테이블 동기화 실행 (순차적으로, 같은 연결 재사용)
            for (Map<String, Object> tableInfo : localTables) {
                String tableName = (String) tableInfo.get("tableName");
                if (tableName == null) continue;
                
                // 연결을 재사용하는 메서드 호출
                Map<String, Object> syncResult = syncTableWithConnection(tableName, localConn, remoteConn);
                
                Map<String, Object> tableResult = new HashMap<>();
                tableResult.put("tableName", tableName);
                tableResult.put("success", syncResult.get("success"));
                tableResult.put("message", syncResult.get("message"));
                tableResult.put("syncedRows", syncResult.get("syncedRows"));
                
                tableResults.add(tableResult);
                
                if (Boolean.TRUE.equals(syncResult.get("success"))) {
                    successCount++;
                    Object syncedRows = syncResult.get("syncedRows");
                    if (syncedRows instanceof Number) {
                        totalSyncedRows += ((Number) syncedRows).longValue();
                    }
                } else {
                    failCount++;
                }
                
                log.info("테이블 동기화 진행: {}/{} - {}", successCount + failCount, totalTables, tableName);
            }
            
            // 4. 결과 정리
            result.put("success", true);
            result.put("message", String.format("전체 동기화 완료: 성공 %d개, 실패 %d개, 총 %d건", 
                successCount, failCount, totalSyncedRows));
            result.put("totalTables", totalTables);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalSyncedRows", totalSyncedRows);
            result.put("tableResults", tableResults);
            
            log.info("전체 테이블 동기화 완료: 성공 {}개, 실패 {}개", successCount, failCount);
            
        } catch (Exception e) {
            log.error("전체 테이블 동기화 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "전체 동기화 중 오류 발생: " + e.getMessage());
            result.put("tableResults", tableResults); // 부분 결과라도 반환
        } finally {
            // 5. 연결 닫기
            if (localConn != null) {
                try {
                    localConn.close();
                    log.debug("로컬 연결 종료");
                } catch (SQLException e) {
                    log.warn("로컬 연결 종료 중 오류: {}", e.getMessage());
                }
            }
            if (remoteConn != null) {
                try {
                    remoteConn.close();
                    log.debug("원격 연결 종료");
                } catch (SQLException e) {
                    log.warn("원격 연결 종료 중 오류: {}", e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 테이블 데이터 조회
     */
    private List<Map<String, Object>> getTableData(Connection conn, String tableName) throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + tableName)) {
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                data.add(row);
            }
        }
        
        return data;
    }
    
    /**
     * 테이블 코멘트 동기화
     */
    private void syncTableComment(Connection remoteConn, String tableName, Connection localConn) throws SQLException {
        try {
            // 로컬에서 테이블 코멘트 조회
            String comment = null;
            try (PreparedStatement commentStmt = localConn.prepareStatement("""
                SELECT obj_description(c.oid, 'pg_class') as table_comment
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relname = ?
                  AND c.relkind = 'r'
                """)) {
                commentStmt.setString(1, tableName);
                ResultSet commentRs = commentStmt.executeQuery();
                if (commentRs.next()) {
                    comment = commentRs.getString("table_comment");
                }
            }
            
            // 원격에 코멘트 설정 (SQL 인젝션 방지를 위해 이스케이프 처리)
            if (comment != null && !comment.isEmpty()) {
                // 작은따옴표 이스케이프 처리
                String escapedComment = comment.replace("'", "''");
                String sql = "COMMENT ON TABLE " + tableName + " IS '" + escapedComment + "'";
                try (java.sql.Statement updateCommentStmt = remoteConn.createStatement()) {
                    updateCommentStmt.execute(sql);
                    log.debug("테이블 코멘트 동기화 완료: {} - {}", tableName, comment);
                }
            } else {
                // 코멘트가 없으면 제거
                String sql = "COMMENT ON TABLE " + tableName + " IS NULL";
                try (java.sql.Statement removeCommentStmt = remoteConn.createStatement()) {
                    removeCommentStmt.execute(sql);
                    log.debug("테이블 코멘트 제거 완료: {}", tableName);
                }
            }
        } catch (SQLException e) {
            log.warn("테이블 코멘트 동기화 중 오류 발생 (테이블: {}): {}", tableName, e.getMessage());
            // 코멘트 동기화 실패해도 데이터 동기화는 계속 진행
        }
    }
    
    /**
     * 원격 테이블 생성 (로컬 스키마 복사)
     */
    private void createTableIfNotExists(Connection remoteConn, String tableName, Connection localConn) throws SQLException {
        // 테이블 존재 확인
        try (PreparedStatement checkStmt = remoteConn.prepareStatement("""
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                  AND table_name = ?
            )
            """)) {
            checkStmt.setString(1, tableName);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getBoolean(1)) {
                // 테이블이 이미 존재
                return;
            }
        }
        
        // 로컬에서 CREATE TABLE 문 가져오기
        String createTableSql = getCreateTableStatement(localConn, tableName);
        if (createTableSql != null && !createTableSql.isEmpty()) {
            // 여러 SQL 문을 세미콜론으로 분리하여 실행
            String[] sqlStatements = createTableSql.split(";\\s*\n");
            for (String sql : sqlStatements) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    try (PreparedStatement createStmt = remoteConn.prepareStatement(sql)) {
                        createStmt.execute();
                    }
                }
            }
            log.info("원격 테이블 생성 완료: {}", tableName);
        }
    }
    
    /**
     * CREATE TABLE 문 가져오기 (PostgreSQL 스키마 정보 기반)
     */
    private String getCreateTableStatement(Connection conn, String tableName) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        
        // 컬럼 정보 가져오기
        List<String> columns = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("""
            SELECT 
                column_name,
                data_type,
                character_maximum_length,
                numeric_precision,
                numeric_scale,
                is_nullable,
                column_default
            FROM information_schema.columns
            WHERE table_schema = 'public' 
              AND table_name = ?
            ORDER BY ordinal_position
            """)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String colName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                Integer maxLength = rs.getObject("character_maximum_length", Integer.class);
                Integer precision = rs.getObject("numeric_precision", Integer.class);
                Integer scale = rs.getObject("numeric_scale", Integer.class);
                String isNullable = rs.getString("is_nullable");
                String defaultValue = rs.getString("column_default");
                
                // 컬럼명을 따옴표로 감싸서 PostgreSQL 예약어 문제 해결
                StringBuilder colDef = new StringBuilder("    \"").append(colName).append("\" ");
                
                // 데이터 타입 처리
                switch (dataType) {
                    case "character varying":
                        colDef.append("VARCHAR");
                        if (maxLength != null) {
                            colDef.append("(").append(maxLength).append(")");
                        }
                        break;
                    case "character":
                        colDef.append("CHAR");
                        if (maxLength != null) {
                            colDef.append("(").append(maxLength).append(")");
                        }
                        break;
                    case "numeric":
                        colDef.append("NUMERIC");
                        if (precision != null) {
                            colDef.append("(").append(precision);
                            if (scale != null && scale > 0) {
                                colDef.append(",").append(scale);
                            }
                            colDef.append(")");
                        }
                        break;
                    case "bigint":
                        colDef.append("BIGINT");
                        break;
                    case "integer":
                        colDef.append("INTEGER");
                        break;
                    case "boolean":
                        colDef.append("BOOLEAN");
                        break;
                    case "timestamp without time zone":
                        colDef.append("TIMESTAMP");
                        break;
                    case "timestamp with time zone":
                        colDef.append("TIMESTAMPTZ");
                        break;
                    case "uuid":
                        colDef.append("UUID");
                        break;
                    case "text":
                        colDef.append("TEXT");
                        break;
                    default:
                        colDef.append(dataType.toUpperCase());
                }
                
                // NULL 제약
                if ("NO".equals(isNullable)) {
                    colDef.append(" NOT NULL");
                }
                
                // 기본값 처리 (nextval는 제외하고 나중에 시퀀스 처리)
                if (defaultValue != null && !defaultValue.contains("nextval")) {
                    colDef.append(" DEFAULT ").append(defaultValue);
                }
                
                columns.add(colDef.toString());
            }
        }
        
        if (columns.isEmpty()) {
            throw new SQLException("테이블 '" + tableName + "'의 컬럼 정보를 찾을 수 없습니다.");
        }
        
        // PRIMARY KEY 정보 가져오기
        List<String> pkColumns = new ArrayList<>();
        try (PreparedStatement pkStmt = conn.prepareStatement("""
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = 'public'
              AND tc.table_name = ?
              AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY kcu.ordinal_position
            """)) {
            pkStmt.setString(1, tableName);
            ResultSet pkRs = pkStmt.executeQuery();
            while (pkRs.next()) {
                // PRIMARY KEY 컬럼명도 따옴표로 감싸기
                pkColumns.add("\"" + pkRs.getString("column_name") + "\"");
            }
        }
        
        sql.append(String.join(",\n", columns));
        if (!pkColumns.isEmpty()) {
            sql.append(",\n    PRIMARY KEY (").append(String.join(", ", pkColumns)).append(")");
        }
        sql.append("\n)");
        
        // 시퀀스 생성 및 연결 (SERIAL 타입 또는 nextval 기본값)
        try (PreparedStatement seqStmt = conn.prepareStatement("""
            SELECT column_name, column_default
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = ?
              AND column_default LIKE 'nextval%'
            """)) {
            seqStmt.setString(1, tableName);
            ResultSet seqRs = seqStmt.executeQuery();
            while (seqRs.next()) {
                String colName = seqRs.getString("column_name");
                String defaultVal = seqRs.getString("column_default");
                // 시퀀스명 추출 (nextval('sequence_name'::regclass) 형태)
                if (defaultVal != null && defaultVal.contains("nextval")) {
                    int start = defaultVal.indexOf("'") + 1;
                    int end = defaultVal.indexOf("'", start);
                    if (start > 0 && end > start) {
                        String seqName = defaultVal.substring(start, end);
                        // 컬럼명을 따옴표로 감싸기
                        sql.append(";\nCREATE SEQUENCE IF NOT EXISTS ").append(seqName)
                           .append(" OWNED BY ").append(tableName).append(".\"").append(colName).append("\"")
                           .append(";\nALTER TABLE ").append(tableName)
                           .append(" ALTER COLUMN \"").append(colName).append("\"")
                           .append(" SET DEFAULT nextval('").append(seqName).append("')");
                    }
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * 원격 서버에 데이터 삽입
     */
    private void insertDataToRemote(Connection remoteConn, String tableName, List<Map<String, Object>> data) throws SQLException {
        if (data.isEmpty()) return;
        
        // 첫 번째 행으로 컬럼 정보 가져오기
        Map<String, Object> firstRow = data.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());
        
        // INSERT 문 생성 (컬럼명을 따옴표로 감싸서 PostgreSQL 예약어 문제 해결)
        String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
        // 모든 컬럼명을 따옴표로 감싸기
        List<String> quotedColumns = new ArrayList<>();
        for (String column : columns) {
            quotedColumns.add("\"" + column + "\"");
        }
        String columnNames = String.join(", ", quotedColumns);
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnNames, placeholders);
        
        try (PreparedStatement insertStmt = remoteConn.prepareStatement(insertSql)) {
            remoteConn.setAutoCommit(false);
            
            for (Map<String, Object> row : data) {
                for (int i = 0; i < columns.size(); i++) {
                    Object value = row.get(columns.get(i));
                    insertStmt.setObject(i + 1, value);
                }
                insertStmt.addBatch();
            }
            
            insertStmt.executeBatch();
            remoteConn.commit();
            remoteConn.setAutoCommit(true);
        }
    }
    
    /**
     * 로컬 데이터베이스 연결
     */
    private Connection getLocalConnection() throws SQLException {
        String url = localDbUrl;
        if (!url.contains("?")) {
            url += "?characterEncoding=utf8&useUnicode=true&stringtype=unspecified";
        } else if (!url.contains("characterEncoding")) {
            url += "&characterEncoding=utf8&useUnicode=true&stringtype=unspecified";
        }
        Connection conn = DriverManager.getConnection(url, localDbUsername, localDbPassword);
        // UTF-8 인코딩 명시
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("SET client_encoding TO 'UTF8'");
        }
        return conn;
    }
    
    /**
     * 원격 데이터베이스 연결
     */
    private Connection getRemoteConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s?characterEncoding=utf8&useUnicode=true&stringtype=unspecified", 
            remoteDbHost, remoteDbPort, remoteDbDatabase);
        Connection conn = DriverManager.getConnection(url, remoteDbUsername, remoteDbPassword);
        // UTF-8 인코딩 명시
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("SET client_encoding TO 'UTF8'");
        }
        return conn;
    }
}

