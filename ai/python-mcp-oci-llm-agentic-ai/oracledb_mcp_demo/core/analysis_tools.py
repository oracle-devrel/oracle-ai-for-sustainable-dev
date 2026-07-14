"""Specialized tools for analysis tasks."""

import time
from typing import Dict, Any, List
import re

from .logging_config import get_logger

logger = get_logger(__name__)


class KnowledgeResponseGenerator:
    """Generates knowledge-based responses for analysis topics."""
    
    ORACLE_PERFORMANCE_TIPS = {
        "general": [
            "1. **Indexing Strategy**: Create proper indexes on frequently queried columns",
            "2. **Query Optimization**: Use EXPLAIN PLAN to analyze query execution paths", 
            "3. **Statistics Management**: Keep table/index statistics current with DBMS_STATS",
            "4. **Memory Tuning**: Optimize SGA (System Global Area) and PGA (Program Global Area)",
            "5. **I/O Optimization**: Use appropriate tablespace configurations and storage parameters",
            "6. **Connection Pooling**: Implement connection pooling to reduce connection overhead",
            "7. **SQL Best Practices**: Avoid SELECT *, use bind variables, minimize function calls in WHERE clauses"
        ],
        "indexing": [
            "• Create indexes on columns used in WHERE, JOIN, and ORDER BY clauses",
            "• Use composite indexes for multi-column searches", 
            "• Consider function-based indexes for expressions",
            "• Monitor index usage with V$OBJECT_USAGE",
            "• Drop unused indexes to reduce DML overhead"
        ],
        "memory": [
            "• Size SGA appropriately (typically 70-80% of available RAM)",
            "• Tune shared pool for SQL parsing and caching",
            "• Optimize buffer cache for data block caching",
            "• Configure PGA_AGGREGATE_TARGET for sort/hash operations",
            "• Use automatic memory management (AMM) for dynamic allocation"
        ],
        "sql_tuning": [
            "• Use bind variables to promote SQL sharing",
            "• Write efficient WHERE clauses with selective conditions first",
            "• Avoid implicit data type conversions",
            "• Use EXISTS instead of IN for subqueries when appropriate",
            "• Minimize context switches between SQL and PL/SQL"
        ]
    }
    
    AWR_KNOWLEDGE = {
        "basics": [
            "**AWR (Automatic Workload Repository)** is Oracle's built-in performance monitoring tool",
            "• Automatically collects performance statistics every hour by default",
            "• Stores historical performance data for trend analysis",
            "• Provides comprehensive reports on database activity, wait events, and SQL performance",
            "• Essential for database performance tuning and capacity planning"
        ],
        "usage": [
            "**Generating AWR Reports:**",
            "• Use DBMS_WORKLOAD_REPOSITORY.AWR_REPORT_TEXT() function",
            "• Specify begin and end snapshot IDs for the analysis period", 
            "• Choose appropriate time intervals (1-2 hours for problem analysis)",
            "• Focus on Top 5 Wait Events and Top SQL sections",
            "• Compare reports from different time periods to identify trends"
        ],
        "interpretation": [
            "**Key AWR Report Sections:**",
            "• **Top 5 Wait Events**: Identifies database bottlenecks",
            "• **SQL Statistics**: Shows resource-intensive SQL statements",
            "• **Instance Efficiency**: Overall database health metrics",
            "• **Time Model Statistics**: CPU and wait time breakdown",
            "• **Tablespace I/O**: Storage performance metrics"
        ]
    }
    
    def generate_performance_response(self, prompt: str) -> str:
        """Generate performance tuning response based on prompt context."""
        prompt_lower = prompt.lower()
        
        # Determine specific topic
        if any(word in prompt_lower for word in ["index", "indexing"]):
            topic_tips = self.ORACLE_PERFORMANCE_TIPS["indexing"]
            topic_title = "Oracle Index Optimization"
        elif any(word in prompt_lower for word in ["memory", "sga", "pga"]):
            topic_tips = self.ORACLE_PERFORMANCE_TIPS["memory"] 
            topic_title = "Oracle Memory Tuning"
        elif any(word in prompt_lower for word in ["sql", "query", "tuning"]):
            topic_tips = self.ORACLE_PERFORMANCE_TIPS["sql_tuning"]
            topic_title = "Oracle SQL Tuning"
        else:
            topic_tips = self.ORACLE_PERFORMANCE_TIPS["general"]
            topic_title = "Oracle Performance Tuning"
        
        response = f"""🚀 **{topic_title} Best Practices**

{chr(10).join(topic_tips)}

💡 **Quick Performance Health Check:**
• Check V$SYSTEM_EVENT for top wait events
• Review V$SQL for high resource consumption queries  
• Monitor V$SGA_DYNAMIC_COMPONENTS for memory usage
• Use V$SESSION_LONGOPS for long-running operations

📊 **Recommended Tools:**
• Oracle Enterprise Manager (OEM) for comprehensive monitoring
• AWR reports for historical analysis
• SQL Tuning Advisor for query optimization
• SQL Access Advisor for index recommendations

For specific performance issues, provide more details about your symptoms or use AWR analysis for detailed database metrics."""
        
        return response
    
    def generate_awr_response(self, prompt: str) -> str:
        """Generate AWR knowledge response based on prompt context."""
        prompt_lower = prompt.lower()
        
        if any(word in prompt_lower for word in ["how", "use", "generate"]):
            topic_content = self.AWR_KNOWLEDGE["usage"]
            topic_title = "How to Use AWR Reports"
        elif any(word in prompt_lower for word in ["interpret", "read", "understand"]):
            topic_content = self.AWR_KNOWLEDGE["interpretation"] 
            topic_title = "AWR Report Interpretation"
        else:
            topic_content = self.AWR_KNOWLEDGE["basics"]
            topic_title = "AWR (Automatic Workload Repository) Overview"
        
        response = f"""📊 **{topic_title}**

{chr(10).join(topic_content)}

🔧 **AWR Configuration:**
• Snapshot interval: EXEC DBMS_WORKLOAD_REPOSITORY.MODIFY_SNAPSHOT_SETTINGS(interval=>60)
• Retention period: EXEC DBMS_WORKLOAD_REPOSITORY.MODIFY_SNAPSHOT_SETTINGS(retention=>10080)
• Manual snapshot: EXEC DBMS_WORKLOAD_REPOSITORY.CREATE_SNAPSHOT()

📈 **AWR Best Practices:**
• Collect snapshots during peak and off-peak hours
• Use consistent time intervals for trend analysis
• Focus on Delta values rather than absolute numbers
• Compare similar workload periods for accurate analysis

To generate an actual AWR report from your database, use: "generate awr report" or "analyze awr data"."""
        
        return response


class PerformanceAnalyzer:
    """Analyzes current database performance metrics."""
    
    def __init__(self, mcp_session):
        """Initialize with MCP session for database queries."""
        self.mcp_session = mcp_session
    
    async def analyze_current_performance(self, prompt: str) -> str:
        """Analyze current database performance."""
        logger.info("Analyzing current database performance")
        
        try:
            # Quick performance check queries
            queries = {
                "wait_events": """
                    SELECT event, total_waits, time_waited_micro/1000000 as time_waited_sec
                    FROM v$system_event 
                    WHERE wait_class != 'Idle' 
                    ORDER BY time_waited_micro DESC 
                    FETCH FIRST 5 ROWS ONLY
                """,
                "top_sql": """
                    SELECT sql_id, executions, elapsed_time/1000000 as elapsed_sec, 
                           buffer_gets, disk_reads
                    FROM v$sql 
                    WHERE elapsed_time > 0
                    ORDER BY elapsed_time DESC 
                    FETCH FIRST 5 ROWS ONLY
                """,
                "session_counts": """
                    SELECT status, count(*) as session_count
                    FROM v$session 
                    GROUP BY status
                """,
                "memory_usage": """
                    SELECT component, current_size/1024/1024 as size_mb
                    FROM v$sga_dynamic_components
                    WHERE current_size > 0
                    ORDER BY current_size DESC
                """
            }
            
            results = {}
            for name, sql in queries.items():
                try:
                    result = await self.mcp_session.call_tool("mcp_oracle-sqlcl-mcp_run-sql", {"sql": sql})
                    results[name] = self._extract_result_text(result)
                except Exception as e:
                    logger.warning(f"Failed to execute {name} query: {e}")
                    results[name] = f"Query failed: {str(e)}"
            
            # Format comprehensive response
            response = f"""🔍 **Current Database Performance Analysis**

**📊 Top Wait Events:**
{results.get('wait_events', 'No data available')}

**⚡ Top Resource-Consuming SQL:**
{results.get('top_sql', 'No data available')}

**👥 Session Activity:**
{results.get('session_counts', 'No data available')}

**💾 Memory Components:**
{results.get('memory_usage', 'No data available')}

**💡 Quick Recommendations:**
• Monitor the top wait events for bottlenecks
• Review high elapsed time SQL statements  
• Check for excessive active sessions
• Ensure memory components are appropriately sized

For historical analysis and trending, generate an AWR report: "generate awr report"
"""
            
            return response
            
        except Exception as e:
            logger.error(f"Performance analysis failed: {e}")
            return f"❌ Failed to analyze current performance: {str(e)}"
    
    def _extract_result_text(self, result) -> str:
        """Extract text content from MCP result."""
        if hasattr(result, 'content') and result.content:
            text_parts = []
            for content in result.content:
                if hasattr(content, 'text'):
                    text_parts.append(str(content.text))
            return '\n'.join(text_parts)
        return str(result)


class AWRReportGenerator:
    """Generates AWR reports using database snapshots."""
    
    def __init__(self, mcp_session):
        """Initialize with MCP session for database operations."""
        self.mcp_session = mcp_session
    
    async def generate_awr_report(self, prompt: str, snapshots_data: str) -> str:
        """Generate AWR report from snapshot data."""
        logger.info("Generating AWR report from snapshots")
        
        try:
            # Parse snapshot IDs from the data
            snap_ids = self._parse_snapshot_ids(snapshots_data)
            if len(snap_ids) < 2:
                return "❌ Insufficient snapshots available for AWR analysis"
            
            # Use the two most recent snapshots
            begin_snap = snap_ids[1]  # Earlier snapshot
            end_snap = snap_ids[0]    # Latest snapshot
            
            logger.info(f"Generating AWR report for snapshots {begin_snap} to {end_snap}")
            
            # Generate AWR report using Oracle's built-in function
            awr_sql = f"""SELECT output FROM TABLE(DBMS_WORKLOAD_REPOSITORY.awr_report_text(
  (SELECT dbid FROM v$database), 
  (SELECT instance_number FROM v$instance), 
  {begin_snap}, 
  {end_snap}
))"""
            
            # Execute the AWR query
            logger.info("Executing AWR report SQL")
            result = await self.mcp_session.call_tool("mcp_oracle-sqlcl-mcp_run-sql", {"sql": awr_sql})
            
            # Extract the report content
            awr_content = self._extract_result_text(result)
            
            return f"""🔍 **AWR Analysis Results (Snapshots {begin_snap} → {end_snap})**

{awr_content.strip()}

📊 **Analysis Summary:**
- Report generated for snapshot range: {begin_snap} to {end_snap}
- This AWR report contains detailed Oracle database performance metrics
- Key areas to review: Top SQL, Wait Events, System Statistics, and Resource Usage
- Focus on sections with highest time/resource consumption for tuning opportunities
"""
            
        except Exception as e:
            logger.error(f"Failed to generate AWR report: {e}")
            return f"❌ Failed to generate AWR analysis: {str(e)}"
    
    def _parse_snapshot_ids(self, snapshot_data: str) -> List[int]:
        """Parse snapshot IDs from Oracle output."""
        snap_ids = []
        if not snapshot_data:
            return snap_ids
            
        logger.debug(f"Parsing snapshot data: {snapshot_data[:200]}...")
        
        # Split into lines and look for snapshot ID patterns
        lines = snapshot_data.split('\n')
        for line in lines:
            # Skip header lines and empty lines
            if ('|' in line and line.strip() and 
                not line.startswith('SNAP_ID') and 
                not line.startswith('-') and
                not '(' in line):
                
                parts = line.split('|')
                if len(parts) >= 1:
                    try:
                        snap_id_str = parts[0].strip()
                        if snap_id_str.isdigit():
                            snap_id = int(snap_id_str)
                            if snap_id not in snap_ids:
                                snap_ids.append(snap_id)
                    except (ValueError, IndexError):
                        continue
        
        # Return unique IDs in descending order
        unique_ids = sorted(list(set(snap_ids)), reverse=True)
        logger.info(f"Parsed {len(unique_ids)} unique snapshot IDs: {unique_ids}")
        return unique_ids
    
    def _extract_result_text(self, result) -> str:
        """Extract text content from MCP result."""
        if hasattr(result, 'content') and result.content:
            text_parts = []
            for content in result.content:
                if hasattr(content, 'text'):
                    text_parts.append(str(content.text))
            return '\n'.join(text_parts)
        return str(result)