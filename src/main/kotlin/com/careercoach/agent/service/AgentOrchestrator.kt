package com.careercoach.agent.service

import com.careercoach.agent.base.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class AgentOrchestrator(
    private val agents: List<Agent>
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val responseCache = ConcurrentHashMap<String, AgentResponse>()
    
    suspend fun orchestrate(context: AgentContext): OrchestratedResponse {
        logger.info("Starting orchestration for profile ${context.profileId}, request type: ${context.requestType}")
        
        val relevantAgents = agents.filter { it.canHandle(context) }
        if (relevantAgents.isEmpty()) {
            logger.warn("No agents can handle context: $context")
            return OrchestratedResponse(
                context = context,
                responses = emptyList(),
                combinedContent = "No suitable agents found for this request",
                success = false
            )
        }
        
        val responses = coroutineScope {
            relevantAgents.map { agent ->
                async(Dispatchers.IO) {
                    try {
                        val cacheKey = "${agent.name}-${context.profileId}-${context.requestType}"
                        responseCache.getOrPut(cacheKey) {
                            agent.process(context)
                        }
                    } catch (e: Exception) {
                        logger.error("Error in agent ${agent.name}", e)
                        AgentResponse(
                            agentName = agent.name,
                            content = "Agent processing failed",
                            confidence = 0.0,
                            metadata = mapOf("error" to (e.message ?: "Unknown error"))
                        )
                    }
                }
            }.awaitAll()
        }
        
        val combinedContent = combineResponses(responses)
        
        return OrchestratedResponse(
            context = context,
            responses = responses,
            combinedContent = combinedContent,
            success = true,
            metadata = mapOf(
                "agents_used" to responses.map { it.agentName },
                "average_confidence" to responses.map { it.confidence }.average()
            )
        )
    }
    
    fun executeSequential(contexts: List<AgentContext>): List<OrchestratedResponse> {
        return runBlocking {
            contexts.map { context ->
                orchestrate(context.copy(
                    previousResponses = responseCache.values.toList()
                ))
            }
        }
    }
    
    fun executeParallel(contexts: List<AgentContext>): List<OrchestratedResponse> {
        return runBlocking {
            contexts.map { context ->
                async { orchestrate(context) }
            }.awaitAll()
        }
    }
    
    private fun combineResponses(responses: List<AgentResponse>): String {
        if (responses.isEmpty()) return ""
        
        val sortedResponses = responses.sortedByDescending { it.confidence }
        
        return buildString {
            appendLine("## Multi-Agent Analysis Results")
            appendLine()
            
            sortedResponses.forEachIndexed { index, response ->
                appendLine("### ${index + 1}. ${response.agentName} (Confidence: ${String.format("%.2f", response.confidence * 100)}%)")
                appendLine()
                appendLine(response.content)
                appendLine()
                
                if (response.metadata.isNotEmpty()) {
                    appendLine("**Metadata:**")
                    response.metadata.forEach { (key, value) ->
                        appendLine("- $key: $value")
                    }
                    appendLine()
                }
            }
        }
    }
    
    fun clearCache() {
        responseCache.clear()
        logger.info("Agent response cache cleared")
    }
    
    fun getAvailableAgents(): List<AgentInfo> {
        return agents.map { agent ->
            AgentInfo(
                name = agent.name,
                description = agent.description,
                specialization = agent.specialization
            )
        }
    }
}

data class OrchestratedResponse(
    val context: AgentContext,
    val responses: List<AgentResponse>,
    val combinedContent: String,
    val success: Boolean,
    val metadata: Map<String, Any> = emptyMap()
)

data class AgentInfo(
    val name: String,
    val description: String,
    val specialization: AgentSpecialization
)