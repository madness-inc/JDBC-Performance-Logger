package ch.sla.jdbcperflogger.console.db;

import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.ID_COLUMN;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.TxCompletionType;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

public class LogRepositoryReadJdbcTest extends AbstractLogRepositoryTest {
    @Test
    public void testcountStatements() {
        insert1Log();
        assertEquals(1, repositoryRead.countStatements());
    }

    @Test
    public void testgetTotalExecAndFetchTimeNanos() {
        final StatementLog stmtLog = insert1Log();
        final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(stmtLog.getLogId(), 123, 567L,
                "myexception");
        repositoryUpdate.updateLogAfterExecution(statementExecutedLog);
        final ResultSetLog resultSetLog = new ResultSetLog(stmtLog.getLogId(), 321, 765);
        repositoryUpdate.updateLogWithResultSetLog(resultSetLog);

        assertEquals(statementExecutedLog.getExecutionTimeNanos() + resultSetLog.getResultSetIterationTimeNanos(),
                repositoryRead.getTotalExecAndFetchTimeNanos());

        {
            final LogSearchCriteria searchCriteria = new LogSearchCriteria();
            assertEquals(statementExecutedLog.getExecutionTimeNanos() + resultSetLog.getResultSetIterationTimeNanos(),
                    repositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria));
        }
        {
            final LogSearchCriteria searchCriteria = new LogSearchCriteria();
            searchCriteria.setSqlPassThroughFilter("error is null");
            assertEquals(0, repositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria));
        }
        {
            final LogSearchCriteria searchCriteria = new LogSearchCriteria();
            searchCriteria.setFilter("myrawsql");
            assertEquals(statementExecutedLog.getExecutionTimeNanos() + resultSetLog.getResultSetIterationTimeNanos(),
                    repositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria));
            searchCriteria.setFilter("myrawsql2");
            assertEquals(0, repositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria));
        }
        {
            final LogSearchCriteria searchCriteria = new LogSearchCriteria();
            searchCriteria.setMinDurationNanos(123L + 321L - 1);
            assertEquals(statementExecutedLog.getExecutionTimeNanos() + resultSetLog.getResultSetIterationTimeNanos(),
                    repositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria));
            searchCriteria.setMinDurationNanos(123L + 321L);
            assertEquals(0, repositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria));
        }

    }

    @Test
    public void testgetStatementsGroupByRawSQL_noCriteria() {
        final List<StatementFullyExecutedLog> fullLogs = insert3Logs();

        repositoryRead.getStatementsGroupByRawSQL(new LogSearchCriteria(), new ResultSetAnalyzer() {

            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                {
                    resultSet.next();
                    final StatementFullyExecutedLog stmtLog1 = fullLogs.get(0);
                    final StatementFullyExecutedLog stmtLog2 = fullLogs.get(1);
                    assertEquals(1, resultSet.getLong(ID_COLUMN));
                    assertEquals(stmtLog1.getStatementType().getId(),
                            resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                    assertEquals(2, resultSet.getLong(LogRepositoryConstants.EXEC_COUNT_COLUMN));
                    assertEquals(stmtLog1.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                    assertEquals(stmtLog1.getExecutionTimeNanos() + stmtLog2.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.TOTAL_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog2.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MAX_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog1.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MIN_EXEC_TIME_COLUMN));
                    assertEquals((stmtLog1.getExecutionTimeNanos() + stmtLog2.getExecutionTimeNanos()) / 2,
                            resultSet.getInt(LogRepositoryConstants.AVG_EXEC_TIME_COLUMN));
                }
                {
                    resultSet.next();
                    final StatementFullyExecutedLog stmtLog = fullLogs.get(2);
                    assertEquals(3, resultSet.getLong(ID_COLUMN));
                    assertEquals(stmtLog.getStatementType().getId(),
                            resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                    assertEquals(1, resultSet.getLong(LogRepositoryConstants.EXEC_COUNT_COLUMN));
                    assertEquals(stmtLog.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.TOTAL_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MAX_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MIN_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.AVG_EXEC_TIME_COLUMN));
                }
            }
        });
    }

    @Test
    public void testgetStatementsGroupByFilledSQL_noCriteria() {
        final List<StatementFullyExecutedLog> fullLogs = insert3Logs();

        repositoryRead.getStatementsGroupByFilledSQL(new LogSearchCriteria(), new ResultSetAnalyzer() {

            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                {
                    resultSet.next();
                    final StatementFullyExecutedLog stmtLog1 = fullLogs.get(0);
                    final StatementFullyExecutedLog stmtLog2 = fullLogs.get(1);
                    assertEquals(1, resultSet.getLong(ID_COLUMN));
                    assertEquals(stmtLog1.getStatementType().getId(),
                            resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                    assertEquals(2, resultSet.getLong(LogRepositoryConstants.EXEC_COUNT_COLUMN));
                    assertEquals(stmtLog1.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                    assertEquals(stmtLog1.getFilledSql(), resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN));
                    assertEquals(stmtLog1.getExecutionTimeNanos() + stmtLog2.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.TOTAL_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog2.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MAX_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog1.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MIN_EXEC_TIME_COLUMN));
                    assertEquals((stmtLog1.getExecutionTimeNanos() + stmtLog2.getExecutionTimeNanos()) / 2,
                            resultSet.getInt(LogRepositoryConstants.AVG_EXEC_TIME_COLUMN));
                }
                {
                    resultSet.next();
                    final StatementFullyExecutedLog stmtLog = fullLogs.get(2);
                    assertEquals(3, resultSet.getLong(ID_COLUMN));
                    assertEquals(stmtLog.getStatementType().getId(),
                            resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                    assertEquals(1, resultSet.getLong(LogRepositoryConstants.EXEC_COUNT_COLUMN));
                    assertEquals(stmtLog.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                    assertEquals(stmtLog.getFilledSql(), resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.TOTAL_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MAX_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.MIN_EXEC_TIME_COLUMN));
                    assertEquals(stmtLog.getExecutionTimeNanos(),
                            resultSet.getInt(LogRepositoryConstants.AVG_EXEC_TIME_COLUMN));
                }

            }
        });
    }

    @Test
    public void testgetStatementsGroupByRawSQL_filterByText() {
        insert3Logs();

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setFilter("toto");
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setFilter("MYRAWSQL");
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 2 grouped rows
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setFilter("myrawsql2");
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 1 grouped row
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setFilter("myfilled");
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 2 grouped row
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByFilledSQL_filterByText() {
        insert3Logs();

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setFilter("toto");
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setFilter("MYRAWSQL");
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 2 grouped rows
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setFilter("myrawsql2");
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 1 grouped row
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setFilter("myfilled");
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 2 grouped row
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByRawSQL_filterByMinDuration() {
        insert3Logs();

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setMinDurationNanos(1000L);
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 1 grouped row
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setMinDurationNanos(10000L);
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByFilledSQL_filterByMinDuration() {
        insert3Logs();

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setMinDurationNanos(1000L);
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 1 grouped row
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
        searchCriteria.setMinDurationNanos(10000L);
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByRawSQL_filterPassthrough() {
        insert3Logs();

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setSqlPassThroughFilter("avg_exec_time >1000");
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 1 grouped row
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByFilledSQL_filterPassthrough() {
        insert3Logs();

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setSqlPassThroughFilter("avg_exec_time >1000");
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 1 grouped row
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByRawSQL_filterCommits() {
        final List<StatementFullyExecutedLog> logs = insert3Logs();
        final StatementFullyExecutedLog log1 = logs.get(0);
        repositoryUpdate.addTxCompletionLog(new TxCompleteLog(log1.getConnectionUuid(), System.currentTimeMillis(),
                TxCompletionType.COMMIT, 321, "mythread", null));

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setRemoveTransactionCompletions(false);
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 3 grouped rows
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });

        searchCriteria.setRemoveTransactionCompletions(true);
        repositoryRead.getStatementsGroupByRawSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 2 grouped rows
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testgetStatementsGroupByFilledSQL_filterCommits() {
        final List<StatementFullyExecutedLog> logs = insert3Logs();
        final StatementFullyExecutedLog log1 = logs.get(0);
        repositoryUpdate.addTxCompletionLog(new TxCompleteLog(log1.getConnectionUuid(), System.currentTimeMillis(),
                TxCompletionType.COMMIT, 321, "mythread", null));

        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setRemoveTransactionCompletions(false);
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 3 grouped rows
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });

        searchCriteria.setRemoveTransactionCompletions(true);
        repositoryRead.getStatementsGroupByFilledSQL(searchCriteria, new ResultSetAnalyzer() {
            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                // check only 2 grouped rows
                assertTrue(resultSet.next());
                assertTrue(resultSet.next());
                assertFalse(resultSet.next());
            }
        });
    }

    @Test
    public void testaddBatchedPreparedStatementsLog() {
        final StatementLog log = insert1Log();

        final List<String> sqlList = Arrays.asList("st1", "st2", "st3");
        final BatchedPreparedStatementsLog batchedLogs = new BatchedPreparedStatementsLog(log.getConnectionUuid(),
                randomUUID(), System.currentTimeMillis(), "myRaw stmt", sqlList, "myThread", 13, true);
        repositoryUpdate.addBatchedPreparedStatementsLog(batchedLogs);
        assertEquals(2, countRowsInTable("statement_log"));
        assertEquals(3, countRowsInTable("batched_statement_log"));

        final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(batchedLogs.getLogId(), 123, null,
                "myexception");
        repositoryUpdate.updateLogAfterExecution(statementExecutedLog);

        repositoryRead.getBatchStatementExecutions(batchedLogs.getLogId(), new ResultSetAnalyzer() {

            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                {
                    assertTrue(resultSet.next());
                    assertEquals(0, resultSet.getInt(LogRepositoryConstants.BATCHED_STMT_ORDER));
                    assertEquals("st1", resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN));
                }
                {
                    assertTrue(resultSet.next());
                    assertEquals(1, resultSet.getInt(LogRepositoryConstants.BATCHED_STMT_ORDER));
                    assertEquals("st2", resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN));
                }
                {
                    assertTrue(resultSet.next());
                    assertEquals(2, resultSet.getInt(LogRepositoryConstants.BATCHED_STMT_ORDER));
                    assertEquals("st3", resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN));
                }
                assertFalse(resultSet.next());
            }
        });
    }

    private List<StatementFullyExecutedLog> insert3Logs() {
        final Properties connProps = new Properties();
        connProps.setProperty("myprop", "myval");
        final ConnectionInfo connectionInfo = new ConnectionInfo(randomUUID(), 12, "jdbc:toto", new Date(), 12,
                connProps);
        repositoryUpdate.addConnection(connectionInfo);

        final List<StatementFullyExecutedLog> fullLogs = new ArrayList<>();
        {
            final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myrawsql", "myfilledsql", Thread
                            .currentThread().getName(), 123, true);
            final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(log.getLogId(), 234L, 4560L,
                    "myexception");
            fullLogs.add(new StatementFullyExecutedLog(log, statementExecutedLog, null));
        }
        {
            final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myrawsql", "myfilledsql", Thread
                            .currentThread().getName(), 123, true);
            final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(log.getLogId(), 2340L, 456L,
                    "myexception");
            final ResultSetLog resultSetLog = new ResultSetLog(log.getLogId(), 789L, 21);
            fullLogs.add(new StatementFullyExecutedLog(log, statementExecutedLog, resultSetLog));
        }
        {
            final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myRawsql2", "myfilledsql2",
                    Thread.currentThread().getName(), 0, true);
            final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(log.getLogId(), 12L, null, null);
            fullLogs.add(new StatementFullyExecutedLog(log, statementExecutedLog, null));
        }

        repositoryUpdate.addStatementFullyExecutedLog(fullLogs);
        return fullLogs;
    }
}