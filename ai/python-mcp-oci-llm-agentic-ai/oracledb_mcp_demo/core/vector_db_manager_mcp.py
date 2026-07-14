"""Vector Database Manager using MCP Server Connection."""

import logging
import asyncio
from typing import Dict, List, Any, Optional, Union, Tuple
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
import uuid
import json

from vector_db.models import FeedbackRecord, SearchResult, SearchFilters, FeedbackStatus
from vector_db.rag_retriever import RAGRetriever
from vector_db.feedback_repository import FeedbackRepository
from vector_db.oracle_vector_store import OracleVectorStore
from .performance_monitor import PerformanceMonitor
from .config import get_config_value

logger = logging.getLogger(__name__)


class InformationType(Enum):
    """Types of information that can be stored."""
    FEEDBACK_RECORD = "feedback_record"
    KNOWLEDGE_ARTICLE = "knowledge_article"
    BEST_PRACTICE = "best_practice"
    TROUBLESHOOTING_GUIDE = "troubleshooting_guide"
    CODE_EXAMPLE = "code_example"
    DOCUMENTATION = "documentation"
    USER_QUERY = "user_query"
    SYSTEM_LOG = "system_log"


class InformationSource(Enum):
    """Sources of information."""
    USER_INPUT = "user_input"
    SYSTEM_GENERATED = "system_generated"
    IMPORTED = "imported"
    API_INTEGRATION = "api_integration"
    BATCH_PROCESSING = "batch_processing"
    FEEDBACK_LOOP = "feedback_loop"


@dataclass
class InformationMetadata:
    """Metadata for information records."""
    title: Optional[str] = None
    description: Optional[str] = None
    tags: List[str] = field(default_factory=list)
    category: Optional[str] = None
    priority: int = 1  # 1-5 scale
    version: str = "1.0"
    author: Optional[str] = None
    source: InformationSource = InformationSource.USER_INPUT
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    updated_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    expires_at: Optional[datetime] = None
    confidence_score: float = 1.0
    review_status: FeedbackStatus = FeedbackStatus.SUBMITTED
    external_references: List[str] = field(default_factory=list)
    custom_fields: Dict[str, Any] = field(default_factory=dict)


@dataclass
class InformationRecord:
    """Complete information record for vector database."""
    content: str
    content_type: InformationType
    metadata: InformationMetadata
    record_id: Optional[str] = None
    embedding_vector: Optional[List[float]] = None
    
    def __post_init__(self):
        """Generate record ID if not provided."""
        if self.record_id is None:
            self.record_id = str(uuid.uuid4())
    
    def to_feedback_record(self) -> FeedbackRecord:
        """Convert to FeedbackRecord for storage."""
        return FeedbackRecord(
            prompt=self.content,
            corrected_answer=None,
            context={
                'content_type': self.content_type.value,
                'title': self.metadata.title,
                'description': self.metadata.description,
                'category': self.metadata.category,
                'priority': self.metadata.priority,
                'version': self.metadata.version,
                'author': self.metadata.author,
                'source': self.metadata.source.value,
                'confidence_score': self.metadata.confidence_score,
                'external_references': self.metadata.external_references,
                'custom_fields': self.metadata.custom_fields
            },
            status=self.metadata.review_status,
            tags=self.metadata.tags,
            record_id=self.record_id,
            created_at=self.metadata.created_at,
            updated_at=self.metadata.updated_at
        )


class MCPVectorDBManager:
    """Vector Database Manager using MCP Server Connection."""
    
    def __init__(self, mcp_session=None, performance_monitor: Optional[PerformanceMonitor] = None):
        """Initialize MCP vector database manager.
        
        Args:
            mcp_session: MCP session for database operations
            performance_monitor: Optional performance monitor
        """
        self.mcp_session = mcp_session
        self.performance_monitor = performance_monitor
        
        logger.info("MCP Vector Database Manager initialized")
    
    async def add_information(self, information: InformationRecord) -> bool:
        """Add new information to the vector database using MCP server.
        
        Args:
            information: Information record to add
            
        Returns:
            True if successful, False otherwise
        """
        try:
            logger.info(f"Adding information via MCP: {information.metadata.title or information.content[:50]}...")
            
            if not self.mcp_session:
                logger.error("No MCP session available")
                return False
            
            # Convert to feedback record
            feedback_record = information.to_feedback_record()
            
            # Create SQL to insert the information
            insert_sql = self._create_insert_sql(feedback_record)
            
            # Execute via MCP server
            result = await self.mcp_session.call_tool("mcp_oracle-sqlcl-mcp_run-sql", {
                "sql": insert_sql
            })
            
            if result and "success" in str(result).lower():
                logger.info(f"Successfully added information with ID: {information.record_id}")
                return True
            else:
                logger.error(f"Failed to add information via MCP: {result}")
                return False
                
        except Exception as e:
            logger.error(f"Error adding information via MCP: {e}")
            return False
    
    async def batch_add_information(self, information_list: List[InformationRecord]) -> Dict[str, int]:
        """Add multiple information records in batch via MCP server.
        
        Args:
            information_list: List of information records to add
            
        Returns:
            Dictionary with success/failure counts
        """
        results = {
            'total': len(information_list),
            'successful': 0,
            'failed': 0,
            'errors': []
        }
        
        logger.info(f"Starting batch addition of {len(information_list)} information records via MCP")
        
        # Process in batches to avoid overwhelming the system
        batch_size = get_config_value('batch.mcp_batch_size', 5)  # From config
        for i in range(0, len(information_list), batch_size):
            batch = information_list[i:i + batch_size]
            
            # Create batch insert SQL
            batch_sql = self._create_batch_insert_sql(batch)
            
            try:
                # Execute batch insert via MCP server
                result = await self.mcp_session.call_tool("mcp_oracle-sqlcl-mcp_run-sql", {
                    "sql": batch_sql
                })
                
                if result and "success" in str(result).lower():
                    results['successful'] += len(batch)
                else:
                    results['failed'] += len(batch)
                    results['errors'].append(f"Batch {i//batch_size + 1}: {result}")
                    
            except Exception as e:
                results['failed'] += len(batch)
                results['errors'].append(f"Batch {i//batch_size + 1}: {str(e)}")
        
        logger.info(f"Batch addition completed: {results['successful']} successful, {results['failed']} failed")
        return results
    
    async def search_information(
        self, 
        query: str, 
        k: int = 4,
        filters: Optional[Dict[str, Any]] = None,
        information_types: Optional[List[InformationType]] = None
    ) -> List[InformationRecord]:
        """Search for information using MCP server.
        
        Args:
            query: Search query
            k: Number of results to return
            filters: Additional search filters
            information_types: Filter by information types
            
        Returns:
            List of matching information records
        """
        try:
            logger.info(f"Searching for information via MCP: {query[:50]}...")
            
            if not self.mcp_session:
                logger.error("No MCP session available")
                return []
            
            # Create search SQL
            search_sql = self._create_search_sql(query, k, filters, information_types)
            
            # Execute search via MCP server
            result = await self.mcp_session.call_tool("mcp_oracle-sqlcl-mcp_run-sql", {
                "sql": search_sql
            })
            
            # Parse results
            information_records = self._parse_search_results(result)
            
            logger.info(f"Found {len(information_records)} matching information records via MCP")
            return information_records
            
        except Exception as e:
            logger.error(f"Error searching information via MCP: {e}")
            return []
    
    async def get_database_stats(self) -> Dict[str, Any]:
        """Get database statistics using MCP server.
        
        Returns:
            Dictionary with database statistics
        """
        try:
            if not self.mcp_session:
                logger.error("No MCP session available")
                return {}
            
            # Create stats SQL
            stats_sql = """
            SELECT 
                COUNT(*) as total_records,
                COUNT(CASE WHEN context IS NOT NULL AND JSON_VALUE(context, '$.content_type') IS NOT NULL THEN 1 END) as typed_records,
                COUNT(CASE WHEN context IS NOT NULL AND JSON_VALUE(context, '$.embedding_vector') IS NOT NULL THEN 1 END) as embedded_records
            FROM hitl_records
            """
            
            # Execute via MCP server
            result = await self.mcp_session.call_tool("mcp_oracle-sqlcl-mcp_run-sql", {
                "sql": stats_sql
            })
            
            # Parse results
            stats = self._parse_stats_results(result)
            
            return stats
            
        except Exception as e:
            logger.error(f"Error getting database stats via MCP: {e}")
            return {}
    
    def _create_insert_sql(self, feedback_record: FeedbackRecord) -> str:
        """Create SQL for inserting a single feedback record."""
        context_json = json.dumps(feedback_record.context or {})
        tags_json = json.dumps(feedback_record.tags or [])
        
        sql = f"""
        INSERT INTO hitl_records (
            record_id, prompt, corrected_answer, context, tags, 
            status, created_at, updated_at
        ) VALUES (
            '{feedback_record.record_id}',
            '{feedback_record.prompt.replace("'", "''")}',
            {f"'{feedback_record.corrected_answer.replace("'", "''")}'" if feedback_record.corrected_answer else 'NULL'},
            '{context_json}',
            '{tags_json}',
            '{feedback_record.status.value}',
            TO_TIMESTAMP('{feedback_record.created_at.isoformat()}', 'YYYY-MM-DD"T"HH24:MI:SS.FF6"Z"'),
            TO_TIMESTAMP('{feedback_record.updated_at.isoformat()}', 'YYYY-MM-DD"T"HH24:MI:SS.FF6"Z"')
        )
        """
        return sql
    
    def _create_batch_insert_sql(self, information_list: List[InformationRecord]) -> str:
        """Create SQL for batch inserting multiple information records."""
        if not information_list:
            return ""
        
        values = []
        for info in information_list:
            feedback_record = info.to_feedback_record()
            context_json = json.dumps(feedback_record.context or {})
            tags_json = json.dumps(feedback_record.tags or [])
            
            value = f"""(
                '{feedback_record.record_id}',
                '{feedback_record.prompt.replace("'", "''")}',
                {f"'{feedback_record.corrected_answer.replace("'", "''")}'" if feedback_record.corrected_answer else 'NULL'},
                '{context_json}',
                '{tags_json}',
                '{feedback_record.status.value}',
                TO_TIMESTAMP('{feedback_record.created_at.isoformat()}', 'YYYY-MM-DD"T"HH24:MI:SS.FF6"Z"'),
                TO_TIMESTAMP('{feedback_record.updated_at.isoformat()}', 'YYYY-MM-DD"T"HH24:MI:SS.FF6"Z"')
            )"""
            values.append(value)
        
        sql = f"""
        INSERT ALL
        {' '.join([f'INTO hitl_records (record_id, prompt, corrected_answer, context, tags, status, created_at, updated_at) VALUES {v}' for v in values])}
        SELECT * FROM dual
        """
        return sql
    
    def _create_search_sql(self, query: str, k: int, filters: Optional[Dict[str, Any]], information_types: Optional[List[InformationType]]) -> str:
        """Create SQL for searching information."""
        sql = f"""
        SELECT 
            record_id, prompt, corrected_answer, context, tags, created_at
        FROM hitl_records
        WHERE 1=1
        """
        
        # Add content type filter
        if information_types:
            type_values = [f"'{t.value}'" for t in information_types]
            sql += f" AND JSON_VALUE(context, '$.content_type') IN ({', '.join(type_values)})"
        
        # Add text search
        if query:
            sql += f" AND (LOWER(prompt) LIKE LOWER('%{query.replace("'", "''")}%') OR LOWER(context) LIKE LOWER('%{query.replace("'", "''")}%'))"
        
        # Add limit
        sql += f" ORDER BY created_at DESC FETCH FIRST {k} ROWS ONLY"
        
        return sql
    
    def _parse_search_results(self, result) -> List[InformationRecord]:
        """Parse search results from MCP server response."""
        information_records = []
        
        try:
            # This is a simplified parser - you might need to adjust based on actual MCP response format
            if result and hasattr(result, 'content'):
                # Parse the result content
                logger.info(f"Parsing search results: {result.content[:200]}...")
                
                # For now, return empty list - you'll need to implement proper parsing
                # based on the actual MCP response format
                pass
                
        except Exception as e:
            logger.error(f"Error parsing search results: {e}")
        
        return information_records
    
    def _parse_stats_results(self, result) -> Dict[str, Any]:
        """Parse database statistics from MCP server response."""
        stats = {
            'total_records': 0,
            'typed_records': 0,
            'embedded_records': 0
        }
        
        try:
            # This is a simplified parser - you might need to adjust based on actual MCP response format
            if result and hasattr(result, 'content'):
                logger.info(f"Parsing stats results: {result.content[:200]}...")
                
                # For now, return default stats - you'll need to implement proper parsing
                # based on the actual MCP response format
                pass
                
        except Exception as e:
            logger.error(f"Error parsing stats results: {e}")
        
        return stats


class InformationImporter:
    """Utility class for importing information using MCP server."""
    
    def __init__(self, mcp_vector_db_manager: MCPVectorDBManager):
        """Initialize information importer.
        
        Args:
            mcp_vector_db_manager: MCP vector database manager instance
        """
        self.mcp_vector_db_manager = mcp_vector_db_manager
    
    async def import_from_text_file(self, file_path: str, content_type: InformationType, metadata: InformationMetadata) -> bool:
        """Import information from a text file using MCP server."""
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                content = file.read()
            
            information = InformationRecord(
                content=content,
                content_type=content_type,
                metadata=metadata
            )
            
            return await self.mcp_vector_db_manager.add_information(information)
            
        except Exception as e:
            logger.error(f"Error importing from text file via MCP: {e}")
            return False
    
    async def import_from_json_file(self, file_path: str) -> Dict[str, int]:
        """Import information from a JSON file using MCP server."""
        try:
            import json
            
            with open(file_path, 'r', encoding='utf-8') as file:
                data = json.load(file)
            
            if isinstance(data, list):
                information_list = []
                for item in data:
                    try:
                        info = self._json_to_information_record(item)
                        if info:
                            information_list.append(info)
                    except Exception as e:
                        logger.warning(f"Skipping invalid item: {e}")
                
                return await self.mcp_vector_db_manager.batch_add_information(information_list)
            else:
                # Single record
                info = self._json_to_information_record(data)
                if info:
                    success = await self.mcp_vector_db_manager.add_information(info)
                    return {'total': 1, 'successful': 1 if success else 0, 'failed': 0 if success else 1, 'errors': []}
                else:
                    return {'total': 1, 'successful': 0, 'failed': 1, 'errors': ['Invalid data format']}
            
        except Exception as e:
            logger.error(f"Error importing from JSON file via MCP: {e}")
            return {'total': 0, 'successful': 0, 'failed': 0, 'errors': [str(e)]}
    
    def _json_to_information_record(self, data: Dict[str, Any]) -> Optional[InformationRecord]:
        """Convert JSON data to InformationRecord."""
        try:
            # Extract required fields
            content = data.get('content') or data.get('text') or data.get('prompt')
            if not content:
                logger.warning("Missing content field in JSON data")
                return None
            
            # Extract content type
            content_type_str = data.get('content_type', 'feedback_record')
            try:
                content_type = InformationType(content_type_str)
            except ValueError:
                content_type = InformationType.FEEDBACK_RECORD
            
            # Extract metadata
            metadata_data = data.get('metadata', {})
            metadata = InformationMetadata(
                title=metadata_data.get('title'),
                description=metadata_data.get('description'),
                tags=metadata_data.get('tags', []),
                category=metadata_data.get('category'),
                priority=metadata_data.get('priority', 1),
                version=metadata_data.get('version', '1.0'),
                author=metadata_data.get('author'),
                source=InformationSource.IMPORTED,
                custom_fields=metadata_data.get('custom_fields', {})
            )
            
            return InformationRecord(
                content=content,
                content_type=content_type,
                metadata=metadata
            )
            
        except Exception as e:
            logger.error(f"Error converting JSON to information record: {e}")
            return None
