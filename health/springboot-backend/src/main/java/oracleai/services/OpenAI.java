package oracleai.services;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import oracleai.AIApplication;

import java.time.Duration;
import java.util.Collections;

public class OpenAI {

    public static String chat(String textContent) throws Exception {
        return new OpenAI().doChat(textContent);
    }

    public String doChat(String textContent) throws Exception {
        try {
            // Check if API key is configured
            if (AIApplication.OPENAI_API_KEY == null || AIApplication.OPENAI_API_KEY.isEmpty()) {
                return "{\"error\": \"OpenAI API key not configured\", \"message\": \"OPENAI_API_KEY environment variable is not set\", \"fallback_response\": \"I'm sorry, OpenAI service is not configured. Please check the API key configuration.\"}";
            }

            // Create OpenAI service with timeout configuration
            OpenAiService service = new OpenAiService(AIApplication.OPENAI_API_KEY, Duration.ofSeconds(60));

            // Create chat completion request (recommended approach for GPT-3.5-turbo and
            // GPT-4)
            ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), textContent);
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo") // Use GPT-3.5-turbo as default (cost-effective and fast)
                    // .model("gpt-4") // Alternative: GPT-4 (more capable but more expensive)
                    // .model("gpt-4-turbo-preview") // Alternative: GPT-4 Turbo (latest features)
                    .messages(Collections.singletonList(message))
                    .maxTokens(600) // Match the token limit from OracleGenAI
                    .temperature(0.75) // Match the temperature from OracleGenAI
                    .topP(0.7) // Match the top_p from OracleGenAI
                    .frequencyPenalty(1.0) // Match the frequency penalty from OracleGenAI
                    .build();

            // Execute the request
            ChatCompletionResult chatCompletionResult = service.createChatCompletion(chatCompletionRequest);

            // Extract and return the response
            if (chatCompletionResult.getChoices() != null && !chatCompletionResult.getChoices().isEmpty()) {
                String response = chatCompletionResult.getChoices().get(0).getMessage().getContent();
                System.out.println("OpenAI Response: " + response);

                // Return structured response similar to OracleGenAI
                return "{\"response\": \"" + response.replace("\"", "\\\"") + "\", \"model\": \""
                        + chatCompletionRequest.getModel() + "\", \"provider\": \"OpenAI\"}";
            } else {
                return "{\"error\": \"No response from OpenAI\", \"message\": \"Empty response received\", \"fallback_response\": \"I'm sorry, I couldn't generate a response. Please try again.\"}";
            }

        } catch (Exception e) {
            System.err.println("Failed to generate text using OpenAI:");
            System.err.println("  API Key configured: "
                    + (AIApplication.OPENAI_API_KEY != null && !AIApplication.OPENAI_API_KEY.isEmpty() ? "Yes" : "No"));
            System.err.println("  Error: " + e.getMessage());
            e.printStackTrace();

            // Return a meaningful error response instead of throwing
            String escapedMessage = e.getMessage()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "{\"error\": \"OpenAI service not available\", \"message\": \"" + escapedMessage
                    + "\", \"fallback_response\": \"I'm sorry, I'm currently unable to process your request using OpenAI. Please try again later.\"}";
        }
    }

    /**
     * Alternative method using the older Completion API (for text-davinci models)
     * This is kept for backwards compatibility but ChatCompletion is recommended
     */
    public String doChatLegacy(String textContent) throws Exception {
        try {
            // Check if API key is configured
            if (AIApplication.OPENAI_API_KEY == null || AIApplication.OPENAI_API_KEY.isEmpty()) {
                return "{\"error\": \"OpenAI API key not configured\", \"message\": \"OPENAI_API_KEY environment variable is not set\", \"fallback_response\": \"I'm sorry, OpenAI service is not configured. Please check the API key configuration.\"}";
            }

            // Create OpenAI service
            OpenAiService service = new OpenAiService(AIApplication.OPENAI_API_KEY, Duration.ofSeconds(60));

            // Create completion request (legacy approach for text-davinci models)
            CompletionRequest completionRequest = CompletionRequest.builder()
                    .model("text-davinci-003") // Legacy model, consider upgrading to chat models
                    .prompt(textContent)
                    .maxTokens(600)
                    .temperature(0.75)
                    .topP(0.7)
                    .frequencyPenalty(1.0)
                    .echo(true) // Include prompt in response (similar to OracleGenAI)
                    .build();

            // Execute the request
            CompletionResult completionResult = service.createCompletion(completionRequest);

            // Extract and return the response
            if (completionResult.getChoices() != null && !completionResult.getChoices().isEmpty()) {
                String response = completionResult.getChoices().get(0).getText();
                System.out.println("OpenAI Legacy Response: " + response);
                return "{\"response\": \"" + response.replace("\"", "\\\"") + "\", \"model\": \""
                        + completionRequest.getModel() + "\", \"provider\": \"OpenAI\"}";
            } else {
                return "{\"error\": \"No response from OpenAI\", \"message\": \"Empty response received\", \"fallback_response\": \"I'm sorry, I couldn't generate a response. Please try again.\"}";
            }

        } catch (Exception e) {
            System.err.println("Failed to generate text using OpenAI (Legacy):");
            System.err.println("  API Key configured: "
                    + (AIApplication.OPENAI_API_KEY != null && !AIApplication.OPENAI_API_KEY.isEmpty() ? "Yes" : "No"));
            System.err.println("  Error: " + e.getMessage());
            e.printStackTrace();

            // Return a meaningful error response instead of throwing
            String escapedMessage = e.getMessage()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "{\"error\": \"OpenAI service not available\", \"message\": \"" + escapedMessage
                    + "\", \"fallback_response\": \"I'm sorry, I'm currently unable to process your request using OpenAI. Please try again later.\"}";
        }
    }
}
