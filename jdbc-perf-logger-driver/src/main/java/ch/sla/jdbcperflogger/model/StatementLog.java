/* 
 *  Copyright 2013 Sylvain LAURENT
 *     
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.model;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ch.sla.jdbcperflogger.StatementType;

@ParametersAreNonnullByDefault
public class StatementLog extends AbstractLogMessage {

    private static final long serialVersionUID = 1L;

    private final String rawSql;
    private final String filledSql;
    private final boolean preparedStatement;

    public StatementLog(final int connectionId, final UUID logId, final long timestamp, final long executionTimeNanos,
            final StatementType statementType, final String sql, final String threadName,
            @Nullable final Throwable sqlException) {
        super(connectionId, logId, timestamp, executionTimeNanos, statementType, threadName, sqlException);
        rawSql = sql;
        filledSql = sql;
        preparedStatement = false;
    }

    public StatementLog(final int connectionId, final UUID logId, final long timestamp, final long executionTimeNanos,
            final StatementType statementType, final String rawSql, final String filledSql, final String threadName,
            @Nullable final Throwable sqlException) {
        super(connectionId, logId, timestamp, executionTimeNanos, statementType, threadName, sqlException);
        this.rawSql = rawSql;
        this.filledSql = filledSql;
        preparedStatement = true;
    }

    public String getRawSql() {
        return rawSql;
    }

    public String getFilledSql() {
        return filledSql;
    }

    public boolean isPreparedStatement() {
        return preparedStatement;
    }

}