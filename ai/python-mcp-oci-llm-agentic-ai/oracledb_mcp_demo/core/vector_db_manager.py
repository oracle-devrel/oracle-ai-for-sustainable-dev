"""Vector Database Manager for comprehensive information management."""

import logging
import asyncio
from typing import Dict, List, Any, Optional, Union, Tuple
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
import uuid

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


class VectorDBManager:
    """Comprehensive manager for vector database operations."""
    
    def __init__(self, rag_retriever: RAGRetriever, performance_monitor: Optional[PerformanceMonitor] = None):
        """Initialize vector database manager.
        
        Args:
            rag_retriever: RAG retriever for database operations
            performance_monitor: Optional performance monitor
        """
        self.rag_retriever = rag_retriever
        self.vector_store = rag_retriever.vector_store
        self.feedback_repository = rag_retriever.feedback_repository
        self.performance_monitor = performance_monitor
        
        logger.info("Vector Database Manager initialized")
    
    async def add_information(self, information: InformationRecord) -> bool:
        """Add new information to the vector database.
        
        Args:
            information: Information record to add
            
        Returns:
            True if successful, False otherwise
        """
        try:
            logger.info(f"Adding information: {information.metadata.title or information.content[:50]}...")
            
            # Convert to feedback record
            feedback_record = information.to_feedback_record()
            
            # Generate embedding if not provided
            if not information.embedding_vector:
                embedding_vector = await self._generate_embedding(information.content)
                if embedding_vector:
                    feedback_record.context['embedding_vector'] = embedding_vector
            
            # Store in feedback repository
            success = await self.feedback_repository.create_feedback(feedback_record)
            
            if success:
                logger.info(f"Successfully added information with ID: {information.record_id}")
                return True
            else:
                logger.error(f"Failed to add information to feedback repository")
                return False
                
        except Exception as e:
            logger.error(f"Error adding information: {e}")
            return False
    
    async def batch_add_information(self, information_list: List[InformationRecord]) -> Dict[str, int]:
        """Add multiple information records in batch.
        
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
        
        logger.info(f"Starting batch addition of {len(information_list)} information records")
        
        # Process in batches to avoid overwhelming the system
        batch_size = get_config_value('batch.vector_db_batch_size', 10)  # From config
        for i in range(0, len(information_list), batch_size):
            batch = information_list[i:i + batch_size]
            
            # Process batch concurrently
            batch_tasks = [self.add_information(info) for info in batch]
            batch_results = await asyncio.gather(*batch_tasks, return_exceptions=True)
            
            for j, result in enumerate(batch_results):
                if isinstance(result, Exception):
                    results['failed'] += 1
                    results['errors'].append(f"Record {i+j}: {str(result)}")
                elif result:
                    results['successful'] += 1
                else:
                    results['failed'] += 1
        
        logger.info(f"Batch addition completed: {results['successful']} successful, {results['failed']} failed")
        return results
    
    async def update_information(self, record_id: str, updates: Dict[str, Any]) -> bool:
        """Update existing information in the vector database.
        
        Args:
            record_id: ID of the record to update
            updates: Dictionary of fields to update
            
        Returns:
            True if successful, False otherwise
        """
        try:
            logger.info(f"Updating information record: {record_id}")
            
            # Get existing record
            existing_record = await self.feedback_repository.get_feedback(record_id)
            if not existing_record:
                logger.error(f"Record not found: {record_id}")
                return False
            
            # Update fields
            for field, value in updates.items():
                if hasattr(existing_record, field):
                    setattr(existing_record, field, value)
                elif field in existing_record.context:
                    existing_record.context[field] = value
            
            # Update timestamp
            existing_record.updated_at = datetime.now(timezone.utc)
            
            # Regenerate embedding if content changed
            if 'prompt' in updates:
                new_embedding = await self._generate_embedding(updates['prompt'])
                if new_embedding:
                    existing_record.context['embedding_vector'] = new_embedding
            
            # Save updated record
            success = await self.feedback_repository.update_feedback(existing_record)
            
            if success:
                logger.info(f"Successfully updated information record: {record_id}")
                return True
            else:
                logger.error(f"Failed to update information record: {record_id}")
                return False
                
        except Exception as e:
            logger.error(f"Error updating information: {e}")
            return False
    
    async def delete_information(self, record_id: str) -> bool:
        """Delete information from the vector database.
        
        Args:
            record_id: ID of the record to delete
            
        Returns:
            True if successful, False otherwise
        """
        try:
            logger.info(f"Deleting information record: {record_id}")
            
            # Soft delete by marking as archived
            success = await self.feedback_repository.update_feedback_status(
                record_id, FeedbackStatus.ARCHIVED
            )
            
            if success:
                logger.info(f"Successfully archived information record: {record_id}")
                return True
            else:
                logger.error(f"Failed to archive information record: {record_id}")
                return False
                
        except Exception as e:
            logger.error(f"Error deleting information: {e}")
            return False
    
    async def search_information(
        self, 
        query: str, 
        k: int = 4,
        filters: Optional[Dict[str, Any]] = None,
        information_types: Optional[List[InformationType]] = None
    ) -> List[InformationRecord]:
        """Search for information in the vector database.
        
        Args:
            query: Search query
            k: Number of results to return
            filters: Additional search filters
            information_types: Filter by information types
            
        Returns:
            List of matching information records
        """
        try:
            logger.info(f"Searching for information: {query[:50]}...")
            
            # Build search filters
            search_filters = SearchFilters()
            if filters:
                for key, value in filters.items():
                    if hasattr(search_filters, key):
                        setattr(search_filters, key, value)
            
            # Add information type filters
            if information_types:
                type_values = [t.value for t in information_types]
                search_filters.tags = type_values
            
            # Execute search
            retrieval_result = await self.rag_retriever.retrieve(
                query=query,
                k=k,
                filters=search_filters
            )
            
            # Convert results to InformationRecord format
            information_records = []
            for result in retrieval_result.results:
                info_record = self._feedback_record_to_information(result)
                if info_record:
                    information_records.append(info_record)
            
            logger.info(f"Found {len(information_records)} matching information records")
            return information_records
            
        except Exception as e:
            logger.error(f"Error searching information: {e}")
            return []
    
    async def get_information(self, record_id: str) -> Optional[InformationRecord]:
        """Get specific information by record ID.
        
        Args:
            record_id: ID of the record to retrieve
            
        Returns:
            Information record if found, None otherwise
        """
        try:
            feedback_record = await self.feedback_repository.get_feedback(record_id)
            if feedback_record:
                return self._feedback_record_to_information(feedback_record)
            return None
            
        except Exception as e:
            logger.error(f"Error getting information: {e}")
            return None
    
    async def list_information(
        self,
        information_type: Optional[InformationType] = None,
        status: Optional[FeedbackStatus] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[InformationRecord]:
        """List information records with optional filtering.
        
        Args:
            information_type: Filter by information type
            status: Filter by status
            limit: Maximum number of records to return
            offset: Number of records to skip
            
        Returns:
            List of information records
        """
        try:
            # Build filters
            filters = {}
            if information_type:
                filters['tags'] = [information_type.value]
            if status:
                filters['status'] = status
            
            # Get records from feedback repository
            records = await self.feedback_repository.list_feedback(
                filters=filters,
                limit=limit,
                offset=offset
            )
            
            # Convert to InformationRecord format
            information_records = []
            for record in records:
                info_record = self._feedback_record_to_information(record)
                if info_record:
                    information_records.append(info_record)
            
            return information_records
            
        except Exception as e:
            logger.error(f"Error listing information: {e}")
            return []
    
    async def _generate_embedding(self, text: str) -> Optional[List[float]]:
        """Generate embedding vector for text.
        
        Args:
            text: Text to embed
            
        Returns:
            Embedding vector if successful, None otherwise
        """
        try:
            if hasattr(self.vector_store, 'embedding_provider'):
                embedding = await self.vector_store.embedding_provider.embed(text)
                return embedding
            else:
                logger.warning("No embedding provider available")
                return None
                
        except Exception as e:
            logger.error(f"Error generating embedding: {e}")
            return None
    
    def _feedback_record_to_information(self, feedback_record: FeedbackRecord) -> Optional[InformationRecord]:
        """Convert FeedbackRecord to InformationRecord.
        
        Args:
            feedback_record: Feedback record to convert
            
        Returns:
            Information record if conversion successful, None otherwise
        """
        try:
            context = feedback_record.context or {}
            
            # Extract information type
            content_type_str = context.get('content_type', 'feedback_record')
            try:
                content_type = InformationType(content_type_str)
            except ValueError:
                content_type = InformationType.FEEDBACK_RECORD
            
            # Extract source
            source_str = context.get('source', 'user_input')
            try:
                source = InformationSource(source_str)
            except ValueError:
                source = InformationSource.USER_INPUT
            
            # Create metadata
            metadata = InformationMetadata(
                title=context.get('title'),
                description=context.get('description'),
                tags=feedback_record.tags or [],
                category=context.get('category'),
                priority=context.get('priority', 1),
                version=context.get('version', '1.0'),
                author=context.get('author'),
                source=source,
                created_at=feedback_record.created_at,
                updated_at=feedback_record.updated_at,
                confidence_score=context.get('confidence_score', 1.0),
                review_status=feedback_record.status,
                external_references=context.get('external_references', []),
                custom_fields=context.get('custom_fields', {})
            )
            
            # Create information record
            return InformationRecord(
                content=feedback_record.prompt,
                content_type=content_type,
                metadata=metadata,
                record_id=feedback_record.record_id,
                embedding_vector=context.get('embedding_vector')
            )
            
        except Exception as e:
            logger.error(f"Error converting feedback record to information record: {e}")
            return None
    
    async def get_database_stats(self) -> Dict[str, Any]:
        """Get database statistics.
        
        Returns:
            Dictionary with database statistics
        """
        try:
            stats = {
                'total_records': 0,
                'records_by_type': {},
                'records_by_status': {},
                'records_by_category': {},
                'embedding_coverage': 0.0,
                'last_updated': None
            }
            
            # Get all records
            all_records = await self.feedback_repository.list_feedback(limit=10000)
            
            if all_records:
                stats['total_records'] = len(all_records)
                
                # Analyze records
                for record in all_records:
                    context = record.context or {}
                    
                    # Count by type
                    info_type = context.get('content_type', 'unknown')
                    stats['records_by_type'][info_type] = stats['records_by_type'].get(info_type, 0) + 1
                    
                    # Count by status
                    status = record.status.value
                    stats['records_by_status'][status] = stats['records_by_status'].get(status, 0) + 1
                    
                    # Count by category
                    category = context.get('category', 'uncategorized')
                    stats['records_by_category'][category] = stats['records_by_category'].get(category, 0) + 1
                    
                    # Check embedding coverage
                    if context.get('embedding_vector'):
                        stats['embedding_coverage'] += 1
                    
                    # Track last update
                    if record.updated_at:
                        if not stats['last_updated'] or record.updated_at > stats['last_updated']:
                            stats['last_updated'] = record.updated_at
                
                # Calculate embedding coverage percentage
                if stats['total_records'] > 0:
                    stats['embedding_coverage'] = stats['embedding_coverage'] / stats['total_records']
            
            return stats
            
        except Exception as e:
            logger.error(f"Error getting database stats: {e}")
            return {}
    
    async def cleanup_expired_records(self) -> int:
        """Clean up expired information records.
        
        Returns:
            Number of records cleaned up
        """
        try:
            logger.info("Starting cleanup of expired records")
            
            # Get all records
            all_records = await self.feedback_repository.list_feedback(limit=10000)
            
            expired_count = 0
            for record in all_records:
                context = record.context or {}
                expires_at = context.get('expires_at')
                
                if expires_at and isinstance(expires_at, str):
                    try:
                        expires_date = datetime.fromisoformat(expires_at.replace('Z', '+00:00'))
                        if datetime.now(timezone.utc) > expires_date:
                            # Archive expired record
                            await self.feedback_repository.update_feedback_status(
                                record.record_id, FeedbackStatus.ARCHIVED
                            )
                            expired_count += 1
                    except ValueError:
                        logger.warning(f"Invalid expiry date format: {expires_at}")
            
            logger.info(f"Cleaned up {expired_count} expired records")
            return expired_count
            
        except Exception as e:
            logger.error(f"Error cleaning up expired records: {e}")
            return 0


class InformationImporter:
    """Utility class for importing information from various sources."""
    
    def __init__(self, vector_db_manager: VectorDBManager):
        """Initialize information importer.
        
        Args:
            vector_db_manager: Vector database manager instance
        """
        self.vector_db_manager = vector_db_manager
    
    async def import_from_text_file(self, file_path: str, content_type: InformationType, metadata: InformationMetadata) -> bool:
        """Import information from a text file.
        
        Args:
            file_path: Path to the text file
            content_type: Type of information being imported
            metadata: Metadata for the imported information
            
        Returns:
            True if successful, False otherwise
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                content = file.read()
            
            information = InformationRecord(
                content=content,
                content_type=content_type,
                metadata=metadata
            )
            
            return await self.vector_db_manager.add_information(information)
            
        except Exception as e:
            logger.error(f"Error importing from text file: {e}")
            return False
    
    async def import_from_json_file(self, file_path: str) -> Dict[str, int]:
        """Import information from a JSON file.
        
        Args:
            file_path: Path to the JSON file
            
        Returns:
            Dictionary with import results
        """
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
                
                return await self.vector_db_manager.batch_add_information(information_list)
            else:
                # Single record
                info = self._json_to_information_record(data)
                if info:
                    success = await self.vector_db_manager.add_information(info)
                    return {'total': 1, 'successful': 1 if success else 0, 'failed': 0 if success else 1, 'errors': []}
                else:
                    return {'total': 1, 'successful': 0, 'failed': 1, 'errors': ['Invalid data format']}
            
        except Exception as e:
            logger.error(f"Error importing from JSON file: {e}")
            return {'total': 0, 'successful': 0, 'failed': 0, 'errors': [str(e)]}
    
    def _json_to_information_record(self, data: Dict[str, Any]) -> Optional[InformationRecord]:
        """Convert JSON data to InformationRecord.
        
        Args:
            data: JSON data dictionary
            
        Returns:
            Information record if conversion successful, None otherwise
        """
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
